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
    "handlebars",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/admin/mapping/util/MappingUtils",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/util/AdminUtils"
], function ($, _, Handlebars, ConnectorDelegate, ConfigDelegate, UIUtils, MappingUtils, eventManager, ResourceDelegate, constants, AdminUtils) {

    var obj = {},
        iconMapping = {
            "org.identityconnectors.ldap.LdapConnector" : "fa fa-book",
            "org.forgerock.openicf.connectors.xml.XMLConnector" : "fa fa-code",
            "org.forgerock.openidm.salesforce": "fa fa-cloud",
            "org.identityconnectors.databasetable.DatabaseTableConnector" : "fa fa-table",
            "org.forgerock.openicf.csvfile.CSVFileConnector" : "fa fa-quote-right",
            "org.forgerock.openicf.connectors.googleapps.GoogleAppsConnector" : "fa fa-google",
            "org.forgerock.openidm.salesforce.Salesforce": "fa fa-cloud",
            "org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConnector": "fa fa-file-code-o",
            "managedobject": "fa fa-database",
            "missing" : "fa fa-question",
            "default" : "fa fa-cubes"
        };

    obj.cleanConnectorName = function(name) {
        var clearName = name.split(".");
        clearName = clearName[clearName.length - 2] + "_" + clearName[clearName.length - 1];
        return clearName;
    };

    obj.getMappingDetails = function(sourceName, targetName) {
        return ConnectorDelegate.currentConnectors().then(function(connectors){
            var details = {};

            details.targetConnector = _.find(connectors, function (connector) {
                return connector.name === targetName;
            }, this);
            details.sourceConnector = _.find(connectors, function (connector) {
                return connector.name === sourceName;
            }, this);

            if(targetName === "managed") {
                details.targetIcon = obj.getIcon("managedobject");
            } else {
                if (details.targetConnector) {
                    details.targetIcon = obj.getIcon(details.targetConnector.connectorRef.connectorName);
                } else {
                    details.targetIcon = obj.getIcon("missing");
                }
            }
            if(sourceName === "managed") {
                details.sourceIcon = obj.getIcon("managedobject");
            } else {
                if (details.sourceConnector) {
                    details.sourceIcon = obj.getIcon(details.sourceConnector.connectorRef.connectorName);
                } else {
                    details.sourceIcon = obj.getIcon("missing");
                }
            }

            details.sourceName = sourceName;
            details.targetName = targetName;

            return details;
        });
    };

    obj.getIcon = function (type) {
        var iconClass = iconMapping[type];
        if(!iconClass) {
            iconClass = iconMapping["default"];
        }
        return {
            "iconClass": iconClass
        };
    };

    obj.deleteConnector = function(connectorName, connectorPath, successCallback) {
        var partialPromise = UIUtils.preloadPartial("partials/connector/_connectorMappings.html"),
            connectorMappingsPromise = obj.getConnectorMappings(connectorName);


        $.when(connectorMappingsPromise, partialPromise).then((connectorMappings) => {
            var connectorMappingsDisplay = Handlebars.compile("{{> connector/_connectorMappings}}")({ connectorMappings: connectorMappings }),
                dialogText = $("<div>").append($.t("templates.connector.connectorDelete", {"connectorName": connectorName, "connectorChildren": connectorMappingsDisplay}));

            AdminUtils.confirmDeleteDialog(dialogText, () => {
                var promise;

                if (connectorMappings.length) {
                    //delete all the connector's mappings
                    _.each(connectorMappings, (mapping) => {
                        if (!promise) {
                            //no promise exists so create it
                            promise = obj.deleteConnectorMapping(mapping);
                        } else {
                            //promise exists now "concat" a new "then" onto the original promise
                            promise = promise.then(() => {
                                return obj.deleteConnectorMapping(mapping);
                            });
                        }
                    });
                } else {
                    promise = $.Deferred().resolve();
                }

                promise.then(function () {
                    var url = connectorPath.split("/");

                    ConfigDelegate.deleteEntity(url[1] +"/" +url[2]).then(function(){
                        ConnectorDelegate.deleteCurrentConnectorsCache();

                        if (successCallback) {
                            successCallback();
                        }

                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteConnectorSuccess");
                    },
                    function(){
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteConnectorFail");
                    });
                });
            });
        });
    };

    obj.deleteConnectorMapping = function(mapping) {
        return ConfigDelegate.readEntity("sync").then(function (syncConfig) {
            return MappingUtils.deleteMapping(mapping.name, mapping.children, syncConfig.mappings);
        });
    };

    obj.getConnectorMappings = function (connectorName) {
        return ConfigDelegate.readEntity("sync").then(function (syncConfig) {
            var promArray = [],
                connectorMappings = _.filter(syncConfig.mappings, function (mapping) {
                    var target = mapping.target.split("/"),
                        source = mapping.source.split("/"),
                        sourceMatch = source[0] === "system" && source[1] === connectorName,
                        targetMatch = target[0] === "system" && target[1] === connectorName;

                    return targetMatch || sourceMatch;
                });

            _.each(connectorMappings, function (mapping) {
                promArray.push(
                    MappingUtils.getMappingChildren(mapping.name).then( function (mappingChildren) {
                        return {
                            name: mapping.name,
                            children: mappingChildren
                        };
                    })
                );
            });

            return $.when.apply($, promArray).then(function () {
                return _.toArray(arguments);
            });
        });
    };

    return obj;
});
