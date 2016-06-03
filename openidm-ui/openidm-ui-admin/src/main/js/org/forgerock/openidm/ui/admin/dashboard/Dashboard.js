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
 * Copyright 2014-2016 ForgeRock AS.
 */

/*global define window*/

define("org/forgerock/openidm/ui/admin/dashboard/Dashboard", [
    "jquery",
    "underscore",
    "bootstrap-dialog",
    "handlebars",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/admin/dashboard/DashboardWidgetLoader",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/openidm/ui/admin/delegates/SiteConfigurationDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/util/AutoScroll",
    "dragula"

], function($, _,
            BootstrapDialog,
            Handlebars,
            AdminAbstractView,
            DashboardWidgetLoader,
            Configuration,
            Router,
            ConfigDelegate,
            EventManager,
            Constants,
            ValidatorsManager,
            SiteConfigurationDelegate,
            UIUtils,
            AutoScroll,
            dragula) {

    var DashboardView = AdminAbstractView.extend({
        template: "templates/admin/dashboard/DashboardTemplate.html",
        events: {
            "click #RenameDashboard": "renameDashboard",
            "click #DuplicateDashboard": "duplicateDashboard",
            "click #DefaultDashboard": "defaultDashboard",
            "click #DeleteDashboard": "deleteDashboard",
            "click .add-widget": "addWidget",
            "click .open-add-widget-dialog": "openAddWidgetDialog",
            "click .widget-delete" : "deleteWidget" //This event relies on child views creating the correct HTML menu item
        },
        partials : [
            "partials/dashboard/_DuplicateDashboard.html",
            "partials/dashboard/_RenameDashboard.html",
            "partials/dashboard/_AddWidget.html"
        ],
        model: {
            loadedWidgets: [],
            allDashboards: [],
            dashboardIndex: 0
        },
        render: function(args, callback) {
            var holderList = null;

            ConfigDelegate.readEntity("ui/dashboard").then(_.bind(function(dashboardConfig){
                this.data.dashboard = dashboardConfig.adminDashboard;
                this.model.uiConf = dashboardConfig;
                this.model.dashboardRef = this;

                if (_.has(this.model.uiConf, "adminDashboards")) {
                    this.model.allDashboards = this.model.uiConf.adminDashboards;
                }

                this.model.dashboardIndex = parseInt(Router.getCurrentHash().split("/")[1], 10) || 0;

                this.model.loadedWidgets = [];
                this.data.dashboard = this.model.allDashboards[this.model.dashboardIndex];

                if (!this.data.dashboard) {
                    EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {route: Router.configuration.routes.newDashboardView});
                    return;
                }

                this.data.isDefault = this.data.dashboard.isDefault;

                this.parentRender(_.bind(function() {
                    $(window).unbind("resize.dashboardResize");
                    var renderedWidgetCount = 0;

                    if (!_.isUndefined(this.data.dashboard.widgets) && this.data.dashboard.widgets.length > 0) {
                        holderList = this.$el.find(".widget-holder");

                        _.each(this.data.dashboard.widgets, function (widget, index) {
                            this.model.loadedWidgets.push(DashboardWidgetLoader.generateWidget(
                                    {
                                        "element" : holderList[index],
                                        "widget" : widget,
                                        "dashboardConfig" : dashboardConfig
                                    },
                                    // This callback is used to make sure every widget has been rendered before initiating the
                                    // drag and drop utility.
                                    _.bind(function() {
                                        renderedWidgetCount ++;
                                        if (renderedWidgetCount === this.data.dashboard.widgets.length) {
                                            this.initDragDrop();
                                        }
                                    }, this))
                            );

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
                        this.initDragDrop();

                    }, this));

                    if (callback) {
                        callback();
                    }
                }, this));
            }, this));
        },

        initDragDrop: function() {
            var start,
                dragDropInstance = dragula([this.$el.find("#dashboardWidgets")[0]], {
                    moves: function (el, container, handle) {
                        return handle.className.indexOf("fa-arrows") > -1 || handle.className.indexOf("btn-move") > -1;
                    }
                });

            dragDropInstance.on("drag", _.bind(function(el, container) {
                start = _.indexOf($(container).find(".widget-holder"), el);
                AutoScroll.startDrag();
            }, this));

            dragDropInstance.on("dragend", _.bind(function(el, container) {
                var widgetCopy = this.data.dashboard.widgets[start],
                    stop = _.indexOf(this.$el.find("#dashboardWidgets .widget-holder"), el);

                AutoScroll.endDrag();

                this.data.dashboard.widgets.splice(start, 1);
                this.data.dashboard.widgets.splice(stop, 0, widgetCopy);

                this.saveChanges("dashboardWidgetsRearranged", false);
            }, this));
        },

        addWidget: function (e) {
            if (e) {
                e.preventDefault();
            }
            this.setElement("#content");

            this.data.dashboard.widgets.push({
                "type" : $(e.currentTarget).attr("data-widget-id"),
                "size" : DashboardWidgetLoader.getWidgetList()[$(e.currentTarget).attr("data-widget-id")].defaultSize
            });

            this.saveChanges("dashboardWidgetAdded", false, _.bind(function() {
                this.render();
                this.dialog.close();
                this.openAddWidgetDialog();
            }, this));
        },

        openAddWidgetDialog: function (e) {
            if (e) {
                e.preventDefault();
            }

            this.dialog = BootstrapDialog.show({
                title: "Add Widgets",
                type: BootstrapDialog.TYPE_DEFAULT,
                size: BootstrapDialog.SIZE_NORMAL,
                message: $(Handlebars.compile("{{> dashboard/_AddWidget}}")({"widgets": DashboardWidgetLoader.getWidgetList()})),
                onshown: _.bind(function () {
                    this.setElement("#AddWidgetDialog");
                }, this),
                onhidden: _.bind(function() {
                    this.setElement("#content");
                }, this),
                buttons: [
                    {
                        label: $.t("common.form.close"),
                        action: function (dialogRef) {
                            dialogRef.close();
                        }
                    }
                ]
            });
        },

        renameDashboard: function(e) {
            e.preventDefault();

            this.dialog = BootstrapDialog.show({
                title: $.t("dashboard.renameTitle"),
                type: BootstrapDialog.TYPE_DEFAULT,
                size: BootstrapDialog.SIZE_NORMAL,
                message: $(Handlebars.compile("{{> dashboard/_RenameDashboard}}")({"existingDashboards": _.pluck(this.model.allDashboards, "name")})),
                onshown: _.bind(function (dialogRef) {
                    this.setElement("#RenameDashboardDialog");
                    ValidatorsManager.bindValidators(dialogRef.$modal.find("#RenameDashboardDialog form"));
                    ValidatorsManager.validateAllFields(dialogRef.$modal.find("#RenameDashboardDialog form"));
                }, this),
                onhidden: _.bind(function() {
                    this.setElement("#content");
                }, this),
                buttons: [
                    {
                        label: $.t("common.form.close"),
                        action: function (dialogRef) {
                            dialogRef.close();
                        }
                    }, {
                        label: $.t("common.form.save"),
                        cssClass: "btn-primary",
                        id: "SaveNewName",
                        action: _.bind(function (dialogRef) {
                            var newName = dialogRef.$modal.find("#DashboardName").val();

                            dialogRef.close();

                            this.data.dashboard.name = newName;
                            this.saveChanges("dashboardRenamed", this.model.dashboardIndex);
                        }, this)
                    }
                ]
            });
        },

        /**
         Override validation to allow dialog to enable/disable Save correctly
         */
        validationSuccessful: function (event) {
            AdminAbstractView.prototype.validationSuccessful(event);

            this.model.dashboardRef.$el.closest(".modal-content").find("#SaveNewName").prop('disabled', false);
        },

        validationFailed: function (event, details) {
            AdminAbstractView.prototype.validationFailed(event, details);

            this.model.dashboardRef.$el.closest(".modal-content").find("#SaveNewName").prop('disabled', true);
        },

        duplicateDashboard: function(e) {
            e.preventDefault();

            this.dialog = BootstrapDialog.show({
                title: $.t("dashboard.duplicateTitle"),
                type: BootstrapDialog.TYPE_DEFAULT,
                size: BootstrapDialog.SIZE_NORMAL,
                message: $(Handlebars.compile("{{> dashboard/_DuplicateDashboard}}")({
                    "defaultName": $.t("dashboard.duplicateOf") + this.data.dashboard.name,
                    "existingDashboards": _.pluck(this.model.allDashboards, "name")
                })),
                onshown: _.bind(function (dialogRef) {
                    this.setElement("#DuplicateDashboardDialog");
                    ValidatorsManager.bindValidators(dialogRef.$modal.find("#DuplicateDashboardDialog form"));
                    ValidatorsManager.validateAllFields(dialogRef.$modal.find("#DuplicateDashboardDialog form"));
                }, this),
                onhidden: _.bind(function() {
                    this.setElement("#content");
                }, this),
                buttons: [
                    {
                        label: $.t("common.form.close"),
                        action: function (dialogRef) {
                            dialogRef.close();
                        }
                    }, {
                        label: $.t("common.form.save"),
                        cssClass: "btn-primary",
                        id: "SaveNewName",
                        action: _.bind(function (dialogRef) {
                            var newDashboard = _.clone(this.data.dashboard, true);

                            newDashboard.name = dialogRef.$modal.find("#DashboardName").val();
                            newDashboard.isDefault = false;
                            this.model.allDashboards.push(newDashboard);

                            this.saveChanges("dashboardDuplicated", this.model.allDashboards.length-1);

                            dialogRef.close();
                        }, this)
                    }
                ]
            });
        },

        defaultDashboard: function(e) {
            e.preventDefault();

            _.each(this.model.allDashboards, function(dashboard) {
                dashboard.isDefault = false;
            }, this);

            this.data.dashboard.isDefault = true;

            this.saveChanges("dashboardDefaulted", this.model.dashboardIndex);
        },

        deleteDashboard: function(e) {
            e.preventDefault();

            this.model.allDashboards.splice(this.model.dashboardIndex, 1);

            var landingIndex = _.findIndex(this.model.allDashboards, {"isDefault": true});

            if (landingIndex === -1 && this.model.allDashboards.length > 0) {
                landingIndex = 0;
            }

            this.saveChanges("dashboardDeleted", landingIndex);
        },

        saveChanges: function(message, landingIndex, callback) {
            this.model.uiConf.adminDashboards = this.model.allDashboards;

            ConfigDelegate.updateEntity("ui/dashboard", this.model.uiConf).then(_.bind(function() {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, message);
                EventManager.sendEvent(Constants.EVENT_UPDATE_NAVIGATION);

                if (this.model.dashboardIndex === landingIndex) {
                    this.render();

                } else if (_.isNumber(landingIndex) && landingIndex > -1) {
                    EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                        route: {
                            view: "org/forgerock/openidm/ui/admin/dashboard/Dashboard",
                            role: "ui-admin",
                            url: "dashboard/" + landingIndex
                        }
                    });
                } else if (_.isNumber(landingIndex)) {
                    EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {route: Router.configuration.routes.newDashboardView});
                }

                if (callback) {
                    callback();
                }
            }, this));
        },

        deleteWidget: function(event) {
            event.preventDefault();

            var currentConf = this.model.uiConf,
                currentWidget = $(event.target).parents(".widget-holder"),
                widgetLocation = this.$el.find(".widget-holder").index(currentWidget);

            currentConf.adminDashboards[this.model.dashboardIndex].widgets.splice(widgetLocation, 1);

            UIUtils.confirmDialog($.t("dashboard.widgetDelete"), "danger", _.bind(function(){
                 ConfigDelegate.updateEntity("ui/dashboard", currentConf).then(_.bind(function() {
                     this.render();
                 }, this));
            }, this));
        }
    });

    return new DashboardView();
});