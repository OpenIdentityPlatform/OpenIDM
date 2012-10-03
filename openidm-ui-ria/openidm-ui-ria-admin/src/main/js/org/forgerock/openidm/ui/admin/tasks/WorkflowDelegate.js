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
define("org/forgerock/openidm/ui/admin/tasks/WorkflowDelegate", [
    "org/forgerock/commons/ui/common/util/Constants", 
    "org/forgerock/commons/ui/common/main/ServiceInvoker"
], function(constants, serviceInvoker) {
    
    var obj = {}, taskManagementUrl, processManagementUrl;
    
    taskManagementUrl       =   "/openidm/workflow/task";
    processManagementUrl    =   "/openidm/workflow/processinstance";
    

    obj.startProccess = function(proccessNameKey, params, successCallback, errorCallback) {
        console.debug("start proccess");
        params.key = proccessNameKey;
        this.serviceCall({url: processManagementUrl + "/?_action=createProcessInstance", type: "POST", success: successCallback, error: errorCallback, data: JSON.stringify(params)});
    };

    obj.deleteProcess = function(id, successCallback, errorCallback) {
        console.debug("delete process");
        this.serviceCall({url: processManagementUrl + "/" + id, type: "DELETE", success: successCallback, error: errorCallback});
    };
    
    obj.getTask = function(id, successCallback, errorCallback) {
        console.debug("get task");
        this.serviceCall({url: taskManagementUrl + "/" + id, type: "GET", success: successCallback, error: errorCallback});
    };
    
    obj.updateTask = function(id, params, successCallback, errorCallback) {
        console.debug("update task");
        var callParams =  {url: taskManagementUrl + "/" + id, type: "PUT", success: successCallback, error: errorCallback, data: JSON.stringify(params)};
        callParams.headers = [];
        callParams.headers["If-Match"] = '"*"';
        this.serviceCall(callParams);
    };
    
    obj.completeTask = function(id, params, successCallback, errorCallback) {
        console.debug("complete task");
        this.serviceCall({url: taskManagementUrl + "/" + id + "?_action=complete", type: "POST", success: successCallback, error: errorCallback, data: JSON.stringify(params)});
    };
    
    obj.getAllTasks = function(successCallback, errorCallback) {
        console.info("getting all tasks");

        obj.serviceCall({url: taskManagementUrl + "?_query-id=query-all-ids", success: function(data) {
            if(successCallback) {
                successCallback(data.result);
            }
        }, error: errorCallback} );
    };
    
    obj.getAllProcessInstances = function(successCallback, errorCallback) {
        console.info("getting all process instances");

        obj.serviceCall({url: processManagementUrl + "?_query-id=query-all-ids", success: function(data) {
            if(successCallback) {
                successCallback(data.result);
            }
        }, error: errorCallback} );
    };
    
    obj.getAllTasksForProccess = function(proccessNameKey, successCallback, errorCallback) {
        console.info("getting all unassigned tasks");
        obj.serviceCall({url: taskManagementUrl + "?_query-id=filtered-query&" + $.param({key: proccessNameKey}), success: function(data) {
            if(successCallback) {
                successCallback(data.result);
            }
        }, error: errorCallback} );
    };
    
    obj.getTasksAssignedToUser = function(userName, successCallback, errorCallback) {
        console.info("getting all tasks assigned to user " + userName);
    
        obj.serviceCall({url: taskManagementUrl + "?_query-id=filtered-query&" + $.param({assignee: userName}), success: function(data) {
            if(successCallback) {
                successCallback(data.result);
            }
        }, error: errorCallback} );
    };
    
    obj.serviceCall = function(callParams) {
        console.log(callParams.url);
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
    
    obj.getTasksAvalibleToUser = function(userName, successCallback, errorCallback) {
        obj.getAllTasks(successCallback, errorCallback);
    };
    
    obj.getAllTasksViewForUser = function(userName, successCallback, errorCallback) {
        obj.getTasksAssignedToUser(userName, function(avalibleTasks) {
            obj.buildStandardViewFromTaskBasicDataMap(avalibleTasks, userName, successCallback, errorCallback);
        }, errorCallback);
    };
    
    obj.getAllAvalibleTasksViewForUser = function(userName, successCallback, errorCallback) {
        obj.getTasksAvalibleToUser(userName, function(avalibleTasks) {
            obj.buildStandardViewFromTaskBasicDataMap(avalibleTasks, null, successCallback, errorCallback);
        }, errorCallback);
    };
    
    obj.buildStandardViewFromTaskBasicDataMap = function(taskInstanceBasicInfoMap, assignee,successCallback, errorCallback) {
        var finished = 0, taskBasicData, getTasksSuccessCallback, pointer, myTasks = {};
        
        getTasksSuccessCallback = function(taskData) {
            taskData.params = {userApplicationLnkId: taskData.description};
            if(taskData.assignee === assignee) {
                myTasks[taskData._id] = taskData;
            }
            
            if(assignee === null && taskData.assignee === "") {
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
            
            taskView = taskInstance.params;
            taskView._id = taskInstance._id;
            
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
    
    return obj;
});



