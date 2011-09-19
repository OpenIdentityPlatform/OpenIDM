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

openidm.app.components.nav.RegistrationView = function() {

	var getRegisterButton = function() {
		return $("#registration input[name='register']");
	};

	var getUser = function() {
		var user = {
			name: $("#registration input[name='firstName']").val(),
			firstname: $("#registration input[name='firstName']").val(),
			lastname: $("#registration input[name='lastName']").val(),
			email: $("#registration input[name='email']").val(),
			password: $("#registration input[name='password']").val()
		};

		return user;
	};

	var getFirstNameInput = function() {
		return $("#registration input[name='firstName']");
	}

	var getLastNameInput = function() {
		return $("#registration input[name='lastName']");
	}

	var getEmailInput = function() {
		return $("#registration input[name='email']");
	}

	var getEmailConfirmInput = function() {
		return $("#registration input[name='emailConfirm']");
	}

	var getPasswordInput = function() {
		return $("#registration input[name='password']");
	}

	var getPasswordConfirmInput = function() {
		return $("#registration input[name='passwordConfirm']");
	}

	var areTermsOfUseAgreed = function() {
		return $("#registration input[name='terms']").is(':checked');
	}

	var show = function(callback) {
		console.log("showing registration form");

		$.ajax({
			  url: "js/app/comp/user/templates/RegistrationTemplate.html",
			  dataType: "text",
			  success: function(data){
				  $("#content").fadeOut(100, function() {
								$(this).html(data).fadeIn(100);
								callback();
							});
			  }
			});
		$('#nav-content > span').html('');
	};

	return {
		getRegisterButton: getRegisterButton,
		getUser: getUser,
		getFirstNameInput: getFirstNameInput,
		getLastNameInput: getLastNameInput,
		getEmailInput: getEmailInput,
		getEmailConfirmInput: getEmailConfirmInput,
		getPasswordInput: getPasswordInput,
		getPasswordConfirmInput: getPasswordConfirmInput,
		areTermsOfUseAgreed: areTermsOfUseAgreed,
		show: show
	}
} ();

