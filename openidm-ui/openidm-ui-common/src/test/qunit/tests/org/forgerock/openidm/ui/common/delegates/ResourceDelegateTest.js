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
 * Copyright 2016 ForgeRock AS.
 * Portions Copyright 2026 3A Systems, LLC.
 */

define([
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate"
], function (ResourceDelegate) {
    QUnit.module('ResourceDelegate Tests');

    QUnit.test("queryStringForSearchableFields escapes and URL-encodes the search text", function (assert) {
        var queryString = ResourceDelegate.queryStringForSearchableFields(["userName", "sn"], 'a"b\\&c'),
            parts = queryString.split("&");

        assert.equal(decodeURIComponent(parts[0]), 'userName sw "a\\"b\\\\&c" or sn sw "a\\"b\\\\&c"',
            "the filter is escaped, and its & does not end the parameter");
        assert.deepEqual(parts.slice(1), ["_pageSize=10", "_fields=*"]);
    });
});
