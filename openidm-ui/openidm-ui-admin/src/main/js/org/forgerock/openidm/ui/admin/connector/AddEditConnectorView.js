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
        connectorList: null,

        render: function(args, callback) {
            this.data.versionDisplay = {};
            this.data.currentMainVersion = null;

            ConnectorDelegate.availableConnectors().then(_.bind(function(connectors){
                this.data.connectors = connectors.connectorRef;

                //Clean up display names to use translated names
                _.each(this.data.connectors , function(connector){
                    connector.displayName = $.t("templates.connector." +connectorUtils.cleanConnectorName(connector.connectorName));
                }, this);

                //Build Connector type selection
                this.data.versionDisplay = _.chain(this.data.connectors)
                    .groupBy( function(connectorRef) {
                        return connectorRef.displayName;
                    })
                    .pairs()
                    .sortBy(function(connectorRef) {
                        return connectorRef[0];
                    })
                    .map(function(connectorRef){
                        connectorRef[1].displayName = connectorRef[0];

                        return {
                            "groupName" : connectorRef[0],
                            "versions" : connectorRef[1]
                        };
                    })
                    .value();

                if(args.length === 0) {
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

                    // FIXME support multiple provisioners based on systemType
                    ConfigDelegate.readEntity("provisioner.openicf/" +args[0]).then(_.bind(function(data){
                        var tempVersion;

                        data.connectorRef.displayName = $.t("templates.connector." +connectorUtils.cleanConnectorName(data.connectorRef.connectorName));
                        this.data.connectorName = data.name;
                        this.data.connectorType = data.connectorRef.connectorName;
                        this.data.systemType = data.connectorRef.systemType;
                        this.data.enabled = data.enabled;
                        this.data.addEditTitle = $.t("templates.connector.editTitle");
                        this.data.addEditSubmitTitle = $.t("templates.connector.updateButtonTitle");
                        this.data.addEditObjectTypeTitle = $.t("templates.connector.editObjectTypeTitle");
                        this.data.objectType = data.objectTypes;
                        this.data.addEditSubmitTitle = $.t("common.form.update");

                        //Filter down to the current edited connector Type
                        this.data.versionDisplay = _.filter(this.data.versionDisplay, function(connector){
                            return $.t("templates.connector." +connectorUtils.cleanConnectorName(this.data.connectorType)) === connector.groupName;
                        }, this);

                        data.connectorRef.bundleVersion = this.versionRangeCheck(data.connectorRef.bundleVersion);
                        this.data.currentMainVersion = this.findMainVersion(data.connectorRef.bundleVersion);

                        //Filter the connector types down to the current major version
                        this.data.versionDisplay[0].versions = _.filter(this.data.versionDisplay[0].versions, function(version){
                            tempVersion = this.findMainVersion(version.bundleVersion);

                            return parseFloat(this.data.currentMainVersion) === parseFloat(tempVersion);
                        }, this);

                        this.parentRender(_.bind(function() {
                            validatorsManager.bindValidators(this.$el);

                            $("#connectorType").val(this.data.connectorType +"_" +data.connectorRef.bundleVersion);

                            if(this.data.rangeFound) {
                                this.$el.find("#connectorErrorMessage .error-message").html($.t("config.messages.ConnectorMessages.connectorVersionChange", {"range" : this.data.oldVersion, "version" : data.connectorRef.bundleVersion}));
                                this.$el.find("#connectorErrorMessage").show();
                            }

                            this.connectorTypeRef = ConnectorRegistry.getConnectorModule(data.connectorRef.connectorName +"_" +this.data.currentMainVersion);

                            this.connectorTypeRef.render({"connectorType": data.connectorRef.connectorName +"_" +this.data.currentMainVersion, "animate": true, "connectorDefaults": data}, _.bind(function(){
                                validatorsManager.validateAllFields(this.$el);

                                //Set the current newest version incase there is a range
                                this.connectorTypeRef.data.connectorDefaults.connectorRef.bundleVersion = data.connectorRef.bundleVersion;
                            }, this));

                            if(callback){
                                callback();
                            }

                        }, this));

                    }, this));
                }

            }, this));
        },

        //This function is to find the newest version of a connector and select it if a user provides a range
        versionRangeCheck: function(version) {
            var cleanVersion = null,
                tempVersion,
                tempMinorVersion,
                mainVersion,
                minorVersion;

            //Checks to see if there is a range
            if(version.indexOf("(") !== -1 || version.indexOf(")") !== -1 || version.indexOf("[") !== -1 || version.indexOf("]") !== -1) {
                if(this.data.versionDisplay[0].versions.length === 1) {
                    cleanVersion = this.data.versionDisplay[0].versions[0].bundleVersion;
                } else {
                    _.each(this.data.versionDisplay[0].versions, function (versions) {
                        if (cleanVersion === null) {
                            cleanVersion = versions.bundleVersion;
                        } else {
                            tempVersion = this.findMainVersion(versions.bundleVersion);
                            tempMinorVersion = this.findMinorVersion(versions.bundleVersion);

                            mainVersion = this.findMainVersion(cleanVersion);
                            minorVersion = this.findMinorVersion(cleanVersion);

                            //Parse float is used to convert the returned string version to a number to allow basic comparison of greater / lesser value
                            if (parseFloat(mainVersion) < parseFloat(tempVersion)) {
                                cleanVersion = versions.bundleVersion;
                            } else if (parseFloat(mainVersion) === parseFloat(tempVersion)){
                                if (parseFloat(minorVersion) < parseFloat(tempMinorVersion)) {
                                    cleanVersion = versions.bundleVersion;
                                }
                            }
                        }
                    }, this);
                }

                this.data.rangeFound = true;
                this.data.oldVersion = version;
            } else {
                this.data.rangeFound = false;
                cleanVersion = version;
            }

            return cleanVersion;
        },

        //Find the major version. If a range is used it will select the newest version of a connector template available
        //A bad main version will kill the connector edit process
        findMainVersion: function(version){
            if(version.length > 0) {
                version = version.split(".");
                version = version[0] + "." + version[1];

                return version;
            } else {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "connectorBadMainVersion");
                return "0.0";
            }

        },

        //Finds the minor version.
        //A bad minor version will NOT kill the connector editing process since we primarily rely on major version for everything except for JAR selection
        findMinorVersion: function(version) {
            if(version.length > 0) {
                version = version.split(".");
                version = version[2] + "." + version[3];

                return version;
            } else {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "connectorBadMinorVersion");
                return "0.0";
            }
        },

        loadConnectorTemplate: function() {
            var connectorData,
                connectorTemplate,
                selectedValue = this.$el.find("#connectorType").val().split("_"),
                mainVersion,
                connectorRef;

            connectorData = _.findWhere(this.data.connectors, {"connectorName": selectedValue[0], "bundleVersion": selectedValue[1]});

            //If for some reason no connector data
            if(_.isUndefined(connectorData)) {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "connectorsNotAvailable");
                eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.connectorView});
            } else {
                mainVersion = this.findMainVersion(connectorData.bundleVersion);

                //Checking to ensure we don't reload the page if a minor version is changed
                if(this.data.currentMainVersion === null || (parseFloat(this.data.currentMainVersion) !== parseFloat(mainVersion)) || this.data.connectorType !==  selectedValue[0]) {
                    this.data.connectorType = selectedValue[0];
                    this.data.currentMainVersion = this.findMainVersion(connectorData.bundleVersion);

                    connectorTemplate = connectorData.connectorName +"_" +mainVersion;

                    connectorRef = {
                        connectorRef: connectorData
                    };

                    ConnectorDelegate.detailsConnector(connectorRef).then(_.bind(function(connectorDefaults){
                        this.data.systemType = connectorDefaults.connectorRef.systemType;
                        this.connectorTypeRef = ConnectorRegistry.getConnectorModule(connectorTemplate);

                        this.connectorTypeRef.render({"connectorType": connectorTemplate, "animate": true, "connectorDefaults": connectorDefaults}, _.bind(function(){
                            validatorsManager.validateAllFields(this.$el);
                        }, this));
                    }, this));
                } else {
                    //Set the bundle version on a minor version change so it saves
                    this.connectorTypeRef.data.connectorDefaults.connectorRef.bundleVersion = selectedValue[1];
                }
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
                ConfigDelegate.updateEntity("provisioner." + this.data.systemType + "/" + mergedResult.name, mergedResult).then(_.bind(function () {
                    _.delay(function () {
                        eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.resourcesView});
                    }, 1500);
                }, this));
            } else {
                ConfigDelegate.createEntity("provisioner." + this.data.systemType + "/" + mergedResult.name, mergedResult).then(_.bind(function () {
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
            objectTypesDialog.render(this.data.userDefinedObjectType ||this.data.objectType, this.getProvisioner(), _.bind(this.saveObjectType, this));
        },

        saveObjectType: function(newObjectType) {
            this.data.userDefinedObjectType = newObjectType;
        }
    });

    return new AddEditConnectorView();
});