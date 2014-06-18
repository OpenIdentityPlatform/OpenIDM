/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All Rights Reserved
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

/*global workflowName,recon */


/* 
    triggerWorkflowFromSync.js - A script designed to start asynchronous actions via workflows. Asynchronous actions
    differ from the more typical synchronous actions defined in sync.json mappings in that they may involve 
    waiting for some external trigger (such as a user interaction) before they continue performing their 
    action.

    This script is designed to create workflows based on the input provided from a synchronization situation
    action handler. For example (from sample9/conf/sync.json):

        {
            "situation" : "ABSENT",
            "action" : {
                "workflowName" : "managedUserApproval",
                "type" : "text/javascript",
                "file" : "workflow/triggerWorkflowFromSync.js"
            }
        },    

    There are two variables that this script depends upon: "workflowName" (provided as part of the action 
    configuration, seen above) and "recon.actionParam" (available to all situation handler scripts). The content
    of recon.actionParam looks like this:

        {
            "_action": "performAction",
            "reconId": "7003daf9-b4b3-4d4c-bbb1-24c18b7208b6",
            "action": "CREATE",
            "mapping": "systemXmlfileAccounts_managedUser",
            "situation": "ABSENT",
            "sourceId": "bjensen"
        }

    There may also be a "targetId" defined, if appropriate for the situation.

    The expectation is that the workflow process being initiated by this script is capable of handling the above
    data structure, and will eventually do something meaningful with it. A very plausable example of this would
    be if the workflow took these parameters and used the openidm.action("sync", 'performAction' ...) method to 
    eventually perform some operation based on them. Here is a bit of groovy code which does that:

        (taken from sample9/workflow/managedUserApproval.bpmn20.xml)

        params = new java.util.HashMap();
        params.put('reconId', reconId)
        params.put('mapping', mapping)
        params.put('situation', situation)
        params.put('action', action)
        params.put('sourceId', sourceId)
        targetId = execution.getVariables().get("targetId")
        if (targetId!=null){
            params.put('targetId', targetId)
        }
       openidm.action('sync', 'performAction', {}, params)

*/

(function () {
    var queryParams = {
            "_queryId" : "filtered-query",
            "processDefinitionKey": workflowName,
            "var-mapping":recon.actionParam.mapping,
            "var-situation":recon.actionParam.situation,
            "var-action":recon.actionParam.action
        },
        process,
        businessKey = "sourceId: " + recon.actionParam.sourceId + ", targetId: " + recon.actionParam.targetId + ", reconId: " + recon.actionParam.reconId;
    
    if (typeof recon.actionParam.sourceId !== "undefined" && null !== recon.actionParam.sourceId) {
        queryParams["var-sourceId"] = recon.actionParam.sourceId;
    }
    if (typeof recon.actionParam.targetId !== "undefined" && null !== recon.actionParam.targetId) {
        queryParams["var-targetId"] = recon.actionParam.targetId;
    }


    /*
     Try to find running process started by the previous reconciliation.
     */
    process = openidm.query('workflow/processinstance', queryParams);
    logger.trace("asynchronous reconciliation: process.result.length => {}", process.result.length);
    
    /*
      Only create a new workflow if there are no matching workflows found
     */
    if (typeof process.result === "undefined" || null === process.result || 0 === process.result.length) {
        recon.actionParam._key = workflowName;
        recon.actionParam._businessKey = businessKey;
        logger.trace("asynchronous reconciliation: Start '{}' process", recon.actionParam._key);

        // pass in all of the details of the recon situation that we are currently processing, 
        // delegating the responsibility for handling this data to the workflow.
        openidm.create('workflow/processinstance', null, recon.actionParam);
    }
    
    /*
     Return "ASYNC" for the Reconciliation engine to finish processing the job.
     */
    return "ASYNC";
}());
