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

import java.util.concurrent.atomic.AtomicInteger;

public class TaskScannerStatistic {

    private long jobStartTime;
    private long jobEndTime;
    private long queryStartTime;
    private long queryEndTime;
    private int numberToProcess = 0;

    // Note: These should be the only ones used during the thread executions
    private AtomicInteger numSuccessful;
    private AtomicInteger numFailed;

    public TaskScannerStatistic() {
        numSuccessful = new AtomicInteger(0);
        numFailed = new AtomicInteger(0);
    }

    public void jobStart() {
        jobStartTime = System.currentTimeMillis();
    }

    public void jobEnd() {
        jobEndTime = System.currentTimeMillis();
    }

    public long getJobDuration() {
        return jobEndTime - jobStartTime;
    }

    public long getJobStartTime() {
        return jobStartTime;
    }

    public long getJobEndTime() {
        return jobEndTime;
    }

    public void queryStart() {
        queryStartTime = System.currentTimeMillis();
    }

    public void queryEnd() {
        queryEndTime = System.currentTimeMillis();
    }

    public long getQueryDuration() {
        return queryEndTime - queryStartTime;
    }

    public void taskSucceded() {
        numSuccessful.incrementAndGet();
    }

    public void taskFailed() {
        numFailed.incrementAndGet();
    }

    public int getNumberOfTasksProcessed() {
        return numSuccessful.get() + numFailed.get();
    }

    public int getNumberOfTasksSucceeded() {
        return numSuccessful.get();
    }

    public int getNumberOfTasksFailed() {
        return numFailed.get();
    }

    public int getNumberOfTasksToProcess() {
        return numberToProcess;
    }

    public int getNumberOfTasksRemaining() {
        return numberToProcess - getNumberOfTasksProcessed();
    }

    public void setNumberOfTasksToProcess(int numberToProcess) {
        this.numberToProcess = numberToProcess;
    }
}
