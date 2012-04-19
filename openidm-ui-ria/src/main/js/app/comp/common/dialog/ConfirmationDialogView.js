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

define("app/comp/common/dialog/ConfirmationDialogView",
        ["app/comp/common/dialog/DialogsCtrl",
         "app/util/UIUtils"], 
        function(dialogsCtrl, UIUtils) {

    var obj = {};

    obj.dialog = dialogsCtrl;

    obj.getActionButton = function() {
        return $("#dialog input[name='dialogOk']");
    };

    obj.getCloseButton = function() {
        return $('#dialogClose');
    };

    obj.show = function(title, desc, actionName, callback, width, height) {
        var self = this, view = {'title': title};

        console.log("showing confirmation dialog");

        UIUtils.fillTemplateWithData("templates/common/ConfirmationDialogTemplate.html", view, function(data) {
            self.dialog.setContent(data);
            self.dialog.setActions("<input type='button' name='dialogClose' id='dialogClose' class='button orange floatRight' value='Close' />" +
                    "<input type='button' name='dialogOk' id='dialogOk' class='button orange floatRight' value='"+ actionName +"' />");
            self.dialog.setWidth(width);
            self.dialog.setHeight(height);
            $("#confirmationDialogContent").html(desc);
            self.dialog.show();
            callback();
        });
    };

    obj.close = function() {
        obj.dialog.close();
    };

    return obj;

});

