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
define(["app/comp/main/controllers/DialogsCtrl"], function(dialogsCtrl) {

	var obj = {};

	obj.dialog = dialogsCtrl;

	obj.getSaveButton = function() {
		return $("#dialog input[name='dialogOk']");
	};

	obj.getCloseButton = function() {
		return $('#dialogClose');
	}

	obj.getPasswordInput = function() {
		return $("#dialog input[name='password']");
	}
	
	obj.getOldPasswordInput = function() {
		return $("#dialog input[name='passwordOld']");
	}

	obj.getPasswordConfirmInput = function() {
		return $("#dialog input[name='passwordConfirm']");
	}

	obj.show = function(callback) {
		self = this;

		console.log("showing change password dialog");
		openidm.fillTemplateWithData("js/app/comp/user/templates/PasswordChangeTemplate.html",null, function(data) {
			self.dialog.setContent(data);
			self.dialog.setActions("<input type='button' name='dialogClose' id='dialogClose' class='button gray floatRight' value='Close' /><input type='button' name='dialogOk' id='dialogOk' class='button gray floatRight' value='Update my new password' />");
			self.dialog.setWidth(800);
			self.dialog.setHeight(270);
			self.dialog.show();
			callback();
		});
	};

	obj.close = function() {
		obj.dialog.close();
	}

	obj.enableSaveButton = function() {
		obj.getSaveButton().removeClass('gray').addClass('orange');
	}

	obj.disableSaveButton = function() {
		obj.getSaveButton().removeClass('orange').addClass('gray');
	}

	return obj;

});

