define(["app/comp/main/controllers/MessagesCtrl", 
        "app/comp/user/views/ProfileView", 
        "app/comp/user/delegates/UserDelegate",
        "app/comp/main/controllers/BreadcrumbsCtrl", 
        "app/comp/user/controllers/ChangePasswordDialogCtrl", 
        "app/util/Validators"], 
        function(messagesCtrl,
        		profileView,
        		userDelegate,
        		breadcrumbsCtrl,
        		changePasswordDialogCtrl, 
        		validators) 
        		{

	var obj = {}

	obj.messages = messagesCtrl;
	obj.view = profileView;
	obj.delegate = userDelegate;
	obj.breadcrumbs = breadcrumbsCtrl;
	obj.changePasswordDialog = changePasswordDialogCtrl;

	obj.validators = new Array();
	
	obj.editMode = false;
	obj.user = null;

	obj.setUser = function(u) {
		self = this;

		obj.user = u;
		obj.reloadUser();
	}

	obj.reloadUser = function() {
		obj.view.setUserName(obj.user.firstname + " " + obj.user.lastname);
		obj.view.getEmailInput().val(obj.user.email);

		obj.view.getFirstNameInput().val(obj.user.firstname);
		obj.view.getLastNameInput().val(obj.user.lastname);

		obj.view.getAddress1Input().val(obj.user.address1);
		obj.view.getAddress2Input().val(obj.user.address2);

		obj.view.getCityInput().val(obj.user.city);
		obj.view.getPostalCodeInput().val(obj.user.postalcode);
		obj.view.getPhoneNumberInput().val(obj.user.phonenumber);
		
		obj.setCountryAndStateProvince(obj.user.country, obj.user.state_province);
		obj.setSecurityQuestionSelect(obj.user.securityquestion);
		
		obj.view.getSecurityQuestion().val(obj.user.securityquestion);
		obj.view.getSecurityAnswer().val(obj.user.securityanswer);
	}

	obj.getUser = function() {
		return obj.user;
	}

	obj.init = function(initCallback) {
		var self = this;

		console.log("ProfileCtrl.init()");

		obj.breadcrumbs.set('My Profile');

		obj.view.show(function() {
			self.registerListeners();

			$("#passwordChangeLink").bind(
					'click',
					function(event) {
						self.changePasswordDialog.init(self.getUser().email,
								self.getUser().password);
					});
			
			self.view.setEditMode(false);
			
			initCallback();
		});
	}

	obj.registerListeners = function() {
		var self = this;

		console.log("ProfileCtrl.registerListeners()");

		obj.view.getEditButton().bind('click', function(event) {
			event.preventDefault();
			
			self.editMode = true;
			self.view.setEditMode(true);
			self.breadcrumbs.set('Edit Profile');
			obj.registerValidators();
			obj.validate();
		});

		obj.view.getCancelButton().bind('click', function(event) {
			event.preventDefault();
			
			self.editMode = false;
			self.view.setEditMode(false);
			self.reloadUser();
			self.breadcrumbs.set('My Profile');
			obj.unregisterValidators();
		});

		obj.view.getSaveButton().bind('click', function(event) {
			event.preventDefault();
			
			self.editMode = false;
			self.saveUser();
			self.view.setEditMode(false);
			self.breadcrumbs.set('My Profile');
			obj.unregisterValidators();
		});
		
		obj.view.getCountryInput().change(obj.adjustStateProvinceDropdown).change();
		obj.view.getStateProvinceInput().change(function(event){
			if(obj.view.getStateProvinceInput().val()!=""){
				obj.view.getFirstStateProvinceOption().text("");
			}else{
				obj.view.getFirstStateProvinceOption().text("Please Select");
			}
		});
	}
	
	obj.afterLoginButtonClicked = function(event) {
		var self = this;

		console.log("ProfileCtrl.afterLoginButtonClicked()");

		obj.delegate.getUser(obj.view.getLogin(), function(r) {
			if (r.password == self.view.getPassword()) {
				self.messages.displayMessage('info',
				'You have been successfully logged in.');
			} else {
				self.messages.displayMessage('info', 'Incorrect password.');
			}

			self.registerListeners();
			self.view.untoggle();
		}, function(r) {
			self.messages.displayMessage('info', 'Incorrect login.');
			self.registerListeners();
		});
	}

	obj.registerValidators = function() {
		var self = this;

		obj.validators[0] = new openidm.validator([ obj.view
				.getFirstNameInput() ], [ new openidm.condition('letters-only',
				openidm.validator.nameValidator) ], 'keyup', 'simple',
				self.validateForm);
		obj.validators[1] = new openidm.validator(
				[ obj.view.getLastNameInput() ], [ new openidm.condition(
						'letters-only', openidm.validator.lastnameValidator) ],
				'keyup', 'simple', self.validateForm);
		obj.validators[2] = new openidm.validator([ obj.view
				.getPhoneNumberInput() ], [ new openidm.condition(
				'letters-only', openidm.validator.phoneNumberValidator) ],
				'keyup', 'simple', self.validateForm);

	};

	obj.unregisterValidators = function() {
		var self = this;

		for ( var i = 0; i < obj.validators.length; i++) {
			obj.validators[i].unregister();
		}
	};

	obj.validate = function() {
		for ( var i = 0; i < obj.validators.length; i++) {
			obj.validators[i].validate()
		}
	};

	obj.validateForm = function() {
		var allOk = true;

		for ( var i = 0; i < obj.validators.length; i++) {
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
	};

	obj.saveUser = function() {
		var self = this;
		
		obj.delegate.updateUser(obj.user, obj.view.getUser(), function() {
			self.messages.displayMessage('info', 'Profile has been updated');
			self.setUser(obj.view.getUser());
		}, function() {
			self.messages.displayMessage('error',
			'Problem with updating profile');
			self.reloadUser();
		}, function() {
			self.reloadUser();
		});
	}

	obj.setStates = function(country,stateProvince) {
		self = this;
		
		$.ajax({
			type : "GET",
			url : "js/app/tmp/" + country + ".json",
			dataType : "json",
			success : function(data) {
				data = [ {
					"key" : "",
					"value" : "Please Select"
				} ].concat(data);
				obj.view.getStateProvinceInput().loadSelect(data);
				if(stateProvince && stateProvince!=""){
					obj.view.getStateProvinceInput().val(stateProvince);
					obj.view.getFirstStateProvinceOption().text("");
				}else{
					obj.view.getFirstStateProvinceOption().text("Please Select")
				}
				
				obj.checkSelectors();				
			},
			error : function(xhr) {
				console.log('Error: ' + xhr.status + ' ' + xhr.statusText)
			}
		});
	}

	obj.setCountryAndStateProvince = function(country, stateProvince) {
		self = this;
		
		$.ajax({
			type : "GET",
			url : "js/app/tmp/countries.json",
			dataType : "json",
			success : function(data) {
				data = [ {
					"key" : "",
					"value" : "Please Select"
				} ].concat(data);
				obj.view.getCountryInput().loadSelect(data);
				if(country && country!=""){
					obj.view.getCountryInput().val(country);
					obj.view.getFirstCoutryOption().text("")
				}
				obj.adjustStateProvinceDropdown(null,stateProvince);
				
				obj.checkSelectors();
			},
			error : function(xhr) {
				console.log('Error: ' + xhr.status + ' ' + xhr.statusText)
			}
		});
	}
	
	obj.setSecurityQuestionSelect = function(question) {
		self = this;
		
		$.ajax({
			type : "GET",
			url : "js/app/tmp/secquestions.json",
			dataType : "json",
			success : function(data) {
				obj.view.getSecurityQuestion().loadSelect(data);
				obj.view.getSecurityQuestion().val(question);
				
				obj.checkSelectors();
			},
			error : function(xhr) {
				console.log('Error: ' + xhr.status + ' ' + xhr.statusText)
			}
		});
	}

	
	obj.adjustStateProvinceDropdown = function(event,stateProvince) {
		var country = obj.view.getCountryInput().val();
		if(!country || country==""){
			console.log("Removing all states/provinces")
			obj.view.getStateProvinceInput().emptySelect();
			if(country==""){
				obj.view.getFirstCoutryOption().text("Please Select")
			}
		}else{
			obj.view.getFirstCoutryOption().text("")
			console.log("Getting data from server for "+country)
			obj.setStates(country,stateProvince)
		}
	}
	
	obj.checkSelectors = function() { 
		var self = this;

		obj.view.getSelects().each(function() {
			if( self.editMode == false && $(this).val() == '' ) {
				self.view.setEditMode(false);
			}
		});		
	}
	
	console.log("ProfileCtrl created");

	return obj;
});

