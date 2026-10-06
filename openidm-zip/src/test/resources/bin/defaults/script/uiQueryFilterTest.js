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

/*global exports, java, org */

/**
 * Tests that the admin UI endpoint scripts ui/reconResults.js and ui/mappingDetails.js escape the
 * values they embed in CREST query filter string literals.
 *
 * A recon id, a situation, a connector object id or a mapping name containing a double quote or a
 * backslash used to produce a filter such as <code>_id eq "a"b"</code>, and the endpoint failed.
 *
 * The scripts are not modules: they expect the OpenIDM host globals (request, openidm) and return
 * the endpoint result. The test reads the real scripts from the classpath, evaluates them with stub
 * globals, parses every captured _queryFilter with the CREST parser and checks that the string
 * literals come back as the raw values.
 */
exports.test = function () {
    var QueryFilters = org.forgerock.json.resource.QueryFilters;

    testReconResults();
    testReconResultsSearch();
    testMappingDetails();

    function testReconResults() {
        var source = readClasspathResource("bin/defaults/script/ui/reconResults.js"),
            sourceId = 'a"b',
            targetId = "c\\",
            reconEnded = '2026-10-03T00:00:00.000Z"\\',
            captured = {},
            request = {
                additionalParameters: {
                    mapping: "systemLdapAccounts_managedUser",
                    source: "system/ldap/account",
                    target: "managed/user",
                    reconId: 'r"1\\',
                    situations: 'CONFIRMED,x"y',
                    page: "1",
                    rows: "20",
                    search: "false"
                }
            },
            openidm = {
                read: function () {
                    return { ended: reconEnded };
                },
                query: function (resource, params) {
                    captured[resource] = String(params._queryFilter);
                    if (resource === "audit/recon") {
                        return { result: [{
                            sourceObjectId: "system/ldap/account/" + sourceId,
                            targetObjectId: "managed/user/" + targetId,
                            situation: "CONFIRMED"
                        }] };
                    }
                    return { result: [{ _id: resource === "managed/user" ? targetId : sourceId }] };
                }
            },
            rows = eval(source)[0].rows;

        assertLiterals(captured["audit/recon"],
                ['r"1\\', "entry", reconEnded, "CONFIRMED", 'x"y'], "reconResults audit/recon");
        assertLiterals(captured["system/ldap/account"], [sourceId], "reconResults source objects");
        assertLiterals(captured["managed/user"], [targetId], "reconResults target objects");

        if (rows.length !== 1 || rows[0].sourceObject._id !== sourceId || rows[0].targetObject._id !== targetId) {
            throw { "message": "reconResults: the source and target objects were not joined to the audit row" };
        }
    }

    /**
     * The search text typed into the recon results grid is matched as-is: it used to be
     * URL-encoded into the literal, so "John Smith" was searched as "John%20Smith".
     */
    function testReconResultsSearch() {
        var source = readClasspathResource("bin/defaults/script/ui/reconResults.js"),
            criteria = 'J "S\\',
            captured = {},
            request = {
                additionalParameters: {
                    source: "system/ldap/account",
                    target: "managed/user",
                    sourceProps: "givenName,sn",
                    reconId: "r1",
                    search: "true",
                    sourceObjectDisplay: criteria
                }
            },
            openidm = {
                read: function () {
                    return {};
                },
                query: function (resource, params) {
                    captured[resource] = String(params._queryFilter);
                    return { result: resource === "system/ldap/account" ? [{ _id: 'a"b' }] : [] };
                }
            };

        eval(source);

        assertLiterals(captured["system/ldap/account"], [criteria, criteria], "reconResults source search");
        assertLiterals(captured["audit/recon"], ["r1", "entry", 'system/ldap/account/a"b'], "reconResults search audit/recon");
    }

    function testMappingDetails() {
        var source = readClasspathResource("bin/defaults/script/ui/mappingDetails.js"),
            mappingName = 'm"1\\',
            captured = null,
            request = { additionalParameters: { mapping: mappingName } },
            openidm = {
                read: function () {
                    return { mappings: [{ name: mappingName }] };
                },
                query: function (resource, params) {
                    captured = String(params._queryFilter);
                    return { result: [] };
                }
            };

        eval(source);

        assertLiterals(captured, ["start", "reconById", mappingName], "mappingDetails audit/recon");
    }

    /**
     * Parses the filter with the CREST parser and compares the values of its comparison leaves, in
     * order, with the expected raw values. A value that broke out of its literal either makes the
     * parse fail or changes the leaves.
     */
    function assertLiterals(filter, expected, scenario) {
        var actual = [],
            parsed;

        try {
            parsed = QueryFilters.parse(filter);
        } catch (e) {
            throw { "message": scenario + ": filter <" + filter + "> does not parse: " + e };
        }
        collectLiterals(parsed, actual);

        if (actual.length !== expected.length || actual.some(function (value, i) { return value !== expected[i]; })) {
            throw {
                "message": scenario + ": filter <" + filter + "> has literals <" + actual.join("|")
                        + ">, expected <" + expected.join("|") + ">"
            };
        }
    }

    function collectLiterals(filter, literals) {
        var visitor,
            visitAll = function (filters) {
                var i;
                for (i = 0; i < filters.size(); i++) {
                    filters.get(i).accept(visitor, null);
                }
                return null;
            },
            leaf = function (p, field, value) {
                literals.push(String(value));
                return null;
            };

        visitor = new org.forgerock.util.query.QueryFilterVisitor({
            visitAndFilter: function (p, filters) { return visitAll(filters); },
            visitOrFilter: function (p, filters) { return visitAll(filters); },
            visitNotFilter: function (p, sub) { return sub.accept(visitor, null); },
            visitEqualsFilter: leaf,
            visitGreaterThanFilter: leaf,
            visitGreaterThanOrEqualToFilter: leaf,
            visitLessThanFilter: leaf,
            visitLessThanOrEqualToFilter: leaf,
            visitStartsWithFilter: leaf,
            visitContainsFilter: leaf,
            visitExtendedMatchFilter: function (p, field, operator, value) { return leaf(p, field, value); },
            visitPresentFilter: function () { return null; },
            visitBooleanLiteralFilter: function () { return null; }
        });

        filter.accept(visitor, null);
    }

    function readClasspathResource(path) {
        var url = java.lang.Thread.currentThread().getContextClassLoader().getResource(path),
            scanner,
            text;
        if (url === null) {
            throw { "message": "classpath resource not found: " + path };
        }
        scanner = new java.util.Scanner(url.openStream(), "UTF-8").useDelimiter("\\A");
        text = scanner.hasNext() ? scanner.next() : "";
        scanner.close();
        return String(text);
    }
};
