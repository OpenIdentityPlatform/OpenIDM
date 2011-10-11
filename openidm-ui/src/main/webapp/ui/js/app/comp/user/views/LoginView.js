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
define(function() {
	var obj = {};

	obj.isOverLogin = false;

	obj.renderLogin = function() {
		var self = this;

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

		$("#container").mousedown(function(event) {
			if (!obj.isOverLogin) {
				self.untoggle();
			}
		});
	}

	obj.renderLogged = function() {
		$("#loginBox").fadeOut(200, function() {
			$("#loginContent").fadeIn(200);
		});
	}

	obj.getLoginButton = function() {
		return $("#loginForm input[name='loginButton']");
	}
	
	obj.enableLoginButton = function() {
		obj.getLoginButton().removeClass('gray').addClass('orange');		
	}
	
	obj.disableLoginButton = function() {
		obj.getLoginButton().removeClass('orange').addClass('gray');		
	}

	obj.getProfileButton = function() {
		return $("#profile_link");
	}

	obj.getLogoutButton = function() {
		return $("#logout_link");
	}

	obj.getProfileButton = function() {
		return $("#profile_link");
	}

	obj.getRegisterButton = function() {
		return $("#loginBox input[name='registerButton']");
	}

	obj.getLogin = function() {
		return $("#loginForm input[name='loginText']").val();
	}

	obj.getPassword = function() {
		return $("#loginForm input[name='loginPassword']").val();
	}
	
	obj.getLoginInput = function() {
		return $("#loginForm input[name='loginText']");		
	}
	
	obj.getPasswordInput = function() {
		return $("#loginForm input[name='loginPassword']");
	}

	obj.untoggle = function() {
		$("#loginForm").removeClass("login_toggle");
		$("#loginRemember").hide();
	}

	obj.toggle = function() {
		$("#loginForm").addClass('login_toggle');
		$("#loginRemember").show();
	}

	obj.setUserName = function(name) {
		$("#user_name").html(name);
	}

	return obj;
}); 

