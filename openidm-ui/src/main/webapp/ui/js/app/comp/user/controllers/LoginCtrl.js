define(["app/comp/main/controllers/MessagesCtrl",
        "app/comp/user/views/LoginView",
        "app/comp/user/delegates/UserDelegate",
        "app/comp/main/controllers/BreadcrumbsCtrl",
        "app/comp/user/controllers/RegistrationCtrl",
        "app/comp/user/controllers/ProfileCtrl",
        "app/comp/user/controllers/ForgottenPasswordDialogCtrl"],
        function (messagesCtrl,
        		loginView,
        		userDelegate,
        		breadcrumbsCtrl,
        		registrationCtrl,
        		profileCtrl,
        		forgotPasswordCtrl) {
	var obj = {};

	obj.messages = messagesCtrl;
	obj.view = loginView;
	obj.delegate = userDelegate;
	obj.breadcrumbs = breadcrumbsCtrl;
	obj.forgottenPasswordDialog = forgotPasswordCtrl;
	obj.loggedUser = null;

	obj.init = function() {
		console.log("LoginCtrl.init()");

		obj.view.renderLogin();
		obj.registerListeners();
	}

	obj.registerListeners = function() {
		var self = this;

		console.log("LoginCtrl.registerListeners()");

		obj.view.getLoginButton().removeAttr('disabled');

		obj.view.getLoginButton().bind('click', function(event) {
			event.preventDefault();

			self.view.getLoginButton().unbind();
			self.view.getLoginButton().attr('disabled', 'disabled');

			self.afterLoginButtonClicked(event);
		});

		obj.view.getLogoutButton().bind('click', function(event) {
			event.preventDefault();
			self.view.renderLogin();

			require("app/comp/main/controllers/MainCtrl").clearContent();
			self.breadcrumbs.set('');
		});

		obj.view.getProfileButton().unbind();
		obj.view.getProfileButton().bind('click', function(event) {
			event.preventDefault();

			profileCtrl.init(function() {
				profileCtrl.reloadUser();
			});
		});

		obj.view.getRegisterButton().unbind();
		obj.view.getRegisterButton().bind('click', function(event) {
			event.preventDefault();

			registrationCtrl.init();

			self.breadcrumbs.set('Registration');
		});
		
		$("#forgotPasswordLink").bind('click', function(event) {
			self.forgottenPasswordDialog.init();
		});
		
		obj.view.getLoginInput().bind('keyup', function(event) {
			self.validateForm();
		});
		
		obj.view.getPasswordInput().bind('keyup', function(event) {
			self.validateForm();
		});
	},
	
	obj.validateForm = function() {
		if( obj.view.getLogin() != "" && obj.view.getPassword() != "" ) {
			obj.view.enableLoginButton();
		} else {
			obj.view.disableLoginButton();
		}
	}
	
	obj.afterLoginButtonClicked = function(event) {
		var self = this;

		console.log("LoginCtrl.afterLoginButtonClicked()");

		obj.delegate.getUser(obj.view.getLogin(), function(r) {
			if (r.password == self.view.getPassword()) {
				self.messages.displayMessage('info',
				'You have been successfully logged in.');
				self.loginUser(r);
			} else {
				self.messages.displayMessage('info', 'Login/password combination is invalid.');
			}

			self.registerListeners();
			self.view.untoggle();
		}, function(r) {
			self.messages.displayMessage('info', 'Login/password combination is invalid.');
			self.registerListeners();
		});
	}

	obj.loginUser = function(user) {
		var self = this;

		obj.breadcrumbs.set('My Profile');
		obj.view.renderLogged();
		obj.view.setUserName(user.email);

		obj.loggedUser = user;

		profileCtrl.init(function() {
			profileCtrl.setUser(self.loggedUser);
		});
	}

	console.debug("loginctrl created");
	return obj;
});
