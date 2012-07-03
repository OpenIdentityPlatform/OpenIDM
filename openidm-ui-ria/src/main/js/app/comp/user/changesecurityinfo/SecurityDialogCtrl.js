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

define("app/comp/user/changesecurityinfo/SecurityDialogCtrl",
        ["app/comp/user/changesecurityinfo/SecurityDialogView",
         "app/comp/user/delegates/UserDelegate",
         "app/comp/common/messages/MessagesCtrl", 
         "app/util/Validator",
         "app/util/Condition",
         "app/comp/common/dialog/AbstractDialogController",
         "app/util/Validators"], 
         function(securityDialogView, userDelegate, messagesCtrl, Validator, Condition, AbstractDialogController, validators) {
    var obj = new AbstractDialogController(securityDialogView);

    obj.delegate = userDelegate;
    obj.messages = messagesCtrl;

    obj.passwordValidators = [];
    obj.passwordIsOk = false;
    obj.passwordEmpty = true;
    obj.questionValidators = [];
    obj.questionIsOk = false;
    obj.questionEmpty = true;

    obj.userName = null;
    //TODO clear sensitive data after dialog close
    obj.oldPassword = null;

    obj.mode = null;	

    obj.init = function(userName, mode) {
        obj.userName = userName;

        obj.passwordValidators = [];
        obj.passwordIsOk = false;
        obj.passwordEmpty = true;

        obj.questionValidators = [];
        obj.questionEmpty = true;
        obj.questionIsOk = false;

        if( mode !== undefined && mode === 'admin' ) {
            obj.mode = 'admin';
        } else {
            obj.mode = 'user';
        }

        obj.view.showLess();

        obj.show(function() {
            obj.registerPasswordValidators();
            obj.registerSecurityValidators();

            obj.view.getCloseButton().on('click', function(event) {
                obj.view.close();
            });

            obj.view.getSaveButton().on('click', function(event) {
                event.preventDefault();

                obj.afterSaveButtonClicked();
            });

            obj.setSecurityQuestionSelect("");
        }, obj.mode);
    };

    obj.getOldPassword = function() {
        return obj.oldPassword;		
    };

    obj.registerPasswordValidators = function() {
        console.log("register dialog validators");

        obj.view.getOldPasswordInput().off().on('focusout', function() {
            userDelegate.checkCredentials(obj.userName, obj.view.getOldPasswordInput().val(), function() {
                obj.view.showMore();
                obj.oldPassword = obj.view.getOldPasswordInput().val();
            });
        });		

        obj.passwordValidators[1] = new Validator([ obj.view.getPasswordConfirmInput(),
                                                            obj.view.getPasswordInput() ], [ new Condition('same',
                                                                    function(inputs) {
                                                                if (inputs[0].val() === "" || inputs[0].val() !== inputs[1].val()) {
                                                                    return "Passwords have to be equal.";
                                                                }
                                                            }) ], 'keyup', 'advanced', obj.validatePasswordForm);

        if( obj.mode === 'user' ) {
            obj.passwordValidators[0] = new Validator([ obj.view.getPasswordInput() ], [
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
                                                                                                        return "At lest one number";
                                                                                                    }
                                                                                                }), new Condition('new', function(inputs) {
                                                                                                    if (inputs[0].val() === "" || inputs[0].val() === obj.getOldPassword()) {
                                                                                                        return "Not equal old password";
                                                                                                    }
                                                                                                }), new Condition('not-equal-username', function(inputs) {
                                                                                                    if (inputs[0].val() === "" || inputs[0].val() === obj.userName) {
                                                                                                        return "Not equal username";
                                                                                                    }
                                                                                                }) ], 'keyup', 'advanced', obj.validatePasswordForm);
        } else {
            obj.passwordValidators[0] = new Validator([ obj.view.getPasswordInput() ], [
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
                                                                                                        return "At lest one number";
                                                                                                    }
                                                                                                }), new Condition('not-equal-username', function(inputs) {
                                                                                                    if (inputs[0].val() === "" || inputs[0].val() === obj.userName) {
                                                                                                        return "Not equal username";
                                                                                                    }
                                                                                                }) ], 'keyup', 'advanced', obj.validatePasswordForm);
        }

        obj.view.getPasswordInput().on('keyup', function(){ obj.passwordEmptyValidator(); });
        obj.view.getPasswordConfirmInput().on('keyup', function(){ obj.passwordEmptyValidator(); });
    };

    obj.passwordEmptyValidator = function() {
        if( obj.view.getPasswordInput().val() === "" && obj.view.getPasswordConfirmInput().val() === "" ) {
            console.log('EMPTY');

            obj.passwordIsOk = false;
            obj.passwordEmpty = true;

            obj.passwordValidators[0].clear(obj.view.getPasswordInput());
            obj.passwordValidators[1].clear(obj.view.getPasswordConfirmInput());

            if( obj.questionIsOk === true) {
                obj.view.enableSaveButton();
            }
        }
    };

    obj.questionEmptyValidator = function() {
        if( obj.view.getAnswer().val() === "" && obj.view.getQuestion().val() === "" ) {
            console.log('EMPTY');

            obj.questionIsOk = false;
            obj.passwordEmpty = true;

            obj.questionValidators[0].clear(obj.view.getAnswer());
            obj.questionValidators[1].clear(obj.view.getQuestion());

            if( obj.passwordIsOk === true ) {
                obj.view.enableSaveButton();
            }
        }
    };

    obj.registerSecurityValidators = function() {
        console.log("register dialog validators");

        obj.questionValidators[0] = new Validator([obj.view.getAnswer()], [new Condition('not-empty', validators.notEmptyValidator)], 
                'change', 'simple', obj.validateQuestionForm);

        obj.questionValidators[1] = new Validator([obj.view.getQuestion()], [new Condition('not-empty', validators.notEmptyValidator)], 
                'change', 'simple', obj.validateQuestionForm);

        obj.view.getAnswer().on('change', function(){obj.questionEmptyValidator(); });
        obj.view.getQuestion().on('keyup', function(){obj.questionEmptyValidator(); });
    };

    obj.setSecurityQuestionSelect = function(question) {
        $.ajax({
            type : "GET",
            url : "data/secquestions.json",
            dataType : "json",
            success : function(data) {
                obj.view.getQuestion().loadSelect(data);
                obj.view.getQuestion().val(question);

            },
            error : function(xhr) {
                console.log('Error: ' + xhr.status + ' ' + xhr.statusText);
            }
        });
    };

    obj.validatePasswordForm = function() {
        var i, allOk = true;
        
        console.log('validate pass form');

        for (i = 0; i < obj.passwordValidators.length; i++) {
            if (obj.passwordValidators[i].isOk() === false) {
                allOk = false;
                break;
            }
        }

        if (allOk) {
            if( obj.questionIsOk === true || obj.questionEmpty === true) {
                obj.view.enableSaveButton();
            }

            obj.passwordIsOk = true;
        } else if (!allOk) {
            obj.view.disableSaveButton();			
            obj.passwordIsOk = false;
        }

        return allOk;
    };

    obj.validateQuestionForm = function() {
        console.log('validate question form');

        var i, allOk = true;

        for (i = 0; i < obj.questionValidators.length; i++) {
            if (obj.questionValidators[i].isOk() === false) {
                allOk = false;
                break;
            }
        }

        if (allOk) {
            if( obj.passwordIsOk === true || obj.passwordEmpty === true) {
                obj.view.enableSaveButton();
            }

            obj.questionIsOk = true;
        } else if (!allOk) {
            obj.view.disableSaveButton();		
            obj.questionIsOk = false;
        }

        return allOk;
    };

    obj.afterSaveButtonClicked = function() {
        var patchDefinitionObject = [], flds = [], vals = [];

        if(obj.questionIsOk === true) {
            patchDefinitionObject.push({replace: "securityquestion", value: obj.view.getQuestion().val()});
            patchDefinitionObject.push({replace: "securityanswer", value: obj.view.getAnswer().val().toLowerCase()});
        }

        if(obj.passwordIsOk === true) {
            patchDefinitionObject.push({replace: "password", value: obj.view.getPasswordInput().val()});
        }

        if( obj.questionIsOk === true || obj.passwordIsOk === true ) {
            obj.delegate.patchSelectedUserAttributes(obj.userName, patchDefinitionObject,	function(r) {
                if(obj.questionIsOk === true) {
                    obj.messages.displayMessage('info','Security question has been changed');
                }

                if(obj.passwordIsOk === true) {
                    obj.messages.displayMessage('info','Password has been changed');
                }			

                obj.view.close();
            }, function(r) {
                obj.messages.displayMessage('error', 'Unknown error');
                obj.view.close();
            });
        }
    };
    return obj;
});

