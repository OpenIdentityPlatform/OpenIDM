define([
    "org/forgerock/openidm/ui/admin/email/EmailProviderConfigView"
], function (EmailConfigView) {
    QUnit.module('EmailConfigView Tests');

    QUnit.test("Find if password exists in email", function () {
        var testConfig = {

            },
            foundPassword;

        foundPassword = EmailConfigView.findPassword(testConfig);

        QUnit.equal(foundPassword, null, "No password found in config");

        testConfig = {
            "auth" : {
                "password" : "test"
            }
        };

        foundPassword = EmailConfigView.findPassword(testConfig);

        QUnit.equal(foundPassword, "test", "Password found in config");
    });

    QUnit.test("Find if password exists in email", function () {
        var testConfig = {
                "auth" : {
                    "password" : ""
                },
                "starttls" : {

                }
            },
            password = "test",
            formData = {
                "starttls" : {
                    "enable" : true
                },
                "auth" : {
                    "enable" : true
                }
            },
            cleanConfig = {};

        cleanConfig = EmailConfigView.cleanSaveData(testConfig, formData, password);

        QUnit.equal(cleanConfig.auth.password, "test", "Password correctly set");

        formData = {
            "auth" : {
                "enable" : true
            }
        };

        cleanConfig = EmailConfigView.cleanSaveData(testConfig, formData, password);

        QUnit.equal(cleanConfig.starttls, undefined, "Correctly removed starttls details");

        formData = {
            "auth" : {}
        };

        cleanConfig = EmailConfigView.cleanSaveData(testConfig, formData, password);

        QUnit.equal(cleanConfig.auth, undefined, "Correctly removed auth details");
    });
});
