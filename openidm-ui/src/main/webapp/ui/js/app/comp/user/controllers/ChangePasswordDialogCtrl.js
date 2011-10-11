define(["app/comp/user/views/ChangePasswordDialogView",
        "app/comp/user/delegates/UserDelegate",
        "app/comp/main/controllers/MessagesCtrl", 
        "app/util/Validator"], 
        function(changePasswordDialogView, 
        		userDelegate, 
        		messagesCtrl, 
        		validator)
        		{
	var obj = {};

	obj.view = changePasswordDialogView;
	obj.delegate = userDelegate;
	obj.messages = messagesCtrl;

	obj.validators = new Array();
	obj.user = null;
	obj.oldPassword = null;

	obj.init = function(id, oldPass) {
		var self = this;

		obj.user = id;
		obj.oldPassword = oldPass

		obj.view.show(function() {
			self.registerValidators();

			self.view.getCloseButton().bind('click', function(event) {
				self.view.close();
			});

			self.view.getSaveButton().bind('click', function(event) {
				event.preventDefault();

				self.afterSaveButtonClicked();
			});
		});
	}

	obj.getOldPassword = function() {
		return obj.oldPassword;		
	}

	obj.getUser = function() {
		return obj.user;		
	}

	obj.registerValidators = function() {
		var self = this;

		console.log("register dialog validators");

		obj.validators[0] = new openidm.validator([ obj.view.getPasswordInput() ], [
		                                                                            new openidm.condition('min-8', function(inputs) {
		                                                                            	if (inputs[0].val().length < 7) {
		                                                                            		return "At least 8 characters length";
		                                                                            	}
		                                                                            }), new openidm.condition('one-capital', function(inputs) {
		                                                                            	var reg = /[(A-Z)]+/;

		                                                                            	if (!reg.test(inputs[0].val())) {
		                                                                            		return "At lest one capital letter";
		                                                                            	}
		                                                                            }), new openidm.condition('one-number', function(inputs) {
		                                                                            	var reg = /[(0-9)]+/;

		                                                                            	if (!reg.test(inputs[0].val())) {
		                                                                            		return "At lest one number";
		                                                                            	}
		                                                                            }), new openidm.condition('new', function(inputs) {
		                                                                            	if (inputs[0].val() == "" || inputs[0].val() == self.getOldPassword()) {
		                                                                            		return "Not equal old password";
		                                                                            	}
		                                                                            }), new openidm.condition('not-equal-username', function(inputs) {
		                                                                            	if (inputs[0].val() == "" || inputs[0].val() == self.getUser()) {
		                                                                            		return "Not equal username";
		                                                                            	}
		                                                                            }) ], 'keyup', 'advanced', self.validateForm);

		obj.validators[1] = new openidm.validator([ obj.view.getPasswordConfirmInput(),
		                                            obj.view.getPasswordInput() ], [ new openidm.condition('same',
		                                            		function(inputs) {
		                                            	if (inputs[0].val() == "" || inputs[0].val() != inputs[1].val()) {
		                                            		return "Passwords have to be equal.";
		                                            	}
		                                            }) ], 'keyup', 'advanced', self.validateForm);
		
		obj.validators[2] = new openidm.validator([ obj.view.getOldPasswordInput() ], [ 
		        new openidm.condition('old-password', function(inputs) {
		        	if (inputs[0].val() == "" || inputs[0].val() != self.getOldPassword()) {
		        		return "Incorrect old password.";
		        	}
		        }) ], 'keyup', 'advanced', self.validateForm);
	}

	obj.validateForm = function() {
		console.log('validate all form');

		var allOk = true;

		for (i = 0; i < obj.validators.length; i++) {
			if (obj.validators[i].isOk() == false) {
				allOk = false;
				break;
			}
		}

		if (allOk) {
			obj.view.enableSaveButton();
		} else if (!allOk) {
			obj.view.disableSaveButton();
		}

		return allOk;
	}

	obj.afterSaveButtonClicked = function() {
		for (k = 0; k < obj.validators.length; k++) {
			obj.validators[k].validate();
		}

		if (obj.validateForm() == true) {
			obj.delegate.changeField(obj.user, "password", obj.view.getPasswordInput().val(),
					function(r) {
				obj.messages.displayMessage('info','Password has been changed');

				obj.view.close();
			}, function(r) {
				obj.messages.displayMessage('error', 'Unknown error');

				obj.view.close();
			});
		}
	}
	return obj;
});

