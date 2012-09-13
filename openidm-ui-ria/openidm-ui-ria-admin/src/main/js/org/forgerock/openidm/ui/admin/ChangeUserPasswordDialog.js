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

/*global define, $, _, ContentFlow */

/**
 * @author mbilski
 */
define("org/forgerock/openidm/ui/admin/ChangeUserPasswordDialog", [
    "org/forgerock/openidm/ui/common/components/Dialog",
    "org/forgerock/openidm/ui/common/main/ValidatorsManager",
    "org/forgerock/openidm/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/user/delegates/UserDelegate",
    "org/forgerock/openidm/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/common/util/Constants"
], function(Dialog, validatorsManager, conf, userDelegate, uiUtils, eventManager, constants) {
    var ChangeUserPasswordDialog = Dialog.extend({    
        contentTemplate: "templates/admin/ChangeUserPasswordDialogTemplate.html",
        
        data: {         
            width: 800,
            height: 220
        },
        
        events: {
            "click input[type=submit]": "formSubmit",
            "onValidate": "onValidate",
            "click .dialogCloseCross img": "close",
            "click input[name='close']": "close",
            "click": "close",
            "click .dialogContainer": "stop"
        },
        
        editedUsername: {},
        
        formSubmit: function(event) {
            event.preventDefault();
            
            if(validatorsManager.formValidated(this.$el)) {            
                var patchDefinitionObject = [], element;
                
                if(this.$el.find("input[name=password]").val() !== "") {
                    patchDefinitionObject.push({replace: "password", value: this.$el.find("input[name=password]").val()});
                }
                
                userDelegate.patchSelectedUserAttributes(this.editedUsername, patchDefinitionObject, _.bind(function(r) {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "securityDataChanged");
                    this.close();
                }, this), _.bind(function(r) {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unknown");
                    this.close();
                }, this));
            }
        },
        
        render: function(params) {
            this.editedUsername = params[0];
            this.actions = {};
            this.addAction("Update", "submit");
            
            this.show(_.bind(function() {
                validatorsManager.bindValidators(this.$el);
                
                this.$el.find("#changeUserPasswordHeadingLabel").text("Security data change for " + this.editedUsername);
                
                this.reloadData();
            }, this));            
        },
        
        reloadData: function() {
            var user = conf.loggedUser;
            validatorsManager.validateAllFields(this.$el);
        }
    }); 
    
    return new ChangeUserPasswordDialog();
});