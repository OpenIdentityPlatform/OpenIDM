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
 * Copyright 2014-2015 ForgeRock AS.
 */

/*global define  */

define("config/process/CommonIDMConfig", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager"
], function($, _, constants, eventManager) {
    var ignorePassword = false,
        obj = [
            {
                startEvent: constants.EVENT_HANDLE_DEFAULT_ROUTE,
                description: "",
                override: true,
                dependencies: [
                    "org/forgerock/commons/ui/common/main/Router",
                    "org/forgerock/commons/ui/common/main/Configuration",
                    "org/forgerock/commons/ui/common/util/ModuleLoader",
                    "org/forgerock/commons/ui/common/SiteConfigurator"
                ],
                processDescription: function(event, router, conf, ModuleLoader, SiteConfigurator) {
                    eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.landingPage });
                    /* if (conf.loggedUser.has("needsResetPassword") && !ignorePassword) {
                        ModuleLoader.load(SiteConfigurator.configuration.delegate).then(function (configurationDelegate) {
                            if (typeof configurationDelegate.checkForDifferences === "function") {
                                configurationDelegate.checkForDifferences();
                            }

                            eventManager.sendEvent(constants.EVENT_SHOW_DIALOG, { route: router.configuration.routes.mandatoryPasswordChangeDialog, base: router.configuration.routes.mandatoryPasswordChangeDialog.base });
                            ignorePassword = true;
                        });

                    } else {
                        eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.landingPage });
                    }*/
                }
            },

            {
                startEvent: constants.EVENT_POLICY_FAILURE,
                description: "Failure to save record due to policy validation",
                dependencies: [ ],
                processDescription: function(event) {
                    var response = event.error.responseObj,
                        failedProperties,
                        errors = "Unknown";

                    if (typeof response === "object" && response !== null &&
                        typeof response.detail === "object" && (response.message === "Failed policy validation" || response.message === "Policy validation failed")) {

                        errors = _.chain(response.detail.failedPolicyRequirements)
                                    .groupBy('property')
                                    .pairs()
                                    .map(function (a) {
                                        return " - " + a[0] + ": " +
                                            _.chain(a[1])
                                                .pluck('policyRequirements')
                                                .map(function (pr) {
                                                    return _.map(pr, function (p) {
                                                        return $.t("common.form.validation." + p.policyRequirement, p.params);
                                                    });
                                                })
                                                .value()
                                                .join(", ");
                                    })
                                    .value()
                                    .join(" <br/> ");

                    }

                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, {key: "resourceValidationError", validationErrors: errors});
                }
            }
        ];

    return obj;
});
