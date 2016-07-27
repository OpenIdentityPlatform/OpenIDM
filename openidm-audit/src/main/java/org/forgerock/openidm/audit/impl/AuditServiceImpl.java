/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2016 ForgeRock AS.
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

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.JsonValueFunctions.listOf;
import static org.forgerock.json.resource.Responses.*;
import static org.forgerock.openidm.audit.impl.AuditLogFilters.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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
import org.forgerock.audit.AuditException;
import org.forgerock.audit.AuditServiceBuilder;
import org.forgerock.audit.AuditServiceConfiguration;
import org.forgerock.audit.AuditServiceProxy;
import org.forgerock.audit.AuditingContext;
import org.forgerock.audit.DependencyProvider;
import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.audit.events.EventTopicsMetaDataBuilder;
import org.forgerock.audit.events.handlers.EventHandlerConfiguration;
import org.forgerock.audit.json.AuditJsonConfig;
import org.forgerock.audit.providers.DefaultKeyStoreHandlerProvider;
import org.forgerock.audit.providers.KeyStoreHandlerProvider;
import org.forgerock.audit.secure.JcaKeyStoreHandler;
import org.forgerock.audit.secure.KeyStoreHandler;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.audit.AuditService;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.crypto.factory.CryptoServiceFactory;
import org.forgerock.openidm.crypto.util.JettyPropertyUtil;
import org.forgerock.openidm.router.IDMConnectionFactory;
import org.forgerock.openidm.router.RouteService;
import org.forgerock.openidm.util.JsonUtil;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.annotations.VisibleForTesting;
import org.forgerock.util.promise.Promise;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public static final String EXCEPTION_FORMATTER = "exceptionFormatter";
    public static final String EXCEPTION = "exception";
    private static final String OPENIDM_KEYSTORE_NAME = "openidm";
    private static final String OPENIDM_KEYSTORE_TYPE = "openidm.keystore.type";
    private static final String OPENIDM_KEYSTORE_LOCATION = "openidm.keystore.location";
    private static final String OPENIDM_KEYSTORE_PASSWORD = "openidm.keystore.password";

    // ----- Declarative Service Implementation

    /** The connection factory */
    @Reference(policy = ReferencePolicy.STATIC)
    private IDMConnectionFactory connectionFactory;

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

    private AuditServiceProxy auditService;
    private JsonValue config; // Existing active configuration

    /** Enhanced configuration service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile EnhancedConfig enhancedConfig;

    /** the script to execute to format exceptions */
    private static ScriptEntry exceptionFormatterScript = null;

    private AuditLogFilter auditFilter = NEVER_FILTER;

    private List<JsonPointer> watchFieldFilters = new ArrayList<>();
    private List<JsonPointer> passwordFieldFilters = new ArrayList<>();

    private static final String AUDIT_SERVICE_CONFIG = "auditServiceConfig";
    private static final String EVENT_HANDLERS = "eventHandlers";
    private static final String EVENT_TOPICS = "eventTopics";
    private static final String EXTENDED_TOPICS = "extendedTopics";
    private static final String CUSTOM_TOPICS  = "customTopics";
    private static final JsonPointer WATCHED_PASSWORDS_CONFIG_POINTER = new JsonPointer(
            EVENT_TOPICS + "/activity/passwordFields");
    private static final JsonPointer WATCHED_FIELDS_CONFIG_POINTER = new JsonPointer(
            EVENT_TOPICS + "/activity/watchedFields");

    private KeyStoreHandlerProvider keyStoreHandlerProvider;

    private final JsonValueObjectConverter<AuditLogFilter> fieldJsonValueObjectConverter =
            new JsonValueObjectConverter<AuditLogFilter>() {
        @Override
        public AuditLogFilter apply(JsonValue fieldsConfig) {
            List<AuditLogFilter> filters = new ArrayList<>();
            // the glob in the JsonPointer will return a map of matched entry types to field
            // configurations
            for (String eventType : fieldsConfig.keys()) {
                // fieldConfig is something like { "field" : "type", "values" : [
                // "summary" ] }
                JsonValue fieldConfig = fieldsConfig.get(eventType);
                filters.add(newEventTypeFilter(eventType,
                        newAndCompositeFilter(fieldConfig.as(listOf(AS_SINGLE_FIELD_VALUES_FILTER)))));
            }
            return newOrCompositeFilter(filters);
        }
    };

    private final AuditLogFilterBuilder auditLogFilterBuilder = new AuditLogFilterBuilder()
            /* filter config events on configured actions to include */
            .add("eventTopics/config/filter/actions",
                    new JsonValueObjectConverter<AuditLogFilter>() {
                        @Override
                        public AuditLogFilter apply(JsonValue actions) {
                            return newActionFilter(TYPE_CONFIG, actions);
                        }
                    })
            /* filter activity events on configured actions to include */
            .add("eventTopics/activity/filter/actions",
                    new JsonValueObjectConverter<AuditLogFilter>() {
                        @Override
                        public AuditLogFilter apply(JsonValue actions) {
                            return newActionFilter(TYPE_ACTIVITY, actions);
                        }
                    })
            /* filter activity events on configured actions to include when a particular trigger context is in scope */
            .add("eventTopics/activity/filter/triggers",
                    new JsonValueObjectConverter<AuditLogFilter>() {
                        @Override
                        public AuditLogFilter apply(JsonValue triggers) {
                            List<AuditLogFilter> filters = new ArrayList<>();
                            for (String trigger : triggers.keys()) {
                                filters.add(newActionFilter(TYPE_ACTIVITY, triggers.get(trigger), trigger));
                            }
                            return newOrCompositeFilter(filters);
                        }
                    })
            /* filter recon events on configured actions to include */
            .add("eventTopics/recon/filter/actions",
                    new JsonValueObjectConverter<AuditLogFilter>() {
                        @Override
                        public AuditLogFilter apply(JsonValue actions) {
                            return newReconActionFilter(actions);
                        }
                    })
            /* filter recon events on configured actions to include when a particular trigger context is in scope */
            .add("eventTopics/recon/filter/triggers",
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
            .add("eventTopics/*/filter/fields", fieldJsonValueObjectConverter);

    private enum DefaultAuditTopics {
        access,
        authentication,
        activity,
        config,
        sync,
        recon,
    }

    /**
     * A map of cached audit event handlers with their default configurations. These handlers should only be used to
     * call their useForQueriesMethod.
     */
    private final Map<String, EventHandlerConfiguration> dummyAuditEventHandlers = new LinkedHashMap<>();

    @Activate
    void activate(ComponentContext compContext) throws Exception {
        logger.debug("Activating Service with configuration {}", compContext.getProperties());
        try {
            // Upon activation the ScriptRegistry is present so we can add script-based audit log filters for event
            // types

            auditLogFilterBuilder.add("eventTopics/*/filter/script",
                    new JsonValueObjectConverter<AuditLogFilter>() {
                        @Override
                        public AuditLogFilter apply(JsonValue scriptConfig) {
                            List<AuditLogFilter> filters = new ArrayList<>();
                            for (String eventType : scriptConfig.keys()) {
                                JsonValue filterConfig = scriptConfig.get(eventType);
                                try {
                                    filters.add(newScriptedFilter(eventType, scriptRegistry.takeScript(filterConfig)));
                                } catch (Exception e) {
                                    logger.error(
                                            "Audit Log Filter builder threw exception {} while processing {} for {}",
                                            e.getClass().getName(), filterConfig.toString(), eventType, e);
                                }
                            }
                            return newOrCompositeFilter(filters);
                        }
                    });

            config = enhancedConfig.getConfigurationAsJson(compContext);
            auditFilter = auditLogFilterBuilder.build(config);
            keyStoreHandlerProvider = createKeyStoreHandlerProvider();

            final DependencyProvider dependencyProvider = new DependencyProvider() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public <T> T getDependency(Class<T> clazz) throws ClassNotFoundException {
                        if (ConnectionFactory.class.isAssignableFrom(clazz)) {
                            return (T) connectionFactory;
                        } else if (KeyStoreHandlerProvider.class.isAssignableFrom(clazz)) {
                            return (T) keyStoreHandlerProvider;
                        } else {
                            throw new ClassNotFoundException("No instance registered for class: " + clazz.getName());
                        }
                    }
            };
            final JsonValue topics = AuditJsonConfig.getJson(getClass().getResourceAsStream("/auditTopics.json"));
            final AuditServiceConfiguration serviceConfig =
                    AuditJsonConfig.parseAuditServiceConfiguration(config.get(AUDIT_SERVICE_CONFIG));
            final EventTopicsMetaData eventTopicsMetaData = EventTopicsMetaDataBuilder
                    .coreTopicSchemas()
                    .withCoreTopicSchemaExtensions(topics.get(EXTENDED_TOPICS))
                    .withAdditionalTopicSchemas(
                            getCustomTopics(topics.get(CUSTOM_TOPICS).copy(), config.get(EVENT_TOPICS).copy())).build();
            final AuditServiceBuilder auditServiceBuilder = AuditServiceBuilder.newAuditService()
                    .withConfiguration(serviceConfig)
                    .withEventTopicsMetaData(eventTopicsMetaData)
                    .withDependencyProvider(dependencyProvider);

            //register Event Handlers
            final JsonValue eventHandlers = config.get(EVENT_HANDLERS);
            for (final JsonValue handlerConfig : eventHandlers) {
                AuditJsonConfig.registerHandlerToService(
                        handlerConfig, auditServiceBuilder, this.getClass().getClassLoader());
            }

            if (!config.get(EXCEPTION_FORMATTER).isNull()) {
                exceptionFormatterScript =  scriptRegistry.takeScript(config.get(EXCEPTION_FORMATTER));
            }

            JsonValue watchedFieldsValue = config.get(WATCHED_FIELDS_CONFIG_POINTER);
            if (null != watchedFieldsValue) {
                watchFieldFilters = getJsonPointers(watchedFieldsValue.asList(String.class));
            }

            JsonValue passwordFieldsValue = config.get(WATCHED_PASSWORDS_CONFIG_POINTER);
            if (null != passwordFieldsValue) {
                passwordFieldFilters = getJsonPointers(passwordFieldsValue.asList(String.class));
            }

            // create the audit service
            if (auditService == null) {
                // first time initialize the audit service proxy
                auditService = new AuditServiceProxy(auditServiceBuilder.build());
                auditService.startup();
            } else {
                // all times after the first reset the delegate.
                auditService.setDelegate(auditServiceBuilder.build());
            }

            createDummyAuditEventHandlers(dummyAuditEventHandlers);

        } catch (Exception ex) {
            logger.warn("Configuration invalid, can not start Audit service.", ex);
            throw ex;
        }
        logger.info("Audit service started.");
    }

    /**
     * Converts the list of strings into a list of JsonPointers
     * @param pointerStrings strings to get converted.
     * @return converted list of pointers.
     */
    private List<JsonPointer> getJsonPointers(List<String> pointerStrings) {
        List<JsonPointer> pointers = new ArrayList<>();
        for (String pointerString : pointerStrings) {
            pointers.add(new JsonPointer(pointerString));
        }
        return pointers;
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
                // don't call deactivate since the AuditServiceProxy in the activate will call shutdown on the old audit
                // service
                cleanup();
                activate(compContext);
                logger.info("Reconfigured audit service {}", compContext.getProperties());
            }
        } catch (Exception ex) {
            logger.warn("Configuration invalid, can not reconfigure Audit service.", ex);
            throw ex;
        }
    }

    private boolean hasConfigChanged(JsonValue existingConfig, JsonValue newConfig) {
        return !existingConfig.isEqualTo(newConfig);
    }

    @Deactivate
    void deactivate(ComponentContext compContext) {
        logger.debug("Deactivating Service {}", compContext.getProperties());
        cleanup();
        auditService.shutdown();
        logger.info("Audit service stopped.");
    }

    private void cleanup() {
        config = null;
        auditFilter = NEVER_FILTER;
        watchFieldFilters.clear();
        passwordFieldFilters.clear();
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
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        logger.debug("Audit read called for {}", request.getResourcePath());
        return auditService.handleRead(context, request);
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
    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
        if (request.getResourcePath() == null) {
            //TODO IGNORE FAILURE PER AUDIT LOGGER?
            return new BadRequestException("Audit service called without specifying which audit log in the identifier")
                .asPromise();
        }

        try {
            formatException(request.getContent());
        } catch (Exception e) {
            logger.error("Failed to format audit entry exception", e);
            return new InternalServerErrorException("Failed to format audit entry exception", e)
                .asPromise();
        }

        // Don't audit the audit log
        if (context.containsContext(AuditingContext.class)) {
            return newResourceResponse(null, null, request.getContent()).asPromise();
        }

        logger.debug("Audit create called for {} with {}", request.getResourcePath(), request.getContent().asMap());

        if (auditFilter.isFiltered(context, request)) {
            logger.debug("Request filtered by filter for {}/{} using method {}",
                    request.getResourcePath(),
                    request.getNewResourceId(),
                    request.getContent().get(new JsonPointer("resourceOperation/operation/method")));
            return newResourceResponse(null, null, request.getContent()).asPromise();
        }

        return auditService.handleCreate(context, request);

    }

    /**
     * Audit service does not support changing audit entries.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
        return auditService.handleUpdate(context, request);
    }

    /**
     * Audit service currently does not support deleting audit entries.
     *
     * Deletes the specified object from the object set.
     *
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
        return auditService.handleDelete(context, request);
    }

    /**
     * Audit service does not support changing audit entries.
     *
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
        return auditService.handlePatch(context, request);
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
    public Promise<QueryResponse, ResourceException> handleQuery(final Context context, final QueryRequest request,
            final QueryResourceHandler handler) {

        logger.debug("Audit query called for {} with {}", request.getResourcePath(), request.getAdditionalParameters());

        return auditService.handleQuery(context, request, handler);
    }

    /**
     * Audit service action handles the actions defined in #ActivityAction.
     *
     * {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {

        logger.debug("Audit handleAction called with action={}", request.getAction());

        AuditAction requestAction = null;
        try {
            requestAction = request.getActionAsEnum(AuditAction.class);
        } catch (Exception e) {
            // this will leave requestAction as null.
            logger.debug("Action is not a OpenIDM action delegating to CAUD", e);
        }

        JsonValue content = request.getContent();

        if (requestAction != null) {

            switch (requestAction) {
                case getChangedWatchedFields:
                    List<String> changedFields =
                            checkForFields(watchFieldFilters, content.get("before"), content.get("after"));

                    return newActionResponse(new JsonValue(changedFields)).asPromise();

                case getChangedPasswordFields:
                    List<String> changedPasswordFields =
                            checkForFields(passwordFieldFilters, content.get("before"), content.get("after"));

                    return newActionResponse(new JsonValue(changedPasswordFields)).asPromise();

                case availableHandlers:
                    return getAvailableAuditEventHandlersWithConfigSchema();

                default:
                    //allow to fall to caud
            }
        }
        //if action unknown in openidm, delegate it caud
        return auditService.handleAction(context, request);
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

    private void formatException(final JsonValue entry) throws Exception {
        if (!entry.isDefined(EXCEPTION)
                || entry.get(EXCEPTION).isNull()
                || exceptionFormatterScript == null) {
            return;
        }

        final Object exception = entry.get(EXCEPTION).getObject();

        if (exception instanceof Exception) {
            final Script s = exceptionFormatterScript.getScript(new RootContext());
            s.put(EXCEPTION, exception);
            entry.put(EXCEPTION, s.eval());
        }
    }

    /**
     * Gets the available audit event handlers from the audit service and the config schema.
     *
     * Should return a json object similar to this:
     * <pre>
     *      [{
     *          "class" : "org.forgerock.audit.events.handlers.impl.CSVAuditEventHandler",
     *          "config" : {
     *              "type" : "object",
     *              "properties" : {
     *                  "logDirectory" : {
     *                      "type" : "string"
     *                  },
     *                  ....
     *              }
     *          }
     *      },
     *      {
     *          "class" : "org.forgerock.audit.events.handlers.impl.AnotherAuditEventHandler",
     *          "config" : {
     *              "type" : "object",
     *              "properties" : {
     *                  "configKey" : {
     *                      "type" : "string"
     *                  },
     *                  ....
     *              }
     *          }
     *      }]
     * </pre>
     * @return A json object containing the available audit event handlers and their config schema.
     * @throws AuditException If an error occurs instantiating one of the audit event handlers
     */
    private Promise<ActionResponse, ResourceException> getAvailableAuditEventHandlersWithConfigSchema() {
        try {
            final List<String> availableAuditEventHandlers = auditService.getConfig().getAvailableAuditEventHandlers();
            final JsonValue result = new JsonValue(new LinkedList<>());

            for (final String auditEventHandler : availableAuditEventHandlers) {
                final JsonValue entry = json(object(
                        field("class", auditEventHandler),
                        field("config",
                                AuditJsonConfig.getAuditEventHandlerConfigurationSchema(
                                        auditEventHandler, getClass().getClassLoader()).getObject()),
                        field("isUsableForQueries", canUseForQueries(auditEventHandler))
                ));
                result.add(entry.getObject());
            }
            return newActionResponse(result).asPromise();
        } catch (AuditException | ServiceUnavailableException e) {
            return new InternalServerErrorException(
                    "Unable to get available audit event handlers and their config schema", e)
                    .asPromise();
        }
    }

    private boolean canUseForQueries(final String auditEventHandler) {
        final EventHandlerConfiguration eventHandlerConfiguration = dummyAuditEventHandlers.get(auditEventHandler);
        return eventHandlerConfiguration != null && eventHandlerConfiguration.isUsableForQueries();
    }

    private JsonValue getCustomTopics(JsonValue defaultTopics, JsonValue configuredTopics) {
        final JsonValue customTopics = defaultTopics;
        for (DefaultAuditTopics defaultAuditTopic : DefaultAuditTopics.values()) {
            configuredTopics.remove(defaultAuditTopic.name().toLowerCase());
        }
        customTopics.asMap().putAll(configuredTopics.asMap());
        return customTopics;
    }

    @VisibleForTesting
    protected KeyStoreHandlerProvider createKeyStoreHandlerProvider() throws Exception {
        final Map<String, KeyStoreHandler> keystoreHandlers = new LinkedHashMap<>(1);
        keystoreHandlers.put(OPENIDM_KEYSTORE_NAME, new JcaKeyStoreHandler(
                JettyPropertyUtil.getProperty(OPENIDM_KEYSTORE_TYPE, false),
                JettyPropertyUtil.getProperty(OPENIDM_KEYSTORE_LOCATION, false),
                JettyPropertyUtil.getProperty(OPENIDM_KEYSTORE_PASSWORD, false)));
        return new DefaultKeyStoreHandlerProvider(keystoreHandlers);
    }

    private void createDummyAuditEventHandlers(final Map<String, EventHandlerConfiguration> handlers)
            throws ServiceUnavailableException {

        final List<String> availableAuditEventHandlers = auditService.getConfig().getAvailableAuditEventHandlers();
        for (final String auditEventHandler : availableAuditEventHandlers) {
            try {
                handlers.put(
                        auditEventHandler,
                        (EventHandlerConfiguration) Class.forName(auditEventHandler + "Configuration").newInstance());
            } catch (ClassNotFoundException|InstantiationException|IllegalAccessException e) {
                logger.warn("Unable to create dummy audit event handler for: {}", auditEventHandler);
            }
        }
    }
}
