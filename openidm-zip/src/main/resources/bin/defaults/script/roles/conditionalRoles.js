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
 * Module which updates conditional role grants for created/updated users/roles. Note that existing grants should
 * be preserved across any changes - change logic should only add-to/remove existing grants. And this logic works
 * by modifying the members array of the role, or the roles array of the user - it does not invoke openidm to mutate roles.
 */
(function () {
    var _ = require('lib/lodash');
    var relationshipHelper = require('roles/relationshipHelper');

    /**
     * This function will be called for the onCreate and onUpdate triggers for managed users. It must determine which
     * conditional role grants will be preserved/applied-to/removed-from the given user.
     * @param user the newly-created, or updated, user
     * @param rolesPropName the name of the array in the user referencing the user's roles
     */
    exports.updateConditionalGrantsForUser = function(user, rolesPropName) {
        var userRoleGrants,
            existingConditionalRoles;

        userRoleGrants = relationshipHelper.getConditionalAndDirectGrants(user, rolesPropName, 'user');
        existingConditionalRoles = relationshipHelper.getConditionalRoles();
        this.evaluateConditionalRoles(user, rolesPropName, existingConditionalRoles, userRoleGrants);
    };

    /**
     * This function will be called for the onCreate and onUpdate triggers for managed users. It will mutate the current
     * set of user role grants in the following manner:
     * 1. add existing direct grants
     * 2. iterate through the existingConditionalRoles, and add the grant if this grant is not already enjoyed by the user
     * and if the condition is satisfied.
     * 3. filter the existing conditional grants by removing those which do not satisfy the condition.
     * @param user the newly-created, or updated, user
     * @param rolesPropName the name of the array referencing the roles in the user object
     * @param existingConditionalRoles the current set of conditional roles in the system
     * @param userRoleGrants the current conditional and direct role grants for the user.
     */
    exports.evaluateConditionalRoles = function(user, rolesPropName, existingConditionalRoles, userRoleGrants) {
        var conditionalGrants = userRoleGrants.conditionalGrants,
            directGrants = userRoleGrants.directGrants;
        user[rolesPropName] =
            directGrants.concat(
                _(existingConditionalRoles)
                    .filter(function (role) {
                        // find those conditional roles which aren't yet granted to the user in any way
                        var roleInGrants =
                            function (grants) {
                                return _.find(grants, function (grant) {return 'managed/role/' + role._id === grant._ref;}) !== undefined;
                            };
                        //only process if the conditional role is not directly or conditionally granted
                        if (!roleInGrants(directGrants)) {
                            return !roleInGrants(conditionalGrants) &&
                                org.forgerock.openidm.condition.Conditions.newCondition(role.condition).evaluate(user, null);
                        }
                        return false;
                    })
                    .map(function(role) {
                        // a new relationship representation for those newly granted roles
                        return {
                            '_ref': 'managed/role/' + role._id, '_refProperties': {'_grantType' : 'conditional'}
                        };
                     })
                     .value()
            ).concat(
                 _.filter(conditionalGrants, function(grant) {
                     // retain the existing conditional grants which are still valid
                     var roleCorrespondingToGrant = _.find(existingConditionalRoles, function (role) { return 'managed/role/' + role._id === grant._ref; });
                     if (roleCorrespondingToGrant === undefined) {
                         logger.warn("In evaluateConditionalRoles, an existing user grant could not be matched to an " +
                             "existing conditional role. The grant in question: {}", grant);
                         return false;
                     } else {
                         return org.forgerock.openidm.condition.Conditions.newCondition(roleCorrespondingToGrant.condition).evaluate(user, null);
                     }
                 })
            );
    }

    /**
     * This function will be called on onUpdate for roles. The logic below will update any conditional role
     * grant changes resulting from the conditional role change.
     * @param oldRole the previous role
     * @param newRole the updated role
     */
    exports.roleUpdate = function(oldRole, newRole) {
        /*
         Only iterate through all of the users if we are dealing with a conditional role, and if the
         role condition has changed. And if the role's condition has been removed, the new role grantees will be only
         those members who previously enjoyed a direct grant.
         */
        if (relationshipHelper.isRoleConditional(newRole) && hasRoleConditionChanged(oldRole, newRole)) {
            newRole.members = processUpdatedConditionalRole(oldRole, newRole);
        } else if (relationshipHelper.isRoleConditional(oldRole) && !relationshipHelper.isRoleConditional(newRole)) {
            newRole.members = processRoleConditionRemoval(oldRole);
        }
    }

    /**
     * Invoked when a role is created. This function will update the members array with user references which enjoy the
     * role.
     * @param newRole the newly-created role
     */
    exports.roleCreate = function(newRole) {
        if (relationshipHelper.isRoleConditional(newRole)) {
            newRole.members = processCreatedConditionalRole(newRole);
        }
    }

    /**
     * This function will be called when a role's condition is removed. It will return only those
     * @param newRole
     */
    function processRoleConditionRemoval(oldRole) {
        return relationshipHelper.getConditionalAndDirectGrants(oldRole, 'members', 'role').directGrants;
    }

    /**
     * This method will constitute the set of users which satisfy the role condition.
     * @param role the newly-created conditional role
     * @returns {Array} user references which satisfy the role condition
     */
    function processCreatedConditionalRole(role) {
        var existingGrants = relationshipHelper.getConditionalAndDirectGrants(role, 'members', 'role'),
            currentConditionalGrants,
            roleMembers = [];

        currentConditionalGrants = existingGrants.conditionalGrants;
        roleMembers = roleMembers.concat(existingGrants.directGrants);

        currentConditionalGrants = processUserSet(role.condition, currentConditionalGrants, appendConditionalGrants);
        roleMembers = roleMembers.concat(currentConditionalGrants);
        return roleMembers;
    }

    /**
     * This function handles an update to a conditional role's condition (which includes the addition of a condition to
     * a non-conditional role).
     * This function will return the updated role member set. This set will be constituted as follows:
     * 1. it will include the existing, direct role members
     * 2. the existing, conditional role members will be mutated as follows:
     *      a. the users who satisfied the old condition, but do not satisfy the new condition, will be removed.
     *      b. the users who did not satisfy the old condition, but do satisfy the new condition, will be added.
     * @param oldRole the old role
     * @param newRole the updated role
     * @returns {Array} the current set of role members
     */
    function processUpdatedConditionalRole(oldRole, newRole) {
        var existingMembers = relationshipHelper.getConditionalAndDirectGrants(newRole, 'members', 'role'),
            currentConditionalGrants,
            roleGainersQuery,
            roleLosersQuery,
            roleMembers = [];

        currentConditionalGrants = existingMembers.conditionalGrants;
        roleMembers = roleMembers.concat(existingMembers.directGrants);

        roleGainersQuery = combineAndNotFirstCondition(oldRole.condition, newRole.condition);
        roleLosersQuery  = combineAndNotFirstCondition(newRole.condition, oldRole.condition);

        currentConditionalGrants = processUserSet(roleLosersQuery, currentConditionalGrants, filterConditionalGrants);
        currentConditionalGrants = processUserSet(roleGainersQuery, currentConditionalGrants, appendConditionalGrants);

        return roleMembers.concat(currentConditionalGrants);
    }

    /**
     * A method to combine conditions so that role updates can generate the query to determine the users which have
     * gained, or lost, a given role grant in an updated conditional role.
     * @param firstCondition a condition which will be negated and 'anded' with the second condition. Can be null
     * @param secondCondition a condition which will be 'anded with the negated first condition. Can be null.
     * @returns {*} a string which is the combination of the negated first condition, 'anded' with the second condition.
     */
    function combineAndNotFirstCondition(firstCondition, secondCondition) {
        if (!isNil(firstCondition)) {
            if (!isNil(secondCondition)) {
                return '! (' + firstCondition + ') and ' + secondCondition;
            } else {
                return '! (' + firstCondition + ')';
            }
        } else {
            if (!isNil(secondCondition)) {
                return secondCondition;
            } else {
                throw {'error: ' : 'IllegalState: both conditions passed to combineAndNotSecondCondition nil!'};
            }
        }
    }

    // _.isNil is not defined in our version of lodash
    function isNil(object) {
        return object === undefined || object === null;
    }

    /**
     * This method will iterate through a user-set satisfying a condition, and apply a function to mutate the list of
     * conditional role members.
     * @param query the query which will generate the list of users who satisfy a new role condition, or have gained
     * or lost an updated role condition
     * @param currentConditionalMembers the current set of conditional role members
     * @param callbackFunction a function which takes a list of users, and the list of current conditional grants
     * @returns {*} the currentConditionalMembers array, as updated by the callbackFunction
     */
    function processUserSet(query, currentConditionalMembers, callbackFunction) {
        var pageSize = 500,
            queryResult,
            pagedResultsCookie;

        pagedResultsCookie = "";
        do {
            queryResult = getUserSet(query, pageSize, pagedResultsCookie);
            pagedResultsCookie = queryResult._pagedResultsCookie;
            currentConditionalMembers = callbackFunction(queryResult.result, currentConditionalMembers);

        } while (pagedResultsCookie);
        return currentConditionalMembers;
    }

    /**
     * Removes those elements from a set of role grants which correspond to user entries in the roleLosers array.
     * @param roleLosers the array of user objects which have lost the role grant
     * @param currentConditionalGrants the set of grant objects currently associated with the role
     * @returns {*} the currentConditionalGrants array with the roleLosers removed.
     */
    function filterConditionalGrants(roleLosers, currentConditionalGrants) {
        return _(currentConditionalGrants)
            .reject(function (currentGrant) {
                return _.find(roleLosers, function (roleLoser) {return 'managed/user/' + roleLoser._id === currentGrant._ref;}) !== undefined;
            })
            .value();
    }

    /**
     * Adds role grant entries to the currentConditionalGrants array corresponding to users which have gained the role grant.
     * @param roleGainers An array of users which have gained the role grant
     * @param currentConditionalGrants the current set of conditional role grants associated with the role
     * @returns {Array.<T>|string|*} the currentConditionalGrants array with the added role grants
     */
    function appendConditionalGrants(roleGainers, currentConditionalGrants) {
        return currentConditionalGrants.concat(
            _(roleGainers)
                .map(function(roleGainer) {
                    return {
                        '_ref': 'managed/user/' + roleGainer._id, '_refProperties': {'_grantType' : 'conditional'}
                    };
                })
                .value()
        );
    }

    /**
     * Determines if a role's condition has changed
     * @param oldRole the old role
     * @param newRole the new role
     * @returns {boolean} true if the role condition has changed
     */
    function hasRoleConditionChanged(oldRole, newRole) {
        return !_.isEqual(oldRole.condition, newRole.condition);
    }

    /**
     * Queries for the set of users satisfying a condition parameter
     * @param condition the queryFilter condition
     * @param pageSize the size of the result set
     * @param pagedResultsCookie used to track paging through a large result-set
     * @returns {*|{type, title, properties}|{queryFilter, fields, sortKeys}} the queryResults
     */
    function getUserSet(condition, pageSize, pagedResultsCookie)  {
        return openidm.query('managed/user', {'_queryFilter' : condition, '_pageSize' : pageSize,
                                                '_pagedResultsCookie' : pagedResultsCookie})
    }
}());