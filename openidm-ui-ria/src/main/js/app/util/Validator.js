/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/*global $, define*/

/**
 * @author mbilski
 */

define("app/util/Validator", 
        [], 
        function() {

    var obj = function(inputs, conditions, eventType, mode, callback, required) {
        this.okFields = [];
        this.ok = false;
        this.inputs = inputs;
        this.conditions = conditions;
        this.callback = callback;
        this.eventType = eventType; // change / lostFocus
        this.mode = mode; // simple / advanced
        this.required = required;

        this.init();
    };

    obj.prototype = {
            ok: null,
            inputs: null,
            conditions: null,
            callback: null,
            eventType: null,
            mode: null,
            required: null
    };

    obj.prototype.isOk = function() {
        var r, i;
        if( this.mode === 'simple' ) {
            return this.ok;
        } else {
            r = true;

            for(i = 0; i < this.conditions.length; i++ ) {
                if( this.okFields[this.conditions[i].name] === false )	{
                    r = false;
                }			
            }

            return r;
        }
    };

    obj.prototype.validate = function() {
        var l;
        for( l = 0; l < this.inputs.length; l++ ) {
            this.validateField(this.inputs[l]);
        }
    };

    obj.prototype.validateField = function(input) {
        console.log("validating field");

        var result, j, remove = true;

        if( this.required === false && $(input).val() === "" ) {			
            this.clear(input);
            this.ok = true;
            this.callback();
            return;
        }

        for( j = 0; j < this.conditions.length; j++ ) {
            result = this.conditions[j].check(this.inputs, this);

            if( result === 'delegate' ) {
                remove = false;
                break;
            }

            if( this.mode === 'simple' ) {
                if(result) {
                    remove = false;
                    this.simpleAddError(input, result);
                    this.addError(input);
                    break;
                }
            } else {
                if(!result) {
                    this.advancedRemoveError(input, this.conditions[j].name);
                } else {
                    this.advancedAddError(input, this.conditions[j].name, result);
                    remove = false;
                }
            }
        }

        if( remove === true && this.mode === 'simple' ) {	
            this.removeError(input);
            this.simpleRemoveError(input);
        } else if( remove === true && this.mode !== 'simple' && this.inputs[0].val() === $(input).val() ) {
            this.removeError(input);
        } else if( this.mode !== 'simple' && remove === false && this.inputs[0].val() === $(input).val() ) {
            this.addError(input);
        }
    };

    obj.prototype.init = function() {
        var i, forOneInput, self = this;

        forOneInput = function() {
            self.validateField(this);
        };
        
        for( i = 0; i < this.inputs.length; i++ ) {
            this.inputs[i].on(this.eventType, forOneInput);
        }

        if( this.mode !== 'simple' ) {
            for(i = 0; i < this.conditions.length; i++ ) {
                this.okFields[this.conditions[i].name] = false;	
            }
        }
    };

    obj.prototype.unregister = function() {
        var i, self = this;

        for(i = 0; i < this.inputs.length; i++ ) {
            this.inputs[i].off(this.eventType);
        }

        this.inputs[0].parent().find('.validationMessage').html('');
        this.inputs[0].parent().find('span').removeClass('error');
        this.inputs[0].parent().find('span').removeClass('ok');
        this.inputs[0].parent().find('span').html('');
    };

    obj.prototype.removeError = function(input) {
        if( $(input).parent().find('span').first().hasClass('error') || !$(input).parent().find('span').first().hasClass('ok')) {
            $(input).parent().find('span').first().removeClass('error');
            $(input).parent().find('span').first().addClass('ok');
            $(input).parent().find('span').first().html('&#10004;');
        }
    };

    obj.prototype.addError = function(input) {		
        if( $(input).parent().find('span').hasClass('ok') || !$(input).parent().find('span').hasClass('error')) {
            $(input).parent().find('span').removeClass('ok');
            $(input).parent().find('span').addClass('error');
            $(input).parent().find('span').html('x');
        }
    };

    obj.prototype.simpleRemoveError = function(input) {		
        $(input).parent().find('.validationMessage').html('');

        this.ok = true;
        this.callback();
    };

    obj.prototype.simpleAddError = function(input, msg) {
        $(input).parent().find('.validationMessage').html(msg);

        this.ok = false;
        this.callback();
    };

    obj.prototype.advancedRemoveError = function(input, name) {
        console.log('remove');
        $(input).parent().parent().parent().find(".groupFieldErrors").find("#"+name).prev('span').html('&#10004;');
        $(input).parent().parent().parent().find(".groupFieldErrors").find("#"+name).prev('span').removeClass('error');
        $(input).parent().parent().parent().find(".groupFieldErrors").find("#"+name).prev('span').addClass('ok');

        this.okFields[name] = true;
        this.callback();
    };

    obj.prototype.advancedAddError = function(input, name, msg) {
        console.log('add');
        $(input).parent().parent().parent().find(".groupFieldErrors").find("#"+name).prev('span').html('x');
        $(input).parent().parent().parent().find(".groupFieldErrors").find("#"+name).prev('span').removeClass('ok');
        $(input).parent().parent().parent().find(".groupFieldErrors").find("#"+name).prev('span').addClass('error');

        this.okFields[name] = false;
        this.callback();
    };

    obj.prototype.clear = function(input) {
        console.log('clearing validation');
        $(input).parent().find('span').first().html("");
        $(input).parent().find('.validationMessage').html("");
    };
    return obj;
});

