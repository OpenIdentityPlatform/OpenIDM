/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
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

/*global require, define*/
define([
    "text!templates/admin/AdminBaseTemplate.html",
    "text!templates/admin/DashboardTemplate.html",
    "text!templates/common/DialogTemplate.html",
    "text!templates/admin/MandatoryPasswordChangeDialogTemplate.html"
], function () {

    /* an unfortunate need to duplicate the file names here, but I haven't
     yet found a way to fool requirejs into doing dynamic dependencies */
    var staticFiles = [
            "templates/admin/AdminBaseTemplate.html",
            "templates/admin/DashboardTemplate.html",
            "templates/common/DialogTemplate.html",
            "templates/admin/MandatoryPasswordChangeDialogTemplate.html"
        ],
        deps = arguments;

    return function (server) {
    
        _.each(staticFiles, function (file, i) {
            server.respondWith(
                "GET",
                new RegExp(file.replace(/([\/\.\-])/g, "\\$1") + "$"),
                [
                    200,
                    { },
                    deps[i]
                ]
            );
        });

        server.respondWith(
            "GET",   
            "/openidm/info/login",
            [
                200, 
                { },
                "{\"authorizationId\":{\"id\":\"openidm-admin\",\"component\":\"repo/internal/user\",\"roles\":[\"openidm-admin\",\"openidm-authorized\"]},\"parent\":{\"id\":\"c0ab0f10-94dc-4c9c-9367-1e29f420b1b6\",\"parent\":null,\"class\":\"org.forgerock.json.resource.RootContext\"},\"class\":\"org.forgerock.json.resource.SecurityContext\",\"authenticationId\":\"openidm-admin\"}"
            ]
        );
    
        server.respondWith(
            "GET",   
            "/openidm/repo/internal/user/openidm-admin",
            [
                200, 
                { },
                "{\"_id\":\"openidm-admin\",\"_rev\":\"1\",\"roles\":\"openidm-admin,openidm-authorized\",\"userName\":\"openidm-admin\",\"password\":\"openidm-admin\"}"
            ]
        );
    
        server.respondWith(
            "GET",   
            "/openidm/policy/repo/internal/user/openidm-admin",
            [
                200, 
                { },
                "{\"resource\":\"repo/internal/user/*\",\"properties\":[{\"policyRequirements\":[\"CANNOT_CONTAIN_CHARACTERS\"],\"policies\":[{\"policyRequirements\":[\"CANNOT_CONTAIN_CHARACTERS\"],\"params\":{\"forbiddenChars\":[\"/\"]},\"policyId\":\"cannot-contain-characters\",\"policyFunction\":\"\\nfunction (fullObject, value, params, property) {\\n    var i, join = function (arr, d) {\\n        var j, list = \\\"\\\";\\n        for (j in arr) {\\n            list += arr[j] + d;\\n        }\\n        return list.replace(new RegExp(d + \\\"$\\\"), \\\"\\\");\\n    };\\n    if (typeof (value) === \\\"string\\\" && value.length) {\\n        for (i in params.forbiddenChars) {\\n            if (value.indexOf(params.forbiddenChars[i]) !== -1) {\\n                return [{\\\"policyRequirement\\\":\\\"CANNOT_CONTAIN_CHARACTERS\\\", \\\"params\\\":{\\\"forbiddenChars\\\":join(params.forbiddenChars, \\\", \\\")}}];\\n            }\\n        }\\n    }\\n    return [];\\n}\\n\"}],\"name\":\"_id\"},{\"policyRequirements\":[\"REQUIRED\",\"AT_LEAST_X_CAPITAL_LETTERS\",\"AT_LEAST_X_NUMBERS\",\"MIN_LENGTH\"],\"policies\":[{\"policyRequirements\":[\"REQUIRED\"],\"policyId\":\"required\",\"policyFunction\":\"\\nfunction (fullObject, value, params, propName) {\\n    if (value === undefined) {\\n        return [{\\\"policyRequirement\\\":\\\"REQUIRED\\\"}];\\n    }\\n    return [];\\n}\\n\"},{\"policyRequirements\":[\"REQUIRED\"],\"policyId\":\"not-empty\",\"policyFunction\":\"\\nfunction (fullObject, value, params, property) {\\n    if (value !== undefined && (value === null || !value.length)) {\\n        return [{\\\"policyRequirement\\\":\\\"REQUIRED\\\"}];\\n    } else {\\n        return [];\\n    }\\n}\\n\"},{\"policyRequirements\":[\"AT_LEAST_X_CAPITAL_LETTERS\"],\"params\":{\"numCaps\":1},\"policyId\":\"at-least-X-capitals\",\"policyFunction\":\"\\nfunction (fullObject, value, params, property) {\\n    var isRequired = _.find(this.failedPolicyRequirements, function (fpr) {\\n        return fpr.policyRequirement === \\\"REQUIRED\\\";\\n    }), isNonEmptyString = (typeof (value) === \\\"string\\\" && value.length), valuePassesRegexp = (function (v) {\\n        var test = isNonEmptyString ? v.match(/[(A-Z)]/g) : null;\\n        return test !== null && test.length >= params.numCaps;\\n    }(value));\\n    if ((isRequired || isNonEmptyString) && !valuePassesRegexp) {\\n        return [{\\\"policyRequirement\\\":\\\"AT_LEAST_X_CAPITAL_LETTERS\\\", \\\"params\\\":{\\\"numCaps\\\":params.numCaps}}];\\n    }\\n    return [];\\n}\\n\"},{\"policyRequirements\":[\"AT_LEAST_X_NUMBERS\"],\"params\":{\"numNums\":1},\"policyId\":\"at-least-X-numbers\",\"policyFunction\":\"\\nfunction (fullObject, value, params, property) {\\n    var isRequired = _.find(this.failedPolicyRequirements, function (fpr) {\\n        return fpr.policyRequirement === \\\"REQUIRED\\\";\\n    }), isNonEmptyString = (typeof (value) === \\\"string\\\" && value.length), valuePassesRegexp = (function (v) {\\n        var test = isNonEmptyString ? v.match(/\\\\d/g) : null;\\n        return test !== null && test.length >= params.numNums;\\n    }(value));\\n    if ((isRequired || isNonEmptyString) && !valuePassesRegexp) {\\n        return [{\\\"policyRequirement\\\":\\\"AT_LEAST_X_NUMBERS\\\", \\\"params\\\":{\\\"numNums\\\":params.numNums}}];\\n    }\\n    return [];\\n}\\n\"},{\"policyRequirements\":[\"MIN_LENGTH\"],\"params\":{\"minLength\":8},\"policyId\":\"minimum-length\",\"policyFunction\":\"\\nfunction (fullObject, value, params, property) {\\n    var isRequired = _.find(this.failedPolicyRequirements, function (fpr) {\\n        return fpr.policyRequirement === \\\"REQUIRED\\\";\\n    }), isNonEmptyString = (typeof (value) === \\\"string\\\" && value.length), hasMinLength = isNonEmptyString ? (value.length >= params.minLength) : false;\\n    if ((isRequired || isNonEmptyString) && !hasMinLength) {\\n        return [{\\\"policyRequirement\\\":\\\"MIN_LENGTH\\\", \\\"params\\\":{\\\"minLength\\\":params.minLength}}];\\n    }\\n    return [];\\n}\\n\"}],\"name\":\"password\"}]}"
            ]
        );
    
        server.respondWith(
            "POST",   
            "/openidm/policy/repo/internal/user/openidm-admin?_action=validateObject",
            [
                200, 
                { },
                "{\"result\":false,\"failedPolicyRequirements\":[{\"policyRequirements\":[{\"policyRequirement\":\"REQUIRED\"}],\"property\":\"password\"},{\"policyRequirements\":[{\"policyRequirement\":\"AT_LEAST_X_CAPITAL_LETTERS\",\"params\":{\"numCaps\":1}}],\"property\":\"password\"},{\"policyRequirements\":[{\"policyRequirement\":\"AT_LEAST_X_NUMBERS\",\"params\":{\"numNums\":1}}],\"property\":\"password\"},{\"policyRequirements\":[{\"policyRequirement\":\"MIN_LENGTH\",\"params\":{\"minLength\":8}}],\"property\":\"password\"}]}"
            ]
        );

    };

});
