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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import org.forgerock.audit.AuditServiceConfiguration;
import org.forgerock.audit.DependencyProviderBase;
import org.forgerock.audit.json.AuditJsonConfig;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.audit.AuditService;
import org.forgerock.openidm.audit.impl.AuditLogFilters.JsonValueObjectConverter;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.crypto.factory.CryptoServiceFactory;
import org.forgerock.openidm.patch.JsonPatch;
import org.forgerock.openidm.router.RouteService;
import org.forgerock.openidm.util.ResourceUtil;
import org.forgerock.script.ScriptRegistry;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.forgerock.openidm.audit.impl.AuditLogFilters.AS_SINGLE_FIELD_VALUES_FILTER;
import static org.forgerock.openidm.audit.impl.AuditLogFilters.newActivityActionFilter;
import static org.forgerock.openidm.audit.impl.AuditLogFilters.newAndCompositeFilter;
import static org.forgerock.openidm.audit.impl.AuditLogFilters.newEventTypeFilter;
import static org.forgerock.openidm.audit.impl.AuditLogFilters.newReconActionFilter;
import static org.forgerock.openidm.audit.impl.AuditLogFilters.newOrCompositeFilter;
import static org.forgerock.openidm.audit.impl.AuditLogFilters.newScriptedFilter;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditServiceImpl.class);

    //TODO ADD SUPPORT FOR KNOWN QUERIES
    // Recognized queries
    //public static final String QUERY_BY_RECON_ID = "audit-by-recon-id";
    //public static final String QUERY_BY_MAPPING = "audit-by-mapping";
    //public static final String QUERY_BY_RECON_ID_AND_SITUATION = "audit-by-recon-id-situation";
    //public static final String QUERY_BY_RECON_ID_AND_TYPE = "audit-by-recon-id-type";
    //public static final String QUERY_BY_ACTIVITY_PARENT_ACTION = "audit-by-activity-parent-action";

    // ----- Declarative Service Implementation

    /** The connection factory */
    @Reference(policy = ReferencePolicy.STATIC, target="(service.pid=org.forgerock.openidm.internal)")
    private ConnectionFactory connectionFactory;

    /** Although we may not need the router here,
     https://issues.apache.org/jira/browse/FELIX-3790
     if using this with for scr 1.6.2
     Ensure we do not get bound on router whilst it is activating
     */
    @Reference(target = "("+ServerConstants.ROUTER_PREFIX + "=/*)")
    private RouteService routeService;

    /** Script Registry service. */
    @Reference(policy = ReferencePolicy.STATIC)
    private ScriptRegistry scriptRegistry;

    private org.forgerock.audit.AuditService auditService;
    private JsonValue config; // Existing active configuration

    /** Enhanced configuration service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private EnhancedConfig enhancedConfig;

    /** the script to execute to format exceptions */
    //private static ScriptEntry exceptionFormatterScript = null;

    private AuditLogFilter auditFilter = AuditLogFilters.NEVER;

    private final List<JsonPointer> watchFieldFilters = new ArrayList<>();
    private final List<JsonPointer> passwordFieldFilters = new ArrayList<>();

    private static final String AUDIT_SERVICE_CONFIG = "auditServiceConfig";
    private static final String EVENT_HANDLERS = "eventHandlers";
    private static final String EXTENDED_EVENT_TYPES = "extendedEventTypes";
    private static final String CUSTOM_EVENT_TYPES = "customEventTypes";

    private final AuditLogFilterBuilder auditLogFilterBuilder = new AuditLogFilterBuilder()
            /* filter activity events on configured actions to include */
            .add("extendedEventTypes/activity/filter/actions",
                    new JsonValueObjectConverter<AuditLogFilter>() {
                        @Override
                        public AuditLogFilter apply(JsonValue actions) {
                            return newActivityActionFilter(actions);
                        }
                    })
            /* filter activity events on configured actions to include when a particular trigger context is in scope */
            .add("extendedEventTypes/activity/filter/triggers",
                    new JsonValueObjectConverter<AuditLogFilter>() {
                        @Override
                        public AuditLogFilter apply(JsonValue triggers) {
                            List<AuditLogFilter> filters = new ArrayList<>();
                            for (String trigger : triggers.keys()) {
                                filters.add(newActivityActionFilter(triggers.get(trigger), trigger));
                            }
                            return newOrCompositeFilter(filters);
                        }
                    })
            /* filter recon events on configured actions to include */
            .add("customEventTypes/recon/filter/actions",
                    new JsonValueObjectConverter<AuditLogFilter>() {
                        @Override
                        public AuditLogFilter apply(JsonValue actions) {
                            return newReconActionFilter(actions);
                        }
                    })
            /* filter recon events on configured actions to include when a particular trigger context is in scope */
            .add("customEventTypes/recon/filter/triggers",
                    new JsonValueObjectConverter<AuditLogFilter>() {
                        @Override
                        public AuditLogFilter apply(JsonValue triggers) {
                            List<AuditLogFilter> filters = new ArrayList<>();
                            for (String trigger : triggers.keys()) {
                                filters.add(newReconActionFilter(triggers.get(trigger), trigger));
                            }
                            return newOrCompositeFilter(filters);
                        }
                    })
            /* filter events with specific field values for any event type */
            .add("*/*/filter/fields",
                    new JsonValueObjectConverter<AuditLogFilter>() {
                        @Override
                        public AuditLogFilter apply(JsonValue fieldsConfig) {
                            List<AuditLogFilter> filters = new ArrayList<>();
                            // the glob in the JsonPointer will return a map of matched entry types to field
                            // configurations
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
        LOGGER.debug("Activating Service with configuration {}", compContext.getProperties());
        try {
            // Upon activation the ScriptRegistry is present so we can add script-based audit log filters for event
            // types

            auditLogFilterBuilder.add("extendedEventTypes/*/filter/script",
                    new JsonValueObjectConverter<AuditLogFilter>() {
                        @Override
                        public AuditLogFilter apply(JsonValue scriptConfig) {
                            List<AuditLogFilter> filters = new ArrayList<>();
                            for (String eventType : scriptConfig.keys()) {
                                JsonValue filterConfig = scriptConfig.get(eventType);
                                try {
                                    filters.add(newScriptedFilter(eventType, scriptRegistry.takeScript(filterConfig)));
                                } catch (Exception e) {
                                    LOGGER.error(
                                            "Audit Log Filter builder threw exception {} while processing {} for {}",
                                            e.getClass().getName(), filterConfig.toString(), eventType, e);
                                }
                            }
                            return newOrCompositeFilter(filters);
                        }
                    });
            auditLogFilterBuilder.add("customEventTypes/*/filter/script",
                    new JsonValueObjectConverter<AuditLogFilter>() {
                        @Override
                        public AuditLogFilter apply(JsonValue scriptConfig) {
                            List<AuditLogFilter> filters = new ArrayList<>();
                            for (String eventType : scriptConfig.keys()) {
                                JsonValue filterConfig = scriptConfig.get(eventType);
                                try {
                                    filters.add(newScriptedFilter(eventType, scriptRegistry.takeScript(filterConfig)));
                                } catch (Exception e) {
                                    LOGGER.error(
                                            "Audit Log Filter builder threw exception {} while processing {} for {}",
                                            e.getClass().getName(), filterConfig.toString(), eventType, e);
                                }
                            }
                            return newOrCompositeFilter(filters);
                        }
                    });

            config = enhancedConfig.getConfigurationAsJson(compContext);
            auditFilter = auditLogFilterBuilder.build(config);

            //create Audit Service
            auditService =
                    new org.forgerock.audit.AuditService(
                            config.get(EXTENDED_EVENT_TYPES), config.get(CUSTOM_EVENT_TYPES));
            AuditServiceConfiguration serviceConfig =
                    AuditJsonConfig.parseAuditServiceConfiguration(config.get(AUDIT_SERVICE_CONFIG));
            auditService.configure(serviceConfig);
            auditService.registerDependencyProvider(new DependencyProviderBase() {
                @SuppressWarnings("unchecked")
                @Override
                public <T> T getDependency(Class<T> clazz) throws ClassNotFoundException {
                    if (ConnectionFactory.class.isAssignableFrom(clazz)) {
                        return (T) connectionFactory;
                    } else {
                        return super.getDependency(clazz);
                    }
                }
            });

            //register Event Handlers
            final JsonValue eventHandlers = config.get(EVENT_HANDLERS);
            for (final JsonValue handlerConfig : eventHandlers) {
                AuditJsonConfig.registerHandlerToService(handlerConfig, auditService, this.getClass().getClassLoader());
            }
        } catch (Exception ex) {
            LOGGER.warn("Configuration invalid, can not start Audit service.", ex);
            throw ex;
        }
        LOGGER.info("Audit service started.");
    }

    /**
     * Configuration modified handling
     * Ensures audit logging service stays registered
     * even whilst configuration changes
     */
    @Modified
    void modified(ComponentContext compContext) throws Exception {
        LOGGER.debug("Reconfiguring audit service with configuration {}", compContext.getProperties());
        try {
            JsonValue newConfig = enhancedConfig.getConfigurationAsJson(compContext);
            if (hasConfigChanged(config, newConfig)) {
                deactivate(compContext);
                activate(compContext);
                LOGGER.info("Reconfigured audit service {}", compContext.getProperties());
            }
        } catch (Exception ex) {
            LOGGER.warn("Configuration invalid, can not reconfigure Audit service.", ex);
            throw ex;
        }
    }

    private boolean hasConfigChanged(JsonValue existingConfig, JsonValue newConfig) {
        return JsonPatch.diff(existingConfig, newConfig).size() > 0;
    }

    @Deactivate
    void deactivate(ComponentContext compContext) {
        LOGGER.debug("Deactivating Service {}", compContext.getProperties());
        auditService = null;
        config = null;
        auditFilter = AuditLogFilters.NEVER;
        watchFieldFilters.clear();
        passwordFieldFilters.clear();
        LOGGER.info("Audit service stopped.");
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
    public void handleRead(final ServerContext context, final ReadRequest request,
                           final ResultHandler<Resource> handler) {
            LOGGER.debug("Audit read called for {}", request.getResourceName());
            auditService.handleRead(context, request, handler);
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
        if (request.getResourceName() == null) {
            //TODO IGNORE FAILURE PER AUDIT LOGGER?
            handler.handleError(
                    new BadRequestException(
                            "Audit service called without specifying which audit log in the identifier"));
            return;
        }

        Map<String, Object> obj = request.getContent().asMap();

        // TODO pretty sure this is in CAUD now
        // Don't audit the audit log
        if (context.containsContext(AuditContext.class)) {
            handler.handleResult(new Resource(null, null, new JsonValue(obj)));
            return;
        }

        // Audit create called for /access with {timestamp=2013-07-30T18:10:03.773Z,
        // principal=openidm-admin, status=SUCCESS, roles=[openidm-admin, openidm-authorized], action=authenticate,
        // userid=openidm-admin, ip=127.0.0.1}
        LOGGER.debug("Audit create called for {} with {}", request.getResourceName(), obj);

        String type = request.getResourceNameObject().head(1).toString();

        if (auditFilter.isFiltered(context, request)) {
            LOGGER.debug("Request filtered by filter for {}/{} using method {}",
                    request.getResourceName(),
                    request.getNewResourceId(),
                    request.getContent().get(new JsonPointer("resourceOperation/operation/method")));
            handler.handleResult(new Resource(null, null, new JsonValue(obj)));
            return;
        }

        try {
            auditService.handleCreate(context, request, handler);
        } catch (RuntimeException ex) {
            LOGGER.warn("Failure writing audit log: {}/ with exception: {}", new Object[]{type, ex});
            //TODO IGNORE FAILURE PER AUDIT LOGGER?
        }
    }

    /**
     * Audit service does not support changing audit entries.
     */
    @Override
    public void handleUpdate(final ServerContext context, final UpdateRequest request,
            final ResultHandler<Resource> handler) {
        auditService.handleUpdate(context, request, handler);
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
        auditService.handleDelete(context, request, handler);
    }

    /**
     * Audit service does not support changing audit entries.
     *
     * {@inheritDoc}
     */
    @Override
    public void handlePatch(final ServerContext context, final PatchRequest request,
            final ResultHandler<Resource> handler) {
        auditService.handlePatch(context, request, handler);
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
        LOGGER.debug("Audit query called for {} with {}", request.getResourceName(), request.getAdditionalParameters());

        //TODO DO WE CARE ABOUT THE FORMATTED KEYWORD
        //final boolean formatted = getFormattedValue(request.getAdditionalParameter("formatted"));
        auditService.handleQuery(context, request, handler);
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
     * Checks to see if there are differences between the values in two JsonValues before and after
     * Returns a list containing the changed fields
     *
     * @param fieldsToCheck list of JsonPointers to search for
     * @param before prior JsonValue
     * @param after JsonValue after applied changes
     * @return list of strings indicating which values changed
     */
    private List<String> checkForFields(List<JsonPointer> fieldsToCheck,  JsonValue before, JsonValue after) {
        List<String> changedFields = new ArrayList<>();
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
    private List<JsonPointer> getEventJsonPointerList(JsonValue config, String event, String fieldName) {
        ArrayList<JsonPointer> fieldList = new ArrayList<>();
        JsonValue fields = config.get(EXTENDED_EVENT_TYPES).get(event).get(fieldName);
        for (JsonValue field : fields) {
            fieldList.add(field.asPointer());
        }
        return fieldList;
    }
}
