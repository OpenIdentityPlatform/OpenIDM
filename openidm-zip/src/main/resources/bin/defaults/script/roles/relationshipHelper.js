/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */


/**
 * A module which defines some utility functions used to evaluate conditional roles.
 */
(function () {
    var _ = require('lib/lodash');

    /**
     * Returns the grants of either the managed role or user
     * @param managedObject the user or role
     * @param grantFieldName the name of the collection referencing grants
     * @param managedObjectType the type of managedObject - either 'user' or 'role'
     * @returns {*} the members of the managedObject members array specified in the managedObject, or, if this array is not present,
     * the result of the relationship query against this particular managedObject.
     */
    exports.getGrants = function(managedObject, grantFieldName, managedObjectType) {
        var path,
            managedObjectId = managedObject._id;
        if (managedObject[grantFieldName] === undefined && managedObjectId !== undefined && managedObjectId !== null) {
            logger.trace("Managed Objects's membership collection {} is not present so querying the relationship", grantFieldName);
            if ('user' === managedObjectType) {
                path = getUserRolesResourcePathString(managedObjectId, grantFieldName);
            } else if ('role' === managedObjectType) {
                path = getRoleMembersResourcePathString(managedObjectId, grantFieldName);
            } else {
                throw {'error' : 'Managed Object type is neither user nor role, but ' + managedObjectType };
            }
            response = openidm.query(path, {"_queryId": "find-relationships-for-resource"});
            return response.result;
        } else {
            return managedObject[grantFieldName] || [];
        }
    }

    /**
     * Returns the user/role grants of a managed object segmented by those conditionally assigned, and those directly assigned.
     * param manadedObject the role or user instance
     * param grantFieldName the name of the relationship field for the specified managed object that contains the grants.
     * param managedObjectType the type of managed object - either 'user' or 'role'
     * returns {{conditionalGrants: *, directGrants: *}} user/role grants, segmented by those conditionally assigned, and those directly assigned.
     */
    exports.getConditionalAndDirectGrants = function(managedObject, grantFieldName, managedObjectType) {
        var objectGrants = this.getGrants(managedObject, grantFieldName, managedObjectType);

        return {
            'conditionalGrants' : _.filter(objectGrants, isGrantConditional),
            'directGrants' : _.reject(objectGrants, isGrantConditional)
        };
    }

    function getUserRolesResourcePathString(userId, rolesPropName) {
        return org.forgerock.json.resource.ResourcePath.valueOf("managed/user").child(userId).child(rolesPropName).toString();
    }

    function getRoleMembersResourcePathString(roleId, membersPropName) {
        return org.forgerock.json.resource.ResourcePath.valueOf("managed/role").child(roleId).child(membersPropName).toString();
    }

    /**
     *
     * @returns {boolean|*|Object|n} the list of existing conditional roles
     */
    exports.getConditionalRoles = function() {
        return openidm.query('managed/role', {_queryFilter: '/condition pr'}).result;
    }

    /**
     * Determines whether a user or role grant is conditional
     * @param grant the role or user role grant
     * @returns {boolean} true if the grant is conditional, false otherwise
     */
    function isGrantConditional(grant) {
        var grantProperties = grant['_refProperties'];
        return grantProperties !== undefined && grantProperties !== null && grantProperties._grantType === 'conditional';
    }

    /**
     *
     * @param role the role
     * @returns {boolean} returns true if the role is conditional - false otherwise.
     */
    exports.isRoleConditional = function(role) {
        return role.condition !== undefined;
    }
}());