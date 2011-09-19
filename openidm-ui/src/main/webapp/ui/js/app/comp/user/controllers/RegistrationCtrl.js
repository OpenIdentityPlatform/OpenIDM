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

openidm.app.components.nav.RegistrationCtrl = function() {

	var messages = openidm.app.components.nav.MessagesCtrl;
	var view = openidm.app.components.nav.RegistrationView;
	var delegate = openidm.app.components.data.UserDelegate;

	var validators = new Array();

	var registerButtonEnabled = false;


	var init = function() {
		var self = this;

		console.log("RegistrationCtrl.init()");

		view.show(function() {
			console.log("registering validators");
			self.disableRegisterButton();
			self.registerValidators();
			console.log("registering validators end");
		});
	};

	var enableRegisterButton = function() {
		var self = this;

		console.log("Enabling register button");

		self.view.getRegisterButton().removeAttr('disabled');

		self.view.getRegisterButton().bind('click', function(event) {
			event.preventDefault();

			self.disableRegisterButton();

			self.afterRegisterButtonClicked(event);
		});

		registerButtonEnabled = true;
	};

	var registerValidators = function() {
		var self = this;

		validators[0] = new openidm.validator([view.getFirstNameInput()], function(text) {
			if (text[0].length < 3) {
				return "Too short.";
			}
		},
		self.validateForm);

		validators[1] = new openidm.validator([view.getLastNameInput()], function(text) {
			if (text[0].length < 3) {
				return "Too short.";
			}
		},
		self.validateForm);

		validators[2] = new openidm.validator([view.getEmailInput()], function(text) {
			if (text[0].length < 3) {
				return "Too short.";
			}
		},
		self.validateForm);

		validators[3] = new openidm.validator([view.getEmailConfirmInput(), view.getEmailInput()], function(text) {
			if (text[0] != text[1]) {
				return "Email have to be equal.";
			}
		},
		self.validateForm);

		validators[2] = new openidm.validator([view.getPasswordInput()], function(text) {
			if (text[0].length < 3) {
				return "Too short.";
			}
		},
		self.validateForm);

		validators[3] = new openidm.validator([view.getPasswordConfirmInput(), view.getPasswordInput()], function(text) {
			if (text[0] != text[1]) {
				return "Passwords have to be equal.";
			}
		},
		self.validateForm);
	};

	/**
	 * Callback function
	 */
	var validateForm = function() {
		var allOk = true;

		for (i = 0; i < validators.length; i++) {
			if (validators[i].isOk() == false) {
				allOk = false;
				break;
			}
		}

		if (allOk && !registerButtonEnabled) {
			openidm.app.components.nav.RegistrationCtrl.enableRegisterButton();
		} else if (!allOk && registerButtonEnabled) {
			openidm.app.components.nav.RegistrationCtrl.disableRegisterButton();
		}
	};

	var disableRegisterButton = function() {
		console.log("Disabling register button");

		view.getRegisterButton().unbind();
		view.getRegisterButton().attr('disabled', 'disabled');

		registerButtonEnabled = false;
	};

	var afterRegisterButtonClicked = function(event) {
		var self = this;

		console.log("RegistrationCtrl.afterRegisterButtonClicked()");

		if (view.areTermsOfUseAgreed() == false) {
			messages.displayMessage('info', 'You must accept terms of use');
			self.enableRegisterButton();
		} else {
			delegate.addUser(view.getUser(), function(r) {
				messages.displayMessage('info', 'User has been registered successfully');
				view.close();
			}, function(r) {
				var response = eval('(' + r.responseText + ')');

				console.log(response);
				if (response.error == 'Conflict') {
					messages.displayMessage('error', 'There is already such user');
				} else {
					messages.displayMessage('error', 'Unknown error');
				}

				self.enableRegisterButton();
			});
		}
	};

	console.log("RegistrationCtrl created");

	return {
		init: init,
		enableRegisterButton: enableRegisterButton,
		registerValidators: registerValidators,
		validateForm: validateForm,
		disableRegisterButton: disableRegisterButton,
		afterRegisterButtonClicked: afterRegisterButtonClicked,
		view: view
	}
} ();

