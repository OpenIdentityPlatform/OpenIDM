/*
 * @license DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2012 ForgeRock AS. All rights reserved.
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

/*global define, $, _, Backbone, window */

/**
 * @author mbilski
 */

define("org/forgerock/openidm/ui/common/components/ConfirmationDialog", [
    "org/forgerock/openidm/ui/common/components/Dialog"
], function(Dialog) {
    var ConfirmationDialog = Dialog.extend({
        template: "templates/common/DialogTemplate.html",        
        contentTemplate: "templates/common/ConfirmationDialogTemplate.html",
        
        events: {
            "click input[type=submit]": "formSubmit",
            "click .dialogCloseCross img": "close",
            "click input[name='close']": "close",
            "click": "close",
            "click .dialogContainer": "stop"
        },
        
        data: {         
            width: 400,
            height: 100
        },
        
        formSubmit: function() {
            this.okCallback();
            this.close();
        },
        
        render: function(title, msg, actionName, okCallback) {
            this.actions = {};
            this.addAction(actionName, "submit");
            
            this.data.msg = msg;
            this.data.title = title;
            this.okCallback = okCallback;
            
            this.show(_.bind(function() {
                this.$el.find("input[type=submit]").removeClass("gray").addClass("orange");
            }, this));            
        }
    });

    return new ConfirmationDialog();
});