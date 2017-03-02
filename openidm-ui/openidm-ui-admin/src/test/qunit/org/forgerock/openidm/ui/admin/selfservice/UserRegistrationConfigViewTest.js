/* global QUnit */
define([
        "org/forgerock/openidm/ui/admin/selfservice/UserRegistrationConfigView",
        "lodash"
    ],
    function ( UserRegistrationConfigView, _) {

        QUnit.module("SelfService");

        QUnit.test('Correct position placement for newly added stage', function(assert) {
            var stages = [
                {
                    type: "captcha",
                    toggledOn: false
                },
                {
                    type: "userDetails",
                    toggledOn: true
                },
                {
                    type: "emailValidation",
                    toggledOn: true
                },
                {
                    type: "kbaSecurityAnswerDefinitionStage",
                    toggledOn: false
                },
                {
                    "name" : "selfRegistration",
                    toggledOn: true
                }
            ];

            assert.equal(UserRegistrationConfigView.findPosition(stages, "emailValidation"), 1, "Email Validation stage correct order found");

            stages[0].toggledOn = true;

            assert.equal(UserRegistrationConfigView.findPosition(stages, "emailValidation"), 2, "Email Validation stage correct order found after turning on captcha");
        });

        QUnit.test('Turning on a stage', function(assert) {
            var stages = [
                {
                    "name": "idmUserDetails",
                    "socialRegistrationEnabled" : true
                },
                {
                    "name": "emailValidation"
                },
                {
                    "name": "kbaSecurityAnswerDefinitionStage"
                },
                {
                    "name" : "selfRegistration"
                }
            ],
            defaultStages = [
                {
                    "name": "captcha"
                },
                {
                    "name": "idmUserDetails",
                    "socialRegistrationEnabled" : false
                },
                {
                    "name": "emailValidation"
                },
                {
                    "name": "kbaSecurityAnswerDefinitionStage"
                },
                {
                    "name" : "selfRegistration"
                }
            ],
            config = [
                {
                    type: "captcha",
                    toggledOn: false
                },
                {
                    type: "idmUserDetails",
                    toggledOn: true
                },
                {
                    type: "emailValidation",
                    toggledOn: true
                },
                {
                    type: "kbaSecurityAnswerDefinitionStage",
                    toggledOn: false
                },
                {
                    "name" : "selfRegistration",
                    toggledOn: true
                }
            ],
            testStages;

            testStages = UserRegistrationConfigView.setSwitchOn($("<div></div>"), stages, config, defaultStages, "captcha");

            assert.equal(testStages.length, 5, "Captcha stage turned on and added to staging list");
            assert.equal(config[0].toggledOn, true, "Config list toggle status turned on");

            testStages = UserRegistrationConfigView.setSwitchOn($("<div></div>"), stages, config, defaultStages, "socialUserDetails");

            assert.equal(testStages[1].socialRegistrationEnabled, true, "Correctly change userDetails stage to enable social");
        });

        QUnit.test('Turning off a stage', function(assert) {
            var stages = [
                    {
                        "name": "captcha"
                    },
                    {
                        "name": "idmUserDetails",
                        "socialRegistrationEnabled" : true
                    },
                    {
                        "name": "emailValidation"
                    },
                    {
                        "name": "kbaSecurityAnswerDefinitionStage"
                    },
                    {
                        "name" : "selfRegistration"
                    }
                ],
                config = [
                    {
                        type: "captcha",
                        toggledOn: true
                    },
                    {
                        type: "userDetails",
                        toggledOn: true
                    },
                    {
                        type: "emailValidation",
                        toggledOn: true
                    },
                    {
                        type: "kbaSecurityAnswerDefinitionStage",
                        toggledOn: false
                    },
                    {
                        "name" : "selfRegistration",
                        toggledOn: true
                    }
                ],
                testStages;

            testStages = UserRegistrationConfigView.setSwitchOff($("<div></div>"), stages, config, "captcha");

            assert.equal(testStages.length, 4, "Captcha stage turned off and removed from staging list");
            assert.equal(config[0].toggledOn, false, "Config list toggle status turned off");

            testStages = UserRegistrationConfigView.setSwitchOff($("<div></div>"), stages, config, "socialUserDetails");

            assert.equal(testStages[1].socialRegistrationEnabled, false, "Correctly turn off social providers for registration");
        });

        QUnit.test('Extract stage details from html card', function(assert) {
            var cardDetails = UserRegistrationConfigView.getCardDetails($("<div data-editable='true' data-type='userDetails'></div>"));

            assert.equal(cardDetails.type, "userDetails", "Type correctly pulled from html card");
            assert.equal(cardDetails.editable, "true", "Editable state correctly pulled from html card");
            assert.equal(cardDetails.disabled, false, "Disabled state correctly pulled from html card");
        });

        QUnit.test('Correctly enable sections in user registration', function(assert) {
            var userDetailsBlock = $("<div class='disabled' data-editable='true' data-type='idmUserDetails'></div>"),
                emailBlock = $("<div data-editable='true' data-type='emailValidation'><input class='section-check' type='checkbox'></div>"),
                fakeBlock = $("<div data-editable='true' data-type='fake'><input class='section-check' type='checkbox'></div>");

            UserRegistrationConfigView.activateStage(false, emailBlock, "emailValidation");

            assert.equal(emailBlock.find(".section-check").is(':checked'), false, "Section not activated because email service not available");

            UserRegistrationConfigView.activateStage(true, emailBlock, "emailValidation");

            assert.equal(emailBlock.find(".section-check").is(':checked'), true, "Section activated because email service available");

            UserRegistrationConfigView.activateStage(false, fakeBlock, "fake");

            assert.equal(fakeBlock.find(".section-check").is(':checked'), true, "None email section enabled with email service off");
        });

        QUnit.test('Find correct managed object', function(assert) {
            var managedList = [
                    {
                        "name" : "test1"
                    },
                    {
                        "name" : "test2"
                    }
                ],
                managed,
                system;

            managed = UserRegistrationConfigView.findManagedSchema(managedList, "managed/test1");

            assert.equal(managed.name, "test1", "Correctly found managed object");

            system = UserRegistrationConfigView.findManagedSchema(managedList, "system/fake/fake");

            assert.equal(_.isUndefined(system.schema), false, "Correctly found system object");
        });

        QUnit.test('Generate grid and select list', function(assert) {
            var managedList = {
                    "test1" : {
                        "type": "string",
                        "userEditable": true
                    },
                    "test2" : {
                        "type": "string",
                        "userEditable": true
                    },
                    "test3" : {
                        "type": "string",
                        "userEditable": true
                    },
                    "_id" : {
                        "type": "string",
                        "userEditable": true
                    },
                    "notstring" : {
                        "type" : "object",
                        "userEditable": true
                    }
                },
                registrationFields = ["test1", "test3"],
                lists;

            lists = UserRegistrationConfigView.generatRegistrationLists(managedList, registrationFields);

            assert.equal(lists.gridItems.length, 2, "Correctly returned safe grid properties");
            assert.equal(lists.listItems.length, 1, "Correctly returned safe list properties");
        });
    });
