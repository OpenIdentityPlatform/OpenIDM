/*! @license 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright ¬¨¬®¬¨¬© 2011-2012 ForgeRock AS. All rights reserved.
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
	        {	"policyId" : "minimum-length",
	        	"policyExec" : "propertyMinLength", 
	        	"policyRequirements" : ["MIN_LENGTH"]
	        },
	        { 	"policyId" : "at-least-one-capital",
	        	"policyExec" : "atLeastOneCapitalLetter", 
	        	"policyRequirements" : ["AT_LEAST_ONE_CAPITAL_LETTER"]
	        },
	        { 	"policyId" : "at-least-one-number",
	        	"policyExec" : "atLeastOneNumber", 
	        	"policyRequirements" : ["AT_LEAST_ONE_NUMBER"]
	        }
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

function propertyMinLength(fullObject, value, params) { 
	java.lang.System.out.println("value = " + value);
	var minLength = params.minLength;
	if (value.length < minLength) {
        return [ { "policyRequirement" : "MIN_LENGTH", "params" : [minLength] } ];
    }
	return [];
} 

function atLeastOneCapitalLetter(fullObject, value, params) {
	reg = /[(A-Z)]+/;
	if (!reg.test(value)) {
		return [ { "policyRequirement" : "AT_LEAST_ONE_CAPITAL_LETTER" } ];
	}
	return [];
}

function atLeastOneNumber(fullObject, value, params) {
	reg = /[(0-9)]+/;
    if (!reg.test(value)) {
    	return [ { "policyRequirement" : "AT_LEAST_ONE_NUMBER" } ];
	}
	return [];
}

function getPropertyValue(requestObject, propName) {
	var propAddress = propName.split("/");
	var tmpObject = requestObject;
	for (var i = 0; i < propAddress.length; i++) {
		tmpObject = tmpObject[propAddress[i]];
		if (tmpObject == null || tmpObject == 'undefined') {
			return null;
		}
	}
	return tmpObject;
}

function getPropertyConfig(component, propName) {
	var props = component.properties;
	for (var i = 0; i < props.length; i++) {
		prop = props[i];
		if (prop.name == propName) {
			return prop;
		}
	}
	return null;
}

function getComponent(components, componentName) {
	if (components != null) {
		for (var i = 0; i < components.length; i++) {
			var component = components[i];
			if (component.component == componentName) {
				return component;
			}
		}
	}
	return null;
}

function getComponentWithPolicyRequirements(components, componentName) {
	var component = getComponent(components, componentName);
	var compProps = component.properties;
	// Loop through the properties for this component
	for (var i = 0; i < compProps.length; i++) {
		var propPolicyReqs = new Array();
		var reqs;
		var prop = compProps[i];
		// loop through the policies of each property
		for (var j = 0; j < prop.policies.length; j++) {
			var policy = getPolicy(prop.policies[j].policyId);
			prop.policies[j].policyFunction = eval(policy.policyExec).toString();
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
	// Return all property configs for this component
	return component;
}

function validate(policies, propName, propValue, retArray) {
	var retObj = {};
	var policyRequirements = new Array();
	for (var i = 0; i < policies.length; i++) {
		params = policies[i].params;
		value = propValue;
		var policy = getPolicy(policies[i].policyId);
		if (policy == null) {
			throw "Unknown policy " + policies[i].policyId;
		}
		var validationFunc = eval(policy.policyExec); 
		var failed = validationFunc.call(this, fullObject, value, params);
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

function processRequest() {
	var method = request.method;
	if (method == "read") {
		if (request.id == null) {
			var compArray = new Array();
			for (var i = 0; i < components.length; i++) {
				var comp = components[i];
				compArray.push(getComponentWithPolicyRequirements(components, comp.component));
			}
			returnObject = {};
			returnObject.components = compArray;
		} else {
			returnObject = getComponentWithPolicyRequirements(components, request.id);
		}
	} else if (method == "action") {
		var action = request.params._action;
		var failedPolicyRequirements = new Array();
		var component;
		returnObject = {};
		if (request.id == null) {
			throw "No component specified";
		} else {
			// Get the policy configuration for the specified component
			component = getComponent(components, request.id);
		}
		if (component == null) {
			// There is no configured policies for this component (nothing to verify)
			returnObject.result = true;
			returnObject.failedPolicyRequirements = failedPolicyRequirements;
		} else {
			// Perform the validation
			if (action == "validateObject") {
				fullObject = request.value;
				for (var i = 0; i < component.properties.length; i++) {
					var propName = component.properties[i].name;
					var policies = component.properties[i].policies;
					// Validate
					var policyRequirements = validate(policies, propName,
							getPropertyValue(request.value, propName), failedPolicyRequirements);
				}
			} else if (action == "validateProperty") {
				var props = request.value;
				for (var propName in props) {
					var prop = getPropertyConfig(component, propName);
					var policies = prop.policies;
					// Validate
					var policyRequirements = validate(policies, propName, props[propName], failedPolicyRequirements);
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

processRequest();

returnObject












