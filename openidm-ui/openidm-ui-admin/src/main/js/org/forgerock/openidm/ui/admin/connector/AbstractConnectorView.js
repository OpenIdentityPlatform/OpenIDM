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

define("org/forgerock/openidm/ui/admin/connector/AbstractConnectorView", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate",
    "org/forgerock/openidm/ui/admin/connector/ConnectorTypeView",
    "org/forgerock/openidm/ui/admin/connector/ConnectorRegistry",
    "org/forgerock/openidm/ui/admin/util/ConnectorUtils",
    "org/forgerock/commons/ui/common/main/Router"

], function($, _,
            AdminAbstractView,
            eventManager,
            validatorsManager,
            constants,
            ConnectorDelegate,
            ConnectorType,
            ConnectorRegistry,
            connectorUtils,
            router) {

    var AddEditConnectorView = AdminAbstractView.extend({
        data: {

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

        loadConnectorTemplate: function(callback) {
            var connectorData,
                connectorTemplate,
                selectedValue = this.$el.find("#connectorType option:selected"),
                mainVersion,
                connectorRef;

            connectorData = _.findWhere(this.data.connectors, {"connectorName": selectedValue.attr('connectorTypeName'), "bundleVersion": selectedValue.attr('bundleVersion')});


            // For each schedule on the page
            _.each(this.addedLiveSyncSchedules, function (source) {
                this.$el.find("#" + source.split("/").join("")).find(".deleteSchedule").click();
                this.addedLiveSyncSchedules.splice(_.indexOf(this.addLiveSyncScheduler, source), 1);
            }, this);
            this.objectTypes = [];


            //If for some reason no connector data
            if(_.isUndefined(connectorData)) {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "connectorsNotAvailable");
                eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.connectorView});
            } else {
                mainVersion = this.findMainVersion(connectorData.bundleVersion);

                //Checking to ensure we don't reload the page if a minor version is changed
                if(this.data.currentMainVersion === null || (parseFloat(this.data.currentMainVersion) !== parseFloat(mainVersion)) || this.data.connectorTypeName !== selectedValue.attr('connectorTypeName')) {
                    this.data.connectorTypeName = selectedValue.attr('connectorTypeName');
                    this.data.systemType = selectedValue.attr('systemType');
                    this.data.currentMainVersion = this.findMainVersion(connectorData.bundleVersion);

                    connectorTemplate = connectorData.connectorName +"_" +mainVersion;

                    connectorRef = {
                        connectorRef: connectorData
                    };

                    $.when(
                        ConnectorRegistry.getConnectorModule(connectorTemplate),
                        ConnectorDelegate.detailsConnector(connectorRef)
                    ).then(_.bind(function(connectorTypeRef, connectorDefaults){
                        this.connectorTypeRef = connectorTypeRef;

                        if(this.connectorTypeRef.oAuthConnector) {
                            this.oAuthConnector = true;
                        } else {
                            this.oAuthConnector = false;
                        }

                        this.connectorTypeRef.render({"connectorType": connectorTemplate,
                                "animate": true,
                                "connectorDefaults": connectorDefaults[0],
                                "editState" : this.data.editState,
                                "systemType" : this.data.systemType },
                            _.bind(function(){
                                this.setSubmitFlow();

                                validatorsManager.validateAllFields(this.$el);

                                if(_.isFunction(callback)){
                                    callback();
                                }

                            }, this));
                    }, this));
                } else {
                    //Set the bundle version on a minor version change so it saves
                    this.connectorTypeRef.data.connectorDefaults.connectorRef.bundleVersion = selectedValue.attr('bundleVersion');
                }
            }
        },

        showError: function(msg) {
            var error = JSON.parse(msg.responseText);

            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "connectorTestFailed");

            this.$el.find("#connectorErrorMessage .message").html(this.parseErrorMessage(error.message));
            this.$el.find("#connectorErrorMessage").show();
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

        setSubmitFlow: function() {
            var connectorSpecificCheck = false;

            if(this.connectorTypeRef.connectorSpecificValidation) {
                connectorSpecificCheck = this.connectorTypeRef.connectorSpecificValidation();
            }

            this.$el.find("#submitConnector").unbind("click");

            if(this.oAuthConnector) {
                if(this.connectorTypeRef.data.connectorDefaults.configurationProperties.clientId !== this.$el.find("#clientId").val() ||
                    this.$el.find("#clientSecret").val().length > 0 ||
                    this.connectorTypeRef.data.connectorDefaults.configurationProperties.refreshToken === null || connectorSpecificCheck) {

                    this.$el.find("#submitConnector").bind("click", _.bind(this.oAuthFormSubmit, this));
                } else {
                    this.$el.find("#submitConnector").bind("click", _.bind(this.connectorFormSubmit, this));
                }
            } else {
                this.$el.find("#submitConnector").bind("click", _.bind(this.connectorFormSubmit, this));
            }
        },

        oAuthFormSubmit: function(event) {
            event.preventDefault();
            var mergedResult = this.getProvisioner();

            this.connectorTypeRef.submitOAuth(mergedResult, this.data.editState);
        }
    });

    return AddEditConnectorView;
});
