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

define([
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/common/delegates/SiteConfigurationDelegate",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/components/Navigation"
], function($, _,
            AbstractDelegate,
            conf,
            commonSiteConfigurationDelegate,
            ConfigDelegate,
            eventManager,
            Constants,
            Navigation) {

    var SiteConfigurationDelegate = function (url) {
            AbstractDelegate.call(this, url);
            return this;
        },
        currentSelfServiceStates = { };

    SiteConfigurationDelegate.prototype = Object.create(commonSiteConfigurationDelegate);

    SiteConfigurationDelegate.prototype.getConfiguration = function (successCallback) {
        return commonSiteConfigurationDelegate.getConfiguration.call(this).then(function (config) {
            // In the admin context, these are always false.
            config.passwordReset = false;
            config.selfRegistration = false;
            config.forgotUsername = false;
            successCallback(config);
        });
    };

    SiteConfigurationDelegate.prototype.updateConfiguration = function (callback) {
        this.getConfiguration(function(result) {
            conf.globalData = result;

            if (callback) {
                callback();
            }
        });
    };

    SiteConfigurationDelegate.prototype.checkForDifferences = function(){
        var promise = $.Deferred();

        if (conf.loggedUser) {

            ConfigDelegate.readEntity("ui/configuration").then(function (uiConfig) {

                if (
                    (_.contains(conf.loggedUser.uiroles,"ui-admin") &&
                    Navigation.configuration.links && Navigation.configuration.links.admin &&
                    Navigation.configuration.links.admin.urls &&
                    Navigation.configuration.links.admin.urls.managed &&
                    Navigation.configuration.links.admin.urls.managed.urls &&
                    Navigation.configuration.links.admin.urls.managed.urls.length === 0)

                    ||

                    !_.isEqual(currentSelfServiceStates, _.pick(uiConfig.configuration, ["passwordReset","selfRegistration","forgotUsername"]))
                ) {
                    currentSelfServiceStates = _.pick(uiConfig.configuration, ["passwordReset","selfRegistration","forgotUsername"]);

                    eventManager.sendEvent(Constants.EVENT_UPDATE_NAVIGATION,
                        {
                            callback: function () {
                                promise.resolve();
                            }
                        }
                    );
                } else {
                    promise.resolve();
                }

            });

        } else {
            promise.resolve();
        }

        return promise;
    };

    return new SiteConfigurationDelegate(Constants.host + "/" + Constants.context + "/config/ui/configuration");
});
