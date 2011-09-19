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

openidm.app.components.nav.ProfileView = function() {

	var setUserName = function(name	) {
		$("#profileInfo h1").html(name);
	}

	var getEmailInput = function() {
		return $("#profileData input[name='email']");
	}

	var getPasswordInput = function() {
		return $("#profileData input[name='password']");
	}

	var show = function(callback) {
		console.log("showing profile form");
		
		$.ajax({
			  url: "js/app/comp/user/templates/ProfileTemplate.html",
			  dataType: "text",
			  success: function(data){
				  $("#content").fadeOut(100, function() {
								$(this).html(data).fadeIn(100);
								callback();
							});
			  }
			});
	}

	var setEditMode = function( edit ) {
		$("#profileData input[type!='button']").each(function() {
			if( edit == true ) {
				$(this).removeClass('readOnly').removeAttr('disabled');
			} else {
				$(this).addClass('readOnly').attr('disabled','disabled');
			}
		});

		if( edit == true ) {
			getEditButton().fadeOut(200, function() {
				$("#actions").fadeIn(200);
			});
		} else {
			$("#actions").fadeOut(200, function() {
				getEditButton().fadeIn(200);
			});
		}
	}

	var getEditButton = function() {
		return $("#profile input[name='editButton']");
	}

	var getCancelButton = function() {
		return $("#profile input[name='cancelButton']");
	}

	var getSaveButton = function() {
		return $("#profile input[name='saveButton']");
	}

	return {
		getEmailInput: getEmailInput,
		getPasswordInput: getPasswordInput,
		getEditButton: getEditButton,
		setEditMode: setEditMode,
		getCancelButton: getCancelButton,
		getSaveButton: getSaveButton,
		show: show,
		setUserName: setUserName
	}
} ();

