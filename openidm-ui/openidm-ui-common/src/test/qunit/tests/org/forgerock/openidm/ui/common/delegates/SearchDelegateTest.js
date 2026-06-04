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
 * Portions copyright 2026 3A Systems, LLC.
 */
define([
    "org/forgerock/openidm/ui/common/delegates/SearchDelegate"
], function (SearchDelegate) {
    QUnit.module('Search Delegate Tests');

    QUnit.test("query Filter Builder", function (assert) {
        var props = ["userName", "givenName", "sn"],
            search = "test",
            additionalQuery = true,
            comparisonOperator = null;

        assert.equal(SearchDelegate.generateQueryFilter(props, search), '(userName sw "test" or (givenName sw "test" or (sn sw "test")))', "Basic Query Filter Generated");
        assert.equal(SearchDelegate.generateQueryFilter(props, search, additionalQuery, comparisonOperator), '((userName sw "test" or (givenName sw "test" or (sn sw "test"))) and (true))', "Complex Query Filter Generated");
    });

    QUnit.test("buildSearchUrl uses the first property as _sortKeys", function (assert) {
        var url = SearchDelegate.buildSearchUrl("managed/user", ["userName", "sn"], "test", null, null, 10);

        assert.equal(
            url,
            '/managed/user?_sortKeys=userName&_pageSize=10&_queryFilter=(userName sw "test" or (sn sw "test"))',
            "_sortKeys is set to the first property"
        );
    });

    QUnit.test("buildSearchUrl skips empty _sortKeys when the first property is missing (discussion #186)", function (assert) {
        // Mapping whose first property has no "source" => leading undefined value.
        var url = SearchDelegate.buildSearchUrl("managed/user", [undefined, "sn"], "test", null, null, 10);

        assert.equal(url.indexOf("_sortKeys=&"), -1, "no empty _sortKeys= is emitted");
        assert.equal(
            url,
            '/managed/user?_sortKeys=sn&_pageSize=10&_queryFilter=(sn sw "test")',
            "_sortKeys falls back to the first non-empty property"
        );
    });

    QUnit.test("buildSearchUrl omits _sortKeys entirely when no property is usable", function (assert) {
        var url = SearchDelegate.buildSearchUrl("managed/user", [undefined, ""], "test", null, null, 10);

        assert.equal(url.indexOf("_sortKeys"), -1, "no _sortKeys parameter is present at all");
        assert.equal(url.indexOf("?_pageSize=10"), "/managed/user".length, "the query string starts directly with _pageSize");
    });

    QUnit.test("buildSearchUrl omits _sortKeys for system resources that may not support sorting", function (assert) {
        var url = SearchDelegate.buildSearchUrl("system/hr/account", ["email", "lastName"], "Sanchez", null, null, 10);

        assert.equal(url.indexOf("_sortKeys"), -1, "no _sortKeys parameter is sent to a system connector");
        assert.equal(
            url,
            '/system/hr/account?_pageSize=10&_queryFilter=(email sw "Sanchez" or (lastName sw "Sanchez"))',
            "system resource query has no _sortKeys but keeps the query filter"
        );
    });
});
