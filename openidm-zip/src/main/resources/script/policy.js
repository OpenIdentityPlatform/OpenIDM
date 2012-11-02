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

var returnObject = {};
var failedPolicies = new Array();

var policyConfig = { 
        "policies" : [
            {   "policyId" : "minimum-length",
                "policyExec" : "propertyMinLength",
                "clientValidation" : true,
                "policyRequirements" : ["MIN_LENGTH"]
            },
            {   "policyId" : "at-least-one-capital",
                "policyExec" : "atLeastOneCapitalLetter", 
                "policyRequirements" : ["AT_LEAST_ONE_CAPITAL_LETTER"]
            },
            {  "policyId" : "at-least-one-number",
                "policyExec" : "atLeastOneNumber", 
                "policyRequirements" : ["AT_LEAST_ONE_NUMBER"]
            },
            {   "policyId" : "required",
                "policyExec" : "required", 
                "policyRequirements" : ["REQUIRED"]
            },
        ] 
    };

function getPolicy(policyId) {
    for (var i = 0; i < policyConfig.policies.length; i++) {
        if (policyConfig.policies[i].policyId == policyId) {
            return policyConfig.policies[i];
        }
    }
    return null;
}

function propertyMinLength(fullObject, value, params, propName) {
    var minLength = params.minLength;
    if (value === undefined || value === null || value.length < minLength) {
        return [ { "policyRequirement" : "MIN_LENGTH", "params" : { "minLength" : minLength } } ];
    }
    return [];
} 

function atLeastOneCapitalLetter(fullObject, value, params, propName) {
    reg = /[(A-Z)]+/;
    if (!reg.test(value)) {
        return [ { "policyRequirement" : "AT_LEAST_ONE_CAPITAL_LETTER" } ];
    }
    return [];
}

function atLeastOneNumber(fullObject, value, params, propName) {
    reg = /[(0-9)]+/;
    if (!reg.test(value)) {
        return [ { "policyRequirement" : "AT_LEAST_ONE_NUMBER" } ];
    }
    return [];
}

function required(fullObject, value, params, propName) {
    if (value === undefined) {
        return [ { "policyRequirement" : "REQUIRED" } ];
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
    var retObj = {};
    var policyRequirements = new Array();
    for (var i = 0; i < policies.length; i++) {
        params = policies[i].params;
        var policy = getPolicy(policies[i].policyId);
        if (policy == null) {
            throw "Unknown policy " + policies[i].policyId;
        }
        var validationFunc = eval(policy.policyExec); 
        var failed = validationFunc.call(this, fullObject, propValue, params, propName);
        if (failed.length > 0) {
            for ( var y = 0; y < failed.length; y++) {
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












