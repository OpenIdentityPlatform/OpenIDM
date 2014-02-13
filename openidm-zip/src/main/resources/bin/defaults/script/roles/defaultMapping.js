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
 * Supported operations: "insert", "replace"
 */

var map = { "result" : true };
var assignments = config.assignments;
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
						operation = "replace";
						target[name] = attribute;
					} else {
						// Process the operation
						if (operation == "insert") {
							if (target[name] == null) {
								target[name] = [];
							}
							for (var x = 0; x < value.length; x++) {
								target[name].push(value[x]);
							}
						} else if (operation == "replace") {
							target[name] = value;
						}
					}
				}
			}
		}
	}
}
map;
