/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All rights reserved.
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

/*global define, $, _, require */

define("org/forgerock/openidm/ui/admin/MandatoryPasswordChangeDialog", [
    "org/forgerock/openidm/ui/common/delegates/InternalUserDelegate",
    "org/forgerock/commons/ui/common/components/Dialog",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "AuthnDelegate"
], function(InternalUserDelegate, Dialog, validatorsManager, conf, eventManager, constants, authnDelegate) {
    var MandatoryPasswordChangeDialog = Dialog.extend({    
        contentTemplate: "templates/admin/MandatoryPasswordChangeDialogTemplate.html",
        baseTemplate: "templates/admin/AdminBaseTemplate.html",
        delegate: InternalUserDelegate,
        events: {
            "click input[type=submit]": "formSubmit",
            "onValidate": "onValidate",
            "click .dialogContainer": "stop",
            "customValidate": "customValidate"
        },
                
        formSubmit: function(event) {
            event.preventDefault();
            
            var patchDefinitionObject = [], element;
            if(validatorsManager.formValidated(this.$el.find("#passwordChange"))) {            
                patchDefinitionObject.push({operation: "replace", field: "password", value: this.$el.find("input[name=password]").val()});
            }
            
            this.delegate.patchSelectedUserAttributes(conf.loggedUser._id, conf.loggedUser._rev, patchDefinitionObject, _.bind(function(r) {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "securityDataChanged");
                delete conf.passwords;
                this.close();

                return authnDelegate.getProfile()
                    .then(function(user) {
                        conf.loggedUser = user;
                        return user;
                    });

            }, this));
        },
        
        render: function() {
            this.actions = [];
                                
            $("#dialogs").hide();

            this.show(_.bind(function() {
                validatorsManager.bindValidators(this.$el, this.delegate.baseEntity + "/" + conf.loggedUser._id, _.bind(function () {
                    $("#dialogs").show();
                    $("#dialogs .dialogCloseCross").hide();
                    $("#dialogs .dialogActions").hide();

                    this.$el.find("[name=password]").focus();
                }, this));
            }, this));
            
            this.$el.find("input[type=submit]").prop('disabled', true);
        },
        
        customValidate: function () {
            if(validatorsManager.formValidated(this.$el.find("#passwordChange")) || validatorsManager.formValidated(this.$el.find("#securityDataChange"))) {
                this.$el.find("input[type=submit]").prop('disabled', false);
            }
            else {
                this.$el.find("input[type=submit]").prop('disabled', true);
            }
            
        }
    }); 
    
    return new MandatoryPasswordChangeDialog();
});

