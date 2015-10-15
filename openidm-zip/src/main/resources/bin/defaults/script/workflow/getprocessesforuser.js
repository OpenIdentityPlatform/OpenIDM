/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 ForgeRock AS. All Rights Reserved
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

if (request.method !== "read") {
    throw {
        "code" : 403,
        "message" : "Access denied"
    };
}

(function () {
    var
    containsRole = function(object, items) {
        var i;
        for (i = 0; i < items.length; i++) {
            if (items[i] !== null && object !== null && items[i] === object) {
                return true;
            }
        }
        return false;
    },

    isProcessAvailableForUser = function(processAccessPolicies, processDefinition, userRoles) {
        var i,
            props,
            property,
            matches,
            requiresRole;

        for (i = 0; i < processAccessPolicies.length; i++) {
            props =  processAccessPolicies[i].propertiesCheck;
            property = props.property;
            matches = props.matches;
            requiresRole = props.requiresRole;

            if (processDefinition[property].match(matches)) {
                if (containsRole(requiresRole, userRoles)) {
                    return true;
                }
            }
        }
        return false;
    },

    getProcessesAvailableForUser = function(processDefinitions, userRoles) {
        var processesAvailableToUser = [],
            processAccessPolicies = openidm.read("config/process/access").workflowAccess,
            processDefinition,
            i;

        for (i = 0; i < processDefinitions.length; i++) {
            processDefinition = processDefinitions[i];
            if (isProcessAvailableForUser(processAccessPolicies, processDefinition, userRoles)) {
                processesAvailableToUser.push(processDefinition);
            }
        }
        return { "processes": processesAvailableToUser };
    },
    processDefinitions = openidm.query("workflow/processdefinition", {
        "_queryId": "query-all-ids"
    }).result;

    return getProcessesAvailableForUser(processDefinitions, context.security.authorization.roles);

} ());
