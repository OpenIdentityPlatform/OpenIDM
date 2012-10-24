/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All rights reserved.
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
define("config/validators/AdminValidators", [
    "org/forgerock/commons/ui/user/delegates/UserDelegate"                                                 
], function(userDelegate) {
    var obj = {
            "adminRegistrationUserName": {
                "name": "Correct and unique userName",
                "dependencies": [
                    "org/forgerock/commons/ui/common/util/ValidatorsUtils",
                   "org/forgerock/commons/ui/user/delegates/UserDelegate"
                ],
                "validator": function(el, input, callback, utils, userDelegate) {
                    var v = $(input).val();
                    
                    if($(el).find("input[name=oldUserName]").val() === v) {
                        callback();
                        return;
                    }
                    
                    if(v === "") {
                        callback($.t("config.validators.required"));
                        return;
                    }
                    
                    userDelegate.checkUserNameAvailability(v, function(available) {
                        if(!available) {
                            callback($.t("config.validators.emailExists"));
                        } else {
                            callback();
                        }
                    });              
                }
            },
            "adminUserProfileEmail": {
                "name": "Correct and unique email but can be same as was",
                "dependencies": [
                    "org/forgerock/commons/ui/common/util/ValidatorsUtils",
                    "org/forgerock/commons/ui/common/main/Configuration"
                ],
                "validator": function(el, input, callback, utils, conf) {
                    var v = $(input).val();
                    
                    if(v === "") {
                        callback($.t("config.validators.required"));
                        return;
                    }
                    
                    if(!utils.emailPattern.test(v)) {
                        callback($.t("config.validators.emailNotValid"));
                        return;
                    }
                    
                    userDelegate.checkUserNameAvailability(v, function(available) {
                        if(!available) {
                            callback($.t("config.validators.emailExists"));
                        } else {
                            callback();
                        }
                    });              
                }
            }
    };
    
    return obj;
});
