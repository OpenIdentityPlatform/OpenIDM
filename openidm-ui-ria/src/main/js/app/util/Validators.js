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

/*global $, define */

define("app/util/Validators",["app/comp/user/delegates/UserDelegate"],function(userDelegate) {

    var obj = {};

    obj.nameValidator = function(inputs) {
        var reg = /^([A-Za-\u0105\u0107\u0119\u0142\u00F3\u015B\u017C\u017A\u0104\u0106\u0118\u0141\u00D3\u015A\u017B\u0179\u00C0\u00C8\u00CC\u00D2\u00D9\u00E0\u00E8\u00EC\u00F2\u00F9\u00C1\u00C9\u00CD\u00D3\u00DA\u00DD\u00E1\u00E9\u00ED\u00F3\u00FA\u00FD\u00C2\u00CA\u00CE\u00D4\u00DB\u00E2\u00EA\u00EE\u00F4\u00FB\u00C3\u00D1\u00D5\u00E3\u00F1\u00F5\u00C4\u00CB\u00CF\u00D6\u00DC\u0178\u00E4\u00EB\u00EF\u00F6\u00FC\u0178\u00A1\u00BF\u00E7\u00C7\u0152\u0153\u00DF\u00D8\u00F8\u00C5\u00E5\u00C6\u00E6\u00DE\u00FE\u00D0\u00F0\-\s])+$/;

        if( inputs[0].val() === "" ) {
            return "Required";
        }

        if( !reg.test(inputs[0].val()) ) {
            return "Only alphabetic characters";
        }
    };

    obj.lastnameValidator = function(inputs) {
        var reg = /^([A-Za-z\u0105\u0107\u0119\u0142\u00F3\u015B\u017C\u017A\u0104\u0106\u0118\u0141\u00D3\u015A\u017B\u0179\u00C0\u00C8\u00CC\u00D2\u00D9\u00E0\u00E8\u00EC\u00F2\u00F9\u00C1\u00C9\u00CD\u00D3\u00DA\u00DD\u00E1\u00E9\u00ED\u00F3\u00FA\u00FD\u00C2\u00CA\u00CE\u00D4\u00DB\u00E2\u00EA\u00EE\u00F4\u00FB\u00C3\u00D1\u00D5\u00E3\u00F1\u00F5\u00C4\u00CB\u00CF\u00D6\u00DC\u0178\u00E4\u00EB\u00EF\u00F6\u00FC\u0178\u00A1\u00BF\u00E7\u00C7\u0152\u0153\u00DF\u00D8\u00F8\u00C5\u00E5\u00C6\u00E6\u00DE\u00FE\u00D0\u00F0\-\s])+$/;

        if( inputs[0].val() === "" ) {
            return "Required";
        }

        if( !reg.test(inputs[0].val()) ) {
            return "Only alphabetic characters";
        }
    };

    obj.phoneNumberValidator = function(inputs) {
        var reg = /^\+?([0-9\- \(\)])*$/;

        if( !reg.test(inputs[0].val()) ) {
            return "Only numbers and special characters";
        }

        if( inputs[0].val() === "" ) {
            return "Required";
        }
    };

    obj.passphraseValidator = function(inputs, self) {
        if( inputs[0].val().length < 4 ) {
            return "Minimum 4 characters";
            }
    };

    obj.emailValidator = function(inputs) {
        var reg = /^([A-Za-z0-9_\-\.])+\@([A-Za-z0-9_\-\.])+\.([A-Za-z]{2,4})$/;

        if( inputs[0].val() === "" ) {
            return "Required";
        }

        if (!reg.test(inputs[0].val())) {
            return "Not a valid email address.";
        }
    };

    obj.notEmptyValidator = function(inputs) {
        if( inputs[0].val() === "" ) {
            return "Required";
        }
    };

    obj.uniqueEmailValidator = function(inputs, self) {
        userDelegate.checkUserNameAvailability(inputs[0].val(), function(available) {
            if(!available) {
                self.simpleAddError(inputs[0], "Email address already exists. <br />&nbsp;&nbsp;<a href='#' id='frgtPasswrdSelfReg' class='ice'>Forgot password?</a>");
                self.addError(inputs[0]);
            } else {
                self.simpleRemoveError(inputs[0]);
                self.removeError(inputs[0]);	
            } 
        });

        return "delegate";
    };

    obj.nonuniqueEmailValidator = function(inputs, self) {
        userDelegate.checkUserNameAvailability(inputs[0].val(), function(available) {
            if(!available) {
                self.simpleRemoveError(inputs[0]);
                self.removeError(inputs[0]);
            } else {
                self.simpleAddError(inputs[0], "User not found");
                self.addError(inputs[0]);
            }
        });

        return "delegate";
    };
    return obj;
});
