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
 * Copyright 2011-2015 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/common/delegates/PolicyDelegate", [
    "lodash",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager"
], function(_, constants, AbstractDelegate, configuration, eventManager) {

    var obj = new AbstractDelegate(constants.host + "/openidm/policy");

    obj.readEntity = function (baseEntity) {
        if (baseEntity === "selfservice/registration") {
            return AbstractDelegate.prototype.readEntity.call(this, "managed/user").then(function (policy) {
                return _.extend(policy, {
                    properties: _.map(policy.properties, function (prop) {
                        return _.extend(prop, {
                            "name": "user." + prop.name
                        });
                    })
                });
            });
        } else if (baseEntity === "selfservice/reset") {
            return AbstractDelegate.prototype.readEntity.call(this, "managed/user").then(function (policy) {
                return _.extend(policy, {
                    properties: _.filter(policy.properties, function (prop) {
                        return prop.name === "password";
                    })
                });
            });
        } else {
            return AbstractDelegate.prototype.readEntity.call(this, baseEntity);
        }
    };

    obj.validateProperty = function (baseEntity, args, callback) {
        var promise;
        /*
         * We are calling the validateObject action here instead of validateProperty
         * because we need to pass in entire object context in order to support policies
         * which may depend upon other properties.  From the response, we look to see if the
         * particular property we are attempting to validate was included in the list of those
         * with errors.
         */

        if (baseEntity === "selfservice/registration") {
            promise = obj.serviceCall({
                suppressSpinner: true,
                url: "/managed/user/-?_action=validateObject",
                data: JSON.stringify(args.fullObject.user),
                type: "POST"
            }).then(function (data) {
                return {
                    failedPolicyRequirements : _.map(data.failedPolicyRequirements, function (failure) {
                        return _.extend(failure, {
                            "property" : "user." + failure.property
                        });
                    })
                };
            });
        } else {
            promise = obj.serviceCall({
                suppressSpinner: true,
                url: "/" + baseEntity + "?_action=validateObject",
                data: JSON.stringify(args.fullObject),
                type: "POST"
            });
        }

        return promise.then(function (data) {
            var properyFailures = _(data.failedPolicyRequirements)
                    .filter(function (failedReq) {
                        return failedReq.property === args.property;
                    })
                    .map(function (failedReq) {
                        return failedReq.policyRequirements;
                    })
                    .flatten()
                    .value();

            if (!properyFailures.length) {
                return {"result": true };
            } else {
                return {
                    "result": false,
                    "failedPolicyRequirements": properyFailures
               };
            }
        })
        .done(function (result) {
            if (callback) {
                callback(result);
            }
        });

    };

    return obj;
});
