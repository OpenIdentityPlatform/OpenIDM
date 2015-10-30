/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
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

define("org/forgerock/openidm/ui/admin/settings/authentication/AuthenticationView", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/settings/authentication/AuthenticationAbstractView",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/settings/authentication/SessionModuleView",
    "org/forgerock/openidm/ui/admin/settings/authentication/AuthenticationModuleView"

], function($, _,
            AuthenticationAbstractView,
            Constants,
            SessionModuleView,
            AuthenticationModuleView) {

    var AuthenticationView = AuthenticationAbstractView.extend({
        template: "templates/admin/settings/AuthenticationTemplate.html",
        element: "#authenticationContainer",
        noBaseTemplate: true,
        events: {
            "click #submitAuth": "save"
        },
        data: {},
        model: {},

        render: function (args, callback) {
            this.data.docHelpUrl = Constants.DOC_URL;

            this.retrieveAuthenticationData(_.bind(function() {
                this.parentRender(function () {

                    SessionModuleView.render();
                    AuthenticationModuleView.render({
                        "addedOpenAM": function() {
                            SessionModuleView.addedOpenAM();
                        }
                    });

                    if (callback) {
                        callback();
                    }

                });
            }, this));
        },

        save: function (e) {
            e.preventDefault();
            this.saveAuthentication().then(function() {
                SessionModuleView.render();
                AuthenticationModuleView.render({
                    "addedOpenAM": function () {
                        SessionModuleView.addedOpenAM();
                    }
                });
            });
        }

    });

    return new AuthenticationView();
});
