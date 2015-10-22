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

    obj.findPropertiesList = function(type) {
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
                        properties = config.objectTypes[type[2]].properties;

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
                    propertiesPromise.resolve(properties.schema.properties);
                } else {
                    propertiesPromise.resolve([]);
                }
            }, this));
        }

        return propertiesPromise;
    };

    return obj;
});