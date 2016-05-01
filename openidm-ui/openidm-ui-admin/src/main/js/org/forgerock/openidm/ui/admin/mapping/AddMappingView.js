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

define([
    "jquery",
    "underscore",
    "bootstrap",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/admin/util/ConnectorUtils",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/MapResourceView"

], function($, _, bootstrap,
            AdminAbstractView,
            eventManager,
            constants,
            router,
            ConnectorDelegate,
            uiUtils,
            connectorUtils,
            ConfigDelegate,
            MapResourceView) {

    var MappingAddView = AdminAbstractView.extend({
        template: "templates/admin/mapping/AddMappingTemplate.html",
        events: {
            "click #resourceConnectorContainer .card" : "addResourceMapping",
            "click #resourceManagedContainer .card" : "addResourceMapping"
        },
        addMappingView: false,
        render: function(args, callback) {
            var connectorPromise,
                managedPromise,
                splitConfig,
                tempIconClass;

            this.data.docHelpUrl = constants.DOC_URL;

            connectorPromise = ConnectorDelegate.currentConnectors();
            managedPromise = ConfigDelegate.readEntity("managed");

            $.when(connectorPromise, managedPromise).then(_.bind(function(connectors, managedObjects){

                _.each(connectors, _.bind(function(connector){
                    connector.displayName = $.t("templates.connector." +connectorUtils.cleanConnectorName(connector.connectorRef.connectorName));

                    tempIconClass = connectorUtils.getIcon(connector.connectorRef.connectorName);
                    connector.iconClass = tempIconClass.iconClass;
                    connector.iconSrc = tempIconClass.src;

                    if(connector.objectTypes) {
                        connector.objectTypes = _.chain(connector.objectTypes)
                            .filter(function(objectTypes){
                                return objectTypes !== "__ALL__";
                            })
                            .sortBy(function(objectType) {
                                return objectType;
                            })
                            .value();

                        if(connector.objectTypes.length > 2) {
                            connector.displayObjectType = connector.objectTypes[0] +", " +connector.objectTypes[1]+ ", (" +(connector.objectTypes.length - 2) +" " +$.t("templates.mapping.more") +")";
                        } else {
                            connector.displayObjectType = connector.objectTypes.join(", ");
                        }
                    }

                    splitConfig = connector.config.split("/");

                    connector.cleanUrlName = splitConfig[1] + "_" +splitConfig[2];
                    connector.cleanEditName = splitConfig[2];
                }, this));

                this.data.currentConnectors = _.filter(connectors, function(connector) {
                    return connector.ok === true;
                }, this);

                this.data.currentManagedObjects = _.sortBy(managedObjects.objects, 'name');

                _.each(this.data.currentManagedObjects, _.bind(function(managedObject){
                    tempIconClass = connectorUtils.getIcon("managedobject");

                    managedObject.iconClass = tempIconClass.iconClass;
                    managedObject.iconSrc = tempIconClass.src;
                }, this));

                this.parentRender(_.bind(function(){
                    this.$el.find('#resourceMappingBody').affix({
                        offset: {
                            top: 240
                        }
                    });

                    MapResourceView.render({
                        "removeCallback": _.bind(function(){
                            this.$el.find(".add-resource-button").prop("disabled", false);
                        }, this),
                        "addCallback" : _.bind(function(source, target){
                            if(source && target) {
                                this.$el.find(".add-resource-button").prop("disabled", true);
                            }
                        }, this)},
                        _.bind(function(){
                            if(args.length > 0) {
                                this.preselectMappingCard(args);
                            }

                            if (callback) {
                                callback();
                            }
                        }, this));

                }, this));
            }, this));
        },

        preselectMappingCard: function(selected) {
            var resourceData = null;

            if(selected[0] === "connector") {
                resourceData =  _.find(this.data.currentConnectors, function(connector) {
                    return selected[1] === connector.name;
                });

                if(resourceData !== null) {
                    resourceData.resourceType = "connector";
                }
            } else {
                resourceData =  _.find(this.data.currentManagedObjects, function(managed) {
                    return selected[1] === managed.name;
                });

                if(resourceData !== null) {
                    resourceData.resourceType = "managed";
                }
            }

            if(resourceData !== null) {
                MapResourceView.addMapping(resourceData);
            }
        },

        addResourceMapping: function(event) {
            var resourceSelected = $(event.currentTarget).closest(".card"),
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
        }
    });

    return new MappingAddView();
});
