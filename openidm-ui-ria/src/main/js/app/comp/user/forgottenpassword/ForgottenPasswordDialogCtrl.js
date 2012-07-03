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

/*global $, define*/

define("app/comp/user/forgottenpassword/ForgottenPasswordDialogCtrl",
        ["app/comp/user/forgottenpassword/ForgottenPasswordDialogView",
         "app/comp/user/delegates/UserDelegate",
         "app/comp/common/messages/MessagesCtrl", 
         "app/comp/user/resetpassword/ResetPasswordDialogCtrl",
         "app/util/Validator",
         "app/util/Condition",
         "app/comp/common/eventmanager/EventManager",
         "app/util/Constants",
         "app/util/Validators"], 
         function(forgottenPasswordDialogView, userDelegate, messagesCtrl, resetPasswordDialogCtrl, Validator, Condition, eventManager, constants, validators) {

    var obj = {};

    obj.view = forgottenPasswordDialogView;
    obj.delegate = userDelegate;
    obj.messages = messagesCtrl;
    obj.resetPasswordDialog = resetPasswordDialogCtrl; 
    obj.validatorsEmail = [];
    obj.validatorsQuestion = [];
    obj.userLogin = null;
    obj.loginCtrl = null;
    obj.validIdx = 0;
    obj.emailValidationOk = false;

    obj.init = function(email) {

        obj.userLogin = email;

        obj.view.show(function() {
            obj.registerValidatorsEmail();
            obj.registerValidatorsQuestion();

            obj.view.getConfirmButton().off('click').on('click', function(event) {
                event.preventDefault();
            });

            obj.view.getCloseButton().off('click').on('click', function(event) {
                obj.dispose();
            });
            obj.view.getPasswordResetLink().off('click').on('click', function(event) {
                obj.resetPasswordDialog.init();
            });

            obj.view.getEmailInput().val(obj.userLogin);

            if(obj.view.getEmailInput().val() !== null && obj.view.getEmailInput().val() !== '') {
                obj.view.getEmailInput().focus();
            }
            obj.view.getEmailInput().focus();
            obj.view.getConfirmButton().focus();
        });

    };

    obj.bindConfirmButton = function() {
        obj.view.getConfirmButton().off('click').on('click', function(event) {
            event.preventDefault();
            obj.changePassword();
        });
    };

    obj.unbindConfirmButton = function() {
        obj.view.getConfirmButton().off();
    };

    obj.changePassword = function() {
        console.log("changing password");

        obj.delegate.setNewPassword(obj.view.getEmailInput().val(), obj.view.getFgtnSecurityAnswer().val(), obj.view.getPasswordInput().val(),
                function(r) {
            eventManager.sendEvent(constants.FORGOTTEN_PASSWORD_CHANGED_SUCCESSFULLY, { userName: obj.view.getEmailInput().val(), password: obj.view.getPasswordInput().val()});
            obj.dispose();
        }, function(r) {
            obj.messages.displayMessage('error', "Unexpected error. Password hasn't been changed");
            obj.dispose();
        });
    };


    obj.registerValidatorsEmail = function() {
        console.log("registerValidatorsEmail");
        obj.validatorsEmail[0] = new Validator([obj.view.getEmailInput()], [ new Condition('email', validators.emailValidator),
                                                                                     new Condition('forgottenEmail', validators.nonuniqueEmailValidator)],
                                                                                     'focusout', 'simple', obj.validateEmailForm);

    };

    obj.registerValidatorsQuestion = function() {
        console.log("registerValidatorsQuestion");
        obj.validatorsQuestion[0] = new Validator([obj.view.getFgtnSecurityAnswer()], [new Condition('correct-answer', 
                function(inputs, self) {
            userDelegate.getBySecurityAnswer(obj.view.getEmailInput().val(),obj.view.getFgtnSecurityAnswer().val(),function() {
                self.removeError(obj.view.getFgtnSecurityAnswer());
                self.simpleRemoveError(obj.view.getFgtnSecurityAnswer());
            }, function() {
                self.simpleAddError(obj.view.getFgtnSecurityAnswer(), "Incorrect answer");
                self.addError(obj.view.getFgtnSecurityAnswer());
            });
            return "delegate";
        }
        )], 'keyup', 'simple',obj.validateQuestionForm);
        obj.validatorsQuestion[1] = new Validator([ obj.view.getPasswordInput()], [
                                                                                           new Condition('min-8', function(inputs) {
                                                                                               if (inputs[0].val().length < 8) {
                                                                                                   return "At least 8 characters length";
                                                                                               }
                                                                                           }), new Condition('one-capital', function(inputs) {
                                                                                               var reg = /[(A-Z)]+/;

                                                                                               if (!reg.test(inputs[0].val())) {
                                                                                                   return "At lest one capital letter";
                                                                                               }
                                                                                           }), new Condition('one-number', function(inputs) {
                                                                                               var reg = /[(0-9)]+/;

                                                                                               if (!reg.test(inputs[0].val())) {
                                                                                                   return "At least one number";
                                                                                               }
                                                                                           }), new Condition('not-equal-username', function(inputs) {
                                                                                               if (inputs[0].val() === "" || inputs[0].val() === obj.currentUserName) {
                                                                                                   return "Not equal username";
                                                                                               }
                                                                                           }) ], 'keyup', 'advanced', obj.validateQuestionForm);

        obj.validatorsQuestion[2] = new Validator([ obj.view.getPasswordConfirmInput(),
                                                            obj.view.getPasswordInput() ], [ new Condition('same',
                                                                    function(inputs) {
                                                                if (inputs[0].val() === "" || inputs[0].val() !== inputs[1].val()) {
                                                                    return "Passwords have to be equal.";
                                                                }
                                                            }) ], 'keyup', 'advanced', obj.validateQuestionForm);

    };

    obj.validateEmailForm = function() {
        var i, allOk = true;
        console.log('validateEmailForm of fields: '+obj.validatorsEmail.length);
        
        for (i = 0; i < obj.validatorsEmail.length; i++) {
            if (obj.validatorsEmail[i].isOk() === false) {
                allOk = false;
                console.log('email validating false on: '+i);
                break;
            }
        }

        if (allOk) {
            console.log('email validating true');
            obj.loadUser();
        } else if (!allOk) {
            obj.detachQuestionPanel();
        }
        obj.emailValidationOk = allOk;
        return allOk;
    };

    obj.validateQuestionForm = function() {
        var i, allOk = true;
        if(obj.validatorsQuestion.length > 0) {
            console.log('validateQuestionForm of fields: '+obj.validatorsQuestion.length);
            for (i = 0; i < obj.validatorsQuestion.length; i++) {
                if (obj.validatorsQuestion[i].isOk() === false) {
                    console.log('question validating false '+i);
                    allOk = false;
                    break;
                }
            }

            if (allOk && obj.emailValidationOk) {
                console.log('question validating true');
                obj.view.enableSaveButton();
                obj.bindConfirmButton();
            } else {
                console.log('question validating false');
                obj.view.disableSaveButton();
                obj.unbindConfirmButton();
            }
        }
        return allOk;
    };

    obj.detachQuestionPanel = function() {
        console.log("detaching question panel");
        obj.view.disableAnswerPanel();

        obj.validatorsQuestion[0].addError(obj.view.getPasswordInput());
        obj.validatorsQuestion[0].addError(obj.view.getPasswordConfirmInput());
        obj.validatorsQuestion[0].addError(obj.view.getFgtnSecurityAnswer());
        obj.view.disableSaveButton();
        
        obj.lastEmailQuestionPhase = null;
    };

    obj.loadUser = function() {
        console.debug("load user");
        if(obj.view.getEmailInput().val() !== null && obj.view.getEmailInput().val() !== '') {
            if(obj.view.getEmailInput().val() === obj.lastEmailQuestionPhase) {
                console.debug("Last email question phase");
                obj.delegate.getSecurityQuestionForUserName(obj.view.getEmailInput().val(), function(securityQuestion) {
                    if(!securityQuestion) {
                        console.debug("ForgottenPassword: User not found");
                    } else {
                        obj.currentSecurityQuestion = securityQuestion;
                        obj.currentUserName = obj.view.getEmailInput().val();
                        obj.showQuestionPanel();	
                    }
                }, function(r) {
                    console.error("Error during forgotten password email check");
                });
            } else {
                console.debug("Not last email question phase");
                obj.lastEmailQuestionPhase = obj.view.getEmailInput().val();
                obj.loadUser();
            }
        }
    };


    obj.showQuestionPanel = function() {
        var i;
        obj.view.getFgtnSecurityQuestion().val(obj.currentSecurityQuestion);
        $.getJSON("data/secquestions.json", function(data) {
            $.each(data, function(i,item){
                if(item.key === obj.currentSecurityQuestion) {
                    obj.view.getFgtnSecurityQuestion().text(item.value);
                }
            });
            obj.view.enableAnswerPanel();
            obj.view.getFgtnSecurityAnswer().focus();
            
            for (i = 0; i < obj.validatorsQuestion.length; i++) {
                obj.validatorsQuestion[i].validate();
            }
        });
    };

    obj.dispose = function() {
        obj.lastEmailQuestionPhase = '';
        obj.view.close();
        var cc;
        for (cc = 0; cc < obj.validatorsEmail.length; cc++) {
            obj.validatorsEmail[cc].unregister();
        }

        for (cc = 0; cc < obj.validatorsQuestion.length; cc++) {
            obj.validatorsQuestion[cc].unregister();
        }

    };

    console.log("Forgotten Created");
    return obj;

});

