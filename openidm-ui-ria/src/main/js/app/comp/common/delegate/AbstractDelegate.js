/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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

/*global $, define*/

/**
 * @author yaromin
 */
define("app/comp/common/delegate/AbstractDelegate",["app/util/Constants", "./ServiceInvoker"],
        function(constants, serviceInvoker) {

    var obj = function AbstractDelegate(serviceUrl) {
        this.serviceUrl = serviceUrl;
    };

    obj.prototype.serviceCall = function(callParams) {
        callParams.url = this.serviceUrl + callParams.url;
        serviceInvoker.restCall(callParams);
    };

    obj.prototype.createEntity = function(object, successCallback, errorCallback) {
        console.debug("create entity");
        this.serviceCall({url: "/" + object.userName, type: "PUT", success: successCallback, error: errorCallback, data: JSON.stringify(object)});
    };

    obj.prototype.deleteEntity = function(id, successCallback, errorCallback) {
        console.debug("delete entity");
        var current = this;
        this.readEntity(id, function(data) {
            var callParams = {url: "/" + id, type: "DELETE", success: successCallback, error: errorCallback };
            if(data._rev) {
                callParams.headers = [];
                callParams.headers["If-Match"] = '"' + data._rev + '"';
            }
            current.serviceCall(callParams);
        }, errorCallback);		

    };

    obj.prototype.readEntity = function(id, successCallback, errorCallback) {
        console.debug("get entity");
        this.serviceCall({url: "/" + id, type: "GET", success: successCallback, error: errorCallback});
    };

    obj.prototype.updateEntity = function(objectParam, successCallback, errorCallback) {
        console.debug("update entity");
        this.serviceCall({url: "/" + objectParam._id, type: "POST", success: successCallback, error: errorCallback, data: objectParam});
    };

    /**
     * Discovers differences between new and old object and invokes patch action only on attributes which are not equal.
     */
    obj.prototype.patchEntityDifferences = function(queryParameters, oldObject, newObject, successCallback, errorCallback, noChangesCallback) {
        console.debug("patching entity");

        var differences = this.getDifferences(oldObject, newObject);
        if(!differences.length){
            console.debug("No changes detected");
            noChangesCallback();
            return;
        }
        this.patchEntity(queryParameters, differences, successCallback, errorCallback, noChangesCallback);
    };

    /**
     * Invokes patch action which modify only selected object attributes defined as PATCH action compatible JSON object {replace: "fieldname", value: "value" } 
     */
    obj.prototype.patchEntity = function(queryParameters, patchDefinition, successCallback, errorCallback, noChangesCallback, fields) {
        //simple transformation
        var i;
        for(i = 0; i < patchDefinition.length; i++) {
            patchDefinition[i].replace = "/" + patchDefinition[i].replace;
        }
        this.serviceCall({url: "/?_action=patch&" + $.param(queryParameters), type: "POST", success: successCallback, error: errorCallback, data: JSON.stringify(patchDefinition)});
    };

    /**
     *  Patches single attribute
     */
    obj.prototype.patchEntityAttribute = function(queryParameters, attributeName, newValue, successCallback, errorCallback, noChangesCallback, fields) {
        this.patchEntity(queryParameters, [{replace: attributeName, value: newValue}], successCallback, errorCallback, noChangesCallback);
    };

    /**
     * Returns differences between new and old object as a PATCH action compatible JSON object
     */
    obj.prototype.getDifferences = function(oldObject, newObject) {
        var newValue, oldValue, field, fieldContents, result = [];
        for ( field in newObject) {
            fieldContents = newObject[field];
            if ( typeof (fieldContents) !== "function") {
                newValue = newObject[field];
                oldValue = oldObject[field];
                if((newValue!=="" || oldValue) && newValue !== oldValue){
                    result.push( { replace: field, value: newValue});
                }
            }
        }
        return result;
    };

    return obj;
});