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

define("app/comp/common/dialog/ConfirmationDialogCtrl",
        ["app/comp/common/dialog/ConfirmationDialogView"], 
        function (view) {
    var obj = {};

    obj.view = view;

    obj.init = function(title, desc, actionName, action, width, height) {
        var w = 400, h = 60;

        if( width !== undefined && height !== undefined ) {
            w = width;
            h = height;
        }

        obj.view.show(title, desc, actionName, function() {
            obj.view.getActionButton().off();
            obj.view.getActionButton().on('click', function() {
                action();
            });

            obj.view.getCloseButton().off();
            obj.view.getCloseButton().on('click', function() {
                obj.view.close();
            });
        }, w, h);
    };

    obj.close = function() {
        obj.view.close();
    };

    return obj;
});

