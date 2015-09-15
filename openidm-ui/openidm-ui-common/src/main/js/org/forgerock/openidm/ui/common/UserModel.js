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

define("org/forgerock/openidm/ui/common/UserModel", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractModel",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/ServiceInvoker"
], function ($, _, AbstractModel, Configuration, Constants, EventManager, ServiceInvoker) {
    var UserModel = AbstractModel.extend({
        protectedAttributeList: [],
        sync: function (method, model, options) {
            var headers = {};
            if (method === "update" || method === "patch") {
                if (this.currentPassword !== undefined) {
                    headers[Constants.HEADER_PARAM_REAUTH] = this.currentPassword;
                    _.extend(options, { "headers": headers });
                }
                options.errorsHandlers = options.errorsHandlers || {
                    "forbidden": {
                        status: "403"
                    }
                };

                if (this.component === "repo/internal/user" && method === "patch") {
                    // patch not yet supported on repo endpoints
                    method = "update";
                }
            }
            return AbstractModel.prototype.sync.call(this, method, model, options)
                .always(_.bind(function () {
                    if (this.has("password")) {
                        // usually password won't be included in the response, but it will for openidm-admin
                        this.unset("password");
                    }

                    delete this.currentPassword;
                }, this))
                .fail(_.bind(function (xhr) {
                    var previous = this.previousAttributes();
                    this.clear();
                    this.set(previous);
                    if (_.isObject(xhr.responseJSON) && _.has(xhr.responseJSON, "code") && xhr.responseJSON.code === 403) {
                        EventManager.sendEvent(Constants.EVENT_USER_UPDATE_POLICY_FAILURE, {
                            error: {
                                responseObj: xhr.responseJSON
                            }
                        });
                    }
                }, this));
        },
        getUIRoles: function (roles) {
            return _.chain(roles)
                    .filter(function (r) {
                        return _.has(Configuration.globalData.roles, r);
                    })
                    .map(function (r) {
                        return Configuration.globalData.roles[r];
                    })
                    .value();
        },
        parse: function (response) {
            var roles = response.roles || [];
            response.roles = roles.concat(this.getUIRoles(roles));

            if (_.has(response, "password")) {
                if (_.isString(response.password)) {
                    response.needsResetPassword = true;
                }
                // usually password won't be included in the response, but it will for openidm-admin
                delete response.password;
            }
            return response;
        },
        login: function (username, password) {
            var headers = {};

            headers[Constants.HEADER_PARAM_USERNAME] = username;
            headers[Constants.HEADER_PARAM_PASSWORD] = password;
            headers[Constants.HEADER_PARAM_NO_SESSION] = false;

            return this.getProfile(headers);
        },
        getProfile: function (headers) {
            return ServiceInvoker.restCall({
                "url": "/" + Constants.context + "/info/login",
                "type" : "GET",
                "headers": headers || {},
                "errorsHandlers": {
                    "forbidden": {
                        status: "403"
                    },
                    "unauthorized": {
                        status: "401"
                    }
                }
            }).then(_.bind(function (sessionDetails) {
                this.id = sessionDetails.authorization.id;
                this.url = "/" + Constants.context + "/" + sessionDetails.authorization.component;
                this.component = sessionDetails.authorization.component;
                return $.when(this.fetch(), this.getValidationRules()).then(_.bind(function () {
                    return this;
                }, this));
            }, this));
        },
        getValidationRules: function () {
            if (!this.policy) {
                return ServiceInvoker.restCall({
                    "url": "/" + Constants.context + "/policy/" + this.component + "/" + this.id
                }).then(_.bind(function (policyRules) {
                    this.policy = policyRules;
                    // derive protectedAttributeList from those properties which have a REAUTH_REQUIRED policy
                    this.protectedAttributeList =
                        _.chain(policyRules.properties)
                         .filter(function (propertyRule) {
                             return _.indexOf(propertyRule.policyRequirements, "REAUTH_REQUIRED") !== -1;
                         })
                         .map(function (propertyRule) {
                             return propertyRule.name;
                         })
                         .value();

                    return policyRules;
                }, this));
            } else {
                return $.Deferred().resolve(this.policy);
            }
        },
        getProtectedAttributes: function () {
            return this.protectedAttributeList;
        },
        setCurrentPassword: function (currentPassword) {
            this.currentPassword = currentPassword;
        }
    });
    return new UserModel();
});
