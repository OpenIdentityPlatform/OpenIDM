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
 * Copyright 2011-2016 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/SpinnerManager",
    "org/forgerock/commons/ui/common/main/Router"
], function($, _, constants, AbstractDelegate, configuration, eventManager, spinner, router) {

    var obj = new AbstractDelegate(constants.host + "/openidm/recon");

    obj.waitForAll = function (reconIds, suppressSpinner, progressCallback, delayTime) {
        var resultPromise = $.Deferred(),
            completedRecons = [],
            checkCompleted,
            startView = router.currentRoute.view;

        if (!delayTime) {
            delayTime = 1000;
        }

        checkCompleted = function () {
            /**
            * Check to make sure we are still on the same page we were when this process
            * started. If not then cancel the process so ajax requests
            * will not continue to fire in the background.
            */
            if (router.currentRoute.view === startView) {
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
                            _.delay(checkCompleted, delayTime);
                        }
                    } else {
                        if (!suppressSpinner) {
                            spinner.showSpinner();
                        }
                        _.delay(checkCompleted, delayTime);
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
                        _.delay(checkCompleted, delayTime);
                    }
                });
            } else {
                resultPromise.reject();
            }
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

    obj.triggerRecon = function (mapping, suppressSpinner, progressCallback, delayTime) {
        return obj.serviceCall({
            "suppressSpinner": suppressSpinner,
            "url": "?_action=recon&mapping=" + mapping,
            "type": "POST"
        }).then(function (reconId) {

            return obj.waitForAll([reconId._id], suppressSpinner, progressCallback, delayTime)
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
