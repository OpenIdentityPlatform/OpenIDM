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
 * Portions Copyright 2026 3A Systems, LLC.
 */

define([
    "jquery",
    "sinon",
    "org/forgerock/openidm/ui/admin/mapping/properties/MappingAssignmentsView",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate"
], function ($, sinon, MappingAssignmentsView, ResourceDelegate) {
    QUnit.module('MappingAssignmentsView Tests');

    QUnit.test("render escapes and URL-encodes the mapping name in the assignment query", function (assert) {
        var stubs = [
                sinon.stub(MappingAssignmentsView, "getMappingName").returns('m"1\\%41'),
                sinon.stub(MappingAssignmentsView, "getCurrentMapping").returns({}),
                sinon.stub(ResourceDelegate, "searchResource", function () {
                    return $.Deferred().promise();
                })
            ],
            searchStub = stubs[2];

        try {
            MappingAssignmentsView.render();

            assert.equal(decodeURIComponent(searchStub.firstCall.args[0]), '/mapping eq "m\\"1\\\\%41"');
            assert.equal(searchStub.firstCall.args[1], "managed/assignment");
        } finally {
            stubs.forEach(function (stub) { stub.restore(); });
        }
    });
});
