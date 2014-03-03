/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
 */


(function (resource, queryId, page, limit, orderBy, orderByDir, searching) {

    var pagingQueryId = (!searching) ? queryId : queryId + "-filtered",
        pagingQueryArgs = request.additionalParameters,
        countTotalQueryId = pagingQueryId + "-count",
        countTotalQueryArgs = request.additionalParameters,
        result = [];

    limit = parseInt(limit || 20);
    limit = (limit > 50) ? 50 : limit; // restrict limit to 50 records maximum

    offset = limit*(parseInt(page || 1)-1);

    pagingQueryArgs._queryId = pagingQueryId;
    pagingQueryArgs.limit = limit;
    pagingQueryArgs.skip = offset;
    pagingQueryArgs.orderBy = orderBy;
    pagingQueryArgs.orderByDir = orderByDir;

    pagingQueryId = openidm.query(resource, pagingQueryArgs);

    countTotalQueryArgs._queryId = countTotalQueryId;
    countTotalQueryId = openidm.query(resource, countTotalQueryArgs);

    return [
        {
        "total": Math.ceil(parseInt(countTotalQueryId.result[0].total)/limit),
        "page": Math.ceil((offset+1)/limit),
        "records": parseInt(countTotalQueryId.result[0].total),
        "rows": pagingQueryId.result,
        "id": "_id"
        }
    ];

}(request.additionalParameters.resource, request.queryId, request.additionalParameters.page, request.additionalParameters.rows, request.additionalParameters.sidx, request.additionalParameters.sord, request.additionalParameters._search === "true"));



