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
 * Copyright 2011-2016 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/common/workflow/tasks/TasksDashboard",
    "org/forgerock/openidm/ui/common/dashboard/DashboardWidgetLoader",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate"
], function($, _,
            AbstractView,
            eventManager,
            constants,
            conf,
            tasksDashboard,
            DashboardWidgetLoader,
            ConfigDelegate) {
    var Dashboard = AbstractView.extend({
        template: "templates/dashboard/DashboardTemplate.html",
        model: {
            loadedWidgets: []
        },
        data: {

        },
        render: function(args, callback) {
            this.model = {
                loadedWidgets: []
            };

            this.data = {};

            if (conf.loggedUser) {
                var roles = conf.loggedUser.uiroles;

                ConfigDelegate.readEntity("ui/dashboard").then((dashboardConfig) => {
                    this.model.dashboard = dashboardConfig.dashboard;

                    this.parentRender(() => {
                        var templElement;

                        if (!_.isUndefined(this.model.dashboard.widgets) && this.model.dashboard.widgets.length > 0) {
                            _.each(this.model.dashboard.widgets, _.bind(function (widget) {

                                if (widget.type === "workflow") {
                                    this.loadWorkflow(roles, callback);
                                } else {
                                    if (widget.size === "x-small") {
                                        templElement = $('<div class="col-sm-4"></div>');
                                    } else if (widget.size === "small") {
                                        templElement = $('<div class="col-sm-6"></div>');
                                    } else if (widget.size === "medium") {
                                        templElement = $('<div class="col-sm-8"></div>');
                                    } else {
                                        templElement = $('<div class="col-sm-12"></div>');
                                    }

                                    this.$el.find("#dashboardWidgets").append(templElement);

                                    this.model.loadedWidgets = [DashboardWidgetLoader.generateWidget({
                                        "element": templElement,
                                        "widget": widget
                                    })];
                                }

                            }, this));
                        } else {
                            this.loadWorkflow(roles, callback);
                        }
                    });
                });
            }
        },

        loadWorkflow: function(roles, callback) {
            if(_.indexOf(roles, 'ui-admin') !== -1) {
                tasksDashboard.data.mode = "openidm-admin";
                tasksDashboard.render([], callback);
            } else {
                tasksDashboard.data.mode = "user";
                tasksDashboard.render([], callback);
            }
        }
    });

    return new Dashboard();
});
