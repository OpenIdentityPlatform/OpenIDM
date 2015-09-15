/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All rights reserved.
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

/*global define */

define("config/process/IDMConfig", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager"
], function($, _, constants, eventManager) {
    var obj = [
        {
            startEvent: constants.EVENT_NOTIFICATION_DELETE_FAILED,
            description: "Error in deleting notification",
            dependencies: [ ],
            processDescription: function(event) {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "errorDeletingNotification");
            }
        },
        {
            startEvent: constants.EVENT_GET_NOTIFICATION_FOR_USER_ERROR,
            description: "Error in getting notifications",
            dependencies: [ ],
            processDescription: function(event) {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "errorFetchingNotifications");
            }
        },
        {
            startEvent: constants.EVENT_USER_UPDATE_POLICY_FAILURE,
            description: "Failure on update of user",
            dependencies: [ ],
            processDescription: function(event) {
                var response = event.error.responseObj,
                    failedProperties,
                    errors = "Unknown";

                if (typeof response === "object" && response !== null &&
                    typeof response.detail === "object" && response.message === "Failed policy validation") {

                    errors = _.chain(response.detail.failedPolicyRequirements)
                                .groupBy('property')
                                .pairs()
                                .map(function (a) {
                                    return a[0] + ": " +
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
                                .join("; ");

                }

                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, {key: "userValidationError", validationErrors: errors});
            }
        }];

    return obj;
});
