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
define(function () {
	var obj = {};

	obj.getUser = function() {
		var user = {
			name : obj.getEmailInput().val(),
			firstname : obj.getFirstNameInput().val(),
			lastname : obj.getLastNameInput().val(),
			email : obj.getEmailInput().val(),
			address1 : obj.getAddress1Input().val(),
			address2 : obj.getAddress2Input().val(),
			city : obj.getCityInput().val(),
			postalcode : obj.getPostalCodeInput().val(),
			phonenumber : obj.removeInvalidCharsFromPhoneNmber(obj.getPhoneNumberInput().val()),
			country : obj.getCountryInput().val(),
			state_province : obj.getStateProvinceInput().val(),
			securityquestion: obj.getSecurityQuestion().val(),
			securityanswer: obj.getSecurityAnswer().val()
		};
		return user;
	}
	
	obj.removeInvalidCharsFromPhoneNmber = function(phoneNumber){
		return phoneNumber.split(' ').join('').split('-').join('');
	}

	obj.setUserName = function(name) {
		$("#profileInfo h1").html(name);
	}

	obj.getEmailInput = function() {
		return $("#profileData input[name='email']");
	}

	obj.getPasswordInput = function() {
		return $("#profileData input[name='password']");
	}
	
	obj.getCountryInput = function() {
		return $('#profile select[name="country"]')
	}
	
	obj.getStateProvinceInput = function() {
		return $("#profile select[name='stateProvince']")
	}
	
	obj.getFirstCoutryOption = function() {
		return $("#profile select[name='country'] > option:first")
	}
	
	obj.getFirstStateProvinceOption = function() {
		return $("#profile select[name='stateProvince'] > option:first")
	}
	
	

	obj.show = function(showCallback) {
		console.log("showing profile form");

		$.ajax({
			type : "GET",
			url : "js/app/comp/user/templates/ProfileTemplate.html",
			dataType : "html",
			success : function(data) {
				$("#content").fadeOut(100, function() {
					$(this).html(data);
					$(this).fadeIn(100);
					showCallback();
				});
			},
			error : showCallback
		});
	}

	obj.setEditMode = function(edit) {
		$("#profileData input[type!='button']").add("#profileData select").not("[name='email']").each(
				function() {
					if (edit == true) {
						$(this).removeClass('readOnly').removeAttr('disabled');
					} else {
						$(this).addClass('readOnly').attr('disabled','disabled');
					}
				});
		
		$("#profileData select option:selected").each(function() {
			if(edit == true) {
				$(this).parent().removeClass('nonvisible');
			} else {
				if( $(this).text() == 'Please Select' ) {
					$(this).parent().addClass('nonvisible');
				}
			}
		});
		
		if (edit == true) {
			obj.getEditButton().fadeOut(200, function() {
				$("#actions").fadeIn(200);
			});
		} else {
			$("#actions").fadeOut(200, function() {
				obj.getEditButton().fadeIn(200);
			});
		}
	}

	obj.getEditButton = function() {
		return $("#profile input[name='editButton']");
	}

	obj.getCancelButton = function() {
		return $("#profile input[name='cancelButton']");
	}

	obj.getSaveButton = function() {
		return $("#profile input[name='saveButton']");
	}

	obj.getFirstNameInput = function() {
		return $("#profile input[name='firstName']");
	}

	obj.getLastNameInput = function() {
		return $("#profile input[name='lastName']");
	}

	obj.getAddress1Input = function() {
		return $("#profile input[name='address1']");
	}

	obj.getAddress2Input = function() {
		return $("#profile input[name='address2']");
	}

	obj.getCityInput = function() {
		return $("#profile input[name='city']");
	}

	obj.getPostalCodeInput = function() {
		return $("#profile input[name='postalCode']");
	}

	obj.getPhoneNumberInput = function() {
		return $("#profile input[name='phoneNumber']");
	}
	
	obj.getSecurityQuestion = function() {
		return $("#profile select[name='securityQuestion']");
	}
	
	obj.getSecurityAnswer = function() {
		return $("#profile input[name='securityAnswer']");
	}
	
	obj.enableSaveButton = function() {
		obj.getSaveButton().removeClass('ashy').addClass('orange');
		obj.getSaveButton().removeAttr('disabled');
	}

	obj.disableSaveButton = function() {
		obj.getSaveButton().removeClass('orange').addClass('ashy');
		obj.getSaveButton().attr('disabled', 'disabled');
	}
	
	obj.getSelects = function() {
		return $("#profile select");		
	}
	
	return obj;

});
