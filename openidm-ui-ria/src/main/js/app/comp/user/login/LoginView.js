/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2012 ForgeRock AS. All rights reserved.
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

/*global $, define*/

/**
 * @author mbilski
 */

define("app/comp/user/login/LoginView",
        [],
        function() {
    var obj = {};

    obj.isOverLogin = false;

    obj.renderLogin = function() {
        var self = this;

        obj.getLoginInput().val("");
        obj.getPasswordInput().val("");

        $("#loginContent").fadeOut(200, function() {
            $("#loginBox").fadeIn(200);
        });

        $("#loginForm").mouseenter(function() {
            self.isOverLogin = true;
        });

        $("#loginForm").mouseleave(function() {
            self.isOverLogin = false;
        });

        $("#loginForm input[type!=submit]").mousedown(function() {
            self.toggle();
        });

        $("#bg").mousedown(function(event) {
            if (!obj.isOverLogin) {
                self.untoggle();
            }
        });
    };

    obj.renderLogged = function() {
        $("#loginBox").fadeOut(200, function() {
            $("#loginContent").fadeIn(200);
        });
    };

    obj.hideCredentialFields = function() {
        obj.getLoginInput().addClass('hidden');
        obj.getPasswordInput().addClass('hidden');
    };

    obj.showCredentialFields = function() {
        obj.getLoginInput().removeClass('hidden');
        obj.getPasswordInput().removeClass('hidden');
    };

    obj.getLoginButton = function() {
        return $("#loginForm input[name='loginButton']");
    };

    obj.getForgottenPasswordLink = function() {
        return $("#forgotPasswordLink");
    };

    obj.enableLoginButton = function() {
        obj.getLoginButton().removeClass('gray').addClass('orange');		
    };

    obj.disableLoginButton = function() {
        obj.getLoginButton().removeClass('orange').addClass('gray');		
    };

    obj.getProfileButton = function() {
        return $("#profile_link");
    };

    obj.getLogoutButton = function() {
        return $("#logout_link");
    };

    obj.getProfileButton = function() {
        return $("#profile_link");
    };

    obj.getRegisterButton = function() {
        return $("#loginBox input[name='registerButton']");
    };

    obj.getLogin = function() {
        return $("#loginForm input[name='loginText']").val();
    };

    obj.getPassword = function() {
        return $("#loginForm input[name='loginPassword']").val();
    };

    obj.getLoginInput = function() {
        return $("#loginForm input[name='loginText']");		
    };

    obj.getPasswordInput = function() {
        return $("#loginForm input[name='loginPassword']");
    };

    obj.untoggle = function() {
        $("#loginForm").removeClass("login_toggle");
        $("#loginRemember").hide();
    };

    obj.toggle = function() {
        $("#loginForm").addClass('login_toggle');
        $("#loginRemember").show();
    };

    obj.setUserName = function(name) {
        $("#user_name").html(name);
    };
    
    obj.hideLoginButton = function() {
        $("input[name=loginButton]").hide();
    };

    return obj;
}); 

