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

    public static final int CREATE = 1;
    public static final int READ = 2;
    public static final int UPDATE = 4;
    public static final int PATCH = 8;
    public static final int QUERY = 16;
    public static final int DELETE = 32;
    public static final int ACTION = 64;

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
