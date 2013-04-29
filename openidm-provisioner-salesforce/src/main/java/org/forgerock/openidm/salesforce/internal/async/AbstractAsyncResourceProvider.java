/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.salesforce.internal.async;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.json.resource.SimpleJsonResource;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.objset.ServiceUnavailableException;
import org.forgerock.openidm.salesforce.internal.SalesforceConnection;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sforce.async.AsyncApiException;
import com.sforce.async.BatchInfo;
import com.sforce.async.BatchResult;
import com.sforce.async.BulkConnection;
import com.sforce.async.JobInfo;
import com.sforce.async.Result;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public abstract class AbstractAsyncResourceProvider extends SimpleJsonResource {

    /**
     * Setup logging for the {@link AbstractAsyncResourceProvider}.
     */
    protected final static Logger logger = LoggerFactory
            .getLogger(AbstractAsyncResourceProvider.class);

    protected final static ObjectMapper mapper = new ObjectMapper();

    private final SalesforceConnection sfconnection;

    private final BulkConnection connection;

    protected AbstractAsyncResourceProvider(final SalesforceConnection connection) {
        this.sfconnection = connection;
        try {
            this.connection = new BulkConnection(sfconnection.getConnectorConfig("async"));
            this.connection.getConfig().setTraceMessage(logger.isTraceEnabled());
            try {
                if (this.connection.getConfig().isTraceMessage()) {
                    this.connection.getConfig().setTraceFile(
                            IdentityServer.getFileForWorkingPath("logs/SF-async.log")
                                    .getAbsolutePath());
                }
            } catch (FileNotFoundException e) {
                this.connection.getConfig().setTraceMessage(false);
            }
        } catch (AsyncApiException e) {
            logger.error("", e);
            throw new ComponentException("Failed to initiate the Async service", e);
        }
    }

    protected BulkConnection getConnection(boolean refresh) throws JsonResourceException,
            AsyncApiException {
        if (refresh) {
            if (!sfconnection.refreshAccessToken(connection.getConfig())) {
                throw new ServiceUnavailableException("Session is expired and can not be renewed");
            }
        }
        return connection;
    }

    public static JsonResourceException adapt(final Throwable t) {
        int resourceResultCode;
        Map<String, Object> detail = null;
        try {
            throw t;
        } catch (final JsonResourceException e) {
            return e;
        } catch (final JsonValueException e) {
            resourceResultCode = JsonResourceException.BAD_REQUEST;
        } catch (AsyncApiException e) {
            detail = new HashMap<String, Object>(2);
            detail.put("exceptionMessage", e.getExceptionMessage());
            detail.put("exceptionCode", e.getExceptionCode().name());

            resourceResultCode = JsonResourceException.INTERNAL_ERROR;

            switch (e.getExceptionCode()) {
            case ClientInputError: {
                resourceResultCode = JsonResourceException.BAD_REQUEST;
                break;
            }
            case InvalidJobState: {
                /*
                 * Closing already aborted Job not allowed
                 */
                resourceResultCode = JsonResourceException.CONFLICT;
                break;
            }
            case InvalidJob:
            case InvalidBatch:
            case InvalidEntity: {
                resourceResultCode = JsonResourceException.NOT_FOUND;
                break;
            }
            }
        } catch (final Throwable tmp) {
            resourceResultCode = JsonResourceException.INTERNAL_ERROR;
        }
        JsonResourceException e = new JsonResourceException(resourceResultCode, t.getMessage(), t);
        e.setDetail(detail);
        return e;
    }

    static JsonValue fromBatchInfo(BatchInfo batchInfo) {
        JsonValue result = new JsonValue(mapper.convertValue(batchInfo, Map.class));
        result.put(ServerConstants.OBJECT_PROPERTY_ID, batchInfo.getId());
        return result;
    }

    static JsonValue fromJobInfo(JobInfo jobInfo) {
        JsonValue result = new JsonValue(mapper.convertValue(jobInfo, Map.class));
        result.put(ServerConstants.OBJECT_PROPERTY_ID, jobInfo.getId());
        return result;
    }

    static JsonValue fromResult(Result batchResult) {
        JsonValue result = new JsonValue(new HashMap<String, Object>());
        result.put("success", batchResult.isSuccess());
        result.put("created", batchResult.isCreated());
        result.put("id", batchResult.getId());
        result.put("errors", mapper.convertValue(batchResult.getErrors(), List.class));
        result.put(ServerConstants.OBJECT_PROPERTY_ID, batchResult.getId());
        return result;
    }

    static JsonValue fromBatchResult(BatchResult batchResult) {
        JsonValue result = new JsonValue(mapper.convertValue(batchResult, Map.class));
        return result;
    }
}
