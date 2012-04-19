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

/**
 * @author mbilski
 */

define("app/comp/user/securitypicture/SelectPictureDialogCtrl",
        ["app/comp/user/securitypicture/SelectPictureDialogView",
         "app/comp/user/delegates/UserDelegate",
         "app/comp/common/messages/MessagesCtrl",
         "app/util/Validator",
         "app/util/Condition",
         "app/util/Validators"], 
         function(selectPictureDialogView, userDelegate, messagesCtrl, Validator, Condition, validators) {
    var obj = {};

    obj.view = selectPictureDialogView;
    obj.selectCallback = null;

    obj.delegate = userDelegate;
    obj.messages = messagesCtrl;

    obj.user = null;

    obj.validators = [];

    obj.init = function(profileCtrl) {
        obj.user = profileCtrl.getUser();
        obj.profileCtrl = profileCtrl;

        obj.view.show(function() {
            obj.registerListeners();

            if(obj.user.image !== null) {
                obj.view.selectImageByValue(obj.user.image);
            }

            obj.view.getPassphraseInput().val(obj.user.passphrase);

            obj.registerValidators();

            obj.view.getSaveButton().on('click', function() { obj.afterSaveButtonClicked(); });
        });
    };


    obj.registerValidators = function() {
        obj.validators[0] = new Validator([obj.view.getPassphraseInput()], 
                [new Condition('not-empty', validators.passphraseValidator)], 'change', 'simple', obj.validateForm);
    };

    obj.validateForm = function() {
        if(obj.validators[0].isOk()) {
            obj.view.enableSaveButton();
        } else {
            obj.view.disableSaveButton();
        }

        return obj.validators[0].isOk();
    };

    obj.registerListeners = function() {
        obj.view.getCloseButton().off().on('click', function() {
            obj.view.close();
        });

        obj.view.getImages().off().on('click', function() {
            obj.view.selectImage(this);
        });
    };

    obj.afterSaveButtonClicked = function() {
        var patchDefinition = [{replace: "image", value: obj.view.getSelectedImage()}, {replace: "passphrase", value: obj.view.getPassphraseInput().val()}];

        if(obj.validateForm() === true) {
            obj.delegate.patchSelectedUserAttributes(obj.user.userName,  patchDefinition,
                    function(r) {
                obj.messages.displayMessage('info','Site identification image has been changed');
                //updating in profile
                obj.profileCtrl.getUser().image = obj.view.getSelectedImage();
                obj.profileCtrl.getUser().passphrase = obj.view.getPassphraseInput().val();
                obj.view.close();
            }, function(r) {
                obj.messages.displayMessage('error', 'Unknown error');
                obj.view.close();
            });
        }
    };

    return obj;
});
