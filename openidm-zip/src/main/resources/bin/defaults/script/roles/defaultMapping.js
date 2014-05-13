/** 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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
var assignments = config.assignments;

var defaultAssignmentOperation = { 
        "file" : "roles/replaceTarget.js",
        "type" : "text/javascript"
    };

var defaultUnassignmentOperation = { 
        "file" : "roles/removeFromTarget.js",
        "type" : "text/javascript"
    };

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

function getConfig(baseConfig) {
    scope.sourceObject = source;
    scope.targetObject = target;
    scope.existingTargetObject = existingTarget;
    for (var key in baseConfig) {
        scope[key] = baseConfig[key];
    }
    return scope;
}

function execOnScript(scriptConfig) {
    var result = openidm.action("script", "eval", {}, getConfig(scriptConfig));
    for (key in target) { 
        delete target[key]; 
    }
    for (key in result) { 
        target[key] = result[key]; 
    }
}

// Check for any assignments that have been removed or modified
if (typeof oldSource !== "undefined") {
    var oldAssignments = oldSource.effectiveAssignments; // Assignments from the old source value
    var currentAssignments = source.effectiveAssignments; // Assignments from the current source value
    var unassigned = [];
    // Loop through old assignments
    for (var key in oldAssignments) {
        // Check that this key is relevant to this mapping
        if (assignments.indexOf(key) > -1) {
            var oldAssignment = oldAssignments[key];
            // Check if this old assignment is in the currentAssignments
            if (!currentAssignments.hasOwnProperty(key)) {
                // This assignment has been unassigned
                var onUnassignment = oldAssignment.onUnassignment;
                // Check if an onUnassignment script is configured
                if (onUnassignment != "undefined" && onUnassignment != null) {
                    execOnScript(onUnassignment);
                }
            } else {
                // This assignment is still assigned, check for any removed attributes
                var currentAssignment = currentAssignments[key];
                var oldAttributes = oldAssignment.attributes;
                var currentAttributes = currentAssignment.attributes;
                // Loop through old attributes
                for (var i = 0; i < oldAttributes.length; i++) {
                    var oldAttribute = oldAttributes[i];
                    var removed = true;
                    for (var j = 0; j < currentAttributes.length; j++) {
                        var currentAttribute = currentAttributes[j];
                        if (oldAttribute.name == currentAttribute.name) {
                            removed = false;
                            break;
                        }
                    }
                    // Check if the old attribute has been removed
                    if (removed) {
                        var unassignmentOperation = oldAttribute.unassignmentOperation;
                        if (unassignmentOperation == null) {
                            // Default to replace and use the entire value
                            unassignmentOperation = defaultUnassignmentOperation;
                        }
                        if (unassignmentOperation != "undefined" && unassignmentOperation != null) {
                            var config = getConfig(unassignmentOperation);
                            config.attributeName = oldAttribute.name;
                            config.attributeValue = oldAttribute.value;
                            var unassignmentResult = openidm.action("script", "eval", {}, config);
                            target[oldAttribute.name] = unassignmentResult;
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
                // Check if an onAssignment script is configured
                if (onAssignment != "undefined" && onAssignment != null) {
                    execOnScript(onAssignment);
                }

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
                    var assignmentResult = openidm.action("script", "eval", {}, config);
                    target[name] = assignmentResult;
                }
            }
        }
    }
}

map;
