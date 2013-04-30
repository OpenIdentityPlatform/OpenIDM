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

import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.salesforce.internal.SalesforceConnection;
import org.forgerock.openidm.util.ResourceUtil;

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
public class AsyncBatchResourceProvider extends AbstractAsyncResourceProvider implements
        CollectionResourceProvider {

    public AsyncBatchResourceProvider(final SalesforceConnection connection) {
        super(connection);
    }

    protected String getJobId(ServerContext context) throws ResourceException {
        Map<String, String> variables = ResourceUtil.getUriTemplateVariables(context);
        if (null != variables && variables.containsKey("jobId")) {
            return variables.get("jobId");
        }
        throw new ForbiddenException("Direct access without Router to this service is forbidden.");
    }

    @Override
    public void createInstance(final ServerContext context, final CreateRequest request,
            final ResultHandler<Resource> handler) {
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

            for (JsonValue sobject : request.getContent()) {
                batchRequest.addSObject(build(sobject));
            }
            BatchInfo batchInfo = batchRequest.completeRequest();
            handler.handleResult(new Resource(batchInfo.getId(), null, fromBatchInfo(batchInfo)));

        } catch (Throwable t) {
            handler.handleError(adapt(t));
        }
    }

    @Override
    public void readInstance(final ServerContext context, final String resourceId,
            final ReadRequest request, final ResultHandler<Resource> handler) {
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
            handler.handleResult(new Resource(batchInfo.getId(), null, fromBatchInfo(batchInfo)));
        } catch (Throwable t) {
            handler.handleError(adapt(t));
        }
    }

    @Override
    public void queryCollection(final ServerContext context, final QueryRequest request,
            final QueryResultHandler handler) {
        try {

            if (ServerConstants.QUERY_ALL_IDS.equalsIgnoreCase(request.getQueryId())) {
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
                    handler.handleResource(new Resource(batchInfo.getId(), null,
                            fromBatchInfo(batchInfo)));
                }
                handler.handleResult(new QueryResult());
            } else {
                handler.handleError(new BadRequestException("Only query-all-ids is supported"));
            }
        } catch (Throwable t) {
            handler.handleError(adapt(t));
        }
    }

    @Override
    public void actionCollection(final ServerContext context, final ActionRequest request,
            final ResultHandler<JsonValue> handler) {
        final ResourceException e =
                new NotSupportedException("Action operations are not supported");
        handler.handleError(e);
    }

    @Override
    public void actionInstance(final ServerContext context, final String resourceId,
            final ActionRequest request, final ResultHandler<JsonValue> handler) {
        final ResourceException e =
                new NotSupportedException("Action operations are not supported");
        handler.handleError(e);
    }

    @Override
    public void deleteInstance(final ServerContext context, final String resourceId,
            final DeleteRequest request, final ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Delete operations are not supported");
        handler.handleError(e);
    }

    @Override
    public void patchInstance(final ServerContext context, final String resourceId,
            final PatchRequest request, final ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Patch operations are not supported");
        handler.handleError(e);
    }

    @Override
    public void updateInstance(final ServerContext context, final String resourceId,
            final UpdateRequest request, final ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Update operations are not supported");
        handler.handleError(e);
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

}
