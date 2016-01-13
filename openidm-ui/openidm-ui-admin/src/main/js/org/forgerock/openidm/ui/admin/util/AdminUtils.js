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
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate",
    "org/forgerock/commons/ui/common/util/ModuleLoader"
], function ($, _,
             Handlebars,
             AbstractConfigurationAware,
             ConfigDelegate,
             ConnectorDelegate,
             ModuleLoader) {

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

    /**
     * @param name
     * @returns {string}
     *
     * This function takes in a word and capitalizes the first letter
     */
    obj.capitalizeName = function(name) {
        return name.charAt(0).toUpperCase() + name.substr(1);
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
        } else if (type[0] === "managed") {
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
        } else {
            propertiesPromise.resolve([]);
        }

        return propertiesPromise;
    };
    
    /**
     * @param {string} message The text provided in the main body of the dialog
     * @param {Function} confirmCallback Fired when the delete button is clicked
     *
     * @example
     *  AdminUtils.confirmDeleteDialog($.t("templates.admin.ResourceEdit.confirmDelete"), _.bind(function(){
     *      //Useful stuff here
     *  }, this));
     */
    obj.confirmDeleteDialog = function(message, confirmCallback){
        ModuleLoader.load("bootstrap-dialog").then(function (BootstrapDialog) {
            var btnType = "btn-danger";

            BootstrapDialog.show({
                title: $.t('common.form.confirm') + " " + $.t('common.form.delete'),
                type: "type-danger",
                message: message,
                id: "frConfirmationDialog",
                buttons: [
                    {
                        label: $.t('common.form.cancel'),
                        id: "frConfirmationDialogBtnClose",
                        action: function(dialog){
                            dialog.close();
                        }
                    },
                    {
                        label: $.t('common.form.delete'),
                        cssClass: btnType,
                        id: "frConfirmationDialogBtnDelete",
                        action: function(dialog) {
                            if(confirmCallback) {
                                confirmCallback();
                            }
                            dialog.close();
                        }
                    }
                ]
            });
        });
    };

    /**
     * @param availableProps {array} - array of a resource's availableProps objects from findPropertiesList
     * @param existingFields {array} - properties to be filtered out
     * @returns {array}
     *
     * This function filters out the all props that are named "_id", are not of type string,
     * are encrypted, or are already existing in the current list of availableProps
     */
    obj.filteredPropertiesList = function(availableProps, existingFields) {
        return _.chain(availableProps)
                    .omit((prop, key) => {
                        return prop.type !== "string" ||
                               key === "_id" ||
                               _.has(prop, "encryption") ||
                               _.contains(existingFields, key);
                    })
                    .keys()
                    .sortBy()
                    .value();
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
            return options.fn(item);
        } else {
            return options.inverse(item);
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

    return obj;
});
