/* global QUnit */
define([
        "org/forgerock/openidm/ui/admin/selfservice/UserRegistrationConfigView",
        "org/forgerock/openidm/ui/admin/selfservice/PasswordResetConfigView"
    ],
    function ( UserRegistrationConfigView,
               PasswordResetConfigView) {

        QUnit.module("SelfService");

        QUnit.test('Toggles the email configuration warning', function(assert) {
            var EMAIL_STEPS = ["emailUsername", "emailValidation"];

            var stageConfigBoth = [
                {"name": "userQuery"},
                {"name": "kbaInfo"},
                {"name": "emailValidation"},
                {"name": "resetStage"},
                {"name": "emailUsername"}
            ];

            var stageConfigOne = [
                {"name": "userQuery"},
                {"name": "kbaInfo"},
                {"name": "emailValidation"},
                {"name": "resetStage"}
            ];

            var stageConfigNone = [
                {"name": "userQuery"},
                {"name": "kbaInfo"},
                {"name": "resetStage"}
            ];

            // Email is configured, send in different arrangements of the StageConfig
            assert.equal(UserRegistrationConfigView.showHideEmailWarning(stageConfigBoth, EMAIL_STEPS, true).showWarning, false, "Email is configured with two email steps present");
            assert.equal(UserRegistrationConfigView.showHideEmailWarning(stageConfigOne, EMAIL_STEPS, true).showWarning, false, "Email is configured with one email steps present");
            assert.equal(UserRegistrationConfigView.showHideEmailWarning(stageConfigNone, EMAIL_STEPS, true).showWarning, false, "Email is configured with no email steps present");

            // Email is not configured, send in different arrangements of the StageConfig
            assert.equal(UserRegistrationConfigView.showHideEmailWarning(stageConfigBoth, EMAIL_STEPS, false).showWarning, true, "Email is not configured with two email steps present");
            assert.equal(UserRegistrationConfigView.showHideEmailWarning(stageConfigOne, EMAIL_STEPS, false).showWarning, true, "Email is not configured with one email step present");
            assert.equal(UserRegistrationConfigView.showHideEmailWarning(stageConfigNone, EMAIL_STEPS, false).showWarning, false, "Email is not configured, no email steps present");
        });

        QUnit.test('Properties list filtered properly', function() {
            var props = ['password', 'notpasssword'],
                type = "resetStage",
                details = {
                    password : {
                        encryption: {

                        }
                    },
                    notpassword : {
                        notencryption: {

                        }
                    }
                },
                tempProps;

            tempProps = PasswordResetConfigView.filterPropertiesList(props, type, details);

            QUnit.equal(tempProps.length, 1, "Non-password properties are filtered out");
        });
    });
