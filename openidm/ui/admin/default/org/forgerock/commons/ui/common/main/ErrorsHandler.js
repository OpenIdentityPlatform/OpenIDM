"use strict";

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
 * Copyright 2012-2016 ForgeRock AS.
 */

define(["underscore", "jquery", "org/forgerock/commons/ui/common/main/AbstractConfigurationAware", "org/forgerock/commons/ui/common/main/EventManager", "org/forgerock/commons/ui/common/util/Constants"], function (_, $, AbstractConfigurationAware, eventManager, constants) {
    var obj = new AbstractConfigurationAware();

    obj.handleError = function (error, handlers) {
        var handler;

        if (error.error && !error.status) {
            error.status = error.error;
        }

        if (error.hasOwnProperty('responseText')) {
            try {
                error.responseObj = $.parseJSON(error.responseText);
            } catch (parseErr) {/* Must not be JSON */}
        }

        if (handlers) {
            //find match in handlers
            handler = obj.matchError(error, handlers);
        }

        if (!handler) {
            //find match in default handlers
            handler = obj.matchError(error, obj.configuration.defaultHandlers);
        }

        if (handler) {
            // conditional check needed here until calls to authentication?_action=reauthenticate and
            // OpenAM authentication no longer produce 403 status
            if (error.hasOwnProperty("responseObj") && error.responseObj !== null && !(error.responseObj.code === 403 && error.responseObj.message === "SSO Token cannot be retrieved." || error.responseObj.error === 403 && error.responseObj.message === "Reauthentication failed" || error.responseObj.error === 409 && error.responseObj.message.match(/value to replace not found$/))) {
                if (handler.event) {
                    eventManager.sendEvent(handler.event, { handler: handler, error: error });
                }

                if (handler.message) {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, handler.message);
                }
            }
        } else {
            console.error(error.status);
            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "unknown");
        }
    };

    /**
     * If a generic field for comparison is defined on the handler, that field is then compared to the corresponding
     * field on the error.
     *
     * If there is a status defined on the handler, it is compared to the status of the error.
     *
     * If either of these comparisons is true than that handler is returned.
     *
     * @example
     *  errorsHandlers: {
     *      "timeout": {
     *          "value": 0,
     *          "field": "readyState"
     *      }
     *  }
     *
     * @param error
     * @param handlers
     * @returns {*}
     */
    obj.matchError = function (error, handlers) {
        var handler, handlerName;

        for (handlerName in handlers) {
            handler = handlers[handlerName];

            if (!_.isUndefined(handler.field)) {
                if (error[handler.field] === handler.value) {
                    return handler;
                }
            }

            if (handler.status) {
                if (parseInt(error.status, 0) === parseInt(handler.status, 0)) {
                    return handler;
                }
            }

            //TODO add support for openidm errors
        }
    };

    return obj;
});
