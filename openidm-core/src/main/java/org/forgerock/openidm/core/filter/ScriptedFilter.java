/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 ForgeRock AS. All Rights Reserved
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

import org.apache.commons.lang3.tuple.Pair;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CrossCutFilter;
import org.forgerock.json.resource.CrossCutFilterResultHandler;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.engine.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A NAME does ...
 *
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

    public ScriptedFilter(
            Pair<JsonPointer, ScriptEntry> onRequest,
            Pair<JsonPointer, ScriptEntry> onResponse,
            Pair<JsonPointer, ScriptEntry> onFailure) {
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
            logger.debug("Failed the execute the onRequest script", e);
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
                logger.debug("Failed the execute the onFailure script", e);
                // TODO combine the messages
                // handler.handleError(e);
            } finally {
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
            logger.debug("Failed the execute the onRequest script", e);
            handler.handleError(e);
        }
    }

    @Override
    public void filterGenericResult(final ServerContext context, final ScriptState state,
            final Resource result, final ResultHandler<Resource> handler) {
        if (null != onResponse) {
            logger.info("Filter GenericError response: {}.", context.getId());
            try {
                evaluateOnResponse(context, state, result);
            } catch (ResourceException e) {
                logger.debug("Failed the execute the onFailure script", e);
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
                logger.debug("Failed the execute the onFailure script", e);
                // TODO combine the messages
                // handler.handleError(e);
            } finally {
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
            logger.debug("Failed the execute the onRequest script", e);
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
                logger.debug("Failed the execute the onFailure script", e);
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
                logger.debug("Failed the execute the onFailure script", e);
                // TODO combine the messages
                // handler.handleError(e);
            } finally {
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
            Script script = populateScript(scriptEntry, context, state.request);
            try {
                //TODO Add function to support SKIP/STOP/CONTINUE
                state.state = script.eval();
            } catch (Throwable t) {
                logger.debug("Filter/{} script {} encountered exception at {}", onRequest.getRight()
                        .getName(), onRequest.getLeft(), t);
                ResourceException re = Utils.adapt(t);
                logger.debug("ResourceException detail: " + re.getDetail());
                throw re;
            }
        }
        return CONTINUE;
    }

    public boolean evaluateOnResponse(final ServerContext context, final ScriptState state,
                                     final Resource resource) throws ResourceException {
        if (onResponse != null) {
            ScriptEntry scriptEntry = onResponse.getRight();
            if (!scriptEntry.isActive()) {
                throw new ServiceUnavailableException("Failed to execute inactive script: "
                        + onResponse.getRight().getName());
            }
            Script script = populateScript(scriptEntry, context, state.request);
            script.put("response", resource.getContent());
            try {
                state.state = script.eval();
            } catch (Throwable t) {
                logger.debug("Filter/{} script {} encountered exception at {}", onResponse.getRight()
                        .getName(), onResponse.getLeft(), t);
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
            Script script = populateScript(scriptEntry, context, state.request);
            script.put("exception", error.includeCauseInJsonValue().toJsonValue().asMap());
            try {
                state.state = script.eval();
            } catch (Throwable t) {
                logger.debug("Filter/{} script {} encountered exception at {}", onFailure.getRight()
                        .getName(), onFailure.getLeft(), t);
                throw Utils.adapt(t);
            }
        }
    }

    private Script populateScript(final ScriptEntry scriptEntry, final ServerContext context, final Request request) {
        final Script script = scriptEntry.getScript(context);
        script.put("request", request);
        script.put("context", context);
        return script;
    }
}
