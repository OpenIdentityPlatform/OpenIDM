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

define("app/comp/user/resetpassword/ResetPasswordDialogCtrl",["app/comp/user/resetpassword/ResetPasswordDialogView",
                                                              "app/comp/user/delegates/UserDelegate",
                                                              "app/comp/common/messages/MessagesCtrl", 
                                                              "app/comp/user/changepassword/ChangePasswordDialogCtrl",
                                                              "app/util/Validator",
                                                              "app/util/Condition",
                                                              "app/util/Validators"], 
                                                              function(resetPasswordDialogView, userDelegate, messagesCtrl, changePasswordCtrl, Validator, Condition, validators) {

    var obj = {};

    obj.view = resetPasswordDialogView;
    obj.changeCtrl = changePasswordCtrl;
    obj.delegate = userDelegate;
    obj.messages = messagesCtrl;

    obj.validators = [];	
    obj.userLogin = null;

    obj.init = function() {
        var self = this;

        obj.view.show(function() {
            self.registerValidators();

            self.view.getCloseButton().on('click', function(event) {
                self.view.close();
            });

            self.view.getConfirmButton().on('click', function(event) {
                event.preventDefault();
                obj.sendTokenButtonClicked();
            });
        });
    };


    obj.registerValidators = function() {
        console.log("register forgotten dialog validators");
        obj.validators[0] = new Validator([obj.view.getEmailInput()], [new Condition('email', validators.emailValidator)], 'keyup', 'simple', obj.validateForm);
    };

    obj.validateForm = function() {
        var i, allOk = true;
        console.log('validate form');

        for (i = 0; i < obj.validators.length; i++) {
            if (obj.validators[i].isOk() === false) {
                allOk = false;
                console.log('validate false');
                break;
            }
        }

        if (allOk) {
            console.log('validate true');
            obj.view.enableSaveButton();
        } else if (!allOk) {
            obj.view.disableSaveButton();
        }

        return allOk;
    };


    obj.sendTokenButtonClicked = function() {
        var k, self = this;

        for (k = 0; k < obj.validators.length; k++) {
            obj.validators[k].validate();
        }

        if (obj.validateForm() === true) {

            obj.delegate.getForUserName(obj.view.getEmailInput().val(), function(r) {
                if (r.email === self.view.getEmailInput().val()) {
                    alert('Token will be send. You should check your email and click into link. It will redirect you to password change site.');
                    obj.changeCtrl.init(r.email,r.password);
                } 
            }, function(r) {
                self.messages.displayMessageOn('info', 'General error','#messages');
            });

        }
    };

    console.log("Reset Password ctrl Created");
    return obj;

});
