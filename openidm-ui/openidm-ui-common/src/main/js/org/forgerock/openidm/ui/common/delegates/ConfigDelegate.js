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

/*global $, define, _ */

/**
 * @author jfeasel
 */
define("org/forgerock/openidm/ui/common/delegates/ConfigDelegate", [
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager"
], function(constants, AbstractDelegate, conf, eventManager) {

    var obj = new AbstractDelegate(constants.host + "/openidm/config");


    obj.serviceCall = function (callParams) {

        // we don't want 404 errors to the config store to flash the typical "Not Found" warning to the user,
        // so we override the default 404 behavior with this simple stub handler.
        var defaultErrorsHandlers =  {
            "Not found": {
                status: 404
            }
        };

        callParams.errorsHandlers = _.extend(callParams.errorsHandlers || {}, defaultErrorsHandlers);

        return Object.getPrototypeOf(obj).serviceCall.call(obj,callParams);
    };

    obj.getConfigList = function(successCallback, errorCallback) {
        return obj.serviceCall({ url:"", type: "GET", success: successCallback, error: errorCallback});
    };

    obj.readEntity = function (id, successCallback, errorCallback) {
        var promise = $.Deferred(),
            clone;

        if (!conf.delegateCache.config || !conf.delegateCache.config[id]) {
            if (!conf.delegateCache.config) {
                conf.delegateCache.config = {};
            }
            return Object.getPrototypeOf(obj).readEntity.call(obj,id, successCallback, errorCallback)
                .then(
                function (result) {
                    conf.delegateCache.config[id] = result;
                    return $.extend(true, {}, result);
                },
                function () {
                    delete conf.delegateCache.config[id];
                });
        } else {
            clone = $.extend(true, {}, conf.delegateCache.config[id]);
            promise.resolve(clone);
            if (successCallback) {
                successCallback(clone);
            }
            return promise;
        }
    };

    obj.updateEntity = function(id, objectParam, successCallback, errorCallback) {
        return Object.getPrototypeOf(obj).updateEntity.call(obj, id, objectParam, successCallback, errorCallback)
            .always(function () {
                delete conf.delegateCache.config[id];
            });
    };

    obj.deleteEntity = function(id, successCallback, errorCallback) {
        return Object.getPrototypeOf(obj).deleteEntity.call(obj, id, successCallback, errorCallback)
            .always(function () {
                delete conf.delegateCache.config[id];
            });
    };

    obj.clearDelegateCache = function(id){
        if (id) {
            delete conf.delegateCache.config[id];
        } else {
            conf.delegateCache.config = {};
        }
    };

    return obj;
});
