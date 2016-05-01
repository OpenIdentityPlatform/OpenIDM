/**
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014-2016 ForgeRock AS.
 */

/* eslint no-eval: 0 */

define([
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate"
], function($, _, constants, AbstractDelegate, configuration, eventManager, configDelegate) {

    var obj = new AbstractDelegate(constants.host + "/openidm/sync");

    obj.performAction = function (reconId, mapping, action, sourceId, targetId, linkType) {
        var params = {
            _action: "performAction",
            reconId: reconId,
            mapping: mapping,
            action: action
        };

        if (sourceId) {
            params.sourceId = sourceId;
        } else {
            params.target = true;
        }

        if(linkType) {
            params.linkType = linkType;
        }

        if (targetId) {
            params.targetId = targetId;
        }

        if (!sourceId && !targetId) {
            return $.Deferred().resolve();
        }

        return obj.serviceCall({
            url: "?" + $.param(params),
            type: "POST"
        });
    };

    obj.deleteLinks = function (linkType, id, ordinal) { // ordinal is either firstId or secondId
        if (!_.contains(["firstId","secondId"], ordinal)) {
            throw "Unexpected value passed to deleteLinks: " + ordinal;
        }
        if (!id) {
            return $.Deferred().resolve();
        } else {

            return obj.serviceCall({
                "serviceUrl": constants.host + "/openidm/repo/link",
                "url": "?_queryId=links-for-" + ordinal + "&linkType=" + linkType + "&" + ordinal + "=" + encodeURIComponent(id)
            }).then(function (qry) {
                var i, deletePromises = [];
                for (i=0;i<qry.result.length;i++) {
                    deletePromises.push(obj.serviceCall({
                        "serviceUrl": constants.host + "/openidm/repo/link/",
                        "url" : qry.result[i]._id,
                        "type": "DELETE",
                        "headers": {
                            "If-Match": qry.result[i]._rev
                        }
                    }));
                }

                return $.when.apply($, deletePromises);
            });
        }
    };


    obj.conditionAction = function (sourceObject, p) {
        var result = "",
            object = {};

        if (typeof(p.condition) === "object" && p.condition.type === "text/javascript" &&
                typeof (p.condition.source) === "string") {

            object = sourceObject;

            try {
                result = (eval(p.condition.source) || p.condition.source.length === 0) ? "UPDATE": "DO NOT UPDATE"; // references to "object" variable expected within this string
            } catch (e) {
                result = "ERROR WITH SCRIPT";
            }

        }

        return result;
    };


    obj.translatePropertyToTarget = function (sourceObject, p) {
        var sampleData = null,
            source = {};

        if (typeof(p.transform) === "object" && p.transform.type === "text/javascript" &&
                typeof (p.transform.source) === "string") {

            if (typeof(p.source) !== "undefined" && p.source.length) {
                source = sourceObject[p.source];
            } else {
                source = sourceObject;
            }
            try {
                sampleData = eval(p.transform.source); // references to "source" variable expected within this string
            } catch (e) {
                sampleData = "ERROR WITH SCRIPT";
            }

        } else if (typeof(p.source) !== "undefined" && p.source.length) {

            sampleData = sourceObject[p.source];

        }

        if (typeof(p["default"]) !== "undefined" && p["default"].length) {

            if (sampleData === null || sampleData === undefined) {
                sampleData = p["default"];
            }

        }

        return [p.target, sampleData];
    };

    obj.translateToTarget = function(sourceObject, syncMappingConfig) {

        return _.chain(syncMappingConfig.properties)
                .map(function (p) {
                    return obj.translatePropertyToTarget(sourceObject, p);
                })
                .object()
                .value();

    };

    obj.mappingDetails = function(mapping){
        var promise = $.Deferred(),
            url = "",
            serviceUrl = "/openidm/endpoint/mappingDetails",
            doServiceCall = function(){
                return obj.serviceCall({
                    "type": "GET",
                    "serviceUrl": serviceUrl,
                    "url": url,
                    "errorsHandlers": {
                        "missing": {
                            status: 404
                        }
                    }
                }).done(function (mappingDetails) {
                    promise.resolve(mappingDetails);
                });
            };

        if(mapping){
            url = "?mapping=" + mapping;
        }

        doServiceCall().fail(function (xhr) {
            if(xhr.status === 404){
                configDelegate.createEntity("endpoint/mappingDetails", {
                    "context" : "endpoint/mappingDetails",
                    "type" : "text/javascript",
                    "file" : "ui/mappingDetails.js"
                }).then(function(){
                    _.delay(doServiceCall, 2000);
                });
            }
        });

        return promise;
    };


    return obj;
});
