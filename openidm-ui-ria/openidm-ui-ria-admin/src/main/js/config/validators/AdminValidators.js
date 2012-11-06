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
            "adminUserProfileUserName": {
                "name": "Correct and unique username but can be same as was",
                "dependencies": [
                    "org/forgerock/commons/ui/common/util/ValidatorsUtils",
                    "org/forgerock/commons/ui/common/main/Configuration"
                ],
                "validator": function(el, input, callback, utils, conf) {
                    var v = $(input).val();
                    
                    if(v === "") {
                        callback($.t("common.form.validation.required"));
                        return;
                    }
                    
                    if($(el).find("input[name=oldUserName]").val() === v) {
                        callback();
                        return;
                    }
                    
                    userDelegate.checkUserNameAvailability(v, function(available) {
                        if(!available) {
                            callback($.t("common.form.validation.emailExists"));
                        } else {
                            callback();
                        }
                    });              
                }
            },
            "required_long": {
                "name": "Not empty number",
                "dependencies": [
                ],
                "validator": function(el, input, callback) {
                    var v = $(input).val();
                    if(v === "") {
                        callback($.t("common.form.validation.required"));
                        return;
                    }
                    if(!v.match(/^([0-9]+)$/)) {
                        callback($.t("common.form.validation.shouldBeLong"));
                        return;
                    }
                    callback();
                }
            },
            "long": {
                "name": "Number",
                "dependencies": [
                ],
                "validator": function(el, input, callback) {
                    var v = $(input).val();
                    if(v !== "" && !v.match(/^([0-9]+)$/)) {
                        callback($.t("common.form.validation.shouldBeLong"));
                        return;
                    }
                    callback();
                }
            },
            "required_formattedDate": {
                "name": "Not empty, formatted date",
                "dependencies": [
                    "org/forgerock/commons/ui/common/util/DateUtil"
                ],
                "validator": function(el, input, callback, dateUtil) {
                    var valueToReplace, date, v = $(input).val(), dateFormat = $(input).parent().find('[name=dateFormat]').val();
                    if(v === "") {
                        callback($.t("common.form.validation.required"));
                        return;
                    }
                    if(!dateUtil.isDateStringValid(v, dateFormat)) {
                        callback($.t("common.form.validation.wrongDateFormat") + " (" + dateFormat + ")");
                        return;
                    } else {
                        date = dateUtil.parseDateString(v, dateFormat);
                        valueToReplace = dateUtil.formatDate(date,dateFormat);
                        if (dateUtil.isDateStringValid(valueToReplace, dateFormat)) {
                            $(input).val(valueToReplace);
                        } else {
                            callback($.t("common.form.validation.wrongDateFormat") + " (" + dateFormat + ")");
                            return;
                        }
                    }
                    callback();
                }
            },
            "formattedDate": {
                "name": "Not empty, formatted date",
                "dependencies": [
                    "org/forgerock/commons/ui/common/util/DateUtil"
                ],
                "validator": function(el, input, callback, dateUtil) {
                    var valueToReplace, date, v = $(input).val(), dateFormat = $(input).parent().find('[name=dateFormat]').val();
                    if(v !== ""){
                        if (!dateUtil.isDateStringValid(v, dateFormat)) {
                        callback($.t("common.form.validation.wrongDateFormat") + " (" + dateFormat + ")");
                        return;
                        } else {
                            date = dateUtil.parseDateString(v, dateFormat);
                            valueToReplace = dateUtil.formatDate(date,dateFormat);
                            if (dateUtil.isDateStringValid(valueToReplace, dateFormat)) {
                                $(input).val(valueToReplace);
                            } else {
                                callback($.t("common.form.validation.wrongDateFormat") + " (" + dateFormat + ")");
                                return;
                            }
                        }
                    }
                    callback();
                }
            }
    };
    
    return obj;
});
