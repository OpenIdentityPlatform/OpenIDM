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

/*global define, $, _, Handlebars, form2js */

define("org/forgerock/openidm/ui/admin/connector/AddEditConnectorView", [
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate",
    "org/forgerock/openidm/ui/admin/connector/ConnectorTypeView",
    "org/forgerock/openidm/ui/admin/connector/ConnectorRegistry",
    "org/forgerock/openidm/ui/admin/util/ConnectorUtils",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate"
], function(AdminAbstractView, eventManager, validatorsManager, constants, ConnectorDelegate, ConnectorType, ConnectorRegistry, connectorUtils, router, ConfigDelegate) {
    var AddConnectorView = AdminAbstractView.extend({
        template: "templates/admin/connector/AddEditConnectorTemplate.html",
        events: {
            "change #connectorType" : "loadConnectorTemplate",
            "click input[type=submit]": "formSubmit",
            "onValidate": "onValidate",
            "click #connectorForm fieldset legend" : "sectionHideShow"
        },
        connectorTypeRef: null,

        render: function(args, callback) {
            //Remove when commons updates
            Handlebars.registerHelper('select', function(value, options){
                var selected = $('<select />').html(options.fn(this));
                selected.find('[value=' + value + ']').attr({'selected':'selected'});

                return selected.html();
            });

            ConnectorDelegate.availableConnectors().then(_.bind(function(connectors){
                if(args.length === 0) {
                    this.data.connectors = connectors.connectorRef;
                    this.data.editState = false;
                    this.data.connectorName = "";
                    this.data.addEditTitle = $.t("templates.connector.addTitle");
                    this.data.addEditSubmitTitle = $.t("templates.connector.addButtonTitle");

                    this.parentRender(_.bind(function () {
                        validatorsManager.bindValidators(this.$el);

                        this.loadConnectorTemplate();

                        if(callback){
                            callback();
                        }
                    }, this));
                } else {
                    this.data.editState = true;

                    ConfigDelegate.readEntity("provisioner.openicf/" +args[0]).then(_.bind(function(data){
                        console.log(data);
                        data.connectorRef.displayName = $.t("templates.connector." +connectorUtils.cleanConnectorName(data.connectorRef.connectorName));
                        this.data.connectors = [data.connectorRef];
                        this.data.connectorName = data.name;
                        this.data.connectorType = data.connectorRef.connectorName;
                        this.data.enabled = data.enabled;
                        this.data.addEditTitle = $.t("templates.connector.editTitle");
                        this.data.addEditSubmitTitle = $.t("templates.connector.updateButtonTitle");

                        this.parentRender(function() {
                            validatorsManager.bindValidators(this.$el);

                            this.connectorTypeRef = ConnectorRegistry.getConnectorModule(data.connectorRef.connectorName);

                            this.connectorTypeRef.render({"connectorType": data.connectorRef.connectorName, "animate": true, "connectorDefaults": data}, _.bind(function(){
                                this.$el.find("#connectorForm").tooltip({
                                    position: { my: "left+15 center", at: "right center" },
                                    track:true
                                });

                                validatorsManager.validateAllFields(this.$el);
                            }, this));

                            if(callback){
                                callback();
                            }

                        });

                    }, this));
                }

            }, this));
        },

        loadConnectorTemplate: function() {
            var connectorData = _.findWhere(this.data.connectors, {"connectorName": this.$el.find("#connectorType").val()}),
                connectorTemplate,
                connectorRef;

            if(_.isUndefined(connectorData)) {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "connectorsNotAvailable");
                eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.connectorView});
            } else {
                connectorTemplate = connectorData.connectorName;
                connectorRef = {
                    connectorRef: connectorData
                };

                ConnectorDelegate.detailsConnector(connectorRef).then(_.bind(function(connectorDefaults){
                    this.connectorTypeRef = ConnectorRegistry.getConnectorModule(connectorTemplate);

                    this.connectorTypeRef.render({"connectorType": connectorTemplate, "animate": true, "connectorDefaults": connectorDefaults}, _.bind(function(){
                        this.$el.find("#connectorForm").tooltip({
                            position: { my: "left+15 center", at: "right center" },
                            track:true
                        });

                        validatorsManager.validateAllFields(this.$el);
                    }, this));
                }, this));

            }
        },

        formSubmit: function(event) {
            event.preventDefault();

            var connectorData,
                connDetails = this.connectorTypeRef.data.connectorDefaults,
                mergedResult = {};

            connectorData = form2js('connectorForm', '.', true);

            if(connectorData.enabled === "true") {
                connectorData.enabled = true;
            } else {
                connectorData.enabled = false;
            }

            delete connectorData.connectorType;

            //Add a dummy object type here for now until we have the creator
            connectorData.objectTypes = {};

            $.extend(true, mergedResult, connDetails, connectorData);

            if(this.data.editState) {
                ConfigDelegate.updateEntity("provisioner.openicf/" + mergedResult.name, mergedResult).then(_.bind(function () {
                    this.testConnector(mergedResult);
                }, this));
            } else {
                ConfigDelegate.createEntity("provisioner.openicf/" + mergedResult.name, mergedResult).then(_.bind(function () {
                    this.testConnector(mergedResult);
                }, this));
            }
        },

        testConnector: function(mergedResult) {
            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "connectorSaved");

            _.delay(_.bind(function () {
                ConnectorDelegate.testConnector(mergedResult.name).then(_.bind(function (result) {
                    if (result.ok === true) {
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "connectorTestPass");
                    } else {
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "connectorTestNotPass");
                    }

                    eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.connectorView});
                }, this));
            }, this), 1500);
        },

        sectionHideShow: function(event) {
            var clickedEle = event.target;

            if($(clickedEle).not("legend")){
                clickedEle = $(clickedEle).closest("legend");
            }

            $(clickedEle).find("i").toggleClass("fa-plus-square-o");
            $(clickedEle).find("i").toggleClass("fa-minus-square-o");

            $(clickedEle).parent().find(".group-body").slideToggle("slow");
        }
    });

    return new AddConnectorView();
});

