define(["app/comp/user/views/ForgottenPasswordDialogView",
        "app/comp/user/delegates/UserDelegate",
        "app/comp/main/controllers/MessagesCtrl", 
        "app/comp/user/controllers/ResetPasswordDialogCtrl", 
        "app/util/Validator"], 
        function(forgottenPasswordDialogView, 
        		userDelegate, 
        		messagesCtrl, 
        		resetPasswordDialogCtrl,
        		validator)
        		{

	var obj = {};
	
	obj.view = forgottenPasswordDialogView;
	obj.delegate = userDelegate;
	obj.messages = messagesCtrl;
	obj.resetPasswordDialog = resetPasswordDialogCtrl; 
	obj.validatorsEmail = new Array();
	obj.validatorsQuestion = new Array();
	obj.userLogin = null;
	
	obj.validIdx = 0;
	
	obj.init = function(email) {
		var self = this;
		obj.userLogin = email;
		
		obj.view.show(function() {
			self.registerValidatorsEmail();

			self.view.getCloseButton().bind('click', function(event) {
				self.view.close();
			});

			self.view.getConfirmButton().bind('click', function(event) {
				self.changePassword();
			});
			
			obj.view.showEmail();

			self.view.getPasswordResetLink().bind('click', function(event) {
				self.resetPasswordDialog.init();
			});
			
			self.view.getEmailInput().val(obj.userLogin);

			if(self.view.getEmailInput().val() != null && self.view.getEmailInput().val() != '') {
				self.setQuestionPhase();
			}
			
		});
		
	},
	
	obj.changePassword = function() {
		console.log("changing password");
		
		obj.delegate.changeField(obj.view.getEmailInput().val(), "password", obj.view.getPasswordInput().val(),
				function(r) {
			obj.messages.displayMessage('info','Password has been changed');

			obj.view.close();
		}, function(r) {
			obj.messages.displayMessage('error', 'Unknown error');

			obj.view.close();
		});
	},
	
	
	obj.registerValidatorsEmail = function() {
		console.log("registerValidatorsEmail");
		obj.validatorsEmail[0] = new openidm.validator([obj.view.getEmailInput()], [new openidm.condition('email', openidm.validator.emailValidator), 
		                                                                       new openidm.condition('nonunique', openidm.validator.nonuniqueEmailValidator)], 'focusout', 'simple', obj.validateEmailForm);
	},

	obj.registerValidatorsQuestion = function() {
		console.log("registerValidatorsQuestion");
		obj.validatorsQuestion[0] = new openidm.validator([obj.view.getEmailInput(), obj.view.getFgtnSecurityAnswer()], [new openidm.condition('answer', openidm.validator.answerValidator)], 'focusout', 'simple', obj.validateQuestionForm);
		obj.validatorsQuestion[1] = new openidm.validator([ obj.view.getPasswordInput(),obj.view.getEmailInput() ], [
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
		                                                                            }), new openidm.condition('not-equal-username', function(inputs) {
		                                                                            	if (inputs[0].val() == "" || inputs[0].val() == inputs[1].val()) {
		                                                                            		return "Not equal username";
		                                                                            	}
		                                                                            }) ], 'focusout', 'advanced', obj.validateQuestionForm);

		obj.validatorsQuestion[2] = new openidm.validator([ obj.view.getPasswordConfirmInput(),
		                                            obj.view.getPasswordInput() ], [ new openidm.condition('same',
		                                            		function(inputs) {
		                                            	if (inputs[0].val() == "" || inputs[0].val() != inputs[1].val()) {
		                                            		return "Passwords have to be equal.";
		                                            	}
		                                            }) ], 'focusout', 'advanced', obj.validateQuestionForm);
		
	},
	
	obj.validateEmailForm = function() {
		console.log('validate email');

		var allOk = true;
		var i;
		for (i = 0; i < obj.validatorsEmail.length; i++) {
			if (obj.validatorsEmail[i].isOk() == false) {
				allOk = false;
				console.log('email validating false');
				obj.setEmailPhase();
				break;
			}
		}
	 	
		if (allOk) {
			console.log('email validating true');
			obj.setQuestionPhase();
		} else if (!allOk) {
			obj.setEmailPhase();
		}

		return allOk;
	},

	obj.validateQuestionForm = function() {
		console.log('validate question');
		var allOk = true;
		var i;
		for (i = 0; i < obj.validatorsQuestion.length; i++) {
			if (obj.validatorsQuestion[i].isOk() == false) {
				allOk = false;
				console.log('question validating false '+i);
				break;
			}
		}
	 	
		if (allOk) {
			console.log('question validating true');
			obj.view.enableSaveButton();
		} else if (!allOk) {
			console.log('question validating false');
			obj.view.disableSaveButton();
		}
		return allOk;
	},

	obj.afterQuestionButtonClicked = function() {
		var self = this;
		var k;
		for (k = 0; k < obj.validators.length; k++) {
			obj.validators[k].validate();
		}

		if (obj.validateForm() == true) {
			
			obj.delegate.getUser(obj.view.getEmailInput().val(), function(r) {
				if (r.securityanswer == self.view.getFgtnSecurityAnswer().val()) {
					obj.unlockPasswords();
					obj.registerValidatorsQuestion();
				} else {
					self.messages.displayMessageOn('info', 'Wrong answer.','#fgtnMessages');
					obj.lockPasswords();
				}
	
			}, function(r) {
				self.messages.displayMessageOn('info', 'A General error.','#fgtnMessages');
				obj.lockPasswords();
			});
		}
	},
	
	obj.setEmailPhase = function() {
//		obj.view.getEmailInput().removeAttr('disabled');
//		obj.view.getPasswordInput().attr('disabled','disabled');
//		obj.view.getPasswordConfirmInput().attr('disabled','disabled');
		console.log("setting email phase");
		obj.view.showEmail();

	},
	
	obj.setQuestionPhase = function() {
		obj.view.getEmailInput().attr('disabled','disabled');
//		obj.view.getPasswordInput().attr('disabled','disabled');
//		obj.view.getPasswordConfirmInput().attr('disabled','disabled');
		console.log("setting question phase "+obj.view.getEmailInput().val()+ ">   "+(obj.view.getEmailInput().val() != null && obj.view.getEmailInput().val() != ''));
		obj.view.showAnswer();
		if(obj.view.getEmailInput().val() != null && obj.view.getEmailInput().val() != '') {
			obj.delegate.getUser(obj.view.getEmailInput().val(), function(r) {
				if (r.email == obj.view.getEmailInput().val()) {
					obj.view.getFgtnSecurityQuestion().val(r.securityquestion);
					$.getJSON("js/app/tmp/secquestions.json", function(data){
				          $.each(data, function(i,item){
				        	  if(item.key == r.securityquestion) {
				        		  obj.view.getFgtnSecurityQuestion().text(item.value);
				        	  }
				            });
						
				          obj.registerValidatorsQuestion();
					});
				} else {
					obj.messages.displayMessageOn('info', 'Incorrect email/login.','#fgtnMessages');
				}
	
			}, function(r) {
				//obj.messages.displayMessageOn('info', 'User doesn\'t exist.','#fgtnMessages');
				obj.view.getEmailInput().removeAttr('disabled');
			});
		}
	},
	
	console.log("Forgotten Created");
	return obj;

});

