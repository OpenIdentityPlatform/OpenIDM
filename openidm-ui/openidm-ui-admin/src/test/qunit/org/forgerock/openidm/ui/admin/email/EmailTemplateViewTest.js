define([
    "lodash",
    "sinon",
    "org/forgerock/openidm/ui/admin/email/EmailTemplateView"
], function (_, sinon, EmailTemplateView) {
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

    QUnit.test("generateSampleData test", function () {
        var hasAllProperties = true,
            propertiesList = [
                {
                    "propName": "userName"
                },
                {
                    "propName": "description"
                },
                {
                    "propName": "mail"
                }],
            sampleData = EmailTemplateView.generateSampleData(propertiesList);

        /**
        Check to make sure all properties in the properties list are represented
        in the sampleData object. Doing this and not checking equality because the
        actual property values returned with the sampleData object are random
        **/
        hasAllProperties = _.has(sampleData,"userName") && _.has(sampleData,"description") && _.has(sampleData,"mail");

        QUnit.ok(hasAllProperties, "sample data is an object with the correct properties");
    });

    QUnit.test("getPropertiesList test", function () {
        var schema = {
                "title": "User",
                "properties": {
                    "_id": {
                        "type": "string"
                    },
                    "userName": {
                        "type": "string"
                    },
                    "password": {
                        "type": "string",
                        "encryption": {
                            "key": "openidm-sym-default"
                        }
                    },
                    "description": {
                        "type": "string"
                    },
                    "mail": {
                        "type": "string"
                    },
                    "manager": {
                        "type": "relationship"
                    },
                    "authzRoles": {
                        "type": "array"
                    }
                },
                "order": [
                    "_id",
                    "userName",
                    "password",
                    "description",
                    "mail",
                    "manager",
                    "authzRoles"
                ]
            },
            expectedResult = [
                {
                    "propName": "userName",
                    "type": "string"
                },
                {
                    "propName": "description",
                    "type": "string"
                },
                {
                    "propName": "mail",
                    "type": "string"
                }
            ];

        QUnit.deepEqual(EmailTemplateView.getPropertiesList(schema),expectedResult,"correct properties list is generated from schema definition");
    });
});
