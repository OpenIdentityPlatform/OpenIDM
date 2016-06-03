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

/*global define window*/

define("org/forgerock/openidm/ui/admin/dashboard/NewDashboard", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/delegates/SiteConfigurationDelegate"

], function($, _,
            AdminAbstractView,
            Configuration,
            Router,
            ValidatorsManager,
            ConfigDelegate,
            EventManager,
            Constants,
            SiteConfigurationDelegate) {

    var DashboardView = AdminAbstractView.extend({
        template: "templates/admin/dashboard/NewDashboardTemplate.html",
        events: {
            "submit #NewDashboardForm": "addNewDashboard",
            "onValidate": "onValidate"
        },
        model: {
            "adminDashboards": []
        },

        render: function(args, callback) {
            var adminDashboards = [];

            ConfigDelegate.readEntity("ui/dashboard").then(_.bind(function(dashboardConfig) {
                this.model.uiConf = dashboardConfig;

                if (_.has(this.model.uiConf, "adminDashboards")) {
                    this.model.adminDashboards = this.model.uiConf.adminDashboards;
                }

                this.data.existingDashboards = _.pluck(this.model.adminDashboards, "name");

                this.parentRender(_.bind(function () {
                    ValidatorsManager.bindValidators(this.$el.find("#NewDashboardForm"));
                    ValidatorsManager.validateAllFields(this.$el.find("#NewDashboardForm"));
                    if (callback) {
                        callback();
                    }
                }, this));
            }, this));
        },

        addNewDashboard: function(e) {
            e.preventDefault();

            var isDefault = this.$el.find("#DefaultDashboard").is(":checked");

            if (isDefault) {
                _.each(this.model.adminDashboards, function(dashboard) {
                    dashboard.isDefault = false;
                }, this);
            }
            this.model.adminDashboards.push({
                "name": this.$el.find("#DashboardName").val(),
                "isDefault": isDefault,
                "widgets": []
            });

            ConfigDelegate.updateEntity("ui/dashboard", this.model.uiConf).then(_.bind(function() {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "newDashboardCreated");
                EventManager.sendEvent(Constants.EVENT_UPDATE_NAVIGATION);
                EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {route: {
                    view: "org/forgerock/openidm/ui/admin/dashboard/Dashboard",
                    role: "ui-admin",
                    url: "dashboard/" + (this.model.adminDashboards.length - 1)
                }});
            }, this));
        }
    });

    return new DashboardView();
});
