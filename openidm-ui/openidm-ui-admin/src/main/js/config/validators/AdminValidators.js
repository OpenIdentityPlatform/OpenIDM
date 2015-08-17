/**
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Portions copyright 2015 ForgeRock AS.
 */

/*global define */

define("config/validators/AdminValidators", [
    "jquery",
    "underscore"
], function ($, _) {
    var obj = {
        "changed": {
            "name": "Changed field",
            "dependencies": [
            ],
            "validator": function(el, input, callback) {
                callback();
            }
        },
        "requiredURL": {
            "name": "URL required",
            "dependencies": [
            ],
            "validator": function(el, input, callback) {
                var v = $(input).val();
                // This regex verifies there are no spaces in the context and that only valid URL characters are included.
                if (v.length > 0 && !/^[a-zA-Z0-9\-\.\_\~\:\/\?\#\[\]\@\!\$\&\'\(\)\*\+\,\;\=]+$/.test(v)) {
                    callback(["Not a valid URL"]);
                    return;
                }

                if (v === "/openidm" || v === "/admin" ||  v === "/system") {
                    callback(["The URL cannot be one of the following reserved names: \"openidm\", \"admin\" or \"system\"."]);
                    return;
                }

                callback();
            }
        },
        "whitespace": {
            "name": "No whitespace allowed.",
            "dependencies": [
            ],
            "validator": function(el, input, callback) {
                if (/\s/.test($(input).val())) {
                    callback(["Cannot contain spaces"]);
                    return;
                }

                if ($(input).val().length === 0) {
                    callback(["Required"]);
                    return;
                }

                callback();
            }
        },
        "certificate": {
            "name": "Valid Certificate String",
            "dependencies": [
            ],
            "validator": function(el, input, callback) {

                var v = $(input).val();
                if (v.length && !v.match(/\-\-\-\-\-BEGIN CERTIFICATE\-\-\-\-\-\n[^\-]*\n\-\-\-\-\-END CERTIFICATE\-\-\-\-\-\s*$/)) {
                    callback(["Invalid Certificate"]);
                    return;
                }

                callback();
            }
        },
        "bothRequired": {
            "name": "Two Required Fields",
            "dependencies": [
            ],
            "validator": function(el, input, callback) {
                var inputs = input.parent().parent().find("input"),
                    secondInput;

                if(inputs.length !== 2) {
                    callback([$.t("templates.scriptEditor.bothRequired")]);
                    return;
                }

                if (!$(inputs[0]).val() || $(inputs[0]).val() === "") {
                    callback([$.t("templates.scriptEditor.bothRequired")]);
                    return;
                }

                if (!$(inputs[1]).val() || $(inputs[1]).val() === "") {
                    callback([$.t("templates.scriptEditor.bothRequired")]);
                    return;
                }

                secondInput = inputs.not(input);

                if(secondInput.hasClass("field-error")) {
                    secondInput.trigger("blur");
                }

                secondInput.attr("data-validation-status", "ok");

                callback();
            }
        },
        "spaceCheck": {
            "name": "Whitespace validator",
            "dependencies": [
            ],
            "validator": function(el, input, callback) {
                var v = input.val();
                if (!v || v === "") {
                    callback([$.t("common.form.validation.required")]);
                    return;
                }
                else if (v.indexOf(' ') !== -1) {
                    callback([$.t("common.form.validation.spaceNotAllowed")]);
                    return;
                }

                callback();
            }
        },
        "unique": {
            "name": "Unique value",
            "dependencies": [
            ],
            "validator": function(el, input, callback) {
                var v = input.val().toUpperCase().trim(),
                    usedNames = JSON.parse($(input).attr("data-unique-list").toUpperCase());

                if (v.length > 0 && !_.contains(usedNames, v)) {
                    callback();
                } else {
                    callback([$.t("common.form.validation.unique")]);
                }
            }
        }
    };

    return obj;
});
