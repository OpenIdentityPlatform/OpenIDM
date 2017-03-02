/**
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
 * Copyright 2015-2016 ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "form2js",
    "handlebars",
    "org/forgerock/commons/ui/user/anonymousProcess/AnonymousProcessView",
    "org/forgerock/commons/ui/common/util/OAuth",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/user/anonymousProcess/SelfRegistrationView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/common/util/Constants",
    "org/forgerock/commons/ui/user/anonymousProcess/KBAView"
], function($, _, form2js, Handlebars,
            AnonymousProcessView,
            OAuth,
            Router,
            CommonSelfRegistrationView,
            ValidatorsManager,
            UIUtils,
            Configuration,
            EventManager,
            Constants,
            KBAView) {

    var SelfRegistrationView = AnonymousProcessView.extend({
        baseEntity: "selfservice/registration",
        partials: [
            "partials/process/_coreProfileDetails.html",
            "partials/profile/_multiValueFormFields.html",
            "partials/profile/_emailEntry.html",
            "partials/profile/_preferences.html",
            "partials/providers/_providerButton.html"
        ],
        events: _.extend({
            "click [data-oauth=button]": "oauthHandler",
            "focus .float-label input": "addFloatLabelStyles",
            "blur .float-label": "removeFloatLabelStyles",
            "click #termsAndServiceDisplay" : "openTermsAndService",
            "change #recaptchaResponse" : "recaptchaSet"
        }, CommonSelfRegistrationView.events),
        model: {
            recaptchaPassed : false,
            stagesActive : {
                "kbaSecurityAnswerDefinitionStage" : false,
                "idmUserDetails" : false,
                "captcha" : false,
                "termsAndConditions" : false
            },
            allInOneActive: false
        },
        oauthHandler: function (e) {
            e.preventDefault();
            window.location.href = OAuth.getRequestURL(
                $(e.target).parents("[data-oauth=button]").attr("authorization_endpoint"),
                $(e.target).parents("[data-oauth=button]").attr("client_id"),
                $(e.target).parents("[data-oauth=button]").attr("scope"),
                Router.getLink(Router.configuration.routes.login,
                    [
                        "&provider=" +  $(e.target).parents("[data-oauth=button]").attr("value") +
                        "&redirect_uri=" + OAuth.getRedirectURI() +
                        "&gotoURL=" + (Configuration.gotoURL || "#")
                    ]
                )
            );
        },

        /**
         Intercept the request to the backend to inject the nonce taken from session storage,
         when appropriate
         */
        submitDelegate: function (params, onSubmit) {
            if (params.provider && params.code && params.redirect_uri) {
                params = _.extend({
                    nonce: OAuth.getCurrentNonce()
                }, params);
            }

            CommonSelfRegistrationView.submitDelegate.call(this, params, onSubmit);
        },

        getFormContent: function () {
            var form = $(this.element).find("form")[0],
                tempForm;

            if (form.hasAttribute("data-kba-questions")) {
                return { "kba": KBAView.getQuestions() };
            } else if (this.model.allInOneActive && this.model.stagesActive.kbaSecurityAnswerDefinitionStage) {
                tempForm = form2js(form);

                _.forEach(tempForm, function(value, key) {
                    if(_.startsWith(key, "answer_") || _.startsWith(key, "question_")) {
                        delete tempForm[key];
                    }
                });

                tempForm.kba = KBAView.getQuestions();

                return tempForm;
            } else {
                return form2js(form);
            }
        },

        addFloatLabelStyles: function(e) {
            if (!$(e.target).attr("readonly")) {
                $(e.target).removeClass("input-lg");
                $(e.target).prev().removeClass("sr-only");
                $(e.target).parent().addClass("float-label-with-focus");
            }
        },

        removeFloatLabelStyles: function(e) {
            if (!$(e.target).val()) {
                $(e.target).addClass("input-lg");
                $(e.target).prev().addClass("sr-only");
                $(e.target).parent().removeClass("float-label-with-focus");
            }
        },

        openTermsAndService: function(e) {
            e.preventDefault();

            UIUtils.confirmDialog(this.model.termsOfService, "default", _.noop, { "title" :  $.t("common.user.termsAndConditions.title")});
        },

        recaptchaSet : function(e) {
            e.preventDefault();

            this.model.recaptchaPassed = true;

            ValidatorsManager.validateAllFields(this.$el);
        },

        validationSuccessful: function (event) {
            AnonymousProcessView.prototype.validationSuccessful(event);

            if(this.model.stagesActive.captcha) {
                if(!this.model.recaptchaPassed) {
                    this.$el.find(".recaptcha-wrapper").attr("data-validation-status", "error");
                } else {
                    this.$el.find(".recaptcha-wrapper").attr("data-validation-status", "true");
                }
            }
        },

        attemptCustomTemplate: function(stateData, baseTemplateUrl, response, processStatePromise) {
            var templateUrl = baseTemplateUrl + this.processType
                + "/" + response.type + "-" + response.tag + ".html",
                type = {
                    "action": $.t("templates.socialIdentities.register")
                };

            //Takes an object and a key and returns the value
            Handlebars.registerHelper("findDynamicValue", function(map, key){
                var value = "";

                if(!_.isUndefined(map) && !_.isUndefined(key)) {
                    value = map[key];

                    if(_.isUndefined(value)) {
                        value = "";
                    }
                }

                return value;
            });

            //Takes an object and a key and finds if that key exists in the object
            Handlebars.registerHelper("dynamicValueExist", function(map, key, options){
                var value = false;

                if(!_.isUndefined(map) && !_.isUndefined(key)) {
                    value = !_.isUndefined(map[key]);
                }

                if(value) {
                    return options.fn(this);
                } else {
                    return options.inverse(this);
                }
            });

            //Takes an object and a key and finds if that key doesn't exist in the object
            Handlebars.registerHelper("dynamicValueNotExist", function(map, key, options){
                var value = true;

                if(!_.isUndefined(map) && !_.isUndefined(key)) {
                    value = _.isUndefined(map[key]);
                }

                if(value) {
                    return options.fn(this);
                } else {
                    return options.inverse(this);
                }
            });

            if (_.has(stateData, "requirements.definitions.providers.items.oneOf")) {
                _.each(stateData.requirements.definitions.providers.items.oneOf, (provider) => {
                    provider.icon =  Handlebars.compile(provider.icon)(type);
                });
            }

            if(stateData.additions && stateData.additions.credentialJwt) {
                EventManager.sendEvent(Constants.EVENT_LOGIN_REQUEST, {
                    jwt: stateData.additions.credentialJwt
                });
            } else if (stateData.additions && stateData.additions.id_token){
                EventManager.sendEvent(Constants.EVENT_LOGIN_REQUEST, {
                    idToken: stateData.additions.id_token,
                    provider: stateData.additions.provider,
                    suppressMessage: false
                });
            } else {
                if(stateData.requirements) {
                    _.each(stateData.requirements.stages, (stage) => {
                        this.model.stagesActive[stage] = true;
                    });
                }

                stateData.activeStages = this.model.stagesActive;

                UIUtils.compileTemplate(templateUrl, stateData)
                .then((renderedTemplate) => {
                    processStatePromise.resolve(renderedTemplate);

                    if(response.type === "allInOneRegistration") {
                        this.model.allInOneActive = true;

                        if(this.model.stagesActive.kbaSecurityAnswerDefinitionStage && stateData.requirements.properties.kba) {
                            KBAView.render(stateData.requirements.properties.kba);
                        }

                        if(this.model.stagesActive.termsAndConditions) {
                            this.model.termsOfService = stateData.requirements.terms;
                        }
                    }
                }, _.bind(function () {
                    this.loadGenericTemplate(stateData, baseTemplateUrl, response, processStatePromise);
                }, this));
            }
        }
    });

    SelfRegistrationView.prototype = _.extend(Object.create(CommonSelfRegistrationView), SelfRegistrationView.prototype);

    return new SelfRegistrationView();
});
