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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
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
import org.restlet.data.CharacterSet;
import org.restlet.engine.io.BioUtils;

import com.sforce.async.AsyncApiException;
import com.sforce.async.AsyncExceptionCode;
import com.sforce.async.BatchResult;
import com.sforce.async.Result;

/**
 * An AsyncBatchResultResourceProvider handles all the requests for
 * {code}async/job/{jobId}/batch/{batchId}/result/{id} {code}
 * 
 * {code}(\Qasync/job/\E([^/]+)\Q/batch/\E([^/]+)\Q/result/\E).*{code}
 * 
 * @author Laszlo Hordos
 */
public class AsyncBatchResultResourceProvider extends AbstractAsyncResourceProvider implements
        CollectionResourceProvider /* SingletonResourceProvider */{

    public AsyncBatchResultResourceProvider(SalesforceConnection connection) {
        super(connection);
    }

    protected String[] getJobBatchId(ServerContext context) throws ResourceException {
        Map<String, String> variables = ResourceUtil.getUriTemplateVariables(context);
        if (null != variables && variables.containsKey("jobId") && variables.containsKey("batchId")) {
            return new String[] { variables.get("jobId"), variables.get("batchId") };
        }
        throw new ForbiddenException("Direct access without Router to this service is forbidden.");
    }

    @Override
    public void readInstance(final ServerContext context, final String resourceId,
            final ReadRequest request, final ResultHandler<Resource> handler) {
        try {
            InputStream batchInfo = null;
            String[] ids = getJobBatchId(context);
            try {
                batchInfo = getConnection(false).getQueryResultStream(ids[0], ids[1], resourceId);
            } catch (AsyncApiException e) {
                if (e.getExceptionCode().equals(AsyncExceptionCode.InvalidSessionId)) {
                    batchInfo =
                            getConnection(true).getQueryResultStream(ids[0], ids[1], resourceId);
                } else {
                    throw e;
                }
            }
            JsonValue content = new JsonValue(new HashMap<String, Object>());
            content.put("result", BioUtils.toString(batchInfo, CharacterSet.UTF_8));

            handler.handleResult(new Resource(resourceId, null, content));

        } catch (Throwable t) {
            handler.handleError(adapt(t));
        }
    }

    @Override
    public void queryCollection(final ServerContext context, final QueryRequest request,
            final QueryResultHandler handler) {
        try {

            if (ServerConstants.QUERY_ALL_IDS.equalsIgnoreCase(request.getQueryId())) {
                BatchResult batchResult = null;
                String[] ids = getJobBatchId(context);
                try {
                    batchResult = getConnection(false).getBatchResult(ids[0], ids[1]);
                } catch (AsyncApiException e) {
                    if (e.getExceptionCode().equals(AsyncExceptionCode.InvalidSessionId)) {
                        batchResult = getConnection(true).getBatchResult(ids[0], ids[1]);
                    } else {
                        throw e;
                    }
                }

                for (Result result : batchResult.getResult()) {
                    handler.handleResource(new Resource(result.getId(), null, fromResult(result)));
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
    public void createInstance(final ServerContext context, final CreateRequest request,
            final ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Create operations are not supported");
        handler.handleError(e);
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
}
