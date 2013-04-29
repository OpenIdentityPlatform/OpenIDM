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

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.salesforce.internal.ResultHandler;
import org.forgerock.openidm.salesforce.internal.SalesforceConnection;
import org.forgerock.openidm.salesforce.internal.ServerContext;
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
public class AsyncBatchResultResourceProvider extends AbstractAsyncResourceProvider {

    public AsyncBatchResultResourceProvider(SalesforceConnection connection) {
        super(connection);
    }

    protected String[] getJobBatchId(ServerContext context) throws JsonResourceException {
        return new String[] { context.getMatcher().group(1), context.getMatcher().group(2) };
    }

    public void readInstance(final ServerContext context, final String resourceId,
            final ResultHandler handler) {
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
            content.put(ServerConstants.OBJECT_PROPERTY_ID, resourceId);
            content.put("result", BioUtils.toString(batchInfo, CharacterSet.UTF_8));

            handler.handleResult(content);

        } catch (Throwable t) {
            handler.handleError(adapt(t));
        }
    }

    public void queryCollection(final ServerContext context, final String queryId,
            final ResultHandler handler) {
        try {

            if ("query-all-ids".equalsIgnoreCase(queryId)) {
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
                    handler.handleResource(fromResult(result));
                }
            } else {
                handler.handleError(new JsonResourceException(JsonResourceException.BAD_REQUEST,
                        "Only query-all-ids is supported"));
            }
        } catch (Throwable t) {
            handler.handleError(adapt(t));
        }
    }

    @Override
    protected JsonValue read(JsonValue request) throws JsonResourceException {
        final ServerContext context = ServerContext.get();
        if (context.getMatcher().groupCount() == 4) {
            final ResultHandler handler = new ResultHandler();
            String id = context.getMatcher().group(3);
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
        if (context.getMatcher().groupCount() == 3) {
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
