/** 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 ForgeRock AS. All rights reserved.
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
 * Default mapping script for processing effectiveAssignments
 * Assignment and unassignment operations are configured as scripts.
 * An example operation configuration is:
 * {
 *    "file" : "roles/replaceTarget.js",
 *    "type" : "text/javascript"
 * }; 
 */

// The result map
var map = { "result" : true };
// The "assignments" configured for this mapping
var assignments = config.assignmentsToMap;

// Default operations
var defaultAssignmentOperation = "replaceTarget";
var defaultUnassignmentOperation = "removeFromTarget";

// A map of operation aliases to their corresponding script configurations
var operations = {
        "replaceTarget" : { 
            "file" : "roles/replaceTarget.js",
            "type" : "text/javascript"
        },
        "mergeWithTarget" : { 
            "file" : "roles/mergeWithTarget.js",
            "type" : "text/javascript"
        },
        "removeFromTarget" : { 
            "file" : "roles/removeFromTarget.js",
            "type" : "text/javascript"
        },
        "noOp" : { 
            "file" : "roles/noOp.js",
            "type" : "text/javascript"
        } 
}


function mergeValues(target, name, value) {
    if (target[name] != null && target[name] instanceof Array) {
        for (var x = 0; x < value.length; x++) {
            if (target[name].indexOf(value) == -1) {
                target[name].push(value[x]);
            }
        }
    } else if (target[name] != null && target[name] instanceof Object) {
        var obj = target[name];
        for (var key in value) {
            obj[key] = value[key];
        }
    } else {
        target[name] = value;
    }
}

// Returns the script configuration with the necessary scope fields added
function getConfig(script) {
    var config;
    var scope = {};
    // Check if the script is an alias or a full configuration
    if (script instanceof Object) {
        // Full configuration
        config = script;
    } else if (script instanceof String || typeof(script) === 'string') {
        // Alias
        if (operations.hasOwnProperty(script)) {
            config = operations[script];
        } else {
            throw "Unsupported operation alias " + script;
        }
    }
    // Add additional scope fields
    scope.sourceObject = source;
    scope.targetObject = target;
    scope.existingTargetObject = existingTarget;
    scope.linkQualifier = linkQualifier;
    for (var key in config) {
        scope[key] = config[key];
    }
    return scope;
}

function execOnScript(scriptConfig) {
    var result = openidm.action("script", "eval", getConfig(scriptConfig), {});
    for (key in target) { 
        delete target[key]; 
    }
    for (key in result) { 
        target[key] = result[key]; 
    }
}

function areAttributesEqual(attr1, attr2) {
    return JSON.stringify(attr1) === JSON.stringify(attr2);
}

// Check for any assignments that have been removed or modified
if (typeof oldSource !== 'undefined' && oldSource !== null) {
    var oldAssignments = oldSource.effectiveAssignments; // Assignments from the old source value
    var currentAssignments = source.effectiveAssignments; // Assignments from the current source value
    var unassigned = [];
    // Loop through old assignments
    for (var key in oldAssignments) {
        // Check that this key is relevant to this mapping
        if (assignments.indexOf(key) > -1) {
            var assignmentRemoved = false;
            var oldAssignment = oldAssignments[key];
            // Check if this old assignment is in the currentAssignments
            if (!currentAssignments.hasOwnProperty(key)) {
                // This assignment has been unassigned
                var onUnassignment = oldAssignment.onUnassignment;
                // Check if an onUnassignment script is configured
                if (typeof onUnassignment !== 'undefined' && onUnassignment !== null) {
                    onUnassignment.attributes = oldAssignment.attributes;
                    execOnScript(onUnassignment);
                }
                assignmentRemoved = true;
            }
            // Get the Current assignment, may be null if it has been removed
            var currentAssignment = currentAssignments[key];
            // Get the Old assignment's attributes
            var oldAttributes = oldAssignment.attributes;
            // Loop through old attributes and execute the unassignmentOperation on any that were removed or updated
            for (var i = 0; i < oldAttributes.length; i++) {
                var oldAttribute = oldAttributes[i];
                var removedOrUpdated = true;
                // If the assignment has not been removed, then we need to check if the attribute has been removed or updated.
                if (!assignmentRemoved) {
                    var currentAttributes = currentAssignment.attributes;
                    // Loop through attributes to check if they have been removed/updated
                    for (var j = 0; j < currentAttributes.length; j++) {
                        var currentAttribute = currentAttributes[j];
                        if (oldAttribute.name == currentAttribute.name) {
                            if (areAttributesEqual(oldAttribute, currentAttribute)) {
                                // attribute was found and not updated
                                removedOrUpdated = false;
                            }
                            break;
                        }
                    }
                }
                // Check if the old attribute has been removed
                if (removedOrUpdated) {
                    var unassignmentOperation = oldAttribute.unassignmentOperation;
                    if (unassignmentOperation == null) {
                        // Default to replace and use the entire value
                        unassignmentOperation = defaultUnassignmentOperation;
                    }
                    if (typeof unassignmentOperation !== 'undefined' && unassignmentOperation !== null) {
                        var config = getConfig(unassignmentOperation);
                        config.attributeName = oldAttribute.name;
                        config.attributeValue = oldAttribute.value;
                        var unassignmentResult = openidm.action("script", "eval", config, {});
                        // Update the target (working copy)
                        target[oldAttribute.name] = unassignmentResult;
                        // Update the existingTarget, in order to carry changes over to additional operations that may use the existingTarget
                        if (typeof existingTarget !== 'undefined' && existingTarget !== null) {
                            existingTarget[oldAttribute.name] = unassignmentResult;
                        }
                    }
                }
            }

        }
    }
}

// If any assignments are configured for this mapping, process any matching effectiveAssignments
if (assignments != null) {
    var effectiveAssignments = source.effectiveAssignments;
    if (effectiveAssignments != null) {
        for (var key in effectiveAssignments) {
            if (assignments.indexOf(key) != -1) {
                var assignment = effectiveAssignments[key];
                var attributes = assignment.attributes;
                var onAssignment = assignment.onAssignment;
                var linkQualifiers = assignment.linkQualifiers;

                // Only map if no linkQualifiers were specified or the current linkQualifier is in the list of linkQualifiers specified in the assignment
                if (typeof linkQualifiers === 'undefined' || linkQualifiers === null
                        || linkQualifiers.indexOf(linkQualifier) > -1) {

                    // Check if an onAssignment script is configured
                    if (typeof onAssignment !== 'undefined' && onAssignment !== null) {
                        onAssignment.attributes = attributes;
                        execOnScript(onAssignment);
                    }

                    // Used to carry information across different assignmentOperations
                    var attributesInfo = {};
                    // Loop through attributes, performing the assignmentOperations
                    for (var i = 0; i < attributes.length; i++) {
                        var attribute = attributes[i];
                        var assignmentOperation = attribute.assignmentOperation;
                        var value = attribute.value;
                        var name = attribute.name;
                        if (assignmentOperation == null) {
                            // Default to replace and use the entire value
                            assignmentOperation = defaultAssignmentOperation;
                        }
                        // Process the assignmentOperation
                        var config = getConfig(assignmentOperation);
                        config.attributeName = name;
                        config.attributeValue = value;
                        config.attributesInfo = attributesInfo;
                        // The result of this call should be an object with a field "value" containing the updated target field's value
                        var assignmentResult = openidm.action("script", "eval", config, {});
                        // Set the new target field's value
                        target[name] = assignmentResult.value;
                        // Update any passed back attributesInfo
                        if (assignmentResult.hasOwnProperty("attributesInfo")) {
                            attributesInfo = assignmentResult.attributesInfo;
                        }
                    }
                }
            }
        }
    }
}

map;
