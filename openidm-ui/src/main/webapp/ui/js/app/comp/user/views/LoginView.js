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

openidm.app.components.nav.LoginView = function() {

	var isOverLogin = false;

	var renderLogin = function() {
		var self = this;

		$("#loginContent").fadeOut(200, function() {
			$("#loginBox").fadeIn(200);
		});

		$("#loginForm").mouseenter(function() {
			isOverLogin = true;
		});

		$("#loginForm").mouseleave(function() {
			isOverLogin = false;
		});

		$("#loginForm input[type!=submit]").mousedown(function() {
			self.toggle();
		});

		$("#container").mousedown(function(event) {
			if (!isOverLogin) {
				self.untoggle();
			}
		});
	}

	var renderLogged = function() {
		$("#loginBox").fadeOut(200, function() {
			$("#loginContent").fadeIn(200);
		});
	}

	var getLoginButton = function() {
		return $("#loginForm input[name='loginButton']");
	}

	var getLogoutButton = function() {
		return $("#logout_link");
	}

	var getRegisterButton = function() {
		return $("#loginBox input[name='registerButton']");
	}

	var getLogin = function() {
		return $("#loginForm input[name='loginText']").val();
	}

	var getPassword = function() {
		return $("#loginForm input[name='loginPassword']").val();
	}

	var untoggle = function() {
		$("#loginForm").removeClass("login_toggle");
		$("#loginRemember").hide();
	}

	var toggle = function() {
		$("#loginForm").addClass('login_toggle');
		$("#loginRemember").show();
	}

	var setUserName = function(name) {
		$("#user_name").html(name);
	}

	return {
		renderLogin: renderLogin,
		renderLogged: renderLogged,
		getLoginButton: getLoginButton,
		getLogoutButton: getLogoutButton,
		getRegisterButton: getRegisterButton,
		getLogin: getLogin,
		getPassword: getPassword,
		untoggle: untoggle,
		toggle: toggle,
		setUserName: setUserName
	}
} ();

