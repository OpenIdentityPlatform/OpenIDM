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

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.openidm.salesforce.internal.ResultHandler;
import org.forgerock.openidm.salesforce.internal.SalesforceConnection;
import org.forgerock.openidm.salesforce.internal.ServerContext;

import com.sforce.async.AsyncApiException;
import com.sforce.async.AsyncExceptionCode;
import com.sforce.async.BatchInfo;
import com.sforce.async.BatchInfoList;
import com.sforce.async.BatchRequest;
import com.sforce.async.JobInfo;
import com.sforce.async.SObject;

/**
 * An AsyncBatchResourceProvider handles all the requests for
 * {code}async/job/{jobId}/batch/{id} {code}
 * {code}(\Qasync/job/\E([^/]+)\Q/batch/\E).*{code}
 * 
 * @author Laszlo Hordos
 */
public class AsyncBatchResourceProvider extends AbstractAsyncResourceProvider {

    public AsyncBatchResourceProvider(final SalesforceConnection connection) {
        super(connection);
    }

    protected String getJobId(ServerContext context) throws JsonResourceException {
        return context.getMatcher().group(1);
    }

    public void createInstance(final ServerContext context, final JsonValue content,
            final ResultHandler handler) {
        try {
            JobInfo jobInfo = new JobInfo();
            jobInfo.setId(getJobId(context));
            BatchRequest batchRequest = null;
            try {
                batchRequest = getConnection(false).createBatch(jobInfo);
            } catch (AsyncApiException e) {
                if (e.getExceptionCode().equals(AsyncExceptionCode.InvalidSessionId)) {
                    batchRequest = getConnection(true).createBatch(jobInfo);
                } else {
                    throw e;
                }
            }

            for (JsonValue sobject : content) {
                batchRequest.addSObject(build(sobject));
            }
            BatchInfo batchInfo = batchRequest.completeRequest();
            handler.handleResult(fromBatchInfo(batchInfo));

        } catch (Throwable t) {
            handler.handleError(adapt(t));
        }
    }

    public void readInstance(final ServerContext context, final String resourceId,
            final ResultHandler handler) {
        try {
            BatchInfo batchInfo = null;
            try {
                batchInfo = getConnection(false).getBatchInfo(getJobId(context), resourceId);
            } catch (AsyncApiException e) {
                if (e.getExceptionCode().equals(AsyncExceptionCode.InvalidSessionId)) {
                    batchInfo = getConnection(true).getBatchInfo(getJobId(context), resourceId);
                } else {
                    throw e;
                }
            }
            handler.handleResult(fromBatchInfo(batchInfo));
        } catch (Throwable t) {
            handler.handleError(adapt(t));
        }
    }

    public void queryCollection(final ServerContext context, final String queryId,
            final ResultHandler handler) {
        try {

            if ("query-all-ids".equalsIgnoreCase(queryId)) {
                BatchInfoList batchInfoList = null;
                try {
                    batchInfoList = getConnection(false).getBatchInfoList(getJobId(context));
                } catch (AsyncApiException e) {
                    if (e.getExceptionCode().equals(AsyncExceptionCode.InvalidSessionId)) {
                        batchInfoList = getConnection(true).getBatchInfoList(getJobId(context));
                    } else {
                        throw e;
                    }
                }

                for (BatchInfo batchInfo : batchInfoList.getBatchInfo()) {
                    handler.handleResource(fromBatchInfo(batchInfo));
                }
            } else {
                handler.handleError(new JsonResourceException(JsonResourceException.BAD_REQUEST,
                        "Only query-all-ids is supported"));
            }
        } catch (Throwable t) {
            handler.handleError(adapt(t));
        }
    }

    SObject build(final JsonValue from) {
        SObject result = new SObject();
        for (String key : from.keys()) {
            JsonValue value = from.get(key);
            if (value.isString()) {
                result.setField(key, value.asString());
            } else if (value.isMap()) {
                result.setFieldReference(key, build(value));
            } else {
                throw new JsonValueException(value, "Expecting String,Map values only");
            }
        }
        return result;
    }

    @Override
    protected JsonValue create(JsonValue request) throws JsonResourceException {
        final ServerContext context = ServerContext.get();
        final ResultHandler handler = new ResultHandler();
        JsonValue content = request.get("value");
        createInstance(context, content, handler);
        return handler.getResult();
    }

    @Override
    protected JsonValue read(JsonValue request) throws JsonResourceException {
        final ServerContext context = ServerContext.get();
        if (context.getMatcher().groupCount() == 3) {
            final ResultHandler handler = new ResultHandler();
            String id = context.getMatcher().group(2);
            readInstance(context, id, handler);
            return handler.getResult();
        } else {
            throw new JsonResourceException(JsonResourceException.BAD_REQUEST,
                    "Read collection is not supported");
        }
    }

    @Override
    protected JsonValue query(JsonValue request) throws JsonResourceException {
        final ServerContext context = ServerContext.get();
        if (context.getMatcher().groupCount() == 2) {
            final ResultHandler handler = new ResultHandler();
            String queryId = request.get("params").get("_queryId").required().asString();
            queryCollection(context, queryId, handler);
            return handler.getResult();
        } else {
            throw new JsonResourceException(JsonResourceException.BAD_REQUEST,
                    "Query instance is not supported");
        }
    }
}
