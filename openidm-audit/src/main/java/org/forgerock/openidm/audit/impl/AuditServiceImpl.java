/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All rights reserved.
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
import org.apache.felix.scr.annotations.Service;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.patch.JsonPatch;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.audit.AuditService;
import org.forgerock.openidm.audit.util.Action;
import org.forgerock.openidm.audit.util.ActivityLog;
import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.InvalidException;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.crypto.factory.CryptoServiceFactory;
import org.forgerock.openidm.util.DateUtil;
import org.forgerock.openidm.util.JsonUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Audit module
 * 
 * @author aegloff
 */
@Component(name = AuditServiceImpl.PID, immediate=true, policy=ConfigurationPolicy.REQUIRE)
@Service
@Properties({
    @Property(name = "service.description", value = "Audit Service"),
    @Property(name = "service.vendor", value = "ForgeRock AS"),
    @Property(name = "openidm.router.prefix", value = AuditService.ROUTER_PREFIX) })
public class AuditServiceImpl implements AuditService {

    public static final String PID = "org.forgerock.openidm.audit";

    /**
     * Setup logging for the {@link AuditServiceImpl}.
     */
    final static Logger logger = LoggerFactory.getLogger(AuditServiceImpl.class);
    private final static ObjectMapper mapper;

    // Keys in the JSON configuration
    public final static String CONFIG_LOG_TO = "logTo";
    public final static String CONFIG_LOG_TYPE = "logType";
    public final static String CONFIG_LOG_TYPE_CSV = "csv";
    public final static String CONFIG_LOG_TYPE_REPO = "repository";

    EnhancedConfig enhancedConfig = new JSONEnhancedConfig();

    Map<String, List<String>> actionFilters;
    Map<String, Map<String, List<String>>> triggerFilters;
    List<JsonPointer> watchFieldFilters;
    List<JsonPointer> passwordFieldFilters;

    List<AuditLogger> auditLoggers;
    JsonValue config; // Existing active configuration
    DateUtil dateUtil;

    static {
        JsonFactory jsonFactory = new JsonFactory();
        jsonFactory.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true);
        mapper = new ObjectMapper(jsonFactory);
    }

    /**
     * Gets an object from the audit logs by identifier. The returned object is
     * not validated against the current schema and may need processing to
     * conform to an updated schema.
     * <p>
     * The object will contain metadata properties, including object identifier
     * {@code _id}, and object version {@code _rev} to enable optimistic
     * concurrency
     * 
     * {@link org.forgerock.json.resource.RequestHandler#handleRead(org.forgerock.json.resource.ServerContext, org.forgerock.json.resource.ReadRequest, org.forgerock.json.resource.ResultHandler)
     * Reads} an existing resource within the collection.
     * 
     * @param context
     *            The request server context.
     * @param resourceId
     *            The ID of the targeted resource within the collection.
     * @param request
     *            The read request.
     * @param handler
     *            The result handler to be notified on completion.
     * @see org.forgerock.json.resource.RequestHandler#handleRead(org.forgerock.json.resource.ServerContext,
     *      org.forgerock.json.resource.ReadRequest,
     *      org.forgerock.json.resource.ResultHandler)
     */
    public void readInstance(ServerContext context, String resourceId, ReadRequest request,
            ResultHandler<Resource> handler) {
        handler.handleResult(new Resource(null, null, null));
    }

    /**
     * {@link org.forgerock.json.resource.RequestHandler#handleCreate(org.forgerock.json.resource.ServerContext,
     * org.forgerock.json.resource.CreateRequest, org.forgerock.json.resource.ResultHandler) Adds} a new resource
     * instance to the collection.
     * <p/>
     * Create requests are targeted at the collection itself and may include a user-provided resource ID for the new
     * resource as part of the request itself. The user-provider resource ID may be accessed using the method {@link
     * org.forgerock.json.resource.CreateRequest#getNewResourceId()}.
     *
     * @param context
     *         The request server context.
     * @param request
     *         The create request.
     * @param handler
     *         The result handler to be notified on completion.
     * @see org.forgerock.json.resource.RequestHandler#handleCreate(org.forgerock.json.resource.ServerContext,
     *      org.forgerock.json.resource.CreateRequest, org.forgerock.json.resource.ResultHandler)
     * @see org.forgerock.json.resource.CreateRequest#getNewResourceId()
     */
    public void createInstance(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {

        if (logger.isDebugEnabled()){
            StringBuilder sb = new StringBuilder(request.getResourceName());
            if (null != request.getNewResourceId()){
                sb.append("/").append(request.getNewResourceId());
            }
            logger.debug("Audit create called for {} with {}", sb , request.getContent());
        }

        if (request.getResourceName() == null) {
            handler.handleError(
            new BadRequestException(
                    "Audit service called without specifying which audit log in the identifier"));
            return;
        }

        //TODO (UPGRADE) : Use Router to split
        String[] splitTypeAndId = splitFirstLevel(request.getResourceName());
        String type = splitTypeAndId[0];
        String localId = splitTypeAndId[1];

        String trigger = getTrigger(context);

        // Filter
        List<String> actionFilter = actionFilters.get(type);
        Map<String, List<String>> triggerFilter = triggerFilters.get(type);

        if (triggerFilter != null && trigger != null) {
            List<String> triggerActions = triggerFilter.get(trigger);
            if (triggerActions == null) {
                logger.debug("Trigger filter not set for " + trigger + ", allowing all actions");
            } else if (!triggerActions.contains(request.getContent().get("action").asString())) {
                logger.debug("Filtered by trigger filter");
                return;
            }
        }

        if (actionFilter != null) {
            // TODO: make filters that can operate on a variety of conditions
            if (!actionFilter.contains(request.getContent().get("action").asString())) {
                logger.debug("Filtered by action filter");
                return;
            }
        }

        // Activity log preprocessing
        if (type.equals("activity")) {
            processActivityLog(request.getContent().asMap());
        }

        // Generate an ID if there is none
        if (localId == null || localId.isEmpty()) {
            localId = UUID.randomUUID().toString();
            request.getContent().put(ServerConstants.OBJECT_PROPERTY_ID, localId);
            logger.debug("Assigned id {}", localId);
        }
        String id = type + "/" + localId;

        // Generate unified timestamp
        if (request.getContent().get("timestamp").isNull()) {
            request.getContent().put("timestamp", dateUtil.now());
        }

        logger.debug("Create audit entry for {} with {}", id, request.getContent().getObject());
        for (AuditLogger auditLogger : auditLoggers) {
            try {
                //TODO UPDATE
                auditLogger.create(id,request.getContent().asMap());
            } catch (ResourceException ex) {
                logger.warn("Failure writing audit log: {} with logger {}", new String[] { id,
                    auditLogger.toString(), ex.getMessage() });
                handler.handleError(ex);
            } catch (RuntimeException ex) {
                logger.warn("Failure writing audit log: {} with logger {}", new String[] { id,
                    auditLogger.toString(), ex.getMessage() });
                throw ex;
            }
        }
    }

    /**
     * Do any preprocessing for activity log objects Checks for any changed
     * fields and adds those to the object Also adds a flag to detect if any of
     * the flagged password fields have changed NOTE: both the watched fields
     * and the password fields will be in the list of "changedField" if they
     * differ
     * 
     * @param activity
     *            activity object to update
     */
    private void processActivityLog(Map<String, Object> activity) {
        List<String> changedFields = new ArrayList<String>();
        boolean passwordChanged = false;

        Object rawBefore = activity.get(ActivityLog.BEFORE);
        Object rawAfter = activity.get(ActivityLog.AFTER);

        if (!(rawBefore == null && rawAfter == null)) {
            JsonValue before = new JsonValue(rawBefore);
            JsonValue after = new JsonValue(rawAfter);

            // Check to see if any of the watched fields have changed and add
            // them to the comprehensive list
            List<String> changedWatchFields = checkForFields(watchFieldFilters, before, after);
            changedFields.addAll(changedWatchFields);

            // Check to see if any of the password fields have changed -- also
            // update our flag
            List<String> changedPasswordFields =
                    checkForFields(passwordFieldFilters, before, after);
            passwordChanged = !changedPasswordFields.isEmpty();
            changedFields.addAll(changedPasswordFields);

            // Update the before and after fields with their proper string
            // values now that we're done diffing
            // TODO Figure out if this is even necessary? Doesn't seem to be...
            // Once it goes to the log,
            // the object will have toString() called on it anyway which will
            // convert it to (seemingly) the same format
            try {
                activity.put(ActivityLog.BEFORE, (JsonUtil.jsonIsNull(before)) ? null : mapper
                        .writeValueAsString(before.getObject()));
            } catch (IOException e) {
                activity.put(ActivityLog.BEFORE, (JsonUtil.jsonIsNull(before)) ? null : before
                        .getObject().toString());
            }
            try {
                activity.put(ActivityLog.AFTER, (JsonUtil.jsonIsNull(after)) ? null : mapper
                        .writeValueAsString(after.getObject())); // how can we
                                                                 // know for
                                                                 // system
                                                                 // objects?
            } catch (IOException e) {
                activity.put(ActivityLog.AFTER, (JsonUtil.jsonIsNull(after)) ? null : after
                        .getObject().toString()); // how can we know for system
                                                  // objects?
            }
        }

        // Add the list of changed fields to the object
        activity.put(ActivityLog.CHANGED_FIELDS, changedFields.isEmpty() ? null : changedFields);
        // Add the flag indicating password fields have changed
        activity.put(ActivityLog.PASSWORD_CHANGED, passwordChanged);
    }

    /**
     * Checks to see if there are differences between the values in two
     * JsonValues before and after Returns a list containing the changed fields
     * 
     * @param fieldsToCheck
     *            list of JsonPointers to search for
     * @param before
     *            prior JsonValue
     * @param after
     *            JsonValue after applied changes
     * @return list of strings indicating which values changed
     */
    private List<String> checkForFields(List<JsonPointer> fieldsToCheck, JsonValue before,
            JsonValue after) {
        List<String> changedFields = new ArrayList<String>();
        for (JsonPointer jpointer : fieldsToCheck) {
            // Need to be sure to decrypt any encrypted values so we can compare
            // their string value
            // (JsonValue does not have an #equals method that works for this
            // purpose)
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
     * Checks to see if two objects are equal either as nulls or through their
     * comparator
     * 
     * @param a
     *            first object to compare
     * @param b
     *            reference object to compare against
     * @return boolean indicating equality either as nulls or as objects
     */
    private static boolean fieldsEqual(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    /**
     * Searches ObjectSetContext for the value of "trigger" and return it.
     */
    private String getTrigger(Context context) {
        /*String trigger = null;
        // Loop through parent contexts, and return highest "trigger"
        while (!context.isNull()) {
            JsonValue tmp = context.get("trigger");
            if (!tmp.isNull()) {
                trigger = tmp.asString();
            }
            context = context.get("parent");
        }
        return trigger;*/
        //TODO UPDATE
        return context.getId();
    }

    /**
     * Audit service does not support changing audit entries.
     * <p/>
     * {@link org.forgerock.json.resource.RequestHandler#handleUpdate(org.forgerock.json.resource.ServerContext, org.forgerock.json.resource.UpdateRequest, org.forgerock.json.resource.ResultHandler)
     * Updates} an existing resource within the collection.
     * 
     * @param context
     *            The request server context.
     * @param resourceId
     *            The ID of the targeted resource within the collection.
     * @param request
     *            The update request.
     * @param handler
     *            The result handler to be notified on completion.
     * @see org.forgerock.json.resource.RequestHandler#handleUpdate(org.forgerock.json.resource.ServerContext,
     *      org.forgerock.json.resource.UpdateRequest,
     *      org.forgerock.json.resource.ResultHandler)
     */
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request,
                               ResultHandler<Resource> handler) {
        handler.handleError(new ForbiddenException("Not allowed on audit service"));
    }

    /**
     * Audit service currently does not support deleting audit entries.
     * <p/>
     *  {@link org.forgerock.json.resource.RequestHandler#handleDelete(org.forgerock.json.resource.ServerContext,
     * org.forgerock.json.resource.DeleteRequest, org.forgerock.json.resource.ResultHandler) Removes} a resource instance
     * from the collection.
     *
     * @param context
     *         The request server context.
     * @param resourceId
     *         The ID of the targeted resource within the collection.
     * @param request
     *         The delete request.
     * @param handler
     *         The result handler to be notified on completion.
     * @see org.forgerock.json.resource.RequestHandler#handleDelete(org.forgerock.json.resource.ServerContext,
     *      org.forgerock.json.resource.DeleteRequest, org.forgerock.json.resource.ResultHandler)
     */
    public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request,
                               ResultHandler<Resource> handler) {
        handler.handleError(new ForbiddenException("Not allowed on audit service"));
    }


    /**
     * Audit service does not support changing audit entries.
     * <p/>
     * {@link org.forgerock.json.resource.RequestHandler#handlePatch(org.forgerock.json.resource.ServerContext,
     * org.forgerock.json.resource.PatchRequest, org.forgerock.json.resource.ResultHandler) Patches} an existing resource
     * within the collection.
     *
     * @param context
     *         The request server context.
     * @param resourceId
     *         The ID of the targeted resource within the collection.
     * @param request
     *         The patch request.
     * @param handler
     *         The result handler to be notified on completion.
     * @see org.forgerock.json.resource.RequestHandler#handlePatch(org.forgerock.json.resource.ServerContext,
     *      org.forgerock.json.resource.PatchRequest, org.forgerock.json.resource.ResultHandler)
     */
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request,
                              ResultHandler<Resource> handler) {
        handler.handleError(new ForbiddenException("Not allowed on audit service"));
    }

    /**
     * {@link org.forgerock.json.resource.RequestHandler#handleQuery(org.forgerock.json.resource.ServerContext,
     * org.forgerock.json.resource.QueryRequest, org.forgerock.json.resource.QueryResultHandler) Searches} the collection
     * for all resources which match the query request criteria.
     *
     * @param context
     *         The request server context.
     * @param request
     *         The query request.
     * @param handler
     *         The query result handler to be notified on completion.
     * @see org.forgerock.json.resource.RequestHandler#handleQuery(org.forgerock.json.resource.ServerContext,
     *      org.forgerock.json.resource.QueryRequest, org.forgerock.json.resource.QueryResultHandler)
     */
    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        handler.handleResult(new QueryResult());
    }

    /**
     * Audit service does not support actions on audit entries.
     * <p/>
     * Performs the provided {@link org.forgerock.json.resource.RequestHandler#handleAction(
     *org.forgerock.json.resource.ServerContext, org.forgerock.json.resource.ActionRequest,
     * org.forgerock.json.resource.ResultHandler) action} against the resource collection.
     *
     * @param context
     *         The request server context.
     * @param request
     *         The action request.
     * @param handler
     *         The result handler to be notified on completion.
     * @see org.forgerock.json.resource.RequestHandler#handleAction(org.forgerock.json.resource.ServerContext,
     *      org.forgerock.json.resource.ActionRequest, org.forgerock.json.resource.ResultHandler)
     */
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        handler.handleError(new ForbiddenException("Not allowed on audit service"));
    }

    /**
     * Audit service does not support actions on audit entries.
     * <p/>
     * Performs the provided {@link org.forgerock.json.resource.RequestHandler#handleAction(
     *org.forgerock.json.resource.ServerContext, org.forgerock.json.resource.ActionRequest,
     * org.forgerock.json.resource.ResultHandler) action} against a resource within the collection.
     *
     * @param context
     *         The request server context.
     * @param resourceId
     *         The ID of the targeted resource within the collection.
     * @param request
     *         The action request.
     * @param handler
     *         The result handler to be notified on completion.
     * @see org.forgerock.json.resource.RequestHandler#handleAction(org.forgerock.json.resource.ServerContext,
     *      org.forgerock.json.resource.ActionRequest, org.forgerock.json.resource.ResultHandler)
     */
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request,
                               ResultHandler<JsonValue> handler) {
        handler.handleError(new ForbiddenException("Not allowed on audit service"));
    }


    // TODO: replace with common utility to handle ID, this is temporary
    // Assumes single level type
    static String[] splitFirstLevel(String id) {
        String firstLevel = id;
        String rest = null;
        int firstSlashPos = id.indexOf("/");
        if (firstSlashPos > -1) {
            firstLevel = id.substring(0, firstSlashPos);
            rest = id.substring(firstSlashPos + 1);
        }
        logger.trace("Extracted first level: {} rest: {}", firstLevel, rest);
        return new String[] { firstLevel, rest };
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
        auditLoggers = getAuditLoggers(config, compContext);
        actionFilters = getActionFilters(config);
        triggerFilters = getTriggerFilters(config);
        watchFieldFilters =  getEventJsonPointerList(config, "activity", "watchedFields");
        passwordFieldFilters = getEventJsonPointerList(config, "activity", "passwordFields");
        logger.debug("Audit service filters enabled: {}", actionFilters);
    }

    Map<String, List<String>> getActionFilters(JsonValue config) {
        Map<String, List<String>> configFilters = new HashMap<String, List<String>>();

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
                List<String> filter = new ArrayList<String>();
                for (JsonValue action : filterActions) {
                    Enum actionEnum = action.asEnum(Action.class);
                    filter.add(actionEnum.toString());
                }
                configFilters.put(eventTypeName, filter);
            }
        }
        return configFilters;
    }

    Map<String, Map<String, List<String>>> getTriggerFilters(JsonValue config) {
        Map<String, Map<String, List<String>>> configFilters = new HashMap<String, Map<String, List<String>>>();

        JsonValue eventTypes = config.get("eventTypes");
        if(!eventTypes.isNull()) {
            Set<String> eventTypesKeys = eventTypes.keys();
            // Loop through event types ("activity", "recon", etc..)
            for (String eventTypeKey : eventTypesKeys) {
                JsonValue eventType = eventTypes.get(eventTypeKey);
                JsonValue filterTriggers = eventType.get("filter").get("triggers");
                if (!filterTriggers.isNull()) {
                    // Create map of the trigger's actions
                    Map<String, List<String>> filter = new HashMap<String, List<String>>();
                    Set<String> keys = filterTriggers.keys();
                    // Loop through individual triggers (that each contain a list of actions)
                    for (String key : keys) {
                        JsonValue trigger = filterTriggers.get(key);
                        // Create a empty list of actions for this trigger
                        List<String> triggerActions = new ArrayList<String>();
                        // Loop through the trigger's actions
                        for (JsonValue triggerAction : trigger) {
                            // Add action to list
                            Enum actionEnum = triggerAction.asEnum(Action.class);
                            triggerActions.add(actionEnum.toString());
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
     * Fetches a list of JsonPointers from the config file under a specified
     * event and field name Expects it to look similar to:
     * 
     * <PRE>
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
     * @param config
     *            the config object to draw from
     * @param event
     *            which event to draw from. ie "activity"
     * @param fieldName
     *            which fieldName to draw from. ie "watchedFields"
     * @return list containing the JsonPointers generated by the strings in the
     *         field
     */
    List<JsonPointer> getEventJsonPointerList(JsonValue config, String event, String fieldName) {
        ArrayList<JsonPointer> fieldList = new ArrayList<JsonPointer>();
        JsonValue fields = config.get("eventTypes").get(event).get(fieldName);
        for (JsonValue field : fields) {
            fieldList.add(field.asPointer());
        }
        return fieldList;
    }

    List<AuditLogger> getAuditLoggers(JsonValue config, ComponentContext compContext) {
        List<AuditLogger> configuredLoggers = new ArrayList<AuditLogger>();
        List logTo = config.get(CONFIG_LOG_TO).asList();
        for (Map entry : (List<Map>) logTo) {
            String logType = (String) entry.get(CONFIG_LOG_TYPE);
            // TDDO: make pluggable
            AuditLogger auditLogger = null;
            if (logType != null && logType.equalsIgnoreCase(CONFIG_LOG_TYPE_CSV)) {
                auditLogger = new CSVAuditLogger();
            } else if (logType != null && logType.equalsIgnoreCase(CONFIG_LOG_TYPE_REPO)) {
                // TODO: UPGRADE
                //auditLogger = new RepoAuditLogger();
            } else {
                throw new InvalidException("Configured audit logType is unknown: " + logType);
            }
            if (auditLogger != null) {
                auditLogger.setConfig(entry, compContext.getBundleContext());
                logger.info("Audit configured to log to {}", logType);
                configuredLoggers.add(auditLogger);
            }
        }
        return configuredLoggers;
    }

    @Deactivate
    void deactivate(ComponentContext compContext) {
        logger.debug("Deactivating Service {}", compContext.getProperties());
        for (AuditLogger auditLogger : auditLoggers) {
            try {
                auditLogger.cleanup();
            } catch (Exception ex) {
                logger.info("AuditLogger cleanup reported failure", ex);
            }
        }
        logger.info("Audit service stopped.");
    }
}
