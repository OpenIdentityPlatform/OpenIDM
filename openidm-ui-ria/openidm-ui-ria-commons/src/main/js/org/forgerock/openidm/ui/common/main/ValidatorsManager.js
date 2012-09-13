/*
 * @license DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock AS. All rights reserved.
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

/*global require, define, _, $ */

/**
 * @author mbilski
 */
define("org/forgerock/openidm/ui/common/main/ValidatorsManager", [
    "config/ValidatorsConfiguration"    
], function(validators) {
    var obj = {};
    
    obj.bindValidators = function(el) {
        var inputs, event, input;        
        
        el.find("[data-validator]").not("[data-validation-status='error']").attr("data-validation-status", "error");
        
        _.each(el.find("[data-validator]"), function(input) {
            input = $(input);
            
            if(input.attr('data-validator-event')) {
                event = input.attr('data-validator-event');
            } else {
                event = "change";
            }
            
            input.on(event, _.bind(obj.validate, {input: input, el: el, validatorType: input.attr('data-validator')}));
        });
    };
    
    obj.validateAllFields = function(el) {
        _.each(el.find("[data-validator]"), function(input){
            var event = $(input).attr('data-validator-event');
            
            if(event) {
                $(input).trigger(event);
            } else {
                $(input).trigger("change");
            }
        });
    };
    
    obj.formValidated = function(el) {
        return el.find("[data-validation-status=error]").length === 0 && el.find("[data-validation-status=ok]").length !== 0;
    };

    obj.validate = function(event) {       
        var parameters = [this.el, this.input, _.bind(obj.afterValidation, this)], validatorConfig, i;
        validatorConfig = validators[this.validatorType];
        
        if(validatorConfig) {
            this.el.trigger("onValidate", [this.input, "inProgress"]);
            
            for(i = 0; i < validatorConfig.dependencies.length; i++) {
                parameters.push(require(validatorConfig.dependencies[i]));
            }
            
            validatorConfig.validator.apply(this, parameters);
        } else {
            console.error("Could not find such validator: " + validatorConfig);
        }           
    };
    
    obj.afterValidation = function(msg) {
        if(msg === "inProgress") {
            this.input.attr("data-validation-status", "error");
        } else if(msg === "disabled") {
            this.input.attr("data-validation-status", "disabled");
        } else if(msg) {
            this.input.attr("data-validation-status", "error");
        } else {
            this.input.attr("data-validation-status", "ok");
        }
        
        this.el.trigger("onValidate", [this.input, msg, this.validatorType]); 
    };

    return obj;

});    

