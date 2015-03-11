package org.forgerock.openidm.script;

import javax.script.Bindings;

import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;

/**
 * A NAME does ...
 * 
 */
public interface ScriptCustomizer {

    void handleCreate(ServerContext context, CreateRequest request, Bindings bindings)
            throws ResourceException;

    void handleRead(ServerContext context, ReadRequest request, Bindings bindings)
            throws ResourceException;

    void handleUpdate(ServerContext context, UpdateRequest request, Bindings bindings)
            throws ResourceException;

    void handleAction(ServerContext context, ActionRequest request, Bindings bindings)
            throws ResourceException;

    void handleDelete(ServerContext context, DeleteRequest request, Bindings bindings)
            throws ResourceException;

    void handlePatch(ServerContext context, PatchRequest request, Bindings bindings)
            throws ResourceException;

    void handleQuery(ServerContext context, QueryRequest request, Bindings bindings)
            throws ResourceException;

}
