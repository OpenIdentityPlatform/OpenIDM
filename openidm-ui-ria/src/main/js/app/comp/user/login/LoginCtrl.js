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

/*global define, require*/

/**
 * @author mbilski
 */

define("app/comp/user/login/LoginCtrl",
        ["app/comp/user/login/LoginView",
         "app/comp/user/delegates/UserDelegate",
         "app/comp/common/eventmanager/EventManager",
         "app/util/Constants",
         "app/comp/common/configuration/AbstractConfigurationAware"],
         function (loginView, userDelegate, eventManager, constants, AbstractConfigurationAware) {
    var obj = new AbstractConfigurationAware();

    obj.view = loginView;
    obj.delegate = userDelegate;

    obj.eventManager = eventManager;

    obj.init = function() {
        console.log("LoginCtrl.init()");

        if(obj.configuration.showCredentialFields) {
            obj.view.showCredentialFields();
        }

        if(obj.configuration.showCredentialFields) {
            obj.view.disableLoginButton();
        }

        obj.view.renderLogin();
        obj.registerListeners();
    };

    obj.registerListeners = function() {
        console.log("LoginCtrl.registerListeners()");

        obj.view.getLoginButton().removeAttr('disabled');

        obj.view.getLoginButton().off().on('click', function(event) {
            obj.view.getLoginButton().blur();
            event.preventDefault();

            obj.afterLoginButtonClicked(event);
        });

        obj.view.getLogoutButton().off().on('click', function(event) {
            event.preventDefault();
            obj.view.renderLogin();
            obj.eventManager.sendEvent(constants.EVENT_LOGOUT);
            obj.view.disableLoginButton();
        });

        obj.view.getProfileButton().off().on('click', function(event) {
            event.preventDefault();
            obj.eventManager.sendEvent(constants.EVENT_SHOW_PROFILE_REQUEST);
        });

        obj.view.getRegisterButton().off().on('click', function(event) {
            event.preventDefault();
            obj.eventManager.sendEvent(constants.EVENT_SELF_REGISTRATION_REQUEST);
        });

        obj.view.getForgottenPasswordLink().off('click').on('click', function(event) {
            obj.eventManager.sendEvent(constants.EVENT_FORGOTTEN_SHOW_REQUEST, obj.view.getLoginInput().val());
        });

        obj.view.getLoginInput().off().on('keyup', function(event) {
            obj.validateForm();
        });

        obj.view.getPasswordInput().off().on('keyup', function(event) {
            obj.validateForm();
        });
    };

    obj.validateForm = function() {
        if( obj.view.getLogin() !== "" && obj.view.getPassword() !== "" ) {
            obj.view.enableLoginButton();
        } else {
            obj.view.disableLoginButton();
        }
    };

    obj.afterLoginButtonClicked = function(event) {
        if(obj.configuration.loginButtonDisabledByDefault === true) {
            obj.view.getLoginButton().off();
            obj.view.getLoginButton().attr('disabled', 'disabled');
        } 
        obj.view.untoggle();
        obj.eventManager.sendEvent(constants.EVENT_LOGIN_REQUEST, {userName: obj.view.getLogin(), password: obj.view.getPassword(), loginMethod: obj.configuration.loginMethod});
    };

    obj.afterLoginFailed = function() {
        obj.registerListeners();
        obj.view.toggle();
    };

    obj.afterSuccessfulLogin = function(event) {
        obj.registerListeners();
        obj.view.renderLogged();
        obj.view.setUserName(event.userName);
    };

    obj.setUserName = function(userName) {
        obj.view.setUserName(userName);
    };

    obj.loginUser = function(userName, password) {
        require(obj.configuration.loginHelperClass).loginRequest(userName, password);
    };

    obj.logoutUser = function() {
        require(obj.configuration.loginHelperClass).logoutRequest();
    };

    obj.isCurrentlyLoggedUser = function(userName) {
        require(obj.configuration.loginHelperClass).logoutRequest();
    };

    console.debug("loginctrl created");
    return obj;
});
