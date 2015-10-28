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
 * Copyright 2014-2015 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/admin/connector/AddConnectorView", [
    "jquery",
    "underscore",
    "form2js",
    "org/forgerock/openidm/ui/admin/connector/AbstractConnectorView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate",
    "org/forgerock/openidm/ui/admin/connector/ConnectorTypeView",
    "org/forgerock/openidm/ui/admin/connector/ConnectorRegistry",
    "org/forgerock/openidm/ui/admin/util/ConnectorUtils",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate"

], function($, _,
            form2js,
            AbstractConnectorView,
            eventManager,
            validatorsManager,
            constants,
            ConnectorDelegate,
            ConnectorType,
            ConnectorRegistry,
            connectorUtils,
            router,
            ConfigDelegate) {

    var AddEditConnectorView = AbstractConnectorView.extend({
        template: "templates/admin/connector/AddConnectorTemplate.html",
        events: {
            "change #connectorType" : "loadConnectorTemplate",
            "onValidate": "onValidate"
        },
        data: {

        },
        connectorTypeRef: null,
        connectorList: null,
        oAuthConnector: false,

        render: function(args, callback) {
            this.data = {};
            this.data.docHelpUrl = constants.DOC_URL;
            this.data.versionDisplay = {};
            this.data.currentMainVersion = null;
            this.oAuthConnector = false;
            this.connectorTypeRef = null;
            this.connectorList = null;
            this.name = null;

            ConnectorDelegate.availableConnectors().then(_.bind(function(connectors){
                this.data.connectors = connectors.connectorRef;

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

                this.data.versionDisplay = _.filter(this.data.versionDisplay, function(version){
                    return version.versions[0].bundleName !== "org.forgerock.openicf.connectors.groovy-connector";
                }, this);

                this.data.editState = false;
                this.data.connectorName = "";

                this.parentRender(_.bind(function () {
                    validatorsManager.bindValidators(this.$el);

                    this.loadConnectorTemplate(callback);
                }, this));
            }, this));
        },

        getProvisioner: function() {
            var connectorData,
                connDetails = this.connectorTypeRef.data.connectorDefaults,
                mergedResult = {},
                tempArrayObject,
                tempKeys,
                arrayComponents = $(".connector-array-component");

            connectorData = form2js('connectorForm', '.', true);

            if(this.connectorTypeRef.getGenericState()) {
                delete connectorData.root;
                connectorData.configurationProperties = this.connectorTypeRef.getGenericConnector();
            }

            if (connectorData.enabled === "true") {
                connectorData.enabled = true;
            } else {
                connectorData.enabled = false;
            }

            delete connectorData.connectorType;

            connectorData.configurationProperties.readSchema = false;

            $.extend(true, mergedResult, connDetails, connectorData);

            //Added logic to ensure array parts correctly add and delete what is set
            _.each(arrayComponents, function(component){
                tempArrayObject = form2js($(component).prop("id"), ".", true);
                tempKeys = _.keys(tempArrayObject.configurationProperties);

                if(tempKeys.length) {
                    mergedResult.configurationProperties[tempKeys[0]] = tempArrayObject.configurationProperties[tempKeys[0]];
                }

            }, this);

            return mergedResult;
        },

        connectorFormSubmit: function(event) {
            event.preventDefault();
            var mergedResult = this.getProvisioner(),
                urlName = mergedResult.name;

            //Checks for connector specific save function to do any additional changes to data
            if(this.connectorTypeRef.connectorSaved) {
                mergedResult = this.connectorTypeRef.connectorSaved(mergedResult);
            }

            ConnectorDelegate.deleteCurrentConnectorsCache();

            ConnectorDelegate.testConnector(mergedResult).then(_.bind(function (testResult) {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "connectorSaved");

                    if(!mergedResult.objectTypes) {
                        mergedResult.objectTypes = testResult.objectTypes;
                    }

                    ConfigDelegate.createEntity(this.data.systemType + "/" + urlName, mergedResult).then(_.bind(function () {
                        eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.editConnectorView, args: [this.data.systemType +"_" +urlName,""]});
                    }, this));

                }, this),
                _.bind(function(result) {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "connectorTestFailed");

                    this.showError(result);
                }, this)
            );
        }
    });

    return new AddEditConnectorView();
});