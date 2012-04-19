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

define("app/comp/common/dialog/DialogsView",
        [],
        function() {
    var obj = {};

    obj.isOverDialog = false;

    obj.show = function() {
        $("#dialog").fadeIn(300, function() {
            $(this).show();
        });
    };

    obj.close = function() {
        $("#dialog").fadeOut(300, function() {
            $(this).hide();
        });
    };

    obj.setContent = function(content) {
        $("#dialogContent").html(content);
    };

    obj.setActions = function(actions) {
        $("#dialogActions").html(actions);
    };

    obj.setWidth = function(width) {
        $("#dialogContainer").css('width', width);

    };

    obj.setHeight = function(height) {
        $("#dialogContainer").css('height', height + 50);
        $("#dialogContent").css('height', height);
        $("#dialogFrame").css('margin-top', -height/2-100);
    };

    obj.renderDialog = function() {

        $("#dialogCloseCross").click(function(){
            console.log("cross closing");
            obj.close();
        });
        $("#dialog").click(function() {
            console.log("close closing");
            obj.close();
        });

        $("#dialogContainer").click(function(event) {
            event.stopPropagation();
        });

    };

    return obj;
});

