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
 * Copyright 2016 ForgeRock AS.
 * Portions Copyright 2026 3A Systems, LLC.
 */

define([
    "jquery",
    "sinon",
    "org/forgerock/openidm/ui/admin/util/WorkflowUtils",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate"
], function ($, sinon, WorkflowUtils, ResourceDelegate) {
    QUnit.module('WorkflowUtils Tests');

    function candidateUsersFilter(candidateUsers) {
        var parentView = {
                model: {
                    get: function () {
                        return { candidateUsers: candidateUsers };
                    }
                }
            },
            stub = sinon.stub(ResourceDelegate, "searchResource", function () {
                return $.Deferred().promise();
            }),
            filter;

        try {
            WorkflowUtils.showCandidateUserSelection(parentView);
            filter = stub.firstCall.args[0];
        } finally {
            stub.restore();
        }
        return decodeURIComponent(filter);
    }

    QUnit.test("showCandidateUserSelection escapes the candidate user names", function (assert) {
        assert.equal(candidateUsersFilter(['a"b', "c\\%41"]), 'userName eq "a\\"b" or userName eq "c\\\\%41"');
    });

    QUnit.test("showCandidateUserSelection queries nothing when there are no candidate users", function (assert) {
        assert.equal(candidateUsersFilter([]), "false");
    });

    QUnit.test("userSearchQueryFilter escapes and URL-encodes the search text", function (assert) {
        var filter = WorkflowUtils.userSearchQueryFilter(["displayName", "userName"], "co", 'a"b\\%41');

        assert.equal(decodeURIComponent(filter), 'displayName co "a\\"b\\\\%41" or userName co "a\\"b\\\\%41"');
    });
});
