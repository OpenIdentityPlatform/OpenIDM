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

/*global source */

// Transform a comma-separated XML "roles" attribute into the array form
// expected by the managed/user "authzRoles" relationship.
//
// Lives in a separate file (rather than as an inline transform inside
// sync.json) so the script is registered through the file-based code path of
// ScriptRegistry. Inline scripts embedded in mapping configs can be invoked
// during the very first reconciliation while the registry is still moving the
// freshly compiled script from STARTING (status=8) to ACTIVE (status=32),
// producing a transient "Script status is 8" ScriptException that gets
// flagged by the strict CI log scan.
(function () {
    if (source === null || source === undefined || String(source).length === 0) {
        return [];
    }
    return String(source).split(',').map(function (r) {
        return {
            "_ref": (r.indexOf('openidm-') === 0 ? 'repo/internal/role/' : 'managed/role/') + r
        };
    });
}());

