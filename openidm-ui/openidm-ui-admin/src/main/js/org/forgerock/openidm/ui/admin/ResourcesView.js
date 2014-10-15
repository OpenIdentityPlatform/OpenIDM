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

/*global define, $, _, Handlebars */

define("org/forgerock/openidm/ui/admin/ResourcesView", [
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/admin/util/ConnectorUtils",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate"
], function(AdminAbstractView, eventManager, constants, router, ConnectorDelegate, uiUtils, connectorUtils, ConfigDelegate) {
    var ResourcesView = AdminAbstractView.extend({
        template: "templates/admin/ResourcesViewTemplate.html",
        events: {
            "click .connector-delete": "deleteConnections",
            "click .managed-delete": "deleteManaged"
        },
        addMappingView: false,
        render: function(args, callback) {
            var connectorPromise,
                managedPromise,
                repoCheckPromise,
                splitConfig;

            connectorPromise = ConnectorDelegate.currentConnectors();
            managedPromise = ConfigDelegate.readEntity("managed");
            repoCheckPromise = ConfigDelegate.getConfigList();

            $.when(connectorPromise, managedPromise, repoCheckPromise).then(_.bind(function(connectors, managedObjects, configFiles){
                _.each(connectors[0], _.bind(function(connector){
                    connector.displayName = $.t("templates.connector." +connectorUtils.cleanConnectorName(connector.connectorRef.connectorName));

                    splitConfig = connector.config.split("/");

                    connector.cleanUrlName = splitConfig[1] + "_" +splitConfig[2];
                    connector.cleanEditName = splitConfig[2];
                }, this));

                this.data.currentConnectors = connectors[0];
                this.data.currentManagedObjects = _.sortBy(managedObjects.objects, 'name');

                this.data.currentRepo = _.find(configFiles[0].configurations, function(file){
                    return file.pid.search("repo.") !== -1;
                }, this).pid;

                if(this.data.currentRepo === "repo.orientdb") {
                    ConfigDelegate.readEntity(this.data.currentRepo).then(_.bind(function (repo) {
                        this.data.repoObject = repo;
                        this.resourceRender(callback);
                    }, this));
                } else {
                    this.resourceRender(callback);
                }
            }, this));
        },

        resourceRender: function(callback) {
            this.parentRender(_.bind(function(){
                if(this.$el.find(".resource-unavailable").length !== 0) {
                    this.$el.find(".resource-unavailable").tooltip({
                        tooltipClass: "resource-error-tooltip"
                    });
                }

                if (callback) {
                    callback();
                }

            }, this));
        },

        deleteConnections: function(event) {
            var selectedItems = $(event.currentTarget).parents(".resource-body");

            uiUtils.jqConfirm($.t("templates.connector.connectorDelete"), function(){
                ConfigDelegate.deleteEntity("provisioner.openicf/" +selectedItems.attr("data-connector-title")).then(function(){
                        selectedItems.remove();
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteConnectorSuccess");
                    },
                    function(){
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteConnectorFail");
                    });
            }, "330px");
        },

        deleteManaged: function(event) {
            var selectedItems = $(event.currentTarget).parents(".resource-body"),
                promises = [];

            uiUtils.jqConfirm($.t("templates.managed.managedDelete"), _.bind(function(){
                _.each(this.data.currentManagedObjects, _.bind(function(managedObject, index){
                    if(managedObject.name === selectedItems.attr("data-managed-title")) {
                        this.data.currentManagedObjects.splice(index, 1);
                    }
                }, this));

                if(this.data.currentRepo === "repo.orientdb") {
                    if(this.data.repoObject.dbStructure.orientdbClass["managed_"+selectedItems.attr("data-managed-title")] !== undefined){
                        delete this.data.repoObject.dbStructure.orientdbClass["managed_"+selectedItems.attr("data-managed-title")];
                    }

                    promises.push(ConfigDelegate.updateEntity(this.data.currentRepo, this.data.repoObject));
                }

                promises.push(ConfigDelegate.updateEntity("managed", {"objects" : this.data.currentManagedObjects}));

                $.when.apply($, promises).then(function(){
                        selectedItems.remove();
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteManagedSuccess");
                    },
                    function(){
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteManagedFail");
                    });
            },this), "340px");
        }
    });

    return new ResourcesView();
});