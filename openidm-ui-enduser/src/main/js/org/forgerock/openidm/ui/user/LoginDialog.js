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

/*global $, _, define*/

define("org/forgerock/openidm/ui/user/LoginDialog", [
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager", 
    "org/forgerock/commons/ui/common/util/Constants", 
    "org/forgerock/commons/ui/common/components/Dialog",
    "org/forgerock/commons/ui/common/main/SessionManager",
    "org/forgerock/commons/ui/common/main/ViewManager"
], function(validatorsManager, conf, eventManager, constants, Dialog, sessionManager, viewManager) {
    var LoginDialog = Dialog.extend({
        contentTemplate: "templates/user/LoginDialog.html",
        element: '#dialogs',
        events: {
            "click input[type=submit]": "login",
            "click .dialogCloseCross img": "loginClose",
            "click input[name='close']": "loginClose",
            "click .dialogContainer": "stop",
            "onValidate": "onValidate",
            "keypress input": "submitForm"
        },
        
        displayed: false,
        
        submitForm: function(event) {
            if(event.which === 13) {
                this.login(event);
            }
        },
        
        render: function () {
            if(this.displayed === false) {
                this.displayed = true;
                
                this.addAction("Login", "submit");
                this.show(_.bind(function(){ 
                    validatorsManager.bindValidators(this.$el);
                    this.resize();
                    
                    $(".dialog-background").off('click').on('click', _.bind(this.loginClose, this));
                                        
                    if (conf.loggedUser && conf.loggedUser.userName)
                    {
                        $("input[name=login]").val(conf.loggedUser.userName).trigger("keyup");
                        $("input[name=password]").focus();
                    }
                    else
                    {
                        $("input[name=login]").focus();
                    }  
                }, this));
            }
        },
        
        loginClose: function(e) {
            if (e) {
                e.preventDefault();
            }
            
            this.displayed = false;
            this.close(e);
        },
        
        login: function (e) {
            e.preventDefault();
            
            if(validatorsManager.formValidated(this.$el)) {
                var userName, password, refreshOnLogin, _this = this;
                userName = this.$el.find("input[name=login]").val();
                password = this.$el.find("input[name=password]").val();
                refreshOnLogin = this.$el.find("input[name=refreshOnLogin]:checked").val();
                
                sessionManager.login({"userName":userName, "password":password}, function(user) {
                    conf.setProperty('loggedUser', user);
                    eventManager.sendEvent(constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: false});
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "loggedIn");
                    _this.loginClose();
                    
                    if (refreshOnLogin) {
                        viewManager.refresh();
                    }
                    
                }, function() {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "authenticationFailed"); 
                });
            }
        },
        data : {
            height: 200, 
            width: 400
        }
    });

    return new LoginDialog();
    
});

