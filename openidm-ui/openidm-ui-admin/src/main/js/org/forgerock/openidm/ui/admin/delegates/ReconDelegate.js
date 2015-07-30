/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All rights reserved.
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

/*global define */

define("org/forgerock/openidm/ui/admin/delegates/ReconDelegate", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/SpinnerManager"
], function($, _, constants, AbstractDelegate, configuration, eventManager, spinner) {

    var obj = new AbstractDelegate(constants.host + "/openidm/recon");

    obj.waitForAll = function (reconIds, suppressSpinner, progressCallback) {
            var resultPromise = $.Deferred(),
                completedRecons = [],
                checkCompleted;

            checkCompleted = function () {

                obj.serviceCall({
                    "type": "GET",
                    "url": "/" + reconIds[completedRecons.length],
                    "suppressSpinner": suppressSpinner,
                    "errorsHandlers": {
                        "Not found": {
                            status: 404
                        }
                    }
                }).then(function (reconStatus) {

                    if (progressCallback) {
                        progressCallback(reconStatus);
                    }

                    if (reconStatus.ended.length !== 0) {
                        completedRecons.push(reconStatus);
                        if (completedRecons.length === reconIds.length) {
                            resultPromise.resolve(completedRecons);
                        } else {
                            _.delay(checkCompleted, 1000);
                        }
                    } else {
                        if (!suppressSpinner) {
                            spinner.showSpinner();
                        }
                        _.delay(checkCompleted, 1000);
                    }


                }, function () {
                    // something went wrong with the read on /recon/_id, perhaps this recon was interrupted during a restart of the server?

                    completedRecons.push({
                        "reconId": reconIds[completedRecons.length],
                        "status": "failed"
                    });

                    if (completedRecons.length === reconIds.length) {
                        resultPromise.resolve(completedRecons);
                    } else {
                        _.delay(checkCompleted, 1000);
                    }
                });

            };

            if (!suppressSpinner) {
                spinner.showSpinner();
            }
            _.delay(checkCompleted, 100);

            return resultPromise;
        };



    obj.triggerRecons = function (mappings, suppressSpinner) {
        var reconIds = [],
            reconPromises = [];

        _.each(mappings, function (m) {
            reconPromises.push(obj.serviceCall({
                    "suppressSpinner": suppressSpinner,
                    "url": "?_action=recon&mapping=" + m,
                    "type": "POST"
                }).then(function (reconId) {
                    reconIds.push(reconId._id);
                }));
        });

        return $.when.apply($, reconPromises);

    };

    obj.triggerRecon = function (mapping, suppressSpinner, progressCallback) {

        return obj.serviceCall({
            "suppressSpinner": suppressSpinner,
            "url": "?_action=recon&mapping=" + mapping,
            "type": "POST"
        }).then(function (reconId) {

            return obj.waitForAll([reconId._id], suppressSpinner, progressCallback)
                      .then(function (reconArray) {
                        return reconArray[0];
                      }) ;
        });

    };

    obj.triggerReconById = function (mapping, id, suppressSpinner) {

        return obj.serviceCall({
            "suppressSpinner": suppressSpinner,
            "url": "?_action=reconById&mapping=" + mapping + "&ids=" + id,
            "type": "POST"
        }).then(function (reconId) {
            return obj.waitForAll([reconId._id], suppressSpinner);
        });

    };

    obj.stopRecon = function (id, suppressSpinner) {
        return obj.serviceCall({
            "suppressSpinner": suppressSpinner,
            "serviceUrl": "/openidm/recon/" + id,
            "url": "?_action=cancel",
            "type": "POST"
        });
    };

    obj.getNewLinksFromRecon = function(reconId, endDate){
        var newLinks = [],
            prom = $.Deferred(),
            linkPromArray = [],
            queryFilter = 'reconId eq "' + reconId + '" and !(entryType eq "summary") and timestamp gt "' + endDate + '"',
            getTargetObj = _.bind(function(link){
                return this.serviceCall({
                    "type": "GET",
                    "serviceUrl": "/openidm/" + link.targetObjectId,
                    "url":  ""
                }).then(function(targetObject){
                    newLinks.push({ sourceObjectId: link.sourceObjectId , targetObject: targetObject });
                    return;
                });
            }, this);

        this.serviceCall({
            "type": "GET",
            "serviceUrl": "/openidm/repo/audit/recon",
            "url":  "?_queryFilter=" + encodeURIComponent(queryFilter)
        }).then(function(qry){
            if(qry.result.length){
                _.each(qry.result, function(link){
                    linkPromArray.push(getTargetObj(link));
                });

                $.when.apply($,linkPromArray).then(function(){
                    prom.resolve(newLinks);
                });
            } else {
                return prom.resolve(newLinks);
            }
        });

        return prom;
    };

    obj.getLastAuditForObjectId = function(reconId, objectIdType, objectId) {
        var queryFilter = 'reconId eq "' + reconId + '" and ' + objectIdType + ' eq "' + objectId + '"';
        return obj.serviceCall({
            "type": "GET",
            "serviceUrl": "/openidm/repo/audit/recon",
            "url":  "?_queryFilter=" + encodeURIComponent(queryFilter)
        });
    };

    return obj;
});
