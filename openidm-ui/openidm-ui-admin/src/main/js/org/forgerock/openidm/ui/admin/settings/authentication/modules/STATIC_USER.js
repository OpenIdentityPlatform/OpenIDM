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
    "form2js",
    "org/forgerock/openidm/ui/admin/settings/authentication/AuthenticationAbstractView"
], function($, _,
            Form2js,
            AuthenticationAbstractView) {

    var StaticUserView = AuthenticationAbstractView.extend({
        template: "templates/admin/settings/authentication/modules/STATIC_USER.html",

        knownProperties: AuthenticationAbstractView.prototype.knownProperties.concat([
            "username",
            "password"
        ]),

        render: function (args) {
            this.data = _.clone(args, true);
            this.data.resources = [ "repo/internal/user" ]; // STATIC and INTERNAL modules only have the single option.
            this.data.customProperties = this.getCustomPropertiesList(this.knownProperties, this.data.config.properties || {});

            this.parentRender(() => {
                this.postRenderComponents({
                    "customProperties":this.data.customProperties,
                    "name": this.data.config.name,
                    "augmentSecurityContext": this.data.config.properties.augmentSecurityContext || {}
                });
            });
        }
    });

    return new StaticUserView();
});
