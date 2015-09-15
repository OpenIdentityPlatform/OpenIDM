/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/*global define window*/

define("org/forgerock/openidm/ui/admin/dashboard/Dashboard", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/admin/dashboard/DashboardWidgetLoader",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function($, _,
            AdminAbstractView,
            DashboardWidgetLoader,
            conf,
            UIUtils) {
    var DashboardView = AdminAbstractView.extend({
        template: "templates/admin/dashboard/DashboardTemplate.html",
        events: {

        },
        model: {
            loadedWidgets: []
        },
        render: function(args, callback) {
            var counter = 0,
                holderList = null;

            this.data.dashboard = conf.globalData.adminDashboard;
            this.model.loadedWidgets = [];

            this.parentRender(_.bind(function() {
                $(window).unbind("resize.dashboardResize");

                if (!_.isUndefined(this.data.dashboard.widgets) && this.data.dashboard.widgets.length > 0) {
                    holderList = this.$el.find(".widget-holder");

                    _.each(this.data.dashboard.widgets, function (widget) {
                        this.model.loadedWidgets.push(DashboardWidgetLoader.generateWidget({
                            "element" : holderList[counter],
                            "widget" : widget
                        }));

                        counter++;
                    }, this);
                }

                //Calls widget specific resize function if needed
                $(window).bind("resize.dashboardResize", _.bind(function () {
                    if (!$.contains(document, this.$el.find("#dashboardWidgets")[0])) {
                        $(window).unbind("resize");
                    } else {
                        _.each(this.model.loadedWidgets, function(dashboardHolder){
                            if(dashboardHolder.model.widget.resize){
                                dashboardHolder.model.widget.resize();
                            }
                        }, this);
                    }
                }, this));

                if(callback){
                    callback();
                }
            }, this));
        }
    });

    return new DashboardView();
});