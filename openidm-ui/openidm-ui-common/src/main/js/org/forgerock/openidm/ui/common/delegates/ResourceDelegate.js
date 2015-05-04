/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 ForgeRock AS. All rights reserved.
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

/**
 * @author huck.elliott
 */
define("org/forgerock/openidm/ui/common/delegates/ResourceDelegate", [
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/components/Messages"
], function(constants, AbstractDelegate, configDelegate, messagesManager) {

    var obj = new AbstractDelegate(constants.host + "/openidm/");

    obj.getSchema = function(args){
        var objectType = args[0],
            objectName = args[1],
            objectName2 = args[2];

        if(objectType === "managed") {
            return configDelegate.readEntity("managed").then(function(managed){
                var managedObject = _.findWhere(managed.objects,{ name: objectName });

                if(managedObject){
                    if(managedObject.schema){
                        return managedObject.schema;
                    } else {
                        return false;
                    }
                } else {
                    return "invalidObject";
                }
            });
        } else {
            return obj.getProvisioner(objectType, objectName).then(function(prov){
                var schema;

                if(prov.objectTypes){
                    schema = prov.objectTypes[objectName2];
                    if(schema){
                        schema.title = objectName;
                        return schema;
                    } else {
                        return false;
                    }
                } else {
                    return "invalidObject";
                }
            });
        }
    };

    obj.createResource = function (serviceUrl) {
        return AbstractDelegate.prototype.createEntity.apply(_.extend({}, AbstractDelegate.prototype, this, {"serviceUrl": serviceUrl}), _.toArray(arguments).slice(1));
    };
    obj.readResource = function (serviceUrl) {
        return AbstractDelegate.prototype.readEntity.apply(_.extend({}, AbstractDelegate.prototype, this, {"serviceUrl": serviceUrl}), _.toArray(arguments).slice(1));
    };
    obj.updateResource = function (serviceUrl) {
        return AbstractDelegate.prototype.updateEntity.apply(_.extend({}, AbstractDelegate.prototype, this, {"serviceUrl": serviceUrl}), _.toArray(arguments).slice(1));
    };
    obj.deleteResource = function (serviceUrl, id, successCallback, errorCallback) {
        var callParams = {
                serviceUrl: serviceUrl, url: "/" + id, 
                type: "DELETE", 
                success: successCallback, 
                error: errorCallback,
                errorsHandlers: {
                    "Conflict": {
                        status: 409
                    }
                },
                headers: {
                    "If-Match": "*"
                }
            };
        
        return obj.serviceCall(callParams).fail(function(err){
            var response = err.responseJSON;
            if(response.code === 409) {
                messagesManager.messages.addMessage({"type": "error", "message": response.message});
            }
        });
    };
    obj.patchResourceDifferences = function (serviceUrl) {
        return AbstractDelegate.prototype.patchEntityDifferences.apply(_.extend({}, AbstractDelegate.prototype, this, {"serviceUrl": serviceUrl}), _.toArray(arguments).slice(1));
    };

    obj.getServiceUrl = function(args) {
        var url = "/" + constants.context + "/" + args[0] + "/" + args[1];

        if(args[0] === "system") {
            url += "/" + args[2];
        }

        return url;
    };

    obj.searchResource = function(filter, serviceUrl) {
        return obj.serviceCall({
            url: serviceUrl +"?_queryFilter="+filter
        });
    };
    
    obj.getProvisioner = function(objectType, objectName) {
        return obj.serviceCall({
            serviceUrl: obj.serviceUrl + objectType + "/" + objectName,
            url: "?_action=test",
            type: "POST"
        }).then(function(connector) {
            var config = connector.config.replace("config/","");
            
            return configDelegate.readEntity(config);
        });
    };

    return obj;
});