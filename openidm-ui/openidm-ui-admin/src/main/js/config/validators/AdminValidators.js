/**
 * Created by forgerock on 8/7/14.
 */
/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
 */

/*global define, $, _ */

define("config/validators/AdminValidators", [
], function(constants, eventManager) {
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
        }, "spaceCheck": {
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
        }
    };

    return obj;
});
