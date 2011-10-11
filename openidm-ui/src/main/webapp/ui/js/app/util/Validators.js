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
