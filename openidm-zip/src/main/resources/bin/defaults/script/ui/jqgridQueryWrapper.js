/** 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

/*global request */


(function (resource, queryId, page, limit, orderBy, orderByDir, searching) {

    var pagingQueryId = (!searching) ? queryId : queryId + "-filtered",
        pagingQueryArgs = request.additionalParameters;

    limit = parseInt(limit || 20);
    limit = (limit > 100) ? 100 : limit; // restrict limit to 100 records maximum

    offset = limit*(parseInt(page || 1)-1);

    pagingQueryArgs._queryId = pagingQueryId;
    pagingQueryArgs._pageSize = limit;
    pagingQueryArgs._pagedResultsOffset = offset;
    pagingQueryArgs.orderBy = orderBy;
    pagingQueryArgs.orderByDir = orderByDir;

    var queryResult = openidm.query(resource, pagingQueryArgs);

    var totalResults = offset + queryResult.result.length + queryResult.remainingPagedResults;

    return [
        {
        "total": Math.ceil(parseInt(totalResults)/limit),
        "page": Math.ceil((offset+1)/limit),
        "records": parseInt(totalResults),
        "rows": queryResult.result,
        "id": "_id"
        }
    ];

}(request.additionalParameters.resource, request.queryId, request.additionalParameters.page, request.additionalParameters.rows, request.additionalParameters.sidx, request.additionalParameters.sord, request.additionalParameters.search === "true"));



