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
        sourceSearchResults = [],
        targetSearchResults = [],
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
        queryFilter = '/reconId eq "'+reconId+'" AND /entryType eq "entry"',
        buildQueryFilter = function(props,searchString){
            var conditions = _.chain(props.split(","))
                .reject(function(p){ return !p; })
                .map(function(p){
                    return p + ' sw "' + encodeURIComponent(searchString) + '"';
                })
                .value();

            return conditions.join(" or (") + new Array(conditions.length).join(")");
        },
        buildINClause = function (array, fieldName) {
            var inClause = ' AND (';

            inClause += _.map(array,
                function (val) {
                    return fieldName + ' eq "' + val + '"';
                })
                .join(" OR ");

            inClause += ')';

            return inClause;
        },
        recon = openidm.read("recon/" + reconId);

    limit = parseInt(limit || 20);
    limit = (limit > 50) ? 50 : limit; // restrict limit to 50 records maximum

    offset = limit*(parseInt(page || 1)-1);

    if (recon.ended) {
        queryFilter += ' AND /timestamp LE "'+recon.ended+'"'
    }

    if (situations) {
        queryFilter += buildINClause(situations.split(','), '/situation');
    }


    if (searching) {

        if (sourceCriteria && sourceCriteria.length) {

            sourceSearchResults = openidm.query(source, {
                "_queryFilter" : buildQueryFilter(sourceProps, sourceCriteria)
            }).result;

            if (sourceSearchResults.length) {
                queryFilter += buildINClause(

                    _(sourceSearchResults)
                        .pluck('_id')
                        .map(function (id) { return source + "/" + id; })
                        .value(),

                    '/sourceObjectId'
                );
            }

        }

        if (targetCriteria && targetCriteria.length) {

            targetSearchResults = openidm.query(target, {
                "_queryFilter" : buildQueryFilter(targetProps, targetCriteria)
            }).result;

            if (targetSearchResults.length) {
                queryFilter += buildINClause(

                    _(targetSearchResults)
                        .pluck('_id')
                        .map(function (id) { return target + "/" + id; })
                        .value(),

                    '/targetObjectId'
                );
            }

        }

        if (!sourceSearchResults.length && !targetSearchResults.length) {
            // if they are searching for records but there are no results found for them,
            // there is no point in querying the audit data; give up and return nothing.

            return [{
                "limit": limit,
                "page": 1,
                "rows": [],
                "id": "_id"
            }];
        }
    }

    reconAudit = openidm.query("audit/recon", {
        "_queryFilter": queryFilter,
        "_pageSize": limit,
        "_pagedResultsOffset": offset,
        "_sortKeys": "-timestamp",
        "_fields": "sourceObjectId,targetObjectId,ambiguousTargetObjectIds,timestamp,situation,linkQualifier",
        "situations": situations ? situations : '',
        "completed": recon.ended,
        "formatted": false
    });

    if (!reconAudit.result) {
        return [{
            "limit": limit,
            "page": 1,
            "rows": [],
            "id": "_id"
        }];
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

        if(reconAudit.result[i].situation === "CONFIRMED") {
            reconAudit.result[i].hasLink = true;
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
