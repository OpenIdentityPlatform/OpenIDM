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

package org.forgerock.openidm.core.filter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.CrossCutFilter;
import org.forgerock.json.resource.CrossCutFilterResultHandler;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PersistenceConfig;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.SecurityContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.engine.Utils;
import org.forgerock.util.Factory;
import org.forgerock.util.LazyMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public class ScriptedFilter implements CrossCutFilter<ScriptedFilter.ScriptState> {

    /**
     * Indicates that the request processing should continue normally. If
     * returned from the {@link #evaluateOnRequest} method, the filter then
     * invokes the
     * {@link CrossCutFilterResultHandler#handleContinue(org.forgerock.json.resource.ServerContext, Object)}
     * method.
     */
    public static final int CONTINUE = 0;

    /**
     * Indicates that after the {@link #evaluateOnRequest} method, the request
     * processing should skip the
     * {@link CrossCutFilterResultHandler#handleContinue(org.forgerock.json.resource.ServerContext, Object)}
     * method to continue with the {@link RequestHandler} method.
     */
    public static final int SKIP = 1;

    /**
     * Indicates that the request processing should stop and return the current
     * response from the filter.
     */
    public static final int STOP = 2;

    /**
     * Setup logging for the {@link ScriptedFilter}.
     */
    private static final Logger logger = LoggerFactory.getLogger(ScriptedFilter.class);

    public static class ScriptState {

        private Object state = null;

        private Request request;

        public ScriptState(final Request request) {
            this.request = request;
        }
    }

    private final Pair<JsonPointer, ScriptEntry> onRequest;
    private final Pair<JsonPointer, ScriptEntry> onResponse;
    private final Pair<JsonPointer, ScriptEntry> onFailure;
    private PersistenceConfig persistenceConfig = null;

    public ScriptedFilter(Pair<JsonPointer, ScriptEntry> onRequest,
            Pair<JsonPointer, ScriptEntry> onResponse, Pair<JsonPointer, ScriptEntry> onFailure) {
        this.onRequest = onRequest;
        this.onResponse = onResponse;
        this.onFailure = onFailure;
    }

    // ----- Filter Action Request

    @Override
    public void filterActionRequest(final ServerContext context, final ActionRequest request,
            final RequestHandler next,
            final CrossCutFilterResultHandler<ScriptState, JsonValue> handler) {
        final ScriptState state = new ScriptState(request);
        try {
            switch (evaluateOnRequest(context, state)) {
            case CONTINUE: {
                handler.handleContinue(context, state);
                break;
            }
            case SKIP: {
                break;
            }
            case STOP: {
                break;
            }
            }
        } catch (ResourceException e) {
            logger.error("Failed the execute the onRequest script", e);
            handler.handleError(e);
        }
    }

    @Override
    public void filterActionResult(final ServerContext context, final ScriptState state,
            final JsonValue result, final ResultHandler<JsonValue> handler) {
        if (null != onResponse) {
            handler.handleResult(result);
        } else {
            handler.handleResult(result);
        }
    }

    @Override
    public void filterActionError(final ServerContext context, final ScriptState state,
            final ResourceException error, final ResultHandler<JsonValue> handler) {
        if (null != onFailure) {
            logger.info("Filter GenericError response: {}.", context.getId());
            try {
                evaluateOnFailure(context, state, error, handler);
            } catch (ResourceException e) {
                logger.error("Failed the execute the onFailure script", e);
                // TODO combine the messages
                handler.handleError(error);
            }
        } else {
            handler.handleError(error);
        }
    }

    // ----- Filter Generic Request

    @Override
    public void filterGenericRequest(final ServerContext context, final Request request,
            final RequestHandler next,
            final CrossCutFilterResultHandler<ScriptState, Resource> handler) {
        logger.info("Request");
        final ScriptState state = new ScriptState(request);
        try {
            switch (evaluateOnRequest(context, state)) {
            case CONTINUE: {
                handler.handleContinue(context, state);
                break;
            }
            case SKIP: {
                break;
            }
            case STOP: {
                break;
            }
            }
        } catch (ResourceException e) {
            logger.error("Failed the execute the onRequest script", e);
            handler.handleError(e);
        }
    }

    @Override
    public void filterGenericResult(final ServerContext context, final ScriptState state,
            final Resource result, final ResultHandler<Resource> handler) {
        handler.handleResult(result);
        logger.info("Result");
        if (null != onResponse) {
            logger.info("Filter GenericError response: {}.", context.getId());
            try {
                evaluateOnResponse(context, state, result);
            } catch (ResourceException e) {
                logger.error("Failed the execute the onFailure script", e);
                // TODO combine the messages
                // Todo What to do here?
                // handler.handleError(e);
            } finally {
                handler.handleResult(result);
            }
        } else {
            handler.handleResult(result);
        }
    }

    @Override
    public void filterGenericError(final ServerContext context, final ScriptState state,
            final ResourceException error, final ResultHandler<Resource> handler) {
        if (null != onFailure) {
            logger.info("Filter GenericError response: {}.", context.getId());
            try {
                evaluateOnFailure(context, state, error, handler);
            } catch (ResourceException e) {
                logger.error("Failed the execute the onFailure script", e);
                // TODO combine the messages
                handler.handleError(error);
            }
        } else {
            handler.handleError(error);
        }
    }

    // ----- Filter Query Request

    @Override
    public void filterQueryRequest(final ServerContext context, QueryRequest request,
            RequestHandler next, CrossCutFilterResultHandler<ScriptState, QueryResult> handler) {

        final ScriptState state = new ScriptState(request);
        try {
            switch (evaluateOnRequest(context, state)) {
            case CONTINUE: {
                handler.handleContinue(context, state);
                break;
            }
            case SKIP: {
                break;
            }
            case STOP: {
                break;
            }
            }
        } catch (ResourceException e) {
            logger.error("Failed the execute the onRequest script", e);
            handler.handleError(e);
        }

    }

    @Override
    public void filterQueryResource(final ServerContext context, final ScriptState state,
            final Resource resource, final QueryResultHandler handler) {
        if (null != onResponse) {
            logger.debug("Filter GenericError response: {}.", context.getId());
            try {
                evaluateOnResponse(context, state, resource);
            } catch (ResourceException e) {
                logger.error("Failed the execute the onFailure script", e);
                // TODO combine the messages
                // Todo What to do here?
                // handler.handleError(e);
            } finally {
                handler.handleResource(resource);
            }
        } else {
            handler.handleResource(resource);
        }

    }

    @Override
    public void filterQueryResult(final ServerContext context, final ScriptState state,
            QueryResult result, final QueryResultHandler handler) {
        if (null != onResponse) {
            handler.handleResult(result);
        } else {
            handler.handleResult(result);
        }
    }

    @Override
    public void filterQueryError(final ServerContext context, final ScriptState state,
            final ResourceException error, final QueryResultHandler handler) {
        if (null != onFailure) {
            logger.debug("Filter GenericError response: {}.", context.getId());
            try {
                evaluateOnFailure(context, state, error, handler);
            } catch (ResourceException e) {
                logger.error("Failed the execute the onFailure script", e);
                // TODO combine the messages
                handler.handleError(error);
            }
        } else {
            handler.handleError(error);
        }
    }

    // ----- Evaluate Script methods

    protected int evaluateOnRequest(final ServerContext context, final ScriptState state)
            throws ResourceException {
        if (onRequest != null) {
            ScriptEntry scriptEntry = onRequest.getRight();
            if (!scriptEntry.isActive()) {
                throw new ServiceUnavailableException("Failed to execute inactive script: "
                        + onRequest.getRight().getName());
            }
            Script script = scriptEntry.getScript(context);

            script.put("request", getRequestMap(state.request, context));
            script.put("context", context);
            script.put("_context", getLazyContext(context));
            try {
                //TODO Add function to support SKIP/STOP/CONTINUE
                state.state = script.eval();
            } catch (Throwable t) {
                logger.debug("Filter/{} script {} encountered exception at {}", onRequest.getRight()
                        .getName(), onRequest.getLeft(), t);
                throw Utils.adapt(t);
            }
        }
        return CONTINUE;
    }

    public boolean evaluateOnResponse(final ServerContext context, final ScriptState state,
                                     final Resource resource) throws ResourceException {
        if (onFailure != null) {
            ScriptEntry scriptEntry = onFailure.getRight();
            if (!scriptEntry.isActive()) {
                throw new ServiceUnavailableException("Failed to execute inactive script: "
                        + onFailure.getRight().getName());
            }
            Script script = scriptEntry.getScript(context);

            script.put("request", getRequestMap(state.request, context));
            script.put("context", context);
            script.put("response", resource);

            script.put("_context", getLazyContext(context));
            try {
                state.state = script.eval();
            } catch (Throwable t) {
                logger.debug("Filter/{} script {} encountered exception at {}", onFailure.getRight()
                        .getName(), onFailure.getLeft(), t);
                throw Utils.adapt(t);
            }
        }
        return true;
    }

    public void evaluateOnFailure(final ServerContext context, final ScriptState state,
            final ResourceException error, final ResultHandler<?> handler) throws ResourceException {
        if (onFailure != null) {
            ScriptEntry scriptEntry = onFailure.getRight();
            if (!scriptEntry.isActive()) {
                throw new ServiceUnavailableException("Failed to execute inactive script: "
                        + onFailure.getRight().getName());
            }
            Script script = scriptEntry.getScript(context);

            script.put("request", getRequestMap(state.request, context));
            script.put("context", context);
            script.put("exception", error.toJsonValue().asMap());

            script.put("_context", getLazyContext(context));
            try {
                state.state = script.eval();
            } catch (Throwable t) {
                logger.debug("Filter/{} script {} encountered exception at {}", onFailure.getRight()
                        .getName(), onFailure.getLeft(), t);
                throw Utils.adapt(t);
            }
        }
    }

    private LazyMap<String, Object> getLazyContext(final ServerContext context) {
        return new LazyMap<String, Object>(new Factory<Map<String, Object>>() {
            @Override
            public Map<String, Object> newInstance() {
                final JsonValue serverContext;
                try {
                    serverContext = serialiseServerContext(context);
                    if (null != serverContext) {
                        return serverContext.required().asMap();
                    }
                } catch (ResourceException e) {
                    logger.error("Failed to serialise the ServerContext", e);
                    /* ignore */
                }
                return Collections.emptyMap();
            }
        });
    }

    protected JsonValue serialiseServerContext(final ServerContext context)
            throws ResourceException {
        if (null != context && null != persistenceConfig) {
            return ServerContext.saveToJson(context, persistenceConfig);
        }
        return null;
    }
    
    private Map<String, Object> getRequestMap(Request request, ServerContext context) {
    	Map<String, Object> requestMap = new HashMap<String, Object>();
    	JsonValue value = new JsonValue(null);
    	if (request instanceof ActionRequest) {
    		value = ((ActionRequest)request).getContent();
    		requestMap.put("params", ((ActionRequest)request).getAdditionalActionParameters());
    		requestMap.put("method", "action");
    	} else if (request instanceof CreateRequest) {
    		value = ((CreateRequest)request).getContent();
    		requestMap.put("method", "create");
    	} else if (request instanceof ReadRequest) {
    		requestMap.put("method", "read");
    	} else if (request instanceof UpdateRequest) {
    		value = ((UpdateRequest)request).getNewContent();
    		requestMap.put("method", "update");
    	} else if (request instanceof DeleteRequest) {
    		requestMap.put("method", "delete");
    	} else if (request instanceof PatchRequest) {
    		requestMap.put("method", "patch");
    	} else if (request instanceof QueryRequest) {
    		requestMap.put("params", ((QueryRequest)request).getAdditionalQueryParameters());
    		requestMap.put("method", "query");
    	} else {
    		requestMap.put("method", null);
    	}
    	if (context.containsContext(SecurityContext.class)) {
    		requestMap.put("security", context.asContext(SecurityContext.class).getAuthorizationId());
    	}
    	if (context.containsContext(HttpContext.class)) {
    		HttpContext httpContext = context.asContext(HttpContext.class);
    		requestMap.put("headers", httpContext.getHeaders());
    		requestMap.put("fromHttp", "true");
    		requestMap.put("params", httpContext.getParameters());
    	} else {
    		requestMap.put("fromHttp", "false");
    	}
    	requestMap.put("type", request.getRequestType());
    	requestMap.put("value", value.getObject());
    	requestMap.put("id", request.getResourceName());
    	
    	return requestMap;
    }
}
