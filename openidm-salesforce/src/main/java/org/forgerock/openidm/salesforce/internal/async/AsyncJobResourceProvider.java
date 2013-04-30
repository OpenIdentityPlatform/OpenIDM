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
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.salesforce.internal.SalesforceConnection;

import com.sforce.async.AsyncApiException;
import com.sforce.async.AsyncExceptionCode;
import com.sforce.async.ConcurrencyMode;
import com.sforce.async.ContentType;
import com.sforce.async.JobInfo;
import com.sforce.async.JobStateEnum;
import com.sforce.async.OperationEnum;

/**
 * An AsyncJobResourceProvider handles all the requests for {code}async/job/{id}
 * {code} {code}(\Qasync/job/\E).*{code} {code}(\Q/\E){code}
 * {code}(\Q/\E([^/]+)\Q/\E){code}
 * 
 * @author Laszlo Hordos
 */
public class AsyncJobResourceProvider extends AbstractAsyncResourceProvider implements
        CollectionResourceProvider {

    public AsyncJobResourceProvider(final SalesforceConnection connection) {
        super(connection);
    }

    @Override
    public void createInstance(final ServerContext context, final CreateRequest request,
            final ResultHandler<Resource> handler) {
        try {
            if (request.getNewResourceId() == null) {

                JsonValue content = request.getContent();
                JobInfo.Builder builder = new JobInfo.Builder();
                builder.object(content.get("object").required().asString());
                OperationEnum operation =
                        content.get("operation").required().asEnum(OperationEnum.class);
                builder.operation(operation);
                if (OperationEnum.upsert.equals(operation)) {
                    builder.externalIdFieldName(content.get("externalIdFieldName").required()
                            .asString());
                } else {
                    builder.externalIdFieldName(content.get("externalIdFieldName").asString());
                }
                builder.assignmentRuleId(content.get("assignmentRuleId").asString());
                if (content.isDefined("concurrencyMode")) {
                    builder.concurrencyMode(content.get("concurrencyMode").asEnum(
                            ConcurrencyMode.class));
                }
                builder.contentType(ContentType.XML);

                builder.state(content.get("state").asEnum(JobStateEnum.class));

                JobInfo jobInfo = null;
                try {
                    jobInfo = getConnection(false).createJob(new JobInfo(builder));
                } catch (AsyncApiException e) {
                    if (e.getExceptionCode().equals(AsyncExceptionCode.InvalidSessionId)) {
                        jobInfo = getConnection(true).createJob(new JobInfo(builder));
                    } else {
                        throw e;
                    }
                }
                handler.handleResult(new Resource(jobInfo.getId(), null, fromJobInfo(jobInfo)));

            } else {
                handler.handleError(new BadRequestException(
                        "Create Job with client assigned Id is not supported"));
            }

        } catch (Throwable t) {
            handler.handleError(adapt(t));
        }
    }

    @Override
    public void actionInstance(final ServerContext context, final String resourceId,
            final ActionRequest request, final ResultHandler<JsonValue> handler) {
        try {
            if ("close".equalsIgnoreCase(request.getAction())) {
                JobInfo jobInfo = null;
                try {
                    jobInfo = getConnection(false).closeJob(resourceId);
                } catch (AsyncApiException e) {
                    if (e.getExceptionCode().equals(AsyncExceptionCode.InvalidSessionId)) {
                        jobInfo = getConnection(true).closeJob(resourceId);
                    } else {
                        throw e;
                    }
                }
                handler.handleResult(fromJobInfo(jobInfo));
            } else if ("abort".equalsIgnoreCase(request.getAction())) {
                JobInfo jobInfo = null;
                try {
                    jobInfo = getConnection(false).abortJob(resourceId);
                } catch (AsyncApiException e) {
                    if (e.getExceptionCode().equals(AsyncExceptionCode.InvalidSessionId)) {
                        jobInfo = getConnection(true).abortJob(resourceId);
                    } else {
                        throw e;
                    }
                }
                handler.handleResult(fromJobInfo(jobInfo));
            } else {
                handler.handleError(new BadRequestException("Unsupported action: "
                        + request.getAction()));
            }
        } catch (Throwable t) {
            handler.handleError(adapt(t));
        }
    }

    @Override
    public void readInstance(final ServerContext context, final String resourceId,
            final ReadRequest request, final ResultHandler<Resource> handler) {
        try {
            JobInfo jobInfo = null;
            try {
                jobInfo = getConnection(false).getJobStatus(resourceId);
            } catch (AsyncApiException e) {
                if (e.getExceptionCode().equals(AsyncExceptionCode.InvalidSessionId)) {
                    jobInfo = getConnection(true).getJobStatus(resourceId);
                } else {
                    throw e;
                }
            }
            handler.handleResult(new Resource(jobInfo.getId(), null, fromJobInfo(jobInfo)));
        } catch (Throwable t) {
            handler.handleError(adapt(t));
        }
    }

    @Override
    public void updateInstance(final ServerContext context, final String resourceId,
            final UpdateRequest request, final ResultHandler<Resource> handler) {
        try {
            JsonValue content = request.getNewContent();
            JobInfo.Builder builder = new JobInfo.Builder();
            builder.object(content.get("object").asString());
            builder.operation(content.get("operation").asEnum(OperationEnum.class));
            if (content.isDefined("concurrencyMode")) {
                builder.concurrencyMode(content.get("concurrencyMode")
                        .asEnum(ConcurrencyMode.class));
            }
            builder.contentType(ContentType.XML);
            builder.externalIdFieldName(content.get("externalIdFieldName").asString());
            builder.state(content.get("state").asEnum(JobStateEnum.class));
            builder.id(resourceId);

            JobInfo jobInfo = null;
            try {
                jobInfo = getConnection(false).updateJob(new JobInfo(builder));
            } catch (AsyncApiException e) {
                if (e.getExceptionCode().equals(AsyncExceptionCode.InvalidSessionId)) {
                    jobInfo = getConnection(true).updateJob(new JobInfo(builder));
                } else {
                    throw e;
                }
            }
            handler.handleResult(new Resource(jobInfo.getId(), null, fromJobInfo(jobInfo)));
        } catch (Throwable t) {
            handler.handleError(adapt(t));
        }
    }

    @Override
    public void actionCollection(final ServerContext context, final ActionRequest request,
            final ResultHandler<JsonValue> handler) {
        final ResourceException e =
                new NotSupportedException("Actions are not supported for resource collection");
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
    public void queryCollection(final ServerContext context, final QueryRequest request,
            final QueryResultHandler handler) {
        final ResourceException e = new NotSupportedException("Query operations are not supported");
        handler.handleError(e);
    }

}
