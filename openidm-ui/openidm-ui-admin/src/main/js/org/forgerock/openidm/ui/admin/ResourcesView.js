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
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/MapResourceView"
], function(AdminAbstractView, eventManager, constants, router, ConnectorDelegate, uiUtils, connectorUtils, ConfigDelegate, MapResourceView) {
    var ResourcesView = AdminAbstractView.extend({
        template: "templates/admin/ResourcesViewTemplate.html",
        events: {
            "click .connector-delete": "deleteConnections",
            "click .managed-delete": "deleteManaged",
            "click .add-resource-button" : "addResourceMapping"
        },
        openMapping: false,
        render: function(args, callback) {
            var connectorPromise,
                managedPromise,
                repoCheckPromise;

            if(args[0] === "open") {
                this.openMapping  = true;
            } else {
                this.openMapping  = false;
            }

            connectorPromise = ConnectorDelegate.currentConnectors();
            managedPromise = ConfigDelegate.readEntity("managed");
            repoCheckPromise = ConfigDelegate.getConfigList();

            $.when(connectorPromise, managedPromise, repoCheckPromise).then(_.bind(function(connectors, managedObjects, configFiles){
                _.each(connectors[0], _.bind(function(connector){
                    if(_.isUndefined(connector.error)) {
                        connector.displayName = $.t("templates.connector." +connectorUtils.cleanConnectorName(connector.connectorRef.connectorName));
                        connector.displayObjectType = connector.objectTypes.join(",");
                        connector.cleanUrlName = connector.config.split("/")[2];
                        connector.editable = true;
                    } else {
                        //Temp code for handling a bad connector until a better method of testing valid connectors is developed
                        connector.displayName = $.t("templates.connector.connectorNameUnknown");
                        connector.cleanUrlName = connector.name;
                        connector.editable = false;
                        connector.errorMessage = $.t("templates.connector.connectorError");
                    }
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

                MapResourceView.render({
                    "removeCallback": _.bind(function(){
                        this.$el.find(".add-resource-button").prop("disabled", false);
                    }, this),
                    "addCallback" : _.bind(function(source, target){
                        if(source && target) {
                            this.$el.find(".add-resource-button").prop("disabled", true);
                        }
                    }, this)}, _.bind(function(){
                        if(this.openMapping) {
                            this.displayMapping(true, false);
                        }
                    }, this));

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
        },

        addResourceMapping: function(event) {
            this.displayMapping(true, true, _.bind(function(){
                var resourceSelected = $(event.currentTarget).parents(".resource-body"),
                    resourceType = resourceSelected.attr("data-resource-type"),
                    resourceData = null,
                    resourceLocation = null;

                if(resourceType === "connector") {
                    resourceLocation = this.$el.find("#resourceConnectorContainer .resource-body").index(resourceSelected);
                    resourceData = this.data.currentConnectors[resourceLocation];
                    resourceData.resourceType = "connector";
                } else {
                    resourceLocation = this.$el.find("#resourceManagedContainer .resource-body").index(resourceSelected);
                    resourceData = this.data.currentManagedObjects[resourceLocation];
                    resourceData.resourceType = "managed";
                }

                MapResourceView.addMapping(resourceData);
            }, this));
        },

        displayMapping: function(open, animation, callback) {
            if(open) {
                if(!this.$el.find("#resourceMappingBody").is(':visible')) {
                    if(animation) {
                        $("#resourceMappingBody").slideDown("slow", function () {
                            if (callback) {
                                callback();
                            }
                        });
                    } else {

                        $("#resourceMappingBody").show(0, function() {
                            if (callback) {
                                callback();
                            }
                        });
                    }
                } else {
                    if(callback) {
                        callback();
                    }
                }
            } else {
                if(animation) {
                    this.$el.find("#resourceMappingBody").slideUp("slow");
                } else {
                    this.$el.find("#resourceMappingBody").hide();
                }
            }
        }
    });

    return new ResourcesView();
});