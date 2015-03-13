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

/*global define, $, _, ContentFlow, require */

/**
 * @author mbilski
 */
define("org/forgerock/openidm/ui/admin/users/ChangeUserPasswordDialog", [
    "org/forgerock/commons/ui/common/components/Dialog",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "UserDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants"
], function(Dialog, validatorsManager, conf, userDelegate, uiUtils, eventManager, constants) {
    var ChangeUserPasswordDialog = Dialog.extend({    
        contentTemplate: "templates/admin/ChangeUserPasswordDialogTemplate.html",
        delegate: userDelegate,
        data: {
            width: 800,
            height: 300
        },
        
        events: {
            "click input[type=submit]": "formSubmit",
            "onValidate": "onValidate",
            "click .dialogCloseCross img": "close",
            "click input[name='close']": "close",
            "click .modal-content": "stop"
        },
        
        editedUsername: {},
        
        formSubmit: function(event) {
            event.preventDefault();
            
            if(validatorsManager.formValidated(this.$el)) {
                var patchDefinitionObject = [], element;
                
                if(this.$el.find("input[name=password]").val() !== "") {
                    patchDefinitionObject.push({operation: "replace", field :"/password", value: this.$el.find("input[name=password]").val()});
                }
                
                this.delegate.getForUserName(this.editedUsername, _.bind(function(user) {
                    this.delegate.patchSelectedUserAttributes(user._id, user._rev, patchDefinitionObject, _.bind(function(r) {
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "securityDataChanged");
                        this.close();
                        
                        userDelegate.getForUserName(this.editedUsername, function(user) {
                            require("org/forgerock/openidm/ui/admin/users/AdminUserProfileView").editedUser = user;
                        });
                    }, this));
                }, this));
            }
        },
        
        render: function(params, callback) {
            this.editedUsername = params[0];
            this.actions = [];
            this.addAction("Update", "submit");

            this.addTitle ($.t("openidm.ui.admin.users.ChangeUserPasswordDialog.securityDataChangeForWhom", { postProcess: 'sprintf', sprintf: [this.editedUsername] }));

            this.delegate.getForUserName(this.editedUsername, _.bind(function(user) {
                this.show(_.bind(function() {
                    // necessary to add the details of the user to the form as hidden variables for
                    // some types of policy validation checking (cannotContainOthers for example)
                    var form = this.$el.find("form");
                    _.each(user, function (value, name) {
                        form.append($("<input>").attr("type", "hidden").attr("name", name).val(value));
                    });

                    validatorsManager.bindValidators(this.$el, this.delegate.baseEntity + "/" + user._id, _.bind(function () {
                        this.$el.find("input[type=submit]").prop('disabled', true);
                        this.$el.find("[name=password]").focus();

                        if (callback) {
                            callback();
                        }

                        this.reloadData();
                    
                    }, this));
                }, this));
            }, this));
        },
        
        reloadData: function() {
            var user = conf.loggedUser;
            validatorsManager.validateAllFields(this.$el);
        }
    }); 
    
    return new ChangeUserPasswordDialog();
});
