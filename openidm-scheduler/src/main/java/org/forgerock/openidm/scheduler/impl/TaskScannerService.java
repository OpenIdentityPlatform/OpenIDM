/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2012 ForgeRock AS. All Rights Reserved
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
*
*/
package org.forgerock.openidm.scheduler.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResource;
import org.forgerock.json.resource.JsonResourceAccessor;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.objset.ObjectSetContext;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.ObjectSetJsonResource;
import org.forgerock.openidm.quartz.impl.ExecutionException;
import org.forgerock.openidm.quartz.impl.ScheduledService;
import org.forgerock.openidm.scope.ScopeFactory;
import org.forgerock.openidm.script.Script;
import org.forgerock.openidm.script.ScriptException;
import org.forgerock.openidm.script.Scripts;
import org.forgerock.openidm.util.DateUtil;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "org.forgerock.openidm.taskscanner", policy = ConfigurationPolicy.IGNORE, immediate = true)
@Properties({
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM TaskScanner Service"),
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "taskscanner")
})
@Service
public class TaskScannerService extends ObjectSetJsonResource implements ScheduledService {
    private final static Logger logger = LoggerFactory.getLogger(TaskScannerService.class);
    private final static DateUtil dateUtil = DateUtil.getDateUtil();

    private final static String FIND_BY_ID = "find-by-id";

    private final static ObjectMapper mapper;
    static {
        JsonFactory jsonFactory = new JsonFactory();
        jsonFactory.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true);
        mapper = new ObjectMapper(jsonFactory);
    }

    @Reference
    private ScopeFactory scopeFactory;

    @Reference(
        name = "ref_ManagedObjectService_JsonResourceRouterService",
        referenceInterface = JsonResource.class,
        bind = "bindRouter",
        unbind = "unbindRouter",
        cardinality = ReferenceCardinality.MANDATORY_UNARY,
        policy = ReferencePolicy.STATIC,
        target = "(service.pid=org.forgerock.openidm.router)"
    )
    private JsonResource router;

    protected void bindRouter(JsonResource router) {
        this.router = router;
    }

    protected void unbindRouter(JsonResource router) {
        this.router = null;
    }

    private JsonResourceAccessor accessor() {
        return new JsonResourceAccessor(router, ObjectSetContext.get());
    }

    private Map<String, Object> newScope() {
        return scopeFactory.newInstance(ObjectSetContext.get());
    }

    /**
     * Invoked by the Task Scanner whenever the task scanner is triggered by the scheduler
     */
    @Override
    public void execute(Map<String, Object> context) throws ExecutionException {
        String invokerName = (String) context.get(INVOKER_NAME);
        String scriptName = (String) context.get(CONFIG_NAME);
        JsonValue params = new JsonValue(context).get(CONFIGURED_INVOKE_CONTEXT);

        performTask(invokerName, scriptName, params);
    }

    /**
     * Performs the task associated with the task scanner event.
     * Runs the query and executes the script across each resulting object.
     *
     * @param invokerName name of the invoker
     * @param scriptName name of the script associated with the task scanner event
     * @param params parameters necessary for the execution of the script
     * @throws ExecutionException
     */
    private void performTask(String invokerName, String scriptName, JsonValue params)
            throws ExecutionException {
        JsonValue scriptValue = params.get("task").expect(Map.class).get("script").expect(Map.class);
        JsonValue scanValue = params.get("scan").expect(Map.class);

        if (!scriptValue.isNull()) {
            String id = scanValue.get("object").required().asString();
            JsonPointer startField = getTaskStatePointer(scanValue, "started");
            JsonPointer completedField = getTaskStatePointer(scanValue, "completed");

            JsonValue flattenedScanValue = flattenJson(scanValue);
            JsonValue results = performQuery(id, flattenedScanValue);

            Script script = Scripts.newInstance(scriptName, scriptValue);

            logger.debug("Results: {}", results.size());
            for (JsonValue input : results) {
                execScript(scriptName, invokerName, script, id, startField, completedField, input);
            }
        } else {
            throw new ExecutionException("No valid script '" + scriptValue + "' configured in task scanner.");
        }
    }

    /**
     * Performs a query on a resource and returns the result set
     * @param resourceID the identifier of the resource to query
     * @param params parameters to supply to the query
     * @return the set of results from the performed query
     */
    private JsonValue performQuery(String resourceID, JsonValue params) {
        JsonValue queryResults = null;
        try {
            queryResults = accessor().query(resourceID, params);
        } catch(JsonResourceException e) {
            logger.warn("JsonResourceException", e);
        }
        return queryResults.get("result");
    }

    /**
     * Fetches a JsonPointer from the "taskState" object in value
     * @param value JsonValue to fetch the pointer from
     * @param field the subfield to fetch from the taskState object
     * @return the JsonPointer contained within the "taskState/${field}" object
     */
    private JsonPointer getTaskStatePointer(JsonValue value, String field) {
        return value.get("taskState").required().get(field).required().asPointer();
    }

    /**
     * Adds an object to a JsonValue and performs an update
     * @param resourceID the resource identifier that the updated value belongs to
     * @param value value to perform the update with
     * @param path JsonPointer to the updated/added field
     * @param obj object to add to the field
     * @return the updated JsonValue
     */
    private JsonValue updateValueWithObject(String resourceID, JsonValue value, JsonPointer path, Object obj) {
        ensureJsonPointerExists(path, value);
        value.put(path, obj);
        return performUpdate(resourceID, value);
    }

    /**
     * Performs an update on a given resource with a supplied JsonValue
     * @param resourceID the resource identifier to perform the update on
     * @param value the object to update with
     * @return the updated object
     */
    private JsonValue performUpdate(String resourceID, JsonValue value) {
        String id = value.get("_id").required().asString();
        String fullID = retrieveFullID(resourceID, value);
        String rev = value.get("_rev").required().asString();

        try {
            accessor().update(fullID, rev, value);
        } catch (JsonResourceException e) {
            logger.warn("Update error", e);
        }

        return retrieveObject(resourceID, id);
    }

    /**
     * Constructs a full object ID from the supplied resourceID and the JsonValue
     * @param resourceID resource ID that the value originates from
     * @param value JsonValue to create the full ID with
     * @return string indicating the full id
     */
    private String retrieveFullID(String resourceID, JsonValue value) {
        String id = value.get("_id").required().asString();
        return resourceID + '/' + id;
    }

    /**
     * Fetches an updated copy of some specified object from the given resource
     * @param resourceID the resource identifier to fetch an object from
     * @param value the value to retrieve an updated copy of
     * @return the updated value
     */
    private JsonValue retrieveUpdatedObject(String resourceID, JsonValue value) {
        return retrieveObject(resourceID, value.get("_id").required().asString());
    }

    /**
     * Retrieves a specified object from a resource
     * @param resourceID the resource identifier to fetch the object from
     * @param id the identifier of the object to fetch
     * @return the object retrieved from the resource
     */
    private JsonValue retrieveObject(String resourceID, String id) {
        JsonValue params = new JsonValue(new HashMap<String, Object>());
        params.add("id", id);
        params.add("_query-id", FIND_BY_ID);
        return performQuery(resourceID, params).get(0);
    }

    /**
     * Performs the individual executions of the supplied script
     *
     * Passes <b>"input"</b> and <b>"objectID"</b> to the script.<br>
     *   <b>"objectID"</b> contains the full ID of the supplied object (including resource identifier).
     *      Useful for performing updates.<br>
     *   <b>"input"</b> contains the supplied object
     *
     * @param scriptName name of the script
     * @param invokerName name of the invoker
     * @param script the script to execute
     * @param resourceID the resource identifier for the object that the script will be performed on
     * @param startField JsonPointer to the field that will be marked at script start
     * @param completedField JsonPointer to the field that will be marked at script completion
     * @param input value to input to the script
     * @throws ExecutionException
     */
    private void execScript(String scriptName, String invokerName, Script script, String resourceID,
            JsonPointer startField, JsonPointer completedField, JsonValue input) throws ExecutionException {
        if (script != null) {
            Map<String, Object> scope = newScope();

            logger.debug("Input: {}", input);

            JsonValue _input = updateValueWithObject(resourceID, input, startField, dateUtil.now());
            logger.debug("Updated StartField: {}", _input);
            scope.put("input", _input.getObject());
            scope.put("objectID", retrieveFullID(resourceID, _input));;

            try {
                Object returnedValue = script.exec(scope);
                _input = retrieveUpdatedObject(resourceID, _input);
                logger.debug("After script execution: {}", _input);

                if (returnedValue == Boolean.TRUE) {
                   _input = updateValueWithObject(resourceID, _input, completedField, dateUtil.now());
                   logger.debug("Updated CompletedField: {}", _input);
                }

            } catch (ScriptException se) {
                String msg = scriptName + " script invoked by " + invokerName + " encountered exception";
                logger.debug(msg, se);
                throw new ExecutionException(msg, se);
            }
        }
    }

    /**
     * Flattens JsonValue into a one-level-deep object
     * @param original original JsonValue object
     * @return flattened JsonValue
     */
    private static JsonValue flattenJson(JsonValue original) {
        return flattenJson("", original);
    }

    /**
     * Flattens JsonValue into a one-level-deep object
     * @param parent name of the parent object (for nested objects)
     * @param original original JsonValue object
     * @return flattened JsonValue
     */
    private static JsonValue flattenJson(String parent, JsonValue original) {
        JsonValue flattened = new JsonValue(new HashMap<String, Object>());
        Iterator<String> iter = original.keys().iterator();
        while (iter.hasNext()) {
            String oKey = iter.next();
            String key = (parent.isEmpty() ? "" : parent + ".") + oKey;

            JsonValue value = original.get(oKey);
            if (value.isMap()) {
                addAllToJson(flattened, flattenJson(key, value));
            } else {
                flattened.put(key, value.getObject());
            }
        }
        return flattened;
    }

    /**
     * Adds all objects from one JsonValue to another (performs a merge).
     * Any values contained in both objects will be overwritten to reflect the values in <b>from</b>
     * <br><br>
     * <i><b>NOTE:</b> this should be a part of JsonValue itself (so we can support merging two JsonValue objects)</i>
     * @param to JsonValue that will have objects added to it
     * @param from JsonValue that will be used as reference for updating
     */
    private static void addAllToJson(JsonValue to, JsonValue from) {
        Iterator<String> iter = from.keys().iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            to.put(key, from.get(key).getObject());
        }
    }

    /**
     * Ensure that some JsonPointer exists within a supplied object so that some object can be placed in that field
     * @param ptr JsonPointer to ensure exists at each level
     * @param obj object to ensure the JsonPointer exists within
     */
    private static void ensureJsonPointerExists(JsonPointer ptr, JsonValue obj) {
        JsonValue refObj = obj;
        for (String p : ptr) {
            if (!refObj.isDefined(p)) {
                refObj.put(p, new JsonValue(new HashMap<String, Object>()));
                refObj = refObj.get(p);
            }
        }
    }

    @Override
    public Map<String, Object> action(String id, Map<String, Object> params)
            throws ObjectSetException {
        String action = (String) params.get("_action");
        try {
            if (action.equals("execute")) {
                try {
                    return onExecute(id, params);
                } catch (JsonProcessingException e) {
                    throw new ObjectSetException(ObjectSetException.INTERNAL_ERROR, e);
                } catch (IOException e) {
                    throw new ObjectSetException(ObjectSetException.INTERNAL_ERROR, e);
                }
            } else {
                return null;
            }
        } catch (ExecutionException e) {
            throw new ObjectSetException(ObjectSetException.INTERNAL_ERROR, e);
        }
    }

    /**
     * Performs the "execute" action, executing a supplied configuration
     *
     * Expects a field "config" containing a JSON object in the query string.
     * This object need only be the equivalent to the "invokeContext" field in the configuration script
     *
     * @param id the id to perform the action on
     * @param params field contaning the
     * @return the set of parameters supplied
     * @throws ExecutionException
     * @throws JsonProcessingException
     * @throws IOException
     */
    private Map<String, Object> onExecute(String id, Map<String, Object> params)
            throws ExecutionException, JsonProcessingException, IOException {
        String paramsString = (String) params.get("config");
        TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {};
        Map<String,Object> paramsMap = mapper.readValue(paramsString, typeRef);

        JsonValue _params = new JsonValue(paramsMap);

        String scriptName = _params.get(CONFIG_NAME).asString();

        performTask("REST", scriptName, _params);
        // TODO Should this return something better? Some indication that it's successfully completed execution.
        // I stole this from SchedulerService and a few other ones
        return params;
    }
}
