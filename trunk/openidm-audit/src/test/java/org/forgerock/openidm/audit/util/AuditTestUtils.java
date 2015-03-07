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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openidm.audit.util;

import org.forgerock.openidm.audit.impl.AuditServiceImpl;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.forgerock.openidm.audit.impl.AuditServiceImpl.*;
import static org.forgerock.openidm.audit.util.ActivityLogger.ACTION;
import static org.forgerock.openidm.audit.util.ActivityLogger.ACTIVITY_ID;
import static org.forgerock.openidm.audit.util.ActivityLogger.AFTER;
import static org.forgerock.openidm.audit.util.ActivityLogger.BEFORE;
import static org.forgerock.openidm.audit.util.ActivityLogger.CHANGED_FIELDS;
import static org.forgerock.openidm.audit.util.ActivityLogger.MESSAGE;
import static org.forgerock.openidm.audit.util.ActivityLogger.OBJECT_ID;
import static org.forgerock.openidm.audit.util.ActivityLogger.PARENT_ACTION_ID;
import static org.forgerock.openidm.audit.util.ActivityLogger.PASSWORD_CHANGED;
import static org.forgerock.openidm.audit.util.ActivityLogger.REQUESTER;
import static org.forgerock.openidm.audit.util.ActivityLogger.REVISION;
import static org.forgerock.openidm.audit.util.ActivityLogger.ROOT_ACTION_ID;
import static org.forgerock.openidm.audit.util.ActivityLogger.STATUS;
import static org.forgerock.openidm.audit.util.ActivityLogger.TIMESTAMP;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AuditTestUtils {

    public static final Map<String, Object> TEST_MAP = new HashMap<String, Object>();
    public static final List<String> TEST_LIST = new ArrayList<String>();

    public static final String FLATTENED_TEST_MAP = "{\"key\" : \"value\"}";
    public static final String FLATTENED_TEST_LIST = "[\"value1\", \"value2\"]";

    public enum Operation {
        READ, CREATE
    }

    static {
        TEST_MAP.put("key", "value");
        TEST_LIST.add("value1");
        TEST_LIST.add("value2");
    }

    public static final Map<String, Object> createLogEntry(final String type, final Operation operation) {
        Map<String, Object> entry;

        if (AuditServiceImpl.TYPE_RECON.equals(type)) {
            entry = createReconEntryForReadOperation("_id", "action", "actionId", "ambiguousTargetObjectIds",
                    "entryType", "exception", "mapping", "message", FLATTENED_TEST_MAP, "reconAction", "reconciling",
                    "reconId", "rootActionId", "situation", "sourceObjectId", "status", "targetObjectId", "timestamp");
            if (Operation.CREATE.equals(operation)) {
                entry.put("messageDetail", TEST_MAP);
            }
        } else if (AuditServiceImpl.TYPE_ACCESS.equals(type)) {
            entry = createAccessEntryForReadOperation("_id", "action", "ip", "principal", FLATTENED_TEST_LIST, "status",
                    "timestamp", "userid");
            if (Operation.CREATE.equals(operation)) {
                entry.put("roles", TEST_LIST);
            }
        } else if (AuditServiceImpl.TYPE_SYNC.equals(type)) {
            entry = createSyncEntryForReadOperation("_id", "timestamp", "rootActionId", "status", "message",
                    FLATTENED_TEST_MAP, "exception", "action", "actionId", "situation", "sourceObjectId",
                    "targetObjectId", "mapping");
            if (Operation.CREATE.equals(operation)) {
                entry.put("messageDetail", TEST_MAP);
            }
        } else if (AuditServiceImpl.TYPE_ACTIVITY.equals(type)) {
            entry =  createActivityEntryForReadOperation("_id", "action", "activityId", FLATTENED_TEST_MAP,
                    FLATTENED_TEST_MAP, FLATTENED_TEST_LIST, "message", "objectId", "parentActionId", "passwordChanged",
                    "requester", "rev", "rootActionId", "status", "timestamp");
            if (Operation.CREATE.equals(operation)) {
                entry.put("after", TEST_MAP);
                entry.put("before", TEST_MAP);
                entry.put("changeFields", TEST_LIST);
            }
        } else {
            assertThat(false);
            return null;
        }
        return entry;
    }

    public static final Map<String, Object> createReconEntryForReadOperation(
        final String id,
        final String action,
        final String actionId,
        final String ambiguousTargetObjectIds,
        final String entryType,
        final String exception,
        final String mapping,
        final String message,
        final String messageDetail, // flattened Map
        final String reconAction,
        final String reconciling,
        final String reconId,
        final String rootActionId,
        final String situation,
        final String sourceObjectId,
        final String status,
        final String targetObjectId,
        final String timestamp
    ) {
        final Map<String, Object> entry = new HashMap<String, Object>();
        entry.put(LOG_ID, id);
        entry.put(RECON_LOG_ENTRY_TYPE, entryType);
        entry.put(RECON_LOG_TIMESTAMP, timestamp);
        entry.put(RECON_LOG_RECON_ID, reconId);
        entry.put(RECON_LOG_RECON_ACTION, reconAction);
        entry.put(RECON_LOG_ROOT_ACTION_ID, rootActionId);
        entry.put(RECON_LOG_STATUS, status);
        entry.put(RECON_LOG_MESSAGE, message);
        entry.put(RECON_LOG_MESSAGE_DETAIL, messageDetail);
        entry.put(RECON_LOG_EXCEPTION, exception);
        if (AuditConstants.RECON_LOG_ENTRY_TYPE_RECON_ENTRY.equals(entryType)) {
            // recon entry
            entry.put(RECON_LOG_ACTION_ID, actionId);
            entry.put(RECON_LOG_ACTION, action);
            entry.put(RECON_LOG_AMBIGUOUS_TARGET_OBJECT_IDS, ambiguousTargetObjectIds);
            entry.put(RECON_LOG_RECONCILING, reconciling);
            entry.put(RECON_LOG_SITUATION, situation);
            entry.put(RECON_LOG_SOURCE_OBJECT_ID, sourceObjectId);
            entry.put(RECON_LOG_TARGET_OBJECT_ID, targetObjectId);
        }
        entry.put(RECON_LOG_MAPPING, mapping);
        return entry;
    }

    public static final Map<String, Object> createActivityEntryForReadOperation(
        final String id,
        final String action,
        final String activityId,
        final String after, //flattened map
        final String before, //flattened map
        final String changedFields, //flattened list
        final String message,
        final String objectId,
        final String parentActionId,
        final String passwordChanged,
        final String requester,
        final String rev,
        final String rootActionId,
        final String status,
        final String timestamp
    ) {
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put(LOG_ID, id);
        entry.put(ACTIVITY_ID, activityId);
        entry.put(TIMESTAMP, timestamp);
        entry.put(ACTION, action);
        entry.put(MESSAGE, message);
        entry.put(OBJECT_ID, objectId);
        entry.put(REVISION, rev);
        entry.put(ROOT_ACTION_ID, rootActionId);
        entry.put(PARENT_ACTION_ID, parentActionId);
        entry.put(REQUESTER, requester);
        entry.put(BEFORE, before);
        entry.put(AFTER, after);
        entry.put(STATUS, status);
        entry.put(CHANGED_FIELDS, changedFields);
        entry.put(PASSWORD_CHANGED, passwordChanged);
        return entry;
    }

    public static final Map<String, Object> createAccessEntryForReadOperation(
        final String id,
        final String action,
        final String ip,
        final String principal,
        final String roles, //flattened list
        final String status,
        final String timestamp,
        final String userid
    ) {
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put(LOG_ID, id);
        entry.put(ACCESS_LOG_ACTION, action);
        entry.put(ACCESS_LOG_IP, ip);
        entry.put(ACCESS_LOG_PRINCIPAL, principal);
        entry.put(ACCESS_LOG_ROLES, roles);
        entry.put(ACCESS_LOG_STATUS, status);
        entry.put(ACCESS_LOG_TIMESTAMP, timestamp);
        entry.put(ACCESS_LOG_USERID, userid);
        return entry;
    }

    public static final Map<String, Object> createSyncEntryForReadOperation(
        final String id,
        final String timestamp,
        final String rootActionId,
        final String status,
        final String message,
        final String messageDetail, //flattened map
        final String exception,
        final String action,
        final String actionId,
        final String situation,
        final String sourceObjectId,
        final String targetObjectId,
        final String mapping
    ) {
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put(LOG_ID, id);
        entry.put(SYNC_LOG_TIMESTAMP, timestamp);
        entry.put(SYNC_LOG_ROOT_ACTION_ID, rootActionId);
        entry.put(SYNC_LOG_STATUS, status);
        entry.put(SYNC_LOG_MESSAGE, message);
        entry.put(SYNC_LOG_MESSAGE_DETAIL, messageDetail);
        entry.put(SYNC_LOG_EXCEPTION, exception);
        entry.put(SYNC_LOG_ACTION, action);
        entry.put(SYNC_LOG_ACTION_ID, actionId);
        entry.put(SYNC_LOG_SITUATION, situation);
        entry.put(SYNC_LOG_SOURCE_OBJECT_ID, sourceObjectId);
        entry.put(SYNC_LOG_TARGET_OBJECT_ID, targetObjectId);
        entry.put(SYNC_LOG_MAPPING, mapping);
        return entry;
    }

    public static void checkLogEntry(final String type, final Map<String, Object> result, final Operation operation) {
        if (AuditServiceImpl.TYPE_RECON.equals(type)) {
            checkReconEntry(result, operation);
        } else if (AuditServiceImpl.TYPE_ACCESS.equals(type)) {
            checkAccessEntry(result, operation);
        } else if (AuditServiceImpl.TYPE_SYNC.equals(type)) {
            checkSyncEntry(result, operation);
        } else if (AuditServiceImpl.TYPE_ACTIVITY.equals(type)) {
            checkActivityEntry(result, operation);
        } else {
            assertThat(false);
        }
    }

    private static void checkReconEntry(final Map<String, Object> result, final Operation operation) {
        assertThat(result.get(LOG_ID).equals(LOG_ID));
        assertThat(result.get(RECON_LOG_ENTRY_TYPE).equals(RECON_LOG_ENTRY_TYPE));
        assertThat(result.get(RECON_LOG_TIMESTAMP).equals(RECON_LOG_TIMESTAMP));
        assertThat(result.get(RECON_LOG_RECON_ID).equals(RECON_LOG_RECON_ID));
        assertThat(result.get(RECON_LOG_RECON_ACTION).equals(RECON_LOG_RECON_ACTION));
        assertThat(result.get(RECON_LOG_ROOT_ACTION_ID).equals(RECON_LOG_ROOT_ACTION_ID));
        assertThat(result.get(RECON_LOG_STATUS).equals(RECON_LOG_STATUS));
        assertThat(result.get(RECON_LOG_MESSAGE).equals(RECON_LOG_MESSAGE));
        if (Operation.READ.equals(operation)) {
            assertThat(result.get(RECON_LOG_MESSAGE_DETAIL).equals(AuditTestUtils.TEST_MAP));
        } else {
            assertThat(result.get(RECON_LOG_MESSAGE_DETAIL).equals(AuditTestUtils.FLATTENED_TEST_MAP));
        }
        assertThat(result.get(RECON_LOG_EXCEPTION).equals(RECON_LOG_EXCEPTION));
        if (AuditConstants.RECON_LOG_ENTRY_TYPE_RECON_ENTRY.equals(RECON_LOG_ENTRY_TYPE)) {
            // recon entry
            assertThat(result.get(RECON_LOG_ACTION_ID).equals(RECON_LOG_ACTION_ID));
            assertThat(result.get(RECON_LOG_ACTION).equals(RECON_LOG_ACTION));
            assertThat(result.get(RECON_LOG_AMBIGUOUS_TARGET_OBJECT_IDS).equals(RECON_LOG_AMBIGUOUS_TARGET_OBJECT_IDS));
            assertThat(result.get(RECON_LOG_RECONCILING).equals(RECON_LOG_RECONCILING));
            assertThat(result.get(RECON_LOG_SITUATION).equals(RECON_LOG_SITUATION));
            assertThat(result.get(RECON_LOG_SOURCE_OBJECT_ID).equals(RECON_LOG_SOURCE_OBJECT_ID));
            assertThat(result.get(RECON_LOG_TARGET_OBJECT_ID).equals(RECON_LOG_TARGET_OBJECT_ID));
        }
        assertThat(result.get(RECON_LOG_MAPPING).equals(RECON_LOG_MAPPING));
    }

    private static void checkAccessEntry(final Map<String, Object> result, final Operation operation) {
        assertThat(result.get(LOG_ID).equals(LOG_ID));
        assertThat(result.get(ACCESS_LOG_ACTION).equals(ACCESS_LOG_ACTION));
        assertThat(result.get(ACCESS_LOG_IP).equals(ACCESS_LOG_IP));
        assertThat(result.get(ACCESS_LOG_PRINCIPAL).equals(ACCESS_LOG_PRINCIPAL));
        if (Operation.READ.equals(operation)) {
            assertThat(result.get(ACCESS_LOG_ROLES).equals(AuditTestUtils.TEST_LIST));
        } else {
            assertThat(result.get(ACCESS_LOG_ROLES).equals(AuditTestUtils.FLATTENED_TEST_LIST));
        }
        assertThat(result.get(ACCESS_LOG_STATUS).equals(ACCESS_LOG_STATUS));
        assertThat(result.get(ACCESS_LOG_TIMESTAMP).equals(ACCESS_LOG_TIMESTAMP));
        assertThat(result.get(ACCESS_LOG_USERID).equals(ACCESS_LOG_USERID));
    }

    private static void checkActivityEntry(final Map<String, Object> result, final Operation operation) {
        assertThat(result.get(LOG_ID).equals(LOG_ID));
        assertThat(result.get(ACTIVITY_ID).equals(ACTIVITY_ID));
        assertThat(result.get(TIMESTAMP).equals(TIMESTAMP));
        assertThat(result.get(ACTION).equals(ACTION));
        assertThat(result.get(MESSAGE).equals(MESSAGE));
        assertThat(result.get(OBJECT_ID).equals(OBJECT_ID));
        assertThat(result.get(REVISION).equals(REVISION));
        assertThat(result.get(ROOT_ACTION_ID).equals(ROOT_ACTION_ID));
        assertThat(result.get(PARENT_ACTION_ID).equals(PARENT_ACTION_ID));
        assertThat(result.get(REQUESTER).equals(REQUESTER));
        assertThat(result.get(STATUS).equals(STATUS));
        if (Operation.READ.equals(operation)) {
            assertThat(result.get(BEFORE).equals(AuditTestUtils.TEST_MAP));
            assertThat(result.get(AFTER).equals(AuditTestUtils.TEST_MAP));
            assertThat(result.get(CHANGED_FIELDS).equals(AuditTestUtils.TEST_LIST));
        } else {
            assertThat(result.get(BEFORE).equals(AuditTestUtils.FLATTENED_TEST_MAP));
            assertThat(result.get(AFTER).equals(AuditTestUtils.FLATTENED_TEST_MAP));
            assertThat(result.get(CHANGED_FIELDS).equals(AuditTestUtils.FLATTENED_TEST_LIST));
        }
        assertThat(result.get(PASSWORD_CHANGED).equals(PASSWORD_CHANGED));
    }

    private static void checkSyncEntry(final Map<String, Object> result, final Operation operation) {
        assertThat(result.get(LOG_ID).equals(LOG_ID));
        assertThat(result.get(SYNC_LOG_TIMESTAMP).equals(SYNC_LOG_TIMESTAMP));
        assertThat(result.get(SYNC_LOG_ROOT_ACTION_ID).equals(SYNC_LOG_ROOT_ACTION_ID));
        assertThat(result.get(SYNC_LOG_STATUS).equals(SYNC_LOG_STATUS));
        assertThat(result.get(SYNC_LOG_MESSAGE).equals(SYNC_LOG_MESSAGE));
        if (Operation.READ.equals(operation)) {
            assertThat(result.get(SYNC_LOG_MESSAGE_DETAIL).equals(AuditTestUtils.TEST_MAP));
        } else {
            assertThat(result.get(SYNC_LOG_MESSAGE_DETAIL).equals(AuditTestUtils.FLATTENED_TEST_MAP));
        }
        assertThat(result.get(SYNC_LOG_EXCEPTION).equals(SYNC_LOG_EXCEPTION));
        assertThat(result.get(SYNC_LOG_ACTION).equals(SYNC_LOG_ACTION));
        assertThat(result.get(SYNC_LOG_ACTION_ID).equals(SYNC_LOG_ACTION_ID));
        assertThat(result.get(SYNC_LOG_SITUATION).equals(SYNC_LOG_SITUATION));
        assertThat(result.get(SYNC_LOG_SOURCE_OBJECT_ID).equals(SYNC_LOG_SOURCE_OBJECT_ID));
        assertThat(result.get(SYNC_LOG_TARGET_OBJECT_ID).equals(SYNC_LOG_TARGET_OBJECT_ID));
        assertThat(result.get(SYNC_LOG_MAPPING).equals(SYNC_LOG_MAPPING));
    }
}
