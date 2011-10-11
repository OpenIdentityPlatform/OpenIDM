define(["app/comp/user/views/ResetPasswordDialogView",
        "app/comp/user/delegates/UserDelegate",
        "app/comp/main/controllers/MessagesCtrl", 
        "app/comp/user/controllers/ChangePasswordDialogCtrl",
        "app/util/Validator"], 
        function(resetPasswordDialogView, 
        		userDelegate, 
        		messagesCtrl, 
        		changePasswordCtrl,
        		validator)
        		{

	var obj = {};
	
	obj.view = resetPasswordDialogView;
	obj.changeCtrl = changePasswordCtrl;
	obj.delegate = userDelegate;
	obj.messages = messagesCtrl;

	obj.validators = new Array();
	obj.userLogin = null;
	
	obj.init = function() {
		var self = this;
		
		obj.view.show(function() {
			self.registerValidators();

			self.view.getCloseButton().bind('click', function(event) {
				self.view.close();
			});
			
			self.view.getConfirmButton().bind('click', function(event) {
				event.preventDefault();
				obj.sendTokenButtonClicked();
			});
		});
	}
	
	
	obj.registerValidators = function() {
		console.log("register forgotten dialog validators");
		obj.validators[0] = new openidm.validator([obj.view.getEmailInput()], [new openidm.condition('email', openidm.validator.emailValidator)], 'keyup', 'simple', obj.validateForm);
	}

	obj.validateForm = function() {
		console.log('validate form');

		var allOk = true;
		//var i;
		for (i = 0; i < obj.validators.length; i++) {
			if (obj.validators[i].isOk() == false) {
				allOk = false;
				console.log('validate false');
				break;
			}
		}
	 	
		if (allOk) {
			console.log('validate true');
			obj.view.enableSaveButton();
		} else if (!allOk) {
			obj.view.disableSaveButton();
		}

		return allOk;
	}


	obj.sendTokenButtonClicked = function() {
		var self = this;
		
		for (k = 0; k < obj.validators.length; k++) {
			obj.validators[k].validate();
		}

		if (obj.validateForm() == true) {
			
			obj.delegate.getUser(obj.view.getEmailInput().val(), function(r) {
			if (r.email == self.view.getEmailInput().val()) {
				alert('Token will be send. You should check your email and click into link. It will redirect you to password change site.');
				obj.changeCtrl.init(r.email,r.password);
			} else {
				// never happen
			}

		}, function(r) {
			self.messages.displayMessageOn('info', 'General error','#messages');
		});
			
		}
	}
	
	console.log("Reset Password ctrl Created");
	return obj;

});
