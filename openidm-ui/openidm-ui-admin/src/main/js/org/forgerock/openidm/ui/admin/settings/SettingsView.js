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
 * Portions copyright 2026 3A Systems, LLC.
 */

define([
    "underscore",
    "jquery",
    "handlebars",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/admin/settings/audit/AuditView",
    "org/forgerock/openidm/ui/admin/settings/SelfServiceView",
    "org/forgerock/openidm/ui/admin/settings/EmailConfigView",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "bootstrap-tabdrop"
], function(_,
            $,
            Handlebars,
            AdminAbstractView,
            AuditView,
            SelfServiceView,
            EmailConfigView,
            Router,
            Constants,
            EventManager) {

    var SettingsView = AdminAbstractView.extend({
        template: "templates/admin/settings/SettingsTemplate.html",
        events: {
            "click a[data-toggle=tab]": "updateRoute"
        },

        render: function(args, callback) {
            this.data.tabName = args[0] || "audit";

            this.parentRender(_.bind(function() {
                AuditView.render();
                SelfServiceView.render();
                EmailConfigView.render({}, _.noop);

                this.$el.find(".nav-tabs").tabdrop();

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
                    routeName: "settingsView",
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

    return new SettingsView();
});
