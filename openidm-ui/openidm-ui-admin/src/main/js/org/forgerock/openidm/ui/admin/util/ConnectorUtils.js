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

define("org/forgerock/openidm/ui/admin/util/ConnectorUtils", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate"
], function ($, _, ConnectorDelegate) {

    var obj = {}, iconMapping = {
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

    obj.toggleValue = function(e) {
        var element = $(e.target), oldValue = element.val() === "true";
        element.val(!oldValue);
    };

    return obj;
});
