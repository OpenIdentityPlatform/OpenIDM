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

package org.forgerock.openidm.router;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;

import java.util.List;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 * @see <a href="https://bugster.forgerock.org/jira/browse/CREST-7">CREST-7</a>
 */
public final class FilterChain implements RequestHandler {

    public FilterChain(List<Filter> filters) {};

    public void handleAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        handler.handleError(new ForbiddenException("Operation is not implemented"));
    }

    public void handleCreate(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new ForbiddenException("Operation is not implemented"));
    }

    public void handleDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new ForbiddenException("Operation is not implemented"));
    }

    public void handlePatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new ForbiddenException("Operation is not implemented"));
    }

    public void handleQuery(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        handler.handleError(new ForbiddenException("Operation is not implemented"));
    }

    public void handleRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new ForbiddenException("Operation is not implemented"));
    }

    public void handleUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new ForbiddenException("Operation is not implemented"));
    }
}
