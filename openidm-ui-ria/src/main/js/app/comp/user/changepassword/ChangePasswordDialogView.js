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

define("app/comp/user/changepassword/ChangePasswordDialogView",
        ["app/comp/common/dialog/DialogsCtrl",
         "app/util/UIUtils"], 
        function(dialogsCtrl, UIUtils) {

    var obj = {};

    obj.dialog = dialogsCtrl;

    obj.getSaveButton = function() {
        return $("#dialog input[name='dialogOk']");
    };

    obj.getCloseButton = function() {
        return $('#dialogClose');
    };

    obj.getPasswordInput = function() {
        return $("#dialog input[name='password']");
    };

    obj.getOldPasswordInput = function() {
        return $("#dialog input[name='passwordOld']");
    };

    obj.getPasswordConfirmInput = function() {
        return $("#dialog input[name='passwordConfirm']");
    };

    obj.show = function(callback) {
        var view = {user: false};

        console.log("showing change password dialog");
        UIUtils.fillTemplateWithData("templates/user/PasswordChangeTemplate.html", view, function(data) {
            obj.dialog.setContent(data);
            obj.dialog.setActions("<input type='button' name='dialogClose' id='dialogClose' class='button orange floatRight' value='Close' /><input type='button' name='dialogOk' id='dialogOk' class='button gray floatRight' value='Update' />");
            obj.dialog.setWidth(800);
            obj.dialog.setHeight(240);
            obj.dialog.show();
            callback();
        });
    };

    obj.close = function() {
        obj.dialog.close();
    };

    obj.enableSaveButton = function() {
        obj.getSaveButton().removeClass('gray').addClass('orange');
    };

    obj.disableSaveButton = function() {
        obj.getSaveButton().removeClass('orange').addClass('gray');
    };

    return obj;

});

