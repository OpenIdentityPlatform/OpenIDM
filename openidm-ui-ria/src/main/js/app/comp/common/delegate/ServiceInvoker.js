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
define("app/comp/common/delegate/ServiceInvoker",
        ["app/util/Constants",
         "app/comp/common/eventmanager/EventManager", 
         "app/util/ObjectUtil",
         "app/comp/common/configuration/AbstractConfigurationAware"],
         function(constants,em,objUtil,AbstractConfigurationAware) {

    var obj = new AbstractConfigurationAware();

    obj.restCall = function(callParamsParam) {
        var current = this, callParams, realSuccess, realError;

        callParamsParam.contentType = 'application/json; charset=utf-8';
        callParams = callParamsParam;
        obj.applyDefaultHeadersIfNecessary(callParams, obj.configuration.defaultHeaders);

        //TODO This line can be deleted when the bug https://bugster.forgerock.org/jira/browse/OPENIDM-568 is fixed
        if(callParams.headers[constants.OPENIDM_HEADER_PARAM_NO_SESION] === false) {
            delete callParams.headers[constants.OPENIDM_HEADER_PARAM_NO_SESION];
        }

        em.sendEvent(constants.EVENT_START_REST_CALL);
        realSuccess = callParams.success;
        realError = callParams.error;
        callParams.success = function (data,textStatus, jqXHR) {
            em.sendEvent(constants.EVENT_END_REST_CALL, { data: data, textStatus: textStatus, jqXHR: jqXHR});
            if(realSuccess) {
                realSuccess(data);
            }
        };
        callParams.error = function (data,textStatus, jqXHR) {
            //TODO try to handle error
            em.sendEvent(constants.EVENT_REST_CALL_ERROR, { data: data, textStatus: textStatus, jqXHR: jqXHR});
            if(realError) {
                realError(data);
            }
        };
        callParams.dataType = "json";
        callParams.contentType = "application/json";

        $.ajax(callParams);	
    };

    /**
     * Test TODO create test using below formula
     * var x = {headers:{"a": "a"},b:"b"};
     * require("app/comp/common/delegate/ServiceInvoker").applyDefaultHeadersIfNecessary(x, {a:"x",b:"b"});
     * y ={};
     * require("app/comp/common/delegate/ServiceInvoker").applyDefaultHeadersIfNecessary(y, {a:"c",d:"c"});
     */
    obj.applyDefaultHeadersIfNecessary = function(callParams, defaultHeaders) {
        var oneHeaderName;
        if(!defaultHeaders) {
            return;
        }
        if(!callParams.headers) {
            callParams.headers = defaultHeaders;
        } else {
            for(oneHeaderName in defaultHeaders) {
                if(callParams.headers[oneHeaderName] === undefined) {
                    callParams.headers[oneHeaderName] = defaultHeaders[oneHeaderName];
                }
            }
        }
    };

    return obj;
});