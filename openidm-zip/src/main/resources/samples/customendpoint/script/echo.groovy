/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
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

import org.forgerock.json.resource.ActionRequest
import org.forgerock.json.resource.CreateRequest
import org.forgerock.json.resource.DeleteRequest
import org.forgerock.json.resource.NotSupportedException
import org.forgerock.json.resource.PatchRequest
import org.forgerock.json.resource.QueryRequest
import org.forgerock.json.resource.ReadRequest
import org.forgerock.json.resource.UpdateRequest

if (request instanceof CreateRequest) {
    return [
            method: "create",
            resourceName: request.resourcePath,
            newResourceId: request.newResourceId,
            parameters: request.additionalParameters,
            content: request.content.getObject(),
            context: context
    ]
} else if (request instanceof ReadRequest) {
    return [
            method: "read",
            resourceName: request.resourcePath,
            parameters: request.additionalParameters,
            context: context
    ]
} else if (request instanceof UpdateRequest) {
    return [
            method: "update",
            resourceName: request.resourcePath,
            revision: request.revision,
            parameters: request.additionalParameters,
            content: request.content.getObject(),
            context: context
    ]
} else if (request instanceof PatchRequest) {
    return [
            method: "patch",
            resourceName: request.resourcePath,
            revision: request.revision,
            patch: request.patchOperations,
            parameters: request.additionalParameters,
            context: context
    ]
} else if (request instanceof QueryRequest) {
    // query results must be returned as a list of maps
    return [
            [
                    method: "query",
                    resourceName: request.resourcePath,
                    pagedResultsCookie: request.pagedResultsCookie,
                    pagedResultsOffset: request.pagedResultsOffset,
                    pageSize: request.pageSize,
                    queryExpression: request.queryExpression,
                    queryId: request.queryId,
                    queryFilter: request.queryFilter.toString(),
                    parameters: request.additionalParameters,
                    context: context
            ]
    ]
} else if (request instanceof DeleteRequest) {
    return [
            method: "delete",
            resourceName: request.resourcePath,
            revision: request.revision,
            parameters: request.additionalParameters,
            context: context
    ]
} else if (request instanceof ActionRequest) {
    return [
            method: "action",
            action: request.action,
            content: request.content.getObject(),
            parameters: request.additionalParameters,
            context: context
    ]
} else {
    throw new NotSupportedException(request.getClass().getName());
}
