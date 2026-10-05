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
    "org/forgerock/openidm/ui/admin/util/BackgridUtils"
], function (BackgridUtils) {
    QUnit.module('BackgridUtils Tests');

    function queryFilterFor(text) {
        var gridState = {
            state: {
                filters: [{ name: "userName", query: function () { return text; } }]
            }
        };
        return BackgridUtils.queryFilter.call(gridState, {});
    }

    QUnit.test("queryFilter escapes a double quote in the filter text", function (assert) {
        assert.equal(queryFilterFor('a"b'), 'userName sw "a\\"b"');
    });

    QUnit.test("queryFilter escapes a backslash so it cannot swallow the closing quote", function (assert) {
        assert.equal(queryFilterFor('a\\'), 'userName sw "a\\\\"');
    });

    QUnit.test("queryFilter escapes a backslash-quote sequence so it cannot break out of the literal", function (assert) {
        assert.equal(queryFilterFor('a\\"b'), 'userName sw "a\\\\\\"b"');
    });
});
