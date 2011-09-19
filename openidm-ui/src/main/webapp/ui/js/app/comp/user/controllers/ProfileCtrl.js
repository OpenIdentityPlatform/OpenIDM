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

openidm.app.components.nav.ProfileCtrl = function() {

	var messages = openidm.app.components.nav.MessagesCtrl;
	var view = openidm.app.components.nav.ProfileView;
	var delegate = openidm.app.components.data.UserDelegate;
	var breadcrumbs = openidm.app.components.nav.BreadcrumbsCtrl;

	var user = null;

	var setUser = function(u) {
		self = this;

		self.user = u;
		self.reloadUser();
	}

	var reloadUser = function() {
		self = this;

		view.setUserName(self.user.firstname + " " + self.user.lastname);
		view.getEmailInput().val(self.user.email);
	}

	var init = function(callback) {
		var self = this;
		console.log("ProfileCtrl.init()");

		view.show(function() {
			self.registerListeners();
			callback();
		});
	}

	var registerListeners = function() {
		var self = this;

		console.log("ProfileCtrl.registerListeners()");

		view.getEditButton().bind('click', function(event) {
			event.preventDefault();

			self.view.setEditMode(true);
			self.breadcrumbs.set('Edit Profile');
		});

		view.getCancelButton().bind('click', function(event) {
			event.preventDefault();

			self.view.setEditMode(false);
			self.breadcrumbs.set('My Profile');
		});

		view.getSaveButton().bind('click', function(event) {
			event.preventDefault();
			console.log('saving');
			self.saveUser();
		});
	}

	var afterLoginButtonClicked = function(event) {
		var self = this;

		console.log("ProfileCtrl.afterLoginButtonClicked()");

		delegate.getUser(view.getLogin(), function(r) {
			if (r.password == self.view.getPassword()) {
				self.messages.displayMessage('info', 'You have been successfully logged in.');
				self.view.renderLogged();
				self.view.setUserName(self.view.getLogin());
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

	var saveUser = function() {
		var self = this;

		if( view.getEmailInput().val() != "" ) {
			self.user.email = view.getEmailInput().val();
		}

		if( view.getPasswordInput().val() != "" ) {
			self.user.password = view.getPasswordInput().val();
		}

		self.user._rev = parseInt(self.user._rev) + 1 + "";

		delegate.updateUser(self.user, function() {
			self.messages.displayMessage('info', 'Profile has been updated');
		}, function() {
			self.messages.displayMessage('error', 'Problem with updating profile');
		});
	}

	console.log("ProfileCtrl created");

	return {
		init: init,
		registerListeners: registerListeners,
		afterLoginButtonClicked: afterLoginButtonClicked,
		view: view,
		messages: messages,
		setUser: setUser,
		reloadUser: reloadUser,
		saveUser: saveUser,
		breadcrumbs: breadcrumbs
	}
} ();

