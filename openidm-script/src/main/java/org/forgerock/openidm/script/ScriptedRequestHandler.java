package org.forgerock.openidm.script;

import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptException;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;

/**
 * A ScriptedRequestHandler //TODO implement more later
 * 
 * @author Laszlo Hordos
 */
public class ScriptedRequestHandler implements RequestHandler {

    private final ScriptEntry scriptEntry;

    private final ScriptCustomizer customizer;

    public ScriptedRequestHandler(final ScriptEntry scriptEntry, final ScriptCustomizer customizer) {
        this.scriptEntry = scriptEntry;
        this.customizer = customizer;
    }

    public void handleAction(final ServerContext context, final ActionRequest request,
            final ResultHandler<JsonValue> handler) {
        try {
            if (scriptEntry.isActive()) {
                final Script script = scriptEntry.getScript(context);
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
                        + scriptEntry.getName()));
            }
        } catch (ScriptException e) {
            handler.handleError(new InternalServerErrorException(e));
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e));
        }
    }

    public void handleCreate(ServerContext context, CreateRequest request,
            ResultHandler<Resource> handler) {
        // To change body of implemented methods use File | Settings | File
        // Templates.
    }

    public void handleDelete(ServerContext context, DeleteRequest request,
            ResultHandler<Resource> handler) {
        // To change body of implemented methods use File | Settings | File
        // Templates.
    }

    public void handlePatch(ServerContext context, PatchRequest request,
            ResultHandler<Resource> handler) {
        // To change body of implemented methods use File | Settings | File
        // Templates.
    }

    public void handleQuery(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        // To change body of implemented methods use File | Settings | File
        // Templates.
    }

    public void handleRead(ServerContext context, ReadRequest request,
            ResultHandler<Resource> handler) {
        try {
            if (scriptEntry.isActive()) {
                final Script script = scriptEntry.getScript(context);
                script.setBindings(script.createBindings());
                customizer.handleRead(context, request, script.getBindings());
                Object result = script.eval();
                if (null == result) {
                    handler.handleResult(new Resource(request.getResourceName(), null, new JsonValue(null)));
                } else if (result instanceof JsonValue) {
                    handler.handleResult(new Resource(request.getResourceName(), null, (JsonValue) result));
                } else if (result instanceof Map) {
                    handler.handleResult(new Resource(request.getResourceName(), null, new JsonValue((result))));
                } else {
                    JsonValue resource = new JsonValue(new HashMap<String, Object>(1));
                    resource.put("result", result);
                    handler.handleResult(new Resource(request.getResourceName(), null, resource));
                }
            } else {
                handler.handleError(new ServiceUnavailableException("Inactive script: "
                        + scriptEntry.getName()));
            }
        } catch (ScriptException e) {
            handler.handleError(new InternalServerErrorException(e));
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e));
        }
    }

    public void handleUpdate(ServerContext context, UpdateRequest request,
            ResultHandler<Resource> handler) {
        // To change body of implemented methods use File | Settings | File
        // Templates.
    }
}
