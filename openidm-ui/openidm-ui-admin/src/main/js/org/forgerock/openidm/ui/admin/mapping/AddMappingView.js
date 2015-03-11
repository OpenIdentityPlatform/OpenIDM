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

define("org/forgerock/openidm/ui/admin/mapping/AddMappingView", [
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
    var MappingAddView = AdminAbstractView.extend({
        template: "templates/admin/mapping/AddMappingTemplate.html",
        events: {
            "click .add-resource-button" : "addResourceMapping"
        },
        addMappingView: false,
        render: function(args, callback) {
            var connectorPromise,
                managedPromise,
                iconPromise,
                splitConfig,
                tempIconClass;

            this.data.docHelpUrl = constants.DOC_URL;

            connectorPromise = ConnectorDelegate.currentConnectors();
            managedPromise = ConfigDelegate.readEntity("managed");
            iconPromise = connectorUtils.getIconList();

            $.when(connectorPromise, managedPromise, iconPromise).then(_.bind(function(connectors, managedObjects, iconList){

                _.each(connectors, _.bind(function(connector){
                    connector.displayName = $.t("templates.connector." +connectorUtils.cleanConnectorName(connector.connectorRef.connectorName));

                    tempIconClass = connectorUtils.getIcon(connector.connectorRef.connectorName, iconList);
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
                    tempIconClass = connectorUtils.getIcon("managedobject", iconList);

                    managedObject.iconClass = tempIconClass.iconClass;
                    managedObject.iconSrc = tempIconClass.src;
                }, this));

                this.parentRender(_.bind(function(){
                    $("#submenu").hide();

                    this.scrollHeaderPos = this.$el.find("#resourceMappingBody").offset().top;
                    this.scrollInit = false;

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
                            this.scrollCheck();
                            $(document).scroll(_.bind(this.scrollCheck, this));
                        }, this));

                    if (callback) {
                        callback();
                    }

                }, this));
            }, this));
        },

        scrollCheck: function() {
            if(this.$el.find("#resourceMappingBody").css('position') !== 'fixed') {
                var scrollPos = $(document).scrollTop();

                if(scrollPos > this.scrollHeaderPos) {
                    this.scrollInit = true;

                    this.$el.find("#resourceMappingBody").addClass('attached').css({'top' : (scrollPos - this.scrollHeaderPos)+'px'});
                } else if(this.scrollInit === true) {
                    this.$el.find("#resourceMappingBody").removeClass('attached').css({'top' : '0px'});

                    this.scrollInit = false;
                }
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
        },

        /*
         No longer in use but code could be useful if this feature is desired later in different context
         */
        mappingDetail: function(event){
            var clickedEle = $(event.target),
                index,
                details;

            if(!clickedEle.closest("button").hasClass("add-resource-button")) {
                event.preventDefault();

                if (!clickedEle.hasClass(".resource-body")) {
                    clickedEle = $(event.target).parents(".resource-body");
                }

                if (clickedEle.attr("data-resource-type") === "managed") {
                    index = $("#resourceManagedContainer .resource-body").index(clickedEle);
                    details = this.data.currentManagedObjects[index];

                    eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "editManagedView", args: [details.name]});
                } else {
                    index = $("#resourceConnectorContainer .resource-body").index(clickedEle);
                    details = this.data.currentConnectors[index];

                    eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "editConnectorView", args: [details.cleanUrlName]});
                }
            }
        }
    });

    return new MappingAddView();
});