/*
 *  The contents of this file are subject to the terms of the Common Development and
 *  Distribution License (the License). You may not use this file except in compliance with the
 *  License.
 *
 *  You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 *  specific language governing permission and limitations under the License.
 *
 *  When distributing Covered Software, include this CDDL Header Notice in each file and include
 *  the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 *  Header, with the fields enclosed by brackets [] replaced by your own identifying
 *  information: "Portions copyright [year] [name of copyright owner]".
 *
 *  Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openidm.sync.impl;

import org.forgerock.audit.events.AuditEventBuilder;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.audit.util.Status;
import org.forgerock.openidm.sync.ReconAction;

/**
 * Base implementation of AuditEventBuilder for IDM synchronization related events; currently sync and recon.
 *
 * @param <T> the type of the builder
 */
abstract class AbstractSyncAuditEventBuilder<T extends AbstractSyncAuditEventBuilder<T>>
        extends AuditEventBuilder<T> {

    public static final String ACTION = "action";
    public static final String EXCEPTION = "exception";
    public static final String LINK_QUALIFIER = "linkQualifier";
    public static final String MAPPING = "mapping";
    public static final String MESSAGE = "message";
    public static final String MESSAGE_DETAIL = "messageDetail";
    public static final String SITUATION = "situation";
    public static final String SOURCE_OBJECT_ID = "sourceObjectId";
    public static final String STATUS = "status";
    public static final String TARGET_OBJECT_ID = "targetObjectId";
    public static final String TIMESTAMP = "timestamp";

    /**
     * Sets the Action of the event: CREATE, UPDATE, LINK, etc.
     *
     * @param action
     * @return
     * @see #ACTION
     */
    public T action(ReconAction action) {
        if (null != action) {
            jsonValue.put(ACTION, action.name());
        }
        return self();
    }

    /**
     * Sets the exception field of the event.
     *
     * @param exception
     * @return
     * @see #EXCEPTION
     */
    public T exception(Exception exception) {
        jsonValue.put(EXCEPTION, exception);
        return self();
    }

    /**
     * Sets the linkQualifier of the event.
     *
     * @param linkQualifier
     * @return
     * @see #LINK_QUALIFIER
     */
    public T linkQualifier(String linkQualifier) {
        jsonValue.put(LINK_QUALIFIER, linkQualifier);
        return self();
    }

    /**
     * Sets the mapping of the event.
     *
     * @param mapping
     * @return
     * @see #MAPPING
     */
    public T mapping(String mapping) {
        jsonValue.put(MAPPING, mapping);
        return self();
    }

    /**
     * Sets the message of the event.
     *
     * @param message
     * @return
     * @see #MESSAGE
     */
    public T message(String message) {
        jsonValue.put(MESSAGE, message);
        return self();
    }

    /**
     * Sets the messageDetail of the event.
     *
     * @param details
     * @return
     * @see #MESSAGE_DETAIL
     */
    public T messageDetail(JsonValue details) {
        if (null != details) {
            jsonValue.put(MESSAGE_DETAIL, details.getObject());
        }
        return self();
    }

    /**
     * Saves the situation on the event.
     *
     * @param situation
     * @return
     * @see #SITUATION
     */
    public T situation(Situation situation) {
        if (null != situation) {
            jsonValue.put(SITUATION, situation.name());
        }
        return self();
    }

    /**
     * Saves the sourceObjectId on the event.
     *
     * @param sourceObjectId
     * @return
     * @see #SOURCE_OBJECT_ID
     */
    public T sourceObjectId(String sourceObjectId) {
        jsonValue.put(SOURCE_OBJECT_ID, sourceObjectId);
        return self();
    }

    /**
     * Saves the status of the sync on the event.
     *
     * @param status
     * @return
     * @see #STATUS
     */
    public T status(Status status) {
        if (null != status) {
            jsonValue.put(STATUS, status.name());
        }
        return self();
    }

    /**
     * Saves the targetObjectId on the event.
     *
     * @param targetObjectId
     * @return
     * @see #TARGET_OBJECT_ID
     */
    public T targetObjectId(String targetObjectId) {
        jsonValue.put(TARGET_OBJECT_ID, targetObjectId);
        return self();
    }

    /**
     * Saves the timestamp on the event.
     *
     * @param timestamp
     * @return
     * @see #TIMESTAMP
     */
    public T timestamp(String timestamp) {
        jsonValue.put(TIMESTAMP, timestamp);
        return self();
    }

}
