define(function() {
	
	registerNS("openidm.validator");

	openidm.condition = function(name, checkingFunction) {
		this.name = name;
		this.checkingFunction = checkingFunction;
	};

	openidm.condition.prototype = {
			name: null,
			checkingFunction: null,
			check: function(input, self) {
				return this.checkingFunction(input, self);
			}
	}

	/**
	 * inputs tablica inputow. Pierwszy to ten, na ktorym zostanie wykonane
	 * sprawdzanie. Reszta to inputy dodatkowe (np confirm password, password)
	 *
	 * condition funkcja ktora przyjmuje tablice wartosci (stringi)
	 *
	 * formValidator callback do funkcji ktora sprawdza cala forme
	 * (aby np enable/disable send button)
	 */
	openidm.validator = function(inputs, conditions, eventType, mode, callback) {
		this.okFields = new Array();
		this.ok = false;
		this.inputs = inputs;
		this.conditions = conditions;
		this.callback = callback;
		this.eventType = eventType; // change / lostFocus
		this.mode = mode; // simple / advanced

		this.init();
	};

	openidm.validator.prototype = {
			ok: null,
			inputs: null,
			conditions: null,
			callback: null,
			eventType: null,
			mode: null
	};

	openidm.validator.prototype.isOk = function() {
		if( this.mode == 'simple' ) {
			return this.ok;
		} else {
			var r = true;
			
			for( var i = 0; i < this.conditions.length; i++ ) {
				if( this.okFields[this.conditions[i].name] == false )	{
					r = false;
				}			
			}
			
			return r;
		}
	};

	openidm.validator.prototype.validate = function() {
		for( l = 0; l < this.inputs.length; l++ ) {
			this.validateField(this.inputs[l]);
		}
	}

	openidm.validator.prototype.validateField = function(input) {
		console.log("validating field");

		for( j = 0; j < this.conditions.length; j++ ) {
			result = this.conditions[j].check(this.inputs, this);

			if( this.mode == 'simple' ) {
				if(!result) {
					this.simpleRemoveError(input);
				} else {
					this.simpleAddError(input, result);
					break;
				}
			} else {
				if(!result) {
					this.advancedRemoveError(input, this.conditions[j].name);
				} else {
					this.advancedAddError(input, this.conditions[j].name, result);
				}
			}
		}
	};

	openidm.validator.prototype.init = function() {
		var self = this;

		for( i = 0; i < this.inputs.length; i++ ) {
			this.inputs[i].bind(this.eventType, function() {
				self.validateField(this);
			});
		}
		
		if( this.mode != 'simple' ) {
			for( var i = 0; i < this.conditions.length; i++ ) {
				this.okFields[this.conditions[i].name] = false;	
			}
		}
	}

	openidm.validator.prototype.unregister = function() {
		var self = this;

		for(var i = 0; i < this.inputs.length; i++ ) {
			this.inputs[i].unbind(this.eventType);
		}

		this.inputs[0].parent().find('.validationMessage').html('');
		this.inputs[0].parent().find('span').removeClass('error')
		this.inputs[0].parent().find('span').removeClass('ok');
		this.inputs[0].parent().find('span').html('');
	}

	openidm.validator.prototype.simpleRemoveError = function(input) {
		if( $(input).parent().find('span').hasClass('error') || !$(input).parent().find('span').hasClass('ok')) {
			$(input).parent().find('span').removeClass('error')
			$(input).parent().find('span').addClass('ok');
			$(input).parent().find('span').html('&#10004;');
		}
		$(input).parent().find('.validationMessage').html('');

		this.ok = true;
		this.callback();
	}

	openidm.validator.prototype.simpleAddError = function(input, msg) {
		if( $(input).parent().find('span').hasClass('ok') || !$(input).parent().find('span').hasClass('error')) {
			$(input).parent().find('span').removeClass('ok')
			$(input).parent().find('span').addClass('error');
			$(input).parent().find('span').html('x');
		}

		$(input).parent().find('.validationMessage').html(msg);

		this.ok = false;
		this.callback();
	}

	openidm.validator.prototype.advancedRemoveError = function(input, name) {
		console.log('remove');
		$(input).parent().parent().parent().find(".groupFieldErrors").find("#"+name).prev('span').html('&#10004;');
		$(input).parent().parent().parent().find(".groupFieldErrors").find("#"+name).prev('span').removeClass('error');
		$(input).parent().parent().parent().find(".groupFieldErrors").find("#"+name).prev('span').addClass('ok');

		this.okFields[name] = true;
		this.callback();
	}

	openidm.validator.prototype.advancedAddError = function(input, name, msg) {
		console.log('add');
		$(input).parent().parent().parent().find(".groupFieldErrors").find("#"+name).prev('span').html('x');
		$(input).parent().parent().parent().find(".groupFieldErrors").find("#"+name).prev('span').removeClass('ok');
		$(input).parent().parent().parent().find(".groupFieldErrors").find("#"+name).prev('span').addClass('error');
		
		this.okFields[name] = false;
		this.callback();
	};

});

