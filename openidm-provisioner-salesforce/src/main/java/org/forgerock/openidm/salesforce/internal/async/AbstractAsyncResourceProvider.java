/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 ForgeRock AS. All Rights Reserved
 */

package org.forgerock.openidm.salesforce.internal.async;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.salesforce.internal.SalesforceConnection;
import org.forgerock.openidm.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sforce.async.AsyncApiException;
import com.sforce.async.BatchInfo;
import com.sforce.async.BatchResult;
import com.sforce.async.BulkConnection;
import com.sforce.async.JobInfo;
import com.sforce.async.Result;

/**
 * The AbstractAsyncResourceProvider is an adapter for the Salesforce Bulk API.
 *
 * @author Laszlo Hordos
 */
public abstract class AbstractAsyncResourceProvider {

    /**
     * Setup logging for the {@link AbstractAsyncResourceProvider}.
     */
    protected final static Logger logger = LoggerFactory
            .getLogger(AbstractAsyncResourceProvider.class);

    protected final static ObjectMapper mapper = JsonUtil.build();

    private final SalesforceConnection sfconnection;

    private BulkConnection connection;

    protected AbstractAsyncResourceProvider(final SalesforceConnection connection) {
        this.sfconnection = connection;
    }

    private void init() throws ResourceException {
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
            throw new ServiceUnavailableException("Failed to initiate the Async service", e);
        }
    }

    protected BulkConnection getConnection(boolean refresh) throws ResourceException,
            AsyncApiException {
        if (null == connection) {
            synchronized (this) {
                if (null == connection) {
                    init();
                }
            }
        }
        if (refresh) {
            if (!sfconnection.refreshAccessToken(connection.getConfig())) {
                throw new ServiceUnavailableException("Session is expired and can not be renewed");
            }
        }
        return connection;
    }

    public static ResourceException adapt(final Throwable t) {
        int resourceResultCode;
        JsonValue detail = null;
        try {
            throw t;
        } catch (final ResourceException e) {
            return e;
        } catch (final JsonValueException e) {
            resourceResultCode = ResourceException.BAD_REQUEST;
        } catch (AsyncApiException e) {
            detail = new JsonValue(new HashMap<String, Object>(2));
            detail.put("exceptionMessage", e.getExceptionMessage());
            detail.put("exceptionCode", e.getExceptionCode().name());

            resourceResultCode = ResourceException.INTERNAL_ERROR;

            switch (e.getExceptionCode()) {
            case ClientInputError: {
                resourceResultCode = ResourceException.BAD_REQUEST;
                break;
            }
            case InvalidJobState: {
                /*
                 * Closing already aborted Job not allowed
                 */
                resourceResultCode = ResourceException.CONFLICT;
                break;
            }
            case InvalidJob:
            case InvalidBatch:
            case InvalidEntity: {
                resourceResultCode = ResourceException.NOT_FOUND;
                break;
            }
            }
        } catch (final Throwable tmp) {
            resourceResultCode = ResourceException.INTERNAL_ERROR;
        }
        return ResourceException.getException(resourceResultCode, t.getMessage(), t).setDetail(
                detail);
    }

    static JsonValue fromBatchInfo(BatchInfo batchInfo) {
        JsonValue result = new JsonValue(mapper.convertValue(batchInfo, Map.class));
        result.put(Resource.FIELD_CONTENT_ID, batchInfo.getId());
        return result;
    }

    static JsonValue fromJobInfo(JobInfo jobInfo) {
        JsonValue result = new JsonValue(mapper.convertValue(jobInfo, Map.class));
        result.put(Resource.FIELD_CONTENT_ID, jobInfo.getId());
        return result;
    }

    static JsonValue fromResult(Result batchResult) {
        JsonValue result = new JsonValue(new HashMap<String, Object>());
        result.put("success", batchResult.isSuccess());
        result.put("created", batchResult.isCreated());
        result.put("id", batchResult.getId());
        result.put("errors", mapper.convertValue(batchResult.getErrors(), List.class));
        result.put(Resource.FIELD_CONTENT_ID, batchResult.getId());
        return result;
    }

    static JsonValue fromBatchResult(BatchResult batchResult) {
        JsonValue result = new JsonValue(mapper.convertValue(batchResult, Map.class));
        return result;
    }
}
