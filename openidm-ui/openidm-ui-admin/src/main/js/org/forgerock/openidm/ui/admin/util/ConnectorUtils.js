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

define("org/forgerock/openidm/ui/admin/util/ConnectorUtils", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate"
], function ($, _, ConnectorDelegate, ConfigDelegate) {

    var obj = {};

    obj.iconListDefaults = {
        "icons" : [
            {
                "type" : "org.identityconnectors.ldap.LdapConnector",
                "iconClass" : "icon-ldap",
                "src" : "img/icon-ldap.png"
            },
            {
                "type" : "org.forgerock.openicf.connectors.xml.XMLConnector",
                "iconClass" : "icon-xml",
                "src": "img/icon-xml.png"
            },
            {
                "type" : "org.forgerock.openidm.salesforce",
                "iconClass" : "icon-cloud",
                "src": "img/icon-cloud.png"
            },
            {
                "type" : "org.identityconnectors.databasetable.DatabaseTableConnector",
                "iconClass" : "icon-database",
                "src": "img/icon-managedobject.png"
            },
            {
                "type" : "org.forgerock.openicf.csvfile.CSVFileConnector",
                "iconClass" : "icon-csv",
                "src": "img/icon-csv.png"
            },
            {
                "type" : "org.forgerock.openicf.connectors.googleapps.GoogleAppsConnector",
                "iconClass" : "icon-cloud",
                "src": "img/icon-cloud.png"
            },
            {
                "type" : "org.forgerock.openidm.salesforce.Salesforce",
                "iconClass" : "icon-cloud",
                "src": "img/icon-cloud.png"
            },
            {
                "type" : "org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConnector",
                "iconClass" : "icon-scriptedsql",
                "src": "img/icon-scriptedsql.png"
            },
            {
                "type" : "managedobject",
                "iconClass" : "icon-database",
                "src": "img/icon-managedobject.png"
            }

        ]
    };

    obj.cleanConnectorName = function(name) {
        var clearName = name.split(".");
        clearName = clearName[clearName.length - 2] + "_" +clearName[clearName.length - 1];

        return clearName;
    };

    obj.getMappingDetails = function(sourceName, targetName) {
        var iconList = obj.getIconList(),
            currentConnectors = ConnectorDelegate.currentConnectors(),
            deferred = $.Deferred(),
            details = null;

        $.when(iconList, currentConnectors).then(function(icons, connectors){
            details = {};

            if(targetName !== "managed") {
                details.targetConnector = _.find(connectors, function (connector) {
                    return connector.name === targetName;
                }, this);

                details.targetIcon = obj.getIcon(details.targetConnector.connectorRef.connectorName, icons);
            } else {
                details.targetConnector = null;
                details.targetIcon = obj.getIcon("managedobject", icons);
            }

            if(sourceName !== "managed") {
                details.sourceConnector = _.find(connectors, function (connector) {
                    return connector.name === sourceName;
                }, this);

                details.sourceIcon = obj.getIcon(details.sourceConnector.connectorRef.connectorName, icons);
            } else {
                details.sourceConnector = null;
                details.sourceIcon = obj.getIcon("managedobject", icons);
            }

            details.sourceName = sourceName;
            details.targetName = targetName;

            deferred.resolve(details);
        });

        return deferred;
    };

    obj.getIconList = function() {
        var deferred = $.Deferred();

        ConfigDelegate.readEntity("ui/iconlist").then(function(result){
                deferred.resolve(result.icons);
            },
            _.bind(function(){
                deferred.resolve(this.iconListDefaults.icons);

                ConfigDelegate.createEntity("ui/iconlist", obj.iconListDefaults);
            },this));

        return deferred;
    };

    obj.getIcon = function (iconType, iconList) {
        var foundIcon = null;

        foundIcon = _.find(iconList, function(icon){
            return icon.type === iconType;
        });

        if(!foundIcon) {
            foundIcon = {
                "iconClass" : "connector-icon-default",
                "src": "img/icon-default-01.png"
            };
        }

        return foundIcon;
    };

    return obj;
});
