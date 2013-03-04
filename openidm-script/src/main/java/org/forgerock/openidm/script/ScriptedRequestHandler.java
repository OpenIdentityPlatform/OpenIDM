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

package org.forgerock.openidm.script;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.script.Bindings;
import javax.script.ScriptException;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.script.Scope;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.scope.Function;
import org.forgerock.script.scope.FunctionFactory;
import org.forgerock.script.scope.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A ScriptedRequestHandler
 * 
 * @author Laszlo Hordos
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

    public void handleAction(final ServerContext context, final ActionRequest request,
            final ResultHandler<JsonValue> handler) {
        try {
            final ScriptEntry _scriptEntry = getScriptEntry();
            if (_scriptEntry.isActive()) {
                final Script script = _scriptEntry.getScript(context);
                script.setBindings(script.createBindings());
                customizer.handleAction(context, request, script.getBindings());
                Object result = script.eval();
                if (null == result) {
                    handler.handleResult(new JsonValue(null));
                } else if (result instanceof JsonValue) {
                    handler.handleResult((JsonValue) result);
                } else if (result instanceof Map) {
                    handler.handleResult(new JsonValue((result)));
                } else {
                    JsonValue resource = new JsonValue(new HashMap<String, Object>(1));
                    resource.put("result", result);
                    handler.handleResult(resource);
                }
            } else {
                handler.handleError(new ServiceUnavailableException("Inactive script: "
                        + _scriptEntry.getName()));
            }
        } catch (ScriptException e) {
            handleScriptException(handler, e);
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e.getMessage(), e));
        }
    }

    public void handleCreate(ServerContext context, CreateRequest request,
            ResultHandler<Resource> handler) {
        try {
            final ScriptEntry _scriptEntry = getScriptEntry();
            if (_scriptEntry.isActive()) {
                final Script script = _scriptEntry.getScript(context);
                script.setBindings(script.createBindings());
                customizer.handleCreate(context, request, script.getBindings());
                evaluate(request, handler, script);
            } else {
                handler.handleError(new ServiceUnavailableException("Inactive script: "
                        + _scriptEntry.getName()));
            }
        } catch (ScriptException e) {
            handleScriptException(handler, e);
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e.getMessage(), e));
        }
    }

    public void handleDelete(ServerContext context, DeleteRequest request,
            ResultHandler<Resource> handler) {
        try {
            final ScriptEntry _scriptEntry = getScriptEntry();
            if (_scriptEntry.isActive()) {
                final Script script = _scriptEntry.getScript(context);
                script.setBindings(script.createBindings());
                customizer.handleDelete(context, request, script.getBindings());
                evaluate(request, handler, script);
            } else {
                handler.handleError(new ServiceUnavailableException("Inactive script: "
                        + _scriptEntry.getName()));
            }
        } catch (ScriptException e) {
            handleScriptException(handler, e);
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e.getMessage(), e));
        }
    }

    public void handlePatch(ServerContext context, PatchRequest request,
            ResultHandler<Resource> handler) {
        try {
            final ScriptEntry _scriptEntry = getScriptEntry();
            if (_scriptEntry.isActive()) {
                final Script script = _scriptEntry.getScript(context);
                script.setBindings(script.createBindings());
                customizer.handlePatch(context, request, script.getBindings());
                evaluate(request, handler, script);
            } else {
                handler.handleError(new ServiceUnavailableException("Inactive script: "
                        + _scriptEntry.getName()));
            }
        } catch (ScriptException e) {
            handleScriptException(handler, e);
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e.getMessage(), e));
        }
    }

    /**
     * TODO Implement this method
     * 
     * {@inheritDoc}
     */
    public void handleQuery(final ServerContext context, final QueryRequest request,
            final QueryResultHandler handler) {
        try {
            final ScriptEntry _scriptEntry = getScriptEntry();
            if (_scriptEntry.isActive()) {
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
                QueryResult queryResult = new QueryResult();
                script.putSafe("callback", queryCallback);
                Object result = script.eval();
                if (null != queryResult) {
                    handler.handleResult(queryResult);
                }
            } else {
                handler.handleError(new ServiceUnavailableException("Inactive script: "
                        + _scriptEntry.getName()));
            }
        } catch (ScriptException e) {
            handleScriptException(handler, e);
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e.getMessage(), e));
        }
    }

    public void handleRead(ServerContext context, ReadRequest request,
            ResultHandler<Resource> handler) {
        try {
            final ScriptEntry _scriptEntry = getScriptEntry();
            if (_scriptEntry.isActive()) {
                final Script script = _scriptEntry.getScript(context);
                script.setBindings(script.createBindings());
                customizer.handleRead(context, request, script.getBindings());
                evaluate(request, handler, script);
            } else {
                handler.handleError(new ServiceUnavailableException("Inactive script: "
                        + _scriptEntry.getName()));
            }
        } catch (ScriptException e) {
            handleScriptException(handler, e);
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e.getMessage(), e));
        }
    }

    public void handleUpdate(ServerContext context, UpdateRequest request,
            ResultHandler<Resource> handler) {
        try {
            final ScriptEntry _scriptEntry = getScriptEntry();
            if (_scriptEntry.isActive()) {
                final Script script = _scriptEntry.getScript(context);
                script.setBindings(script.createBindings());
                customizer.handleUpdate(context, request, script.getBindings());
                evaluate(request, handler, script);
            } else {
                handler.handleError(new ServiceUnavailableException("Inactive script: "
                        + _scriptEntry.getName()));
            }
        } catch (ScriptException e) {
            handleScriptException(handler, e);
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e.getMessage(), e));
        }
    }

    // TODO: Investigate the ScriptThrownException
    protected void handleScriptException(final ResultHandler<?> handler, final ScriptException e) {
        JsonValue details = new JsonValue(new HashMap<String, Object>());
        details.put("fileName", e.getFileName());
        details.put("lineNumber", e.getLineNumber());
        details.put("columnNumber", e.getColumnNumber());
        handler.handleError(new InternalServerErrorException(e.getMessage(), e).setDetail(details));
    }

    private void evaluate(final Request request, final ResultHandler<Resource> handler,
            final Script script) throws ScriptException {
        Object result = script.eval();
        if (null == result) {
            handler.handleResult(new Resource(request.getResourceName(), null, new JsonValue(null)));
        } else if (result instanceof JsonValue) {
            handler.handleResult(new Resource(request.getResourceName(), null, (JsonValue) result));
        } else if (result instanceof Map) {
            handler.handleResult(new Resource(request.getResourceName(), null, new JsonValue(
                    (result))));
        } else {
            JsonValue resource = new JsonValue(new HashMap<String, Object>(1));
            resource.put("result", result);
            handler.handleResult(new Resource(request.getResourceName(), null, resource));
        }
    }
}
