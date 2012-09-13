/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2012 ForgeRock AS. All rights reserved.
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

/*global $, _, define*/

define("org/forgerock/openidm/ui/user/ForgottenPasswordDialog", [
    "org/forgerock/openidm/ui/common/components/Dialog",
    "org/forgerock/openidm/ui/common/main/ValidatorsManager",
    "org/forgerock/openidm/ui/user/delegates/UserDelegate",
    "org/forgerock/openidm/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/main/Configuration"
], function(Dialog, validatorsManager, userDelegate, eventManager, constants, conf) {
    var ForgottenPasswordDialog = Dialog.extend({    
        contentTemplate: "templates/user/ForgottenPasswordTemplate.html",
        baseTemplate: "templates/user/LoginBaseTemplate.html",
        
        events: {
            "click input[name=Update]": "formSubmit",
            "click .dialogCloseCross img": "close",
            "click input[name='close']": "close",
            "click": "close",
            "click .dialogContainer": "stop",
            "onValidate": "onValidate"
        },
        
        data: {         
            width: 800,
            height: 210
        },
        
        securityQuestions: {},
        
        render: function() {
            var securityQuestionRef;
            this.securityQuestions = {};
            this.actions = {};
            this.addAction("Update", "submit");
            this.show(_.bind(function() {
                validatorsManager.bindValidators(this.$el); 
                if (conf.forgottenPasswordUserName) {
                    this.$el.find("input[name=resetEmail]").val(conf.forgottenPasswordUserName);
                    this.$el.find("input[name=resetEmail]").trigger("change");
                    delete conf.forgottenPasswordUserName;
                }
                this.data.height = 210;
                this.resize();
            }, this));
            
            securityQuestionRef = this.securityQuestions;
            $.getJSON("data/secquestions.json", function(data) {
                $.each(data, function(i,item){
                    securityQuestionRef[item.key] = item.value;
                });
            });
        },
        
        formSubmit: function(event) {
            if (validatorsManager.formValidated(this.$el)) {
                this.changePassword();
            } else {
                var errorMessage = this.$el.find("input[name=resetEmail][data-validation-status=error]"),userName = this.$el.find("input[name=resetEmail]").val(), securityQuestionRef;
                if (errorMessage.length !== 0) {
                    this.$el.find("#fgtnAnswerDiv").hide();
                    this.$el.find("input[name=fgtnSecurityAnswer]").val("");
                    this.$el.find("input[name=password]").val("");
                    this.$el.find("input[name=passwordConfirm]").val("");
                    this.data.height = 210;
                } else {
                    securityQuestionRef = this.securityQuestions;
                    userDelegate.getSecurityQuestionForUserName(userName,
                            function(result) {
                                $("#fgtnSecurityQuestion").text(securityQuestionRef[result]);
                            });
                    this.$el.find("#fgtnAnswerDiv").show();
                    this.data.height = 350;
                }
                this.resize();
            }
        },
        
        changePassword: function() {
            var dialog = this.close(), userName = this.$el.find("input[name=resetEmail]").val(), securityAnswer = this.$el.find("input[name=fgtnSecurityAnswer]").val(), newPassword = this.$el.find("input[name=password]").val();
            console.log("changing password");
            
            userDelegate.setNewPassword(userName, securityAnswer, newPassword,
                    function(r) {
                eventManager.sendEvent(constants.FORGOTTEN_PASSWORD_CHANGED_SUCCESSFULLY, { userName: userName, password: newPassword});
                dialog.close();
            }, function(r) {
                eventManager.sendEvent(constants.EVENT_USER_PROFILE_UPDATE_FAILED);
            });
        }
    }); 
    
    return new ForgottenPasswordDialog();
});

