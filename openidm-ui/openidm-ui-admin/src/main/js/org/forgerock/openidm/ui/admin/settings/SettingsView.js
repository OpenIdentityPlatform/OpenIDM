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
 * Copyright 2015 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/admin/settings/SettingsView", [
    "underscore",
    "jquery",
    "handlebars",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/admin/settings/authentication/AuthenticationView",
    "org/forgerock/openidm/ui/admin/settings/audit/AuditView",
    "org/forgerock/openidm/ui/admin/settings/SelfServiceView",
    "org/forgerock/openidm/ui/admin/settings/EmailConfigView",
    "org/forgerock/openidm/ui/admin/settings/UpdateView",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "bootstrap-tabdrop"
], function(_,
            $,
            Handlebars,
            AdminAbstractView,
            AuthenticationView,
            AuditView,
            SelfServiceView,
            EmailConfigView,
            UpdateView,
            Router,
            Constants,
            EventManager,
            AbstractDelegate) {

    var SettingsView = AdminAbstractView.extend({
        template: "templates/admin/settings/SettingsTemplate.html",
        events: {
            "click a[data-toggle=tab]": "updateRoute"
        },

        render: function(args, callback) {
            this.data.tabName = args[0] || "authentication";

            this.data.maintenanceModeDelegate = new AbstractDelegate(Constants.host + "/openidm/maintenance");

            this.data.maintenanceModeDelegate.serviceCall({
                url: "?_action=status",
                type: "POST"

            }).then(_.bind(function(data) {
                this.data.maintenanceMode = data.maintenanceEnabled;

                if (data.maintenanceEnabled) {
                    this.data.tabName = "update";
                    EventManager.sendEvent(Constants.ROUTE_REQUEST, {
                        routeName: "settingsView",
                        args: [this.data.tabName],
                        trigger: false
                    });

                }
            }, this));

            this.parentRender(_.bind(function() {
                if (!this.data.maintenanceMode) {
                    AuthenticationView.render();
                    AuditView.render();
                    SelfServiceView.render();
                    EmailConfigView.render({}, _.noop);
                }

                UpdateView.render({step: "version"}, _.bind(function() {
                    this.$el.find(".nav-tabs").tabdrop();
                }, this));

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
                    trigger: false
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
