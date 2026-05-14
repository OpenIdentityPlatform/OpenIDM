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
 * Copyright 2015-2016 ForgeRock AS.
 */

define(["jquery", "underscore", "org/forgerock/commons/ui/common/main/AbstractDelegate", "org/forgerock/commons/ui/common/util/Constants"], function ($, _, AbstractDelegate, Constants) {

    var AnonymousProcessDelegate = function AnonymousProcessDelegate(path, token, additional) {
        this.token = token;
        this.additional = additional || "";
        return AbstractDelegate.call(this, "/" + Constants.context + "/" + path);
    };

    AnonymousProcessDelegate.prototype = Object.create(AbstractDelegate.prototype);
    AnonymousProcessDelegate.prototype.constructor = AnonymousProcessDelegate;

    AnonymousProcessDelegate.prototype.start = function () {
        if (!this.token || !this.lastResponse) {
            return this.serviceCall({
                "type": "GET",
                "url": ""
            }).done(function (response) {
                this.lastResponse = response;
            });
        } else {
            // the presence of a token means this can be treated as more of a "resume" than a start
            return $.Deferred().resolve(this.lastResponse);
        }
    };

    /**
     * Takes a generic object as input to submit to the process, intended to fulfill the requirements
     * outlined by the previous request.
     * @returns {Object} A promise that is resolved when the backend responses to the provided input
     */
    AnonymousProcessDelegate.prototype.submit = function (input) {
        return this.serviceCall({
            "type": "POST",
            "url": "?_action=submitRequirements" + this.additional,
            "data": JSON.stringify({
                "token": this.token,
                "input": input
            }),
            "errorsHandlers": {
                "failed": {
                    status: "400"
                }
            }
        }).then(_.bind(function (response) {
            if (_.has(response, "token")) {
                this.token = response.token;
            }
            this.lastResponse = response;
            return response;
        }, this), _.bind(function (xhr) {
            delete this.token;
            delete this.lastResponse;
            return {
                "status": {
                    "success": false,
                    "reason": xhr.responseJSON.message
                }
            };
        }, this));
    };

    return AnonymousProcessDelegate;
});
