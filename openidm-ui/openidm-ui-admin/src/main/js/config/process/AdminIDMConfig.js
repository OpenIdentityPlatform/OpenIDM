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

/*global define */

define("config/process/AdminIDMConfig", [
    "underscore",
    "org/forgerock/openidm/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager"
], function(_, constants, EventManager) {
    var obj = [
        {
            startEvent: constants.EVENT_HANDLE_DEFAULT_ROUTE,
            description: "",
            override: true,
            dependencies: [
                "org/forgerock/commons/ui/common/main/Configuration",
                "config/routes/AdminRoutesConfig"
            ],
            processDescription: function(event, Configuration, AdminRoutesConfig) {
                var landingPage,
                    dashboardIndex;

                // If a default dashboard is configured set that to the landing page
                if (_.has(Configuration.globalData, "adminDashboards") && Configuration.globalData.adminDashboards.length > 0) {

                    dashboardIndex = _.findIndex(Configuration.globalData.adminDashboards, {"isDefault": true});

                    if (dashboardIndex === -1) {
                        dashboardIndex = 0;
                    }

                    landingPage = {
                        view: "org/forgerock/openidm/ui/admin/dashboard/Dashboard",
                        role: "ui-admin",
                        url: "dashboard/" + dashboardIndex
                    };

                    // If there are no dashboards set the landing page to the new dashboard view
                } else {
                    landingPage = AdminRoutesConfig.newDashboardView;
                }

                EventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: landingPage});

            }
        },
        {
            startEvent: constants.EVENT_QUALIFIER_CHANGED,
            description: "",
            override: true,
            dependencies: [
                "org/forgerock/commons/ui/common/main/Router",
                "org/forgerock/commons/ui/common/main/Configuration",
                "org/forgerock/openidm/ui/admin/mapping/PropertiesView"
            ],
            processDescription: function(event, router, conf, PropertiesView) {
                PropertiesView.render([event]);
            }
        },
        {
            startEvent: constants.EVENT_AUTHENTICATED,
            description: "",
            override: true,
            dependencies: [
                "org/forgerock/commons/ui/common/main/Router",
                "org/forgerock/commons/ui/common/util/Constants",
                "org/forgerock/openidm/ui/admin/delegates/MaintenanceDelegate",
                "org/forgerock/openidm/ui/admin/delegates/SchedulerDelegate",
                "org/forgerock/commons/ui/common/components/Navigation",
                "config/routes/AdminRoutesConfig",
                "org/forgerock/commons/ui/common/util/URIUtils"
            ],
            processDescription: function(event,
                                         Router,
                                         Constants,
                                         MaintenanceDelegate,
                                         SchedulerDelegate,
                                         Navigation,
                                         AdminRoutesConfig,
                                         URIUtils) {

                MaintenanceDelegate.getStatus().then(function (response) {
                    if (response.maintenanceEnabled) {

                        MaintenanceDelegate.getUpdateLogs().then(_.bind(function(updateLogData) {
                            var runningUpdate = _.findWhere(updateLogData.result, {"status": "IN_PROGRESS"});

                            //  In maintenance mode, but no install running
                            if (_.isUndefined(runningUpdate)) {

                                MaintenanceDelegate.disable().then(_.bind(function() {
                                    SchedulerDelegate.resumeJobs();

                                }, this));

                                // An install is running, redirect to settings and disable everything
                            } else {
                                if (!_.contains(URIUtils.getCurrentFragment(), "settings/update")) {
                                    Navigation.configuration = {};
                                    Router.configuration.routes["default"] = AdminRoutesConfig.settingsView;
                                }

                                if (!_.contains(URIUtils.getCurrentFragment(), "settings")) {
                                    EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {route: Router.configuration.routes.settingsView});
                                }
                            }
                        }, this));
                    }
                });
            }

        },
        {
            startEvent: constants.EVENT_UPDATE_NAVIGATION,
            description: "Update Navigation Bar",
            dependencies: [
                "jquery",
                "underscore",
                "org/forgerock/commons/ui/common/components/Navigation",
                "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
                "org/forgerock/commons/ui/common/main/Configuration"
            ],
            processDescription: function(event, $, _, Navigation, ConfigDelegate, Configuration) {
                var completedCallback,
                    name = "";

                if (event) {
                    completedCallback = event.callback;
                }

                ConfigDelegate.readEntity("managed").then(function(managedConfig) {

                    // Updates the Dashboards dropdown values
                    Navigation.configuration.links.admin.urls.dashboard.urls = [];

                    _.each(Configuration.globalData.adminDashboards, function(dashboard, index) {
                        name = dashboard.name;

                        if (index === _.findIndex(Configuration.globalData.adminDashboards, {"isDefault": true})) {
                            name = name + " (" + $.t("dashboard.new.default")+")";
                        }

                        Navigation.configuration.links.admin.urls.dashboard.urls.push({
                            "url": "#dashboard/" + index,
                            "name": name
                        });

                    }, this);

                    Navigation.configuration.links.admin.urls.dashboard.urls.push({
                        divider: true
                    });

                    Navigation.configuration.links.admin.urls.dashboard.urls.push({
                        "url" : "#newDashboard/",
                        "icon": "fa fa-plus",
                        "name": "config.AppConfiguration.Navigation.links.newDashboard"
                    });

                    // Updates the Managed dropdown values
                    Navigation.configuration.links.admin.urls.managed.urls = [];

                    Navigation.configuration.links.admin.urls.managed.urls.push({
                        "header": true,
                        "headerTitle": "data"
                    });

                    _.each(managedConfig.objects, function(managed) {
                        if(!managed.schema) {
                            managed.schema = {};
                        }

                        if(!managed.schema.icon) {
                            managed.schema.icon = "fa-cube";
                        }

                        Navigation.configuration.links.admin.urls.managed.urls.push({
                            "url" : "#resource/managed/" +managed.name +"/list/",
                            "name" : managed.name,
                            "icon" : "fa " +managed.schema.icon,
                            "cssClass" : "navigation-managed-object"
                        });
                    });

                    Navigation.configuration.links.admin.urls.managed.urls.push({
                        divider: true
                    });

                    Navigation.configuration.links.admin.urls.managed.urls.push({
                        "header": true,
                        "headerTitle": "workflow"
                    });

                    Navigation.configuration.links.admin.urls.managed.urls.push({
                        "url": "#workflow/tasks/",
                        "name": "Tasks",
                        "icon": "fa fa-check-circle-o",
                        "inactive": false
                    });

                    Navigation.configuration.links.admin.urls.managed.urls.push({
                        "url": "#workflow/processes/",
                        "name": "Processes",
                        "icon": "fa fa-random",
                        "inactive": false
                    });

                    return Navigation.reload();

                }).then(function () {
                    if (completedCallback) {
                        completedCallback();
                    }
                });
            }
        },
        {
            startEvent: constants.EVENT_SELF_SERVICE_CONTEXT,
            description: "",
            override: true,
            dependencies: [
                "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
                "org/forgerock/commons/ui/common/main/Router"
            ],
            processDescription: function(event, configDelegate, router) {
                configDelegate.readEntity("ui.context/selfservice").then(_.bind(function (data) {
                    location.href = router.getCurrentUrlBasePart() + data.urlContextRoot;
                }, this));
            }
        }
    ];

    return obj;
});
