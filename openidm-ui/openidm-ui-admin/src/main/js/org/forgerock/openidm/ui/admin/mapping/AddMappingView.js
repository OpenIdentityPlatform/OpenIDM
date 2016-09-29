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
            "click #resourceConnectorContainer .card" : "addResourceEvent",
            "click #resourceManagedContainer .card" : "addResourceEvent"
        },
        addMappingView: false,
        data : {

        },
        model : {

        },
        render: function(args, callback) {
            var connectorPromise,
                managedPromise,
                tempIconClass;

            this.data.docHelpUrl = constants.DOC_URL;

            connectorPromise = ConnectorDelegate.currentConnectors();
            managedPromise = ConfigDelegate.readEntity("managed");

            $.when(connectorPromise, managedPromise).then(_.bind(function(connectors, managedObjects){
                _.each(connectors, _.bind(function(connector, index){
                    connectors[index] = this.setupDisplayConnector(connector);

                    connectors[index].displayName = $.t("templates.connector." +connectorUtils.cleanConnectorName(connectors[index].connectorRef.connectorName));

                    tempIconClass = connectorUtils.getIcon(connectors[index].connectorRef.connectorName);
                    connectors[index].iconClass = tempIconClass.iconClass;
                    connectors[index].iconSrc = tempIconClass.src;
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
                    let preselectResult;

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
                                preselectResult = this.preselectMappingCard(args, this.data.currentConnectors, this.data.currentManagedObjects);


                                if(preselectResult !== null) {
                                    MapResourceView.addMapping(preselectResult);
                                }
                            }

                            if (callback) {
                                callback();
                            }
                        }, this));

                }, this));
            }, this));
        },

        /**
         * @param displayConnector - Standard connector object returned from IDM (config and objecttypes)
         * @returns {*} - Returns a modified connector object containing display elements for listing objectType, icon, and sorting
         */
        setupDisplayConnector : function(displayConnector) {
            var connector = _.clone(displayConnector),
                splitConfig;

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

            return connector;
        },

        /**
         * @param selected - Array of URL arguments with type and name ["connector", "ldap"]
         * @param connectors - Array of connector object details
         * @param managedObjects - Array of managed object details
         * @returns {*} Returns the correct connector or managed object details based on the URL arguments to allow for on load selection of a resource
         */
        preselectMappingCard: function(selected, connectors, managedObjects) {
            var resourceData = null;

            if(selected[0] === "connector") {
                resourceData =  _.find(connectors, function(connector) {
                    return selected[1] === connector.name;
                });

                if(resourceData !== null) {
                    resourceData.resourceType = selected[0];
                }
            } else {
                resourceData =  _.find(managedObjects, function(managed) {
                    return selected[1] === managed.name;
                });

                if(resourceData !== null) {
                    resourceData.resourceType = selected[0];
                }
            }

            return resourceData;
        },

        /**
         * @param event - Click event on a card selected in the add mapping view
         *
         * This event finds the dom location of the card and type and passes the details onto addResourceMapping to find the appropriate resource details
         */
        addResourceEvent: function(event) {
            var resourceSelected = $(event.currentTarget).closest(".card"),
                resourceType = resourceSelected.attr("data-resource-type"),
                resourceLocation = null;

            if(resourceType === "connector") {
                resourceLocation = this.$el.find("#resourceConnectorContainer .resource-body").index(resourceSelected);
            } else {
                resourceLocation = this.$el.find("#resourceManagedContainer .resource-body").index(resourceSelected);
            }

            MapResourceView.addMapping(this.addResourceMapping(resourceType,  resourceLocation, this.data.currentConnectors, this.data.currentManagedObjects));
        },

        /**
         *
         * @param resourceType - Current resource type connector or managed
         * @param resourceLocation - The numerical location of the resource based on the DOM location
         * @param connectors - Array of connector object details
         * @param managedObjects - Array of managed object details
         * @returns {*} Returns the correct connector or managed object details based on the URL arguments to allow for on load selection of a resource
         */
        addResourceMapping: function(resourceType, resourceLocation, connectors, managedObjects) {
            var resourceData = null;

            if(resourceType === "connector") {
                resourceData = connectors[resourceLocation];
                resourceData.resourceType = resourceType;
            } else {
                resourceData = managedObjects[resourceLocation];
                resourceData.resourceType = resourceType;
            }

            return resourceData;
        }

    });

    return new MappingAddView();
});
