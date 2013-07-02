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
//    taskDefinitionUrl = "/openidm/workflow/taskdefinition";
    processManagementUrl    =   "/openidm/workflow/processinstance";
    processDefinitionUrl = "/openidm/workflow/processdefinition";
    endpointUrl = "/openidm/endpoint/gettasksview";
    processDefinitionsEndpointUrl = "/openidm/endpoint/getprocessesforuser";


    obj.startProccess = function(proccessNameKey, params, successCallback, errorCallback) {
        console.debug("start proccess");
        params._key = proccessNameKey;
        this.serviceCall({url: processManagementUrl + "/?_action=createProcessInstance", type: "POST", success: successCallback, error: errorCallback, data: JSON.stringify(params)});
    };
    
    obj.startProcessById = function(processDefinitionId, params, successCallback, errorCallback) {
        console.debug("start proccess");
        params._processDefinitionId = processDefinitionId;
        this.serviceCall({url: processManagementUrl + "/?_action=createProcessInstance", type: "POST", success: successCallback, error: errorCallback, data: JSON.stringify(params)});
    };
/*
    obj.deleteProcess = function(id, successCallback, errorCallback) {
        console.debug("delete process");
        this.serviceCall({url: processManagementUrl + "/" + id, type: "DELETE", success: successCallback, error: errorCallback});
    };

    obj.getTask = function(id, successCallback, errorCallback) {
        console.debug("get task");
        this.serviceCall({url: taskManagementUrl + "/" + id, type: "GET", success: successCallback, error: errorCallback});
    };

    obj.getProcess = function(id, successCallback, errorCallback) {
        console.debug("get process instance");
        this.serviceCall({url: processManagementUrl + "/" + id, type: "GET", success: successCallback, error: errorCallback});
    };
    
    obj.getTaskDefinition = function(processDefinitionId, taskDefinitionKey, successCallback, errorCallback) {
        console.debug("get task definition");
        this.serviceCall({url: taskDefinitionUrl + "?_queryId=query-taskdefinition&" 
            + $.param({processDefinitionId: processDefinitionId, taskDefinitionKey: taskDefinitionKey}), success: successCallback, error: errorCallback} );
    };
    
    obj.updateTask = function(id, params, successCallback, errorCallback) {
        console.debug("update task");
        var callParams =  {url: taskManagementUrl + "/" + id, type: "PUT", success: successCallback, error: errorCallback, data: JSON.stringify(params)};
        callParams.headers = [];
        callParams.headers["If-Match"] = '"*"';
        this.serviceCall(callParams);
    };
*/    
    obj.completeTask = function(id, params, successCallback, errorCallback) {
        console.debug("complete task");
        this.serviceCall({url: taskManagementUrl + "/" + id + "?_action=complete", type: "POST", success: successCallback, error: errorCallback, data: JSON.stringify(params)});
    };
    
    obj.getProcessDefinition = function(id, successCallback, errorCallback) {
        this.serviceCall({url: processDefinitionUrl + "/" + id, type: "GET", success: successCallback, error: errorCallback});
    };
/*    
    obj.getAllTasks = function(successCallback, errorCallback) {
        console.info("getting all tasks");

        obj.serviceCall({url: taskManagementUrl + "?_queryId=query-all-ids", success: function(data) {
            if(successCallback) {
                successCallback(data.result);
            }
        }, error: errorCallback} );
    };
    
    obj.getAllProcessInstances = function(successCallback, errorCallback) {
        console.info("getting all process instances");

        obj.serviceCall({url: processManagementUrl + "?_queryId=query-all-ids", success: function(data) {
            if(successCallback) {
                successCallback(data.result);
            }
        }, error: errorCallback} );
    };
*/    
    obj.getAllProcessDefinitions = function(userId, successCallback, errorCallback) {
        console.info("getting all process definitions");
        
        obj.serviceCall({url: processDefinitionsEndpointUrl + "?userId=" + userId, success: function(data) {
            if(successCallback) {
                successCallback(data);
            }
        }, error: errorCallback} );
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
/*    
    obj.getAllTasksForProccess = function(proccessNameKey, successCallback, errorCallback) {
        console.info("getting all unassigned tasks");
        obj.serviceCall({url: taskManagementUrl + "?_queryId=filtered-query&" + $.param({key: proccessNameKey}), success: function(data) {
            if(successCallback) {
                successCallback(data.result);
            }
        }, error: errorCallback} );
    };
    
    obj.getTasksAssignedToUser = function(userName, successCallback, errorCallback) {
        console.info("getting all tasks assigned to user " + userName);
    
        obj.serviceCall({url: taskManagementUrl + "?_queryId=filtered-query&" + $.param({assignee: userName}), success: function(data) {
            if(successCallback) {
                successCallback(data.result);
            }
        }, error: errorCallback} );
    };
*/    
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
/*    
    obj.getTasksAvailableToUser = function(userName, successCallback, errorCallback) {
        obj.getAllTasks(successCallback, errorCallback);
    };
    
    obj.getAllTasksViewForUser = function(userName, successCallback, errorCallback) {
        obj.getTasksAssignedToUser(userName, function(AvailableTasks) {
            obj.buildStandardViewFromTaskBasicDataMap(AvailableTasks, userName, successCallback, errorCallback);
        }, errorCallback);
    };
    
    obj.getAllAvailableTasksViewForUser = function(userName, successCallback, errorCallback) {
        obj.getTasksAvailableToUser(userName, function(AvailableTasks) {
            obj.buildStandardViewFromTaskBasicDataMap(AvailableTasks, null, successCallback, errorCallback);
        }, errorCallback);
    };
    
    obj.buildStandardViewFromTaskBasicDataMap = function(taskInstanceBasicInfoMap, assignee, successCallback, errorCallback) {
        var finished = 0, taskBasicData, getTasksSuccessCallback, pointer, myTasks = {};
        
        getTasksSuccessCallback = function(taskData) {
            if(taskData.assignee === assignee) {
                myTasks[taskData._id] = taskData;
            }
            
            if(assignee === null) {//} && taskData.assignee === "") {
                myTasks[taskData._id] = taskData;
            }
            
            finished++;
            if(finished === taskInstanceBasicInfoMap.length) {
                if(_.isEmpty(myTasks)) {
                    errorCallback();
                } else {
                    successCallback(obj.buildStandardViewFromTaskMap(myTasks));
                }
            }
        };
        
        for (pointer in taskInstanceBasicInfoMap) {
            taskBasicData = taskInstanceBasicInfoMap[pointer];
            obj.getTask(taskBasicData._id, getTasksSuccessCallback);
        }

        if(_.isEmpty(taskInstanceBasicInfoMap)) {
            errorCallback();
        }
    };
    
    obj.buildStandardViewFromTaskMap = function(taskInstanceMap) {
        var result = {}, pointer, taskInstance, taskInstanceProcessName, taskInstanceTaskName, taskView;
        for (pointer in taskInstanceMap) {
            taskInstance = taskInstanceMap[pointer];
            taskInstanceProcessName = taskInstance.processDefinitionId.split(':')[0];
            taskInstanceTaskName = taskInstance.name;
            
            taskView = {};
            taskView._id = taskInstance._id;
            taskView.assignee = taskInstance.assignee;
            taskView.variables = taskInstance.variables;
            taskView.createTime = taskInstance.createTime;
            taskView.processInstanceId = taskInstance.processInstanceId;
            
            if (!result[taskInstanceProcessName]) {
                result[taskInstanceProcessName] = {};
            }
            
            if (!result[taskInstanceProcessName][taskInstanceTaskName]) {
                result[taskInstanceProcessName][taskInstanceTaskName] = {};
            }
            
            if (!result[taskInstanceProcessName][taskInstanceTaskName].tasks) {
                result[taskInstanceProcessName][taskInstanceTaskName].tasks = [];
            }
            result[taskInstanceProcessName][taskInstanceTaskName].tasks.push(taskView);
        }
        
        return result;
    };
*/    
    obj.getAllTaskUsingEndpoint = function(userId, successCallback, errorCallback) {
        obj.serviceCall({url: endpointUrl + "?userId=" + userId, success: function(data) {
            if(_.isEmpty(data)) {
                errorCallback();
            } else if(successCallback) {
                successCallback(data);
            }
        }, error: errorCallback} );
    };
    
    obj.getMyTaskUsingEndpoint = function(userId, successCallback, errorCallback) {
        obj.serviceCall({url: endpointUrl + "?userId=" + userId + "&viewType=assignee", success: function(data) {
            if(_.isEmpty(data)) {
                errorCallback();
            } else if(successCallback) {
                successCallback(data);
            }
        }, error: errorCallback} );
    };
    
    return obj;
});



