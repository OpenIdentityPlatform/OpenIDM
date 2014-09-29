/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All rights reserved.
 */

/*global $, define, _ */
/*jslint evil: true */

define("org/forgerock/openidm/ui/admin/delegates/SyncDelegate", [
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate"
], function(constants, AbstractDelegate, configuration, eventManager, configDelegate) {

    var obj = new AbstractDelegate(constants.host + "/openidm/sync");

    /*obj.performAction = function (reconId, mapping, action, sourceId, targetId) {
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
                                "If-None-Match": "*"
                            }
                        }));
                    }

                    return $.when.apply($, deletePromises);
                });
        }
    };
    
    obj.addToOrphanarium = function (id, orgId) {
        var promise = $.Deferred();

        if (!id) {
            promise.resolve();
            return promise;
        }

        obj.serviceCall({
            "serviceUrl": constants.host + "/openidm/repo/orphanarium/",
            "url": encodeURIComponent(id),
            "type": "GET",
            "errorsHandlers": {
                "missing": {
                    status: 404
                }                
            }
        }).then(
            function (orphan) {
                if (_.indexOf(orphan.orgs, orgId) === -1) {

                    obj.serviceCall({
                        "serviceUrl": constants.host + "/openidm/repo/orphanarium/",
                        "url": encodeURIComponent(id),
                        "type": "PUT",
                        "headers": {
                            "If-Match": "*"
                        },
                        "data": JSON.stringify({"orgs": _.union(orphan.orgs, orgId)})
                    }).then(function () {
                        promise.resolve(); 
                    });

                } else {
                    // must have already been in the orphanarium
                    promise.resolve(); 
                }
            },
            function () {

                obj.serviceCall({
                    "serviceUrl": constants.host + "/openidm/repo/orphanarium/",
                    "url": encodeURIComponent(id),
                    "type": "PUT",
                    "headers": {
                        "If-None-Match": "*"
                    },
                    "data": JSON.stringify({"orgs": [orgId]})
                }).then(function () {
                    promise.resolve(); 
                });

            }            
        );


        return promise;
    };
    
    obj.removeFromOrphanarium = function (id, orgId) {
        var promise = $.Deferred();

        if (!id) {
            promise.resolve();
            return promise;
        }

        obj.serviceCall({
            "serviceUrl": constants.host + "/openidm/repo/orphanarium/",
            "url": encodeURIComponent(id),
            "type": "GET",
            "errorsHandlers": {
                "missing": {
                    status: 404
                }                
            }
        }).then(
            function (orphan) {
                if (orphan.orgs.length > 1 && _.indexOf(orphan.orgs, orgId) !== -1) {

                    obj.serviceCall({
                        "serviceUrl": constants.host + "/openidm/repo/orphanarium/",
                        "url": encodeURIComponent(id),
                        "type": "PUT",
                        "headers": {
                            "If-Match": "*"
                        },
                        "data": JSON.stringify({"orgs": _.filter(orphan.orgs, function (o) { return o !== orgId; })})
                    }).then(function () {
                        promise.resolve(); 
                    });

                } else if (orphan.orgs.length === 0 || orphan.orgs[0] === orgId) {

                    obj.serviceCall({
                        "serviceUrl": constants.host + "/openidm/repo/orphanarium/",
                        "url": encodeURIComponent(id),
                        "type": "DELETE",
                        "headers": {
                            "If-Match": "*"
                        }
                    }).then(function () {
                        promise.resolve(); 
                    });

                } else {
                    promise.resolve(); 
                }
            },
            function () {
                promise.resolve();  
            });


        return promise;
    };*/


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


    return obj;
});
