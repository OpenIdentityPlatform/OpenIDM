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
 * Copyright 2026 3A Systems, LLC.
 */

define([
    "org/forgerock/openidm/ui/common/util/QueryFilterUtils"
], function (QueryFilterUtils) {
    QUnit.module('QueryFilterUtils Tests');

    QUnit.test("escapeStringValue leaves ordinary values unchanged", function (assert) {
        assert.equal(QueryFilterUtils.escapeStringValue("jsmith"), "jsmith");
        assert.equal(QueryFilterUtils.escapeStringValue(""), "");
        assert.equal(QueryFilterUtils.escapeStringValue("o'neil"), "o'neil", "a single quote is not significant inside a double-quoted literal");
    });

    QUnit.test("escapeStringValue escapes a double quote", function (assert) {
        assert.equal(QueryFilterUtils.escapeStringValue('a"b'), 'a\\"b');
    });

    QUnit.test("escapeStringValue escapes a backslash so it cannot swallow the closing quote", function (assert) {
        assert.equal(QueryFilterUtils.escapeStringValue("a\\"), "a\\\\");
    });

    QUnit.test("escapeStringValue escapes the backslash before the quote", function (assert) {
        assert.equal(QueryFilterUtils.escapeStringValue('a\\"b'), 'a\\\\\\"b');
    });

    QUnit.test("escapeStringValue coerces a non-string value", function (assert) {
        assert.equal(QueryFilterUtils.escapeStringValue(42), "42");
    });
});
