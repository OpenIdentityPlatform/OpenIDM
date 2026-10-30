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
    "org/forgerock/openidm/ui/admin/util/AdminUtils",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate"
], function ($, sinon, AdminUtils, ConfigDelegate) {
    QUnit.module('AdminUtils Tests');

    QUnit.test("findPropertiesList reads the type array of a Nullable relationship", function (assert) {
        var done = assert.async(),
            readEntity = sinon.stub(ConfigDelegate, "readEntity", function () {
                return $.Deferred().resolve({
                    objects: [
                        { name: "user", schema: { properties: { manager: { type: ["relationship", "null"] } } } }
                    ]
                });
            });

        AdminUtils.findPropertiesList(["managed", "user"]).always(function () {
            readEntity.restore();
        }).then(function (properties) {
            assert.strictEqual(properties.manager.type, "relationship");
            done();
        });
    });
});
