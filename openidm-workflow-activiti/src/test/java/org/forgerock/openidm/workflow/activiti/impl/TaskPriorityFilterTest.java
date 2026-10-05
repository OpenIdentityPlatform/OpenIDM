/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.openidm.workflow.activiti.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import org.activiti.engine.HistoryService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.task.TaskQuery;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.workflow.activiti.ActivitiConstants;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.services.context.SecurityContext;
import org.testng.annotations.Test;

/**
 * Tests that a non-numeric {@code priority} filter on the task queries is rejected as a bad request.
 */
public class TaskPriorityFilterTest {

    @Test
    public void taskQueryRejectsNonNumericPriority() throws Exception {
        ProcessEngine processEngine = mock(ProcessEngine.class);
        TaskService taskService = mock(TaskService.class);
        when(processEngine.getTaskService()).thenReturn(taskService);
        when(taskService.createTaskQuery()).thenReturn(mock(TaskQuery.class));

        assertBadPriority(new TaskInstanceResource(processEngine));
    }

    @Test
    public void taskHistoryQueryRejectsNonNumericPriority() throws Exception {
        ProcessEngine processEngine = mock(ProcessEngine.class);
        HistoryService historyService = mock(HistoryService.class);
        when(processEngine.getHistoryService()).thenReturn(historyService);
        when(historyService.createHistoricTaskInstanceQuery()).thenReturn(mock(HistoricTaskInstanceQuery.class));

        assertBadPriority(new TaskInstanceHistoryResource(processEngine));
    }

    private static void assertBadPriority(CollectionResourceProvider resource) throws Exception {
        Context context = new SecurityContext(new RootContext(), "user", null);
        QueryRequest request = Requests.newQueryRequest("")
                .setQueryId(ActivitiConstants.QUERY_FILTERED)
                .setAdditionalParameter(ActivitiConstants.ACTIVITI_PRIORITY, "high");

        try {
            resource.queryCollection(context, request, mock(QueryResourceHandler.class)).getOrThrow();
            fail("expected BadRequestException");
        } catch (ResourceException e) {
            assertTrue(e instanceof BadRequestException, "expected BadRequestException, got " + e);
            assertTrue(e.getMessage().contains(ActivitiConstants.ACTIVITI_PRIORITY), e.getMessage());
            assertTrue(e.getMessage().contains("high"), e.getMessage());
        }
    }
}
