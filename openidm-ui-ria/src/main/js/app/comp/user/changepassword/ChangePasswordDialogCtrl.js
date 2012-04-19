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

/*global define */ 

define("app/comp/user/changepassword/ChangePasswordDialogCtrl",
        ["app/comp/user/changepassword/ChangePasswordDialogView",
         "app/comp/user/delegates/UserDelegate",
         "app/comp/common/messages/MessagesCtrl", 
         "app/util/Validator",
         "app/util/Condition"], 
         function(changePasswordDialogView, userDelegate, messagesCtrl, Validator, Condition) {
    var obj = {};

    obj.view = changePasswordDialogView;
    obj.delegate = userDelegate;
    obj.messages = messagesCtrl;

    obj.validators = [];
    obj.userName = null;

    obj.init = function(userName) {
        obj.userName = userName;


        obj.view.show(function() {
            obj.registerValidators();

            obj.view.getCloseButton().on('click', function(event) {
                obj.view.close();
            });

            obj.view.getSaveButton().on('click', function(event) {
                event.preventDefault();

                obj.afterSaveButtonClicked();
            });
        });
    };

    obj.registerValidators = function() {
        console.log("register dialog validators");



        obj.validators[1] = new Validator([ obj.view.getPasswordConfirmInput(),
                                                    obj.view.getPasswordInput() ], [ new Condition('same',
                                                            function(inputs) {
                                                        if (inputs[0].val() === "" || inputs[0].val() !== inputs[1].val()) {
                                                            return "Passwords have to be equal.";
                                                        }
                                                    }) ], 'keyup', 'advanced', obj.validateForm);


        obj.validators[0] = new Validator([ obj.view.getPasswordInput() ], [
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
                                                                                    }) ], 'keyup', 'advanced', obj.validateForm);
    };

    obj.validateForm = function() {
        console.log('validate all form');

        var i, allOk = true;

        for (i = 0; i < obj.validators.length; i++) {
            if (obj.validators[i].isOk() === false) {
                allOk = false;
                break;
            }
        }

        if (allOk) {
            obj.view.enableSaveButton();
        } else if (!allOk) {
            obj.view.disableSaveButton();
        }

        return allOk;
    };

    obj.afterSaveButtonClicked = function() {
        var k;
        for (k = 0; k < obj.validators.length; k++) {
            obj.validators[k].validate();
        }

        if (obj.validateForm() === true) {
            obj.delegate.patchEntityAttribute({"_query-id": "for-userName", uid: obj.userName}, "password", obj.view.getPasswordInput().val(),
                    function(r) {
                obj.messages.displayMessage('info','Password has been changed');

                obj.view.close();
            }, function(r) {
                obj.messages.displayMessage('error', 'Unknown error');

                obj.view.close();
            });
        }
    };
    return obj;
});

