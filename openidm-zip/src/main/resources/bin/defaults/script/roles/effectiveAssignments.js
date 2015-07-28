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

/** 
 * Calculates the effective assignments, based on the effective roles
 */

/*global object */

logger.debug("Invoked effectiveAssignments script on property {}", propertyName);

// Allow for configuration in virtual attribute config, but default
if (effectiveRolesPropName === undefined) {
    var effectiveRolesPropName = "effectiveRoles";
}
logger.trace("Configured effectiveRolesPropName: {}", effectiveRolesPropName);

var effectiveAssignments = [];
var effectiveRoles = object[effectiveRolesPropName];

if (effectiveRoles != null)  {
    for (var i = 0; i < effectiveRoles.length; i++) {
        var roleId = effectiveRoles[i];

        // Only try to retrieve role details for role ids in URL format
        if (roleId != null && roleId.indexOf("/") != -1) {
            var roleInfo =  openidm.read(roleId);
            logger.debug("Role info read: {}", roleInfo);

            if (roleInfo != null) {
                for (var assignmentName in roleInfo.assignments) {
                    var assignment = roleInfo.assignments[assignmentName];
                    var onAssignment = assignment.onAssignment;
                    var onUnassignment = assignment.onUnassignment;
                    var linkQualifers = assignment.linkQualifiers;
                    var effectiveAssignment = {
                        "name" : assignmentName,
                        "attributes" : assignment.attributes
                    };
                    effectiveAssignment["assignedThrough"] = roleId;
                    if (typeof onAssignment !== "undefined" && onAssignment !== null) {
                        effectiveAssignment["onAssignment"] = onAssignment;
                    }
                    if (typeof onUnassignment !== "undefined" && onUnassignment !== null) {
                        effectiveAssignment["onUnassignment"] = onUnassignment;
                    }
                    if (typeof linkQualifers !== "undefined" && linkQualifers !== null) {
                        effectiveAssignment["linkQualifiers"] = linkQualifers;
                    }
                    logger.trace("assignmentName: {} value : {}", assignmentName, assignment);
                    effectiveAssignments.push(effectiveAssignment);
                    logger.trace("effectiveAssignment: {}", effectiveAssignment);
                }
            } else {
                logger.debug("No role details could be read from: {}", roleId);
            }
        } else {
            logger.debug("Role does not point to a resource, will not try to retrieve assignment details for {}", roleId);
        }
    }
}
logger.debug("Calculated effectiveAssignments: {}", effectiveAssignments);

effectiveAssignments;

