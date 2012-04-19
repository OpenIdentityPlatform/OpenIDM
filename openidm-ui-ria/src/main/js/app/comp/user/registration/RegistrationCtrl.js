/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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

/**
 * @author mbilski
 */

define("app/comp/user/registration/RegistrationCtrl",
        ["app/comp/user/registration/RegistrationView",
         "app/comp/user/delegates/UserDelegate",
         "app/util/Validator",
         "app/util/Condition",
         "app/util/Validators",
         "app/comp/common/eventmanager/EventManager",
         "app/util/Constants"], 
         function(registrationView, userDelegate, Validator, Condition, validators, eventManager, constants) {
    var obj = {};

    obj.view = registrationView;
    obj.delegate = userDelegate;

    obj.validators = [];

    obj.selectedPicture = null;

    obj.init = function(mode) {
        console.log("RegistrationCtrl.init()");

        obj.mode = (mode) ? mode : constants.MODE_USER;

        obj.view.show(function() {
            obj.view.disableRegisterButton();

            obj.registerListeners();
            obj.registerValidators();

            $("#termOfUseLink").on('click', function(event) {
                obj.view.showTermsOfUseDialog();
            });

            obj.setSecurityQuestionSelect();

            //passpharse
            obj.view.getImages().off().on('click', function() {
                obj.view.selectImage(this);

                obj.selectedPicture = obj.view.getValueOfImage(this);
            });
        }, obj.mode);

    };

    obj.registerValidators = function() {
        obj.validators[0] = new Validator([obj.view.getFirstNameInput()], [new Condition('letters-only', validators.nameValidator)], 'change', 'simple', obj.validateForm);

        obj.validators[1] = new Validator([obj.view.getLastNameInput()], obj.validators[0].conditions, 'change', 'simple', obj.validateForm);

        obj.validators[2] = new Validator([obj.view.getEmailInput()], [new Condition('email', validators.emailValidator), 
                                                                               new Condition('unique', validators.uniqueEmailValidator)], 'change', 'simple', obj.validateForm);

        obj.validators[3] = new Validator([ obj.view
                                                    .getPhoneInput() ], [ new Condition(
                                                            'letters-only', validators.phoneNumberValidator) ],
                                                            'keyup', 'simple', obj.validateForm);

        obj.validators[4] = new Validator([obj.view.getPasswordInput(), obj.view.getEmailInput()], [new Condition('min-8', function(inputs) {
            if(inputs[0].val().length < 8) {
                return "At least 8 characters length";
            }
        }), new Condition('one-capital', function(inputs) {
            var reg = /[(A-Z)]+/;

            if( !reg.test(inputs[0].val()) ) {
                return "At least one capital letter";
            }
        }), new Condition('one-number', function(inputs) {
            var reg = /[(0-9)]+/;

            if( !reg.test(inputs[0].val()) ) {
                return "At least one number";
            }
        }), new Condition('not-equal-username', function(inputs) {
            if( inputs[0].val() === "" || inputs[0].val() === inputs[1].val() ) {
                return "Cannot match login";
            }
        })], 'keyup', 'advanced', obj.validateForm);


        obj.validators[5] = new Validator([obj.view.getPasswordConfirmInput(), obj.view.getPasswordInput()], [new Condition('same', function(inputs) {
            if (inputs[0].val() === "" || inputs[0].val() !== inputs[1].val()) {
                return "Passwords have to be equal.";
            }
        })], 'keyup', 'advanced', obj.validateForm);

        if( obj.mode === 'user' ) {
            obj.validators[6] = new Validator([obj.view.getTermsOfUseCheckbox()], [new Condition('tou', function(inputs) {
                if ( !inputs[0].is(':checked') ) {
                    return "Acceptance required for registration";
                }
            })], 'click', 'simple', obj.validateForm);

            obj.validators[7] = new Validator([obj.view.getSecurityAnswer()], [new Condition('not-empty', validators.notEmptyValidator)], 'change', 'simple', obj.validateForm);

            obj.validators[8] = new Validator([obj.view.getSecurityQuestion()], [new Condition('not-empty', validators.notEmptyValidator)], 'change', 'simple', obj.validateForm);

            obj.validators[9] = new Validator([obj.view.getPassphrase()], [new Condition('not-empty', validators.passphraseValidator)], 'change', 'simple', obj.validateForm);
        }

    };

    /**
     * Callback function
     */
    obj.validateForm = function() {
        var i, allOk = true;

        for (i = 0; i < obj.validators.length; i++) {
            if (obj.validators[i].isOk() === false) {
                allOk = false;
                break;
            }
        }

        if (allOk) {
            obj.view.enableRegisterButton();
        } else if (!allOk) {
            obj.view.disableRegisterButton();
        }

        obj.view.getForgottenPasswordDialog().on('click', function(event) {
            eventManager.sendEvent(constants.EVENT_FORGOTTEN_SHOW_REQUEST, obj.view.getEmailInput().val());
        });

        return allOk;
    };

    obj.registerListeners = function() {
        obj.view.getRegisterButton().off();
        obj.view.getRegisterButton().on('click', function(event) {
            event.preventDefault();

            obj.view.getRegisterButton().off();
            obj.view.getRegisterButton().on('click', function(event){event.preventDefault(); });

            obj.afterRegisterButtonClicked(event);
        });
    };

    obj.setSecurityQuestionSelect = function(question) {
        $.ajax({
            type : "GET",
            url : "data/secquestions.json",
            dataType : "json",
            success : function(data) {
                obj.view.getSecurityQuestion().loadSelect(data);
                obj.view.getSecurityQuestion().val(question);

            },
            error : function(xhr) {
                console.log('Error: ' + xhr.status + ' ' + xhr.statusText);
            }
        });
    };

    obj.setSelectedPicture = function(picture) {
        obj.selectedPicture = picture;
        obj.validators[9].validate();
    };

    obj.afterRegisterButtonClicked = function(event) {
        console.log("RegistrationCtrl.afterRegisterButtonClicked()");

        if( obj.validateForm() === true ) {
            obj.delegate.createEntity(obj.view.getUser(), function(user) {
                if(obj.mode === constants.MODE_USER) {
                    eventManager.sendEvent(constants.EVENT_USER_SUCCESSFULY_REGISTERED, { user:obj.view.getUser(), selfRegistration: true });
                } else {
                    eventManager.sendEvent(constants.EVENT_USER_SUCCESSFULY_REGISTERED, { user:obj.view.getUser(), selfRegistration: false });
                }
            }, function(response) {
                console.warn(response);
                if (response.error === 'Conflict') {
                    eventManager.sendEvent(constants.EVENT_USER_REGISTRATION_ERROR, { causeDescription: 'User already exists' });
                } else {
                    eventManager.sendEvent(constants.EVENT_USER_REGISTRATION_ERROR, { causeDescription: 'Unknown error' });
                }
                obj.registerListeners();
            });
        } else {
            obj.registerListeners();
        }
    };

    console.log("RegistrationCtrl created");

    return obj;
});

