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
    "lodash",
    "org/forgerock/openidm/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate"
], function($, _, constants,conf,configDelegate) {

    var obj = {},
        themePromise;

    obj.loadThemeCSS = function (theme) {
        $('head').find('link[href*=favicon]').remove();

        $("<link/>", {
            rel: "icon",
            type: "image/x-icon",
            href: require.toUrl(theme.path + theme.icon)
        }).appendTo("head");

        $("<link/>", {
            rel: "shortcut icon",
            type: "image/x-icon",
            href: require.toUrl(theme.path + theme.icon)
        }).appendTo("head");

        _.forEach(theme.stylesheets, function(stylesheet) {
            $("<link/>", {
                rel: "stylesheet",
                type: "text/css",
                href: require.toUrl(stylesheet)
            }).appendTo("head");
        });
    };


    obj.loadThemeConfig = function(){
        var prom = $.Deferred();
        //check to see if the config file has been loaded already
        //if so use what is already there if not load it
        if(conf.globalData.themeConfig){
            prom.resolve(conf.globalData.themeConfig);
            return prom;
        }
        else{
            return configDelegate.readEntity("ui/themeconfig");
        }
    };

    obj.getTheme = function () {
        if (themePromise === undefined) {
            themePromise = obj.loadThemeConfig().then(function (themeConfig) {
                conf.globalData.theme = themeConfig;
                obj.loadThemeCSS(themeConfig);
                return themeConfig;
            });
        }
        return themePromise;
    };


    return obj;
});
