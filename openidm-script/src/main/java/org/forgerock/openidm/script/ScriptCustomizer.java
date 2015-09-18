package org.forgerock.openidm.script;

import javax.script.Bindings;

import org.forgerock.services.context.Context;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.UpdateRequest;

/**
 * A ScriptCustomizer allows an implementer to customize the script bindings per request.
 */
public interface ScriptCustomizer {

    void handleCreate(Context context, CreateRequest request, Bindings bindings) throws ResourceException;

    void handleRead(Context context, ReadRequest request, Bindings bindings) throws ResourceException;

    void handleUpdate(Context context, UpdateRequest request, Bindings bindings) throws ResourceException;

    void handleAction(Context context, ActionRequest request, Bindings bindings) throws ResourceException;

    void handleDelete(Context context, DeleteRequest request, Bindings bindings) throws ResourceException;

    void handlePatch(Context context, PatchRequest request, Bindings bindings) throws ResourceException;

    void handleQuery(Context context, QueryRequest request, Bindings bindings) throws ResourceException;
}
