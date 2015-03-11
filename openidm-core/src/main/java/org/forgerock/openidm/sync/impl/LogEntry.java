/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.sync.impl;

import java.util.Date;
import java.util.HashMap;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Context;
import org.forgerock.openidm.sync.impl.ObjectMapping.Status;
import org.forgerock.openidm.sync.impl.ObjectMapping.SyncOperation;
import org.forgerock.openidm.util.DateUtil;

/**
  An audit log entry representation
 * 
 */
class LogEntry {

    /**
     * The name of the mapping associated with this entry
     */
    private String mappingName;

    /**
     * The root invocation context 
     */
    protected final Context rootContext;
    
    /** 
     * The timestamp of the entry 
     */
    protected Date timestamp;
    
    /** 
     * The synchronization operation 
     */
    protected final SyncOperation op;
    
    /** 
     * The Status of the operation (SUCCESS or FAILURE) 
     */
    protected Status status = ObjectMapping.Status.SUCCESS;
    
    /** 
     * The ID of the source object.
     */ 
    protected String sourceObjectId;
    
    /** 
     * The ID of the targetObject 
     */
    protected String targetObjectId;
    
    /** 
     * A message containing additional information.
     */
    protected String message;
    
    /** 
     * A JsonValue object containing additional information. 
     */
    protected JsonValue messageDetail;
    
    /** 
     * An Exception, if one occurred
     */
    protected Exception exception;
    
    /**
     * An action ID.
     */
    protected String actionId;

    /**
     * The DateUtil for formatting the timestamp
     */
    protected DateUtil dateUtil;

    /**
     * The name of the link qualifier.
     */
    protected String linkQualifier;

    /**
     * Construct a log entry for the provided {@link SyncOperation} and mapping name.
     *
     * @param op the sync operation
     * @param mappingName the mapping name
     * @param rootContext the root context
     * @param dateUtil a date-formatting object
     */
    protected LogEntry(SyncOperation op, String mappingName, Context rootContext, DateUtil dateUtil) {
        this.op = op;
        this.mappingName = mappingName;
        this.rootContext = rootContext;
        this.dateUtil = dateUtil;
    }
    
    /**
     * Returns a JsonValue representing the log entry.
     * 
     * @return the JsonValue representation.
     */
    protected JsonValue toJsonValue() {
        JsonValue jv = new JsonValue(new HashMap<String, Object>());
        jv.put("mapping", mappingName);
        jv.put("rootActionId", rootContext.getId());
        jv.put("sourceObjectId", sourceObjectId);
        jv.put("targetObjectId", targetObjectId);
        jv.put("linkQualifier", linkQualifier);
        jv.put("timestamp", dateUtil.formatDateTime(timestamp));
        jv.put("situation", ((op == null || op.situation == null) ? null : op.situation.toString()));
        jv.put("action", ((op == null || op.action == null) ? null : op.action.toString()));
        jv.put("status", (status == null ? null : status.toString()));
        jv.put("message", message);
        jv.put("messageDetail", messageDetail);
        jv.put("actionId", actionId);
        jv.put("exception", exception);
        return jv;
    }
}
