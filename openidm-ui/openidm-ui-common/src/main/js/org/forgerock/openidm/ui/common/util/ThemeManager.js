/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/*global define */

define("org/forgerock/openidm/ui/common/util/ThemeManager", [
    "jquery",
    "org/forgerock/openidm/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate"
], function($, constants,conf,configDelegate) {

    var obj = {},
        themePromise;

    obj.loadThemeCSS = function (theme) {
        $('head').find('link[href*=favicon]').remove();

        $("<link/>", {
            rel: "icon",
            type: "image/x-icon",
            href: theme.path + theme.icon
        }).appendTo("head");

        $("<link/>", {
            rel: "shortcut icon",
            type: "image/x-icon",
            href: theme.path + theme.icon
        }).appendTo("head");

        $("<link/>", {
            rel: "stylesheet",
            type: "text/css",
            href: theme.stylesheet
        }).appendTo("head");
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
