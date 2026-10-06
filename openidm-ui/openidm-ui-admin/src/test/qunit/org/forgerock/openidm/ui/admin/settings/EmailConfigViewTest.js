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
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/common/util/ThemeManager",
    "org/forgerock/commons/ui/common/main/ValidatorsManager"
], function ($, sinon, EmailConfigView, ConfigDelegate, ThemeManager, ValidatorsManager) {
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

    // renders the real template with the given stored config
    function renderStored(config, callback) {
        var theme = sinon.stub(ThemeManager, "getTheme", function () {
                return $.Deferred().resolve({});
            }),
            read = sinon.stub(ConfigDelegate, "readEntity", function () {
                return $.Deferred().resolve(config);
            }),
            bind = sinon.stub(ValidatorsManager, "bindValidators"),
            validate = sinon.stub(ValidatorsManager, "validateAllFields");

        $("#qunit-fixture").html('<div id="emailContainer"></div>');
        EmailConfigView.model = { externalEmailExists: false };
        EmailConfigView.data = { config: {} };
        EmailConfigView.render([], function () {
            theme.restore();
            read.restore();
            bind.restore();
            validate.restore();
            callback();
            EmailConfigView.undelegateEvents();
        });
    }

    QUnit.test("a stored STARTTLS required renders Use STARTTLS on", function (assert) {
        var done = assert.async();

        renderStored({ host: "smtp.example.com", starttls: { required: true } }, function () {
            assert.ok(EmailConfigView.$el.find("#emailTLS").prop("checked"), "required implies STARTTLS");
            assert.ok(EmailConfigView.$el.find("#emailTLSRequired").prop("checked"), "required is shown");
            done();
        });
    });

    QUnit.test("the rendered Require STARTTLS switch saves false when unchecked", function (assert) {
        var done = assert.async();

        renderStored({ host: "smtp.example.com", starttls: { enable: true, required: true } }, function () {
            var saved,
                update = sinon.stub(ConfigDelegate, "updateEntity", function (id, config) {
                    saved = config;
                    return $.Deferred();
                });

            EmailConfigView.model.externalEmailExists = true;
            EmailConfigView.$el.find("#emailTLSRequired").prop("checked", false);
            EmailConfigView.save({ preventDefault: $.noop });
            update.restore();

            assert.strictEqual(saved.starttls.enable, true, "STARTTLS stays on");
            assert.strictEqual(saved.starttls.required, false, "the template's switch overrides the stored true");
            done();
        });
    });

    QUnit.test("the STARTTLS switches stay consistent on change", function (assert) {
        $("#qunit-fixture").html('<input type="checkbox" id="emailTLS">' +
            '<input type="checkbox" id="emailTLSRequired">');
        EmailConfigView.setElement($("#qunit-fixture"));

        $("#emailTLSRequired").prop("checked", true).trigger("change");
        assert.ok($("#emailTLS").prop("checked"), "checking required turns STARTTLS on");

        $("#emailTLS").prop("checked", false).trigger("change");
        assert.notOk($("#emailTLSRequired").prop("checked"), "turning STARTTLS off clears required");

        EmailConfigView.undelegateEvents();
    });
});
