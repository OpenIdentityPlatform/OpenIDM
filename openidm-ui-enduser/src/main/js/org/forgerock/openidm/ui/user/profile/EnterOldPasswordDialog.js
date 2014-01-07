/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All rights reserved.
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
define("org/forgerock/openidm/ui/user/profile/EnterOldPasswordDialog", [
    "org/forgerock/commons/ui/common/components/Dialog",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants"
], function(Dialog, validatorsManager, conf, eventManager, constants) {
    var EnterOldPasswordDialog = Dialog.extend({    
        contentTemplate: "templates/user/EnterOldPasswordDialog.html",
        
        data: {         
            width: 800,
            height: 200
        },
        
        events: {
            "click input[type=submit]": "formSubmit",
            "click .dialogCloseCross img": "close",
            "click input[name='close']": "close",
            "onValidate": "onValidate",
            "click .dialogContainer": "stop"
        },
        
        formSubmit: function(event) {
            if(event) {
                event.preventDefault();
            }
            
            if(validatorsManager.formValidated(this.$el)) {   
                conf.setProperty('passwords', {
                    password: this.$el.find("input[name=oldPassword]").val()
                });
                
                this.close();                
                eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "changeSecurityData"});
            }
        },
        
        render: function() {
            this.actions = {};
            this.addAction($.t("common.form.continue"), "submit");
            
            this.show(_.bind(function() {
                validatorsManager.bindValidators(this.$el); 
            }, this));            
        }
    }); 
    
    return new EnterOldPasswordDialog();
});