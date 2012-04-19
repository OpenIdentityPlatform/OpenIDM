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

define("app/comp/common/dialog/AbstractDialogController",
        ["app/util/UIUtils"],
        function(UIUtils) {
    var obj = function AbstractDialogController(view) {
        this.view = view;
    };

    obj.prototype.getView = function () {
        return this.view;
    };

    obj.prototype.show = function(callback, mode) {
        var view, current = this;
        
        if( mode === 'user' ) {
            view = {user: true};
        } else {
            view = {user: false};
        }
        

        console.log("showing change password dialog");
        UIUtils.fillTemplateWithData(this.getView().getTemplateName(), view, function(data) {
            current.setContent(data);
            current.getView().applySettings();
            current.getView().show(function () {
                current.getView().bindInternalListners();
                if(callback) { 
                    callback();
                }
            });
        });
    };

    obj.prototype.close = function() {
        console.log("close dialog");
        this.view.close();
    };

    obj.prototype.setContent = function(content) {
        this.view.setContent(content);
    };

    obj.prototype.setActions = function(actions) {
        this.view.settings.actions = actions;
    };

    obj.prototype.setWidth = function(width) {
        this.view.settings.width = width;
    };

    obj.prototype.setHeight = function(height) {
        this.view.settings.height = height;
    };

    return obj;
});

