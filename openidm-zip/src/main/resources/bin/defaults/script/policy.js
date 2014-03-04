/*! @license 
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

/*global additionalPolicies,resources */

var policyConfig = { 
    "policies" : [
        {   "policyId" : "required",
            "policyExec" : "required",
            "clientValidation": true,
            "policyRequirements" : ["REQUIRED"]
        },
        {   "policyId" : "not-empty",
            "policyExec" : "notEmpty",
            "clientValidation": true,
            "validateOnlyIfPresent": true,
            "policyRequirements" : ["REQUIRED"]
        },
        {
            "policyId" : "max-attempts-triggers-lock-cooldown",
            "policyExec" : "maxAttemptsTriggersLockCooldown",
            "policyRequirements" : ["NO_MORE_THAN_X_ATTEMPTS_WITHIN_Y_MINUTES"]
        },
        {   "policyId" : "unique",
            "policyExec" : "unique",  
            "policyRequirements" : ["UNIQUE"]
        },
        {   "policyId" : "no-internal-user-conflict",
            "policyExec" : "noInternalUserConflict",  
            "policyRequirements" : ["UNIQUE"]
        },
        {
            "policyId" : "regexpMatches",
            "policyExec" : "regexpMatches",
            "clientValidation": true,
            "policyRequirements" : ["MATCH_REGEXP"]
        },       
        {
            "policyId" : "valid-date",
            "policyExec" : "validDate",
            "clientValidation": true,
            "validateOnlyIfPresent": true,
            "policyRequirements": ["VALID_DATE"]
        },
        {
            "policyId" : "valid-email-address-format",
            "policyExec" : "validEmailAddressFormat",
            "clientValidation": true,
            "validateOnlyIfPresent": true,
            "policyRequirements": ["VALID_EMAIL_ADDRESS_FORMAT"]
        },
        {
            "policyId" : "valid-name-format",
            "policyExec" : "validNameFormat",
            "clientValidation": true,
            "validateOnlyIfPresent": true,
            "policyRequirements": ["VALID_NAME_FORMAT"]
        },
        {
            "policyId" : "valid-phone-format",
            "policyExec" : "validPhoneFormat",
            "clientValidation": true,
            "validateOnlyIfPresent": true,
            "policyRequirements": ["VALID_PHONE_FORMAT"]
        },
        {   "policyId" : "at-least-X-capitals",
            "policyExec" : "atLeastXCapitalLetters", 
            "clientValidation": true,
            "validateOnlyIfPresent": true,
            "policyRequirements" : ["AT_LEAST_X_CAPITAL_LETTERS"]
        },
        {   "policyId" : "at-least-X-numbers",
            "policyExec" : "atLeastXNumbers", 
            "clientValidation": true,
            "validateOnlyIfPresent": true,
            "policyRequirements" : ["AT_LEAST_X_NUMBERS"]
        },
        {   "policyId" : "minimum-length",
            "policyExec" : "minLength", 
            "clientValidation": true,
            "validateOnlyIfPresent": true,
            "policyRequirements" : ["MIN_LENGTH"]
        },
        {   "policyId" : "cannot-contain-others",
            "policyExec" : "cannotContainOthers", 
            "clientValidation": true,
            "validateOnlyIfPresent": true,
            "policyRequirements" : ["CANNOT_CONTAIN_OTHERS"]
        },
        {   "policyId" : "cannot-contain-characters",
            "policyExec" : "cannotContainCharacters", 
            "clientValidation": true,
            "validateOnlyIfPresent": true,
            "policyRequirements" : ["CANNOT_CONTAIN_CHARACTERS"]
        },
        {
            "policyId" : "required-if-configured",
            "policyExec": "requiredIfConfigured",
            "policyRequirements" : ["REQUIRED"]
            
        },
        {   "policyId" : "re-auth-required",
            "policyExec" : "reauthRequired", 
            "validateOnlyIfPresent": true,
            "policyRequirements" : ["REAUTH_REQUIRED"]
        }
    ] 
},
policyImpl = (function (){

    // internal-use utility function
    var checkExceptRoles = function (exceptRoles) {
            var i, j, roles, role;
            if (context.security.authorizationId !== null) {
                roles = context.security.authorizationId.roles;
                if (exceptRoles) {
                    for (i = 0; i < exceptRoles.length; i++) {
                        role = exceptRoles[i];
                        if (roles) {
                            for (j = 0; j < roles.length; j++) {
                                if (role === roles[j]) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
            return false;
        },
        policyFunctions = {};


    policyFunctions.regexpMatches = function(fullObject, value, params, property) {
        params.flags = params.flags || "";
        if (typeof(value) === "number") {
            value = value + ""; // cast to string;
        }
        if (typeof(value) !== "string" || !(new RegExp(params.regexp, params.flags)).test(value)) {
            return [ {"policyRequirement": "MATCH_REGEXP", "regexp": params.regexp, params: params, "flags": params.flags}];
        } else {
            return [];
        }
    };

    policyFunctions.required = function(fullObject, value, params, propName) {
        if (value === undefined) {
            return [ { "policyRequirement" : "REQUIRED" } ];
        }
        return [];
    };

    policyFunctions.notEmpty = function(fullObject, value, params, property) { 
        if (value !== undefined && (value === null || !value.length)) {
            return [ {"policyRequirement": "REQUIRED"}];
        }
        else {
            return [];
        }
    };

    policyFunctions.maxAttemptsTriggersLockCooldown = function(fullObject, value, params, property) {
        var failures = [],
            lastFailedDate = new Date(fullObject[params.dateTimeField]);
        
        if (value > params.max &&
            (lastFailedDate.getTime() + (1000*60*params.numMinutes)) > (new Date()).getTime()) { 
             failures = [{"policyRequirement": "NO_MORE_THAN_X_ATTEMPTS_WITHIN_Y_MINUTES", params: {"max":params.max,"numMinutes":params.numMinutes}}];
        }
        return failures;
    };

    policyFunctions.noInternalUserConflict = function(fullObject, value, params, property) {
        if (value && value.length) {
            var queryParams = {
                "_queryId": "credential-internaluser-query",
                "username": value
                },
            existing,requestId,requestBaseArray;
    
            requestBaseArray = request.resourceName.split("/");
            if (requestBaseArray.length === 3) {
                requestId = requestBaseArray.pop();
            }
            existing = openidm.query("repo/internal/user",  queryParams);
    
            if (existing.result.length !== 0 && (!requestId || (existing.result[0]._id !== requestId))) {
                return [{"policyRequirement": "UNIQUE"}];
            }
        }
        return [];
    };

    policyFunctions.unique = function(fullObject, value, params, property) {
        var queryParams = {
                "_queryId": "get-by-field-value",
                "field": property,
                "value": value
                },
            existing,requestId,requestBaseArray;
        
        if (value && value.length)
        {
            requestBaseArray = request.resourceName.split("/");
            if (requestBaseArray.length === 3) {
                requestId = requestBaseArray.pop();
            }
            existing = openidm.query(requestBaseArray.join("/"),  queryParams);
    
            if (existing.result.length !== 0 && (!requestId || (existing.result[0]._id !== requestId))) {
                return [{"policyRequirement": "UNIQUE"}];
            }
        }
        return [];
    };

    policyFunctions.validDate = function(fullObject, value, params, property) {
        if (typeof(value) !== "string" || !value.length || isNaN(new Date(value).getTime())) {
            return [ {"policyRequirement": "VALID_DATE"}];
        } else {
            return [];
        }
    };

    policyFunctions.cannotContainCharacters = function(fullObject, value, params, property) {
        var i, 
            join = function (arr, d) { // my own join needed since it appears params.forbiddenChars is not a proper JS array with the normal join method available 
                var j,list = "";
                for (j in arr) {
                    list += arr[j] + d;
                }
                return list.replace(new RegExp(d + "$"), '');
            };
        
        if (typeof(value) === "string" && value.length) {
            for (i in params.forbiddenChars) {
                if (value.indexOf(params.forbiddenChars[i]) !== -1) {
                    return [ { "policyRequirement" : "CANNOT_CONTAIN_CHARACTERS", "params" : {"forbiddenChars" : join(params.forbiddenChars, ", ")} } ];
                }
            }
        }
        return [];
    };

    policyFunctions.validPhoneFormat = function(fullObject, value, params, property) {
        var phonePattern = /^\+?([0-9\- \(\)])*$/;
    
        if (typeof(value) !== "string" || !value.length || !phonePattern.test(value)) {
            return [ {"policyRequirement": "VALID_PHONE_FORMAT"}];
        } else {
            return [];
        }
    };

    policyFunctions.validNameFormat = function(fullObject, value, params, property) {
        var namePattern = /^([A-Za'-\u0105\u0107\u0119\u0142\u00F3\u015B\u017C\u017A\u0104\u0106\u0118\u0141\u00D3\u015A\u017B\u0179\u00C0\u00C8\u00CC\u00D2\u00D9\u00E0\u00E8\u00EC\u00F2\u00F9\u00C1\u00C9\u00CD\u00D3\u00DA\u00DD\u00E1\u00E9\u00ED\u00F3\u00FA\u00FD\u00C2\u00CA\u00CE\u00D4\u00DB\u00E2\u00EA\u00EE\u00F4\u00FB\u00C3\u00D1\u00D5\u00E3\u00F1\u00F5\u00C4\u00CB\u00CF\u00D6\u00DC\u0178\u00E4\u00EB\u00EF\u00F6\u00FC\u0178\u00A1\u00BF\u00E7\u00C7\u0152\u0153\u00DF\u00D8\u00F8\u00C5\u00E5\u00C6\u00E6\u00DE\u00FE\u00D0\u00F0\-\s])+$/;
    
        if (typeof(value) !== "string" || !value.length || !namePattern.test(value)) {
            return [ {"policyRequirement": "VALID_NAME_FORMAT"}];
        } else {
            return [];
        }
    };

    policyFunctions.minLength = function(fullObject, value, params, property) {
        if (typeof(value) !== "string" || !value.length || value.length < params.minLength) {
            return [ { "policyRequirement" : "MIN_LENGTH", "params" : {"minLength":params.minLength} } ];
        } else {
            return [];
        }
    };

    policyFunctions.atLeastXCapitalLetters = function(fullObject, value, params, property) {
        var reg = /[(A-Z)]/g;
        
        if (typeof(value) !== "string" || !value.length || value.match(reg) === null || value.match(reg).length < params.numCaps) {
            return [ { "policyRequirement" : "AT_LEAST_X_CAPITAL_LETTERS", "params" : {"numCaps": params.numCaps} } ];
        } else {
            return [];
        }
    };

    policyFunctions.atLeastXNumbers = function(fullObject, value, params, property) {
        var reg = /\d/g;
        
        if (typeof(value) !== "string" || !value.length || value.match(reg) === null || value.match(reg).length < params.numNums) {
            return [ { "policyRequirement" : "AT_LEAST_X_NUMBERS", "params" : {"numNums": params.numNums}  } ];
        } else {
            return [];
        }
    };

    policyFunctions.validEmailAddressFormat = function(fullObject, value, params, property) {
        var emailPattern = /^([A-Za-z0-9_\-\.])+\@([A-Za-z0-9_\-\.])+\.([A-Za-z]{2,4})$/; 
        
        if (typeof(value) !== "string" || !value.length || !emailPattern.test(value)) {
            return [ {"policyRequirement": "VALID_EMAIL_ADDRESS_FORMAT"}];
        } else {
            return [];
        }
    };

    policyFunctions.cannotContainOthers = function(fullObject, value, params, property) {
        var fieldArray = params.disallowedFields.split(","),
            fullObject_server = {},
            i;
        
        // since this function runs on both the client and the server, we need to 
        // check for the presence of our server-side functions before using them.
        if (typeof(openidm) !== "undefined" && typeof(request) !== "undefined"  && request.resourceName && !request.resourceName.match('/*$')) {
            fullObject_server = openidm.read(request.resourceName);
            if (fullObject_server === null) {
                fullObject_server = {};
            }
        }
        
        if (value && typeof(value) === "string" && value.length) {
            for (i = 0; i < fieldArray.length; i++) {
                if (typeof(fullObject[fieldArray[i]]) === "undefined" && typeof(fullObject_server[fieldArray[i]]) !== "undefined") {
                    fullObject[fieldArray[i]] = fullObject_server[fieldArray[i]];
                }
                
                if (typeof(fullObject[fieldArray[i]]) === "string" && value.match(fullObject[fieldArray[i]])) {
                    return [{"policyRequirement": "CANNOT_CONTAIN_OTHERS", params: {"disallowedFields": fieldArray[i]}}];
                }
            }
        }
        return [];
    };

    policyFunctions.requiredIfConfigured = function(fullObject, value, params, property) {
        var currentValue = openidm.read("config/" + params.configBase),
            baseKeyArray = params.baseKey.split("."),
            i;
        
        if (checkExceptRoles(params.exceptRoles)) {
            return [];
        }
        
        for (i in baseKeyArray) {
            currentValue = currentValue[baseKeyArray[i]];
        }
        
        if (currentValue && (!value || !value.length)) {
            return [ {"policyRequirement": "REQUIRED"}];
        }
        else {
            return [];
        }
    };

    policyFunctions.reauthRequired = function(fullObject, value, params, propName) {
        if (checkExceptRoles(params.exceptRoles)) {
            return [];
        }
        
        var actionParams,response,currentObject;
        
        // Perform reauth if the context indicates that the caller is external
        // or if we have set a parameter to force reauth when handing a patch operation.
        // Important: Interpret any value of additionalParameters.external as `true`
        // so that an external caller cannot abuse this facility by passing in 'false'.
        if (context.caller.external
                || (request.additionalParameters !== null && typeof request.additionalParameters.external !== "undefined")) {

            // don't do a read if the resource ends with "/*", which indicates that this is a new record
            if (typeof request.resourceName === "string" && !request.resourceName.match('/\\*$')) { 
                currentObject = openidm.read(request.resourceName);

                // if the given resource doesn't exist, this also indicates that 
                // this is a new record (likely a client-assigned ID)
                if (currentObject === null) {
                    return [];
                }

                if (openidm.isEncrypted(currentObject[propName])) {
                    currentObject[propName] = openidm.decrypt(currentObject[propName]);
                }
                if (currentObject[propName] === fullObject[propName]) {
                    // this means the value hasn't changed, so don't complain about reauth
                    return [];
                }
                try {
                    response = openidm.action("authentication", "reauthenticate", {});
                } catch (error) {
                    return [ { "policyRequirement" : "REAUTH_REQUIRED" } ];
                }
            }
        }
        return [];
    };

    return policyFunctions;

}()),

policyProcessor = (function (policyConfig,policyImpl){
    //Internal policy code below - do not modify this module
    
    var getPolicy = function(policyId) {
        var i;
        for (i = 0; i < policyConfig.policies.length; i++) {
            if (policyConfig.policies[i].policyId === policyId) {
                return policyConfig.policies[i];
            }
        }
        return null;
    }, 
    
    getPropertyValue = function(requestObject, propName) {
        var propAddress = propName.split("/"),
            tmpObject = requestObject,
            i;
        
        for (i = 0; i < propAddress.length; i++) {
            propAddress[i] = propAddress[i].replace(/\[\*\]$/, ''); // replace a trailing array indicator, if found
            tmpObject = tmpObject[propAddress[i]];
            if (tmpObject === undefined || tmpObject === null) {
                return tmpObject;
            }
        }
        return tmpObject;
    },
    
    getPropertyConfig = function(resource, propName) {
        var props = resource.properties,prop,i;
        for (i = 0; i < props.length; i++) {
            prop = props[i];
            if (prop.name === propName) {
                return prop;
            }
        }
        return null;
    },
    
    resourceMatches = function(resource1, resource2) {
        var rsrc1 = resource1.split("/"),
            rsrc2 = resource2.split("/"),
            i;
        
        if (rsrc1.length === rsrc2.length) {
            for (i = 0; i < rsrc1.length; i++) {
                if (rsrc1[i] !== rsrc2[i] &&
                    rsrc1[i] !== "*" &&
                    rsrc2[i] !== "*") {
                    return false;
                }
            }
            return true;
        }
        return false;
    },
    
    getResource = function(resources, resourceName) {
        var i,resource;
        if (resources !== null) {
            for (i = 0; i < resources.length; i++) {
                resource = resources[i];
                if (resourceMatches(resource.resource, resourceName)) {
                    return resource;
                }
            }
        }
        return null;
    },
    
    getResourceWithPolicyRequirements = function(resource) {
        var compProps = resource.properties,
            i,j,x,reqs,
            propPolicyReqs,
            prop,policy;
            
        // Loop through the properties for this resource
        for (i = 0; i < compProps.length; i++) {
            propPolicyReqs = [];
            prop = compProps[i];
            // loop through the policies of each property
            for (j = 0; j < prop.policies.length; j++) {
                policy = getPolicy(prop.policies[j].policyId);
                // Check if client validation is enabled, if so add source
                if ((policy.clientValidation !== undefined) && policy.clientValidation) {
                    prop.policies[j].policyFunction = policyImpl[policy.policyExec].toString();
                }
                prop.policies[j].policyRequirements = policy.policyRequirements;
                reqs = policy.policyRequirements;
                // loop through the requirements for each policy
                for (x = 0; x < reqs.length; x++) {
                    // Add the requirement if it hasen't been added yet
                    if (propPolicyReqs.indexOf(reqs[x]) === -1) {
                        propPolicyReqs.push(reqs[x]);
                    }
                }
            }
            // Add the requirements array to the property object
            prop.policyRequirements = propPolicyReqs;
        }
        // Return all property configs for this resource
        return resource;
    },
    
    getAllPolicyRequirements = function (policies) {
        var reqs = [],i;
        for (i in policies) {
            reqs = reqs.concat(getPolicy(policies[i].policyId).policyRequirements);
        }
        return reqs;
    },
    
    validate = function(policies, fullObject, propName, propValue, retArray) {
        var retObj = {},
            policyRequirements = [],
            allPolicyRequirements = getAllPolicyRequirements(policies),
            propValueContainer = [],
            i,j,params,policy,validationFunc,failed,y;
        
        for (i = 0; i < policies.length; i++) {
            params = policies[i].params;
            policy = getPolicy(policies[i].policyId);
            if (policy === null) {
                throw "Unknown policy " + policies[i].policyId;
            }
            // validate this property every time unless the property has been marked as "validateOnlyIfPresent" and it isn't present
            if (!(typeof(policy.validateOnlyIfPresent) !== 'undefined' && policy.validateOnlyIfPresent && typeof(propValue) === 'undefined')) {
                validationFunc = policyImpl[policy.policyExec]; 
                
                if (propName.match(/\[\*\]$/)) { // if we are dealing with a property that is an array element
                    propValueContainer = propValue; // then use the propValue provided for the array
                } else { // if we are dealing with a regular property
                    propValueContainer = [propValue]; // then it's a single value array
                }
                
                if (propValueContainer !== undefined && propValueContainer !== null) {
                    for (j=0;j<propValueContainer.length;j++) {
                        
                        retObj = {};
                        retObj.policyRequirements = [];
                        
                        if (openidm.isEncrypted(propValueContainer[j])) {
                            propValueContainer[j] = openidm.decrypt(propValueContainer[j]);
                        }
                            
                        failed = validationFunc.call({ "failedPolicyRequirements": policyRequirements, "allPolicyRequirements": allPolicyRequirements }, fullObject, propValueContainer[j], params, propName);
                        if (failed.length > 0) {
                            retObj.property = propName.replace(/\[\*\]$/, "["+j+"]");
                            for ( y = 0; y < failed.length; y++) {
                                retObj.policyRequirements.push(failed[y]);
                            }
                            retArray.push(retObj);
                        }
                    }
                }
            }
            
        }
    },
    
    mergePolicies = function(oldPolicies, newPolicies) {
        var returnPolicies = [],
            i,j,p,key,
            found,
            newPolicy,
            policy;
        
        for (i = 0; i < oldPolicies.length; i++) {
            returnPolicies.push(oldPolicies[i]);
        }
        found = false;
        for (i = 0; i < newPolicies.length; i++) {
            newPolicy = newPolicies[i];
            for (j = 0; j < returnPolicies.length; j++) {
                policy = returnPolicies[j];
                if (newPolicy.policyId === policy.policyId) {
                    // update old policy with new config
                    returnPolicies[j] = newPolicy;
                    found = true;
                }
            }
            if (!found) {
                p = {};
                p.policyId = newPolicy.policyId;
                p.params = {};
                for (key in newPolicy.params) {
                    p.params[key] = newPolicy.params[key];
                }
                // add new policy
                returnPolicies.push(p);
            }
            found = false;
        }
        return returnPolicies;
    },
    
    getAdditionalPolicies = function(id) {
        var returnArray = [],
            resource,
            object,
            objects,
            obj,
            props,
            prop,
            policies,
            property,
            index = id.indexOf("/"),
            configId,
            resourceConfig,
            i,j,k;
        
        if (index !== -1) {
            resource = id.substring(0, index);
            object = id.substring(index + 1, id.length);
        } else {
            resource = id;
        }
        configId = "config/" + resource;
        resourceConfig = openidm.read(configId);
        if (resourceConfig !== null && resourceConfig.error === undefined) {
            objects = resourceConfig.objects;
            if (objects !== undefined && objects !== null) {
                for (i = 0; i < objects.length; i++) {
                    obj = objects[i];
                    if ((obj.name === object) || resourceMatches(object, obj.name + "/*")) {
                        props = obj.properties;
                        if (props !== undefined && props !== null) {
                            for (j = 0; j < props.length; j++) {
                                prop = props[j];
                                policies = prop.policies;
                                if (policies !== null && policies !== undefined) {
                                    property = {};
                                    property.name = prop.name;
                                    property.policies = [];
                                    for (k = 0; k < policies.length; k++) {
                                        property.policies.push(policies[k]);
                                    }
                                    returnArray.push(property);
                                }
                            }
    
                        } 
                    } 
                }
            } 
        }
        return returnArray;
    },
    
    updateResourceConfig = function(resource, id) {
        var newProps = getAdditionalPolicies(id),
            props = resource.properties,
            i,j,
            found,
            newProp,
            prop;
            
        for (i = 0; i < newProps.length; i++) {
            found = false;
            newProp = newProps[i];
            for (j = 0; j < props.length; j++) {
                prop = props[j];
                if (newProp.name === prop.name) {
                    found = true;
                    if (prop.policies.length > 0) {
                        prop.policies = mergePolicies(prop.policies, newProp.policies);
                    } else {
                        prop.policies = newProp.policies;
                    }
                }
            }
            if (!found) {
                props.push(newProp);
            }
            found = false;
        }
        resource.properties = props;
    },
    
    processRequest =  function() {
        var returnObject = {},
            resource,
            method = request.method,
            compArray,
            rsrc,
            i,
            action,
            failedPolicyRequirements,
            fullObject,
            propName,
            policies,
            policyRequirements,
            props,
            prop;
        
        if (request.resourceName !== null && request.resourceName !== undefined) {
            // Get the policy configuration for the specified resource
            resource = getResource(resources, request.resourceName);
            if (resource === null ) {
                resource = {};
                resource.resource = request.resourceName;
                resource.properties = [];
            }
            // Update the policy configuration with any resource specific
            updateResourceConfig(resource, request.resourceName);
        }
        if (method === "read") {
            if (request.resourceName === null || request.resourceName === "") {
                compArray = [];
                for (i = 0; i < resources.length; i++) {
                    rsrc = resources[i];
                    compArray.push(getResourceWithPolicyRequirements(rsrc));
                }
                returnObject = {};
                returnObject.resources = compArray;
            } else {
                returnObject = getResourceWithPolicyRequirements(resource);
            }
        } else if (method === "action") {
            action = request.action;
            failedPolicyRequirements = [];
            returnObject = {};
            if (request.resourceName === null) {
                throw "No resource specified";
            }
            if (resource === null) {
                // There is no configured policies for this resource (nothing to verify)
                returnObject.result = true;
                returnObject.failedPolicyRequirements = failedPolicyRequirements;
            } else {
                fullObject = request.content;
                // Perform the validation
                if (action === "validateObject") {
                    for (i = 0; i < resource.properties.length; i++) {
                        propName = resource.properties[i].name;
                        policies = resource.properties[i].policies;
                        // Validate
                        policyRequirements = validate(policies, fullObject, propName,
                                getPropertyValue(fullObject, propName), failedPolicyRequirements);
                    }
                } else if (action === "validateProperty") {
                    props = request.content;
                    for (propName in props) {
                        prop = getPropertyConfig(resource, propName);
                        if (prop !== null) {
                            policies = prop.policies;
                            // Validate
                            policyRequirements = validate(policies, fullObject, propName, 
                                    props[propName], failedPolicyRequirements);
                        }
                    }
                } else {
                    throw "Unsupported action: " + action;
                }
                // Set the result to true if no failedPolicyRequirements (failures), false otherwise
                returnObject.result = (failedPolicyRequirements.length === 0);
                // Set the return failedPolicyRequirements
                returnObject.failedPolicyRequirements = failedPolicyRequirements;
            }
        } else {
            throw "Unsupported method: " + method;
        }
        return returnObject;
    };

    return {processRequest:processRequest};

}(policyConfig,policyImpl)),

additionalPolicyLoader = (function (config,impl) { 
    
    var obj = {},
    addPolicy = function(policy) {
        config.policies.push(policy);
    };
    
    obj.load = function (additionalPolicies) {
        var i,j;
        //Load additional policy scripts if configured
        for (i = 0; i < additionalPolicies.length; i++) {
            eval(additionalPolicies[i]);
                
            for (j=0;j<config.policies.length;j++) {
                if (!policyImpl.hasOwnProperty(config.policies[j].policyExec) && 
                    typeof(eval(config.policies[j].policyExec)) === "function") {
                    impl[config.policies[j].policyExec] = eval(config.policies[j].policyExec);
                }
            }
        }
    };
    return obj;
    
}(policyConfig,policyImpl));


if (typeof additionalPolicies !== 'undefined') {
    additionalPolicyLoader.load(additionalPolicies);
}

policyProcessor.processRequest();
