/* @license 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
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

/*global define, $, _ */

/**
 * @author mbilski
 */
define("config/ValidatorsConfiguration", [
    "org/forgerock/openidm/ui/common/util/Constants", 
    "org/forgerock/openidm/ui/common/main/EventManager"
], function(constants, eventManager) {
    var obj = {
        "required": {
            "name": "Required field",
            "dependencies": [
            ],
            "validator": function(el, input, callback) {
                var v = $(input).val();
                
                if(v === "") {
                    callback("Required");
                    return;
                }

                callback();  
            }
        },    
        "registrationEmail": {
            "name": "Correct and unique email",
            "dependencies": [
                "org/forgerock/openidm/ui/common/util/ValidatorsUtils",
                "org/forgerock/openidm/ui/user/delegates/UserDelegate"
            ],
            "validator": function(el, input, callback, utils, userDelegate) {
                var v = $(input).val();
                
                if(v === "") {
                    callback("Required");
                    return;
                }
                
                if(!utils.emailPattern.test(v)) {
                    callback("Not a valid email address.");
                    return;
                }
                
                userDelegate.checkUserNameAvailability(v, function(available) {
                    if(!available) {
                        callback("Email address already exists. <br />&nbsp;&nbsp;<a href='#profile/forgotten_password/' id='frgtPasswrdSelfReg' class='ice'>Forgot password?</a>");
                    } else {
                        callback();
                    }
                });              
            }
        },
        "adminRegistrationEmail": {
            "name": "Correct and unique email",
            "dependencies": [
                "org/forgerock/openidm/ui/common/util/ValidatorsUtils",
               "org/forgerock/openidm/ui/user/delegates/UserDelegate"
            ],
            "validator": function(el, input, callback, utils, userDelegate) {
                var v = $(input).val();
                
                if(v === "") {
                    callback("Required");
                    return;
                }
                
                if(!utils.emailPattern.test(v)) {
                    callback("Not a valid email address.");
                    return;
                }
                
                userDelegate.checkUserNameAvailability(v, function(available) {
                    if(!available) {
                        callback("Email address already exists.");
                    } else {
                        callback();
                    }
                });              
            }
        },
        "name": {
            "name": "Only alphabetic characters",
            "dependencies": [
                "org/forgerock/openidm/ui/common/util/ValidatorsUtils"
            ],
            "validator": function(el, input, callback, utils) {
                var v = $(input).val();
                
                if(v === "") {
                    callback("Required");
                    return;
                }
                
                if(!utils.namePattern.test(v)) {
                    callback("Only alphabetic characters");
                    return;
                }

                callback();  
            }
        },
        "phone": {
            "name": "Only numbers etc",
            "dependencies": [
                "org/forgerock/openidm/ui/common/util/ValidatorsUtils"
            ],
            "validator": function(el, input, callback, utils) {
                var v = $(input).val();
                
                if(v === "") {
                    callback("Required");
                    return;
                }
                
                if(!utils.phonePattern.test(v)) {
                    callback("Only numbers and special characters");
                    return;
                }

                callback(); 
            }
        },
        "password": {
            "name": "Password validator",
            "dependencies": [
                "org/forgerock/openidm/ui/common/util/ValidatorsUtils"
            ],
            "validator": function(el, input, callback, utils) {
                var v = $(input).val(), reg, errors = [];
                
				if(el.find("input[name=oldPassword]").length !== 0) {
                    if(el.find("input[name=oldPassword]").val() === v) {
                        errors.push("Cannot match old password");
                    }
                    
                    if(v === "" && $(el).find("input[name=passwordConfirm]").val() === "") {
                        $(el).find("input[name=passwordConfirm]").trigger("keyup");
                        callback("disabled");
                        utils.hideBox(el);
                        return;
                    }  else {
                        utils.showBox(el);
                    }
                }

                if(v.length < 8) {
                    errors.push("At least 8 characters");
                }
                
                reg = /[(A-Z)]+/;
                if(!reg.test(v)) {
                    errors.push("At least one capital letter");
                }
                
                reg = /[(0-9)]+/;
                if( !reg.test(v) ) {
                    errors.push("At least one number");
                }
                
                if( v === "" || v === $(el).find("input[name=email]").val() ) {
                    errors.push("Cannot match login");
                }
                
                if(errors.length === 0) {
                    callback(); 
                } else {
                    callback(errors);
                }
                
                $(el).find("input[name=passwordConfirm]").trigger("keyup");
            }
        },
        "passwordConfirm": {
            "name": "Password confirmation",
            "dependencies": [
                "org/forgerock/openidm/ui/common/util/ValidatorsUtils"
            ],
            "validator": function(el, input, callback, utils) {
                var v = $(input).val();
                
				if(el.find("input[name=oldPassword]").length !== 0) {
                    if(v === "" && $(el).find("input[name=password]").val() === "") {
                        utils.hideValidation($(el).find("input[name=password]"), el);
                        callback("disabled");
                        utils.hideBox(el);
                        return;
                    } else {
                        utils.showBox(el);
                    }
                }

                if( v === "" || v !== $(el).find("input[name=password]").val() ) {
                    callback(["Confirmation matches password"]);
                    return;
                }

                callback(); 
            }
        },
        "passPhrase": {
            "name": "Min 4 characters",
            "dependencies": [
            ],
            "validator": function(el, input, callback) {
                var v = $(input).val();
                if($(el).find("input[name=oldPassPhrase]").length !== 0) {
                    if($(el).find("input[name=oldPassPhrase]").val() === v) {
                        callback("disabled");
                        return;
                    }
                }
                
                if(v.length < 4) {
                    callback("Minimum 4 characters");
                    return;
                }

                callback();  
            }
        },
        "siteImage": {
            "name": "Site image not same as old",
            "dependencies": [
            ],
            "validator": function(el, input, callback) {
                var v = $(input).val();
                if(el.find("input[name=oldSiteImage]").length !== 0) {
                    if(el.find("input[name=oldSiteImage]").val() === v) {
                        callback("disabled");
                        return;
                    }
                }
                
                callback();  
            }
        },
        "termsOfUse": {
            "name": "Acceptance required",
            "dependencies": [
            ],
            "validator": function(el, input, callback) {              
                if(!$(input).is(':checked')) {
                    callback("Acceptance required for registration");
                    return;
                }

                callback();  
            }
        },
        "profileEmail": {
            "name": "Correct and unique email",
            "dependencies": [
                "org/forgerock/openidm/ui/common/util/ValidatorsUtils",
                "org/forgerock/openidm/ui/user/delegates/UserDelegate",
                "org/forgerock/openidm/ui/common/main/Configuration"
            ],
            "validator": function(el, input, callback, utils, userDelegate, conf) {
                var v = $(input).val();
                
                if(conf.loggedUser.email === v) {
                    callback();
                    return;
                }
                
                if(v === "") {
                    callback("Required");
                    return;
                }
                
                if(!utils.emailPattern.test(v)) {
                    callback("Not a valid email address.");
                    return;
                }
                
                userDelegate.checkUserNameAvailability(v, function(available) {
                    if(!available) {
                        callback("Email address already exists.");
                    } else {
                        callback();
                    }
                });              
            }
        },
	 "adminUserProfileEmail": {
            "name": "Correct and unique email but can be same as was",
            "dependencies": [
                "org/forgerock/openidm/ui/common/util/ValidatorsUtils",
                "org/forgerock/openidm/ui/user/delegates/UserDelegate",
                "org/forgerock/openidm/ui/common/main/Configuration"
            ],
            "validator": function(el, input, callback, utils, userDelegate, conf) {
                var v = $(input).val();
                
                if($(el).find("input[name=oldEmail]").val() === v) {
                    callback();
                    return;
                }
                
                if(v === "") {
                    callback("Required");
                    return;
                }
                
                if(!utils.emailPattern.test(v)) {
                    callback("Not a valid email address.");
                    return;
                }
                
                userDelegate.checkUserNameAvailability(v, function(available) {
                    if(!available) {
                        callback("Email address already exists.");
                    } else {
                        callback();
                    }
                });              
            }
        },
        "oldPassword": {
            "name": "Required field",
            "dependencies": [
                "org/forgerock/openidm/ui/common/main/Configuration",
                "org/forgerock/openidm/ui/user/delegates/UserDelegate"
            ],
            "validator": function(el, input, callback, conf, userDelegate) {
                var v = $(input).val();
                
                if(v === "") {
                    callback("Incorrect password");
                    return;
                }
                
                userDelegate.checkCredentials(conf.loggedUser.userName, v, function(result) {
                    if(result.result) {
                        callback();
                        $(input).attr('data-validation-status', 'ok');
                        $("input[name='Continue']").click();
                    } else {
                        callback("Incorrect password");
                    }
                });
            }
        },
        "resetPasswordCorrectEmail": {
            "name": "Reset Password Correct Email",
            "dependencies": [
                "org/forgerock/openidm/ui/common/util/ValidatorsUtils",
                "org/forgerock/openidm/ui/user/delegates/UserDelegate"
            ],
            "validator": function(el, input, callback, utils, userDelegate) {
                var v = $(input).val();
                
                if(v === "") {
                    callback("Required");
                    $(input).attr('data-validation-status', 'error');
                    $("input[name='Update']").click();
                    return;
                }
                
                if(!utils.emailPattern.test(v)) {
                    callback("Not a valid email address.");
                    $(input).attr('data-validation-status', 'error');
                    $("input[name='Update']").click();
                    return;
                }
               
                userDelegate.checkUserNameAvailability(v, function(available) {
                    if(!available) {
                        callback();
                        $(input).attr('data-validation-status', 'ok');
                    } else {
                        callback("No such user");
                        $(input).attr('data-validation-status', 'error');
                    }
                    
                    $("input[name=fgtnSecurityAnswer]").trigger("keyup");
                    $("input[name=password]").trigger("keyup");
                    $("input[name=passwordConfirm]").trigger("keyup");
                    
                    $("input[name='Update']").click();
                });  
            }
        },
        "securityAnswer": {
            "name": "Check if security answer is correct",
            "dependencies": [
                "org/forgerock/openidm/ui/common/util/ValidatorsUtils",
                "org/forgerock/openidm/ui/user/delegates/UserDelegate"
            ],
            "validator": function(el, input, callback, utils, userDelegate) {
                var v = $(input).val(), userName;
                if(v === "") {
                    callback("Required");
                    return;
                }
                userName = $(el).find("input[name='resetEmail']").val();
                userDelegate.getBySecurityAnswer(userName, v, 
                        function(result) {
                    callback();
                },      function() {
                    callback("x");
                });
            }
        },
		"newSecurityAnswer": {
            "name": "",
            "dependencies": [
            ],
            "validator": function(el, input, callback) {
                var v = $(input).val();
                
                if(el.find("input[name=oldSecurityQuestion]").val() !== el.find("select[name=securityQuestion]").val()) {
                    if(v === "") {
                        callback("Required");
                    } else {
                        callback();
                    }
                    
                    return;
                }
                
                if(v === "") {
                    callback("disabled");
                } else {
                    callback();
                }
            }
        }
    };
    
    return obj;
});
