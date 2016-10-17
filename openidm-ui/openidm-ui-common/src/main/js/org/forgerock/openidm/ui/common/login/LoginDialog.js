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
 * Copyright 2016 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/LoginDialog",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/common/login/ProviderLoginDialog",
    "org/forgerock/openidm/ui/common/delegates/SocialDelegate"
], function( $, _,
             AbstractView,
             CommonsLoginDialog,
             Configuration,
             ProviderLoginDialog,
             SocialDelegate) {

    var LoginDialog = AbstractView.extend({
        render: function (options) {
            SocialDelegate.loginProviders().then((configuredProviders) => {
                var userNameFlag = Configuration.loggedUser.userNamePasswordLogin;

                if (userNameFlag || configuredProviders.providers.length === 0) {
                    CommonsLoginDialog.render(options);
                } else {
                    ProviderLoginDialog.render(options, Configuration.loggedUser,  configuredProviders);
                }
            });
        }
    });
    return new LoginDialog();
});
