/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2015 ForgeRock AS.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.forgerock.openidm.audit.impl;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.audit.AuditService;
import org.forgerock.openidm.audit.impl.AuditLogFilters.JsonValueObjectConverter;
import org.forgerock.openidm.audit.util.AuditConstants;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.config.enhanced.InvalidException;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.crypto.factory.CryptoServiceFactory;
import org.forgerock.openidm.router.RouteService;
import org.forgerock.openidm.patch.JsonPatch;
import org.forgerock.openidm.util.DateUtil;
import org.forgerock.openidm.util.JsonUtil;
import org.forgerock.openidm.util.ResourceUtil;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.util.Function;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.forgerock.openidm.audit.impl.AuditLogFilters.AS_SINGLE_FIELD_VALUES_FILTER;
import static org.forgerock.openidm.audit.impl.AuditLogFilters.newActivityActionFilter;
import static org.forgerock.openidm.audit.impl.AuditLogFilters.newAndCompositeFilter;
import static org.forgerock.openidm.audit.impl.AuditLogFilters.newEventTypeFilter;
import static org.forgerock.openidm.audit.impl.AuditLogFilters.newReconActionFilter;
import static org.forgerock.openidm.audit.impl.AuditLogFilters.newOrCompositeFilter;

import static org.forgerock.openidm.audit.impl.AuditLogFilters.newScriptedFilter;
import static org.forgerock.openidm.audit.util.ActivityLogger.ACTION;
import static org.forgerock.openidm.audit.util.ActivityLogger.ACTIVITY_ID;
import static org.forgerock.openidm.audit.util.ActivityLogger.AFTER;
import static org.forgerock.openidm.audit.util.ActivityLogger.BEFORE;
import static org.forgerock.openidm.audit.util.ActivityLogger.CHANGED_FIELDS;
import static org.forgerock.openidm.audit.util.ActivityLogger.MESSAGE;
import static org.forgerock.openidm.audit.util.ActivityLogger.OBJECT_ID;
import static org.forgerock.openidm.audit.util.ActivityLogger.PARENT_ACTION_ID;
import static org.forgerock.openidm.audit.util.ActivityLogger.PASSWORD_CHANGED;
import static org.forgerock.openidm.audit.util.ActivityLogger.REQUESTER;
import static org.forgerock.openidm.audit.util.ActivityLogger.REVISION;
import static org.forgerock.openidm.audit.util.ActivityLogger.ROOT_ACTION_ID;
import static org.forgerock.openidm.audit.util.ActivityLogger.STATUS;
import static org.forgerock.openidm.audit.util.ActivityLogger.TIMESTAMP;

/**
 * This audit service is the entry point for audit logging on the router.
 */
@Component(name = "org.forgerock.openidm.audit", immediate=true, policy=ConfigurationPolicy.REQUIRE)
@Service
@Properties({
    @Property(name = "service.description", value = "Audit Service"),
    @Property(name = "service.vendor", value = "ForgeRock AS"),
    @Property(name = "openidm.router.prefix", value = AuditService.ROUTER_PREFIX + "/*")
})
public class AuditServiceImpl implements AuditService {
    private static final Logger logger = LoggerFactory.getLogger(AuditServiceImpl.class);
    private static final ObjectMapper mapper;

    // Keys in the JSON configuration
    public static final String CONFIG_LOG_TO = "logTo";
    public static final String CONFIG_LOG_TYPE = "logType";
    public static final String CONFIG_LOG_TYPE_CSV = "csv";
    public static final String CONFIG_LOG_TYPE_REPO = "repository";
    public static final String CONFIG_LOG_TYPE_ROUTER = "router";

    // Types of logs
    public static final String TYPE_RECON = "recon";
    public static final String TYPE_SYNC = "sync";
    public static final String TYPE_ACTIVITY = "activity";
    public static final String TYPE_ACCESS = "access";

    // Recognized queries
    public static final String QUERY_BY_RECON_ID = "audit-by-recon-id";
    public static final String QUERY_BY_MAPPING = "audit-by-mapping";
    public static final String QUERY_BY_RECON_ID_AND_SITUATION = "audit-by-recon-id-situation";
    public static final String QUERY_BY_RECON_ID_AND_TYPE = "audit-by-recon-id-type";
    public static final String QUERY_BY_ACTIVITY_PARENT_ACTION = "audit-by-activity-parent-action";

    // Log property keys
    public static final String LOG_ID = "_id";

    public static final String ACCESS_LOG_ACTION = "action";
    public static final String ACCESS_LOG_IP = "ip";
    public static final String ACCESS_LOG_PRINCIPAL = "principal";
    public static final String ACCESS_LOG_ROLES = "roles";
    public static final String ACCESS_LOG_STATUS = "status";
    public static final String ACCESS_LOG_TIMESTAMP = "timestamp";
    public static final String ACCESS_LOG_USERID = "userid";

    // activity log property key constants are in org.forgerock.util.ActivityLog

    public static final String RECON_LOG_ENTRY_TYPE = "entryType";
    public static final String RECON_LOG_TIMESTAMP = "timestamp";
    public static final String RECON_LOG_RECON_ID = "reconId";
    public static final String RECON_LOG_RECON_ACTION = "reconAction";
    public static final String RECON_LOG_ROOT_ACTION_ID = "rootActionId";
    public static final String RECON_LOG_STATUS = "status";
    public static final String RECON_LOG_MESSAGE = "message";
    public static final String RECON_LOG_MESSAGE_DETAIL = "messageDetail";
    public static final String RECON_LOG_EXCEPTION = "exception";
    public static final String RECON_LOG_ACTION_ID = "actionId";
    public static final String RECON_LOG_ACTION = "action";
    public static final String RECON_LOG_AMBIGUOUS_TARGET_OBJECT_IDS = "ambiguousTargetObjectIds";
    public static final String RECON_LOG_RECONCILING = "reconciling";
    public static final String RECON_LOG_SITUATION = "situation";
    public static final String RECON_LOG_SOURCE_OBJECT_ID = "sourceObjectId";
    public static final String RECON_LOG_TARGET_OBJECT_ID = "targetObjectId";
    public static final String RECON_LOG_MAPPING = "mapping";
    public static final String RECON_LOG_LINK_QUALIFIER = "linkQualifier";

    public static final String SYNC_LOG_TIMESTAMP = "timestamp";
    public static final String SYNC_LOG_ACTION_ID = "actionId";
    public static final String SYNC_LOG_ROOT_ACTION_ID = "rootActionId";
    public static final String SYNC_LOG_STATUS = "status";
    public static final String SYNC_LOG_MESSAGE = "message";
    public static final String SYNC_LOG_MESSAGE_DETAIL = "messageDetail";
    public static final String SYNC_LOG_EXCEPTION = "exception";
    public static final String SYNC_LOG_ACTION = "action";
    public static final String SYNC_LOG_SITUATION = "situation";
    public static final String SYNC_LOG_SOURCE_OBJECT_ID = "sourceObjectId";
    public static final String SYNC_LOG_TARGET_OBJECT_ID = "targetObjectId";
    public static final String SYNC_LOG_MAPPING = "mapping";
    public static final String SYNC_LOG_LINK_QUALIFIER = "linkQualifier";

    // ----- Declarative Service Implementation

    /** The connection factory */
    @Reference(policy = ReferencePolicy.STATIC, target="(service.pid=org.forgerock.openidm.internal)")
    protected ConnectionFactory connectionFactory;

    /** Although we may not need the router here,
     https://issues.apache.org/jira/browse/FELIX-3790
     if using this with for scr 1.6.2
     Ensure we do not get bound on router whilst it is activating
     */
    @Reference(target = "("+ServerConstants.ROUTER_PREFIX + "=/*)")
    RouteService routeService;

    /** Script Registry service. */
    @Reference(policy = ReferencePolicy.STATIC)
    protected ScriptRegistry scriptRegistry;

    /** the script to execute to format exceptions */
    private static ScriptEntry exceptionFormatterScript = null;

    /**
     * A type-alias factory function to create an AuditLogger from config.
     */
    interface AuditLoggerFactory extends Function<JsonValue, AuditLogger, InvalidException> { }

    /** the function used to instantiate AuditLoggers from config */
    AuditLoggerFactory auditLoggerFactory = new AuditLoggerFactory() {
        @Override
        public AuditLogger apply(JsonValue config) throws InvalidException {
            String logType = config.get(CONFIG_LOG_TYPE).asString();

            // TODO: make pluggable
            final AuditLogger auditLogger;
            if (CONFIG_LOG_TYPE_CSV.equalsIgnoreCase(logType)) {
                auditLogger = new CSVAuditLogger();
            } else if (CONFIG_LOG_TYPE_REPO.equalsIgnoreCase(logType)) {
                auditLogger = new RepoAuditLogger(connectionFactory);
            } else if (CONFIG_LOG_TYPE_ROUTER.equalsIgnoreCase(logType)) {
                auditLogger = new RouterAuditLogger(connectionFactory);
            } else {
                throw new InvalidException("Configured audit logType is unknown: " + logType);
            }

            auditLogger.setConfig(config);
            logger.info("Audit configured to log to {}", logType);
            if (auditLogger.isUsedForQueries()) {
                logger.info("Audit logger used for queries set to " + logType);
            }
            return auditLogger;
        }
    };

    EnhancedConfig enhancedConfig = new JSONEnhancedConfig();

    AuditLogFilter auditFilter = AuditLogFilters.NEVER;

    List<JsonPointer> watchFieldFilters = new ArrayList<JsonPointer>();
    List<JsonPointer> passwordFieldFilters = new ArrayList<JsonPointer>();

    List<AuditLogger> globalAuditLoggers = new ArrayList<AuditLogger>();
    Map<String,List<AuditLogger>> eventAuditLoggers = new HashMap<String, List<AuditLogger>>();
    JsonValue config; // Existing active configuration

    // TODO make dateUtil configurable (needs to be done in all locations)
    DateUtil dateUtil = DateUtil.getDateUtil("UTC");

    static {
        JsonFactory jsonFactory = new JsonFactory();
        jsonFactory.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true);
        mapper = new ObjectMapper(jsonFactory);
    }

    final AuditLogFilterBuilder auditLogFilterBuilder = new AuditLogFilterBuilder()
            /* filter activity events on configured actions to include */
            .add("eventTypes/activity/filter/actions",
                    new JsonValueObjectConverter<AuditLogFilter>() {
                        @Override
                        public AuditLogFilter apply(JsonValue actions) {
                            return newActivityActionFilter(actions);
                        }
                    })
            /* filter activity events on configured actions to include when a particular trigger context is in scope */
            .add("eventTypes/activity/filter/triggers",
                    new JsonValueObjectConverter<AuditLogFilter>() {
                        @Override
                        public AuditLogFilter apply(JsonValue triggers) {
                            List<AuditLogFilter> filters = new ArrayList<AuditLogFilter>();
                            for (String trigger : triggers.keys()) {
                                filters.add(newActivityActionFilter(triggers.get(trigger), trigger));
                            }
                            return newOrCompositeFilter(filters);
                        }
                    })
            /* filter recon events on configured actions to include */
            .add("eventTypes/recon/filter/actions",
                    new JsonValueObjectConverter<AuditLogFilter>() {
                        @Override
                        public AuditLogFilter apply(JsonValue actions) {
                            return newReconActionFilter(actions);
                        }
                    })
            /* filter recon events on configured actions to include when a particular trigger context is in scope */
            .add("eventTypes/recon/filter/triggers",
                    new JsonValueObjectConverter<AuditLogFilter>() {
                        @Override
                        public AuditLogFilter apply(JsonValue triggers) {
                            List<AuditLogFilter> filters = new ArrayList<AuditLogFilter>();
                            for (String trigger : triggers.keys()) {
                                filters.add(newReconActionFilter(triggers.get(trigger), trigger));
                            }
                            return newOrCompositeFilter(filters);
                        }
                    })
            /* filter events with specific field values for any event type */
            .add("eventTypes/*/filter/fields",
                    new JsonValueObjectConverter<AuditLogFilter>() {
                        @Override
                        public AuditLogFilter apply(JsonValue fieldsConfig) {
                            List<AuditLogFilter> filters = new ArrayList<AuditLogFilter>();
                            // the glob in the JsonPointer will return a map of matched entry types to field configurations
                            for (String eventType : fieldsConfig.keys()) {
                                // fieldConfig is something like { "field" : "type", "values" : [ "summary" ] }
                                JsonValue fieldConfig = fieldsConfig.get(eventType);
                                filters.add(newEventTypeFilter(eventType,
                                        newAndCompositeFilter(fieldConfig.asList(AS_SINGLE_FIELD_VALUES_FILTER))));
                            }
                            return newOrCompositeFilter(filters);
                        }
                    });

    @Activate
    void activate(ComponentContext compContext) throws Exception {
        logger.debug("Activating Service with configuration {}", compContext.getProperties());
        try {
            // Upon activation the ScriptRegistry is present so we can add script-based audit log filters for event types
            auditLogFilterBuilder.add("eventTypes/*/filter/script",
                    new JsonValueObjectConverter<AuditLogFilter>() {
                        @Override
                        public AuditLogFilter apply(JsonValue scriptConfig) {
                            List<AuditLogFilter> filters = new ArrayList<AuditLogFilter>();
                            for (String eventType : scriptConfig.keys()) {
                                JsonValue filterConfig = scriptConfig.get(eventType);
                                try {
                                    filters.add(newScriptedFilter(eventType, scriptRegistry.takeScript(filterConfig)));
                                } catch (Exception e) {
                                    logger.error("Audit Log Filter builder threw exception {} while processing {} for {}",
                                            e.getClass().getName(), filterConfig.toString(), eventType, e);
                                }
                            }
                            return newOrCompositeFilter(filters);
                        }
                    });
            setConfig(enhancedConfig.getConfigurationAsJson(compContext));
        } catch (Exception ex) {
            logger.warn("Configuration invalid, can not start Audit service.", ex);
            throw ex;
        }
        logger.info("Audit service started.");
    }

    /**
     * Configuration modified handling
     * Ensures audit logging service stays registered
     * even whilst configuration changes
     */
    @Modified
    void modified(ComponentContext compContext) throws Exception {
        logger.debug("Reconfiguring audit service with configuration {}", compContext.getProperties());
        try {
            JsonValue newConfig = enhancedConfig.getConfigurationAsJson(compContext);
            if (hasConfigChanged(config, newConfig)) {
                deactivate(compContext);
                activate(compContext);
                logger.info("Reconfigured audit service {}", compContext.getProperties());
            }
        } catch (Exception ex) {
            logger.warn("Configuration invalid, can not reconfigure Audit service.", ex);
            throw ex;
        }
    }

    private boolean hasConfigChanged(JsonValue existingConfig, JsonValue newConfig) {
        return JsonPatch.diff(existingConfig, newConfig).size() > 0;
    }

    void setConfig(JsonValue jsonConfig) throws Exception {
        config = jsonConfig;
        globalAuditLoggers.addAll(getGlobalAuditLoggers(config));
        eventAuditLoggers.putAll(getEventAuditLoggers(config));
        auditFilter = auditLogFilterBuilder.build(config);
        watchFieldFilters.addAll(getEventJsonPointerList(config, TYPE_ACTIVITY, "watchedFields"));
        passwordFieldFilters.addAll(getEventJsonPointerList(config, TYPE_ACTIVITY, "passwordFields"));
        JsonValue efConfig = config.get("exceptionFormatter");
        if (!efConfig.isNull()) {
            exceptionFormatterScript =  scriptRegistry.takeScript(efConfig);
        }
    }

    @Deactivate
    void deactivate(ComponentContext compContext) {
        logger.debug("Deactivating Service {}", compContext.getProperties());
        for (AuditLogger auditLogger : globalAuditLoggers) {
            try {
                auditLogger.cleanup();
            } catch (Exception ex) {
                logger.info("AuditLogger cleanup reported failure", ex);
            }
        }
        for (List<AuditLogger> auditLoggers : eventAuditLoggers.values()) {
            for (AuditLogger auditLogger : auditLoggers) {
                try {
                    auditLogger.cleanup();
                } catch (Exception ex) {
                    logger.info("AuditLogger cleanup reported failure", ex);
                }
            }
        }

        // remove script-based audit log filters for event types
        auditLogFilterBuilder.remove("eventTypes/*/filter/script");

        globalAuditLoggers.clear();
        eventAuditLoggers.clear();
        auditFilter = AuditLogFilters.NEVER;
        watchFieldFilters.clear();
        passwordFieldFilters.clear();
        logger.info("Audit service stopped.");
    }

    /**
     * Gets an object from the audit logs by identifier. The returned object is not validated
     * against the current schema and may need processing to conform to an updated schema.
     * <p>
     * The object will contain metadata properties, including object identifier {@code _id},
     * and object version {@code _rev} to enable optimistic concurrency
     *
     * {@inheritDoc}
     */
    @Override
    public void handleRead(final ServerContext context, final ReadRequest request, final ResultHandler<Resource> handler) {
        try {
            final String type = request.getResourceNameObject().head(1).toString();
            final String id = request.getResourceNameObject().size() > 1
                    ? request.getResourceNameObject().tail(1).toString()
                    : null;

            logger.debug("Audit read called for {}", request.getResourceName());
            AuditLogger auditLogger = getQueryAuditLogger(type);
            Map<String, Object> r = auditLogger.read(context, type, id);
            handler.handleResult(new Resource((String)r.get(Resource.FIELD_CONTENT_ID), null, new JsonValue(r)));
        } catch (Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
    }

    /**
     * Creates a new object in the object set.
     * <p>
     * This method sets the {@code _id} property to the assigned identifier for the object,
     * and the {@code _rev} property to the revised object version (For optimistic concurrency)
     *
     * {@inheritDoc}
     */
    @Override
    public void handleCreate(final ServerContext context, final CreateRequest request,
            final ResultHandler<Resource> handler) {
        try {
            if (request.getResourceName() == null) {
                throw new BadRequestException("Audit service called without specifying which audit log in the identifier");
            }

            Map<String, Object> obj = request.getContent().asMap();

            // Don't audit the audit log
            if (context.containsContext(AuditContext.class)) {
                handler.handleResult(new Resource(null, null, new JsonValue(obj)));
                return;
            }

            // Audit create called for /access with {timestamp=2013-07-30T18:10:03.773Z, principal=openidm-admin, status=SUCCESS, roles=[openidm-admin, openidm-authorized], action=authenticate, userid=openidm-admin, ip=127.0.0.1}
            logger.debug("Audit create called for {} with {}", request.getResourceName(), obj);

            String type = request.getResourceNameObject().head(1).toString();
            JsonValue action = request.getContent().get("action");

            if (auditFilter.isFiltered(context, request)) {
                logger.debug("Filtered by filter for action {}", new Object[] { action.toString() });
                handler.handleResult(new Resource(null, null, new JsonValue(obj)));
                return;
            }

            // Activity log preprocessing
            if (TYPE_ACTIVITY.equals(type)) {
                processActivityLog(obj);
            }

            // Generate an ID for the object
            final String localId = (request.getNewResourceId() == null || request.getNewResourceId().isEmpty())
                    ? UUID.randomUUID().toString()
                    : request.getNewResourceId();
            obj.put(Resource.FIELD_CONTENT_ID, localId);

            // Generate unified timestamp
            if (null == obj.get("timestamp")) {
                obj.put("timestamp", dateUtil.now());
            }

            logger.debug("Create audit entry for {}/{} with {}", type, localId, obj);
            for (AuditLogger auditLogger : getAuditLoggerForEvent(type)) {
                try {
                    auditLogger.create(context, type, obj);
                } catch (ResourceException ex) {
                    logger.warn("Failure writing audit log: {}/{} with logger {}", new Object[] {type, localId, auditLogger, ex});
                    if (!auditLogger.isIgnoreLoggingFailures()) {
                        throw ex;
                    }
                } catch (RuntimeException ex) {
                    logger.warn("Failure writing audit log: {}/{} with logger {}", new Object[] {type, localId, auditLogger, ex});
                    if (!auditLogger.isIgnoreLoggingFailures()) {
                        throw ex;
                    }
                }
            }
            handler.handleResult(new Resource(localId, null, new JsonValue(obj)));
        } catch (Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
    }

    /**
     * Do any preprocessing for activity log objects
     * Checks for any changed fields and adds those to the object
     * Also adds a flag to detect if any of the flagged password fields have changed
     * NOTE: both the watched fields and the password fields will be in the list of "changedField" if they differ
     * @param activity activity object to update
     */
    private void processActivityLog(Map<String, Object> activity) {
        List<String> changedFields = new ArrayList<String>();
        boolean passwordChanged = false;

        Object rawBefore = activity.get(BEFORE);
        Object rawAfter = activity.get(AFTER);

        if (!(rawBefore == null && rawAfter == null)) {
            JsonValue before = new JsonValue(rawBefore);
            JsonValue after = new JsonValue(rawAfter);

            // Check to see if any of the watched fields have changed and add them to the comprehensive list
            List<String> changedWatchFields = checkForFields(watchFieldFilters, before, after);
            changedFields.addAll(changedWatchFields);

            // Check to see if any of the password fields have changed -- also update our flag
            List<String> changedPasswordFields = checkForFields(passwordFieldFilters, before, after);
            passwordChanged = !changedPasswordFields.isEmpty();
            changedFields.addAll(changedPasswordFields);

            // Update the before and after fields with their proper string values now that we're done diffing
            // TODO Figure out if this is even necessary? Doesn't seem to be... Once it goes to the log,
            // the object will have toString() called on it anyway which will convert it to (seemingly) the same format
            try {
                activity.put(BEFORE, (JsonUtil.jsonIsNull(before)) ? null : mapper.writeValueAsString(before.getObject()));
            } catch (IOException e) {
                activity.put(BEFORE, (JsonUtil.jsonIsNull(before)) ? null : before.getObject().toString());
            }
            try {
                activity.put(AFTER, (JsonUtil.jsonIsNull(after)) ? null : mapper.writeValueAsString(after.getObject())); // how can we know for system objects?
            } catch (IOException e) {
                activity.put(AFTER, (JsonUtil.jsonIsNull(after)) ? null : after.getObject().toString()); // how can we know for system objects?
            }
        }

        // Add the list of changed fields to the object
        activity.put(CHANGED_FIELDS, changedFields.isEmpty() ? null : changedFields);
        // Add the flag indicating password fields have changed
        activity.put(PASSWORD_CHANGED, passwordChanged);
    }

    /**
     * Checks to see if there are differences between the values in two JsonValues before and after
     * Returns a list containing the changed fields
     *
     * @param fieldsToCheck list of JsonPointers to search for
     * @param before prior JsonValue
     * @param after JsonValue after applied changes
     * @return list of strings indicating which values changed
     */
    private List<String> checkForFields(List<JsonPointer> fieldsToCheck,  JsonValue before, JsonValue after) {
        List<String> changedFields = new ArrayList<String>();
        for (JsonPointer jpointer : fieldsToCheck) {
            // Need to be sure to decrypt any encrypted values so we can compare their string value
            // (JsonValue does not have an #equals method that works for this purpose)
            CryptoService crypto = CryptoServiceFactory.getInstance();
            Object beforeValue = crypto.decryptIfNecessary(before.get(jpointer)).getObject();
            Object afterValue = crypto.decryptIfNecessary(after.get(jpointer)).getObject();
            if (!fieldsEqual(beforeValue, afterValue)) {
                changedFields.add(jpointer.toString());
            }
        }

        return changedFields;
    }

    /**
     * Checks to see if two objects are equal either as nulls or through their comparator
     * @param a first object to compare
     * @param b reference object to compare against
     * @return boolean indicating equality either as nulls or as objects
     */
    private static boolean fieldsEqual(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    /**
     * Audit service does not support changing audit entries.
     */
    @Override
    public void handleUpdate(final ServerContext context, final UpdateRequest request,
            final ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupported(request));
    }

    /**
     * Audit service currently does not support deleting audit entries.
     *
     * Deletes the specified object from the object set.
     *
     * {@inheritDoc}
     */
    @Override
    public void handleDelete(ServerContext context, DeleteRequest request,
            ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupported(request));
    }

    /**
     * Audit service does not support changing audit entries.
     *
     * {@inheritDoc}
     */
    @Override
    public void handlePatch(final ServerContext context, final PatchRequest request,
            final ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupported(request));
    }

    /**
     * Performs the query on the specified object and returns the associated results.
     * <p>
     * Queries are parametric; a set of named parameters is provided as the query criteria.
     * The query result is a JSON object structure composed of basic Java types.
     *
     * The returned map is structured as follow:
     * <ul>
     * <li>The top level map contains meta-data about the query, plus an entry with the actual result records.
     * <li>The <code>QueryConstants</code> defines the map keys, including the result records (QUERY_RESULT)
     * </ul>
     *
     * {@inheritDoc}
     */
    @Override
    public void handleQuery(final ServerContext context, final QueryRequest request, final QueryResultHandler handler) {
        try {
            final String type = request.getResourceNameObject().head(1).toString();
            final boolean formatted = getFormattedValue(request.getAdditionalParameter("formatted"));
            logger.debug("Audit query called for {} with {}", request.getResourceName(), request.getAdditionalParameters());
            AuditLogger auditLogger = getQueryAuditLogger(type);
            auditLogger.query(context, request, handler, type, formatted);
        } catch (Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
    }
    /**
     * Audit service does not support actions on audit entries.
     *
     * {@inheritDoc}
     */
    @Override
    public void handleAction(final ServerContext context, final ActionRequest request,
            final ResultHandler<JsonValue> handler) {
        handler.handleError(ResourceUtil.notSupported(request));
    }

    /**
     * Returns the logger to use for reads/queries.
     *
     * @param type the event type for which to return the query logger
     * @return an AuditLogger to use for queries.
     * @throws ResourceException on failure to find an appropriate logger.
     */
    private AuditLogger getQueryAuditLogger(String type) throws ResourceException {
        // look for a query logger for this eventtype
        if (eventAuditLoggers != null
                && eventAuditLoggers.containsKey(type)) {
            AuditLogger auditLogger = getQueryAuditLogger(eventAuditLoggers.get(type));
            if (auditLogger != null) {
                return auditLogger;
            }
        }

        // look for a global query logger
        if (globalAuditLoggers.size() > 0) {
            AuditLogger auditLogger = getQueryAuditLogger(globalAuditLoggers);
            if (auditLogger != null) {
                return auditLogger;
            }
        }

        // pick first available eventtype logger
        if (eventAuditLoggers != null
                && eventAuditLoggers.containsKey(type)
                && eventAuditLoggers.get(type).size() > 0) {
            return eventAuditLoggers.get(type).get(0);
        }

        // pick first global logger
        if (globalAuditLoggers != null
                && globalAuditLoggers.size() > 0) {
            return globalAuditLoggers.get(0);
        }

        // give up
        throw new InternalServerErrorException("No audit loggers available");
    }

    /**
     * Return the first audit logger in <tt>auditLoggers</tt> that is configured for queries.
     *
     * @param auditLoggers a <tt>List</tt> of <tt>AuditLoggers</tt> to inspect
     * @return an AuditLogger configured for queries, null if one does not exist in the list
     */
    private AuditLogger getQueryAuditLogger(List<AuditLogger> auditLoggers) {
        for (AuditLogger auditLogger : auditLoggers) {
            if (auditLogger.isUsedForQueries()) {
                return auditLogger;
            }
        }
        return null;
    }

    /**
     * Return the configured <tt>AuditLoggers</tt> for an event type.
     *
     * @param type the event type
     * @return the configured AuditLoggers for this event type
     */
    private List<AuditLogger> getAuditLoggerForEvent(String type) {
        // defer to event-specific audit loggers first if there are any
        if (eventAuditLoggers != null
                && eventAuditLoggers.containsKey(type)
                && eventAuditLoggers.get(type).size() > 0) {
            return eventAuditLoggers.get(type);
        }
        else {
            return globalAuditLoggers;
        }
    }

    /**
     * Fetches a list of JsonPointers from the config file under a specified event and field name
     * Expects it to look similar to:
     *
     *<PRE>
     * {
     *    "eventTypes": {
     *      "activity" : {
     *        "watchedFields": [ "email" ],
     *        "passwordFields": [ "password1", "password2" ]
     *      }
     *    }
     * }
     * </PRE>
     *
     * @param config the config object to draw from
     * @param event which event to draw from. ie "activity"
     * @param fieldName which fieldName to draw from. ie "watchedFields"
     * @return list containing the JsonPointers generated by the strings in the field
     */
    List<JsonPointer> getEventJsonPointerList(JsonValue config, String event, String fieldName) {
        ArrayList<JsonPointer> fieldList = new ArrayList<JsonPointer>();
        JsonValue fields = config.get("eventTypes").get(event).get(fieldName);
        for (JsonValue field : fields) {
            fieldList.add(field.asPointer());
        }
        return fieldList;
    }

    Map<String, List<AuditLogger>> getEventAuditLoggers(JsonValue config) {
        Map<String, List<AuditLogger>> configuredLoggers = new HashMap<String, List<AuditLogger>>();

        JsonValue eventTypes = config.get("eventTypes");
        if (!eventTypes.isNull()) {
            Set<String> eventTypesKeys = eventTypes.keys();
            // Loop through event types ("activity", "recon", etc..)
            for (String eventTypeKey : eventTypesKeys) {
                JsonValue eventType = eventTypes.get(eventTypeKey);
                configuredLoggers.put(eventTypeKey, getAuditLoggers(eventType.get(CONFIG_LOG_TO)));
            }
        }

        return configuredLoggers;
    }

    List<AuditLogger> getGlobalAuditLoggers(JsonValue config) {
        return getAuditLoggers(config.get(CONFIG_LOG_TO));
    }

    List<AuditLogger> getAuditLoggers(JsonValue logTo) {
        List<AuditLogger> configuredLoggers = new ArrayList<AuditLogger>();
        if (logTo != null && logTo.isList()) {
            for (AuditLogger auditLogger : logTo.asList(auditLoggerFactory)) {
                configuredLoggers.add(auditLogger);
            }
        }
        return configuredLoggers;
    }

    public static void preformatLogEntry(String type, Map<String, Object> entryMap) {
        if (TYPE_RECON.equals(type) || TYPE_SYNC.equals(type)) {
            Object exception = entryMap.get("exception");
            try {
                if (exception == null) {
                    entryMap.put("exception", "");
                } else if (!(exception instanceof String)) {
                    entryMap.put("exception", AuditServiceImpl.formatException((Exception) exception));
                }
            } catch (Exception e) {
                logger.warn("Error formatting Exception: " + e);
            }
        }
    }

    public static String formatException(Exception e) throws Exception {
        if (e == null) {
            return "";
        }
        String result = e.getMessage();
        if (exceptionFormatterScript != null) {
            Script s = exceptionFormatterScript.getScript(new RootContext());
            s.put("exception", e);
            result = (String) s.eval();
        }
        return result;
    }

    public static Map<String, Object> formatLogEntry(Map<String, Object> entry, String type) {
        if (AuditServiceImpl.TYPE_RECON.equals(type)) {
            return AuditServiceImpl.formatReconEntry(entry);
        } else if (AuditServiceImpl.TYPE_SYNC.equals(type)) {
            return AuditServiceImpl.formatSyncEntry(entry);
        } else if (AuditServiceImpl.TYPE_ACTIVITY.equals(type)) {
            return AuditServiceImpl.formatActivityEntry(entry);
        } else if (AuditServiceImpl.TYPE_ACCESS.equals(type)) {
            return AuditServiceImpl.formatAccessEntry(entry);
        } else {
            return entry;
        }
    }

    /**
     * Returns an ordered audit log access entry.
     *
     * @param entry the full entry to format
     * @return the formatted entry
     */
    static Map<String,Object> formatAccessEntry(Map<String,Object> entry) {
        Map<String, Object> formattedEntry = new LinkedHashMap<String, Object>();
        formattedEntry.put(LOG_ID, entry.get(LOG_ID));
        formattedEntry.put(ACCESS_LOG_ACTION, entry.get(ACCESS_LOG_ACTION));
        formattedEntry.put(ACCESS_LOG_IP, entry.get(ACCESS_LOG_IP));
        formattedEntry.put(ACCESS_LOG_PRINCIPAL, entry.get(ACCESS_LOG_PRINCIPAL));
        formattedEntry.put(ACCESS_LOG_ROLES, entry.get(ACCESS_LOG_ROLES));
        formattedEntry.put(ACCESS_LOG_STATUS, entry.get(ACCESS_LOG_STATUS));
        formattedEntry.put(ACCESS_LOG_TIMESTAMP, entry.get(ACCESS_LOG_TIMESTAMP));
        formattedEntry.put(ACCESS_LOG_USERID, entry.get(ACCESS_LOG_USERID));
        return formattedEntry;
    }

    /**
     * Returns an ordered audit log activity entry.
     *
     * @param entry the full entry to format
     * @return the formatted entry
     */
    static Map<String,Object> formatActivityEntry(Map<String,Object> entry) {
        Map<String, Object> formattedEntry = new LinkedHashMap<String, Object>();
        formattedEntry.put(LOG_ID, entry.get(LOG_ID));
        formattedEntry.put(ACTIVITY_ID, entry.get(ACTIVITY_ID));
        formattedEntry.put(TIMESTAMP, entry.get(TIMESTAMP));
        formattedEntry.put(ACTION, entry.get(ACTION));
        formattedEntry.put(MESSAGE, entry.get(MESSAGE));
        formattedEntry.put(OBJECT_ID, entry.get(OBJECT_ID));
        formattedEntry.put(REVISION, entry.get(REVISION));
        formattedEntry.put(ROOT_ACTION_ID, entry.get(ROOT_ACTION_ID));
        formattedEntry.put(PARENT_ACTION_ID, entry.get(PARENT_ACTION_ID));
        formattedEntry.put(REQUESTER, entry.get(REQUESTER));
        formattedEntry.put(BEFORE, entry.get(BEFORE));
        formattedEntry.put(AFTER, entry.get(AFTER));
        formattedEntry.put(STATUS, entry.get(STATUS));
        formattedEntry.put(CHANGED_FIELDS, entry.get(CHANGED_FIELDS));
        formattedEntry.put(PASSWORD_CHANGED, entry.get(PASSWORD_CHANGED));
        return formattedEntry;
    }

    /**
     * Returns a audit log sync entry.
     *
     * @param entry the full entry to format
     * @return the formatted entry
     */
    public static Map<String, Object> formatSyncEntry(Map<String, Object> entry) {
        Map<String, Object> formattedEntry = new LinkedHashMap<String, Object>();
        formattedEntry.put(LOG_ID, entry.get(LOG_ID));
        formattedEntry.put(SYNC_LOG_TIMESTAMP, entry.get(SYNC_LOG_TIMESTAMP));
        formattedEntry.put(SYNC_LOG_ROOT_ACTION_ID, entry.get(SYNC_LOG_ROOT_ACTION_ID));
        formattedEntry.put(SYNC_LOG_STATUS, entry.get(SYNC_LOG_STATUS));
        formattedEntry.put(SYNC_LOG_MESSAGE, entry.get(SYNC_LOG_MESSAGE));
        formattedEntry.put(SYNC_LOG_MESSAGE_DETAIL, entry.get(SYNC_LOG_MESSAGE_DETAIL));
        formattedEntry.put(SYNC_LOG_EXCEPTION, entry.get(SYNC_LOG_EXCEPTION));
        formattedEntry.put(SYNC_LOG_ACTION, entry.get(SYNC_LOG_ACTION));
        formattedEntry.put(SYNC_LOG_ACTION_ID, entry.get(SYNC_LOG_ACTION_ID));
        formattedEntry.put(SYNC_LOG_SITUATION, entry.get(SYNC_LOG_SITUATION));
        formattedEntry.put(SYNC_LOG_SOURCE_OBJECT_ID, entry.get(SYNC_LOG_SOURCE_OBJECT_ID));
        formattedEntry.put(SYNC_LOG_TARGET_OBJECT_ID, entry.get(SYNC_LOG_TARGET_OBJECT_ID));
        formattedEntry.put(SYNC_LOG_MAPPING, entry.get(SYNC_LOG_MAPPING));
        formattedEntry.put(SYNC_LOG_LINK_QUALIFIER, entry.get(SYNC_LOG_LINK_QUALIFIER));
        return formattedEntry;
    }

    /**
     * Returns a audit log recon entry formatted based on the entryType (summary, start, recon entry).
     *
     * @param entry the full entry to format
     * @return the formatted entry
     */
    public static Map<String, Object> formatReconEntry(Map<String, Object> entry) {
        Map<String, Object> formattedEntry = new LinkedHashMap<String, Object>();
        formattedEntry.put(LOG_ID, entry.get(LOG_ID));
        formattedEntry.put(RECON_LOG_ENTRY_TYPE, entry.get(RECON_LOG_ENTRY_TYPE));
        formattedEntry.put(RECON_LOG_TIMESTAMP, entry.get(RECON_LOG_TIMESTAMP));
        formattedEntry.put(RECON_LOG_RECON_ID, entry.get(RECON_LOG_RECON_ID));
        formattedEntry.put(RECON_LOG_RECON_ACTION, entry.get(RECON_LOG_RECON_ACTION));
        formattedEntry.put(RECON_LOG_ROOT_ACTION_ID, entry.get(RECON_LOG_ROOT_ACTION_ID));
        formattedEntry.put(RECON_LOG_STATUS, entry.get(RECON_LOG_STATUS));
        formattedEntry.put(RECON_LOG_MESSAGE, entry.get(RECON_LOG_MESSAGE));
        formattedEntry.put(RECON_LOG_MESSAGE_DETAIL, entry.get(RECON_LOG_MESSAGE_DETAIL));
        formattedEntry.put(RECON_LOG_EXCEPTION, entry.get(RECON_LOG_EXCEPTION));
        if (AuditConstants.RECON_LOG_ENTRY_TYPE_RECON_ENTRY.equals(entry.get(RECON_LOG_ENTRY_TYPE))) {
            // recon entry
            formattedEntry.put(RECON_LOG_ACTION_ID, entry.get(RECON_LOG_ACTION_ID));
            formattedEntry.put(RECON_LOG_ACTION, entry.get(RECON_LOG_ACTION));
            formattedEntry.put(RECON_LOG_AMBIGUOUS_TARGET_OBJECT_IDS, entry.get(RECON_LOG_AMBIGUOUS_TARGET_OBJECT_IDS));
            formattedEntry.put(RECON_LOG_RECONCILING, entry.get(RECON_LOG_RECONCILING));
            formattedEntry.put(RECON_LOG_SITUATION, entry.get(RECON_LOG_SITUATION));
            formattedEntry.put(RECON_LOG_SOURCE_OBJECT_ID, entry.get(RECON_LOG_SOURCE_OBJECT_ID));
            formattedEntry.put(RECON_LOG_TARGET_OBJECT_ID, entry.get(RECON_LOG_TARGET_OBJECT_ID));
            formattedEntry.put(RECON_LOG_LINK_QUALIFIER, entry.get(RECON_LOG_LINK_QUALIFIER));
        }
        formattedEntry.put(RECON_LOG_MAPPING, entry.get(RECON_LOG_MAPPING));
        return formattedEntry;
    }

    public static JsonValue getReconResults(List<Map<String, Object>> entryList, boolean formatted) {
        JsonValue results = json(object());

        if (formatted) {
            List<Map<String, Object>> resultEntries = new ArrayList<Map<String, Object>>();
            for (Map<String, Object> entry : entryList) {
                resultEntries.add(AuditServiceImpl.formatReconEntry(entry));
            }
            results.put("result", resultEntries);
        } else {
            results.put("result", entryList);
        }
        return results;
    }

    public static JsonValue getActivityResults(List<Map<String, Object>> entryList, boolean formatted) {
        return getResults(entryList, formatted, AuditServiceImpl.TYPE_ACTIVITY);
    }

    public static JsonValue getAccessResults(List<Map<String, Object>> entryList, boolean formatted) {
        return getResults(entryList, formatted, AuditServiceImpl.TYPE_ACCESS);
    }
    
    private static JsonValue getResults(List<Map<String, Object>> entryList, boolean formatted, String type) {
        JsonValue results = json(object());
        if (formatted) {
            List<Map<String, Object>> formattedList = new ArrayList<Map<String, Object>>();
            for (Map<String, Object> entry : entryList) {
                if (type.equals(AuditServiceImpl.TYPE_ACTIVITY)) {
                    formattedList.add(AuditServiceImpl.formatActivityEntry(entry));
                } else if (type.equals(AuditServiceImpl.TYPE_ACCESS)) {
                    formattedList.add(AuditServiceImpl.formatAccessEntry(entry));
                } else {
                    formattedList.add(entry);
                }
            }
            results.put("result", formattedList);
        } else {
            results.put("result", entryList);
        }
        results.put("result", entryList);
        return results;
    }

    protected static JsonValue parseJsonString(String stringified, Class<?> jsonStructureType) {
        try {
            return new JsonValue(mapper.readValue(stringified, jsonStructureType));
        } catch (IOException ex) {
            throw new JsonException("String passed into parsing is not valid JSON", ex);
        }
    }

    private boolean getFormattedValue(Object formatted) {
        if (formatted == null) {
            return true;
        } else if (formatted instanceof String) {
            return Boolean.valueOf((String)formatted);
        } else if (formatted instanceof Boolean) {
            return Boolean.valueOf((Boolean)formatted);
        } else {
            return false;
        }
    }

    protected static void unflattenEntry(Map<String, Object> entry) {
        if (entry.get(BEFORE) instanceof String) {
            entry.put(BEFORE, parseJsonString((String)entry.get(BEFORE), Map.class).getObject());
        }
        if (entry.get(AFTER) instanceof String) {
            entry.put(AFTER, parseJsonString((String)entry.get(AFTER), Map.class).getObject());
        }
        if (entry.get(ACCESS_LOG_ROLES) instanceof String) {
            entry.put(ACCESS_LOG_ROLES, parseJsonString((String)entry.get(ACCESS_LOG_ROLES), List.class).getObject());
        }
        if (entry.get(RECON_LOG_MESSAGE_DETAIL) instanceof String) {
            entry.put(RECON_LOG_MESSAGE_DETAIL,
                parseJsonString((String)entry.get(RECON_LOG_MESSAGE_DETAIL), Map.class).getObject());
        }
        if (entry.get(CHANGED_FIELDS) instanceof String) {
            entry.put(CHANGED_FIELDS, parseJsonString((String)entry.get(CHANGED_FIELDS), List.class).getObject());
        }
    }
}
