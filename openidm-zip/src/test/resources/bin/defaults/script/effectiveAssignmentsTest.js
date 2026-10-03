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

/*global exports, java */

/**
 * Tests against roles/effectiveAssignments.js.
 *
 * effectiveAssignments.js is a virtual property script, not a module: it reads its inputs from the
 * script scope and yields the effective assignments as its last expression. The test evaluates the
 * real script inside a function with stubbed host globals.
 */
exports.test = function () {
    var source = readClasspathResource("bin/defaults/script/roles/effectiveAssignments.js");

    [
        // effectiveRolesPropName binding, user object, expected assignment count, description
        [undefined, { effectiveRoles: [ { _ref: "managed/role/r1" } ] }, 1,
            "effectiveRolesPropName defaults to effectiveRoles before the roles are read"],
        ["roles", { roles: [ { _ref: "managed/role/r1" } ] }, 1,
            "a configured effectiveRolesPropName is honoured"],
        [undefined, {}, 0, "a user without effective roles has no effective assignments"]
    ].forEach(function (testcase) {
        var propName = testcase[0], object = testcase[1], expected = testcase[2], scenario = testcase[3],
            actual = evalEffectiveAssignments(source, propName, object).length;
        if (actual !== expected) {
            throw { "message": "effectiveAssignments: " + scenario + " - got <" + actual + ">, expected <" + expected + ">" };
        }
    });

    function evalEffectiveAssignments(source, effectiveRolesPropName, object) {
        var context = {},
            propertyName = "effectiveAssignments",
            logger = { debug: function () {}, trace: function () {} },
            openidm = { read: function (id) {
                return id === "managed/role/r1"
                    ? { assignments: [ { _ref: "managed/assignment/a1" } ] }
                    : { _id: "a1" };
            } };
        return eval(source);
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
