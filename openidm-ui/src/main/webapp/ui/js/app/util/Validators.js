define(["app/comp/user/delegates/UserDelegate"],function(userDelegate) {
	registerNS("openidm.validator");

	openidm.validator.nameValidator = function(inputs) {
		var reg = /^([A-Za-z])+$/;

		if( inputs[0].val() == "" ) {
			return "Required";
		}

		if( !reg.test(inputs[0].val()) ) {
			return "Only alphabetic characters";
		}
	};

	openidm.validator.lastnameValidator = function(inputs) {
		var reg = /^([A-Za-z])+$/;

		if( inputs[0].val() == "" ) {
			return "Required";
		}

		if( !reg.test(inputs[0].val()) ) {
			return "Only alphabetic characters";
		}
	};

	openidm.validator.phoneNumberValidator = function(inputs) {
			var reg = /^\+?([0-9\- ])*$/;

			if( !reg.test(inputs[0].val()) ) {
				return "Only numbers and special characters";
			}
	};



	openidm.validator.emailValidator = function(inputs) {
		var reg = /^([A-Za-z0-9_\-\.])+\@([A-Za-z0-9_\-\.])+\.([A-Za-z]{2,4})$/;

		if( inputs[0].val() == "" ) {
			return "Required";
		}

		if (!reg.test(inputs[0].val())) {
			return "Not a valid email address.";
		}
	};



	openidm.validator.uniqueEmailValidator = function(inputs, self) {
		userDelegate.getUser(inputs[0].val(), function() {
			self.simpleAddError(inputs[0], "Email address already exists. <br /><a href='#' id='frgtPasswrdSelfReg' class='ice'>Forgot password?</a>");
		}, function() {
			self.simpleRemoveError(inputs[0]);
		});
	};
	
	openidm.validator.nonuniqueEmailValidator = function(inputs, self) {
		userDelegate.getUser(inputs[0].val(), function() {
			self.simpleRemoveError(inputs[0]);
		}, function() {
			self.simpleAddError(inputs[0], "Email address non exists.");
		});
	};
	
	openidm.validator.answerValidator = function(inputs, self) {
		userDelegate.getUser(inputs[0].val(), function(r) {
			if (r.email == inputs[0].val()) {
				if(inputs[1].val()==r.securityanswer) {
        			self.simpleRemoveError(inputs[1]);
        		  }
        		  else {
        			self.simpleAddError(inputs[1], "Wrong answer!");
        		  }
				
			} else {
    			self.simpleAddError(inputs[1], "Never happen!");
			}

		}, function(r) {
			self.simpleAddError(inputs[1], "Never happen!");
		});
		
		/*
		
		
		
		userDelegate.getAnswer(inputs[0].val(), function() {
			self.simpleAddError(inputs[0], "Email address already exists. <br /><a href='#' id='frgtPasswrdSelfReg' class='ice'>Forgot password?</a>");
		}, function() {
			self.simpleRemoveError(inputs[0]);
		});*/
	};
	
});
