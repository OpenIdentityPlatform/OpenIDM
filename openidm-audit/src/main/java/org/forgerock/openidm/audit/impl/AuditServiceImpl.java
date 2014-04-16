/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All rights reserved.
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
import java.util.EnumSet;
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
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.patch.JsonPatch;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestType;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.audit.AuditService;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.config.enhanced.InvalidException;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.crypto.factory.CryptoServiceFactory;
import org.forgerock.openidm.router.RouteService;
import org.forgerock.openidm.sync.TriggerContext;
import org.forgerock.openidm.util.DateUtil;
import org.forgerock.openidm.util.JsonUtil;
import org.forgerock.openidm.util.ResourceUtil;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * Audit module
 * @author aegloff
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

    /** Script Registry service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected ScriptRegistry scriptRegistry;

    private void bindScriptRegistry(final ScriptRegistry service) {
        scriptRegistry = service;
    }

    private void unbindScriptRegistry(final ScriptRegistry service) {
        scriptRegistry = null;
    }

    private static ScriptEntry script = null;

    EnhancedConfig enhancedConfig = new JSONEnhancedConfig();

    Map<String, Set<RequestType>> actionFilters;
    Map<String, Map<String, Set<RequestType>>> triggerFilters;
    List<JsonPointer> watchFieldFilters;
    List<JsonPointer> passwordFieldFilters;

    List<AuditLogger> globalAuditLoggers;
    Map<String,List<AuditLogger>> eventAuditLoggers;
    JsonValue config; // Existing active configuration
    DateUtil dateUtil;

    static {
        JsonFactory jsonFactory = new JsonFactory();
        jsonFactory.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true);
        mapper = new ObjectMapper(jsonFactory);
    }

    @Reference(policy = ReferencePolicy.STATIC, target="(service.pid=org.forgerock.openidm.internal)")
    protected ConnectionFactory connectionFactory;

    /** Although we may not need the router here,
         https://issues.apache.org/jira/browse/FELIX-3790
        if using this with for scr 1.6.2
        Ensure we do not get bound on router whilst it is activating
    */
    // ----- Declarative Service Implementation

    @Reference(target = "("+ServerConstants.ROUTER_PREFIX + "=/*)")
    RouteService routeService;

    private void bindRouteService(final RouteService service) throws ResourceException {
        routeService = service;
    }

    private void unbindRouteService(final RouteService service) {
        routeService = null;
    }


    /**
     * Gets an object from the audit logs by identifier. The returned object is not validated
     * against the current schema and may need processing to conform to an updated schema.
     * <p>
     * The object will contain metadata properties, including object identifier {@code _id},
     * and object version {@code _rev} to enable optimistic concurrency
     *
     * @throws NotFoundException if the specified object could not be found.
     * @throws ForbiddenException if access to the object is forbidden.
     * @throws BadRequestException if the passed identifier is invalid
     * @return the requested object.
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
            t.printStackTrace();
            handler.handleError(ResourceUtil.adapt(t));
        }
    }

    /**
     * Creates a new object in the object set.
     * <p>
     * This method sets the {@code _id} property to the assigned identifier for the object,
     * and the {@code _rev} property to the revised object version (For optimistic concurrency)
     *
     * @param obj the contents of the object to create in the object set.
     * @throws NotFoundException if the specified id could not be resolved.
     * @throws ForbiddenException if access to the object or object set is forbidden.
     * @throws PreconditionFailedException if an object with the same ID already exists.
     */
    @Override
    public void handleCreate(final ServerContext context, final CreateRequest request,
            final ResultHandler<Resource> handler) {
        try {
            if (request.getResourceName() == null) {
                throw new BadRequestException("Audit service called without specifying which audit log in the identifier");
            }

            Map<String, Object> obj = request.getContent().asMap();

            // Audit create called for /access with {timestamp=2013-07-30T18:10:03.773Z, principal=openidm-admin, status=SUCCESS, roles=[openidm-admin, openidm-authorized], action=authenticate, userid=openidm-admin, ip=127.0.0.1}  
            logger.debug("Audit create called for {} with {}", request.getResourceName(), obj);

            String type = request.getResourceNameObject().head(1).toString();
            String trigger = getTrigger(context);
            JsonValue action = request.getContent().get("action");

            // Filter
            Set<RequestType> actionFilter = actionFilters.get(type);
            Map<String, Set<RequestType>> triggerFilter = triggerFilters.get(type);

            if (triggerFilter != null && trigger != null) {
                try {
                    Set<RequestType> triggerActions = triggerFilter.get(trigger);
                    if (triggerActions == null) {
                        logger.debug("Trigger filter not set for " + trigger + ", allowing all actions");
                    } else if (!triggerActions.contains(action.asEnum(RequestType.class))) {
                        logger.debug("Filtered by trigger filter for action {}", new Object[] { action.toString() });
                        handler.handleResult(new Resource(null, null, new JsonValue(obj)));
                        return;
                    }
                } catch (JsonValueException e) {
                    // note this is permissive on an unknown trigger filter action; 
                    // i.e., an action of "money" will not be filtered because it is not a valid RequestType
                    logger.debug("Action {} is not one of supported action types, not filtering", new Object[] { action.toString(), e });
                }
            }

            if (actionFilter != null) {
                // TODO: make filters that can operate on a variety of conditions
                try {
                    if (!actionFilter.contains(action.asEnum(RequestType.class))) {
                        logger.debug("Filtered by action filter for action {}", new Object[] { action.toString() } );
                        handler.handleResult(new Resource(null, null, new JsonValue(obj)));
                        return;
                    }
                } catch (JsonValueException e) {
                    // note this is permissive on an unknown filter action; 
                    // i.e., an action of "money" will not be filtered because it is not a valid RequestType
                    logger.debug("Action {} is not one of supported action types, not filtering", new Object[] { action.toString(), e });
                }
            }

            // Activity log preprocessing
            if (type.equals("activity")) {
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
        } catch (Throwable t){
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
     * Searches ObjectSetContext for the value of "trigger" and return it.
     */
    private String getTrigger(Context context) {
        /*
        String trigger = null;
        // Loop through parent contexts, and return highest "trigger"
        while (context != null) {
            JsonValue tmp = (JsonValue) context.getParams().get("trigger");
            if (!tmp.isNull()) {
                trigger = tmp.asString();
            }
            context = context.getParent();
        }
        */
        return context.containsContext(TriggerContext.class)
                ? context.asContext(TriggerContext.class).getTrigger()
                : null;
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
     * @param rev the version of the object to delete or {@code null} if not provided.
     * @throws NotFoundException if the specified object could not be found.
     * @throws ForbiddenException if access to the object is forbidden.
     * @throws ConflictException if version is required but is {@code null}.
     * @throws PreconditionFailedException if version did not match the existing object in the set.
     */
    @Override
    public void handleDelete(ServerContext context, DeleteRequest request,
            ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupported(request));
    }

    /**
     * Audit service does not support changing audit entries.
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
     * - The top level map contains meta-data about the query, plus an entry with the actual result records.
     * - The <code>QueryConstants</code> defines the map keys, including the result records (QUERY_RESULT)
     *
     * @param params the parameters of the query to perform.
     * @return the query results, which includes meta-data and the result records in JSON object structure format.
     * @throws NotFoundException if the specified object could not be found.
     * @throws BadRequestException if the specified params contain invalid arguments, e.g. a query id that is not
     * configured, a query expression that is invalid, or missing query substitution tokens.
     * @throws ForbiddenException if access to the object or specified query is forbidden.
     */
    @Override
    public void handleQuery(final ServerContext context, final QueryRequest request,
            final QueryResultHandler handler) {
        try {
            final String type = request.getResourceNameObject().head(1).toString();
            Map<String,String> params = new HashMap<String,String>();
            params.putAll(request.getAdditionalParameters());
            params.put("_queryId", request.getQueryId());
            logger.debug("Audit query called for {} with {}", request.getResourceName(), request.getAdditionalParameters());
            AuditLogger auditLogger = getQueryAuditLogger(type);
            Map<String, Object> result = auditLogger.query(context, type, params);

            for (Map<String,Object> o: (Iterable<Map<String,Object>>) result.get("result")) {
                String id = (String) o.get(Resource.FIELD_CONTENT_ID);
                handler.handleResource(new Resource(id, null, new JsonValue(o)));
            }
            handler.handleResult(new QueryResult());
        } catch (Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
    }
    /**
     * Audit service does not support actions on audit entries.
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
     * @return the confiugred AuditLoggers for this event type
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

    @Activate
    void activate(ComponentContext compContext) throws Exception {
        logger.debug("Activating Service with configuration {}", compContext.getProperties());
        try {
            // TODO make dateUtil configurable (needs to be done in all locations)
            dateUtil = DateUtil.getDateUtil("UTC");
            setConfig(compContext);
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
        logger.debug("Reconfiguring aduit service with configuration {}", compContext.getProperties());
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

    private void setConfig(ComponentContext compContext) throws Exception {
        config = enhancedConfig.getConfigurationAsJson(compContext);
        globalAuditLoggers = getGlobalAuditLoggers(config, compContext);
        eventAuditLoggers = getEventAuditLoggers(config, compContext);
        actionFilters = getActionFilters(config);
        triggerFilters = getTriggerFilters(config);
        watchFieldFilters =  getEventJsonPointerList(config, "activity", "watchedFields");
        passwordFieldFilters = getEventJsonPointerList(config, "activity", "passwordFields");
        JsonValue efConfig = config.get("exceptionFormatter");
        if (!efConfig.isNull()) {
            script =  scriptRegistry.takeScript(efConfig);
        }

        logger.debug("Audit service filters enabled: {}", actionFilters);
    }

    Map<String, Set<RequestType>> getActionFilters(JsonValue config) {
        Map<String, Set<RequestType>> configFilters = new HashMap<String, Set<RequestType>>();

        Map<String, Object> eventTypes = config.get("eventTypes").asMap();
        if (eventTypes == null) {
            return configFilters;
        }
        for (Map.Entry<String, Object> eventType : eventTypes.entrySet()) {
            String eventTypeName = eventType.getKey();
            JsonValue eventTypeValue = new JsonValue(eventType.getValue());
            JsonValue filterActions = eventTypeValue.get("filter").get("actions");
            // TODO: proper filter mechanism
            if (!filterActions.isNull()) {
                Set<RequestType> filter = EnumSet.noneOf(RequestType.class);
                for (JsonValue action : filterActions) {
                    try {
                        filter.add(action.asEnum(RequestType.class));
                    } catch (JsonValueException e) {
                        logger.warn("Action value {} is not a known filter action", new Object[] { action.toString() });
                    }
                }
                configFilters.put(eventTypeName, filter);
            }
        }
        return configFilters;
    }

    Map<String, Map<String, Set<RequestType>>> getTriggerFilters(JsonValue config) {
        Map<String, Map<String, Set<RequestType>>> configFilters = new HashMap<String, Map<String, Set<RequestType>>>();

        JsonValue eventTypes = config.get("eventTypes");
        if(!eventTypes.isNull()) {
            Set<String> eventTypesKeys = eventTypes.keys();
            // Loop through event types ("activity", "recon", etc..)
            for (String eventTypeKey : eventTypesKeys) {
                JsonValue eventType = eventTypes.get(eventTypeKey);
                JsonValue filterTriggers = eventType.get("filter").get("triggers");
                if (!filterTriggers.isNull()) {
                    // Create map of the trigger's actions
                    Map<String, Set<RequestType>> filter = new HashMap<String, Set<RequestType>>();
                    Set<String> keys = filterTriggers.keys();
                    // Loop through individual triggers (that each contain a list of actions)
                    for (String key : keys) {
                        JsonValue trigger = filterTriggers.get(key);
                        // Create a empty set of actions for this trigger
                        Set<RequestType> triggerActions = EnumSet.noneOf(RequestType.class);
                        // Loop through the trigger's actions
                        for (JsonValue triggerAction : trigger) {
                            // Add action to list
                            try {
                                triggerActions.add(triggerAction.asEnum(RequestType.class));
                            } catch (JsonValueException e) {
                                logger.warn("Action value {} is not a known filter action", new Object[] { triggerAction.toString() });
                            }
                        }
                        // Add list of actions to map of trigger's actions
                        filter.put(key, triggerActions);
                    }
                    // add filter to map of filters
                    configFilters.put(eventTypeKey, filter);
                }
            }
        }

        return configFilters;
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

    Map<String, List<AuditLogger>> getEventAuditLoggers(JsonValue config, ComponentContext compContext) {
        Map<String, List<AuditLogger>> configuredLoggers = new HashMap<String, List<AuditLogger>>();

        JsonValue eventTypes = config.get("eventTypes");
        if (!eventTypes.isNull()) {
            Set<String> eventTypesKeys = eventTypes.keys();
            // Loop through event types ("activity", "recon", etc..)
            for (String eventTypeKey : eventTypesKeys) {
                JsonValue eventType = eventTypes.get(eventTypeKey);
                configuredLoggers.put(eventTypeKey, getAuditLoggers(eventType.get(CONFIG_LOG_TO).asList(), compContext));
            }
        }

        return configuredLoggers;
    }

    List<AuditLogger> getGlobalAuditLoggers(JsonValue config, ComponentContext compContext) {
        return getAuditLoggers(config.get(CONFIG_LOG_TO).asList(), compContext);
    }

    List<AuditLogger> getAuditLoggers(List logTo, ComponentContext compContext) {
        List<AuditLogger> configuredLoggers = new ArrayList<AuditLogger>();
        if (logTo != null) {
            for (Map entry : (List<Map>)logTo) {
                String logType = (String) entry.get(CONFIG_LOG_TYPE);
                // TDDO: make pluggable
                AuditLogger auditLogger = null;
                if (CONFIG_LOG_TYPE_CSV.equalsIgnoreCase(logType)) {
                    auditLogger = new CSVAuditLogger();
                } else if (CONFIG_LOG_TYPE_REPO.equalsIgnoreCase(logType)) {
                    auditLogger = new RepoAuditLogger(connectionFactory);
                } else if (CONFIG_LOG_TYPE_ROUTER.equalsIgnoreCase(logType)) {
                    auditLogger = new RouterAuditLogger(connectionFactory);
                } else {
                    throw new InvalidException("Configured audit logType is unknown: " + logType);
                }
                if (auditLogger != null) {
                    auditLogger.setConfig(entry, compContext.getBundleContext());
                    logger.info("Audit configured to log to {}", logType);
                    configuredLoggers.add(auditLogger);
                    if (auditLogger.isUsedForQueries()) {
                        logger.info("Audit logger used for queries set to " + logType);
                    }
                }
            }
        }
        return configuredLoggers;
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
        logger.info("Audit service stopped.");
    }

    public static void preformatLogEntry(String type, Map<String, Object> entryMap) {
        if (type.equals(AuditServiceImpl.TYPE_RECON)) {
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
        if (script != null) {
            Script s = script.getScript(new RootContext());
            s.put("exception", e);
            result = (String) s.eval();
        }
        return result;
    }

    public static Map<String, Object> formatLogEntry(Map<String, Object> entry, String type) {
        if (AuditServiceImpl.TYPE_RECON.equals(type)) {
            return AuditServiceImpl.formatReconEntry(entry);
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
    private static Map<String,Object> formatAccessEntry(Map<String,Object> entry) {
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
    private static Map<String,Object> formatActivityEntry(Map<String,Object> entry) {
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
        formattedEntry.put(RECON_LOG_ROOT_ACTION_ID, entry.get(RECON_LOG_ROOT_ACTION_ID));
        formattedEntry.put(RECON_LOG_STATUS, entry.get(RECON_LOG_STATUS));
        formattedEntry.put(RECON_LOG_MESSAGE, entry.get(RECON_LOG_MESSAGE));
        formattedEntry.put(RECON_LOG_MESSAGE_DETAIL, entry.get(RECON_LOG_MESSAGE_DETAIL));
        formattedEntry.put(RECON_LOG_EXCEPTION, entry.get(RECON_LOG_EXCEPTION));
        if ("".equals(entry.get(RECON_LOG_ENTRY_TYPE)) || null == entry.get(RECON_LOG_ENTRY_TYPE)) {
            // recon entry
            formattedEntry.put(RECON_LOG_ACTION_ID, entry.get(RECON_LOG_ACTION_ID));
            formattedEntry.put(RECON_LOG_ACTION, entry.get(RECON_LOG_ACTION));
            formattedEntry.put(RECON_LOG_AMBIGUOUS_TARGET_OBJECT_IDS, entry.get(RECON_LOG_AMBIGUOUS_TARGET_OBJECT_IDS));
            formattedEntry.put(RECON_LOG_RECONCILING, entry.get(RECON_LOG_RECONCILING));
            formattedEntry.put(RECON_LOG_SITUATION, entry.get(RECON_LOG_SITUATION));
            formattedEntry.put(RECON_LOG_SOURCE_OBJECT_ID, entry.get(RECON_LOG_SOURCE_OBJECT_ID));
            formattedEntry.put(RECON_LOG_TARGET_OBJECT_ID, entry.get(RECON_LOG_TARGET_OBJECT_ID));
        } else {
            formattedEntry.put("mapping", entry.get("mapping"));
        }
        return formattedEntry;
    }

    public static Map<String, Object> getReconResults(List<Map<String, Object>> entryList, boolean formatted) {
        Map<String, Object> results = new HashMap<String, Object>();
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

    public static Map<String, Object> getActivityResults(List<Map<String, Object>> entryList, boolean formatted) {
        return getResults(entryList, formatted, AuditServiceImpl.TYPE_ACTIVITY);
    }

    public static Map<String, Object> getAccessResults(List<Map<String, Object>> entryList, boolean formatted) {
        return getResults(entryList, formatted, AuditServiceImpl.TYPE_ACCESS);
    }
    
    private static Map<String, Object> getResults(List<Map<String, Object>> entryList, boolean formatted, String type) {
        Map<String, Object> results = new LinkedHashMap<String, Object>();
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

    protected static JsonValue parseJsonString(String stringified) {
        JsonValue jsonValue = null;
        try {
            Map parsedValue = mapper.readValue(stringified, Map.class);
            jsonValue = new JsonValue(parsedValue);
        } catch (IOException ex) {
            throw new JsonException("String passed into parsing is not valid JSON", ex);
        }
        return jsonValue;
    }

    protected static boolean getBoolValue(Object bool) {
        if (bool instanceof String) {
            return Boolean.valueOf((String)bool);
        } else if (bool instanceof Boolean) {
            return Boolean.valueOf((Boolean)bool);
        } else {
            return false;
        }
    }
}
