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
 * Copyright 2026 3A Systems, LLC
 */

/**
 * Tests against auth/queryFilter.js, the helper used by the authentication augmentation scripts
 * (auth/populateAsManagedUser.js and auth/amSessionCheck.js) to safely embed an untrusted
 * authenticationId into a managed/user query filter (GHSA-64q4-cp2r-m7rg).
 */
exports.test = function () {
    var queryFilter = require("auth/queryFilter");

    testEscapeStringValue();
    testInjectionIsNeutralized();

    function testEscapeStringValue() {
        // [ rawValue, expectedEscapedValue ]
        [
            // ordinary identifiers are untouched
            ["jsmith", "jsmith"],
            ["", ""],
            ["user@example.com", "user@example.com"],
            // the GHSA-64q4-cp2r-m7rg payload: the closing quote is escaped so it can no longer
            // terminate the literal and start a second predicate
            ['zzz" or /userName eq "victim', 'zzz\\" or /userName eq \\"victim'],
            // a bare double-quote is escaped
            ['a"b', 'a\\"b'],
            // a backslash is escaped (and must be escaped before quotes)
            ["a\\b", "a\\\\b"],
            // a trailing backslash cannot escape the closing delimiter once doubled
            ["foo\\", "foo\\\\"],
            // backslash immediately before a quote: both are escaped independently
            ['a\\"b', 'a\\\\\\"b']
        ].map(function (testcase) {
            (function (rawValue, expectedEscapedValue) {
                var escaped = queryFilter.escapeStringValue(rawValue) + "";
                if (escaped !== expectedEscapedValue) {
                    throw {
                        "message": "escapeStringValue(<" + rawValue + ">) returned <" + escaped
                            + ">, expected <" + expectedEscapedValue + ">"
                    };
                }
            }).apply(null, testcase);
        });
    }

    /**
     * Verifies that, once escaped, the value contributes no syntactically significant (unescaped)
     * double-quote to the filter, so the only unescaped quotes are the two delimiters the script
     * adds around the value. This is what prevents the injected
     * <code>" or /userName eq "victim</code> from becoming a second predicate.
     */
    function testInjectionIsNeutralized() {
        [
            "jsmith",
            'zzz" or /userName eq "victim',
            'a"b"c',
            "trailingBackslash\\",
            'mixed\\"quote'
        ].map(function (rawValue) {
            var filter = '/userName eq "' + queryFilter.escapeStringValue(rawValue) + '"';
            // Remove every escaped pair (\\ or \") so that only structural characters remain;
            // any double-quote left is then an actual filter delimiter.
            var structural = filter.replace(/\\[\\"]/g, "");
            var unescapedQuotes = (structural.match(/"/g) || []).length;
            if (unescapedQuotes !== 2) {
                throw {
                    "message": "filter for <" + rawValue + "> has " + unescapedQuotes
                        + " unescaped quote(s), expected exactly 2 delimiters; filter was <" + filter + ">"
                };
            }
        });
    }
};
