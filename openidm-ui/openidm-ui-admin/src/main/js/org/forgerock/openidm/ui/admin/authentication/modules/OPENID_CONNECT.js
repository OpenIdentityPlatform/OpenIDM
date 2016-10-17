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
    "org/forgerock/openidm/ui/admin/authentication/AuthenticationAbstractView",
    "org/forgerock/openidm/ui/admin/delegates/ExternalAccessDelegate",
    "libs/codemirror/lib/codemirror",
    "libs/codemirror/mode/xml/xml"
], function($, _,
            Form2js,
            AuthenticationAbstractView,
            ExternalAccessDelegate,
            codemirror) {

    var OpenIDConnectView = AuthenticationAbstractView.extend({
        template: "templates/admin/authentication/modules/OPENID_CONNECT.html",
        events : _.extend({
            "change input[name='properties.resolvers[].well-known']" : "handleWellKnownChange"
        }, AuthenticationAbstractView.prototype.events),

        knownProperties: AuthenticationAbstractView.prototype.knownProperties.concat([
            "openIdConnectHeader",
            "resolvers"
        ]),

        handleWellKnownChange : function (e) {
            ExternalAccessDelegate.externalRestRequest($(e.target).val()).then((config) => {
                _.keys(config).forEach((k) => {
                    // Tests will fail with es6 template strings, so until grunt is updated to run babel prior to qunit we cannot make use of them.
                    // this.$el.find(`[name='properties.resolvers[].${k}']`).val(config[k]);
                    this.$el.find("[name='properties.resolvers[]."+k+"']").val(config[k]);

                });
            });
        },

        getConfig: function () {
            var config = AuthenticationAbstractView.prototype.getConfig.call(this);

            if (this.model.iconCode && _.has(config, "properties.resolvers[0].icon")) {
                config.properties.resolvers[0].icon  = this.model.iconCode.getValue();
            }

            if (_.has(config, "properties.resolvers[0].client_id")) {
                config.properties.resolvers[0].client_id = config.properties.resolvers[0].client_id.trim();
            }

            if (_.has(config, "properties.resolvers[0].client_secret")) {
                config.properties.resolvers[0].client_secret = config.properties.resolvers[0].client_secret.trim();
            } else if (_.has(this.data.config, "properties.resolvers[0].client_secret")) {
                // client_secret will be omitted from the config when it is left empty in the form
                // this will restore the previous value for it, if there had been one
                config.properties.resolvers[0].client_secret = this.data.config.properties.resolvers[0].client_secret;
            }


            return config;
        },

        render: function (args) {
            this.data = _.clone(args, true);
            if (!_.has(this.data, "config.properties.resolvers") || !this.data.config.properties.resolvers.length) {
                this.data.config.properties.resolvers = [{
                    scope: ["openid"]
                }];
            }
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

                this.model.iconCode = codemirror.fromTextArea(this.$el.find(".button-html")[0], {
                    lineNumbers: true,
                    viewportMargin: Infinity,
                    theme: "forgerock",
                    mode: "xml",
                    htmlMode: true,
                    lineWrapping: true
                });

            });
        }

    });

    return new OpenIDConnectView();
});
