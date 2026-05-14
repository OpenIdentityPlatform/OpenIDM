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
 * Copyright 2011-2016 ForgeRock AS.
 */

define(["jquery", "underscore"], function ($, _) {

    var obj = {},
        eventRegistry = {},
        subscriptions = {};

    obj.sendEvent = function (eventId, event) {
        return $.when.apply($, _.map(eventRegistry[eventId], function (eventHandler) {
            var promise = $.Deferred();
            window.setTimeout(function () {
                $.when(eventHandler(event)).always(promise.resolve);
            });
            return promise;
        })).then(function () {
            var promise;
            if (_.has(subscriptions, eventId)) {
                promise = subscriptions[eventId];
                delete subscriptions[eventId];
                promise.resolve();
            }
            return;
        });
    };

    obj.registerListener = function (eventId, callback) {
        if (!_.has(eventRegistry, eventId)) {
            eventRegistry[eventId] = [callback];
        } else {
            eventRegistry[eventId].push(callback);
        }
    };

    obj.unregisterListener = function (eventId, callbackToRemove) {
        if (_.has(eventRegistry, eventId)) {
            if (callbackToRemove !== undefined) {
                eventRegistry[eventId] = _.omit(eventRegistry[eventId], function (callback) {
                    return callback === callbackToRemove;
                });
            } else {
                delete eventRegistry[eventId];
            }
        }
    };

    /**
     * Returns a promise that will be resolved the next time the provided eventId has completed processing.
     */
    obj.whenComplete = function (eventId) {
        if (!_.has(subscriptions, eventId)) {
            subscriptions[eventId] = $.Deferred();
        }
        return subscriptions[eventId];
    };

    return obj;
});
