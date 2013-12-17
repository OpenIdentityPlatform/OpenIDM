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

/*global define, $, form2js, _, js2form, document */

/**
 * @author mbilski
 */
define("org/forgerock/openidm/ui/user/profile/UserProfileView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "UserDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/user/delegates/CountryStateDelegate"
], function(AbstractView, validatorsManager, uiUtils, userDelegate, eventManager, constants, conf, countryStateDelegate) {
    var UserProfileView = AbstractView.extend({
        template: "templates/user/UserProfileTemplate.html",
        baseTemplate: "templates/common/DefaultBaseTemplate.html",
        delegate: userDelegate,
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
                
                if(data.phoneNumber) {
                    data.phoneNumber = data.phoneNumber.split(' ').join('').split('-').join('').split('(').join('').split(')').join('');
                }
                
                this.delegate.patchUserDifferences(conf.loggedUser, data, _.bind(function() {
                    if(conf.loggedUser.userName !== data.userName) {
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "profileUpdateSuccessful");
                        eventManager.sendEvent(constants.EVENT_LOGOUT);
                        return;
                    }
                    if ($.inArray("ui-admin", conf.loggedUser.roles) === -1) {
                        this.delegate.getForUserID(data._id, function(user) {
                            conf.loggedUser = user;
                            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "profileUpdateSuccessful");
                            self.reloadData();
                        });
                    } else {
                        this.delegate.getForUserName(data.userName, function(user) {
                            conf.loggedUser = user;
                            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "profileUpdateSuccessful");
                            self.reloadData();
                        });
                    }
                }, this));
            } else {
                console.log('dupa');
            }
        },
        
        render: function(args, callback) {
            this.parentRender(function() {
                var self = this,
                    baseEntity = this.delegate.baseEntity + "/" + conf.loggedUser._id;
                if (conf.globalData.userComponent === "internal/user") {
                    baseEntity = "repo/internal/user/" + conf.loggedUser._id;
                }
                validatorsManager.bindValidators(this.$el, baseEntity, _.bind(function () {
                    
                    countryStateDelegate.getAllCountries( function(countries) {
                        uiUtils.loadSelectOptions(countries, $("select[name='country']"), true, _.bind(function() {
                            if(conf.loggedUser.country) {
                                this.$el.find("select[name='country'] > option:first").text("");
                                this.$el.find("select[name='country']").val(conf.loggedUser.country);
                                
                                this.loadStates();
                            }
                        }, self));
                    });

                    this.reloadData();

                    if(callback) {
                        callback();
                    }
                    
                }, this));
                
                
            });            
        },
        
        loadStates: function() {
            var country = this.$el.find('select[name="country"]').val(), self = this;            
              
            if(country) {
                this.$el.find("select[name='country'] > option:first").text("");
                
                countryStateDelegate.getAllStatesForCountry(country, function(states) {
                    uiUtils.loadSelectOptions(states, $("select[name='stateProvince']"), true, _.bind(function() {
                        if(conf.loggedUser.stateProvince) {
                            this.$el.find("select[name='stateProvince'] > option:first").text("");
                            this.$el.find("select[name='stateProvince']").val(conf.loggedUser.stateProvince);
                        }
                    }, self));
                });
            } else {
                this.$el.find("select[name='stateProvince']").emptySelect();
                this.$el.find("select[name='country'] > option:first").text($.t("common.form.pleaseSelect"));
                this.$el.find("select[name='stateProvince'] > option:first").text($.t("common.form.pleaseSelect"));
            }
        },
        
        selectState: function() {
            var state = $('#profile select[name="stateProvince"]').val();
            
            if(state) {
                this.$el.find("select[name='stateProvince'] > option:first").text("");
            } else {
                this.$el.find("select[name='stateProvince'] > option:first").text($.t("common.form.pleaseSelect")); 
            }
        },
        
        reloadData: function() {
            js2form(document.getElementById(this.$el.attr("id")), conf.loggedUser);
            this.$el.find("input[type=submit]").val($.t("common.form.update"));
            validatorsManager.validateAllFields(this.$el);
        }
    }); 
    
    return new UserProfileView();
});


