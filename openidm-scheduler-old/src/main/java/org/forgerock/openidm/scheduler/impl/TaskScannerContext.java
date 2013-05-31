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

import java.util.LinkedHashMap;
import java.util.Map;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openidm.util.ConfigMacroUtil;
import org.joda.time.Days;
import org.joda.time.ReadablePeriod;

// TODO: cache components that are fetched frequently
public class TaskScannerContext {

    enum TaskScannerState {
        INITIALIZED,
        ACTIVE,
        COMPLETED,
        CANCELLED,
        ERROR
    }

    private String invokerName;
    private String scriptName;
    private JsonValue params;
    private ServerContext context;
    private boolean canceled = false;
    private TaskScannerStatistic statistics;
    private TaskScannerState state;

    public TaskScannerContext(String invokerName, String scriptName, JsonValue params, ServerContext context) {
        this.invokerName = invokerName;
        this.scriptName = scriptName;
        this.params = params;
        this.context = context;
        this.statistics = new TaskScannerStatistic();
        this.state = TaskScannerState.INITIALIZED;
    }

    public void startJob() {
        state = TaskScannerState.ACTIVE;
        statistics.jobStart();
    }

    public void endJob() {
        state = TaskScannerState.COMPLETED;
        statistics.jobEnd();
    }

    public void startQuery() {
        statistics.queryStart();
    }

    public void endQuery() {
        statistics.queryEnd();
    }

    public void cancel() {
        state = TaskScannerState.CANCELLED;
        this.canceled = true;
    }

    public void interrupted() {
        state = TaskScannerState.ERROR;
    }

    public boolean isStarted() {
        return state == TaskScannerState.ACTIVE;
    }

    public boolean isCompleted() {
        return state == TaskScannerState.COMPLETED;
    }

    public boolean hasErorr() {
        return state == TaskScannerState.ERROR;
    }

    public boolean isInactive() {
        return !isStarted();
    }

    public boolean isCanceled() {
        return this.canceled;
    }

    public String getState() {
        return state.toString();
    }

    public void setNumberOfTasksToProcess(int number) {
        statistics.setNumberOfTasksToProcess(number);
    }

    public ServerContext getContext() {
        return this.context;
    }

    public JsonValue getParams() {
        return this.params;
    }

    public String getInvokerName() {
        return this.invokerName;
    }

    public String getScriptName() {
        return this.scriptName;
    }

    public String getTaskScanID() {
        return context.getId();
    }

    /**
     * Retrieve the timeout period from the supplied config
     * @param value JsonValue configuration containing "recovery/timeout"
     * @return the timeout period taken from the config
     */
    public ReadablePeriod getRecoveryTimeout() {
        String timeoutStr = getScanValue().get("recovery").get("timeout").asString();

        // Default to a 0-Day period if there's nothing to add
        if (timeoutStr == null) {
            return Days.days(0);
        }

        return ConfigMacroUtil.getTimePeriod(timeoutStr);
    }

    public Integer getMaxRecords() {
        return params.get("maxRecords").asInteger();
    }

    public JsonValue getScriptValue() {
        return params.get("task").expect(Map.class).get("script").expect(Map.class);
    }

    public JsonValue getScanValue() {
        return params.get("scan").expect(Map.class);
    }

    public String getObjectID() {
        return getScanValue().get("object").required().asString();
    }

    public JsonPointer getStartField() {
        return getTaskStatePointer(getScanValue(), "started");
    }

    public JsonPointer getCompletedField() {
        return getTaskStatePointer(getScanValue(), "completed");
    }

    public boolean getWaitForCompletion() {
        JsonValue waitParam = params.get("waitForCompletion").defaultTo("false");
        Boolean waitForCompletion = Boolean.FALSE;
        if (waitParam.isBoolean()) {
            waitForCompletion = waitParam.asBoolean();
        } else {
            waitForCompletion = Boolean.parseBoolean(waitParam.asString());
        }
        return waitForCompletion.booleanValue();
    }

    public int getNumberOfThreads() {
        JsonValue numParams = params.get("numberOfThreads").defaultTo(10);
        return numParams.asInteger();
    }

    public TaskScannerStatistic getStatistics() {
        return this.statistics;
    }

    public Map<String, Object> getProgress() {
        Map<String, Object> progress = new LinkedHashMap<String, Object>();
        progress.put("state", state);
        progress.put("processed", statistics.getNumberOfTasksProcessed());
        progress.put("total", statistics.getNumberOfTasksToProcess());
        progress.put("successes", statistics.getNumberOfTasksSucceeded());
        progress.put("failures", statistics.getNumberOfTasksFailed());
        return progress;
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
}
