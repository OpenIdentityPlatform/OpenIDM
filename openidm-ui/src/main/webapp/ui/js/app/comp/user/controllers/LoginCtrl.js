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
registerNS("openidm.app.components.nav");

openidm.app.components.nav.LoginCtrl = function() {

	var messages = openidm.app.components.nav.MessagesCtrl;
	var view = openidm.app.components.nav.LoginView;
	var delegate = openidm.app.components.data.UserDelegate;
	var breadcrumbs = openidm.app.components.nav.BreadcrumbsCtrl;

	var init = function() {
		var self = this;
		console.log("LoginCtrl.init()");

		view.renderLogin();
		self.registerListeners();
	}

	var registerListeners = function() {
		var self = this;

		console.log("LoginCtrl.registerListeners()");

		view.getLoginButton().removeAttr('disabled');

		view.getLoginButton().bind('click', function(event) {
			event.preventDefault();

			self.view.getLoginButton().unbind();
			self.view.getLoginButton().attr('disabled', 'disabled');

			self.afterLoginButtonClicked(event);
		});

		view.getLogoutButton().bind('click', function(event) {
			event.preventDefault();
			self.view.renderLogin();

			openidm.app.components.main.MainCtrl.clearContent();
			self.breadcrumbs.set('');
		});

		view.getRegisterButton().bind('click', function(event) {
			event.preventDefault();
			console.log('asdasd');
			openidm.app.components.nav.RegistrationCtrl.init();

			self.breadcrumbs.set('Registration');
		});
	}

	var afterLoginButtonClicked = function(event) {
		var self = this;

		console.log("LoginCtrl.afterLoginButtonClicked()");

		delegate.getUser(view.getLogin(), function(r) {
			if (r.password == self.view.getPassword()) {
				self.messages.displayMessage('info', 'You have been successfully logged in.');
				self.breadcrumbs.set('My Profile');
				self.view.renderLogged();
				self.view.setUserName(self.view.getLogin());

				openidm.app.components.nav.ProfileCtrl.init(function() {
					openidm.app.components.nav.ProfileCtrl.setUser(r);
				});
			} else {
				self.messages.displayMessage('info', 'Incorrect password.');
			}

			self.registerListeners();
			self.view.untoggle();
		}, function(r) {
			self.messages.displayMessage('info', 'Incorrect login.');
			self.registerListeners();
		});
	}

	console.log("LoginCtrl created");

	return {
		init: init,
		registerListeners: registerListeners,
		afterLoginButtonClicked: afterLoginButtonClicked,
		view: view,
		messages: messages,
		breadcrumbs: breadcrumbs
	}
} ();

