/*! @license 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2012 ForgeRock AS. All rights reserved.
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

/**
 * @author jdabrowski
 * 
 * This script validates if user is valid.
 */

var errors = [];

function requiredValidator(toValidate, fieldName) {
    if (!toValidate || toValidate === "") {
        errors.push(fieldName + " is required");
        return false;
    }
    return true;
}

function atLeastLengthValidator(toValidate, minLength, fieldName) {
    if (toValidate.length < minLength) {
    	errors.push(fieldName + " should have at least " + minLength + " characters");
    }
}

function alLeastOneNumberValidator(toValidate, fieldName) {
    var reg = /[(0-9)]+/;
    if (!reg.test(toValidate)) {
    	errors.push(fieldName + " should have at least one number");
    }
}

function alLeastOneCapitalCharValidator(toValidate, fieldName) {
    var reg = /[(A-Z)]+/;
    if (!reg.test(toValidate)) {
    	errors.push(fieldName + " should have at least one capital letter");
    }
}

function requiredOnlyAlfabeticalValidator(toValidate, fieldName) {
    
    var exists = requiredValidator(toValidate, fieldName);
    if (!exists) {
    	return;
    }
    
    var reg = /^([A-Za-\u0105\u0107\u0119\u0142\u00F3\u015B\u017C\u017A\u0104\u0106\u0118\u0141\u00D3\u015A\u017B\u0179\u00C0\u00C8\u00CC\u00D2\u00D9\u00E0\u00E8\u00EC\u00F2\u00F9\u00C1\u00C9\u00CD\u00D3\u00DA\u00DD\u00E1\u00E9\u00ED\u00F3\u00FA\u00FD\u00C2\u00CA\u00CE\u00D4\u00DB\u00E2\u00EA\u00EE\u00F4\u00FB\u00C3\u00D1\u00D5\u00E3\u00F1\u00F5\u00C4\u00CB\u00CF\u00D6\u00DC\u0178\u00E4\u00EB\u00EF\u00F6\u00FC\u0178\u00A1\u00BF\u00E7\u00C7\u0152\u0153\u00DF\u00D8\u00F8\u00C5\u00E5\u00C6\u00E6\u00DE\u00FE\u00D0\u00F0\-\s])+$/;
    if (!reg.test(toValidate)) {
    	errors.push("Only alphabetic characters allowed in " + fieldName);
    }
};

function numbersAndSpecialCharsValidator(toValidate, fieldName) {
    
	var exists = requiredValidator(toValidate, fieldName);
    if (!exists) {
    	return;
    }
    
    var reg = /^\+?([0-9\- \(\)])*$/;
    if (!reg.test(toValidate)) {
    	errors.push("Only numbers and special characters allowed in " + fieldName);
    }
}

function requiredEmailValidator(toValidate, fieldName) {
	var exists = requiredValidator(toValidate, fieldName);
    if (!exists) {
    	return;
    }
    
    var reg = /^([A-Za-z0-9_\-\.])+\@([A-Za-z0-9_\-\.])+\.([A-Za-z]{2,4})$/;
    if (!reg.test(toValidate)) {
    	errors.push("Not a valid " + fieldName);
    }
}

function requiredPasswordValidator(toValidate, fieldName) {
	var exists = requiredValidator(toValidate, fieldName);
    if (!exists) {
    	return;
    }
    atLeastLengthValidator(toValidate, 8, fieldName);
    alLeastOneNumberValidator(toValidate, fieldName);
    alLeastOneCapitalCharValidator(toValidate, fieldName);
}

function notEqualValidator(firstVal, secondVal, message) {
    if (firstVal === "" || firstVal === secondVal) {
        errors.push(message);
    }
}

function isUserValid() {
    var user = openidm.decrypt(object);
    requiredOnlyAlfabeticalValidator(user.givenName, "Given name");
    requiredOnlyAlfabeticalValidator(user.familyName, "Family name");
    numbersAndSpecialCharsValidator(user.phoneNumber, "Mobile Phone Number");
    requiredEmailValidator(user.email, "Email");
    requiredEmailValidator(user.userName, "UserName");
    requiredPasswordValidator(user.password, "Password");
    notEqualValidator(user.userName, user.password, "UserName and Password cannot be equal");
    requiredValidator(user.securityQuestion, "Security Question");
    requiredValidator(user.securityAnswer, "Security Answer");
    
    if(errors.length > 0) {
    	throw errors;
    }
};

//TODO Backend validation defined here supports specific data format required by the default OpenIDM UI. If OpenIDM is used only with default UI this line can be uncommented to ensure proper stored data format.  
//isUserValid();