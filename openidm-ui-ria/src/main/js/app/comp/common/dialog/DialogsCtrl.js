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

/*global define*/

/**
 * @author mbilski
 */

define("app/comp/common/dialog/DialogsCtrl",
        ["app/comp/common/dialog/DialogsView"], 
        function (dialogsView) {
    var obj = {};

    obj.view = dialogsView;

    obj.init = function() {
        console.log("DialogsCtrl.init()");

        obj.view.renderDialog();
    };

    obj.show = function() {
        console.log("show dialog");
        obj.view.show();
    };

    obj.close = function() {
        console.log("close dialog");
        obj.view.close();
    };

    obj.setContent = function(content) {
        obj.view.setContent(content);
    };

    obj.setActions = function(actions) {
        obj.view.setActions(actions);
    };

    obj.setWidth = function(width) {
        obj.view.setWidth(width);
    };

    obj.setHeight = function(height) {
        obj.view.setHeight(height);
    };

    return obj;
});

