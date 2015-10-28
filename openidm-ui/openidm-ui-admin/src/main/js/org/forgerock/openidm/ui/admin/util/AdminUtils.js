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
 * Copyright 2015 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/admin/util/AdminUtils", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractConfigurationAware",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate"
], function ($, _,
             AbstractConfigurationAware,
             ConfigDelegate,
             ConnectorDelegate) {

    var obj = {};

    obj.findPropertiesList = function(type, required) {
        var connectorUrl,
            properties,
            propertiesPromise = $.Deferred();

        if(type[0] === "system") {
            ConnectorDelegate.currentConnectors().then(_.bind(function(connectors) {
                connectorUrl = _.find(connectors, function(connector) {
                    return connector.name === type[1];
                }, this);

                if(connectorUrl && connectorUrl.config && connectorUrl.config.length > 0) {
                    connectorUrl = connectorUrl.config.split("/");

                    ConfigDelegate.readEntity(connectorUrl[1] +"/" +connectorUrl[2]).then(_.bind(function(config) {
                        if(required) {
                            properties = _.pick(config.objectTypes[type[2]].properties, function(property) {
                                return property.required === true;
                            });
                        } else {
                            properties = config.objectTypes[type[2]].properties;
                        }

                        propertiesPromise.resolve(properties, config);
                    }, this));
                } else {
                    propertiesPromise.resolve([]);
                }
            }, this));
        } else {
            ConfigDelegate.readEntity("managed").then(_.bind(function(managed) {
                properties = _.find(managed.objects, function(managedObject) {
                    return managedObject.name === type[1];
                }, this);

                if(properties.schema && properties.schema.properties) {
                    if(required) {

                        properties = _.pick(properties.schema.properties, function(value, key) {
                            var found = false;

                            _.each(properties.schema.required, function(field) {
                                if(field === key) {
                                    found = true;
                                }
                            });

                            return found;
                        });

                        propertiesPromise.resolve(properties);
                    } else {
                        propertiesPromise.resolve(properties.schema.properties);
                    }
                } else {
                    propertiesPromise.resolve([]);
                }
            }, this));
        }

        return propertiesPromise;
    };

    return obj;
});