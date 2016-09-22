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
    "underscore",
    "bootstrap",
    "org/forgerock/openidm/ui/common/dashboard/widgets/AbstractWidget",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/admin/util/ClusterUtils"
], function($, _, bootstrap,
            AbstractWidget,
            router,
            ClusterUtils
        ) {
    var widgetInstance = {},
        Widget = AbstractWidget.extend({
            template: "templates/admin/dashboard/widgets/ClusterStatusWidgetTemplate.html",
            model : {
                "overrideTemplate" : "dashboard/widget/_clusterStatusConfig",
                "defaultRefreshRate" : 30000 //30 seconds
            },
            widgetRender: function(args, callback) {
                //widgetRender will sometimes be called with no args
                if (args && args.widget) {
                    this.data.refreshRate = args.widget.refreshRate;
                }

                //add to events and load settings partial
                this.events["click .refreshClusterData"] = "reloadWidget";
                this.events["click .clusterNode_row a"] = "openNodeDetailDialog";
                this.partials.push("partials/dashboard/widget/_clusterStatusConfig.html");

                //set the startView to be used in conditional logic to make sure we are
                //still on the dashboard page
                this.startView = this.startView || router.currentRoute.view;

                //add the refresh button to the the ellipse under the "Settings" button
                this.data.menuItems = [{
                    "icon" : "fa-refresh",
                    "menuClass" : "refreshClusterData",
                    "title" : $.t("dashboard.clusterStatusWidget.refresh")
                }];

                ClusterUtils.getClusterData().then((cluster) => {
                    this.data.cluster = _.sortBy(cluster, (o) => { return o.instanceId; });
                    this.parentRender(() => {
                        this.delayDataRefresh();
                        if(callback) {
                            callback();
                        }
                    });
                });
            },
            /**
            * This function sets a delayed refresh (checking first to make sure
            * we are still on the same page the timer originally started on)
            * to get the most recent status of the cluster.
            * The default value is set in this.model.defaultRefreshRate which is
            * used when there have been no override widget settings set
            */
            delayDataRefresh: function () {
                // have to set _this because "this" is bound to the window in delay()
                var _this = this;
                //check to make sure this function is called only when we are
                //on page where the "timer" was originally started
                if (this.startView === router.currentRoute.view) {
                    _.delay(() => {
                        _this.widgetRender();
                    }, this.data.refreshRate || this.model.defaultRefreshRate);
                }
            },
            reloadWidget: function (e) {
                if (e) {
                    e.preventDefault();
                }

                this.widgetRender();
            },
            /**
            * This function is called on node row click and opens up a BootstrapDialog which loads node details
            **/
            openNodeDetailDialog: function (e) {
                var instanceId = $(e.target).closest("a").attr("instanceId"),
                    node = _.find(this.data.cluster,{ instanceId : instanceId });

                e.preventDefault();

                ClusterUtils.openNodeDetailDialog(node);
            }
        });

    widgetInstance.generateWidget = function(loadingObject, callback) {
        var widget = {};

        $.extend(true, widget, new Widget());

        widget.render(loadingObject, callback);

        return widget;
    };

    return widgetInstance;
});
