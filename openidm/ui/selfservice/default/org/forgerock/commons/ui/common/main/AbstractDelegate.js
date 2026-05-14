"use strict";

var _typeof = typeof Symbol === "function" && typeof Symbol.iterator === "symbol" ? function (obj) { return typeof obj; } : function (obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; };

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

define(["underscore", "org/forgerock/commons/ui/common/util/Constants", "org/forgerock/commons/ui/common/main/Configuration", "org/forgerock/commons/ui/common/main/ServiceInvoker"], function (_, constants, configuration, serviceInvoker) {

    var obj = function AbstractDelegate(serviceUrl) {
        var baseRegex = new RegExp("^/" + constants.context + "\/([\\w/]*)"),
            baseEntity = serviceUrl.match(baseRegex);
        this.serviceUrl = serviceUrl;

        if (baseEntity && baseEntity.length > 1) {
            this.baseEntity = baseEntity[1];
        }
    };

    obj.prototype.serviceCall = function (callParams) {

        if (!callParams.hasOwnProperty('headers')) {
            callParams.headers = {};
        }
        if (configuration.hasOwnProperty('passwords') && configuration.passwords.hasOwnProperty('password')) {
            callParams.headers[constants.HEADER_PARAM_REAUTH] = configuration.passwords.password;
        }

        callParams.url = callParams.hasOwnProperty('url') ? callParams.url : "";
        if (callParams.serviceUrl) {
            callParams.url = callParams.serviceUrl + callParams.url;
        } else {
            callParams.url = this.serviceUrl + callParams.url;
        }
        return serviceInvoker.restCall(callParams);
    };

    obj.prototype.createEntity = function (id, objectParam, successCallback, errorCallback) {
        var headers = {};

        if ((typeof id === "undefined" ? "undefined" : _typeof(id)) === "object" && id !== null) {
            throw "Invalid id value passed to createEntity";
        }

        if (id !== null && id !== undefined) {
            if (objectParam._rev) {
                headers["If-None-Match"] = '"' + objectParam._rev + '"';
            } else {
                headers["If-None-Match"] = '"' + "*" + '"';
            }
            return this.serviceCall({ url: "/" + id,
                type: "PUT",
                success: successCallback,
                error: errorCallback,
                data: JSON.stringify(objectParam),
                headers: headers
            });
        } else {
            return this.serviceCall({ url: "?_action=create",
                type: "POST",
                success: successCallback,
                error: errorCallback,
                data: JSON.stringify(objectParam),
                headers: headers
            });
        }
    };

    obj.prototype.deleteEntity = function (id, successCallback, errorCallback) {
        var current = this;
        return this.readEntity(id, null, errorCallback).then(function (data) {
            var callParams = { url: "/" + id, type: "DELETE", success: successCallback, error: errorCallback };
            if (data._rev) {
                callParams.headers = [];
                callParams.headers["If-Match"] = '"' + data._rev + '"';
            }
            return current.serviceCall(callParams);
        });
    };

    obj.prototype.readEntity = function (id, successCallback, errorCallback) {
        return this.serviceCall({ url: "/" + id, type: "GET", success: successCallback, error: errorCallback });
    };

    obj.prototype.updateEntity = function (id, objectParam, successCallback, errorCallback) {
        var headers = {};

        if (objectParam._rev) {
            headers["If-Match"] = '"' + objectParam._rev + '"';
        } else {
            headers["If-Match"] = '"' + "*" + '"';
        }

        return this.serviceCall({ url: "/" + id,
            type: "PUT",
            success: successCallback,
            error: errorCallback,
            data: JSON.stringify(objectParam),
            headers: headers
        });
    };

    /**
     * Discovers differences between new and old object and invokes patch action only on attributes which are not equal.
     */
    obj.prototype.patchEntityDifferences = function (queryParameters, oldObject, newObject, successCallback, errorCallback, noChangesCallback, errorsHandlers) {

        var differences = this.getDifferences(oldObject, newObject);
        if (!differences.length) {
            if (noChangesCallback) {
                noChangesCallback();
            }
            return;
        }
        return this.patchEntity(queryParameters, differences, successCallback, errorCallback, noChangesCallback, errorsHandlers);
    };

    /**
     * Invokes patch action which modify only selected object attributes defined as PATCH action compatible JSON
     * object {"operation": "replace", "field": "fieldname", value: "value" }
     */
    obj.prototype.patchEntity = function (queryParameters, patchDefinition, successCallback, errorCallback, noChangesCallback, errorsHandlers) {
        //simple transformation
        var i;
        for (i = 0; i < patchDefinition.length; i++) {
            if (typeof patchDefinition[i].field === "string" && _.indexOf(patchDefinition[i].field, "/") !== 0) {
                patchDefinition[i].field = "/" + patchDefinition[i].field;
            }
        }
        return this.serviceCall({ url: "/" + queryParameters.id,
            type: "PATCH",
            success: successCallback,
            error: errorCallback,
            data: JSON.stringify(patchDefinition),
            headers: {
                "If-Match": '"' + queryParameters.rev + '"'
            },
            errorsHandlers: errorsHandlers
        });
    };

    /**
     *  Patches single attribute
     */
    obj.prototype.patchEntityAttribute = function (queryParameters, attributeName, newValue, successCallback, errorCallback, noChangesCallback) {
        return this.patchEntity(queryParameters, [{ operation: "replace", field: attributeName, value: newValue }], successCallback, errorCallback, noChangesCallback);
    };

    /**
     * Returns differences between new and old object as a PATCH action compatible JSON object
     */
    obj.prototype.getDifferences = function (oldObject, newObject, method) {
        var newValue,
            oldValue,
            field,
            fieldContents,
            result = [],
            patchCmd = {};
        if (!method) {
            method = "replace";
        }
        for (field in newObject) {
            fieldContents = newObject[field];
            if (typeof fieldContents !== "function") {
                newValue = newObject[field];
                oldValue = oldObject[field];
                if ((newValue !== "" || oldValue) && !_.isEqual(newValue, oldValue)) {
                    patchCmd = {
                        operation: method,
                        field: field,
                        value: newValue
                    };
                    result.push(patchCmd);
                }
            }
        }
        return result;
    };

    return obj;
});
