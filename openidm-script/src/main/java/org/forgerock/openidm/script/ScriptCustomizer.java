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
 * @author Laszlo Hordos
 */
public interface ScriptCustomizer {

    void handleCreate(ServerContext context, CreateRequest request, Bindings handler)
            throws ResourceException;

    void handleRead(ServerContext context, ReadRequest request, Bindings handler)
            throws ResourceException;

    void handleUpdate(ServerContext context, UpdateRequest request, Bindings handler)
            throws ResourceException;

    void handleAction(ServerContext context, ActionRequest request, Bindings handler)
            throws ResourceException;

    void handleDelete(ServerContext context, DeleteRequest request, Bindings handler)
            throws ResourceException;

    void handlePatch(ServerContext context, PatchRequest request, Bindings handler)
            throws ResourceException;

    void handleQuery(ServerContext context, QueryRequest request, Bindings handler)
            throws ResourceException;

}
