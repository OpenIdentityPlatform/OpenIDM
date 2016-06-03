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

define("org/forgerock/openidm/ui/common/dashboard/widgets/AbstractWidget", [
    "jquery",
    "underscore",
    "handlebars",
    "form2js",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "bootstrap-dialog",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/URIUtils"
], function($, _,
            handlebars,
            form2js,
            AbstractView,
            BootstrapDialog,
            Constants,
            Configuration,
            ConfigDelegate,
            EventManager,
            URIUtils) {
    var AbstractWidget = AbstractView.extend({
        noBaseTemplate: true,
        partials : [
            "partials/dashboard/_widgetHeader.html",
            "partials/dashboard/widget/_generalConfig.html"
        ],
        data : {},
        model: {},
        events: {
            "click .widget-settings" : "widgetSettings"
        },

        render: function(args, callback) {
            this.element = args.element;
            this.data.widgetTitle = args.title;
            this.data.widgetType = args.widget.type;
            this.data.showConfigButton = args.showConfigButton;
            this.data.dashboardConfig = args.dashboardConfig;

            this.widgetRender(args, callback);
        },

        widgetSettings : function(event) {
            event.preventDefault();

            //We have to go outside the view scope to actually find the widgets proper location
            var currentConf = this.data.dashboardConfig,
                currentWidget = $(event.target).parents(".widget-holder"),
                widgetLocation = $(".widget-holder").index(currentWidget),
                self = this,
                currentTemplate = "dashboard/widget/_generalConfig",
                currentDashboard = URIUtils.getCurrentFragment().split("/")[1];


            if(this.model.overrideTemplate) {
                currentTemplate = this.model.overrideTemplate;
            }

            //Load in additional partials
            this.model.dialog = BootstrapDialog.show({
                title: "Configure Widget",
                type: BootstrapDialog.TYPE_DEFAULT,
                size: BootstrapDialog.SIZE_WIDE,
                message: $(handlebars.compile("{{>"  +currentTemplate +"}}")(currentConf.adminDashboards[currentDashboard].widgets[widgetLocation])),
                onshown: function (dialogRef) {
                    if(self.customSettingsLoad) {
                        self.customSettingsLoad(dialogRef);
                    }
                },
                buttons: [
                    {
                        label: $.t("common.form.close"),
                        action: function (dialogRef) {
                            dialogRef.close();
                        }
                    },
                    {
                        label: $.t("common.form.save"),
                        cssClass: "btn-primary",
                        id: "saveUserConfig",
                        action: function (dialogRef) {
                            if(_.isUndefined(self.customSettingsSave)) {
                                var formData = form2js("widgetConfigForm", ".", true);

                                _.extend(currentConf.adminDashboards[currentDashboard].widgets[widgetLocation], formData);

                                self.saveWidgetConfiguration(currentConf);

                                dialogRef.close();
                            } else {
                                self.customSettingsSave(dialogRef, currentConf, currentDashboard, widgetLocation);
                            }
                        }
                    }
                ]
            });
        },
        saveWidgetConfiguration: function(currentConfig) {
            var currentDashboard = URIUtils.getCurrentFragment().split("/")[1];

            ConfigDelegate.updateEntity("ui/dashboard", currentConfig).then(_.bind(function() {
                this.updateConfiguration(function() {
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "dashboardWidgetConfigurationSaved");

                    EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {route: {
                        view: "org/forgerock/openidm/ui/admin/dashboard/Dashboard",
                        role: "ui-admin",
                        url: "dashboard/" + currentDashboard,
                        forceUpdate: true
                    }});
                });
            }, this));
        },
        updateConfiguration: function(callback){
            ConfigDelegate.readEntity("ui/configuration").then(function(uiConf) {
                Configuration.globalData = uiConf.configuration;

                if (callback) {
                    callback();
                }
            });
        }
    });

    return AbstractWidget;
});