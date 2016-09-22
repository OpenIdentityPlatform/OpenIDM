define(["org/forgerock/openidm/ui/admin/user/EditUserView"], function (EditUserView) {
    QUnit.module('EditUserView Tests');

    QUnit.test("Conditionally displays alert", function (assert) {
        EditUserView.setEmailServiceAvailable(true);
        EditUserView.data.newObject = true;
        EditUserView.setResetPasswordScriptAvailable(true);
        assert.equal(EditUserView.getEmailConfigAlertHidden(), true, "Email is configured, this is a new object, the reset script is available. Should be hidden");

        EditUserView.setEmailServiceAvailable(true);
        EditUserView.data.newObject = true;
        EditUserView.setResetPasswordScriptAvailable(false);
        assert.equal(EditUserView.getEmailConfigAlertHidden(), true, "Email is configured, this is a new object, the reset script is NOT available. Should be hidden");

        EditUserView.setEmailServiceAvailable(true);
        EditUserView.data.newObject = false;
        EditUserView.setResetPasswordScriptAvailable(true);
        assert.equal(EditUserView.getEmailConfigAlertHidden(), true, "Email is configured, this is an EXISTING object, the reset script is available. Should be hidden");

        EditUserView.setEmailServiceAvailable(true);
        EditUserView.data.newObject = false;
        EditUserView.setResetPasswordScriptAvailable(false);
        assert.equal(EditUserView.getEmailConfigAlertHidden(), true, "Email is configured, this is an EXISTING object, the reset script is NOT available. Should be hidden");

        EditUserView.setEmailServiceAvailable(false);
        EditUserView.data.newObject = true;
        EditUserView.setResetPasswordScriptAvailable(true);
        assert.equal(EditUserView.getEmailConfigAlertHidden(), true, "Email is NOT configured, this is a new object, the reset script is available. Should be hidden");

        EditUserView.setEmailServiceAvailable(false);
        EditUserView.data.newObject = true;
        EditUserView.setResetPasswordScriptAvailable(false);
        assert.equal(EditUserView.getEmailConfigAlertHidden(), true, "Email is NOT configured, this is a new object, the reset script is NOT available. Should be hidden");

        EditUserView.setEmailServiceAvailable(false);
        EditUserView.data.newObject = false;
        EditUserView.setResetPasswordScriptAvailable(true);
        assert.equal(EditUserView.getEmailConfigAlertHidden(), false, "Email is NOT configured, this is an EXISTING object, the reset script is available. Should be SHOWN");

        EditUserView.setEmailServiceAvailable(false);
        EditUserView.data.newObject = false;
        EditUserView.setResetPasswordScriptAvailable(false);
        assert.equal(EditUserView.getEmailConfigAlertHidden(), true, "Email is NOT configured, this is an EXISTING object, the reset script is NOT available. Should be hidden");
    });

    QUnit.test("Conditionally displays reset button", function (assert) {
        var newEl;

        EditUserView.setEmailServiceAvailable(true);
        EditUserView.data.newObject = false;
        EditUserView.setResetPasswordScriptAvailable(true);
        newEl = EditUserView.configureResetPasswordButton($('<a></a>'));
        assert.equal(newEl.prop("style").display === "none", false, "Email is configured, this is an EXISTING object, the reset script is available. Button should be SHOWN.");
        assert.equal(newEl.attr("disabled") === undefined, true, "Email is configured, this is an EXISTING object, the reset script is available.  Button should be ENABLED.");

        EditUserView.setEmailServiceAvailable(true);
        EditUserView.data.newObject = false;
        EditUserView.setResetPasswordScriptAvailable(false);
        newEl = EditUserView.configureResetPasswordButton($('<a></a>'));
        assert.equal(newEl.prop("style").display === "none", true, "Email is configured, this is an EXISTING object, the reset script is NOT available.  Button should be HIDDEN.");

        EditUserView.setEmailServiceAvailable(false);
        EditUserView.data.newObject = false;
        EditUserView.setResetPasswordScriptAvailable(true);
        newEl = EditUserView.configureResetPasswordButton($('<a></a>'));
        assert.equal(newEl.prop("style").display === "none", false, "Email is NOT configured, this is an EXISTING object, the reset script is available. Button should be SHOWN.");
        assert.equal(newEl.attr("disabled") === undefined, false, "Email is NOT configured, this is an EXISTING object, the reset script is available.  Button should be DISABLED.");

        EditUserView.setEmailServiceAvailable(false);
        EditUserView.data.newObject = false;
        EditUserView.setResetPasswordScriptAvailable(false);
        newEl = EditUserView.configureResetPasswordButton($('<a></a>'));
        assert.equal(newEl.prop("style").display === "none", true, "Email is NOT configured, this is an EXISTING object, the reset script is NOT available. Button should be HIDDEN.");

        EditUserView.setEmailServiceAvailable(true);
        EditUserView.data.newObject = true;
        EditUserView.setResetPasswordScriptAvailable(true);
        newEl = EditUserView.configureResetPasswordButton($('<a></a>'));
        assert.equal(newEl.prop("style").display === "none", true, "Email is configured, this is a NEW object, the reset script is available. Button should be HIDDEN.");

        EditUserView.setEmailServiceAvailable(true);
        EditUserView.data.newObject = true;
        EditUserView.setResetPasswordScriptAvailable(false);
        newEl = EditUserView.configureResetPasswordButton($('<a></a>'));
        assert.equal(newEl.prop("style").display === "none", true, "Email is configured, this is a NEW object, the reset script is NOT available. Button should be HIDDEN.");

        EditUserView.setEmailServiceAvailable(false);
        EditUserView.data.newObject = true;
        EditUserView.setResetPasswordScriptAvailable(true);
        newEl = EditUserView.configureResetPasswordButton($('<a></a>'));
        assert.equal(newEl.prop("style").display === "none", true, "Email is NOT configured, this is a NEW object, the reset script is available. Button should be HIDDEN.");

        EditUserView.setEmailServiceAvailable(false);
        EditUserView.data.newObject = true;
        EditUserView.setResetPasswordScriptAvailable(false);
        newEl = EditUserView.configureResetPasswordButton($('<a></a>'));
        assert.equal(newEl.prop("style").display === "none", true, "Email is NOT configured, this is a NEW object, the reset script is NOT available. Button should be HIDDEN.");

    });
});
//# sourceMappingURL=EditUserViewTest.js.map
