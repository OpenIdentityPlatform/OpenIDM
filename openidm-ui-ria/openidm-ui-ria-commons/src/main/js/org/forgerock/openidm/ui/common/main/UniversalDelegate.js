/*
 * @license DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2012 ForgeRock AS. All rights reserved.
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
 * @author jdabrowski
 */
define("org/forgerock/openidm/ui/common/main/UniversalDelegate", [
    "org/forgerock/openidm/ui/common/util/Constants", 
    "org/forgerock/openidm/ui/common/main/ServiceInvoker"
], function(constants, serviceInvoker) {

    var obj = {};

    obj.serviceCall = function(callParams) {
        callParams.url = constants.host + "/openidm/managed/" + callParams.url;
        serviceInvoker.restCall(callParams);
    };

    obj.create = function(serviceUrl, object , successCallback, errorCallback) {
        this.serviceCall({url: serviceUrl + "/1", type: "PUT", success: successCallback, error: errorCallback, data: JSON.stringify(object)});
    };

    obj.remove = function(serviceUrl, id, successCallback, errorCallback) {
        var current = this;
        this.read(serviceUrl, id, function(data) {
            var callParams = {url: serviceUrl + "/" + id, type: "DELETE", success: successCallback, error: errorCallback };
            if(data._rev) {
                callParams.headers = [];
                callParams.headers["If-Match"] = '"' + data._rev + '"';
            }
            current.serviceCall(callParams);
        }, errorCallback);      
    };

    obj.read = function(serviceUrl, id, successCallback, errorCallback) {
        this.serviceCall({url: serviceUrl + "/" + id, type: "GET", success: successCallback, error: errorCallback});
    };
    
    obj.readByName = function(serviceUrl, name, successCallback, errorCallback) {
        obj.serviceCall({url: serviceUrl + "/?_query-id=get-"+serviceUrl+"-by-name&" + $.param({name: name}), success: function(data) {
            if(successCallback) {
                successCallback(data.result);
            }
        }, error: errorCallback} );
    };
    
    obj.getAll = function(serviceUrl, successCallback, errorCallback) {
        obj.serviceCall({url: serviceUrl + "/?_query-id=query-all-"+serviceUrl+"s", success: function(data) {
            if(successCallback) {
                successCallback(data.result);
            }
        }, error: errorCallback} );
    };

    obj.update = function(serviceUrl, objectParam, successCallback, errorCallback) {
        this.serviceCall({url: serviceUrl + "/" + objectParam._id, type: "POST", success: successCallback, error: errorCallback, data: objectParam});
    };

    return obj;
});