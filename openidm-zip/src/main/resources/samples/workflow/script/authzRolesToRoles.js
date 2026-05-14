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
 * Copyright 2026 3A Systems, LLC.
 */
/*global source, openidm */
// Flatten the managed/user authzRoles relationship into the comma-separated
// "roles" attribute consumed by the XML connector.
//
// File-based (vs inline in sync.json) for the same race-condition reason
// documented in rolesToAuthzRoles.js: avoids ScriptRegistry STARTING-state
// ("Script status is 8") errors during the first reconciliation after boot.
(function () {
    if (source === null || source === undefined || source._id === null || source._id === undefined) {
        return "";
    }
    var result = openidm.query(
        'managed/user/' + source._id + '/authzRoles',
        {'_queryFilter': 'true'}
    );
    if (!result || !result.result) {
        return "";
    }
    return result.result.map(function (r) {
        return r._ref.split('/').pop();
    }).join(',');
}());
