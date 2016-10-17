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
    "org/forgerock/openidm/ui/admin/authentication/AuthenticationAbstractView",
    "org/forgerock/openidm/ui/admin/authentication/SessionModuleView",
    "org/forgerock/openidm/ui/admin/authentication/AuthenticationModuleView",
    "org/forgerock/openidm/ui/admin/authentication/ProvidersView"

], function($, _,
            AuthenticationAbstractView,
            SessionModuleView,
            AuthenticationModuleView,
            ProvidersView) {

    var AuthenticationView = AuthenticationAbstractView.extend({
        template: "templates/admin/authentication/AuthenticationTemplate.html",
        noBaseTemplate: false,
        element: "#content",
        events: {
            "show.bs.tab #providerTab a[data-toggle='tab']": "renderProviders",
            "show.bs.tab #sessionTab a[data-toggle='tab']": "renderSession",
            "show.bs.tab #modulesTab a[data-toggle='tab']": "renderModules"
        },
        data: {},
        model: {},

        render: function (args, callback) {
            this.retrieveAuthenticationData(_.bind(function() {
                this.parentRender(function () {
                    ProvidersView.render();

                    if (callback) {
                        callback();
                    }

                });
            }, this));
        },

        renderProviders: function(e) {
            ProvidersView.render();
        },
        renderSession: function(e) {
            SessionModuleView.render();
        },
        renderModules: function(e) {
            AuthenticationModuleView.render();
        },

        save: function (e) {
            e.preventDefault();

            this.saveAuthentication().then(function() {
                ProvidersView.render();
            });
        }
    });

    return new AuthenticationView();
});
