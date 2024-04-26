define([
    "lodash",
    "org/forgerock/openidm/ui/admin/authentication/AuthenticationModuleDialogView"
], function (_, AuthenticationModuleDialogView) {
    QUnit.module('AuthenticationModuleDialogView Tests');

    QUnit.test("Retrieve all module views", function (assert) {

        var modules = [
            "CLIENT_CERT",
            "IWA",
            "OAUTH",
            "OPENAM_SESSION",
            "OPENID_CONNECT",
            "SOCIAL_PROVIDERS",
            "STATIC_USER",
            "TRUSTED_ATTRIBUTE"
        ];

        _.each(modules, function(module) {
            var ready = assert.async();
            AuthenticationModuleDialogView.getModuleView(module).then(function(view) {
                ready();
                assert.equal(view.template, "templates/admin/authentication/modules/"+module+".html", module + " template retrieved");
            });

        });

    });
});