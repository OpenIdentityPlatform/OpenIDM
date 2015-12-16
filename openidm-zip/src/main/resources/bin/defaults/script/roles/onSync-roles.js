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
 * If a role is sync'ed, this script will determine if users of this role needs to also
 * be sync'ed.  Users are always sync'ed, except if the only change is in the ignoreProperties.
 */
(function () {
    var _ = require('lib/lodash');

    /**
     * If shouldSyncUsers then this will call sync on all users of the role that got sync'ed
     * @param resourceName the role path.
     * @param oldValue old state of the role
     * @param newValue updated state of the role
     * @param ignoredProperties always sync except, if there's a change and the only change is the ignoreProperties,
     * then no sync is needed.
     */
    exports.syncUsersOfRoles =
        function (resourceName, oldValue, newValue, ignoredProperties) {
            logger.debug("onSync-roles script invoked for {}", resourceName.toString());
            var members;
            if (shouldSyncUsers(oldValue, newValue, ignoredProperties)) {
                members = openidm.query(resourceName.toString() + '/members', 
                        {"_queryId": "find-relationships-for-resource"}).result;
                _.each(members, function (user) {
                    logger.debug("onSync-roles will call triggerSyncCheck for {}", user._ref);
                    // Issue triggerSyncCheck action and set fields to "*" to indicate all default fields plus any
                    // virtual fields on the managed user, which will pick up changes to "effectiveAssignments".
                    openidm.action(user._ref, "triggerSyncCheck", {}, {}, ["*"]);
                });
            } else {
                logger.debug("onSync-roles will NOT call triggerSyncCheck the users", resourceName.toString());
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

        // If we don't match, after removing ignored fields, then should sync as it means changes were on other fields.
        return !_.isEqual(oldCopy, newCopy);
    }
}());
