/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
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

/*global require, define*/
define([
    "text!locales/en/translation.json",
    "text!libs/less-1.5.1-min.js"
], function () {

    /* an unfortunate need to duplicate the file names here, but I haven't
     yet found a way to fool requirejs into doing dynamic dependencies */
    var staticFiles = [
            "locales/en/translation.json",
            "libs/less-1.5.1-min.js"
        ],
        deps = arguments;

    return function (server) {

        _.each(staticFiles, function (file, i) {
            server.respondWith(
                "GET",
                new RegExp(file.replace(/([\/\.\-])/g, "\\$1") + "$"),
                [
                    200,
                    { },
                    deps[i]
                ]
            );
        });

        server.respondWith(
            "GET",
            "/openidm/config/ui/configuration",
            [
                200,
                { },
                "{\"configuration\":{\"defaultNotificationType\":\"info\",\"passwordResetLink\":\"\",\"selfRegistration\":false,\"roles\":{\"openidm-tasks-manager\":\"Tasks Manager\",\"openidm-authorized\":\"User\",\"openidm-admin\":\"Administrator\"},\"notificationTypes\":{\"error\":{\"iconPath\":\"images/notifications/error.png\",\"name\":\"common.notification.types.error\"},\"warning\":{\"iconPath\":\"images/notifications/warning.png\",\"name\":\"common.notification.types.warning\"},\"info\":{\"iconPath\":\"images/notifications/info.png\",\"name\":\"common.notification.types.info\"}},\"siteImages\":[\"images/passphrase/mail.png\",\"images/passphrase/user.png\",\"images/passphrase/report.png\",\"images/passphrase/twitter.png\"],\"siteIdentification\":false,\"lang\":\"en\",\"securityQuestions\":false}}"
            ]
        );


        server.respondWith(
            "GET",
            "/openidm/info/login",
            [
                401,
                { },
                "{ \"code\": 401, \"reason\": \"Unauthorized\", \"message\": \"Access Denied\" }"
            ]
        );

        server.respondWith(
            "GET",
            "/openidm/config/ui/themeconfig",
            [
                200,
                { },
                "{\"icon\":\"favicon.ico\",\"settings\":{\"footer\":{\"mailto\":\"info@forgerock.com\",\"phone\":\"+47-2108-1746\"},\"logo\":{\"title\":\"ForgeRock\",\"src\":\"images/logo.png\",\"height\":\"80\",\"alt\":\"ForgeRock\",\"width\":\"120\"},\"lessVars\":{\"active-menu-font-color\":\"#f9f9f9\",\"color-active\":\"#80b7ab\",\"color-info\":\"blue\",\"color-error\":\"#d97986\",\"header-border-color\":\"#5D5D5D\",\"login-container-width\":\"430px\",\"button-hover-lightness\":\"4%\",\"input-border-invalid-color\":\"#f8b9b3\",\"footer-height\":\"126px\",\"color-warning\":\"yellow\",\"line-height\":\"18px\",\"message-background-color\":\"#fff\",\"column-padding\":\"0px\",\"href-color-hover\":\"#5e887f\",\"input-border-basic\":\"#DBDBDB\",\"font-size\":\"14px\",\"background-font-color\":\"#5a646d\",\"login-container-label-align\":\"left\",\"background-image\":\"url('../images/box-bg.png')\",\"background-position\":\"950px -100px\",\"input-background-invalid-color\":\"#fff\",\"content-background\":\"#f9f9f9\",\"highlight-color\":\"#eeea07\",\"background-color\":\"#eee\",\"inactive-menu-color\":\"#5d6871\",\"href-color\":\"#80b7ab\",\"active-menu-color\":\"#80b7ab\",\"site-width\":\"960px\",\"input-background-color\":\"#fff\",\"color-inactive\":\"gray\",\"inactive-menu-font-color\":\"#f9f9f9\",\"font-family\":\"Arial, Helvetica, sans-serif\",\"font-color\":\"#5a646d\",\"footer-background-color\":\"rgba(238, 238, 238, 0.7)\",\"medium-container-width\":\"850px\",\"color-success\":\"#71bd71\",\"background-repeat\":\"no-repeat\"}}}"
            ]
        );

    };
});
