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
 * Portions copyright 2020-2026 3A Systems LLC.
 */

define(["jquery", "underscore", "org/forgerock/commons/ui/common/main/AbstractConfigurationAware", "org/forgerock/commons/ui/common/util/Constants", "org/forgerock/commons/ui/common/main/EventManager", "org/forgerock/commons/ui/common/main/Configuration", "org/forgerock/commons/ui/common/util/ModuleLoader"], function ($, _, AbstractConfigurationAware, constants, eventManager, conf, ModuleLoader) {
    var obj = new AbstractConfigurationAware();

    obj.initialized = false;

    eventManager.registerListener(constants.EVENT_READ_CONFIGURATION_REQUEST, function () {
        if (!conf.globalData) {
            conf.setProperty('globalData', {});
            conf.globalData.auth = {};
        }

        if (!conf.delegateCache) {
            conf.setProperty('delegateCache', {});
        }

        if (obj.configuration && obj.initialized === false) {
            obj.initialized = true;

            if (obj.configuration.remoteConfig === true) {
                ModuleLoader.load(obj.configuration.delegate).then(function (configurationDelegate) {
                    configurationDelegate.getConfiguration(function (config) {
                        obj.processConfiguration(config);
                        eventManager.sendEvent(constants.EVENT_APP_INITIALIZED);
                    }, function () {
                        obj.processConfiguration({});
                        eventManager.sendEvent(constants.EVENT_APP_INITIALIZED);
                    });
                });
            } else {
                obj.processConfiguration(obj.configuration);
                eventManager.sendEvent(constants.EVENT_APP_INITIALIZED);
            }
        }
    });

    obj.processConfiguration = function (config) {
        // whatever settings were found will be saved in globalData
        _.extend(conf.globalData, config);

        if (config.defaultNotificationType) {
            conf.defaultType = config.defaultNotificationType;
        }

        if (config.notificationTypes) {
            conf.notificationTypes = config.notificationTypes;
        }

        if (config.roles) {
            conf.globalData.userRoles = config.roles;
        }

        conf.globalData.auth.cookieName = config.cookieName;
        conf.globalData.auth.cookieDomains = config.domains;
        conf.globalData.auth.cookieSameSite = config.cookieSameSite;
    };

    obj.configurePage = function (route, params) {
        var promise = $.Deferred();

        if (obj.configuration.remoteConfig === true) {
            ModuleLoader.load(obj.configuration.delegate).then(function (configurationDelegate) {
                if (typeof configurationDelegate.checkForDifferences === "function") {
                    configurationDelegate.checkForDifferences(route, params).then(function (config) {
                        if (config) {
                            obj.processConfiguration(config);
                        }
                        promise.resolve();
                    });
                } else {
                    promise.resolve();
                }
            });
        } else {
            promise.resolve();
        }

        return promise;
    };

    return obj;
});
