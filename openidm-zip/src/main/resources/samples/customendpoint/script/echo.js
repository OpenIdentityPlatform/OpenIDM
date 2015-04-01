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
(function(){
    if (request.method === "create") {
        return {
                method: "create",
                resourceName: request.resourceName,
                newResourceId: request.newResourceId,
                parameters: request.additionalParameters,
                content: request.content,
                context: context.current
                };
    } else if (request.method === "read") {
        return {
                method: "read",
                resourceName: request.resourceName,
                parameters: request.additionalParameters,
                context: context.current
                };
    } else if (request.method === "update") {
        return {
                method: "update",
                resourceName: request.resourceName,
                revision: request.revision,
                parameters: request.additionalParameters,
                content: request.content,
                context: context.current
                };
    } else if (request.method === "patch") {
        return {
                method: "patch",
                resourceName: request.resourceName,
                revision: request.revision,
                parameters: request.additionalParameters,
                patch: request.patchOperations,
                context: context.current
                };
    } else if (request.method === "query") {
        // query results must be returned as a list of maps
        return [ {
                method: "query",
                resourceName: request.resourceName,
                pagedResultsCookie: request.pagedResultsCookie,
                pagedResultsOffset: request.pagedResultsOffset,
                pageSize: request.pageSize,
                queryExpression: request.queryExpression,
                queryId: request.queryId,
                queryFilter: request.queryFilter,
                parameters: request.additionalParameters,
                content: request.content,
                context: context.current
                } ];
    } else if (request.method === "delete") {
        return {
                method: "delete",
                resourceName: request.resourceName,
                revision: request.revision,
                parameters: request.additionalParameters,
                context: context.current
                };
    } else if (request.method === "action") {
        return {
                method: "action",
                action: request.action,
                content: request.content,
                parameters: request.additionalParameters,
                context: context.current
                };
    } else {
        throw { code : 500, message : "Unknown request type " + request.method };
    }
})();


