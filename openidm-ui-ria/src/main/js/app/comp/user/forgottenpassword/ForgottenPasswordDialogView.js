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

define("app/comp/user/forgottenpassword/ForgottenPasswordDialogView", 
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
        return $("#dialog input[name='forgottenEmail']");
    };

    obj.getPasswordInput = function() {
        return $("#dialog input[name='password']");
    };

    obj.getPasswordConfirmInput = function() {
        return $("#dialog input[name='passwordConfirm']");
    };

    obj.getFgtnSecurityQuestion = function() {
        return $("#fgtnSecurityQuestion");
    };

    obj.getFgtnSecurityAnswer = function() {
        return $("#dialog input[name='fgtnSecurityAnswer']");
    };

    obj.getFgtnEmailDiv = function() {
        return $("#fgtnEmailDiv");
    };

    obj.getFgtnAnswerDiv = function() {
        return $("#fgtnAnswerDiv");
    };

    obj.getPasswordResultDiv = function() {
        return $("#fgtnPasswordResult");
    };

    obj.getPasswordResetLink = function() {
        return $("#passwordResetLink");
    };

    obj.showPassword = function (val) {
        obj.getPasswordResultDiv().text("");
        obj.getPasswordResultDiv().append("<div class='field'>" + val + "</div>");
    };

    obj.reply = function() {
        console.log("View reply");
    };

    obj.show = function(callback) {
        var self = this;

        console.log("showing forgotten password dialog");

        $.ajax({
            type: "GET",
            url: "templates/user/ForgottenPasswordTemplate.html",
            dataType: "html",
            success: function(data) {
                self.dialog.setContent(data);
                self.dialog.setActions("<input type='button' name='dialogClose' id='dialogClose' class='button orange floatRight' value='Close' /><input type='button' name='dialogOk' id='dialogOk' class='button gray floatRight' value='Update' />");
                self.dialog.setWidth(800);
                self.dialog.setHeight(210);
                self.dialog.show();
                self.disableAnswerPanel();
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

    obj.enableAnswerPanel = function() {
        obj.getFgtnAnswerDiv().show();
        obj.getConfirmButton().val('Update');
        obj.dialog.setHeight(380);
    };

    obj.disableAnswerPanel = function() {
        obj.getFgtnAnswerDiv().hide();
        obj.getConfirmButton().val('Continue');
        obj.getFgtnSecurityAnswer().val('');
        obj.getPasswordInput().val('');
        obj.getPasswordConfirmInput().val('');
        obj.dialog.setHeight(210);
    };

    console.log("Forgotten Password View created");
    return obj;

});

