/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

/*global define, $, _, Handlebars, form2js, window */

define("org/forgerock/openidm/ui/admin/connector/oauth/GoogleTypeView", [
    "org/forgerock/openidm/ui/admin/connector/oauth/AbstractOAuthView",
    "org/forgerock/openidm/ui/admin/delegates/ExternalAccessDelegate"
], function(AbstractOAuthView, ExternalAccessDelegate) {

    var GoogleTypeView = AbstractOAuthView.extend({
        getScopes: function() {
            var googleScope = "https://www.googleapis.com/auth/admin.directory.group%20"
                +"https://www.googleapis.com/auth/admin.directory.orgunit%20"
                +"https://www.googleapis.com/auth/admin.directory.user%20"
                +"https://www.googleapis.com/auth/apps.licensing";

            return googleScope;
        },

        getToken: function(mergedResult, oAuthCode) {
            return ExternalAccessDelegate.getToken(mergedResult.configurationProperties.clientSecret,
                mergedResult.configurationProperties.clientId, oAuthCode,
                window.location.protocol+"//"+window.location.host + "/admin/oauth.html",
                "https://accounts.google.com/o/oauth2/token");
        }
    });

    return new GoogleTypeView();
});