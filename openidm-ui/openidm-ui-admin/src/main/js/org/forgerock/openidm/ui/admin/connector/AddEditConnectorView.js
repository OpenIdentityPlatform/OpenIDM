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
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/objectTypes/ObjectTypesDialog"
], function(AdminAbstractView, eventManager, validatorsManager, constants, ConnectorDelegate, ConnectorType, ConnectorRegistry, connectorUtils, router, ConfigDelegate, objectTypesDialog) {

    var AddEditConnectorView = AdminAbstractView.extend({
        template: "templates/admin/connector/AddEditConnectorTemplate.html",
        events: {
            "change #connectorType" : "loadConnectorTemplate",
            "click #addEditConnector": "formSubmit",
            "onValidate": "onValidate",
            "click #connectorForm fieldset legend" : "sectionHideShow",
            "click .error-box .close-button" : "closeError",
            "click #addEditObjectType": "addEditObjectType",
            "click #validateConnector": "validate",
            "change input" : "disableButtons"
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
                    this.data.addEditObjectTypeTitle = $.t("templates.connector.addObjectTypeTitle");
                    this.data.addEditSubmitTitle = $.t("common.form.add");

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
                        data.connectorRef.displayName = $.t("templates.connector." +connectorUtils.cleanConnectorName(data.connectorRef.connectorName));
                        this.data.connectors = [data.connectorRef];
                        this.data.connectorName = data.name;
                        this.data.connectorType = data.connectorRef.connectorName;
                        this.data.enabled = data.enabled;
                        this.data.addEditTitle = $.t("templates.connector.editTitle");
                        this.data.addEditSubmitTitle = $.t("templates.connector.updateButtonTitle");
                        this.data.addEditObjectTypeTitle = $.t("templates.connector.editObjectTypeTitle");
                        this.data.objectType = data.objectTypes;
                        this.data.addEditSubmitTitle = $.t("common.form.update");

                        this.parentRender(function() {
                            validatorsManager.bindValidators(this.$el);

                            this.connectorTypeRef = ConnectorRegistry.getConnectorModule(data.connectorRef.connectorName);

                            this.connectorTypeRef.render({"connectorType": data.connectorRef.connectorName, "animate": true, "connectorDefaults": data}, _.bind(function(){
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

        getProvisioner: function() {
            var connectorData,
                connDetails = this.connectorTypeRef.data.connectorDefaults,
                mergedResult = {};

            connectorData = form2js('connectorForm', '.', true);

            if (connectorData.enabled === "true") {
                connectorData.enabled = true;
            } else {
                connectorData.enabled = false;
            }

            delete connectorData.connectorType;

            //Add a dummy object type here for now until we have the creator
            connectorData.configurationProperties.readSchema = false;
            connectorData.objectTypes = {};

            $.extend(true, mergedResult, connDetails, connectorData);

            mergedResult.objectTypes = this.data.userDefinedObjectType || this.data.objectType;

            return mergedResult;
        },

        formSubmit: function(event) {
            event.preventDefault();
            var mergedResult = this.getProvisioner();

            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "connectorSaved");

            if(this.data.editState) {
                ConfigDelegate.updateEntity("provisioner.openicf/" + mergedResult.name, mergedResult).then(_.bind(function () {
                    _.delay(function () {
                        eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.resourcesView});
                    }, 1500);
                }, this));
            } else {
                ConfigDelegate.createEntity("provisioner.openicf/" + mergedResult.name, mergedResult).then(_.bind(function () {
                    _.delay(function() {
                        eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.resourcesView});
                    }, 1500);
                }, this));
            }
        },


        validate: function(event) {
            event.preventDefault();

            var mergedResult = this.getProvisioner();

            ConnectorDelegate.testConnector(mergedResult).then(_.bind(function (result) {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "connectorTestPass");

                    if(!this.data.editState) {
                        this.data.objectType = result.objectTypes;
                    }

                    this.data.userDefinedObjectType = null;
                    this.$el.find("#addEditObjectType").prop('disabled', false);
                    this.$el.find("#addEditConnector").prop('disabled', false);
                }, this), _.bind(function(result) {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "connectorTestFailed");

                    if(!this.data.editState) {
                        this.data.objectType = {};
                    }

                    this.$el.find("#addEditObjectType").prop('disabled', true);
                    this.$el.find("#addEditConnector").prop('disabled', true);
                    this.showError(result);
                }, this)
            );
        },

        disableButtons: function() {
            this.$el.find("#addEditObjectType").prop('disabled', true);
            this.$el.find("#addEditConnector").prop('disabled', true);
        },

        sectionHideShow: function(event) {
            var clickedEle = event.target;

            if($(clickedEle).not("legend")){
                clickedEle = $(clickedEle).closest("legend");
            }

            $(clickedEle).find("i").toggleClass("fa-plus-square-o");
            $(clickedEle).find("i").toggleClass("fa-minus-square-o");

            $(clickedEle).parent().find(".group-body").slideToggle("slow");
        },

        showError: function(msg) {
            var error = JSON.parse(msg.responseText);

            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "connectorTestFailed");

            this.$el.find("#connectorErrorMessage .error-message").html(this.parseErrorMessage(error.message));
            this.$el.find("#connectorErrorMessage").show();
        },

        closeError : function() {
            this.$el.find("#connectorErrorMessage").hide();
        },

        parseErrorMessage: function(err) {
            var transformErrors = [
                {
                    searchString: 'UnknownHostException',
                    replaceAll: true,
                    replaceString: 'templates.connector.errorMessages.unknownHost'
                },
                {
                    searchString: 'port out of range',
                    replaceAll: true,
                    replaceString: 'templates.connector.errorMessages.portOutOfRange'
                },
                {
                    searchString: 'Connection refused',
                    replaceAll: true,
                    replaceString: 'templates.connector.errorMessages.connectionRefused'
                },
                {
                    searchString: 'Operation timed out',
                    replaceAll: true,
                    replaceString: 'templates.connector.errorMessages.operationTimedOut'
                },
                {
                    searchString: 'SSLHandshakeException',
                    replaceAll: true,
                    replaceString: 'templates.connector.errorMessages.sslHandshakeException'
                },
                {
                    searchString: 'data 52e',
                    replaceAll: true,
                    replaceString: 'templates.connector.errorMessages.invalidCredentials'
                },
                {
                    searchString: 'NamingException',
                    replaceAll: true,
                    replaceString: 'templates.connector.errorMessages.invalidCredentials'
                },
                {
                    searchString: 'Bad Base Context(s)',
                    replaceAll: false,
                    replaceString: 'templates.connector.errorMessages.badBaseContext'
                },
                {
                    searchString: 'java.lang.String to int',
                    replaceAll: true,
                    replaceString: 'templates.connector.errorMessages.portOutOfRange'
                }
            ];

            _.each(transformErrors, function(e){
                if(err.indexOf(e.searchString) > -1){
                    if(e.replaceAll) {
                        err = $.t(e.replaceString);
                    } else {
                        err = err.replace(e.searchString,$.t(e.replaceString));
                    }
                }
            });

            return err;
        },

        addEditObjectType: function() {
            objectTypesDialog.render(this.data.userDefinedObjectType ||this.data.objectType, _.bind(this.saveObjectType, this));
        },

        saveObjectType: function(newObjectType) {
            this.data.userDefinedObjectType = newObjectType;
        }
    });

    return new AddEditConnectorView();
});

