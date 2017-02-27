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
 * Copyright 2017 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function ($, _, UIUtils) {
    var obj = {};

    /**
    * This function takes in a table row click event and returns the index of the clicked row
    *
    * @param {object} event - a click event
    * @returns {number} - the index of the clicked table row
    */
    obj.getClickedRowIndex = function (e) {
        var index;

        _.each($(e.currentTarget).closest("table tbody").find("tr"), function(tr, i) {
            if (tr === $(e.currentTarget).closest("tr")[0]) {
                index = i;
            }
        });

        return index;
    };
    /**
    * @param {object} schema - an object schema (at the least "properties", "order", "required")
    * @returns {array} - an array of property objects ordered by schema.order
    */
    obj.convertSchemaToPropertiesArray = function(schema) {
        return _.map(schema.order || _.keys(schema.properties), (propName) => {
            var prop = schema.properties[propName];

            prop.required = _.indexOf(schema.required, propName) >= 0;

            prop.propName = propName;

            return prop;
        });
    };
    /**
    * @param {array} propArray - an ordered array of property objects
    * @returns {object} - a schema object including ("properties", "order", "required")
    */
    obj.convertPropertiesArrayToSchema = function(propArray) {
        var schema = {
            properties: {},
            order: [],
            required: []
        };

        _.each(propArray, (prop) => {
            schema.order.push(prop.propName);

            if (prop.required) {
                schema.required.push(prop.propName);
            }

            schema.properties[prop.propName] = _.omit(prop,"propName","required");
        });

        return schema;
    };
    /**
    * digs into items of an array and gets a ref to the last spot where items exist
    * this is here so to handle deeply nested objects (why would anyone do such a thing?!?)
    * @param {object} - the object representing the "items" property of an array type property
    * @returns {object} - the last in the line of array items objects
    */
    obj.handleArrayNest = function (arrayItems) {
        var getItemsFromItems = function (items) {
            items = items.items;

            if (items.items) {
                return getItemsFromItems(items);
            } else {
                return items;
            }
        };

        if (arrayItems.items) {
            arrayItems = getItemsFromItems(arrayItems);
        }

        return arrayItems;
    };
    /**
     * @param {object} view the view where the tabs exist
     * @param {string} newTab a string representing a hash address to the anchor of the new tab to be viewed
     * @param {Function} confirmCallback Fired when the "Save Changes" button is clicked
     */
    obj.confirmSaveChanges = function(view, newTab, confirmCallback, cancelCallback){
        var overrides = {
            title : $.t("common.form.save") + "?",
            okText : $.t("common.form.save"),
            cancelText : $.t("templates.admin.ResourceEdit.discard"),
            cancelCallback: () => {
                view.render(view.args, () => {
                    view.$el.find('a[href="' + newTab + '"]').tab('show');
                });
            }
        };

        if (cancelCallback) {
            overrides.cancelCallback = cancelCallback;
        }

        UIUtils.confirmDialog($.t("templates.admin.ResourceEdit.saveChangesMessage"), "danger", confirmCallback, overrides);
    };
    /**
    * This function takes in a title and propertyType and returns a default schema property based on propertyType
    * @param {string} title
    * @param {string} propertyType
    * @returns {object} - a schema property object
    */
    obj.getPropertyTypeDefault = function (title, propertyType) {
        var defaultProps = {
            "boolean" : {
                title: title,
                type: "boolean",
                viewable: true,
                searchable: false,
                userEditable: true
            },
            "array" : {
                title: title,
                type: "array",
                viewable: true,
                searchable: false,
                userEditable: true,
                items : {
                    type: "string"
                }
            },
            "object" : {
                title: title,
                type: "object",
                viewable: true,
                searchable: false,
                userEditable: true,
                properties: {},
                order: [],
                required: []
            },
            "relationship" : {
                title: title,
                type: "relationship",
                viewable: true,
                searchable: false,
                userEditable: false,
                returnByDefault: false,
                reverseRelationship: false,
                reversePropertyName: "",
                validate: false,
                properties: {
                    _ref : {
                        type : "string"
                    },
                    _refProperties : {
                        type : "object",
                        properties : {
                            _id : {
                                type : "string"
                            }
                        }
                    }
                },
                resourceCollection: []
            },
            "relationships" : {
                title: title,
                type: "array",
                items: {
                    type: "relationship",
                    reverseRelationship: false,
                    reversePropertyName: "",
                    validate: false,
                    properties: {
                        _ref : {
                            type : "string"
                        },
                        _refProperties : {
                            type : "object",
                            properties : {
                                _id : {
                                    type : "string"
                                }
                            }
                        }
                    },
                    resourceCollection: []
                },
                viewable: true,
                searchable: false,
                userEditable: false,
                returnByDefault: false
            },
            "number" : {
                title: title,
                type: "number",
                viewable: true,
                searchable: true,
                userEditable: true
            },
            "string" : {
                title: title,
                type: "string",
                viewable: true,
                searchable: true,
                userEditable: true
            }
        };

        return defaultProps[propertyType || "string"];
    };
    /**
    * This function is called when a managed object is deleted. It looks into all managed objects,
    * grabs all their relationship type properties, and removes and any resourceCollection array items
    * that have path equal to the managed object being deleted.
    **/
    obj.removeRelationshipOrphans = function(managedConfigObjects, deletedObject) {
        var deletedObjectPath = "managed/" + deletedObject;

        _.each(managedConfigObjects, (managedObject) => {
            var singletonRelationships = _.filter(managedObject.schema.properties, { type : "relationship" }),
                arraysOfRelatiohsips = _.filter(managedObject.schema.properties, (prop) => {
                    return (prop.type === "array" && prop.items.type === "relationship");
                }),
                doDelete = (resourceCollection) => {
                    var removeIndex = _.findIndex(resourceCollection, { path : deletedObjectPath });
                    if (removeIndex > -1) {
                        resourceCollection.splice(removeIndex,1);
                    }
                };

            _.each(singletonRelationships, (rel) => {
                doDelete(rel.resourceCollection);
            });

            _.each(arraysOfRelatiohsips, (rel) => {
                doDelete(rel.items.resourceCollection);
            });
        });

        return managedConfigObjects;
    };

    return obj;
});
