"use strict";

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

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

define(["jquery", "underscore", "org/forgerock/commons/ui/common/main/AbstractModel", "org/forgerock/commons/ui/common/main/Configuration", "org/forgerock/commons/ui/common/util/Constants", "org/forgerock/commons/ui/common/main/EventManager"], function ($, _, AbstractModel, Configuration, Constants, EventManager) {
    var sessionStorageTarget = (opener || window).sessionStorage,
        ServiceInvokerTarget = (opener || window).require("org/forgerock/commons/ui/common/main/ServiceInvoker"),
        UserModel;

    UserModel = AbstractModel.extend({
        protectedAttributeList: [],
        sync: function sync(method, model, options) {
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
            return AbstractModel.prototype.sync.call(this, method, model, options).fail(_.bind(function (xhr) {
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
            }, this)).always(_.bind(function () {
                if (this.has("password")) {
                    // usually password won't be included in the response, but it will for openidm-admin
                    this.unset("password");
                }

                delete this.currentPassword;
            }, this));
        },
        getUIRoles: function getUIRoles(roles) {
            var getRoleFromRef = function getRoleFromRef(role) {
                if (_.isObject(role) && _.has(role, "_ref")) {
                    role = role._ref.split("/").pop();
                }

                return role;
            };

            return _.chain(roles).filter(function (r) {
                return _.has(Configuration.globalData.roles, getRoleFromRef(r));
            }).map(function (r) {
                if (Configuration.globalData.roles[getRoleFromRef(r)] === "ui-user") {
                    return ["ui-user", "ui-self-service-user"];
                } else {
                    return Configuration.globalData.roles[getRoleFromRef(r)];
                }
            }).flatten().value();
        },
        parse: function parse(response) {
            if (_.has(response, "password")) {
                if (_.isString(response.password)) {
                    response.needsResetPassword = true;
                }
                // usually password won't be included in the response, but it will for openidm-admin
                delete response.password;
            }
            return response;
        },
        invalidateSession: function invalidateSession() {
            var promise = new $.Deferred();
            ServiceInvokerTarget.restCall({
                "url": "/" + Constants.context + "/authentication?_action=logout",
                "type": "POST",
                "errorsHandlers": {
                    "unauthorized": {
                        status: "401"
                    }
                }
            }).always(function () {
                return promise.resolve();
            });
            return promise;
        },
        login: function login(username, password) {
            var _this = this;

            var headers = {};
            headers[Constants.HEADER_PARAM_USERNAME] = username;
            headers[Constants.HEADER_PARAM_PASSWORD] = password;
            headers[Constants.HEADER_PARAM_NO_SESSION] = false;

            return this.logout().then(function () {
                return _this.getProfile(headers);
            });
        },
        tokenLogin: function tokenLogin(authToken, provider) {
            var _this2 = this;

            var headers = {};

            headers[Constants.HEADER_PARAM_USERNAME] = "";
            headers[Constants.HEADER_PARAM_PASSWORD] = "";
            headers[Constants.HEADER_PARAM_AUTH_TOKEN] = authToken;
            headers[Constants.HEADER_PARAM_AUTH_PROVIDER] = provider;
            headers[Constants.HEADER_PARAM_NO_SESSION] = false;

            return this.logout().then(function () {
                if (provider === "OPENAM") {
                    sessionStorageTarget.setItem("authDetails", JSON.stringify({ authToken: authToken, provider: provider }));
                }
                return _this2.getProfile(headers);
            });
        },
        /**
         * Updates a header map to include AUTH TOKEN and PROVIDER values from a separate map
         * Will remove them if the separate map is empty.
         * @param {object} currentHeaders the headers that exist present
         * @param {object} authDetails may contain authToken and provider; if present, wil be set in returned header map
         * @returns {object} possibly modified copy of currentHeaders
         */
        setAuthTokenHeaders: function setAuthTokenHeaders(currentHeaders, authDetails) {
            var updatedHeaders = _.cloneDeep(currentHeaders);
            if (authDetails) {
                var _$extend;

                updatedHeaders = _.extend(updatedHeaders, (_$extend = {}, _defineProperty(_$extend, Constants.HEADER_PARAM_AUTH_TOKEN, authDetails.authToken), _defineProperty(_$extend, Constants.HEADER_PARAM_AUTH_PROVIDER, authDetails.provider), _$extend));
            } else {
                delete updatedHeaders[Constants.HEADER_PARAM_AUTH_TOKEN];
                delete updatedHeaders[Constants.HEADER_PARAM_AUTH_PROVIDER];
            }
            return updatedHeaders;
        },
        logout: function logout() {
            var _this3 = this;

            return this.invalidateSession().then(function () {
                sessionStorageTarget.removeItem("authDetails");
                ServiceInvokerTarget.configuration.defaultHeaders = _this3.setAuthTokenHeaders(ServiceInvokerTarget.configuration.defaultHeaders || {}, null);
                return _this3.logoutUrl;
            });
        },
        getProfile: function getProfile(headers) {
            var _this4 = this;

            ServiceInvokerTarget.configuration.defaultHeaders = this.setAuthTokenHeaders(ServiceInvokerTarget.configuration.defaultHeaders || {}, JSON.parse(sessionStorageTarget.getItem("authDetails")));

            return ServiceInvokerTarget.restCall({
                "url": "/" + Constants.context + "/info/login",
                "type": "GET",
                "headers": headers || {},
                "errorsHandlers": {
                    "forbidden": {
                        status: "403"
                    },
                    "unauthorized": {
                        status: "401"
                    }
                }
            }).fail(function () {
                _this4.logout();
            }).then(_.bind(function (sessionDetails) {
                this.id = sessionDetails.authorization.id;
                this.url = "/" + Constants.context + "/" + sessionDetails.authorization.component;
                this.component = sessionDetails.authorization.component;
                this.protectedAttributeList = sessionDetails.authorization.protectedAttributeList || [];
                this.logoutUrl = sessionDetails.authorization.logoutUrl;
                this.baseEntity = this.component + "/" + this.id;
                this.uiroles = this.getUIRoles(sessionDetails.authorization.roles);
                this.provider = sessionDetails.authorization.provider;

                if (sessionDetails.authorization.provider) {
                    this.provider = sessionDetails.authorization.provider;
                } else {
                    this.provider = null;
                }

                if (sessionDetails.authorization.moduleId === "oAuth" || sessionDetails.authorization.moduleId === "OpenIdConnect") {
                    this.userNamePasswordLogin = false;
                } else {
                    this.userNamePasswordLogin = true;
                }

                return this.fetch().then(_.bind(function () {
                    return this;
                }, this));
            }, this));
        },
        getProtectedAttributes: function getProtectedAttributes() {
            return this.protectedAttributeList;
        },
        setCurrentPassword: function setCurrentPassword(currentPassword) {
            this.currentPassword = currentPassword;
        },

        bindProvider: function bindProvider(provider, code, nonce, redirect_uri) {
            var _this5 = this;

            return ServiceInvokerTarget.restCall({
                "type": "POST",
                "url": this.url + "/" + this.id + "?_action=bind&" + $.param({
                    "provider": provider,
                    "redirect_uri": redirect_uri,
                    "nonce": nonce,
                    "code": code
                })
            }).then(function (resp) {
                _this5.set(resp);
                return resp;
            });
        },

        unbindProvider: function unbindProvider(provider) {
            var _this6 = this;

            return ServiceInvokerTarget.restCall({
                "type": "POST",
                "url": this.url + "/" + this.id + "?_action=unbind&" + $.param({
                    "provider": provider
                })
            }).then(function (resp) {
                _this6.set(resp);
                return resp;
            });
        }
    });

    return UserModel;
});
