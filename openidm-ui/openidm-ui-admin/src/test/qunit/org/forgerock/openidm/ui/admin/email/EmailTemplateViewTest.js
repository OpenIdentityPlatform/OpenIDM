define([
    "sinon",
    "org/forgerock/openidm/ui/admin/email/EmailTemplateView"
], function (sinon, EmailTemplateView) {
    QUnit.module('EmailTemplateView Tests');

    QUnit.test("updateCurrentConfig test", function () {
        var originalConfig =  {
                "enabled" : false,
                "from" : "originalUser@sample.com",
                "subject" : {
                    "en" : "Original subject"
                },
                "message" : {
                    "en" : "Original message"
                },
                "defaultLocale" : "en"
            },
            changedConfig = {
                "enabled" : true,
                "from" : "changedUser@sample.com",
                "subject" : {
                    "en" : "Changed subject"
                },
                "message" : {
                    "en" : "Changed message"
                },
                "defaultLocale" : "en"
            },
            getFormDataStub = sinon.stub(EmailTemplateView,"getFormData", function () {
                return {
                    "enabled" : true,
                    "from" : "changedUser@sample.com",
                    "subject" : {
                        "en" : "Changed subject"
                    }
                };
            });

        EmailTemplateView.cmBox = {
            getValue : function () {
                return "Changed message";
            }
        };

        QUnit.deepEqual(EmailTemplateView.updateCurrentConfig(originalConfig), changedConfig, "current config updated correctly with new data");
    });
});
