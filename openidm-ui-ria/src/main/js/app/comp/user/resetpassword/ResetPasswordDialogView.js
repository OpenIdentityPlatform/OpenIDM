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

define("app/comp/user/resetpassword/ResetPasswordDialogView",
        ["app/comp/common/dialog/DialogsCtrl"], 
        function(dialogsCtrl) {

    var obj = {};

    obj.dialog = dialogsCtrl;

    obj.getConfirmButton = function() {
        return $("#dialog input[name='dialogOk']");
    };

    obj.getCloseButton = function() {
        return $('#dialogClose');
    };

    obj.getEmailInput = function() {
        return $("#dialog input[name='resetEmail']");
    };

    obj.getPasswordResetLink = function() {
        return $("#passwordResetLink");
    };

    obj.show = function(callback) {
        var self = this;

        console.log("showing reset password dialog");

        $.ajax({
            type: "GET",
            url: "templates/user/ResetPasswordTemplate.html",
            dataType: "html",
            success: function(data) {
                self.dialog.setContent(data);
                self.dialog.setActions("<input type='button' name='dialogClose' id='dialogClose' class='button gray floatRight' value='Close' /><input type='button' name='dialogOk' id='dialogOk' class='button gray floatRight' value='Remind me' />");
                self.dialog.setWidth(800);
                self.dialog.setHeight(210);
                self.dialog.show();
                callback();
            },
            error: callback
        });
    };

    obj.showD = function(callback) {
        var self = this;

        console.log("showing change password dialog");

        $.ajax({
            type: "GET",
            url: "templates/user/PasswordChangeTemplate.html",
            dataType: "html",
            success: function(data) {
                self.dialog.setContent("<h2>Now you should check email and click special link. This is only demo.</h2>"+data);
                self.dialog.setHeight(240);
                callback();
            },
            error: callback
        });
    };


    obj.close = function() {
        obj.dialog.close();
    };

    obj.enableSaveButton = function() {
        obj.getConfirmButton().removeClass('gray').addClass('orange');
    };

    obj.disableSaveButton = function() {
        obj.getConfirmButton().removeClass('orange').addClass('gray');
    };

    obj.showEmail = function() {
        obj.getFgtnAnswerDiv().hide();
        obj.getFgtnEmailDiv().show();
    };

    obj.showAnswer = function() {
        obj.getFgtnAnswerDiv().show();
        obj.getFgtnEmailDiv().hide();
    };

    console.log("Reset Password View created");
    return obj;

});

