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

/*
 Try to find running process started by the previous reconciliation.
 */

/*global workflowName,recon */

(function () {
    var queryParams = {
            "_queryId" : "filtered-query",
            "processDefinitionKey": workflowName,
            "_var-mapping":recon.actionParam.mapping,
            "_var-situation":recon.actionParam.situation,
            "_var-action":recon.actionParam.action,
            "_var-sourceId":recon.actionParam.sourceId
        },
        process = openidm.query('workflow/processinstance', queryParams);
    
    if (null !== recon.actionParam.targetId) {
        queryParams["_var-targetId"] = recon.actionParam.targetId;
    }
    
    logger.trace("asynchronous reconciliation: process.result.length => {}", process.result.length);
    
    /*
     Check if the result of the search.
     */
    if (typeof process.result === "undefined" || null === process.result || 0 === process.result.length) {
        /*
         There is no process instance found so we start one.
         */
        recon.actionParam._key = workflowName;
        logger.trace("asynchronous reconciliation: Start '{}' process", recon.actionParam._key);
        openidm.action('workflow/processinstance', {"_action" : "createProcessInstance"}, recon.actionParam);
    }
    
    /*
     Return "ASYNC" for the Reconciliation engine to finish processing the job.
     */
    return "ASYNC";
}());