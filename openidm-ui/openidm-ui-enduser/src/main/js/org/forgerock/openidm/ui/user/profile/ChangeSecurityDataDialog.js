/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/*global define, $, _, ContentFlow */

/**
 * @author mbilski
 */
define("org/forgerock/openidm/ui/user/profile/ChangeSecurityDataDialog", [
    "org/forgerock/commons/ui/common/components/Dialog",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "UserDelegate",
    "AuthnDelegate",
    "org/forgerock/openidm/ui/common/delegates/InternalUserDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/user/delegates/SecurityQuestionDelegate"
], function(Dialog, validatorsManager, conf, userDelegate, authnDelegate, internalUserDelegate, uiUtils, eventManager, constants, securityQuestionDelegate) {
    var ChangeSecurityDataDialog = Dialog.extend({    
        contentTemplate: "templates/user/ChangeSecurityDataDialogTemplate.html",

        events: {
            "click input[type=submit]": "formSubmit",
            "onValidate": "onValidate",
            "customValidate": "customValidate",
            "click .dialogCloseCross img": "close",
            "click input[name='close']": "close",
            "click .dialogContainer": "stop",
            "check_reauth": "reauth"
        },
        reauth: function(event, propertyName){
            // we only need to force re-authentication if the properties needing it are one of the two we are prepared to change
            if (propertyName === "password" || (conf.globalData.hasOwnProperty("securityQuestions") && propertyName === "securityAnswer")) {
                if (!conf.hasOwnProperty('passwords')) {
                    this.reauth_required = true;
                    eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "enterOldPassword"});
                } else {
                    this.reauth_required = false;
                }
            }
        },
        
        formSubmit: function(event) {
            event.preventDefault();
            
            var patchDefinitionObject = [], element;
            
            if(validatorsManager.formValidated(this.$el.find("#passwordChange"))) {            
                patchDefinitionObject.push({operation: "replace", field: "password", value: this.$el.find("input[name=password]").val()});
            }
    
            if(validatorsManager.formValidated(this.$el.find("#securityDataChange"))) {            
                patchDefinitionObject.push({operation: "replace", field: "securityQuestion", value: this.$el.find("select[name=securityQuestion]").val()});
                patchDefinitionObject.push({operation: "replace", field: "securityAnswer", value: this.$el.find("input[name=securityAnswer]").val()});
            }
            
            this.delegate.patchSelectedUserAttributes(conf.loggedUser._id, conf.loggedUser._rev, patchDefinitionObject, _.bind(function(r) {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "securityDataChanged");
                delete conf.passwords;
                this.close();

                return authnDelegate.getProfile()
                    .then(function(user) {
                        conf.loggedUser = user;
                        return user;
                    });

            }, this));
        },
        customValidate: function () {

            if(validatorsManager.formValidated(this.$el.find("#passwordChange")) || validatorsManager.formValidated(this.$el.find("#securityDataChange"))) {
                this.$el.find("input[type=submit]").prop('disabled', false);
            }
            else {
                this.$el.find("input[type=submit]").prop('disabled', true);
            }
            
        },
        render: function() {
            this.actions = [];
            this.addAction($.t("common.form.update"), "submit");

            this.delegate = conf.globalData.userComponent === "repo/internal/user" ? internalUserDelegate : userDelegate;

            $("#dialogs").hide();
            this.show(_.bind(function() {
                validatorsManager.bindValidators(this.$el, this.delegate.baseEntity + "/" + conf.loggedUser._id, _.bind(function () {
                    $("#dialogs").show();
                    if (!this.reauth_required) {
                        this.reloadData();
                    }
                }, this));
                
            }, this));
            
            this.$el.find("input[type=submit]").prop('disabled', true);
        },
        
        reloadData: function() {
            var user = conf.loggedUser, self = this;
            this.$el.find("input[name=_id]").val(conf.loggedUser._id);
            
            if (conf.globalData.securityQuestions) {
                securityQuestionDelegate.getAllSecurityQuestions(function(secquestions) {
                    uiUtils.loadSelectOptions(secquestions, self.$el.find("select[name='securityQuestion']"), 
                        false, _.bind(function() {
                            this.$el.find("select[name='securityQuestion']").val(user.securityQuestion);                
                            this.$el.find("input[name=oldSecurityQuestion]").val(user.securityQuestion);                
                        validatorsManager.validateAllFields(this.$el);
                    }, self));
                });
            }
            
            this.$el.find("select[name=securityQuestion]").on('change', _.bind(function() {
                this.$el.find("input[name=securityAnswer]").trigger('change');
            }, this));
        }
    }); 
    
    return new ChangeSecurityDataDialog();
});