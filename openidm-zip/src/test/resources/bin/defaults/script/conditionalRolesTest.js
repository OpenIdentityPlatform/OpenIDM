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
 * Tests against conditionalRoles.js.
 */
exports.test = function() {
    var conditionalRoles = require("roles/conditionalRoles");

    isTemporalConstraintsMultiValue();

    function isTemporalConstraintsMultiValue() {
            var dateUtil = org.forgerock.openidm.util.DateUtil.getDateUtil(),
                testConstraint = {},
                now = dateUtil.currentDateTime(),
                currentDuration = dateUtil.formatDateTime(now.minusDays(1))
                    + "/" + dateUtil.formatDateTime(now.plusDays(1)),
                expiredDuration = dateUtil.formatDateTime(now.minusDays(2))
                    + "/" + dateUtil.formatDateTime(now.minusDays(1));
            [
                [
                    {
                        "_id": "roleWithOneConstraint",
                        "temporalConstraints": [
                            {
                                "duration": currentDuration
                            }
                        ]
                    },
                    false
                ],
                [
                    {
                        "_id": "roleWithMultipleConstraints",
                        "temporalConstraints": [
                            {
                                "duration": currentDuration
                            },
                            {
                                "duration": expiredDuration
                            }
                        ]
                    },
                    true
                ]
            ].map(
                function (testcase) {
                    (function (role, expectedResult) {
                        var result = conditionalRoles.isTemporalConstraintsMultiValue(role)
                        if (result !== expectedResult) {
                            throw {
                                "message": "Determining if temporalConstraints is multivalue " + JSON.stringify(role) +
                                ", got <" + result + ">, expected <" + expectedResult + ">"
                            };
                        }
                    }).apply(null, testcase);
                });
        }
}