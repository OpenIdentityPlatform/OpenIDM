/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
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

/*global define */

define("config/process/AdminIDMConfig", [
    "underscore",
    "org/forgerock/openidm/ui/common/util/Constants"
], function(_, constants) {
    var obj = [
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
                startEvent: constants.EVENT_UPDATE_NAVIGATION,
                description: "Update Navigation Bar",
                dependencies: [
                    "org/forgerock/commons/ui/common/components/Navigation",
                    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate"
                ],
                processDescription: function(event, Navigation, ConfigDelegate) {
                    var completedCallback;

                    if (event) {
                        completedCallback = event.callback;
                    }

                    ConfigDelegate.readEntity("managed").then(function(managedConfig){
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
                startEvent: constants.EVENT_CHANGE_BASE_VIEW,
                description: "",
                override: true,
                dependencies: [
                    "org/forgerock/commons/ui/common/components/Navigation",
                    "org/forgerock/commons/ui/common/main/Configuration",
                    "org/forgerock/openidm/ui/common/components/Footer"
                ],
                processDescription: function(event, navigation, conf,footer) {
                    navigation.init();

                    footer.render();
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
