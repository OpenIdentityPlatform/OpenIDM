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
 * Copyright 2016 ForgeRock AS.
 */

/**
 * Tests against temporalConstraints.js.
 */
exports.test = function() {
    var temporalConstraints = require("roles/temporalConstraints");

    areConstraintsPendingOrPast();

    function areConstraintsPendingOrPast() {
        var dateUtil = org.forgerock.openidm.util.DateUtil.getDateUtil(),
            testConstraint = {},
            now = dateUtil.currentDateTime(),
            currentDuration = dateUtil.formatDateTime(now.minusDays(1))
                + "/" + dateUtil.formatDateTime(now.plusDays(1)),
            pendingDuration = dateUtil.formatDateTime(now.plusDays(1))
                + "/" + dateUtil.formatDateTime(now.plusDays(2)),
            expiredDuration = dateUtil.formatDateTime(now.minusDays(2))
                + "/" + dateUtil.formatDateTime(now.minusDays(1));

        [
            [
                {
                    "_id" : "role1",
                    "temporalConstraints" : [
                        {
                            "duration" : currentDuration
                        }
                    ]
                },
                false
            ],
            [
                {
                    "_id" : "role2",
                    "temporalConstraints" : [
                        {
                            "duration" : pendingDuration
                        }
                    ]
                },
                false
            ],
            [
                {
                    "_id" : "role2",
                    "temporalConstraints" : [
                        {
                            "duration" : expiredDuration
                        }
                    ]
                },
                true
            ]
        ].map(
            function (testcase) {
                (function (role, expectedResult) {
                    var result = temporalConstraints.areConstraintsExpired(role);
                    if (result !== expectedResult) {
                        throw {
                            "message": "Determining expired constraints for role " + JSON.stringify(role) +", got <"
                            + result + ">, expected <" + expectedResult + ">"
                        };
                    }
                }).apply(null, testcase);
            });
    }
}