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

define("app/comp/user/changesecurityinfo/SecurityDialogView",
        ["app/comp/common/dialog/AbstractDialogView"], 
        function(AbstractDialogView) {

    var obj = new AbstractDialogView("templates/user/SecurityDialogTemplate.html", {width: 800, height: 140, actions:"<input type='button' name='dialogClose' id='dialogClose' class='button orange floatRight' value='Close' />" +
    "<input type='button' name='dialogOk' id='dialogOk' class='button gray floatRight' value='Continue' />"});

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

    obj.getQuestion = function() {
        return $("#dialog select[name='fgtnSecurityQuestion']");
    };

    obj.getAnswer = function() {
        return $("#dialog input[name='fgtnSecurityAnswer']");
    };

    obj.showMore = function() {
        $("#securityPassword").hide();
        obj.settings.height= 370;
        obj.applyHeight();
        $("#securityMore").show();
        $("#dialog input[name=dialogOk]").val("Update");
    };

    obj.showLess = function() {
        $("#securityPassword").show();
        obj.settings.height= 140;
        obj.applyHeight();
        $("#securityMore").hide();
    };

    obj.enableSaveButton = function() {
        obj.getSaveButton().removeClass('gray').addClass('orange');
    };

    obj.disableSaveButton = function() {
        obj.getSaveButton().removeClass('orange').addClass('gray');
    };

    return obj;

});

