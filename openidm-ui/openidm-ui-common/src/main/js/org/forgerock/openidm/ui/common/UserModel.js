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

define([
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
            if (options.silent === true) {
                return this;
            }

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
                .fail(_.bind(function (xhr) {
                    var previous = this.previousAttributes();
                    this.clear();
                    this.set(previous);
                    if (_.isObject(xhr.responseJSON) && _.has(xhr.responseJSON, "code") && xhr.responseJSON.code === 403) {
                        if (xhr.responseJSON.message.indexOf("Reauthentication failed ") === 0) {
                            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "reauthFailed");
                        } else {
                            EventManager.sendEvent(Constants.EVENT_POLICY_FAILURE, {
                                error: {
                                    responseObj: xhr.responseJSON
                                }
                            });
                        }
                    }
                }, this))
                .always(_.bind(function () {
                    if (this.has("password")) {
                        // usually password won't be included in the response, but it will for openidm-admin
                        this.unset("password");
                    }

                    delete this.currentPassword;
                }, this));
        },
        getUIRoles: function (roles) {
            var getRoleFromRef = function (role) {
                if (_.isObject(role) && _.has(role, "_ref")) {
                    role = role._ref.split("/").pop();
                }

                return role;
            };

            return _.chain(roles)
                    .filter(function (r) {
                        return _.has(Configuration.globalData.roles, getRoleFromRef(r));
                    })
                    .map(function (r) {
                        if (Configuration.globalData.roles[getRoleFromRef(r)] === "ui-user") {
                            return ["ui-user","ui-self-service-user"];
                        } else {
                            return Configuration.globalData.roles[getRoleFromRef(r)];
                        }
                    })
                    .flatten()
                    .value();
        },
        parse: function (response) {
            if (_.has(response, "password")) {
                if (_.isString(response.password)) {
                    response.needsResetPassword = true;
                }
                // usually password won't be included in the response, but it will for openidm-admin
                delete response.password;
            }
            return response;
        },
        invalidateSession: function () {
            return ServiceInvoker.restCall({
                "url": "/" + Constants.context + "/authentication?_action=logout",
                "type" : "POST"
            });
        },
        login: function (username, password) {
            var headers = {};
            headers[Constants.HEADER_PARAM_USERNAME] = username;
            headers[Constants.HEADER_PARAM_PASSWORD] = password;
            headers[Constants.HEADER_PARAM_NO_SESSION] = false;

            return this.getProfile(headers);
        },
        tokenLogin: function (authToken, provider) {
            var headers = {};

            headers[Constants.HEADER_PARAM_USERNAME] = "";
            headers[Constants.HEADER_PARAM_PASSWORD] = "";
            headers[Constants.HEADER_PARAM_AUTH_TOKEN] = authToken;
            headers[Constants.HEADER_PARAM_AUTH_PROVIDER] = provider;
            headers[Constants.HEADER_PARAM_NO_SESSION] = false;

            if (provider === "OPENAM") {
                sessionStorage.setItem("authDetails", JSON.stringify({authToken,provider}));
            }
            return this.getProfile(headers);
        },
        /**
         * Updates a header map to include AUTH TOKEN and PROVIDER values from a separate map
         * Will remove them if the separate map is empty.
         * @param {object} currentHeaders the headers that exist present
         * @param {object} authDetails may contain authToken and provider; if present, wil be set in returned header map
         * @returns {object} possibly modified copy of currentHeaders
         */
        setAuthTokenHeaders: (currentHeaders, authDetails) => {
            let updatedHeaders = _.cloneDeep(currentHeaders);
            if (authDetails) {
                updatedHeaders = _.extend({
                    [Constants.HEADER_PARAM_AUTH_TOKEN] : authDetails.authToken,
                    [Constants.HEADER_PARAM_AUTH_PROVIDER] : authDetails.provider
                }, updatedHeaders);
            } else {
                delete updatedHeaders[Constants.HEADER_PARAM_AUTH_TOKEN];
                delete updatedHeaders[Constants.HEADER_PARAM_AUTH_PROVIDER];
            }
            return updatedHeaders;
        },
        logout: function () {
            if (this.id) {
                return this.invalidateSession().then(() => {
                    sessionStorage.removeItem("authDetails");
                    ServiceInvoker.configuration.defaultHeaders = this.setAuthTokenHeaders(
                        ServiceInvoker.configuration.defaultHeaders || {},
                        null
                    );
                });
            } else {
                return $.Deferred().resolve();
            }
        },
        getProfile: function (headers) {
            ServiceInvoker.configuration.defaultHeaders = this.setAuthTokenHeaders(
                ServiceInvoker.configuration.defaultHeaders || {},
                JSON.parse(sessionStorage.getItem("authDetails"))
            );

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
            })
            .fail(() => {
                this.logout();
            })
            .then(_.bind(function (sessionDetails) {
                this.id = sessionDetails.authorization.id;
                this.url = "/" + Constants.context + "/" + sessionDetails.authorization.component;
                this.component = sessionDetails.authorization.component;
                this.protectedAttributeList = sessionDetails.authorization.protectedAttributeList || [];
                this.baseEntity = this.component + "/" + this.id;
                this.uiroles = this.getUIRoles(sessionDetails.authorization.roles);
                this.provider =  sessionDetails.authorization.provider;

                if(sessionDetails.authorization.provider) {
                    this.provider = sessionDetails.authorization.provider;
                } else {
                    this.provider = null;
                }

                if(sessionDetails.authorization.moduleId === "oAuth" || sessionDetails.authorization.moduleId === "OpenIdConnect") {
                    this.userNamePasswordLogin = false;
                } else {
                    this.userNamePasswordLogin = true;
                }

                return this.fetch().then(_.bind(function () {
                    return this;
                }, this));
            }, this));
        },
        getProtectedAttributes: function () {
            return this.protectedAttributeList;
        },
        setCurrentPassword: function (currentPassword) {
            this.currentPassword = currentPassword;
        },

        bindProvider: function (provider, code, nonce, redirect_uri) {
            return ServiceInvoker.restCall({
                "type": "POST",
                "url": this.url + "/" + this.id +"?_action=bind&" +
                $.param({
                    "provider": provider,
                    "redirect_uri": redirect_uri,
                    "nonce": nonce,
                    "code": code
                })
            }).then((resp) => {
                this.set(resp);
                return resp;
            });
        },

        unbindProvider: function (provider) {
            return ServiceInvoker.restCall({
                "type": "POST",
                "url": this.url + "/" + this.id + "?_action=unbind&" +
                $.param({
                    "provider": provider
                })
            }).then((resp) => {
                this.set(resp);
                return resp;
            });
        }
    });

    return UserModel;
});
