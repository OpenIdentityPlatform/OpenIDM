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

/**
 * Tests against roles/postOperation-roles.js, which is not a module: it runs with the globals of a managed object
 * postCreate/postUpdate/postDelete script, so it is evaluated here as the body of a function taking those globals.
 */
exports.test = function() {
    var source = readScript("bin/defaults/script/roles/postOperation-roles.js"),
        postOperationRoles = new Function("request", "context", "oldObject", "newObject", "resourceName", "openidm",
            source);

    createJobsForConstraint();

    function readScript(path) {
        var stream = java.lang.Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (stream === null) {
            throw { "message": "Script not found on the classpath: " + path };
        }
        try {
            return String(new java.lang.String(stream.readAllBytes(), "UTF-8"));
        } finally {
            stream.close();
        }
    }

    /**
     * Runs the script for a created or updated object and returns the ids of the schedules it created.
     */
    function createdJobs(method, resourceName, oldObject, newObject) {
        var jobs = [],
            openidm = {
                "create": function (resourceContainer, newResourceId) {
                    jobs.push(String(newResourceId));
                },
                "delete": function () {}
            };
        postOperationRoles({ "method": method }, null, oldObject, newObject, new java.lang.String(resourceName),
            openidm);
        return jobs;
    }

    function grant(constraints) {
        return { "roles": [ { "_ref": "managed/role/r1",
            "_refProperties": { "_id": "g1", "temporalConstraints": constraints } } ] };
    }

    function createJobsForConstraint() {
        var dateUtil = org.forgerock.openidm.util.DateUtil.getDateUtil(),
            now = dateUtil.currentDateTime(),
            pendingDuration = dateUtil.formatDateTime(now.plusDays(1))
                + "/" + dateUtil.formatDateTime(now.plusDays(2)),
            // end before start, see issue #250
            reversedDuration = dateUtil.formatDateTime(now.plusDays(1))
                + "/" + dateUtil.formatDateTime(now.minusDays(1));
        [
            [ "create", "managed/role/r1", null, { "temporalConstraints": [ { "duration": pendingDuration } ] }, 2 ],
            // an invalid constraint that is already stored must not fail the request, nor create a schedule
            [ "create", "managed/role/r1", null, { "temporalConstraints": [ { "duration": reversedDuration } ] }, 0 ],
            [ "create", "managed/role/r1", null, { "temporalConstraints": [ { "duration": "not an interval" } ] }, 0 ],
            [ "create", "managed/role/r1", null, { "temporalConstraints": [ null ] }, 0 ],
            [ "create", "managed/user/u1", null, grant([ { "duration": pendingDuration } ]), 2 ],
            [ "create", "managed/user/u1", null, grant([ { "duration": reversedDuration } ]), 0 ],
            [ "create", "managed/user/u1", null, grant([ null ]), 0 ],
            [ "update", "managed/user/u1", grant([ { "duration": reversedDuration } ]),
                grant([ { "duration": pendingDuration } ]), 2 ],
            [ "update", "managed/user/u1", grant([ { "duration": pendingDuration } ]), grant([ null ]), 0 ]
        ].map(
            function (testcase) {
                (function (method, resourceName, oldObject, newObject, expectedJobs) {
                    var jobs = createdJobs(method, resourceName, oldObject, newObject);
                    if (jobs.length !== expectedJobs) {
                        throw {
                            "message": method + " of " + resourceName + " " + JSON.stringify(newObject)
                            + " created jobs " + JSON.stringify(jobs) + ", expected " + expectedJobs + " jobs"
                        };
                    }
                }).apply(null, testcase);
            });
    }
}
