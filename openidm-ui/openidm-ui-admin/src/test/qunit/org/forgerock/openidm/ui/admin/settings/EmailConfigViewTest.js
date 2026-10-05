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
 * Portions Copyright 2026 3A Systems, LLC.
 */

define([
    "jquery",
    "sinon",
    "org/forgerock/openidm/ui/admin/settings/EmailConfigView",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate"
], function ($, sinon, EmailConfigView, ConfigDelegate) {
    QUnit.module('EmailConfigView Tests');

    QUnit.test("save keeps the STARTTLS keys the form does not edit", function (assert) {
        var saved,
            stub = sinon.stub(ConfigDelegate, "updateEntity", function (id, config) {
                saved = config;
                return $.Deferred();
            });

        $("#qunit-fixture").html('<input type="checkbox" id="emailToggle" checked>' +
            '<form id="emailConfigForm">' +
            '<input type="text" name="host" value="smtp.example.com">' +
            '<input type="checkbox" name="starttls.enable" value="true" checked>' +
            '</form>');
        EmailConfigView.$el = $("#qunit-fixture");
        EmailConfigView.model = { externalEmailExists: true };
        EmailConfigView.data = {
            config: {
                host: "smtp.example.com",
                starttls: { enable: true, trustedHosts: ["smtp.internal"], trustAll: false }
            }
        };

        EmailConfigView.save({ preventDefault: $.noop });
        stub.restore();

        assert.ok(saved.starttls.enable, "the form's STARTTLS flag is saved");
        assert.deepEqual(saved.starttls.trustedHosts, ["smtp.internal"], "trustedHosts is kept");
        assert.strictEqual(saved.starttls.trustAll, false, "trustAll is kept");
    });
});
