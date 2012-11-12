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

//var params;
//var value;
//var fullObject;
var returnObject = {};
var failedPolicies = new Array();

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
            {
                "policyId" : "valid-date",
                "policyExec" : "validDate",
                "clientValidation": true,
                "policyRequirements": ["VALID_DATE"]
            },
            {
                "policyId" : "valid-email-address-format",
                "policyExec" : "validEmailAddressFormat",
                "clientValidation": true,
                "policyRequirements": ["VALID_EMAIL_ADDRESS_FORMAT"]
            },
            {
                "policyId" : "valid-name-format",
                "policyExec" : "validNameFormat",
                "clientValidation": true,
                "policyRequirements": ["VALID_NAME_FORMAT"]
            },
            {
                "policyId" : "valid-phone-format",
                "policyExec" : "validPhoneFormat",
                "clientValidation": true,
                "policyRequirements": ["VALID_PHONE_FORMAT"]
            },
            {   "policyId" : "at-least-X-capitals",
                "clientValidation": true,
                "policyExec" : "atLeastXCapitalLetters", 
                "policyRequirements" : ["AT_LEAST_X_CAPITAL_LETTERS"]
            },
            {   "policyId" : "at-least-X-numbers",
                "clientValidation": true,
                "policyExec" : "atLeastXNumbers", 
                "policyRequirements" : ["AT_LEAST_X_NUMBERS"]
            },
            {   "policyId" : "minimum-length",
                "clientValidation": true,
                "policyExec" : "propertyMinLength", 
                "policyRequirements" : ["MIN_LENGTH"]
            },
            {   "policyId" : "cannot-contain-others",
                "clientValidation": true,
                "policyExec" : "cannotContainOthers", 
                "policyRequirements" : ["CANNOT_CONTAIN_OTHERS"]
            },
            {
                "policyId" : "required-if-configured",
                "policyExec": "requiredIfConfigured",
                "policyRequirements" : ["REQUIRED"]
                
            },
            {   "policyId" : "re-auth-required",
                "policyExec" : "reauthRequired", 
                "policyRequirements" : ["REAUTH_REQUIRED"]
            }
        ] 
    };


function maxAttemptsTriggersLockCooldown(fullObject, value, params, property) {
    var failures = [],
        lastFailedDate = new Date(fullObject[params.dateTimeField]);
    
    if (    value > params.max &&
            (lastFailedDate.getTime() + (1000*60*params.numMinutes)) > (new Date()).getTime()
    ) { 
         failures = [{"policyRequirement": "NO_MORE_THAN_X_ATTEMPTS_WITHIN_Y_MINUTES", params: {"max":params.max,"numMinutes":params.numMinutes}}];
    }
    return failures;
}

function cannotContainOthers(fullObject, value, params, property) {
    var fieldArray = params.disallowedFields.split(","),
        fullObject_server = {};
    
    if (typeof(openidm) !== "undefined" && typeof(request) !== "undefined"  && request.id && !request.id.match('/$')) {
        fullObject_server = openidm.read(request.id)
    }
    
    if (value && typeof(value) === "string" && value.length) {
        for (var i = 0; i < fieldArray.length; i++) {
            if (typeof(fullObject[fieldArray[i]]) === "undefined" && typeof(fullObject_server[fieldArray[i]]) !== "undefined") {
                fullObject[fieldArray[i]] = fullObject_server[fieldArray[i]];
            }
            
            if (typeof(fullObject[fieldArray[i]]) === "string" && value.match(fullObject[fieldArray[i]]))
                return [{"policyRequirement": "CANNOT_CONTAIN_OTHERS", params: {"disallowedFields": fieldArray[i]}}];
        }
    }
    return [];
}

function validDate(fullObject, value, params, property) {
    if (value && value.length && isNaN(new Date(value).getTime())) {
        return [ {"policyRequirement": "VALID_DATE"}];
    }
    else
        return [];
}

function validPhoneFormat(fullObject, value, params, property) {
    var phonePattern = /^\+?([0-9\- \(\)])*$/;
    if (value && value.length && !phonePattern.test(value))
        return [ {"policyRequirement": "VALID_PHONE_FORMAT"}];
    else
        return [];
}

function validNameFormat(fullObject, value, params, property) {
    var namePattern = /^([A-Za'-\u0105\u0107\u0119\u0142\u00F3\u015B\u017C\u017A\u0104\u0106\u0118\u0141\u00D3\u015A\u017B\u0179\u00C0\u00C8\u00CC\u00D2\u00D9\u00E0\u00E8\u00EC\u00F2\u00F9\u00C1\u00C9\u00CD\u00D3\u00DA\u00DD\u00E1\u00E9\u00ED\u00F3\u00FA\u00FD\u00C2\u00CA\u00CE\u00D4\u00DB\u00E2\u00EA\u00EE\u00F4\u00FB\u00C3\u00D1\u00D5\u00E3\u00F1\u00F5\u00C4\u00CB\u00CF\u00D6\u00DC\u0178\u00E4\u00EB\u00EF\u00F6\u00FC\u0178\u00A1\u00BF\u00E7\u00C7\u0152\u0153\u00DF\u00D8\u00F8\u00C5\u00E5\u00C6\u00E6\u00DE\u00FE\u00D0\u00F0\-\s])+$/;
    if (value && value.length && !namePattern.test(value))
        return [ {"policyRequirement": "VALID_NAME_FORMAT"}];
    else
        return [];
}

function validEmailAddressFormat(fullObject, value, params, property) {
    var emailPattern = /^([A-Za-z0-9_\-\.])+\@([A-Za-z0-9_\-\.])+\.([A-Za-z]{2,4})$/; 
    if (value && value.length && !emailPattern.test(value))
        return [ {"policyRequirement": "VALID_EMAIL_ADDRESS_FORMAT"}];
    else
        return [];
}

function required(fullObject, value, params, propName) {
    if (value === undefined) {
        return [ { "policyRequirement" : "REQUIRED" } ];
    }
    return [];
}

function notEmpty(fullObject, value, params, property) { 
    if (!value || !value.length)
        return [ {"policyRequirement": "REQUIRED"}];
    else
        return []; 
}
function requiredIfConfigured(fullObject, value, params, property) {
    var currentValue = openidm.read("config/" + params.configBase),
        baseKeyArray = params.baseKey.split(".");
    
    for (var i in baseKeyArray)
        currentValue = currentValue[baseKeyArray[i]];
    
    if (currentValue && (!value || !value.length))
        return [ {"policyRequirement": "REQUIRED"}];
    else
        return [];
}

function unique(fullObject, value, params, property) {
    var queryParams = {
            "_query-id": "get-by-field-value",
            "field": property,
            "value": value
            },
        existing,requestId,requestBaseArray;
    
    if (value && value.length)
    {
        requestBaseArray = request.id.split("/");
        if (requestBaseArray.length === 3) {
            requestId = requestBaseArray.pop();
        }
        existing = openidm.query(requestBaseArray.join("/"),  queryParams);

        if (existing.result.length != 0 && (!requestId || (existing.result[0]["_id"] != requestId))) {
            return [{"policyRequirement": "UNIQUE"}];
        }
    }
    return [];
}

function propertyMinLength(fullObject, value, params, property) { 
    var minLength = params.minLength;
    if (typeof value !== "string" || value.length < minLength) {
        return [ { "policyRequirement" : "MIN_LENGTH", "params" : {"minLength":minLength} } ];
    }
    return [];
} 

function atLeastXCapitalLetters(fullObject, value, params, property) {
    var reg = /[(A-Z)]/g;
    if (typeof value !== "string" || !value.length || value.match(reg) === null || value.match(reg).length < params.numCaps) {
        return [ { "policyRequirement" : "AT_LEAST_X_CAPITAL_LETTERS", "params" : {"numCaps": params.numCaps} } ];
    }
    return [];
}

function atLeastXNumbers(fullObject, value, params, property) {
    var reg = /\d/g;
    if (typeof value !== "string" || !value.length || value.match(reg) === null || value.match(reg).length < params.numNums) {
        return [ { "policyRequirement" : "AT_LEAST_X_NUMBERS", "params" : {"numNums": params.numNums}  } ];
    }
    return [];
}

function getPolicy(policyId) {
    for (var i = 0; i < policyConfig.policies.length; i++) {
        if (policyConfig.policies[i].policyId == policyId) {
            return policyConfig.policies[i];
        }
    }
    return null;
}

function reauthRequired(fullObject, value, params, propName) {
    var req = request.parent.parent;
    if (typeof req.type !== 'undefined' && req.type == "http") {
        try {
            var actionParams = {
                "_action": "reauthenticate"
            };
            var response = openidm.action("authentication",  actionParams);
        } catch (error) {
            return [ { "policyRequirement" : "REAUTH_REQUIRED" } ];
        }
    }
    return [];
}

function getPropertyValue(requestObject, propName) {
    var propAddress = propName.split("/");
    var tmpObject = requestObject;
    for (var i = 0; i < propAddress.length; i++) {
        tmpObject = tmpObject[propAddress[i]];
        if (tmpObject === undefined || tmpObject === null) {
            return tmpObject;
        }
    }
    return tmpObject;
}

function getPropertyConfig(resource, propName) {
    var props = resource.properties;
    for (var i = 0; i < props.length; i++) {
        prop = props[i];
        if (prop.name == propName) {
            return prop;
        }
    }
    return null;
}

function getResource(resources, resourceName) {
    if (resources != null) {
        for (var i = 0; i < resources.length; i++) {
            var resource = resources[i];
            if (resourceMatches(resource.resource, resourceName)) {
                return resource;
            }
        }
    }
    return null;
}

function resourceMatches(resource1, resource2) {
    rsrc1 = resource1.split("/");
    rsrc2 = resource2.split("/");
    if (rsrc1.length == rsrc2.length) {
        for (var i = 0; i < rsrc1.length; i++) {
            if (rsrc1[i] != rsrc2[i] &&
                rsrc1[i] != "*" &&
                rsrc2[i] != "*") {
                return false;
            }
        }
        return true;
    }
    return false;
}

function getResourceWithPolicyRequirements(resource) {
    var compProps = resource.properties;
    // Loop through the properties for this resource
    for (var i = 0; i < compProps.length; i++) {
        var propPolicyReqs = new Array();
        var reqs;
        var prop = compProps[i];
        // loop through the policies of each property
        for (var j = 0; j < prop.policies.length; j++) {
            var policy = getPolicy(prop.policies[j].policyId);
            // Check if client validation is enabled, if so add source
            if ((policy.clientValidation !== undefined) && policy.clientValidation) {
                prop.policies[j].policyFunction = eval(policy.policyExec).toString();
            }
            prop.policies[j].policyRequirements = policy.policyRequirements;
            reqs = policy.policyRequirements;
            // loop through the requirements for each policy
            for (var x = 0; x < reqs.length; x++) {
                // Add the requirement if it hasen't been added yet
                if (propPolicyReqs.indexOf(reqs[x]) == -1) {
                    propPolicyReqs.push(reqs[x]);
                }
            }
        }
        // Add the requirements array to the property object
        prop.policyRequirements = propPolicyReqs;
    }
    // Return all property configs for this resource
    return resource;
}

function validate(policies, fullObject, propName, propValue, retArray) {
    var retObj = {},
        policyRequirements = new Array(),
        i,params,policy,validationFunc,failed,y;
    
    for (i = 0; i < policies.length; i++) {
        params = policies[i].params;
        policy = getPolicy(policies[i].policyId);
        if (policy == null) {
            throw "Unknown policy " + policies[i].policyId;
        }
        validationFunc = eval(policy.policyExec); 
        
        if (propValue && openidm.isEncrypted(propValue)) {
            propValue = openidm.decrypt(propValue);
        }
            
        failed = validationFunc.call(this, fullObject, propValue, params, propName);
        if (failed.length > 0) {
            for ( y = 0; y < failed.length; y++) {
                policyRequirements.push(failed[y]);
            }
        }
    }
    if (policyRequirements.length > 0) {
        retObj.property = propName;
        retObj.policyRequirements = policyRequirements;
        retArray.push(retObj);
    }
}

function mergePolicies(oldPolicies, newPolicies) {
    returnPolicies = oldPolicies.slice(0);
    var found = false;
    for (var i = 0; i < newPolicies.length; i++) {
        var newPolicy = newPolicies[i];
        for (var j = 0; j < returnPolicies.length; j++) {
            var policy = returnPolicies[j];
            if (newPolicy.name === policy.name) {
                // update old policy with new config
                returnPolicies[j] = newPolicy;
                found = true;
            }
        }
        if (!found) {
            var p = {};
            p.policyId = newPolicy.get("policyId");
            p.params = new Array();
            for (var key in newPolicy.get("params")) {
                var param = {};
                param[key] = newPolicy.get("params").get(key);
                p.params.push(param);
            }
            // add new policy
            returnPolicies.push(newPolicy);
        }
        found = false;
    }
    return returnPolicies;
}

function updateResourceConfig(resource, id) {
    var newProps = getAdditionalPolicies(id);
    var props = resource.properties;
    for (var i = 0; i < newProps.length; i++) {
        var found = false;
        var newProp = newProps[i];
        for (var j = 0; j < props.length; j++) {
            var prop = props[j];
            if (newProp.name === prop.name) {
                found = true;
                prop.policies = mergePolicies(prop.policies, newProp.policies);
            }
        }
        if (!found) {
            props.push(newProp);
        }
        found = false;
    }
    resource.properties = props;
}

function getAdditionalPolicies(id) {
    var returnArray = new Array();
    var resource;
    var object;
    var index = id.indexOf("/");
    if (index != -1) {
        resource = id.substring(0, index);
        object = id.substring(index + 1, id.length);
    } else {
        resource = id;
    }
    var configId = "config/" + resource;
    var resourceConfig = openidm.read(configId);
    if (resourceConfig != null && resourceConfig.error === undefined) {
        var objects = resourceConfig.objects;
        if (objects !== undefined && objects !== null) {
            for (var i = 0; i < objects.length; i++) {
                var obj = objects[i];
                if (obj.name == object) {
                    var props = obj.properties;
                    if (props != null) {
                        for (var j = 0; j < props.length; j++) {
                            var prop = props[j];
                            var policies = prop.policies;
                            if (policies != null) {
                                var property = {};
                                property.name = prop.name;
                                property.policies = new Array();
                                for (var k = 0; k < policies.length; k++) {
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
}

function processRequest() {
    var resource;
    var method = request.method;
    if (request.id != null && request.id !== undefined) {
        // Get the policy configuration for the specified resource
        resource = getResource(resources, request.id);
        if (resource == null ) {
            resource = {};
            resource.resource = request.id;
            resource.properties = new Array();
        }
        // Update the policy configuration with any resource specific
        updateResourceConfig(resource, request.id);
    }
    if (method == "read") {
        if (request.id == null) {
            var compArray = new Array();
            for (var i = 0; i < resources.length; i++) {
                var rsrc = resources[i];
                compArray.push(getResourceWithPolicyRequirements(rsrc));
            }
            returnObject = {};
            returnObject.resources = compArray;
        } else {
            returnObject = getResourceWithPolicyRequirements(resource);
        }
    } else if (method == "action") {
        var action = request.params._action;
        var failedPolicyRequirements = new Array();
        returnObject = {};
        if (request.id == null) {
            throw "No resource specified";
        }
        if (resource == null) {
            // There is no configured policies for this resource (nothing to verify)
            returnObject.result = true;
            returnObject.failedPolicyRequirements = failedPolicyRequirements;
        } else {
            var fullObject = request.value;
            if (fullObject === undefined) {
                fullObject = request.params.value;
            }
            // Perform the validation
            if (action == "validateObject") {
                for (var i = 0; i < resource.properties.length; i++) {
                    var propName = resource.properties[i].name;
                    var policies = resource.properties[i].policies;
                    // Validate
                    var policyRequirements = validate(policies, fullObject, propName,
                            getPropertyValue(fullObject, propName), failedPolicyRequirements);
                }
            } else if (action == "validateProperty") {
                var props = request.value;
                for (var propName in props) {
                    var prop = getPropertyConfig(resource, propName);
                    var policies = prop.policies;
                    // Validate
                    var policyRequirements = validate(policies, fullObject, propName, 
                            props[propName], failedPolicyRequirements);
                }
            } else {
                throw "Unsupported action: " + action;
            }
            // Set the result to true if no failedPolicyRequirements (failures), false otherwise
            returnObject.result = failedPolicyRequirements.length == 0;
            // Set the return failedPolicyRequirements
            returnObject.failedPolicyRequirements = failedPolicyRequirements;
        }
    } else {
        throw "Unsupported method: " + method;
    }
}

//Load additional policy scripts if configured
if (typeof additionalPolicies != 'undefined') {
  for (var i = 0; i < additionalPolicies.length; i++) {
      try {
          eval(additionalPolicies[i]);
      } catch (error) {
          java.lang.System.out.println("Error executing addtional policy script: " + error);
      }
  }
}

processRequest();

returnObject;
