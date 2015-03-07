/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All rights reserved.
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
    "org/forgerock/commons/ui/user/profile/UserProfileView",
    "org/forgerock/openidm/ui/user/delegates/CountryStateDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/ValidatorsManager"
], function(commonProfileView, countryStateDelegate, conf, uiUtils, validatorsManager) {
    
    var obj = $.extend({}, commonProfileView);
    
    obj.data.hasAddressDetails = true;
    
    obj.render = function(args, callback) {
        if(conf.globalData.userComponent && conf.globalData.userComponent === "repo/internal/user"){
            obj.data.adminUser = true;
        } else {
            obj.data.adminUser = false;
        }
        this.parentRender(function() {
            var self = this,
                baseEntity = this.delegate.getUserResourceName(conf.loggedUser);

            validatorsManager.bindValidators(this.$el, baseEntity, _.bind(function () {
                
                countryStateDelegate.getAllCountries( function(countries) {
                    uiUtils.loadSelectOptions(countries, $("select[name='country']"), true, _.bind(function() {
                        if(conf.loggedUser.country) {
                            this.$el.find("select[name='country'] > option:first").text("");
                            this.$el.find("select[name='country']").val(conf.loggedUser.country);
                            
                            this.changeCountry();
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
    };
    
    obj.events["change select[name='country']"] = function() {
        obj.changeCountry();
    };
    
    obj.changeCountry = function() {
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
    };
    
    obj.events["change select[name='stateProvince']"] = function() {
        obj.loadStates();
    };
    
    obj.loadStates = function() {
        var state = $('#profile select[name="stateProvince"]').val();
        
        if(state) {
            this.$el.find("select[name='stateProvince'] > option:first").text("");
        } else {
            this.$el.find("select[name='stateProvince'] > option:first").text($.t("common.form.pleaseSelect")); 
        }
    };
    
    return obj;
});


