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
    var _ = require('lib/lodash');

    isTemporalConstraintsMultiValue();

    function isTemporalConstraintsMultiValue() {
            var dateUtil = org.forgerock.openidm.util.DateUtil.getDateUtil(),
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

    evaluateConditionalRoles();
    function evaluateConditionalRoles() {
        var
            mailGoogleRoleDef = {
                'name' : 'MailGoogleRole',
                'condition' : "/mail co 'google'",
                'description' : 'does mail contain google'
            },
            mailExampleRoleDef = {
                'name' : 'MailExampleRole',
                'condition' : "/mail co 'example'",
                'description' : 'does mail contain example'
            },
            mailComRoleDef = {
                'name' : 'MailComRole',
                'condition' : "/mail co 'com'",
                'description' : 'does mail contain com'
            };

        function getSystemConditionalRoles() {
            return [
                getRole(mailGoogleRoleDef),
                getRole(mailExampleRoleDef),
                getRole(mailComRoleDef)
            ];
        }
        
        function getRole(roleDef) {
            return {
                '_id' : roleDef.name,
                '_rev' : '1',
                'name' :  'role' + roleDef.name,
                'condition' :  roleDef.condition,
                'description' : roleDef.description
            };
        }

        function baseUser(userName, email) {
            var userId = 'user' + userName;
            return {
                '_id' : userId,
                '_ref' : '1',
                //passing in email as param - cannot catenate the userId with the mail suffix, as this will cause condition evaluation to fail
                //due to mozilla type marshalling - strings composed with + marshal to org.mozilla.javascript.ConsString
                //which will fail the QueryFilterCondition#isCompatible type congruence check.
                'mail' : email,
                'givenName' : userId + ' givenName',
                'sn' : userId + 'sn',
                'description' : 'description of ' + userId,
                'userName' : userId + '@example.com',
                'status' : 'active'
            };
        }

        function getRoleGrant(roleDef, isConditional) {
            var grantDef = {}
            if (roleDef) {
                grantDef = {
                    '_ref': 'managed/role/' + roleDef.name,
                    '_refProperties': getRoleGrantRefProperties(isConditional)
                };
            }
            return grantDef;
        }

        function getRoleGrantRefProperties(isConditional) {
            var refProperties =
            {
                '_rev': '1',
                '_id': 'superRandomGrantId'
            }
            if (isConditional) {
                    refProperties['_grantType'] = 'conditional';
            }
            return refProperties;
        }

        function getUserRoleGrants(conditionalRole, directRole) {
            return {
                'conditionalGrants' : conditionalRole != null ? [getRoleGrant(conditionalRole, true)] : [],
                'directGrants' : directRole != null ? [getRoleGrant(directRole, false)] : []
            };
        }

        function userHasGrants(userRoles, roleIds) {
            var roleIdInGrants =
                function(roleId) {
                    return _.find(userRoles, function (roleGrant) { return 'managed/role/' + roleId === roleGrant._ref}) !== undefined;
                };
            return roleIds.every(roleIdInGrants)
        }

        /*
        The conditionalRoles script does not create an id for the grants it creates - this occurs on persistence. So
        if the grant was created by the script, it will have an undefined id.
         */
        function grantsCreatedByScriptInvocation(userRoles, roleIds) {
            var roleIdInGrants =
                function(roleId) {
                    var grant =  _.find(userRoles, function (roleGrant) { return 'managed/role/' + roleId === roleGrant._ref});
                    if (grant === undefined) {
                        return false;
                    }
                    return grant._refProperties._id === undefined;
                };
            return roleIds.every(roleIdInGrants)
        }

        [
            [
                baseUser('googleUser', 'baseUser@google.com'),
                getSystemConditionalRoles(),
                getUserRoleGrants(null, mailGoogleRoleDef),
                [mailComRoleDef.name, mailGoogleRoleDef.name],
                [mailComRoleDef.name]
            ],
            [
                baseUser('exampleUser', 'baseUser@example.com'),
                getSystemConditionalRoles(),
                getUserRoleGrants(null, null),
                [mailComRoleDef.name, mailExampleRoleDef.name],
                [mailComRoleDef.name, mailExampleRoleDef.name]
            ],
            [
                baseUser('exampleUser', 'baseUser@dillrod.com'),
                getSystemConditionalRoles(),
                getUserRoleGrants(null, null),
                [mailComRoleDef.name],
                [mailComRoleDef.name]
            ],
            [
                baseUser('exampleUser', 'baseUser@google.com'),
                getSystemConditionalRoles(),
                getUserRoleGrants(mailGoogleRoleDef, null),
                [mailComRoleDef.name, mailGoogleRoleDef.name],
                [mailComRoleDef.name]
            ]

        ].map(
            function (testcase) {
                (function (user, systemConditionalRoles, userRoleGrants, expectedRoleIds, createdRoleIds) {
                    conditionalRoles.evaluateConditionalRoles(user, 'roles', systemConditionalRoles, userRoleGrants);
                    if (!userHasGrants(user.roles, expectedRoleIds) || !grantsCreatedByScriptInvocation(user.roles, createdRoleIds)) {
                        throw {
                            "message": "Evaluating conditional roles failure: script-produced user roles: <" + JSON.stringify(user.roles) +
                            ">, but should have had grants corresponding to role ids <" + JSON.stringify(expectedRoleIds) + ">" +
                            "of which <" + JSON.stringify(createdRoleIds) + "> role ids should have been created by the script."
                        };
                    }
                }).apply(null, testcase);
            });

    }
}