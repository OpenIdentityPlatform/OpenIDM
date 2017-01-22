/* global QUnit */
define([
        "org/forgerock/openidm/ui/admin/selfservice/UserRegistrationConfigView",
        "org/forgerock/openidm/ui/admin/selfservice/PasswordResetConfigView"
    ],
    function ( UserRegistrationConfigView) {

        QUnit.module("SelfService");

        QUnit.test('Toggles the email configuration warning', function(assert) {
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
            assert.equal(UserRegistrationConfigView.showHideEmailWarning(stageConfigOne, true).showWarning, false, "Email is configured with one email steps present");
            assert.equal(UserRegistrationConfigView.showHideEmailWarning(stageConfigNone, true).showWarning, false, "Email is configured with no email steps present");

            // Email is not configured, send in different arrangements of the StageConfig
            assert.equal(UserRegistrationConfigView.showHideEmailWarning(stageConfigOne, false).showWarning, true, "Email is not configured with one email step present");
            assert.equal(UserRegistrationConfigView.showHideEmailWarning(stageConfigNone, false).showWarning, false, "Email is not configured, no email steps present");
        });

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

            UserRegistrationConfigView.activateStage(false, userDetailsBlock, "idmUserDetails");

            assert.equal(userDetailsBlock.hasClass("active"), true, "User details section correctly activated");

            assert.equal(emailBlock.find(".section-check").is(':checked'), false, "Section not activated because email service not available");

            UserRegistrationConfigView.activateStage(true, emailBlock, "emailValidation");

            assert.equal(emailBlock.find(".section-check").is(':checked'), true, "Section activated because email service available");

            UserRegistrationConfigView.activateStage(false, fakeBlock, "fake");

            assert.equal(fakeBlock.find(".section-check").is(':checked'), true, "None email section enabled with email service off");
        });
    });
