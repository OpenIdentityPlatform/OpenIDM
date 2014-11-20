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
(function (mapping, source, target, sourceProps, targetProps, reconId, situations, page, limit, searching, sourceCriteria, targetCriteria, orderBy, orderByDir) {

    var _ = require('lib/lodash'),
        reconAudit,
        sourceSearchResults,
        targetSearchResults,
        filteredIds = ['NOOP'],
        i = 0, j = 0,
        offset,

        sourceIds = [],
        sourceData = [],
        sourceDataMap = {},

        targetIds = [],
        targetData = [],
        targetDataMap = {},

        syncConfig,
        result = [],
        queryId = "audit-by-recon-id-situations-latest",
        buildQueryFilter = function(props,searchString){
            var conditions = _.chain(props.split(","))
                                .reject(function(p){ return !p; })
                                .map(function(p){
                                    return p + ' sw "' + encodeURIComponent(searchString) + '"';
                                })
                                .value();
            
            return conditions.join(" or (") + new Array(conditions.length).join(")");
        },
        recon = openidm.read("recon/" + reconId);
    
    limit = parseInt(limit || 20);
    limit = (limit > 50) ? 50 : limit; // restrict limit to 50 records maximum

    offset = limit*(parseInt(page || 1)-1);

    if (!searching) {

        reconAudit = openidm.query("repo/audit/recon", {
            "_queryId": queryId,
            "reconId": reconId,
            "situations": situations ? situations : '',
            "completed": recon.ended,
            "_pageSize": limit,
            "_pagedResultsOffset": offset,
            "orderBy": orderBy,
            "orderByDir": orderByDir,
            "formatted": false
        });
    } else {

        if (sourceCriteria && sourceCriteria.length) {

            sourceSearchResults = openidm.query(source, {
                "_queryFilter" : buildQueryFilter(sourceProps, sourceCriteria)
            }).result;

            for (i=0;i<sourceSearchResults.length;i++) {
                filteredIds.push(source + "/" + sourceSearchResults[i]._id);
            }
        }

        if (targetCriteria && targetCriteria.length) {

            targetSearchResults = openidm.query(target, {
                "_queryFilter" : buildQueryFilter(targetProps, targetCriteria)
            }).result;

            for (i=0;i<targetSearchResults.length;i++) {
                filteredIds.push(target + "/" + targetSearchResults[i]._id);
            }
        }

        reconAudit = openidm.query("repo/audit/recon", {
                "_queryId": queryId + "-filtered",
                "reconId": reconId,
                "situations": situations ? situations : '',
                "filteredIds":  filteredIds.join(","),
                "completed": recon.ended,
                "_pageSize": limit,
                "_pagedResultsOffset": offset,
                "orderBy": orderBy,
                "orderByDir": orderByDir,
                "formatted": false
            });     
    }

    if (!reconAudit.result) {
        return {
            "limit": limit,
            "page": 1,
            "rows": [],
            "id": "_id"
        };
    }

    for (i = 0; i<reconAudit.result.length; i++) {
        if (
                reconAudit.result[i].targetObjectId != null && 
                reconAudit.result[i].situation !== "FOUND_ALREADY_LINKED"
            ) {
            targetIds.push('_id eq "' + reconAudit.result[i].targetObjectId.replace(target + "/", "") + '"');
        }
        if (reconAudit.result[i].sourceObjectId != null) {
            sourceIds.push('_id eq "' + reconAudit.result[i].sourceObjectId.replace(source + "/", "") + '"');
        }
    }

    if (sourceIds.length) {
        // [a,b] => "a or (b)"; [a,b,c] => "a or (b or (c))"
        sourceData = openidm.query(source, {"_queryFilter": sourceIds.join(" or (") + Array(sourceIds.length).join(")") });
        for (i=0;i<sourceData.result.length;i++) {
            sourceDataMap[sourceData.result[i]._id] = sourceData.result[i];
        }
    }

    if (targetIds.length) {
        // [a,b] => "a or (b)"; [a,b,c] => "a or (b or (c))"
        targetData = openidm.query(target, {"_queryFilter": targetIds.join(" or (") + Array(targetIds.length).join(")") });
        for (i=0;i<targetData.result.length;i++) {
            targetDataMap[targetData.result[i]._id] = targetData.result[i];
        }
    }

    for (i = 0; i<reconAudit.result.length; i++) {
        if (reconAudit.result[i].sourceObjectId !== null && reconAudit.result[i].sourceObjectId !== undefined) {
            reconAudit.result[i].sourceObject = sourceDataMap[reconAudit.result[i].sourceObjectId.replace(source + "/", "")];
        }
        if (reconAudit.result[i].targetObjectId !== null && reconAudit.result[i].targetObjectId !== undefined) {
            reconAudit.result[i].targetObject = targetDataMap[reconAudit.result[i].targetObjectId.replace(target + "/", "")];
        }
        if (!!reconAudit.result[i].sourceObject && !!reconAudit.result[i].targetObject) {
            if(openidm.query("repo/link", {'_queryFilter' : 'linkType eq "' + mapping + '" and firstId eq "' + reconAudit.result[i].sourceObject._id + '" and secondId eq "' + reconAudit.result[i].targetObject._id + '"'}).result.length > 0){
                reconAudit.result[i].hasLink = true;
            }
        }
        result.push(reconAudit.result[i]);
    }

    return [
            {
            "limit": limit,
            "page": Math.ceil((offset+1)/limit),
            "rows": result,
            "id": "_id"
            }
        ];
}(
    request.additionalParameters.mapping,
    request.additionalParameters.source,
    request.additionalParameters.target,
    request.additionalParameters.sourceProps,
    request.additionalParameters.targetProps,
    request.additionalParameters.reconId,
    request.additionalParameters.situations,
    request.additionalParameters.page,
    request.additionalParameters.rows,
    request.additionalParameters.search === "true",
    request.additionalParameters.sourceObjectDisplay,
    request.additionalParameters.targetObjectDisplay,
    request.additionalParameters.sidx,
    request.additionalParameters.sord
));
