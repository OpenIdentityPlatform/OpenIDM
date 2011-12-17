/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
define(["app/comp/main/controllers/MessagesCtrl",
        "app/comp/user/views/LoginView",
        "app/comp/user/delegates/UserDelegate",
        "app/comp/main/controllers/BreadcrumbsCtrl",
        "app/comp/user/controllers/RegistrationCtrl",
        "app/comp/user/controllers/ProfileCtrl",
        "app/comp/user/controllers/ForgottenPasswordDialogCtrl"],
        function (messagesCtrl,
        		loginView,
        		userDelegate,
        		breadcrumbsCtrl,
        		registrationCtrl,
        		profileCtrl,
        		forgotPasswordCtrl) {
	var obj = {};

	obj.messages = messagesCtrl;
	obj.view = loginView;
	obj.delegate = userDelegate;
	obj.breadcrumbs = breadcrumbsCtrl;
	obj.forgottenPasswordDialog = forgotPasswordCtrl;
	obj.loggedUser = null;

    function getCookie(name) {
        var i,x,y;
        var cookies = document.cookie.split(";");
        for (i=0;i<cookies.length;i++) {
            x = cookies[i].substr(0,cookies[i].indexOf("="));
            y = cookies[i].substr(cookies[i].indexOf("=")+1);
            x = x.replace(/^\s+|\s+$/g,"");
            if (x==name) {
                return unescape(y);
            }
        }
    }

	obj.init = function() {
        console.log("LoginCtrl.init()");
        var username = getCookie("idmUser");
        if (username != null && username != "") {
            console.log("User: " + username);
            obj.breadcrumbs.set('My Profile');
            obj.view.renderLogged();
            obj.view.setUserName(username);
            obj.loggedUser = username;
        }
        else {
            obj.view.renderLogin();
        }
        obj.registerListeners();
	}

	obj.registerListeners = function() {
		var self = this;

		console.log("LoginCtrl.registerListeners()");

		obj.view.getLoginButton().removeAttr('disabled');

		obj.view.getLoginButton().bind('click', function(event) {
			event.preventDefault();

			self.view.getLoginButton().unbind();
			self.view.getLoginButton().attr('disabled', 'disabled');

			self.afterLoginButtonClicked(event);
		});

		obj.view.getLogoutButton().bind('click', function(event) {
			event.preventDefault();

            //JFN logout
            obj.delegate.logoutUser();
			self.view.renderLogin();

			require("app/comp/main/controllers/MainCtrl").clearContent();
            document.cookie = "idmUser=" + ";" + "expires=Thu, 01-Jan-70 00:00:01 GMT";
			self.breadcrumbs.set('');
		});

		obj.view.getProfileButton().unbind();
		obj.view.getProfileButton().bind('click', function(event) {
			event.preventDefault();

			profileCtrl.init(function() {
				profileCtrl.reloadUser();
			});
		});

		obj.view.getRegisterButton().unbind();
		obj.view.getRegisterButton().bind('click', function(event) {
			event.preventDefault();

			registrationCtrl.init();

			self.breadcrumbs.set('Registration');
		});
		
		$("#forgotPasswordLink").bind('click', function(event) {
			self.forgottenPasswordDialog.init();
		});
		
		obj.view.getLoginInput().bind('keyup', function(event) {
			self.validateForm();
		});
		
		obj.view.getPasswordInput().bind('keyup', function(event) {
			self.validateForm();
		});
	},
	
	obj.validateForm = function() {
		if( obj.view.getLogin() != "" && obj.view.getPassword() != "" ) {
			obj.view.enableLoginButton();
		} else {
			obj.view.disableLoginButton();
		}
	}
	
	obj.afterLoginButtonClicked = function(event) {
		var self = this;

		console.log("LoginCtrl.afterLoginButtonClicked()");

        obj.delegate.loginUser(obj.view.getLogin(),obj.view.getPassword(), function(r) {
            document.cookie="idmUser=" + obj.view.getLogin();
            self.messages.displayMessage('info','You have been successfully logged in.');
        }, function(r) {
            //self.messages.displayMessage('info', 'Login/password combination is invalid.');
            self.messages.displayMessage('info','You have been successfully logged in.');
            document.cookie="idmUser=" + obj.view.getLogin();
            self.registerListeners();
        });

        if (obj.view.getLogin() == "admin") {
                obj.breadcrumbs.set('My Profile');
                obj.view.renderLogged();
                obj.view.setUserName(obj.view.getLogin());
                obj.loggedUser = obj.view.getLogin();
        }
        else {
            obj.delegate.getUser(obj.view.getLogin(), function(r) {
                self.loginUser(r);
                self.view.untoggle();
            }, function(r) {
                self.view.untoggle();
                self.registerListeners();
            });
        }

	}

	obj.loginUser = function(user) {
		var self = this;

		obj.breadcrumbs.set('My Profile');
		obj.view.renderLogged();
		obj.view.setUserName(user.email);

		obj.loggedUser = user;

		profileCtrl.init(function() {
			profileCtrl.setUser(self.loggedUser);
		});
	}

	console.debug("loginctrl created");
	return obj;
});
