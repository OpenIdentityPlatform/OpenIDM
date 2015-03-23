/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock Inc. All rights reserved.
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
 * "Portions copyright [year] [name of copyright owner]"
 */
package org.forgerock.openidm.sync.impl;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Context;
import org.forgerock.openidm.util.DateUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

import java.util.Date;
import java.util.UUID;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertTrue;


public class LogEntryTest {

    @Test
    public void toJsonValueTest() {

        final String MAPPING_NAME = "mapping-name";
        ObjectMapping.SyncOperation syncOperationMock = mock(ObjectMapping.SyncOperation.class);
        Context contextMock = mock(Context.class);
        DateUtil dateUtil = DateUtil.getDateUtil("UTC");

        final String testId = UUID.randomUUID().toString();
        final String actionId = UUID.randomUUID().toString();

        when(contextMock.getId()).thenReturn(testId);

        LogEntry logEntry = new LogEntry(syncOperationMock, MAPPING_NAME, contextMock, dateUtil);

        logEntry.sourceObjectId = "sourceObjectId";
        logEntry.targetObjectId = "targetObjectId";
        logEntry.dateUtil = dateUtil;
        logEntry.timestamp = new Date();
        logEntry.linkQualifier = "default";
        logEntry.op.situation = Situation.LINK_ONLY;
        logEntry.status = ObjectMapping.Status.SUCCESS;
        logEntry.message = "message";
        logEntry.messageDetail = json(object(field("message", "testing log entry")));
        logEntry.actionId =  actionId;
        logEntry.exception = new Exception();

        JsonValue logEntryVal = logEntry.toJsonValue();

        assertEquals(logEntryVal.get("mapping").asString(), MAPPING_NAME);
        assertEquals(logEntryVal.get("rootActionId").asString(), testId);
        assertEquals(logEntryVal.get("sourceObjectId").asString(), logEntry.sourceObjectId);
        assertEquals(logEntryVal.get("targetObjectId").asString(), logEntry.targetObjectId);
        assertEquals(logEntryVal.get("linkQualifier").asString(), "default");
        assertTrue(logEntryVal.get("timestamp") != null);
        assertEquals(logEntryVal.get("situation").asString(), Situation.LINK_ONLY.toString());
        assertEquals(logEntryVal.get("action").asString(), null);
        assertEquals(logEntryVal.get("status").asString(), ObjectMapping.Status.SUCCESS.toString());
        assertEquals(logEntryVal.get("message").asString(), logEntry.message);
        assertEquals(logEntryVal.get("messageDetail").get("message").asString(), "testing log entry");
        assertEquals(logEntryVal.get("actionId").asString(), actionId);
        assertTrue(logEntryVal.get("exception") != null );

    }
}
