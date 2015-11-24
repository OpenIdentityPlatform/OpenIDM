/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.openidm.script;

import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;

import javax.script.Bindings;
import javax.script.ScriptException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CountPolicy;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Name;
import org.forgerock.openidm.smartevent.Publisher;
import org.forgerock.script.Scope;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.exception.ScriptThrownException;
import org.forgerock.script.scope.Function;
import org.forgerock.script.scope.FunctionFactory;
import org.forgerock.script.scope.Parameter;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A ScriptedRequestHandler implements a RequestHandler using a script.
 */
public class ScriptedRequestHandler implements Scope, RequestHandler {

    /**
     * Setup logging for the {@link ScriptedRequestHandler}.
     */
    private static final Logger logger = LoggerFactory.getLogger(ScriptedRequestHandler.class);

    private final AtomicReference<ScriptEntry> scriptEntry;

    private final ScriptCustomizer customizer;

    public ScriptedRequestHandler(final ScriptEntry scriptEntry, final ScriptCustomizer customizer) {
        if (null == scriptEntry) {
            throw new NullPointerException();
        }
        if (null == customizer) {
            throw new NullPointerException();
        }
        this.scriptEntry = new AtomicReference<ScriptEntry>(scriptEntry);
        this.customizer = customizer;
    }

    private ScriptEntry getScriptEntry() {
        return scriptEntry.get();
    }

    /**
     * Eventually set the script value.
     * <p/>
     * 
     * @param newScriptEntry
     * @throws NullPointerException
     *             when the {@code newScriptEntry} is null.
     */
    public void setScriptEntry(final ScriptEntry newScriptEntry) {
        if (null == newScriptEntry) {
            throw new NullPointerException();
        }
        scriptEntry.lazySet(newScriptEntry);
    }

    // ----- Implementation of Scope interface

    @Override
    public void put(final String key, final Object value) {
        getScriptEntry().put(key, value);
    }

    @Override
    public Object get(String key) {
        return getScriptEntry().get(key);
    }

    @Override
    public Bindings getBindings() {
        return getScriptEntry().getBindings();
    }

    @Override
    public void setBindings(final Bindings bindings) {
        getScriptEntry().setBindings(bindings);
    }

    @Override
    public void flush() {
        getScriptEntry().flush();
    }

    @Override
    public Bindings createBindings() {
        return getScriptEntry().createBindings();
    }

    // ----- Implementation of RequestHandler interface

    public Promise<ActionResponse, ResourceException> handleAction(final Context context, final ActionRequest request) {
        EventEntry measure = Publisher.start(Name.get("openidm/internal/script/" + this.getScriptEntry().getName().getName() + "/action"), null, null);
        try {
            final ScriptEntry _scriptEntry = getScriptEntry();
            if (!_scriptEntry.isActive()) {
                throw new ServiceUnavailableException("Inactive script: " + _scriptEntry.getName());
            }
            final Script script = _scriptEntry.getScript(context);
            script.setBindings(script.createBindings());
            customizer.handleAction(context, request, script.getBindings());
            Object result = script.eval();
            if (null == result) {
                return newActionResponse(new JsonValue(null)).asPromise();
            } else if (result instanceof JsonValue) {
                return newActionResponse((JsonValue) result).asPromise();
            } else if (result instanceof Map) {
                return newActionResponse(new JsonValue(result)).asPromise();
            } else {
                JsonValue resource = new JsonValue(new HashMap<String, Object>(1));
                resource.put("result", result);
                return newActionResponse(new JsonValue(result)).asPromise();
            }
        } catch (ScriptException e) {
            return convertScriptException(e).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        } finally {
            measure.end();
        }
    }

    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
        EventEntry measure = Publisher.start(Name.get("openidm/internal/script/" + this.getScriptEntry().getName().getName() + "/create"), null, null);
        try {
            final ScriptEntry _scriptEntry = getScriptEntry();
            if (!_scriptEntry.isActive()) {
                throw new ServiceUnavailableException("Inactive script: " + _scriptEntry.getName());
            }
            final Script script = _scriptEntry.getScript(context);
            script.setBindings(script.createBindings());
            customizer.handleCreate(context, request, script.getBindings());
            return evaluate(request, script);
        } catch (ScriptException e) {
            return convertScriptException(e).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        } finally {
            measure.end();
        }
    }

    public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
        EventEntry measure = Publisher.start(Name.get("openidm/internal/script/" + this.getScriptEntry().getName().getName() + "/delete"), null, null);
        try {
            final ScriptEntry _scriptEntry = getScriptEntry();
            if (!_scriptEntry.isActive()) {
                throw new ServiceUnavailableException("Inactive script: " + _scriptEntry.getName());
            }
            final Script script = _scriptEntry.getScript(context);
            script.setBindings(script.createBindings());
            customizer.handleDelete(context, request, script.getBindings());
            return evaluate(request, script);
        } catch (ScriptException e) {
            return convertScriptException(e).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        } finally {
            measure.end();
        }
    }

    public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
        EventEntry measure = Publisher.start(Name.get("openidm/internal/script/" + this.getScriptEntry().getName().getName() + "/patch"), null, null);
        try {
            final ScriptEntry _scriptEntry = getScriptEntry();
            if (!_scriptEntry.isActive()) {
                throw new ServiceUnavailableException("Inactive script: " + _scriptEntry.getName());
            }
            final Script script = _scriptEntry.getScript(context);
            script.setBindings(script.createBindings());
            customizer.handlePatch(context, request, script.getBindings());
            return evaluate(request, script);
        } catch (ScriptException e) {
            return convertScriptException(e).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        } finally {
            measure.end();
        }
    }

    /**
     * TODO Implement this method
     * 
     * {@inheritDoc}
     */
    public Promise<QueryResponse, ResourceException> handleQuery(final Context context, final QueryRequest request,
            final QueryResourceHandler handler) {
        EventEntry measure = Publisher.start(Name.get("openidm/internal/script/" + this.getScriptEntry().getName().getName() + "/query"), null, null);
        try {
            final ScriptEntry _scriptEntry = getScriptEntry();
            if (!_scriptEntry.isActive()) {
                throw new ServiceUnavailableException("Inactive script: " + _scriptEntry.getName());
            }
            final Script script = _scriptEntry.getScript(context);
            script.setBindings(script.createBindings());
            customizer.handleQuery(context, request, script.getBindings());

            final Function<Void> queryCallback = new Function<Void>() {
                                             @Override
                                             public Void call(Parameter scope, Function<?> callback, Object... arguments)
                                             throws ResourceException, NoSuchMethodException {
                    if (arguments.length == 3 && null != arguments[2]) {
                        if (arguments[2] instanceof Map) {

                        }
                        if (arguments[2] instanceof JsonValue) {

                        } else {
                            throw new NoSuchMethodException(FunctionFactory
                                    .getNoSuchMethodMessage("callback", arguments));
                        }
                    } else if (arguments.length >= 2 && null != arguments[1]) {
                        if (arguments[1] instanceof Map) {

                        }
                        if (arguments[1] instanceof JsonValue) {

                        } else {
                            throw new NoSuchMethodException(FunctionFactory
                                    .getNoSuchMethodMessage("callback", arguments));
                        }
                    } else if (arguments.length >= 1 && null != arguments[0]) {
                        if (arguments[0] instanceof Map) {

                        }
                        if (arguments[0] instanceof JsonValue) {

                        } else {
                            throw new NoSuchMethodException(FunctionFactory
                                    .getNoSuchMethodMessage("callback", arguments));
                        }
                    } else {
                        throw new NoSuchMethodException(FunctionFactory.getNoSuchMethodMessage(
                                "callback", arguments));
                    }
                    return null;
            }
        };
            script.putSafe("callback", queryCallback);
            Object rawResult = script.eval();
            JsonValue result = null;
            if (rawResult instanceof JsonValue) {
                result = (JsonValue) rawResult;
            } else {
                result = new JsonValue(rawResult);
            }
            QueryResponse queryResponse = newQueryResponse();
            // Script can either
            // - return null and instead use callback hook to call
            //   handleResource, handleResult, handleError
            //   careful! script MUST call handleResult or handleError itself
            // or
            // - return a result list of resources
            // or
            // - return a full query result structure
            if (!result.isNull()) {
                if (result.isList()) {
                    // Script may return just the result elements as a list
                    handleQueryResultList(result, handler);
                } else {
                    // Or script may return a full query response structure,
                    // with meta-data and results field
                    if (result.isDefined(QueryResponse.FIELD_RESULT)) {
                        handleQueryResultList(result.get(QueryResponse.FIELD_RESULT), handler);
                        queryResponse = newQueryResponse(
                                result.get(QueryResponse.FIELD_PAGED_RESULTS_COOKIE).asString(),
                                result.get(QueryResponse.FIELD_TOTAL_PAGED_RESULTS_POLICY).asEnum(CountPolicy.class),
                                result.get(QueryResponse.FIELD_TOTAL_PAGED_RESULTS).asInteger());
                    } else {
                        logger.debug("Script returned unexpected query result structure: ",
                                 result.getObject());
                        return new InternalServerErrorException(
                                "Script returned unexpected query result structure of type "
                                + result.getObject().getClass())
                            .asPromise();
                    }
                }
            }
            return queryResponse.asPromise();
        } catch (ScriptException e) {
            return convertScriptException(e).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        } finally {
            measure.end();
        }
    }
    
    /**
     * Takes a list of results (in json value wrapped form) and
     * calls the handleResource for each on the handler. 
     * @param resultList the list of results, possibly with id and rev entries
     * @param handler the handle to set the results on
     */
    private void handleQueryResultList(JsonValue resultList, QueryResourceHandler handler) {
        for (JsonValue entry : resultList) {
            // These can end up null
            String id = null;
            String rev = null;
            if (entry.isMap()) {
                id = entry.get(ResourceResponse.FIELD_ID).asString();
                rev = entry.get(ResourceResponse.FIELD_REVISION).asString();
            }
            handler.handleResource(newResourceResponse(id, rev, entry));
        }
    }
    

    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        EventEntry measure = Publisher.start(Name.get("openidm/internal/script/" + this.getScriptEntry().getName().getName() + "/read"), null, null);
        try {
            final ScriptEntry _scriptEntry = getScriptEntry();
            if (!_scriptEntry.isActive()) {
                throw new ServiceUnavailableException("Inactive script: " + _scriptEntry.getName());
            }
            final Script script = _scriptEntry.getScript(context);
            script.setBindings(script.createBindings());
            customizer.handleRead(context, request, script.getBindings());
            return evaluate(request, script);
        } catch (ScriptException e) {
            return convertScriptException(e).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        } finally {
            measure.end();
        }
    }

    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
        EventEntry measure = Publisher.start(Name.get("openidm/internal/script/" + this.getScriptEntry().getName().getName() + "/update"), null, null);
        try {
            final ScriptEntry _scriptEntry = getScriptEntry();
            if (!_scriptEntry.isActive()) {
                throw new ServiceUnavailableException("Inactive script: " + _scriptEntry.getName());
            }
            final Script script = _scriptEntry.getScript(context);
            script.setBindings(script.createBindings());
            customizer.handleUpdate(context, request, script.getBindings());
            return evaluate(request, script);
        } catch (ScriptException e) {
            return convertScriptException(e).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        } finally {
            measure.end();
        }
    }

    protected ResourceException convertScriptException(final ScriptException scriptException) {

        ResourceException convertedError;
        try {
            throw scriptException;
        } catch (ScriptThrownException e) {
            convertedError = e.toResourceException(ResourceException.INTERNAL_ERROR, scriptException.getMessage());
        } catch (ScriptException e) {
            convertedError = new InternalServerErrorException(scriptException.getMessage(), scriptException);
        }
        if (convertedError.getDetail().isNull()) {
            convertedError.setDetail(new JsonValue(new HashMap<String, Object>()));
        }
        final JsonValue detail = convertedError.getDetail();
        if (detail.get("fileName").isNull()
                && detail.get("lineNumber").isNull()
                && detail.get("columnNumber").isNull()) {
            detail.put("fileName", scriptException.getFileName());
            detail.put("lineNumber", scriptException.getLineNumber());
            detail.put("columnNumber", scriptException.getColumnNumber());
        }

        return convertedError;
    }

    private Promise<ResourceResponse, ResourceException> evaluate(final Request request, final Script script)
            throws ScriptException {
        Object result = script.eval();
        ResourcePath resourcePath = request.getResourcePathObject();
        if (null == result) {
            return new NotFoundException("script returned null").asPromise();
        }
        JsonValue resultJson = (result instanceof JsonValue)
                ? (JsonValue) result
                : new JsonValue(result);
        // If the resultJson isn't able to provide an ID, then we default to the resourcePath.
        String id = resultJson.get(ResourceResponse.FIELD_CONTENT_ID).defaultTo("").asString();
        if (id.isEmpty() && resourcePath.size() > 0) {
            id = resourcePath.leaf();
        }
        return newResourceResponse(id, null, resultJson).asPromise();
    }
}
