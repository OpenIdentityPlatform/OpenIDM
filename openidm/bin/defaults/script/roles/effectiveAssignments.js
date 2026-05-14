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
 * Calculates the effective assignments, based on the effective roles.
 * 
 * In the case of a reconciliation run, the assignments and roles will be pre-loaded into the ReconContext. This script
 * will first attempt to find the roles and assignments in the ReconContext and if they are not found will issue a read.
 */

/*global object */

var effectiveAssignments = [],
    effectiveRoles = object[effectiveRolesPropName],
    reconContext = context.recon,
    assignments = typeof(reconContext) === "undefined" ? null : reconContext.assignments,
    roles = typeof(reconContext) === "undefined" ? null : reconContext.roles;

logger.debug("Invoked effectiveAssignments script on property {}", propertyName);

// Allow for configuration in virtual attribute config, but default
if (effectiveRolesPropName === undefined) {
    var effectiveRolesPropName = "effectiveRoles";
}

logger.trace("Configured effectiveRolesPropName: {}", effectiveRolesPropName);

/**
 * Returns a managed role object representing the supplied role id.  
 * 
 * If the ReconContext is present it will use the stored values for roles, otherwise it will issue a read request.
 * 
 * @param roleId the id of the managed role
 * @returns a managed role object
 */
function getRole(roleId) {
    // First check if roles were loaded in the context (in case of recon)
    if (roles != null) {
        for (var index in roles) {
            var role = roles[index];
            if (roleId == "managed/role/" + role._id) {
                return role;
            }
        }
    }
    return openidm.read(roleId, null, [ "assignments" ]);
}

/**
 * Returns a managed assignment object representing the supplied assignment id.  
 * 
 * If the ReconContext is present it will use the stored values for assignments, otherwise it will issue a read request.
 * 
 * @param assignmentId the id of the managed assignment
 * @returns a managed assignment object
 */
function getAssignment(assignmentId) {
    // First check if assignments were loaded in the context (in case of recon)
    if (assignments != null) {
        for (var index in assignments) {
            var assignment = assignments[index];
            if (assignmentId == "managed/assignment/" + assignment._id) {
                return assignment;
            }
        }
    }
    return openidm.read(assignmentId, null);
}

if (effectiveRoles != null)  {
    var assignmentMap = {};
    for (var i = 0; i < effectiveRoles.length; i++) {
        var roleId = effectiveRoles[i];

        // Only try to retrieve role details for role ids in URL format
        if (roleId !== null && roleId._ref !== null && roleId._ref.indexOf("managed/role") != -1) {
            var roleRelationship =  getRole(roleId._ref);
            logger.debug("Role relationship read: {}", roleRelationship);

            if (roleRelationship != null) {
                for (var assignmentName in roleRelationship.assignments) {
                    var assignmentRelationship = roleRelationship.assignments[assignmentName];
                    var assignment = getAssignment(assignmentRelationship._ref);
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

