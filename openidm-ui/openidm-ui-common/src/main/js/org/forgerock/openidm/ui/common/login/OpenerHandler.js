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
    "lodash",
    "handlebars",
    "org/forgerock/commons/ui/common/main/Router"
], function($, _,
            Handlebars,
            Router) {

    var OpenerLoginView = function () {},
        obj;

    obj = new OpenerLoginView();

    obj.render = function (args) {
        var params = Router.convertCurrentUrlToJSON().params,
            moduleName,
            type = args[0];

        if(type.includes("bind/")) {
            type = "bind";
        }

        switch (type) {
            case "loginDialog":
                moduleName = "org/forgerock/openidm/ui/common/login/ProviderLoginDialog";
                break;
            case "bind":
                moduleName = "org/forgerock/openidm/ui/user/profile/SocialIdentitiesTab";
                break;
            default:
                moduleName = null;
                window.location = "#noOpenerFound";
                break;
        }

        if(!_.isNull(moduleName)) {
            opener.require([moduleName], function(module){
                module.oauthReturn(params);

                window.close();
            });
        }

        return;
    };

    return obj;
});
