/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All rights reserved.
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
import java.io.PrintWriter;
import java.io.StringWriter;
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
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.patch.JsonPatch;
import org.forgerock.openidm.audit.AuditService;
import org.forgerock.openidm.audit.util.Action;
import org.forgerock.openidm.audit.util.ActivityLog;
import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.InvalidException;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.crypto.factory.CryptoServiceFactory;
import org.forgerock.openidm.objset.BadRequestException;
import org.forgerock.openidm.objset.ConflictException;
import org.forgerock.openidm.objset.ForbiddenException;
import org.forgerock.openidm.objset.InternalServerErrorException;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetContext;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.ObjectSetJsonResource;
import org.forgerock.openidm.objset.Patch;
import org.forgerock.openidm.objset.PreconditionFailedException;
import org.forgerock.openidm.script.Script;
import org.forgerock.openidm.script.Scripts;
import org.forgerock.openidm.util.DateUtil;
import org.forgerock.openidm.util.JsonUtil;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Audit module
 * @author aegloff
 */
@Component(name = "org.forgerock.openidm.audit", immediate=true, policy=ConfigurationPolicy.REQUIRE)
@Service
@Properties({
    @Property(name = "service.description", value = "Audit Service"),
    @Property(name = "service.vendor", value = "ForgeRock AS"),
    @Property(name = "openidm.router.prefix", value = AuditService.ROUTER_PREFIX)
})
public class AuditServiceImpl extends ObjectSetJsonResource implements AuditService {
    final static Logger logger = LoggerFactory.getLogger(AuditServiceImpl.class);
    private final static ObjectMapper mapper;

    // Keys in the JSON configuration
    public final static String CONFIG_LOG_TO = "logTo";
    public final static String CONFIG_LOG_TYPE = "logType";
    public final static String CONFIG_LOG_TYPE_CSV = "csv";
    public final static String CONFIG_LOG_TYPE_REPO = "repository";
    
    public final static String TYPE_RECON = "recon";
    public final static String TYPE_ACTIVITY = "activity";

    public final static String QUERY_BY_RECON_ID = "audit-by-recon-id";
    public final static String QUERY_BY_MAPPING = "audit-by-mapping";
    public final static String QUERY_BY_RECON_ID_AND_SITUATION = "audit-by-recon-id-situation";
    public final static String QUERY_BY_RECON_ID_AND_TYPE = "audit-by-recon-id-type";
    public final static String QUERY_BY_ACTIVITY_PARENT_ACTION = "audit-by-activity-parent-action";

    private static Script script = null;
    
    EnhancedConfig enhancedConfig = new JSONEnhancedConfig();

    Map<String, List<String>> actionFilters;
    Map<String, Map<String, List<String>>> triggerFilters;
    List<JsonPointer> watchFieldFilters;
    List<JsonPointer> passwordFieldFilters;

    AuditLogger queryLogger = null;
    List<AuditLogger> auditLoggers;
    JsonValue config; // Existing active configuration
    DateUtil dateUtil;

    static {
        JsonFactory jsonFactory = new JsonFactory();
        jsonFactory.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true);
        mapper = new ObjectMapper(jsonFactory);
    }
    
    /** Although we may not need the router here, 
         https://issues.apache.org/jira/browse/FELIX-3790
        if using this with for scr 1.6.2
        Ensure we do not get bound on router whilst it is activating
    */
/*    @Reference(
        referenceInterface = JsonResource.class,
        bind = "bindRouter",
        unbind = "unbindRouter",
        cardinality = ReferenceCardinality.MANDATORY_UNARY,
        policy = ReferencePolicy.STATIC,
        target = "(service.pid=org.forgerock.openidm.router)"
    )
    Object router;
    protected void bindRouter(JsonResource router) {
        this.router = router;
    }
    protected void unbindRouter(JsonResource router) {
        this.router = router;
    }
*/
    /**
     * Gets an object from the audit logs by identifier. The returned object is not validated
     * against the current schema and may need processing to conform to an updated schema.
     * <p>
     * The object will contain metadata properties, including object identifier {@code _id},
     * and object version {@code _rev} to enable optimistic concurrency
     *
     * @param fullId the identifier of the object to retrieve from the object set.
     * @throws NotFoundException if the specified object could not be found.
     * @throws ForbiddenException if access to the object is forbidden.
     * @throws BadRequestException if the passed identifier is invalid
     * @return the requested object.
     */
    @Override
    public Map<String, Object> read(String fullId) throws ObjectSetException {
        logger.debug("Audit read called for {}", fullId);
        AuditLogger auditLogger = getQueryAuditLogger();
        return auditLogger.read(fullId);
    }

    /**
     * Creates a new object in the object set.
     * <p>
     * This method sets the {@code _id} property to the assigned identifier for the object,
     * and the {@code _rev} property to the revised object version (For optimistic concurrency)
     *
     * @param fullId the client-generated identifier to use, or {@code null} if server-generated identifier is requested.
     * @param obj the contents of the object to create in the object set.
     * @throws NotFoundException if the specified id could not be resolved.
     * @throws ForbiddenException if access to the object or object set is forbidden.
     * @throws PreconditionFailedException if an object with the same ID already exists.
     */
    @Override
    public void create(String fullId, Map<String, Object> obj) throws ObjectSetException {
        logger.debug("Audit create called for {} with {}", fullId, obj);
        
        if (fullId == null) {
            throw new BadRequestException("Audit service called without specifying which audit log in the identifier");
        }

        String[] splitTypeAndId =  splitFirstLevel(fullId);
        String type = splitTypeAndId[0];
        String localId = splitTypeAndId[1];

        String trigger = getTrigger();

        // Filter
        List<String> actionFilter = actionFilters.get(type);
        Map<String, List<String>> triggerFilter = triggerFilters.get(type);

        if (triggerFilter != null && trigger != null) {
            List<String> triggerActions = triggerFilter.get(trigger);
            if (triggerActions == null) {
                logger.debug("Trigger filter not set for " + trigger + ", allowing all actions");
            } else if (!triggerActions.contains(obj.get("action"))) {
                logger.debug("Filtered by trigger filter");
                return;
            }
        }

        if (actionFilter != null) {
            // TODO: make filters that can operate on a variety of conditions
            if (!actionFilter.contains(obj.get("action"))) {
                logger.debug("Filtered by action filter");
                return;
            }
        }

        // Activity log preprocessing
        if (type.equals("activity")) {
            processActivityLog(obj);
        }

        // Generate an ID if there is none
        if (localId == null || localId.isEmpty()) {
            localId = UUID.randomUUID().toString();
            obj.put(ObjectSet.ID, localId);
            logger.debug("Assigned id {}", localId);
        }
        String id = type + "/" + localId;

        // Generate unified timestamp
        if (null == obj.get("timestamp")) {
            obj.put("timestamp", dateUtil.now());
        }

        logger.debug("Create audit entry for {} with {}", id, obj);
        for (AuditLogger auditLogger : auditLoggers) {
            try {
                auditLogger.create(id, obj);
            } catch (ObjectSetException ex) {
                logger.warn("Failure writing audit log: {} with logger {}", new Object[] {id, auditLogger, ex});
                throw ex;
            } catch (RuntimeException ex) {
                logger.warn("Failure writing audit log: {} with logger {}", new Object[] {id, auditLogger, ex});
                throw ex;
            }
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

        Object rawBefore = activity.get(ActivityLog.BEFORE);
        Object rawAfter = activity.get(ActivityLog.AFTER);

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
                activity.put(ActivityLog.BEFORE, (JsonUtil.jsonIsNull(before)) ? null : mapper.writeValueAsString(before.getObject()));
            } catch (IOException e) {
                activity.put(ActivityLog.BEFORE, (JsonUtil.jsonIsNull(before)) ? null : before.getObject().toString());
            }
            try {
                activity.put(ActivityLog.AFTER, (JsonUtil.jsonIsNull(after)) ? null : mapper.writeValueAsString(after.getObject())); // how can we know for system objects?
            } catch (IOException e) {
                activity.put(ActivityLog.AFTER, (JsonUtil.jsonIsNull(after)) ? null : after.getObject().toString()); // how can we know for system objects?
            }
        }

        // Add the list of changed fields to the object
        activity.put(ActivityLog.CHANGED_FIELDS, changedFields.isEmpty() ? null : changedFields);
        // Add the flag indicating password fields have changed
        activity.put(ActivityLog.PASSWORD_CHANGED, passwordChanged);
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
    private String getTrigger() {
        JsonValue context = ObjectSetContext.get();
        String trigger = null;
        // Loop through parent contexts, and return highest "trigger"
        while (!context.isNull()) {
            JsonValue tmp = context.get("trigger");
            if (!tmp.isNull()) {
                trigger = tmp.asString();
            }
            context = context.get("parent");
        }
        return trigger;
    }

    /**
     * Audit service does not support changing audit entries.
     */
    @Override
    public void update(String fullId, String rev, Map<String, Object> obj) throws ObjectSetException {
        throw new ForbiddenException("Not allowed on audit service");
    }

    /**
     * Audit service currently does not support deleting audit entries.
     *
     * Deletes the specified object from the object set.
     *
     * @param fullId the identifier of the object to be deleted.
     * @param rev the version of the object to delete or {@code null} if not provided.
     * @throws NotFoundException if the specified object could not be found.
     * @throws ForbiddenException if access to the object is forbidden.
     * @throws ConflictException if version is required but is {@code null}.
     * @throws PreconditionFailedException if version did not match the existing object in the set.
     */
    @Override
    public void delete(String fullId, String rev) throws ObjectSetException {
        throw new ForbiddenException("Not allowed on audit service");
    }

    /**
     * Audit service does not support changing audit entries.
     */
    @Override
    public void patch(String id, String rev, Patch patch) throws ObjectSetException {
        throw new ForbiddenException("Not allowed on audit service");
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
     * @param fullId identifies the object to query.
     * @param params the parameters of the query to perform.
     * @return the query results, which includes meta-data and the result records in JSON object structure format.
     * @throws NotFoundException if the specified object could not be found.
     * @throws BadRequestException if the specified params contain invalid arguments, e.g. a query id that is not
     * configured, a query expression that is invalid, or missing query substitution tokens.
     * @throws ForbiddenException if access to the object or specified query is forbidden.
     */
    @Override
    public Map<String, Object> query(String fullId, Map<String, Object> params) throws ObjectSetException {
        logger.debug("Audit query called for {} with {}", fullId, params);
        AuditLogger auditLogger = getQueryAuditLogger();
        return auditLogger.query(fullId, params);
    }

    /**
     * Audit service does not support actions on audit entries.
     */
    @Override
    public Map<String, Object> action(String fullId, Map<String, Object> params) throws ObjectSetException {
        throw new ForbiddenException("Not allowed on audit service");
    }
    
    
    /**
     * Returns the logger to use for reads/queries.
     *
     * @return an AuditLogger to use for queries.
     * @throws ObjectSetException on failure to find an appropriate logger.
     */
    public AuditLogger getQueryAuditLogger() throws ObjectSetException {
        if (queryLogger != null) {
            return queryLogger;
        } else if (auditLoggers.size() > 0) {
            return auditLoggers.get(0);
        } else {
            throw new InternalServerErrorException("No audit loggers available");
        }
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
        JsonValue efConfig = config.get("exceptionFormatter");
        if (!efConfig.isNull()) {
            script = Scripts.newInstance((String)compContext.getProperties().get(Constants.SERVICE_PID), efConfig);
        }
        
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

    List<AuditLogger> getAuditLoggers(JsonValue config, ComponentContext compContext) {
        List<AuditLogger> configuredLoggers = new ArrayList<AuditLogger>();
        List logTo = config.get(CONFIG_LOG_TO).asList();
        for (Map entry : (List<Map>)logTo) {
            String logType = (String) entry.get(CONFIG_LOG_TYPE);
            // TDDO: make pluggable
            AuditLogger auditLogger = null;
            if (logType != null && logType.equalsIgnoreCase(CONFIG_LOG_TYPE_CSV)) {
                auditLogger = new CSVAuditLogger();
            } else if (logType != null && logType.equalsIgnoreCase(CONFIG_LOG_TYPE_REPO)) {
                auditLogger = new RepoAuditLogger();
            } else {
                throw new InvalidException("Configured audit logType is unknown: " + logType);
            }
            if (auditLogger != null) {
                auditLogger.setConfig(entry, compContext.getBundleContext());
                logger.info("Audit configured to log to {}", logType);
                configuredLoggers.add(auditLogger);
                if (entry.containsKey("useForQueries")) {
                    if ((Boolean)entry.get("useForQueries")) {
                        logger.info("Audit logger used for queries set to " + logType);
                        queryLogger = auditLogger;
                    }
                }
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
            Map<String, Object> scope = new HashMap<String, Object>();
            scope.put("exception", e);
            result = (String) script.exec(scope);
        }
        return result;
    }

    public static Map<String, Object> formatLogEntry(Map<String, Object> entry, String type) {
        if (type.equals(AuditServiceImpl.TYPE_RECON)) {
            return AuditServiceImpl.formatReconEntry(entry);
        } else {
            return entry;
        }
    }

    /**
     * Returns a audit log recon entry formatted based on the entryType (summary, start, recon entry).
     * 
     * @param entry the full entry to format
     * @return the formatted entry
     */
    public static Map<String, Object> formatReconEntry(Map<String, Object> entry) {
        Map<String, Object> formattedEntry = new HashMap<String, Object>();
        formattedEntry.put("_id", entry.get("_id"));
        formattedEntry.put("entryType", entry.get("entryType"));
        formattedEntry.put("timestamp", entry.get("timestamp"));
        formattedEntry.put("reconId", entry.get("reconId"));
        formattedEntry.put("rootActionId", entry.get("rootActionId"));
        formattedEntry.put("status", entry.get("status"));
        formattedEntry.put("message", entry.get("message"));
        formattedEntry.put("messageDetail", entry.get("messageDetail"));
        formattedEntry.put("exception", entry.get("exception"));
        if ("".equals(entry.get("entryType"))) {
            // recon entry
            formattedEntry.put("actionId", entry.get("actionId"));
            formattedEntry.put("action", entry.get("action"));
            formattedEntry.put("ambiguousTargetObjectIds", entry.get("ambiguousTargetObjectIds"));
            formattedEntry.put("reconciling", entry.get("reconciling"));
            formattedEntry.put("situation", entry.get("situation"));
            formattedEntry.put("sourceObjectId", entry.get("sourceObjectId"));
            formattedEntry.put("targetObjectId", entry.get("targetObjectId"));
        } else {
            formattedEntry.put("mapping", entry.get("mapping"));
        }
        return formattedEntry;
    }
    
    public static Map<String, Object> getReconResults(List<Map<String, Object>> entryList, String reconId, boolean formatted) {
        Map<String, Object> results = new HashMap<String, Object>();
        List<Map<String, Object>> resultEntries = new ArrayList<Map<String, Object>>();
        if (formatted) {
            if (reconId != null) {
                for (Map<String, Object> entry : entryList) {
                    if (reconId.equals(entry.get("reconId"))) {
                        if ("start".equals(entry.get("entryType"))) {
                            results.put("start", AuditServiceImpl.formatReconEntry(entry));
                        } else if ("summary".equals(entry.get("entryType"))) {
                            results.put("summary", AuditServiceImpl.formatReconEntry(entry));
                        } else {
                            resultEntries.add(AuditServiceImpl.formatReconEntry(entry));
                        }
                    }
                }
            } else {
                for (Map<String, Object> entry : entryList) {
                    resultEntries.add(AuditServiceImpl.formatReconEntry(entry));
                }
            }
            if (resultEntries.size() > 0) {
                results.put("result", resultEntries);
            }
        } else {
            results.put("result", entryList);
        }
        return results;
    }

    public static Map<String, Object> getActivityResults(List<Map<String, Object>> entryList) {
        Map<String, Object> results = new HashMap<String, Object>();
        results.put("result", entryList);
        return results;
    }
    
    protected static JsonValue parseJsonString(String stringified) {
        JsonValue jsonValue = null;
        try {
            Map parsedValue = (Map) mapper.readValue(stringified, Map.class);
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
