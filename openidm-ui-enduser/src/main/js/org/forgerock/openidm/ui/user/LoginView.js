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

/*global define, $, form2js, _, js2form, window */

/**
 * @author huck.elliott
 */
define("org/forgerock/openidm/ui/user/LoginView", [
    "org/forgerock/commons/ui/common/LoginView",
    "SiteIdentificationDelegate",
    "org/forgerock/commons/ui/common/main/Configuration"
], function(commonLoginView, siteIdentificationDelegate, conf) {
    
    var handleLoginChange = function() {
            var login = this.$el.find("input[name=login]").val();
            
            if(conf.globalData.siteIdentification) {
                if (login.length) {
                    siteIdentificationDelegate.getSiteIdentificationForLogin(login, _.bind(function(data) {
                        this.$el.find("#siteImage").html('<img src="'+ encodeURI(data.siteImage) +'" />').show();
                        
                        this.$el.find("#passPhrase").text(data.passPhrase).show();
                        this.$el.find("#identificationMessage").hide();
                    }, this));
                }
                else {
                    this.$el.find("#siteImage").hide();
                    this.$el.find("#passPhrase").hide();
                    this.$el.find("#identificationMessage").show();
                }
            }
        },
        LoginView = function () {},
        obj;

    LoginView.prototype = commonLoginView;
    
    obj = new LoginView();

    obj.render = function (args) {
        if (conf.globalData.securityQuestions || conf.globalData.selfRegistration || conf.globalData.siteIdentification) {
            obj.baseTemplate = "templates/common/MediumBaseTemplate.html";
        }
        commonLoginView.render.call(this, args, _.bind(handleLoginChange, this));
    };

    obj.events["change input[name=login]"] = handleLoginChange;
    
    return obj;
});


