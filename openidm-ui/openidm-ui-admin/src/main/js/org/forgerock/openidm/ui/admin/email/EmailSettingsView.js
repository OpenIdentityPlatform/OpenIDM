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
 * Copyright 2017 ForgeRock AS.
 */

define([
    "lodash",
    "jquery",
    "handlebars",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/admin/email/EmailProviderConfigView",
    "org/forgerock/openidm/ui/admin/email/EmailTemplatesListView",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "bootstrap-tabdrop"
], function(_,
            $,
            Handlebars,
            AdminAbstractView,
            EmailProviderConfigView,
            EmailTemplatesListView,
            Router,
            Constants,
            EventManager,
            AbstractDelegate) {

    var EmailSettingsView = AdminAbstractView.extend({
        template: "templates/admin/email/EmailSettingsTemplate.html",
        events: {
            "click a[data-toggle=tab]": "updateRoute"
        },

        render: function(args, callback) {
            this.data.tabName = args[0] || "provider";

            this.parentRender(_.bind(function() {
                EmailProviderConfigView.render({}, _.noop);
                EmailTemplatesListView.render({}, _.noop);
                if (callback) {
                    callback();
                }
            }, this));
        },

        updateRoute: function (e) {
            if (e) {
                e.preventDefault();
            }

            if ($(e.currentTarget).parent().hasClass("disabled")) {
                return false;

            } else {
                var route = $(e.currentTarget).attr("data-route");
                EventManager.sendEvent(Constants.ROUTE_REQUEST, {
                    routeName: "emailSettingsView",
                    args: [route],
                    trigger: true
                });
            }
        }
    });

    Handlebars.registerHelper('sw', function(val, val2, options) {
        if (val.indexOf(val2) === 0) {
            return options.fn(this);
        } else {
            return options.inverse(this);
        }
    });

    return new EmailSettingsView();
});
