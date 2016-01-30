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
    "org/forgerock/openidm/ui/admin/delegates/SiteConfigurationDelegate"

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
            SiteConfigurationDelegate) {

    var DashboardView = AdminAbstractView.extend({
        template: "templates/admin/dashboard/DashboardTemplate.html",
        events: {
            "click #RenameDashboard": "renameDashboard",
            "click #DuplicateDashboard": "duplicateDashboard",
            "click #DefaultDashboard": "defaultDashboard",
            "click #DeleteDashboard": "deleteDashboard",
            "click .add-widget": "addWidget",
            "click .open-add-widget-dialog": "openAddWidgetDialog",
            "onValidate": "onValidate",
            "customValidate": "customValidate"
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
            var counter = 0,
                holderList = null;

            this.model.uiConf = Configuration.globalData;

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

        customValidate: function() {
            this.validationResult = ValidatorsManager.formValidated(this.$el.find("form"));

            this.$el.closest(".modal-content").find("#SaveNewName").prop('disabled', !this.validationResult);
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

            ConfigDelegate.updateEntity("ui/configuration", {"configuration": this.model.uiConf}).then(_.bind(function() {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, message);
                EventManager.sendEvent(Constants.EVENT_UPDATE_NAVIGATION);

                SiteConfigurationDelegate.updateConfiguration(_.bind(function() {
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
            }, this));
        }
    });

    return new DashboardView();
});