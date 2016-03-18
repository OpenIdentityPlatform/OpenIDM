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
 * Calculates the effective roles
 */  

/*global object */

/**
 * Module which calculates the effective roles
 */
(function () {
    var _ = require('lib/lodash');
    var relationshipHelper = require('roles/relationshipHelper');

    /**
     * This function calculates the effectiveRoles of a given user object.
     * 
     * @param object and object representing a user.
     */
    exports.calculateEffectiveRoles = function(object, rolesPropName) {
        var directRoles = null,
            objectId = object._id,
            response,
            effectiveRoles;
        
        logger.debug("Invoked effectiveRoles script on property {}", propertyName);

        logger.trace("Configured rolesPropName: {}", rolesPropName);
        
        if (object[rolesPropName] === undefined && objectId !== undefined && objectId !== null) {
            // User's roles are not present, so query for them
            //logger.trace("User's " + rolesPropName + " is not present so querying the roles", rolesPropName);
            //var path = org.forgerock.json.resource.ResourcePath.valueOf("managed/user").child(objectId).child(rolesPropName);
            //response = openidm.query(path.toString(), {"_queryId": "find-relationships-for-resource"});
            directRoles = relationshipHelper.getGrants(object, "roles", "user");
        } else {
            directRoles = object[rolesPropName];
        }

        effectiveRoles = directRoles == null 
             ? [] 
             : directRoles.map(function(role) { return { "_ref" : role._ref }; });

        // This is the location to expand to dynamic roles, 
        // project role script return values can then be added via
        // effectiveRoles = effectiveRoles.concat(dynamicRolesArray);

        // Filter roles by temporal constraints
        effectiveRoles = effectiveRoles.filter(function(roleRelationship) {
            var role = openidm.read(roleRelationship._ref);
            return processConstraintsForRole(role);
         });
        
        return effectiveRoles;
    };
    
    exports.processTemporalConstraintsForRole = processConstraintsForRole;
    
    /**
     * Processes the temporal constraints of a given role. If any temporal constraints are defined, this function will
     * return true if the current time instant (now) is contained within any of the temporal constraints, false
     * otherwise.  If no constraints are defined the function will return true.
     * 
     * @param role the role to process.
     * @returns false if temporal constraints are defined and don't include the current time instant, true otherwise.
     */
    function processConstraintsForRole(role) {
        if (role.temporalConstraints !== undefined) {
            // Loops through constraints
            for (index in role.temporalConstraints) {
                var constraint = role.temporalConstraints[index];
                // If at least one constraint passes, the role is in effect
                if (org.forgerock.openidm.util.DateUtil.getDateUtil().isNowWithinInterval(constraint.duration)) {
                    return true;
                }
            }
            return false;
        }
        // No temporal constraints
        return true;
    };
    
}());
