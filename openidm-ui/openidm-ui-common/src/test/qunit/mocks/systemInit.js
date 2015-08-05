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

/*global define*/

define([ ], function () {

    return function (server) {

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
                "{\"icon\":\"favicon.ico\",\"path\":\"\",\"stylesheet\":\"css/styles.css\",\"settings\":{\"logo\":{\"src\":\"images/logo-horizontal.png\",\"title\":\"ForgeRock\",\"alt\":\"ForgeRock\"},\"loginLogo\":{\"src\":\"images/login-logo.png\",\"title\":\"ForgeRock\",\"alt\":\"ForgeRock\",\"height\":\"104px\",\"width\":\"210px\"},\"footer\":{\"mailto\":\"info@forgerock.com\"}},\"_id\":\"ui/themeconfig\"}"
            ]
        );

    };
});
