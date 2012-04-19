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

define("app/comp/common/dialog/AbstractDialogView", 
        [],
        function() {

    var obj = function AbstractDialogView(templateName, settings) {
        this.settings = settings;
        this.templateName = templateName;
    };

    obj.prototype.getCloseButton = function() {
        return $('#dialogClose');
    };

    obj.prototype.show = function(callback) {
        $("#dialog").fadeIn(300, function() {
            $(this).show(callback);
        });
    };

    obj.prototype.close = function() {
        $("#dialog").fadeOut(300, function() {
            $(this).hide();
        });
    };

    obj.prototype.getTemplateName = function() {
        return this.templateName;
    };

    obj.prototype.applySettings = function() {
        this.applyActions();
        this.applyWidth();
        this.applyHeight();
    };	

    obj.prototype.setContent = function(content) {
        $("#dialogContent").html(content);
    };

    obj.prototype.applyActions = function() {
        $("#dialogActions").html(this.settings.actions);
    };

    obj.prototype.applyWidth = function() {
        $("#dialogContainer").css('width', this.settings.width);

    };

    obj.prototype.applyHeight = function() {
        $("#dialogContainer").css('height', this.settings.height + 50);
        $("#dialogContent").css('height', this.settings.height);
        $("#dialogFrame").css('margin-top', - this.settings.height/2-100);
    };

    obj.prototype.bindInternalListners = function() {
        var current = obj;

        $("#dialogCloseCross").click(function(){
            console.log("cross closing");
            current.close();
        });
        $("#dialog").click(function() {
            console.log("close closing");
            current.close();
        });

        $("#dialogContainer").click(function(event) {
            event.stopPropagation();
        });

    };

    return obj;
});
