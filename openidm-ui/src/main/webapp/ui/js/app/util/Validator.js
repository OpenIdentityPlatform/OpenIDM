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
registerNS("openidm.validator");

/**
 * inputs tablica inputow. Pierwszy to ten, na ktorym zostanie wykonane
 * sprawdzanie. Reszta to inputy dodatkowe (np confirm password, password)
 *
 * condition funkcja ktora przyjmuje tablice wartosci (stringi)
 *
 * formValidator callback do funkcji ktora sprawdza cala forme
 * (aby np enable/disable send button)
 */
openidm.validator = function(inputs, condition, formValidator) {
	this.ok = false;
	this.inputs = inputs;
	this.condition = condition;
	this.formValidator = formValidator;

	this.init();
};

openidm.validator.prototype = {
	ok: null,
	inputs: null,
	condition: null,
	formValidator: null
};

openidm.validator.prototype.isOk = function() {
	return this.ok;
};

openidm.validator.prototype.init = function() {
	var self = this;

	this.inputs[0].bind('change', function() {
		console.log("validating field");

		values = new Array();
		for( i = 0 ; i < self.inputs.length; i++ ) {
			values[i] = self.inputs[i].val();
		}

		result = self.condition(values);
		console.log("result " + result);

		if( !result ) {
			if( $(this).parent().find('span').hasClass('error') || !$(this).parent().find('span').hasClass('ok')) {
	  	 		$(this).parent().find('span').removeClass('error')
				$(this).parent().find('span').addClass('ok');
				$(this).parent().find('span').html('ok');
			}
			$(this).parent().find('.validationMessage').html('');
			self.ok = true;

			self.formValidator();
		} else {
			if( $(this).parent().find('span').hasClass('ok') || !$(this).parent().find('span').hasClass('error')) {
   	 			$(this).parent().find('span').removeClass('ok')
				$(this).parent().find('span').addClass('error');
				$(this).parent().find('span').html('error');
			}

			$(this).parent().find('.validationMessage').html(result);

			self.ok = false;

			self.formValidator();
		}
	});
}

