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
 * @author jdabrowski
 */
define("org/forgerock/openidm/ui/admin/workflow/WorkflowDelegate", [
    "org/forgerock/commons/ui/common/util/Constants", 
    "org/forgerock/commons/ui/common/main/ServiceInvoker"
], function(constants, serviceInvoker) {
    
    var obj = {}, taskManagementUrl, processManagementUrl, taskDefinitionUrl, processDefinitionUrl, endpointUrl, processDefinitionsEndpointUrl;
    
    taskManagementUrl       =   "/openidm/workflow/taskinstance";
    processManagementUrl    =   "/openidm/workflow/processinstance";
    processDefinitionUrl = "/openidm/workflow/processdefinition";
    endpointUrl = "/openidm/endpoint/gettasksview";
    processDefinitionsEndpointUrl = "/openidm/endpoint/getprocessesforuser";


    obj.startProccess = function(proccessNameKey, params, successCallback, errorCallback) {
        console.debug("start proccess");
        params._key = proccessNameKey;
        this.serviceCall({url: processManagementUrl + "/?_action=create", type: "POST", success: successCallback, error: errorCallback, data: JSON.stringify(params)});
    };
    
    obj.startProcessById = function(processDefinitionId, params, successCallback, errorCallback) {
        console.debug("start proccess");
        params._processDefinitionId = processDefinitionId;
        this.serviceCall({url: processManagementUrl + "/?_action=create", type: "POST", success: successCallback, error: errorCallback, data: JSON.stringify(params)});
    };

    obj.completeTask = function(id, params, successCallback, errorCallback) {
        console.debug("complete task");
        this.serviceCall({url: taskManagementUrl + "/" + id + "?_action=complete", type: "POST", success: successCallback, error: errorCallback, data: JSON.stringify(params)});
    };
    
    obj.getProcessDefinition = function(id, successCallback, errorCallback) {
        this.serviceCall({url: processDefinitionUrl + "/" + id, type: "GET", success: successCallback, error: errorCallback});
    };
 
    obj.getAllProcessDefinitions = function(userId, successCallback, errorCallback) {
        console.info("getting all process definitions");
        
        obj.serviceCall({
            url: processDefinitionsEndpointUrl + "?_queryId=getprocessesforuser&userId=" + userId,  
            type: "GET",
            success: function(data) {
                if(successCallback) {
                    successCallback(data.result);
                }
            }, 
            error: errorCallback
        });
    };
        
    obj.getAllUniqueProcessDefinitions = function(userId, successCallback, errorCallback) {
        obj.getAllProcessDefinitions(userId, function(processDefinitions) {
            
            var result = {}, ret = [], i, processDefinition, splittedProcessDefinition, processName, currentProcessVersion, newProcesVersion, r;
            for (i=0; i < processDefinitions.length; i++) {
                processDefinition = processDefinitions[i];
                splittedProcessDefinition = processDefinition._id.split(':');
                processName = splittedProcessDefinition[0];
                if (result[processName]) {
                    currentProcessVersion = result[processName]._id.split(':')[1];
                    newProcesVersion = splittedProcessDefinition[1];
                    if (parseInt(newProcesVersion,10) > parseInt(currentProcessVersion,10)) {
                        result[processName] = processDefinition;
                    }
                } else {
                    result[processName] = processDefinition;
                }
            }
            for (r in result) {
                ret.push(result[r]);
            }
            successCallback(ret);
        }, errorCallback);
    };
   
    obj.serviceCall = function(callParams) {
        serviceInvoker.restCall(callParams);
    };
    
    obj.assignTaskToUser = function(taskId, userName, successCallback, errorCallback) {
        var callParams, params;
        console.debug("assign user to task");
        params = {assignee: userName};
        callParams =  {url: taskManagementUrl + "/" + taskId, type: "PUT", success: successCallback, error: errorCallback, data: JSON.stringify(params)};
        callParams.headers = [];
        callParams.headers["If-Match"] = '"*"';
        this.serviceCall(callParams);
    };

    obj.getAllTaskUsingEndpoint = function(userId, successCallback, errorCallback) {
        obj.serviceCall({
            url: endpointUrl + "?_queryId=gettasksview&userId=" + userId, 
            type: "GET",
            success: function(data) {
                if(_.isEmpty(data.result[0])) {
                    errorCallback();
                } else if(successCallback) {
                    successCallback(data.result[0]);
                }
            }, 
            error: errorCallback
        });
    };
    
    obj.getMyTaskUsingEndpoint = function(userId, successCallback, errorCallback) {
        obj.serviceCall({
            url: endpointUrl + "?_queryId=gettasksview&userId=" + userId + "&viewType=assignee",  
            type: "GET",
            success: function(data) {
                if(_.isEmpty(data.result[0])) {
                    errorCallback();
                } else if(successCallback) {
                    successCallback(data.result[0]);
                }
            }, 
            error: errorCallback
        });
    };
    
    return obj;
});



