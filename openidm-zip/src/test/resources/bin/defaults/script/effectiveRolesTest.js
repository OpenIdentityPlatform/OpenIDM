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
 * Tests against effectiveRoles.js.
 */
exports.test = function() {
    var effectiveRoles = require("roles/effectiveRoles");

    testProcessTemporalConstraintsForRole();

    function testProcessTemporalConstraintsForRole() {
        var dateUtil = org.forgerock.openidm.util.DateUtil.getDateUtil(),
            testConstraint = {},
            now = dateUtil.currentDateTime(),
            passDuration1 = dateUtil.formatDateTime(now.minusDays(1)) 
                + "/" + dateUtil.formatDateTime(now.plusDays(1)),
            passDuration2 = dateUtil.formatDateTime(now.minusHours(1)) 
                + "/" + dateUtil.formatDateTime(now.plusHours(1)),
            failDuration1 = dateUtil.formatDateTime(now.plusDays(1)) 
                + "/" + dateUtil.formatDateTime(now.plusDays(2)),
            failDuration2 = dateUtil.formatDateTime(now.plusHours(1)) 
                + "/" + dateUtil.formatDateTime(now.plusHours(2));
        
        // test cases for applyConstraint
        [
            [ 
                { 
                    "_id" : "role1", 
                    "temporalConstraints" : [ 
                        { 
                            "duration" : passDuration1 
                        }
                    ] 
                }, 
                true
            ],
            [ 
                { 
                    "_id" : "role2", 
                    "temporalConstraints" : [ 
                        { 
                            "duration" : passDuration1 
                        },
                        { 
                            "duration" : failDuration2 
                        }
                    ] 
                }, 
                true
            ],
            [ 
                { 
                    "_id" : "role3", 
                    "temporalConstraints" : [ 
                        { 
                            "duration" : passDuration1 
                        },
                        { 
                            "duration" : failDuration1 
                        }
                    ] 
                }, 
                true
            ],
            [ 
                { 
                    "_id" : "role4", 
                    "temporalConstraints" : [ 
                        { 
                            "duration" : failDuration1 
                        },
                        { 
                            "duration" : failDuration2 
                        }
                    ] 
                }, 
                false
            ]
        ].map(
            function (testcase) {
                (function (role, expectedResult) {
                    // apply the temporal constraint
                    var result = effectiveRoles.processTemporalConstraints(role);
                    if (result !== expectedResult) {
                        throw {
                            "message": "Applying constraint failed for role " + role._id +", got <" 
                            + result + ">, expected <" + expectedResult + ">"
                        };
                    }
                }).apply(null, testcase);
            });
    }
}