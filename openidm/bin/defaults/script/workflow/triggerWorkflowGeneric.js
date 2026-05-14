/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All Rights Reserved
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

/*global workflowName, openidm, params */

/* 
 triggerWorkflowGeneric.js - A script to create workflow from event hooks given a set of parameters.
 The parameters is a map of key value pairs. The values are expected to be script snip-its which
 are evaluated and their values reassigned to the key.


 Example "params" for Contractor Onboarding Process workflow:
 {
     "mail": "object.mail",
     "startDate": "object.startDate",
     "sn": "object.sn",
     "department": "object.dept",
     "provisionToXML": "false",
     "_formGenerationTemplate": "object.formGenTemp"
     "endDate": "object.endDate",
     "givenName": "object.name",
     "password": "object.pw",
     "description": "object.desc",
     "telephoneNumber": "object.phone",
     "userName": "object.username",
     "jobTitle": "object.title"
 }

 Example "workflowName": "contractorOnboarding:1:3"

 In addition to the well-known "params" and "workflowName" variables, there can be additional variables exposed to this script depending on the context in which it is invoked.
 For example, things like "object", "source", "target", etc...

 Those variables are only going to be used indirectly, by evaluating the param keys, but they are still important to be aware of in order to make sense of this script.
 */

(function () {
    for (var script in params) {
        try {
            params[script] = eval(params[script]);
        } catch (e) {

        }
    }

    params._key = workflowName;

    openidm.create('workflow/processinstance', null, params);

    return;
}());
