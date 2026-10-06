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
    "org/forgerock/openidm/ui/common/resource/ResourceCollectionRelationshipsView",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate",
    "org/forgerock/commons/ui/common/util/ModuleLoader"
], function ($, sinon, ResourceCollectionRelationshipsView, ResourceDelegate, ModuleLoader) {
    QUnit.module('ResourceCollectionRelationshipsView Tests');

    QUnit.test("render escapes and URL-encodes the parent id in the relationship query", function (assert) {
        var pending = function () { return $.Deferred().promise(); },
            stubs = [
                sinon.stub(ResourceDelegate, "searchResource", pending),
                sinon.stub(ModuleLoader, "load", pending)
            ],
            searchStub = stubs[0],
            parts;

        try {
            new ResourceCollectionRelationshipsView().render({
                element: "#relationships",
                schema: { properties: {} },
                prop: {
                    propName: "manager",
                    parentId: 'a"b\\%41',
                    resourceCollection: {
                        path: "managed/user",
                        query: { fields: ["userName"] }
                    }
                }
            });

            parts = searchStub.firstCall.args[0].split("&");
            assert.equal(decodeURIComponent(parts[0]), 'manager eq "a\\"b\\\\%41"');
            assert.deepEqual(parts.slice(1), ["_pageSize=100", "_sortKeys=userName"]);
            assert.equal(searchStub.firstCall.args[1], "managed/user");
        } finally {
            stubs.forEach(function (stub) { stub.restore(); });
        }
    });
});
