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
 */
(function () {
    var relationshipHelper = require('roles/relationshipHelper');
    var effectiveRoles = require('roles/effectiveRoles');

    /**
     *
     * @returns {{expired: Array}} A map with a list keyed with 'expired', corresponding to roles with expired temporal
     * constraints.
     * @see areConstraintsExpired for details on the semantics for populating the list of roles with expired temporal
     * constraints.
     */
    exports.getRolesWithExpiredTemporalConstraints = function() {
        var pageSize = 500,
            queryResult,
            pagedResultsCookie = "",
            rolesWithExpiredConstraints = [];

        do {
            queryResult = getRolesWithTemporalConstraints(pageSize, pagedResultsCookie);
            pagedResultsCookie = queryResult._pagedResultsCookie;
            rolesWithExpiredConstraints = rolesWithExpiredConstraints
                .concat(queryResult.result.filter(areRoleConstraintsExpired));
        } while (pagedResultsCookie);
        return { 'expired' : rolesWithExpiredConstraints };
    };

    /**
     * Returns a set of roles with temporalConstraints
     * @param pageSize the size of the result set
     * @param pagedResultsCookie used to track paging through a large result-set
     * @returns {*|{type, title, properties}|{queryFilter, fields, sortKeys}} the queryResults
     */
    function getRolesWithTemporalConstraints(pageSize, pagedResultsCookie)  {
        return openidm.query('managed/role', {'_queryFilter' : '/temporalConstraints pr', '_pageSize' : pageSize,
            '_pagedResultsCookie' : pagedResultsCookie})
    }

    /**
     *
     * @param managedObjectType the type of managed object - user or role.
     * @param grantFieldName the field name identifying the grants for the type of managed object in question - typically
     * roles for user types and members for role types.
     * @param managedObjectId the id of the managed object
     * @returns {{expired: *}} A map with a list, keyed by the expired keyword. The lists will
     * contain the expired grants.
     * @see areConstraintsExpired for details on the semantics for populating the list.
     */
    exports.getGrantsWithExpiredTemporalConstraints = function(managedObjectType, grantFieldName, managedObjectId) {
        var allGrants = relationshipHelper.getGrants({ _id : managedObjectId}, grantFieldName, managedObjectType);
        return {
            'expired' : allGrants.filter(areGrantConstraintsExpired)
        }
    }

    function isNil(object) {
        return object === undefined || object === null;
    }

    function areGrantConstraintsExpired(grant) {
        if (!isNil(grant._refProperties)) {
            return areConstraintsExpired(grant._refProperties);
        }
        return false;
    }

    function areRoleConstraintsExpired(role) {
        if (!isNil(role.temporalConstraints)) {
            return areConstraintsExpired(role);
        }
        return false;
    }

    exports.areConstraintsExpired = areConstraintsExpired;

    /**
     * Returns true if:                                                                                                                               ß
     * 1. none of the temporal constraints for the role/grant are currently in effect AND
     * 2. no temporal constraint is pending, and at least one temporal constraint is expired.
     * @param object the grant or role
     * @returns {boolean} true or false, depending on the rules defined above.
     */
     function areConstraintsExpired(object) {
        var constraintExpired = false;
            dateUtil = org.forgerock.openidm.util.DateUtil.getDateUtil();

        for (index in object.temporalConstraints) {
            var constraint = object.temporalConstraints[index];
            // If one constraint passes, the role is in effect, and not expired. If one constraint is in the future, it is
            // also not expired.
            if (dateUtil.isNowWithinInterval(constraint.duration) || dateUtil.isIntervalInFuture(constraint.duration)) {
                return false;
            }
            constraintExpired = constraintExpired || dateUtil.isIntervalInPast(constraint.duration);
        }
        return constraintExpired;
    };
}());