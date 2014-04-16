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
 * Supported operations: "replaceTarget", "mergeWithTarget"
 */

var map = { "result" : true };
var assignments = config.assignments;

function mergeValues(targetValue, value) {
    if (targetValue != null && targetValue instanceof Array) {
        for (var x = 0; x < value.length; x++) {
            if (targetValue.indexOf(value) == -1) {
                targetValue.push(value[x]);
            }
        }
    } else if (targetValue != null && targetValue instanceof Object) {
        for (var key in value) {
            targetValue[key] = value[key];
        }
    } else {
        targetValue = value;
    }
}

if (assignments != null) {
    var effectiveAssignments = source.effectiveAssignments;
    if (effectiveAssignments != null) {
        for (var key in effectiveAssignments) {
            if (assignments.indexOf(key) != -1) {
                var assignment = effectiveAssignments[key];
                var attributes = assignment.attributes;
                //for (var attributeName in attributes) {
                for (var i = 0; i<attributes.length; i++) {
                    //var attribute = attributes[attributeName];
                    var attribute = attributes[i];
                    var operation = attribute.operation;
                    var value = attribute.value;
                    var name = attribute.name;
                    if (operation == null) {
                        // Default to replace and use the entire value
                        operation = "replaceTarget";
                    }
                    // Process the operation
                    if (operation == "replaceTarget") {
                        mergeValues(target[name], value);
                    } else if (operation == "mergeWithTarget") {
                        if (existingTarget[name] !== null) {
                            mergeValues(target[name], existingTarget[name]);
                        }
                        mergeValues(target[name], value);
                    } else {
                        console.log("WARNING: Unsupported assignment operation: " + operation);
                    }
                }
            }
        }
    }
}


map;
