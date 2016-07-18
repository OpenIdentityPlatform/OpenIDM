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
    "org/forgerock/openidm/ui/admin/settings/authentication/AuthenticationAbstractView",
    "org/forgerock/openidm/ui/common/delegates/SocialDelegate"

], function($, _,
            Form2js,
            AuthenticationAbstractView,
            SocialDelegate) {

    var OpenIDConnectView = AuthenticationAbstractView.extend({
        template: "templates/admin/settings/authentication/modules/OPENID_CONNECT.html",
        events: _.extend({
            "change #toggle-enabled": "enableModule"
        }, AuthenticationAbstractView.prototype.events),

        knownProperties: AuthenticationAbstractView.prototype.knownProperties.concat([
            "openIdConnectHeader"
        ]),

        partials: AuthenticationAbstractView.prototype.partials.concat([
            "partials/_alert.html"
        ]),

        enableModule: function(e) {
            function setDisableState(state) {
                this.$el.find("#noSocialProvidersAlert").toggle(state);
                $("#submitAuditEventHandlers").toggleClass("disabled", state);
            }

            if ($(e.currentTarget).is(":checked")) {
                SocialDelegate.providerList().then(_.bind(function (data) {
                    if (data.providers.length === 0) {
                        setDisableState.call(this, true);
                    } else {
                        setDisableState.call(this, false);
                    }
                }, this));

            } else {
                setDisableState.call(this, false);
            }
        },

        render: function (args) {
            this.data = _.clone(args, true);
            this.data.userOrGroupValue = "userRoles";
            this.data.userOrGroupOptions = _.clone(AuthenticationAbstractView.prototype.userOrGroupOptions, true);
            this.data.customProperties = this.getCustomPropertiesList(this.knownProperties, this.data.config.properties || {});
            this.data.userOrGroupDefault = this.getUserOrGroupDefault(this.data.config || {});


            this.parentRender(() => {
                this.postRenderComponents({
                    "customProperties": this.data.customProperties,
                    "name": this.data.config.name,
                    "augmentSecurityContext": this.data.config.properties.augmentSecurityContext || {},
                    "userOrGroup": this.data.userOrGroupDefault
                });
            });
        }

    });

    return new OpenIDConnectView();
});
