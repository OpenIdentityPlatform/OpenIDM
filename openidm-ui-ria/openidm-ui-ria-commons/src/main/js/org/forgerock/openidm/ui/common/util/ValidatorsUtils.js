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

/*global $, define, _ */

define("org/forgerock/openidm/ui/common/util/ValidatorsUtils", [
], function() {
    var obj = {};

    obj.namePattern = /^([A-Za-\u0105\u0107\u0119\u0142\u00F3\u015B\u017C\u017A\u0104\u0106\u0118\u0141\u00D3\u015A\u017B\u0179\u00C0\u00C8\u00CC\u00D2\u00D9\u00E0\u00E8\u00EC\u00F2\u00F9\u00C1\u00C9\u00CD\u00D3\u00DA\u00DD\u00E1\u00E9\u00ED\u00F3\u00FA\u00FD\u00C2\u00CA\u00CE\u00D4\u00DB\u00E2\u00EA\u00EE\u00F4\u00FB\u00C3\u00D1\u00D5\u00E3\u00F1\u00F5\u00C4\u00CB\u00CF\u00D6\u00DC\u0178\u00E4\u00EB\u00EF\u00F6\u00FC\u0178\u00A1\u00BF\u00E7\u00C7\u0152\u0153\u00DF\u00D8\u00F8\u00C5\u00E5\u00C6\u00E6\u00DE\u00FE\u00D0\u00F0\-\s])+$/;
    obj.phonePattern = /^\+?([0-9\- \(\)])*$/;
    obj.emailPattern = /^([A-Za-z0-9_\-\.])+\@([A-Za-z0-9_\-\.])+\.([A-Za-z]{2,4})$/;
    
    obj.setTick = function(input, isOk) {
        var span = $(input).nextAll("span");
        
        if(isOk) {
            span.removeClass('ok');
            span.addClass('error');
            span.html('x');
        } else {
            span.removeClass('error');
            span.addClass('ok');
            span.html('&#10004;');
        }
    };
    
    obj.setErrors = function(el, validatorType, msg) {
        _.each(el.find("label[data-for-validator="+validatorType+"]"), function(input) {
            var type = $(input).text(), span = $(input).prev("span");
            
            if( $.inArray(type, msg) !== -1 ) {
                span.removeClass('ok');
                span.addClass('error');
                span.html('x');
            } else {
                span.removeClass('error');
                span.addClass('ok');
                span.html('&#10004;');
            }
        });
    };
    
    obj.hideValidation = function(input, el) {
        $(input).nextAll("span").hide();
        $(input).nextAll("div.validationMessage:first").hide();
    };
    
    obj.showValidation = function(input, el) {
        $(input).nextAll("span").show();
        $(input).nextAll("div.validationMessage:first").show();
    };
    
    obj.hideBox = function(el) {
        el.find(".groupFieldErrors").hide();
    };
    
    obj.showBox = function(el) {
        el.find(".groupFieldErrors").show();
    };
    
    return obj;
});
