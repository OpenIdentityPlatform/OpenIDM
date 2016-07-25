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
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/common/delegates/SiteConfigurationDelegate",
    "org/forgerock/commons/ui/common/components/Navigation",
    "org/forgerock/openidm/ui/common/delegates/SocialDelegate",
    "UserProfileView"
], function($, _, conf,
            commonSiteConfigurationDelegate,
            nav,
            SocialDelegate,
            UserProfileView) {

    var obj = Object.create(commonSiteConfigurationDelegate),
        hasKba = false,
        hasSocialProviders = false,
        cachedUserComponent = null;

    obj.adminCheck = false;

    obj.getConfiguration = function (successCallback, errorCallback) {
        return $.when(
            commonSiteConfigurationDelegate.getConfiguration(),
            SocialDelegate.providerList()
        ).then(function (configuration, socialProviders) {
            if (socialProviders.providers.length > 0) {
                hasSocialProviders = true;
            }

            if (configuration.kbaEnabled === true) {
                hasKba = true;
            }

            if (successCallback) {
                successCallback(configuration);
            }
            return configuration;
        }, errorCallback);
    };

    obj.checkForDifferences = function(){
        var tabList = [];

        // every time the logged-in user component changes, reregister appropriate profile tabs
        if (conf.loggedUser && conf.loggedUser.component !== cachedUserComponent) {
            cachedUserComponent = conf.loggedUser.component;
            UserProfileView.resetTabs();
            // repo/internal/user records don't support "fancy" tabs like kba and social providers
            if (conf.loggedUser.component !== "repo/internal/user") {
                if (hasSocialProviders) {
                    tabList.push("org/forgerock/openidm/ui/user/profile/SocialIdentitiesTab");
                }
                if (hasKba) {
                    tabList.push("org/forgerock/commons/ui/user/profile/UserProfileKBATab");
                }
            }
            if (tabList.length) {
                require(tabList, function () {
                    _.each(_.toArray(arguments), UserProfileView.registerTab, UserProfileView);
                });
            }
        }

        if (conf.loggedUser && _.contains(conf.loggedUser.uiroles,"ui-admin") && !obj.adminCheck){
            nav.configuration.userBar.unshift({
                "id": "admin_link",
                "href": "/admin",
                "i18nKey": "openidm.admin.label"
            });

            obj.adminCheck = true;
        }

        nav.reload();
        return $.Deferred().resolve();
    };


    return obj;
});
