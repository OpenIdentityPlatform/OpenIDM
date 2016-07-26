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
 * Copyright 2015-2016 ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "form2js",
    "jsonEditor",
    "org/forgerock/openidm/ui/admin/settings/authentication/AuthenticationAbstractView"

], function($, _,
            Form2js,
            JSONEditor,
            AuthenticationAbstractView) {

    var OpenAMSessionView = AuthenticationAbstractView.extend({
        template: "templates/admin/settings/authentication/modules/OPENAM_SESSION.html",

        knownProperties: AuthenticationAbstractView.prototype.knownProperties.concat([
            "openamDeploymentUrl",
            "openamUseExclusively",
            "openamLoginUrl",
            "openamLoginLinkText",
            "openamSSOTokenCookieName",
            "openamUserAttribute",
            "groupComparisonMethod",
            "truststoreType",
            "truststorePath",
            "truststorePassword"
        ]),

        render: function (args) {
            this.data = _.clone(args, true);
            this.data.userOrGroupValue = "userRoles";
            this.data.userOrGroupOptions = _.clone(AuthenticationAbstractView.prototype.userOrGroupOptions, true);
            this.data.customProperties = this.getCustomPropertiesList(this.knownProperties, this.data.config.properties || {});
            this.data.userOrGroupDefault = this.getUserOrGroupDefault(this.data.config || {});

            this.parentRender(() => {
                this.postRenderComponents({
                    "customProperties":this.data.customProperties,
                    "name": this.data.config.name,
                    "augmentSecurityContext": this.data.config.properties.augmentSecurityContext || {},
                    "userOrGroup": this.data.userOrGroupDefault
                });
            });
        }
    });

    return new OpenAMSessionView();
});