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
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.openidm.salesforce.internal.ResultHandler;
import org.forgerock.openidm.salesforce.internal.SalesforceConnection;
import org.forgerock.openidm.salesforce.internal.ServerContext;

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
public class AsyncJobResourceProvider extends AbstractAsyncResourceProvider {

    public AsyncJobResourceProvider(final SalesforceConnection connection) {
        super(connection);
    }

    public void createInstance(final ServerContext context, final JsonValue content,
            final ResultHandler handler) {
        try {
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
                builder.concurrencyMode(content.get("concurrencyMode")
                        .asEnum(ConcurrencyMode.class));
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
            handler.handleResult(fromJobInfo(jobInfo));

        } catch (Throwable t) {
            handler.handleError(adapt(t));
        }
    }

    public void actionInstance(final ServerContext context, final String resourceId,
            final String action, final ResultHandler handler) {
        try {
            if ("close".equalsIgnoreCase(action)) {
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
            } else if ("abort".equalsIgnoreCase(action)) {
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
                handler.handleError(new JsonResourceException(JsonResourceException.BAD_REQUEST,
                        "Unsupported action: " + action));
            }
        } catch (Throwable t) {
            handler.handleError(adapt(t));
        }
    }

    public void readInstance(final ServerContext context, final String resourceId,
            final ResultHandler handler) {
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
            handler.handleResult(fromJobInfo(jobInfo));
        } catch (Throwable t) {
            handler.handleError(adapt(t));
        }
    }

    public void updateInstance(final ServerContext context, final String resourceId,
            final JsonValue content, final ResultHandler handler) {
        try {
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
            handler.handleResult(fromJobInfo(jobInfo));
        } catch (Throwable t) {
            handler.handleError(adapt(t));
        }
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
        if (context.getMatcher().groupCount() == 2) {
            final ResultHandler handler = new ResultHandler();
            String id = context.getMatcher().group(1);
            readInstance(context, id, handler);
            return handler.getResult();
        } else {
            throw new JsonResourceException(JsonResourceException.BAD_REQUEST,
                    "Read collection is not supported");
        }
    }

    @Override
    protected JsonValue action(JsonValue request) throws JsonResourceException {
        final ServerContext context = ServerContext.get();
        if (context.getMatcher().groupCount() == 2) {
            final ResultHandler handler = new ResultHandler();
            String id = context.getMatcher().group(1);
            String action = request.get("params").get("_action").required().asString();
            actionInstance(context, id, action, handler);
            return handler.getResult();
        } else {
            throw new JsonResourceException(JsonResourceException.BAD_REQUEST,
                    "Action collection is not supported");
        }
    }

}
