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
define(["app/comp/main/controllers/DialogsCtrl"],function(dialogsCtrl) {
	var obj = {};

	obj.dialogs = dialogsCtrl;

	obj.getRegisterButton = function() {
		return $("#registration input[name='register']");
	};

	obj.getUser = function() {
		var user = {
				name : $("#registration input[name='email']").val(),
				firstname : $("#registration input[name='firstName']").val(),
				lastname : $("#registration input[name='lastName']").val(),
				email : $("#registration input[name='email']").val(),
				password : $("#registration input[name='password']").val(),
				//TODO: Until 'replace' won't add new values
				address1 : "",
				address2 : "",
				city : "",
				postalcode : "",
				phonenumber : "",
				country : "",
				state_province : "",
				securityquestion : $("#registration select[name='securityQuestion']").val(),
				securityanswer : $("#registration input[name='securityAnswer']").val()
		};

		return user;
	};

	obj.getFirstNameInput = function() {
		return $("#registration input[name='firstName']");
	}

	obj.getLastNameInput = function() {
		return $("#registration input[name='lastName']");
	}

	obj.getEmailInput = function() {
		return $("#registration input[name='email']");
	}

	obj.getPasswordInput = function() {
		return $("#registration input[name='password']");
	}

	obj.getPasswordConfirmInput = function() {
		return $("#registration input[name='passwordConfirm']");
	}

	obj.getTermsOfUseCheckbox = function() {
		return $("#registration input[name='terms']");
	}

	obj.areTermsOfUseAgreed = function() {
		return $("#registration input[name='terms']").is(':checked');
	}

	obj.getSecurityQuestion = function() {
		return $("#registration select[name='securityQuestion']");
	}
	
	obj.getSecurityAnswer = function() {
		return $("#registration input[name='securityAnswer']");
	}
	
	obj.show = function(callback) {
		console.log("showing registration form");

		$.ajax({
			type : "GET",
			url : "js/app/comp/user/templates/RegistrationTemplate.html",
			dataType : "html",
			success : function(data) {
				$("#content").fadeOut(100, function() {
					$(this).html(data);
					$(this).fadeIn(100);
					callback();
				});
			},
			error : callback
		});

		$('#nav-content > span').html('');
	};

	obj.showTermsOfUseDialog = function() {
		var self = this;

		$.get("js/app/comp/user/templates/TermOfUseTemplate.html", function(data) {
			self.dialogs.setContent(data);
			self.dialogs.setActions("<input type='button' name='dialogClose' id='dialogClose' class='button gray floatRight' value='Close' />");
			$("#dialogClose").bind('click', function(event) {
				self.dialogs.close();
			});
			self.dialogs.show();
		});		
	}

	obj.enableRegisterButton = function() {
		obj.getRegisterButton().removeClass('gray').addClass('orange');
	}

	obj.disableRegisterButton = function() {
		obj.getRegisterButton().removeClass('orange').addClass('gray');
	}

	return obj;
});
