define(["app/comp/main/controllers/MessagesCtrl",
        "app/comp/user/views/RegistrationView",
        "app/comp/user/delegates/UserDelegate",
        "app/util/Validator",
        "app/util/Validators",
        "app/comp/user/controllers/ForgottenPasswordDialogCtrl"], function(messagesCtrl,registrationView,userDelegate, validator,validators,forgottenPasswordDialog) {
		var obj = {};
		
		obj.messages = messagesCtrl;
		obj.view = registrationView;
		obj.delegate = userDelegate;

		obj.validators = new Array();

		obj.init = function() {
			var self = this;

			console.log("RegistrationCtrl.init()");
			
			obj.view.show(function() {
				self.view.disableRegisterButton();
				
				self.registerListeners();
				self.registerValidators();

				$("#termOfUseLink").bind('click', function(event) {
					self.view.showTermsOfUseDialog();
				});
				
				self.setSecurityQuestionSelect();
			});
		};

		obj.registerValidators = function() {
			var self = this;

			obj.validators[0] = new openidm.validator([obj.view.getFirstNameInput()], [new openidm.condition('letters-only', openidm.validator.nameValidator)], 'change', 'simple', self.validateForm);

			obj.validators[1] = new openidm.validator([obj.view.getLastNameInput()], obj.validators[0].conditions, 'change', 'simple', obj.validateForm);

			obj.validators[2] = new openidm.validator([obj.view.getEmailInput()], [new openidm.condition('email', openidm.validator.emailValidator), 
			                                                                       new openidm.condition('unique', openidm.validator.uniqueEmailValidator)], 'focusout', 'simple', obj.validateForm);


			obj.validators[3] = new openidm.validator([obj.view.getPasswordInput(), obj.view.getEmailInput()], [new openidm.condition('min-8', function(inputs) {
				if(inputs[0].val().length < 7) {
					return "At least 8 characters length";
				}
			}), new openidm.condition('one-capital', function(inputs) {
				var reg = /[(A-Z)]+/;

				if( !reg.test(inputs[0].val()) ) {
					return "At least one capital letter";
				}
			}), new openidm.condition('one-number', function(inputs) {
				var reg = /[(0-9)]+/;

				if( !reg.test(inputs[0].val()) ) {
					return "At least one number";
				}
			}), new openidm.condition('not-equal-username', function(inputs) {
				if( inputs[0].val() == "" || inputs[0].val() == inputs[1].val() ) {
					return "Cannot match login";
				}
			})], 'keyup', 'advanced', obj.validateForm);


			obj.validators[4] = new openidm.validator([obj.view.getPasswordConfirmInput(), obj.view.getPasswordInput()], [new openidm.condition('same', function(inputs) {
				if (inputs[0].val() == "" || inputs[0].val() != inputs[1].val()) {
					return "Passwords have to be equal.";
				}
			})], 'keyup', 'advanced', obj.validateForm);

			obj.validators[5] = new openidm.validator([obj.view.getTermsOfUseCheckbox()], [new openidm.condition('tou', function(inputs) {
				if ( !inputs[0].is(':checked') ) {
					return "Acceptance required for registration";
				}
			})], 'click', 'simple', obj.validateForm);
//			TODO: valid dla pytania
//			obj.validators[6] = new openidm.validator([obj.view.getSecurityAnswer()], WALIDATOR_NOT_EMPTY, 'change', 'simple', obj.validateForm);

		};

		/**
		 * Callback function
		 */
		obj.validateForm = function() {
			var allOk = true;

			for (i = 0; i < obj.validators.length; i++) {
				if (obj.validators[i].isOk() == false) {
					allOk = false;
					break;
				}
			}

			if (allOk) {
				obj.view.enableRegisterButton();
			} else if (!allOk) {
				obj.view.disableRegisterButton();
			}

			$("#frgtPasswrdSelfReg").bind('click', function(event) {
				forgottenPasswordDialog.init(obj.view.getEmailInput().val());
			});
			
			return allOk;
		};

		obj.registerListeners = function() {
			var self = this;
			
			obj.view.getRegisterButton().unbind();
			obj.view.getRegisterButton().bind('click', function(event) {
				event.preventDefault();
				
				self.view.getRegisterButton().unbind();
				self.view.getRegisterButton().bind('click', function(event){event.preventDefault()});
				
				self.afterRegisterButtonClicked(event);
			});
		};

		obj.setSecurityQuestionSelect = function(question) {
			self = this;
			
			$.ajax({
				type : "GET",
				url : "js/app/tmp/secquestions.json",
				dataType : "json",
				success : function(data) {
					obj.view.getSecurityQuestion().loadSelect(data);
					obj.view.getSecurityQuestion().val(question);
					
				},
				error : function(xhr) {
					console.log('Error: ' + xhr.status + ' ' + xhr.statusText)
				}
			});
		}

		
		obj.afterRegisterButtonClicked = function(event) {
			var self = this;

			console.log("RegistrationCtrl.afterRegisterButtonClicked()");

			for( k= 0; k < obj.validators.length; k++ ) {
				obj.validators[k].validate();
			}

			if( obj.validateForm() == true ) {
				obj.delegate.addUser(obj.view.getUser(), function(r) {
					self.messages.displayMessage('info', 'User has been registered successfully');

					//TODO cycle 
					require("app/comp/user/controllers/LoginCtrl").loginUser(self.view.getUser());
				}, function(r) {
					var response = eval('(' + r.responseText + ')');

					console.log(response);
					if (response.error == 'Conflict') {
						self.messages.displayMessage('error', 'User already exists');
					} else {
						self.messages.displayMessage('error', 'Unknown error');
					}
					
					self.registerListeners();
				});
			} else {
				obj.registerListeners();
			}
		};

		console.log("RegistrationCtrl created");

		return obj;
});

