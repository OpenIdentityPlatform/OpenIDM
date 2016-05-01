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
 * Copyright 2015-2016 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "handlebars",
    "org/forgerock/commons/ui/common/main/AbstractConfigurationAware",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate"
], function ($, _,
             Handlebars,
             AbstractConfigurationAware,
             ConfigDelegate,
             ConnectorDelegate) {

    var obj = {};

    /**
     * Retrieves a list of connectors and managed objects and creates an array of resources
     *
     * @returns {promise} promise - resolves with an array of strings
     */
    obj.getAvailableResourceEndpoints = function() {
        var connectorPromise = ConnectorDelegate.currentConnectors(),
            managedPromise = ConfigDelegate.readEntity("managed"),
            resources = [];

        return $.when(connectorPromise, managedPromise).then(_.bind(function(connectors, managedObjects) {
            _.each(managedObjects.objects, _.bind(function(managed){
                resources.push("managed/" + managed.name);
            }, this));

            _.each(connectors, _.bind(function(connector) {
                _.each(connector.objectTypes, _.bind(function(ot) {
                    if (ot !== "__ALL__") {
                        resources.push("system/" + connector.name + "/" + ot);
                    }
                }, this));
            }, this));

            return resources;
        }, this));
    };

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

    obj.toggleValue = function(e) {
        var toggle = this.$el.find(e.target);
        if (toggle.val() === "true") {
            toggle.val(false);
        } else {
            toggle.val(true);
        }
    };

    /**
     * @description A handlebars helper checking if an item is contained in a list
     *
     * @example:
     *
     * {{#contains ["cat", "dog"]  "bird"}}
     *      <span>DOES CONTAIN ITEM</span>
     * {{else}}
     *      <span>DOES NOT CONTAIN</span>
     * {{/contains}}
     */
    Handlebars.registerHelper("contains", function(list, item, options) {
        if (_.indexOf(list, item) >= 0) {
            return options.fn(this);
        } else {
            return options.inverse(this);
        }
    });

    /**
     * @description A handlebars helper that "eaches" over the union of two lists
     *
     * @example:
     *
     * {{#eachTwoLists ["cat", "dog"]  ["bird", "cat", "bug"]}}
     *      <span>{{this}}</span>
     * {{/eachTwoLists}}
     *
     * Looks like: cat dog bird bug
     */
    Handlebars.registerHelper("eachTwoLists", function(list1, list2, options) {
        var ret = "";

        _.each(_.union(list1, list2), function(val) {
            ret = ret + options.fn(val);
        });

        return ret;
    });

    Handlebars.registerHelper("singleSelect", function(value, options){
        var selected = $("<select />").html(options.fn(this));
        selected.find("[value='" + value + "']").attr({"selected":"selected"});

        return selected.html();
    });

    return obj;
});