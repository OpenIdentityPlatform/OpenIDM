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
    var assignmentMap = {};
    for (var i = 0; i < effectiveRoles.length; i++) {
        var roleId = effectiveRoles[i];

        // Only try to retrieve role details for role ids in URL format
        if (roleId !== null && roleId._ref !== null && roleId._ref.indexOf("managed/role") != -1) {
            var roleRelationship =  openidm.read(roleId._ref, null, [ "assignments" ]);
            logger.debug("Role relationship read: {}", roleRelationship);

            if (roleRelationship != null) {
                for (var assignmentName in roleRelationship.assignments) {
                    var assignmentRelationship = roleRelationship.assignments[assignmentName];
                    var assignment = openidm.read(assignmentRelationship._ref);
                    if (assignment !== null) {
                        assignmentMap[assignmentRelationship._ref] = assignment;
                    }
                }
            } else {
                logger.debug("No role details could be read from: {}", roleId._ref);
            }
        } else {
            logger.debug("Role does not point to a resource, will not try to retrieve assignment details for {}", roleId);
        }
    }
}

// Add all assignments to the effectiveAssignments array
for (var assignment in assignmentMap) {
    effectiveAssignments.push(assignmentMap[assignment]);
    logger.trace("effectiveAssignment: {}", assignmentMap[assignment]);
}

logger.debug("Calculated effectiveAssignments: {}", effectiveAssignments);

effectiveAssignments;

