/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

/*global addPolicy, request, openidm */

addPolicy({
    "policyId" : "is-new",
    "policyExec" : "isNew",
    "policyRequirements" : ["IS_NEW"]
});

function isNew(fullObject, value, params, property) {
    var historyLength, fieldHistory, currentObject, lastFieldValues, i;

    // Don't enforce this policy if the resource ends with "/*", which indicates that this is a create with a 
    // server-supplied id
    if (!request.resourcePath || request.resourcePath.match('/\\*$')) {
        return [];
    }

    // Read the resource
    currentObject = openidm.read(request.resourcePath);

    // Don't enforce this policy if the resource being evaluated wasn't found. Happens in the case of a create with a 
    // client-supplied id.
    if (currentObject === null) {
        return [];
    }

    // Decrypt the "fieldHistory" field.
    fieldHistory = currentObject.fieldHistory;
    
    // Don't enforce this policy if there is no history object available
    if (fieldHistory[property] === null || fieldHistory[property] === undefined) {
        return [];
    }

    // Get the current field value
    if (currentObject[property] !== null && currentObject[property] !== undefined &&
            openidm.isEncrypted(currentObject[property])) {
        currentObject[property] = openidm.decrypt(currentObject[property]);
    }

    // Don't enforce this policy if the password hasn't changed
    if (currentObject[property] === value) {
        return [];
    }

    // Get the last field values
    lastFieldValues = fieldHistory[property];

    if (params.historyLength !== undefined) {
        historyLength = params.historyLength;
    } else {
        historyLength = lastFieldValues.length;
    }

    numOfFields = lastFieldValues.length;
    // Check if the current value matches any previous values
    for(i = numOfFields - 1; i >= (numOfFields - historyLength) && i >= 0; i--) {
        if ((openidm.isHashed(lastFieldValues[i]) && openidm.matches(value, lastFieldValues[i]))
                || (lastFieldValues[i] === value)) {
            return [{"policyRequirement": "IS_NEW"}];
        }
    }
    
    return [];

}