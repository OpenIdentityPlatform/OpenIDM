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
*/
package org.forgerock.openidm.scheduler.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.JsonResource;
import org.forgerock.json.resource.JsonResourceAccessor;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.openidm.objset.ObjectSetContext;
import org.forgerock.openidm.objset.PreconditionFailedException;
import org.forgerock.openidm.quartz.impl.ExecutionException;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.scope.ScopeFactory;
import org.forgerock.openidm.script.Script;
import org.forgerock.openidm.script.ScriptException;
import org.forgerock.openidm.script.Scripts;
import org.forgerock.openidm.util.ConfigMacroUtil;
import org.forgerock.openidm.util.DateUtil;
import org.joda.time.DateTime;
import org.joda.time.ReadablePeriod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskScannerJob {
    private final static Logger logger = LoggerFactory.getLogger(TaskScannerJob.class);
    private final static DateUtil DATE_UTIL = DateUtil.getDateUtil("UTC");

    private TaskScannerContext context;
    private JsonResource router;
    private ScopeFactory scopeFactory;
    private Script script;

    public TaskScannerJob(TaskScannerContext context, JsonResource router, ScopeFactory scopeFactory)
            throws ExecutionException {
        this.context = context;
        this.router = router;
        this.scopeFactory = scopeFactory;

        JsonValue scriptValue = context.getScriptValue();
        if (!scriptValue.isNull()) {
            this.script = Scripts.newInstance(context.getScriptName(), scriptValue);
        } else {
            throw new ExecutionException("No valid script '" + scriptValue + "' configured in task scanner.");
        }
    }

    /**
     * Starts the task associated with a task scanner event.
     * This method may run synchronously or launch a new thread depending upon the settings in the TaskScannerContext
     * @return identifier associated with this task scan job
     * @throws ExecutionException
     */
    public String startTask() throws ExecutionException {
        if (context.getWaitForCompletion()) {
            performTask();
        } else {
            // Launch a new thread for the whole taskscan process
            final JsonValue threadContext = ObjectSetContext.get();
            Runnable command = new Runnable() {
                @Override
                public void run() {
                    try {
                        ObjectSetContext.push(threadContext);
                        performTask();
                    } catch (Exception ex) {
                        logger.warn("Taskscanner failed with unexpected exception", ex);
                    }
                }
            };
            new Thread(command).start();
            // Shouldn't need to keep ahold of this, I don't think? Can just start it and let it go
        }
        return context.getTaskScanID();
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
    private void performTask()
            throws ExecutionException {
        context.startJob();
        logger.info("Task {} started from {} with script {}",
                new Object[] { context.getTaskScanID(), context.getInvokerName(), context.getScriptName() });

        int numberOfThreads = context.getNumberOfThreads();

        JsonValue results;
        context.startQuery();
        try {
            results = fetchAllObjects();
        } catch (JsonResourceException e1) {
            throw new ExecutionException("Error during query", e1);
        }
        context.endQuery();
        Integer maxRecords = context.getMaxRecords();
        if (maxRecords == null) {
            context.setNumberOfTasksToProcess(results.size());
        } else {
            context.setNumberOfTasksToProcess(Math.min(results.size(), maxRecords));
        }
        logger.debug("TaskScan {} query results: {}", context.getInvokerName(), results.size());
        // TODO jump out early if it's empty?

        // Split and prune the result set according to our max and if we're synchronous or not
        List<JsonValue> resultSets = splitResultsOverThreads(results, numberOfThreads, maxRecords);
        logger.debug("Split result set into {} units", resultSets.size());

        final JsonValue threadContext = ObjectSetContext.get();
        ExecutorService fullTaskScanExecutor = Executors.newFixedThreadPool(numberOfThreads);
        List<Callable<Object>> todo = new ArrayList<Callable<Object>>();
        for (final JsonValue result : resultSets) {
            Runnable command = new Runnable() {
                @Override
                public void run() {
                    try {
                        ObjectSetContext.push(threadContext);
                        performTaskOverSet(result);
                    } catch (Exception ex) {
                        logger.warn("Taskscanner failed with unexpected exception", ex);
                    }
                }
            };
            todo.add(Executors.callable(command));
        }

        try {
            fullTaskScanExecutor.invokeAll(todo);
        } catch (InterruptedException e) {
            // Mark it interrupted
            context.interrupted();
            logger.warn("Task scan '" + context.getTaskScanID() + "' interrupted");
        }
        // Don't mark the job as completed if its been deactivated
        if (!context.isInactive()) {
            context.endJob();
        }

        logger.info("Task '{}' completed. Total time: {}ms. Query time: {}ms. Progress: {}",
                new Object[] { context.getTaskScanID(),
                context.getStatistics().getJobDuration(),
                context.getStatistics().getQueryDuration(),
                context.getProgress()
        });
    }

    private List<JsonValue> splitResultsOverThreads(JsonValue results, int numberOfThreads, Integer max) {
        List<List<Object>> resultSets = new ArrayList<List<Object>>();
        for (int i = 0; i < numberOfThreads; i++) {
            resultSets.add(new ArrayList<Object>());
        }

        int i = 0;
        for (JsonValue obj : results) {
            if (max != null && i >= max) {
                break;
            }
            resultSets.get(i % numberOfThreads).add(obj.getObject());
            i++;
        }

        List<JsonValue> jsonSets = new ArrayList<JsonValue>();
        for (List<Object> set : resultSets) {
            jsonSets.add(new JsonValue(set));
        }

        return jsonSets;
    }

    private void performTaskOverSet(JsonValue results)
                    throws ExecutionException {
        for (JsonValue input : results) {
            if (context.isCanceled()) {
                logger.info("Task '" + context.getTaskScanID() + "' cancelled. Terminating execution.");
                break; // Jump out quick since we've cancelled the job
            }
            // Check if this object has a STARTED time already
            JsonValue startTime = input.get(context.getStartField());
            String startTimeString = null;
            if (startTime != null) {
                startTimeString = startTime.asString();
                DateTime startedTime = DATE_UTIL.parseTimestamp(startTimeString);

                // Skip if the startTime + interval has not been passed
                ReadablePeriod period = context.getRecoveryTimeout();
                DateTime expirationDate = startedTime.plus(period);
                if (expirationDate.isAfterNow()) {
                    logger.debug("Object already started and has not expired. Started at: {}. Timeout: {}. Expires at: {}",
                            new Object[] {
                            DATE_UTIL.formatDateTime(startedTime),
                            period,
                            DATE_UTIL.formatDateTime(expirationDate)});
                    continue;
                }
            }

            try {
                claimAndExecScript(input, startTimeString);
            } catch (JsonResourceException e) {
                throw new ExecutionException("Error during claim and execution phase", e);
            }
        }
    }

    /**
     * Flatten a list of parameters and perform a query to fetch all objects from storage
     * @param id the identifier of the resource to query
     * @param params the parameters of the query
     * @return JsonValue containing a list of all the retrieved objects
     * @throws JsonResourceException
     */
    private JsonValue fetchAllObjects() throws JsonResourceException {
        JsonValue flatParams = flattenJson(context.getScanValue());
        ConfigMacroUtil.expand(flatParams);
        return performQuery(context.getObjectID(), flatParams);
    }

    /**
     * Performs a query on a resource and returns the result set
     * @param resourceID the identifier of the resource to query
     * @param params parameters to supply to the query
     * @return the set of results from the performed query
     * @throws JsonResourceException
     */
    private JsonValue performQuery(String resourceID, JsonValue params) throws JsonResourceException {
        JsonValue queryResults = null;
        queryResults = accessor().query(resourceID, params);
        return queryResults.get(QueryConstants.QUERY_RESULT);
    }

    /**
     * Performs a read on a resource and returns the result
     * @param resourceID the identifier of the resource to read
     * @return the results from the performed read
     * @throws JsonResourceException
     */
    private JsonValue performRead(String resourceID) throws JsonResourceException {
        JsonValue readResults = null;
        readResults = accessor().read(resourceID);
        return readResults;
    }

    /**
     * Adds an object to a JsonValue and performs an update
     * @param resourceID the resource identifier that the updated value belongs to
     * @param value value to perform the update with
     * @param path JsonPointer to the updated/added field
     * @param obj object to add to the field
     * @return the updated JsonValue
     * @throws JsonResourceException
     */
    private JsonValue updateValueWithObject(String resourceID, JsonValue value, JsonPointer path, Object obj) throws JsonResourceException {
        ensureJsonPointerExists(path, value);
        value.put(path, obj);
        return performUpdate(resourceID, value);
    }

    /**
     * Performs an update on a given resource with a supplied JsonValue
     * @param resourceID the resource identifier to perform the update on
     * @param value the object to update with
     * @return the updated object
     * @throws JsonResourceException
     */
    private JsonValue performUpdate(String resourceID, JsonValue value) throws JsonResourceException {
        String id = value.get("_id").required().asString();
        String fullID = retrieveFullID(resourceID, value);
        String rev = value.get("_rev").required().asString();

        accessor().update(fullID, rev, value);
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
        return retrieveFullID(resourceID, id);
    }

    /**
     * Constructs a full object ID from the supplied resourceID and the objectID
     * @param resourceID resource ID that the object originates from
     * @param objectID ID of some object
     * @return string indicating the full ID
     */
    private String retrieveFullID(String resourceID, String objectID) {
        return resourceID + '/' + objectID;
    }

    /**
     * Fetches an updated copy of some specified object from the given resource
     * @param resourceID the resource identifier to fetch an object from
     * @param value the value to retrieve an updated copy of
     * @return the updated value
     * @throws JsonResourceException
     */
    private JsonValue retrieveUpdatedObject(String resourceID, JsonValue value)
            throws JsonValueException, JsonResourceException {
        return retrieveObject(resourceID, value.get("_id").required().asString());
    }

    /**
     * Retrieves a specified object from a resource
     * @param resourceID the resource identifier to fetch the object from
     * @param id the identifier of the object to fetch
     * @return the object retrieved from the resource
     * @throws JsonResourceException
     */
    private JsonValue retrieveObject(String resourceID, String id) throws JsonResourceException {
        return performRead(retrieveFullID(resourceID, id));
    }

    private void claimAndExecScript(JsonValue input, String expectedStartDateStr)
            throws ExecutionException, JsonResourceException {
        String id = input.get("_id").required().asString();
        boolean claimedTask = false;
        boolean retryClaimTask = false;

        JsonPointer startField = context.getStartField();
        JsonPointer completedField = context.getCompletedField();
        String resourceID = context.getObjectID();

        JsonValue _input = input;
        do {
            try {
                retryClaimTask = false;
                _input = updateValueWithObject(resourceID, _input, startField, DATE_UTIL.now());
                logger.debug("Claimed task and updated StartField: {}", _input);
                claimedTask = true;
            } catch (PreconditionFailedException ex) {
                    // If the object changed since we queried, get the latest
                    // and check if it's still in a state we want to process the task.
                    _input = retrieveObject(resourceID, id);
                    String currentStartDateStr = _input.get(startField).asString();
                    String currentCompletedDateStr = _input.get(completedField).asString();
                    if (currentCompletedDateStr == null && (currentStartDateStr == null || currentStartDateStr.equals(expectedStartDateStr))) {
                        retryClaimTask = true;
                    } else {
                        // Someone else managed to update the started field first,
                        // claimed the task. Do not execute it here this run.
                        logger.debug("Task for {} {} was already claimed, ignore.", resourceID, id);
                    }
            }
        } while (retryClaimTask && !context.isCanceled());
        if (claimedTask) {
            execScript(_input);
        }
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
     * @throws JsonResourceException
     */
    private void execScript(JsonValue input)
            throws ExecutionException, JsonResourceException {
        if (script != null) {
            String resourceID = context.getObjectID();
            Map<String, Object> scope = newScope();
            scope.put("input", input.getObject());
            scope.put("objectID", retrieveFullID(resourceID, input));

            try {
                Object returnedValue = script.exec(scope);
                JsonValue _input = retrieveUpdatedObject(resourceID, input);
                logger.debug("After script execution: {}", _input);

                if (returnedValue == Boolean.TRUE) {
                   _input = updateValueWithObject(resourceID, _input, context.getCompletedField(), DATE_UTIL.now());
                   context.getStatistics().taskSucceded();
                   logger.debug("Updated CompletedField: {}", _input);
                } else {
                    context.getStatistics().taskFailed();
                }

            } catch (ScriptException se) {
                context.getStatistics().taskFailed();
                String msg = context.getScriptName() + " script invoked by " +
                        context.getInvokerName() + " encountered exception";
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
            }
            refObj = refObj.get(p);
        }
    }

    private JsonResourceAccessor accessor() {
        return new JsonResourceAccessor(router, ObjectSetContext.get());
    }

    private Map<String, Object> newScope() {
        return scopeFactory.newInstance(ObjectSetContext.get());
    }

}
