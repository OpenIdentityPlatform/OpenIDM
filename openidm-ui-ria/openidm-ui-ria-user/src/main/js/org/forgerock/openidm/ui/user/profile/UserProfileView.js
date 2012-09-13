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

/*global define, $, form2js, _, js2form, document */

/**
 * @author mbilski
 */
define("org/forgerock/openidm/ui/user/profile/UserProfileView", [
    "org/forgerock/openidm/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/common/main/ValidatorsManager",
    "org/forgerock/openidm/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/user/delegates/UserDelegate",
    "org/forgerock/openidm/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/main/Configuration"
], function(AbstractView, validatorsManager, uiUtils, userDelegate, eventManager, constants, conf) {
    var UserProfileView = AbstractView.extend({
        template: "templates/user/UserProfileTemplate.html",
        events: {
            "click input[type=submit]": "formSubmit",
            "onValidate": "onValidate",
            "change select[name='country']": "loadStates",
            "change select[name='stateProvince']": "selectState"
        },
        
        formSubmit: function(event) {
            event.preventDefault();
            event.stopPropagation();
            
            if(validatorsManager.formValidated(this.$el)) {
                var data = form2js(this.$el.attr("id"), '.', false), self = this;
                data.userName = data.email.toLowerCase();
                data.phoneNumber = data.phoneNumber.split(' ').join('').split('-').join('').split('(').join('').split(')').join('');
                
                userDelegate.patchUserDifferences(conf.loggedUser, data, function() {
                    if(conf.loggedUser.userName !== data.email) {
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "profileUpdateSuccessful");
                        eventManager.sendEvent(constants.EVENT_LOGOUT);
                        return;
                    }
                    
                    userDelegate.getForUserName(data.email, function(user) {
                        conf.loggedUser = user;
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "profileUpdateSuccessful");
                        self.reloadData();
                    }, function() {
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "profileUpdateFailed");
                        self.reloadData();
                    });
                }, function() {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "profileUpdateFailed");
                    self.reloadData();
                }, function() {
                    self.reloadData();
                });
            }
        },
        
        render: function(args, callback) {
            this.parentRender(function() {
                validatorsManager.bindValidators(this.$el);
                
                uiUtils.loadSelectOptions("data/countries.json", $("select[name='country']"), true, _.bind(function() {
                    if(conf.loggedUser.country) {
                        this.$el.find("select[name='country'] > option:first").text("");
                        this.$el.find("select[name='country']").val(conf.loggedUser.country);
                        
                        this.loadStates();
                    }
                }, this));

                this.reloadData();
                
                if(callback) {
                    callback();
                }
            });            
        },
        
        loadStates: function() {
            var country = $('#profile select[name="country"]').val();            
              
            if(country) {
                this.$el.find("select[name='country'] > option:first").text("");
                
                uiUtils.loadSelectOptions("data/"+country+".json", $("select[name='stateProvince']"), true, _.bind(function() {
                    if(conf.loggedUser.stateProvince) {
                        this.$el.find("select[name='stateProvince'] > option:first").text("");
                        this.$el.find("select[name='stateProvince']").val(conf.loggedUser.stateProvince);
                    }
                }, this));
            } else {
                this.$el.find("select[name='stateProvince']").emptySelect();
                this.$el.find("select[name='country'] > option:first").text("Please Select");
                this.$el.find("select[name='stateProvince'] > option:first").text("Please Select");
            }
        },
        
        selectState: function() {
            var state = $('#profile select[name="stateProvince"]').val();
            
            if(state) {
                this.$el.find("select[name='stateProvince'] > option:first").text("");
            } else {
                this.$el.find("select[name='stateProvince'] > option:first").text("Please Select"); 
            }
        },
        
        reloadData: function() {
            js2form(document.getElementById(this.$el.attr("id")), conf.loggedUser);
            this.$el.find("input[type=submit]").val("Update");
            validatorsManager.validateAllFields(this.$el);
        }
    }); 
    
    return new UserProfileView();
});


