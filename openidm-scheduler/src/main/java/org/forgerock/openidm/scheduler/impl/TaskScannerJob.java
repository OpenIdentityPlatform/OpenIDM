/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2012-2014 ForgeRock AS. All Rights Reserved
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

import javax.script.ScriptException;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.quartz.impl.ExecutionException;
import org.forgerock.openidm.util.ConfigMacroUtil;
import org.forgerock.openidm.util.DateUtil;
import org.forgerock.openidm.util.RequestUtil;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.joda.time.DateTime;
import org.joda.time.ReadablePeriod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskScannerJob {
    private final static Logger logger = LoggerFactory.getLogger(TaskScannerJob.class);
    private final static DateUtil DATE_UTIL = DateUtil.getDateUtil("UTC");

    private ConnectionFactory connectionFactory;
    private TaskScannerContext taskScannerContext;

    public TaskScannerJob(ConnectionFactory connectionFactory, TaskScannerContext context)
            throws ExecutionException {
        this.connectionFactory = connectionFactory;
        this.taskScannerContext = context;
    }

    /**
     * Starts the task associated with a task scanner event.
     * This method may run synchronously or launch a new thread depending upon the settings in the TaskScannerContext
     * @return identifier associated with this task scan job
     * @throws ExecutionException
     */
    public String startTask() throws ExecutionException {
        int numberOfThreads = taskScannerContext.getNumberOfThreads();
        final ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        
        if (taskScannerContext.getWaitForCompletion()) {
            try {
                performTask(executor);
            } catch (ExecutionException ex) {
                throw ex;
            } finally {
                executor.shutdown();
            }
        } else {
            // Launch a new thread for the whole taskscan process
            final ServerContext context = taskScannerContext.getContext();
            Runnable command = new Runnable() {
                @Override
                public void run() {
                    try {
                        performTask(executor);
                    } catch (Exception ex) {
                        logger.warn("Taskscanner failed with unexpected exception", ex);
                    } finally {
                        executor.shutdown();
                    }
                }
            };
            new Thread(command).start();
            // Shouldn't need to keep ahold of this, I don't think? Can just start it and let it go
        }
        return taskScannerContext.getTaskScanID();
    }

    /**
     * Performs the task associated with the task scanner event.
     * Runs the query and executes the script across each resulting object.
     *
     * @param executor ExecutorService in which to invoke this task.
     * @throws ExecutionException
     */
    private void performTask(ExecutorService executor)
            throws ExecutionException {
        taskScannerContext.startJob();
        logger.info("Task {} started from {} with script {}",
                new Object[] { taskScannerContext.getTaskScanID(), taskScannerContext.getInvokerName(), taskScannerContext.getScriptName() });

        JsonValue results;
        taskScannerContext.startQuery();
        try {
            results = fetchAllObjects();
        } catch (ResourceException e1) {
            throw new ExecutionException("Error during query", e1);
        }
        taskScannerContext.endQuery();
        Integer maxRecords = taskScannerContext.getMaxRecords();
        if (maxRecords == null) {
            taskScannerContext.setNumberOfTasksToProcess(results.size());
        } else {
            taskScannerContext.setNumberOfTasksToProcess(Math.min(results.size(), maxRecords));
        }
        logger.debug("TaskScan {} query results: {}", taskScannerContext.getInvokerName(), results.size());
        // TODO jump out early if it's empty?

        // Split and prune the result set according to our max and if we're synchronous or not
        List<JsonValue> resultSets = splitResultsOverThreads(results, taskScannerContext.getNumberOfThreads(), maxRecords);
        logger.debug("Split result set into {} units", resultSets.size());

        List<Callable<Object>> todo = new ArrayList<Callable<Object>>();
        for (final JsonValue result : resultSets) {
            Runnable command = new Runnable() {
                @Override
                public void run() {
                    try {
                        performTaskOverSet(result);
                    } catch (Exception ex) {
                        logger.warn("Taskscanner failed with unexpected exception", ex);
                    }
                }
            };
            todo.add(Executors.callable(command));
        }

        try {
            executor.invokeAll(todo);
        } catch (InterruptedException e) {
            // Mark it interrupted
            taskScannerContext.interrupted();
            logger.warn("Task scan '" + taskScannerContext.getTaskScanID() + "' interrupted");
        }
        // Don't mark the job as completed if its been deactivated
        if (!taskScannerContext.isInactive()) {
            taskScannerContext.endJob();
        }

        logger.info("Task '{}' completed. Total time: {}ms. Query time: {}ms. Progress: {}",
                new Object[] { taskScannerContext.getTaskScanID(),
                taskScannerContext.getStatistics().getJobDuration(),
                taskScannerContext.getStatistics().getQueryDuration(),
                taskScannerContext.getProgress()
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
            if (taskScannerContext.isCanceled()) {
                logger.info("Task '" + taskScannerContext.getTaskScanID() + "' cancelled. Terminating execution.");
                break; // Jump out quick since we've cancelled the job
            }
            // Check if this object has a STARTED time already
            JsonValue startTime = input.get(taskScannerContext.getStartField());
            String startTimeString = null;
            if (startTime != null && !startTime.isNull()) {
                startTimeString = startTime.asString();
                DateTime startedTime = DATE_UTIL.parseTimestamp(startTimeString);

                // Skip if the startTime + interval has not been passed
                ReadablePeriod period = taskScannerContext.getRecoveryTimeout();
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
            } catch (ResourceException e) {
                throw new ExecutionException("Error during claim and execution phase", e);
            }
        }
    }

    /**
     * Flatten a list of parameters and perform a query to fetch all objects from storage.
     *
     * @return JsonValue containing a list of all the retrieved objects
     * @throws ResourceException
     */
    private JsonValue fetchAllObjects() throws ResourceException {
        JsonValue flatParams = flattenJson(taskScannerContext.getScanValue());
        ConfigMacroUtil.expand(flatParams);
        return performQuery(taskScannerContext.getObjectID(), flatParams);
    }

    /**
     * Performs a query on a resource and returns the result set
     * @param resourceID the identifier of the resource to query
     * @param params parameters to supply to the query
     * @return the set of results from the performed query
     * @throws ResourceException
     */
    private JsonValue performQuery(String resourceID, JsonValue params) throws ResourceException {
        final JsonValue queryResults = new JsonValue(new ArrayList<Map<String, Object>>());;

        QueryRequest request = RequestUtil.buildQueryRequestFromParameterMap(resourceID, params.asMap());
        connectionFactory.getConnection().query(taskScannerContext.getContext(), request, new QueryResultHandler() {
            @Override
            public void handleError(ResourceException error) {
                // Ignore
            }

            @Override
            public boolean handleResource(Resource resource) {
                queryResults.add(resource.getContent().getObject());
                return true;
            }

            @Override
            public void handleResult(QueryResult result) {
                // Ignore
            }
        });
        return queryResults;
    }

    /**
     * Performs a read on a resource and returns the result
     * @param resourceID the identifier of the resource to read
     * @return the results from the performed read
     * @throws ResourceException
     */
    private JsonValue performRead(String resourceID) throws ResourceException {
        JsonValue readResults = null;

        readResults = connectionFactory.getConnection().read(taskScannerContext.getContext(), Requests.newReadRequest(resourceID)).getContent();
        return readResults;
    }

    /**
     * Adds an object to a JsonValue and performs an update
     * @param resourceID the resource identifier that the updated value belongs to
     * @param value value to perform the update with
     * @param path JsonPointer to the updated/added field
     * @param obj object to add to the field
     * @return the updated JsonValue
     * @throws ResourceException
     */
    private JsonValue updateValueWithObject(String resourceID, JsonValue value, JsonPointer path, Object obj) throws ResourceException {
        ensureJsonPointerExists(path, value);
        value.put(path, obj);
        return performUpdate(resourceID, value);
    }

    /**
     * Performs an update on a given resource with a supplied JsonValue
     * @param resourceID the resource identifier to perform the update on
     * @param value the object to update with
     * @return the updated object
     * @throws ResourceException
     */
    private JsonValue performUpdate(String resourceID, JsonValue value) throws ResourceException {
        String id = value.get("_id").required().asString();
        String fullID = retrieveFullID(resourceID, value);
        String rev = value.get("_rev").required().asString();
        UpdateRequest updateRequest = Requests.newUpdateRequest(fullID, value);
        updateRequest.setRevision(rev);

        connectionFactory.getConnection().update(taskScannerContext.getContext(), updateRequest);
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
     * @throws ResourceException
     */
    private JsonValue retrieveUpdatedObject(String resourceID, JsonValue value)
            throws JsonValueException, ResourceException {
        return retrieveObject(resourceID, value.get("_id").required().asString());
    }

    /**
     * Retrieves a specified object from a resource
     * @param resourceID the resource identifier to fetch the object from
     * @param id the identifier of the object to fetch
     * @return the object retrieved from the resource
     * @throws ResourceException
     */
    private JsonValue retrieveObject(String resourceID, String id) throws ResourceException {
        return performRead(retrieveFullID(resourceID, id));
    }

    private void claimAndExecScript(JsonValue input, String expectedStartDateStr)
            throws ExecutionException, ResourceException {
        String id = input.get("_id").required().asString();
        boolean claimedTask = false;
        boolean retryClaimTask = false;

        JsonPointer startField = taskScannerContext.getStartField();
        JsonPointer completedField = taskScannerContext.getCompletedField();
        String resourceID = taskScannerContext.getObjectID();

        JsonValue _input = input;
        do {
            try {
                retryClaimTask = false;
                _input = updateValueWithObject(resourceID, _input, startField, DATE_UTIL.now());
                _input = updateValueWithObject(resourceID, _input, completedField, null);
                logger.debug("Claimed task and updated StartField: {}", _input);
                claimedTask = true;
            } catch (PreconditionFailedException ex) {
                    // If the object changed since we queried, get the latest
                    // and check if it's still in a state we want to process the task.
                    _input = retrieveObject(resourceID, id);
                    String currentStartDateStr = (_input.get(startField) == null)  ? null : _input.get(startField).asString();
                    String currentCompletedDateStr = (_input.get(completedField) == null)  ? null : _input.get(completedField).asString();
                    if (currentCompletedDateStr == null && (currentStartDateStr == null || currentStartDateStr.equals(expectedStartDateStr))) {
                        retryClaimTask = true;
                    } else {
                        // Someone else managed to update the started field first,
                        // claimed the task. Do not execute it here this run.
                        logger.debug("Task for {} {} was already claimed, ignore.", resourceID, id);
                    }
            }
        } while (retryClaimTask && !taskScannerContext.isCanceled());
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
     * @param input value to input to the script
     * @throws ExecutionException
     * @throws ResourceException
     */
    private void execScript(JsonValue input)
            throws ExecutionException, ResourceException {
        ScriptEntry script = taskScannerContext.getScriptEntry();

        if (script != null) {
            String resourceID = taskScannerContext.getObjectID();
            ServerContext context = taskScannerContext.getContext();

            try {
                Script scope = script.getScript(context);
                scope.put("input", input.getObject());
                scope.put("objectID", retrieveFullID(resourceID, input));

                Object returnedValue = scope.eval();
                JsonValue _input = retrieveUpdatedObject(resourceID, input);
                logger.debug("After script execution: {}", _input);

                if (returnedValue == Boolean.TRUE) {
                   _input = updateValueWithObject(resourceID, _input, taskScannerContext.getCompletedField(), DATE_UTIL.now());
                   taskScannerContext.getStatistics().taskSucceded();
                   logger.debug("Updated CompletedField: {}", _input);
                } else {
                    taskScannerContext.getStatistics().taskFailed();
                }

            } catch (ScriptException se) {
                taskScannerContext.getStatistics().taskFailed();
                String msg = taskScannerContext.getScriptName() + " script invoked by " +
                        taskScannerContext.getInvokerName() + " encountered exception";
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
}
