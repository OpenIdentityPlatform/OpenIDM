/**
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
 * Copyright 2015 ForgeRock AS.
 */

/**
 * If an assignment is sync'ed, this script will determine if users of any role with this assignment needs to also
 * be sync'ed.  Users are always sync'ed, except if the only change is in the ignoreProperties.
 */
(function () {
    var _ = require('lib/lodash');

    /**
     * If shouldSyncUsers then this will call sync on all users of all roles that have the assignment that got sync'ed
     * @param resourceName the assignment path.
     * @param oldValue old state of the assignment
     * @param newValue updated state of the assignment
     * @param ignoredProperties always sync except, if there's a change and the only change is the ignoreProperties,
     * then no sync is needed.
     */
    exports.syncUsersOfRolesWithAssignment =
        function (resourceName, oldValue, newValue, ignoredProperties) {
            logger.debug("onSync-assignments script invoked for {}", resourceName.toString());
            var roles;
            var users=[];
            if (shouldSyncUsers(oldValue, newValue, ignoredProperties)) {
                roles = openidm.query(resourceName.toString() + '/roles', 
                        {"_queryId": "find-relationships-for-resource"}, ['members']).result;
                _.each(roles, function (role) {
                    _.each(role.members, function (user) {
                        users.push(user._ref);
                    });
                });

                // triggerSyncCheck for each unique user.
                _(users)
                    .uniq()
                    .each(function (user) {
                        logger.debug("onSync-assignments will call triggerSyncCheck for {}", user);
                        // Issue triggerSyncCheck action and set fields to "*" to indicate all default fields plus any
                        // virtual fields on the managed user, which will pick up changes to "effectiveAssignments".
                        openidm.action(user, "triggerSyncCheck", {}, {}, ["*"]);
                    }
                );
            } else {
                logger.debug("onSync-assignments will NOT call triggerSyncCheck the users", resourceName.toString());
            }
        };

    /**
     * Returns true if the oldValue and newValue fully match, or if the only changes are in fields other than any of the
     * ignoredProperties.
     * @param oldValue old state of the resource
     * @param newValue updated state of the resource
     * @param ignoredProperties if only change is one of the ignoreProperties, then no sync is needed.
     * @returns {boolean} true if the oldValue and newValue fully match, or if the only changes are in fields other than
     * any of the ignoredProperties.
     */
    function shouldSyncUsers(oldValue, newValue, ignoredProperties) {
        var oldCopy, newCopy;

        // If they already match then we should sync.
        if (_.isEqual(oldValue, newValue)) {
            return true;
        }

        oldCopy = _.omit(oldValue == null ? {} : oldValue, ignoredProperties);
        newCopy = _.omit(newValue == null ? {} : newValue, ignoredProperties);

        // If we don't match, after removing ignored fields, then should syc as it means changes were on other fields.
        return !_.isEqual(oldCopy, newCopy);
    }
}());
