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

/*global $, define, _ */

define("org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate", [
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/EventManager"
], function(constants, AbstractDelegate, eventManager) {

    var obj = new AbstractDelegate(constants.host + "/openidm/system");

    obj.connectorDelegateCache = {};

    obj.availableConnectors = function() {
        return obj.serviceCall({
            url: "?_action=availableConnectors",
            type: "POST"
        });
    };

    obj.detailsConnector = function(connectorParams) {
        return obj.serviceCall({
            url: "?_action=createCoreConfig",
            type: "POST",
            data: JSON.stringify(connectorParams)
        });
    };

    obj.testConnector = function(connectorParams) {
        var errorHandlers = {
            "error": {
                status: "500"
            }
        };

        return obj.serviceCall({
            url: "?_action=createFullConfig",
            type: "POST",
            data: JSON.stringify(connectorParams),
            errorsHandlers: errorHandlers
        });
    };


    obj.currentConnectors = function() {
        var deferred = $.Deferred(),
            promise = deferred.promise();

        if(obj.connectorDelegateCache.currentConnectors) {
            deferred.resolve(_.clone(obj.connectorDelegateCache.currentConnectors));
        } else {
            obj.serviceCall({
                url: "?_action=test",
                type: "POST"
            }).then(function(result){
                obj.connectorDelegateCache.currentConnectors = result;

                deferred.resolve(result);
            });
        }

        return promise;
    };

    obj.deleteCurrentConnectorsCache = function() {
        delete obj.connectorDelegateCache.currentConnectors;
    };

    obj.connectorDefault = function(name, type) {
        return $.ajax({
            dataType: "json",
            type: "GET",
            url: "/admin/templates/admin/connector/configs/" +type +"/" +name +".json"
        });
    };

    obj.templateCheck = function(name) {
        return $.ajax({
            type: "GET",
            url: "/admin/templates/admin/connector/" +name +".html"
        });
    };

    return obj;
});