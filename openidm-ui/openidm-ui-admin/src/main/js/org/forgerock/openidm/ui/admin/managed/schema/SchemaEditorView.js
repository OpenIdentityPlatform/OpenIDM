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
    "jsonEditor",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/admin/managed/schema/dataTypes/RelationshipTypeView"
], function($, _,
    JSONEditor,
    AdminAbstractView,
    RelationshipTypeView
) {
    var SchemaEditorView = AdminAbstractView.extend({
        template: "templates/admin/managed/schema/SchemaEditorViewTemplate.html",
        element: "#managedSchemaContainer",
        noBaseTemplate: true,
        model: {
            relationshipTypeViewMap: {}
        },
        events: {
            "submit #managedObjectSchemaForm" : "saveManagedSchema",
            "click .json-editor-btn-add" : "bindDataTypeSelectorChangeEvent"
        },

        render: function(args, callback) {
            this.parent = args[0];

            this.parentRender(() => {
                this.loadSchema();
                this.handleRelationships();

                if (callback) {
                    callback();
                }

            });

        },
        handleRelationships: function () {
            /*
                find all the relationship type elements in the json editor and replace
                them with instances of RelationshipTypeView
            */
            this.replaceRelationshipElements();
            this.bindDataTypeSelectorChangeEvent();

            /*
                every time we the "Add Property" button is clicked we need to bind
                the onclick events on the new dataTypeSelection dropdowns
            */
            this.$el.find(".json-editor-btn-add").click((e) => {
                this.bindDataTypeSelectorChangeEvent();
            });
            /*
                every time we the "x Property", "upArrow", or "downArrow" buttons are clicked we need to bind
                the onclick events on the dataTypeSelection dropdowns and re-replace all the relationshipTypeViews
            */
            this.$el.find(".json-editor-btn-delete,.json-editor-btn-moveup,.json-editor-btn-movedown").click((e) => {
                this.replaceRelationshipElements();
                this.bindDataTypeSelectorChangeEvent();
            });
        },
        /*
          binds the change event to all the property type and array itemType dropdowns
         */
        bindDataTypeSelectorChangeEvent: function () {
            //find all the property type and array itemType dropdowns
            var selectElements = this.$el.find("[data-schemaPath$='type']").find("select"),
                dataTypeChange = (e) => {
                    var dataTypeElement = $(e.target).parent(),
                        selectedOption = $(e.target).find("option:selected").val();

                    if (selectedOption === "Relationship") {
                        this.renderRelationshipType(dataTypeElement);
                    } else {
                        dataTypeElement.find(".relationshipTypeView").hide();
                    }
                };
            //remove any existing change events
            selectElements.unbind("change");
            //add the change event
            selectElements.change((e) => {
                dataTypeChange(e);
                //have to call this so child "type" fields get this event
                this.bindDataTypeSelectorChangeEvent();
            });
        },
        /**
         * finds all the item or itemType which have "Relationship" as their selected values
         * @returns - array of elements
        */
        getRelationshipElements: function () {
            return this.$el.find("[data-schemaPath$='type']")
                                            .find("select:eq(0):not([id]) option[value=Relationship]:selected")
                                            .parent().parent();
        },

        replaceRelationshipElements: function() {
            var relationshipElements = this.getRelationshipElements();
            //reset this.model.relationshipTypeViewMap
            this.model.relationshipTypeViewMap = {};
            //remove all existing relationshipTypeViews
            this.$el.find(".relationshipTypeView").remove();

            _.each(relationshipElements, (relationshipElement) => {
                this.renderRelationshipType($(relationshipElement));
            });
        },
        renderRelationshipType: function(relationshipElement) {
            var thisRelationshipTypeView = new RelationshipTypeView(),
                originalFormElements = relationshipElement.find("[data-schematype=object]"),
                typeSelect = relationshipElement.find("select:eq(0):not([id])"),
                elementId = relationshipElement.attr("data-schemaPath").replace(/\./g,"-") + "-relationshipType",
                /*
                    elementId will look like "root-properties-16-type-itemType-relationshipType"
                    propertyNameId should look like "0-root-properties-16-propertyName"
                */
                propertyNameId = "#0-" + elementId.split("-").splice(0,3).join("-") + "-propertyName",
                propertyName = this.$el.find(propertyNameId).val(),
                propertySchema = _.findWhere(this.data.managedObjectSchema.getValue().properties,{propertyName: propertyName});

            /*
             when changing the order of the properties the this.getRelationshipElements()
             might return an element that is not a relationship
             this check keeps everything in working order
            */
            if (propertySchema && _.isObject(propertySchema.type) && !_.isArray(propertySchema.type)) {
                //add the relationshipTypeView container div
                typeSelect.after("<div id='" + elementId + "' class='relationshipTypeView'></div>");
                //fill the container
                thisRelationshipTypeView.render({
                    elementId: elementId,
                    propertySchema: propertySchema
                });
                //add the RelationshipTypeView instance to the relationshipTypeViewMap
                this.model.relationshipTypeViewMap[propertyName] = thisRelationshipTypeView;

                originalFormElements.hide();
            }
        },
        /**
         * replaces the jsonEditor relationship property type values
         * with the values defined in their respective relationshipTypeViews
         * @param managedSchema - the full schema for this managed object before replacing relationship values
         */
        replaceRelationshipTypeValues: function (managedSchema) {
            _.each(this.model.relationshipTypeViewMap, (relationshipTypeView,propName) => {
                var prop = managedSchema.properties[propName];

                if(prop.items) {
                    prop.items = _.extend(prop.items,relationshipTypeView.getValue());
                } else {
                    prop = _.extend(prop,relationshipTypeView.getValue());
                }
            });

            return managedSchema;
        },

        getManagedSchema: function() {
            var managedSchema = _.extend({
                "$schema": "http://forgerock.org/json-schema#",
                "type": "object",
                "title": this.data.managedObjectSchema.getValue().title,
                "description": this.data.managedObjectSchema.getValue().description,
                "icon": this.$el.find("#managedObjectIcon").val()
            }, this.getObjectProperties(this.data.managedObjectSchema.getValue().properties));

            //replace all the relationshipType values
            managedSchema = this.replaceRelationshipTypeValues(managedSchema);

            return managedSchema;
        },

        saveManagedSchema: function(event) {
            event.preventDefault();
            //set showSchema so that when editManagedView is re-rendered after saving it will know to open up the schema tab
            this.parent.args.push("showSchema");

            this.parent.saveManagedObject(this.parent.data.currentManagedObject, this.parent.data.managedObjects);
        },

        loadSchema: function() {
            var JSONEditorDefaults = {
                    disable_edit_json: true,
                    disable_array_delete_all: true,
                    disable_array_reorder: false,
                    disable_collapse: true,
                    disable_properties: true,
                    show_errors: 'always',
                    template: 'handlebars',
                    no_additional_properties: true,
                    additionalItems: false,
                    required_by_default: true
                },
                JSONEditorSchema = {
                    "title": "Managed Object",
                    "type": "object",
                    "headerTemplate": "{{self.title}}",
                    "definitions": {
                        "oneOfTypes": {
                            "oneOf": [
                                {
                                    "title": "String",
                                    "type": "string",
                                    "format": "hidden"
                                }, {
                                    "title": "Array",
                                    "type": "object",
                                    "properties": {
                                        "itemType": {
                                            "title":"Item Type",
                                            "$ref": "#/definitions/oneOfTypes"
                                        }
                                    }
                                }, {
                                    "title": "Boolean",
                                    "type": "string",
                                    "displayType" : "Boolean",
                                    "format": "hidden"
                                }, {
                                    "title": "Integer",
                                    "type": "string",
                                    "format": "hidden"
                                }, {
                                    "title": "Number",
                                    "type": "string",
                                    "format": "hidden"
                                },{
                                    "title": "Object",
                                    "$ref": "#/definitions/managedObject"
                                },
                                {
                                    "title": "Relationship",
                                    "$ref": "#/definitions/relationship"
                                }
                            ]
                        },
                        "managedObject": {
                            "type": "array",
                            "format": "tabs",
                            "items": {
                                "type": "object",
                                "title": "Property",
                                "headerTemplate": "{{self.propertyName}}",
                                "properties": {
                                    "propertyName": {
                                        "title": "Property Name",
                                        "type": "string",
                                        "propertyOrder": 1
                                    },
                                    "title": {
                                        "title": "Readable Title",
                                        "type": "string",
                                        "propertyOrder": 2
                                    },
                                    "description": {
                                        "title": "Description",
                                        "type": "string",
                                        "format": "textarea",
                                        "propertyOrder": 3
                                    },
                                    "viewable": {
                                        "title": "Viewable",
                                        "type": "boolean",
                                        "required": true,
                                        "default": true,
                                        "propertyOrder": 4
                                    },
                                    "searchable": {
                                        "title": "Searchable",
                                        "type": "boolean",
                                        "required": true,
                                        "default": false,
                                        "propertyOrder": 5
                                    },
                                    "userEditable": {
                                        "title": "End users allowed to edit?",
                                        "type": "boolean",
                                        "required": true,
                                        "default": false,
                                        "propertyOrder": 6
                                    },
                                    "minLength": {
                                        "title": "Minimum Length",
                                        "type": "string",
                                        "propertyOrder": 7
                                    },
                                    "pattern": {
                                        "title": "Pattern",
                                        "type": "string",
                                        "propertyOrder": 8
                                    },
                                    "policies": {
                                        "title": "Validation policies",
                                        "type": "array",
                                        "required": true,
                                        "items": {
                                            "type": "object",
                                            "properties": {
                                                "policyId": {
                                                    "type": "string",
                                                    "title": "Policy"
                                                },
                                                "params": {
                                                    "$ref": "#/definitions/policyParams"
                                                }
                                            }
                                        },
                                        "default": [],
                                        "propertyOrder": 9
                                    },
                                    "required": {
                                        "title": "Required",
                                        "type": "boolean",
                                        "required": true,
                                        "default": false,
                                        "propertyOrder": 10
                                    },
                                    "returnByDefault": {
                                        "title": "Return by Default",
                                        "type": "boolean",
                                        "required": true,
                                        "default": false,
                                        "propertyOrder": 11
                                    },
                                    "type": {
                                        "title": "Type",
                                        "$ref": "#/definitions/oneOfTypes",
                                        "propertyOrder": 13
                                    }
                                }
                            }
                        },
                        "relationship": {
                            "type": "object",
                            "properties": {
                                "reverseRelationship": {
                                    "title": "Reverse Relationship",
                                    "type": "boolean",
                                    "default": false
                                },
                                "reversePropertyName": {
                                    "title": "Reverse Property Name",
                                    "type": "string",
                                    "required": true
                                },
                                "validate": {
                                    "title": "Validate",
                                    "type": "boolean",
                                    "required": true,
                                    "default": false
                                },
                                "properties": {
                                    "title": "Properties",
                                    "type": "object",
                                    "properties": {
                                        "_ref": {
                                            "type": "object",
                                            "properties": {
                                                "type": {
                                                    "type": "string",
                                                    "default": "string"
                                                }
                                            }
                                        },
                                        "_refProperties": {
                                            "type": "object",
                                            "properties": {
                                                "type": {
                                                    "type": "string",
                                                    "default": "object"
                                                },
                                                "properties": {
                                                    "$ref": "#/definitions/_refProperties"
                                                }
                                            }
                                        }

                                    }
                                },
                                "resourceCollection": {
                                    "title": "Resource Collection",
                                    "type": "array",
                                    "items": {
                                        "$ref": "#/definitions/resourceCollection"
                                    }
                                }
                            }
                        },
                        "_refProperties": {
                            "title": "Properties",
                            "type": "array",
                            "format": "tabs",
                            "items": {
                                "type": "object",
                                "headerTemplate": "{{self.propertyName}}",
                                "properties": {
                                    "propertyName": {
                                        "title": "Property Name",
                                        "type": "string"
                                    },
                                    "type": {
                                        "title": "Type",
                                        "type": "string",
                                        "default": "string"
                                    },
                                    "label": {
                                        "title": "Label",
                                        "type": "string",
                                        "default": ""
                                    }
                                }
                            },
                            "default": [{
                                "propertyName": "_id",
                                "type": "string",
                                "label": ""
                            }]
                        },
                        "policyParams": {
                            "title": "Params",
                            "type": "array",
                            "format": "tabs",
                            "items": {
                                "type": "object",
                                "properties": {
                                    "paramName": {
                                        "title": "Param Name",
                                        "type": "string"
                                    },
                                    "value": {
                                        "title": "Value",
                                        "type": "string"
                                    }
                                }
                            }
                        },
                        "resourceCollection": {
                            "title": "Resource Collection",
                            "type": "object",
                            "properties": {
                                "path": {
                                    "type": "string",
                                    "title": "Path"
                                },
                                "label": {
                                    "type": "string",
                                    "title": "Label"
                                },
                                "query": {
                                    "type": "object",
                                    "title": "Query",
                                    "properties": {
                                        "queryFilter": {
                                            "title": "Query Filter",
                                            "type": "string"
                                        },
                                        "fields": {
                                            "title": "Fields",
                                            "type": "array",
                                            "format": "table",
                                            "items": {
                                                "title": "Field",
                                                "type": "string"
                                            }
                                        },
                                        "sortKeys": {
                                            "title": "Sort Keys",
                                            "type": "array",
                                            "format": "table",
                                            "items": {
                                                "title": "Sort Key",
                                                "type": "string"
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    "properties": {
                        "title": {
                            "title": "Readable Title",
                            "type": "string"
                        },
                        "description": {
                            "title": "Description",
                            "type": "string"
                        },
                        "properties": {
                            "title": "Schema Properties",
                            "$ref": "#/definitions/managedObject"
                        }
                    }
                };

            // This function needs to be re-written for the ability to set the value of oneOf type
            // enumerations.  Before "type" would take a value and never set the title(dropdown)
            // Now the setValue val is an object containing a value and a title.

            // This function needs to be restored for other instances of JSONEditor after load.

            this.data.jsonEditorProto = _.clone(JSONEditor.defaults.editors.multiple.prototype);

            JSONEditor.defaults.editors.multiple.prototype.setValue = function(val,initial) {
                if (_.isObject(val) && !_.isNull(val)) {
                    this.switcher.value = val.display;
                    this.type = _.indexOf(this.display_text, val.display);
                    this.switchEditor(this.type);
                    this.editors[this.type].setValue(val.val, initial);
                    this.refreshValue();
                }
            };

            this.data.managedObjectSchema = new JSONEditor(this.$el.find(".schemaEditor")[0], _.extend({
                schema: JSONEditorSchema
            }, JSONEditorDefaults));

            this.data.managedObjectSchema.on("change", _.bind(function () {
                $(".schemaEditor input[type='hidden']").parent().hide();
            }, this));

            this.setDefaultData();

            JSONEditor.defaults.editors.multiple.prototype.setValue = this.data.jsonEditorProto.setValue;
        },

        setDefaultData: function() {
            var managedSchema = this.parent.data.currentManagedObject.schema,
                jsonSchema = {};

            if (managedSchema) {
                jsonSchema = {
                    title: managedSchema.title,
                    description:  managedSchema.description,
                    properties: []
                };

                jsonSchema.properties = this.translateSubProperties(managedSchema.properties, managedSchema.order, managedSchema.required);
            }

            this.data.managedObjectSchema.setValue(jsonSchema);
        },

        translateSubProperties: function(properties, order, required, forArray) {
            var jsonEditorProperties = [];

            _.each(order, function(propertyName) {
                jsonEditorProperties.push(this.getType(properties[propertyName], propertyName, required, forArray));
            }, this);

            return jsonEditorProperties;
        },

        getType: function(property, name, required, forArray) {
            var tempProperty = {},
                toModify;

            _.extend(tempProperty, _.omit(property, ["type", "order", "required", "items", "resourceCollection", "properties"]));

            // Copy over object Key as a property
            tempProperty.propertyName = name;
            tempProperty.required = _.indexOf(required, name) >= 0;

            // Copy over advanced properties
            switch (property.type) {
                case "boolean":
                case "integer":
                case "number":
                    if (forArray) {
                        tempProperty.itemType = {
                            "val": property.type,
                            "display": this.toProperCase(property.type)
                        };
                    } else {
                        tempProperty.type = {
                            "val": property.type,
                            "display": this.toProperCase(property.type)
                        };
                    }
                    break;

                case "string":
                    tempProperty.type = {
                        "val": property.type,
                        "display": this.toProperCase(property.type)
                    };
                    break;

                case "array":
                    if (forArray) {
                        toModify = tempProperty.itemType = {
                            "val": {},
                            "display": "Array"
                        };
                    } else {
                        toModify = tempProperty.type = {
                            "val": {},
                            "display": "Array"
                        };
                    }

                    if (_.has(property, "items")) {
                        toModify.val  = this.getType(property.items, null, null, true);
                    }
                    break;

                case "object":
                    if (forArray) {
                        tempProperty.itemType = {
                            "val": this.translateSubProperties(property.properties, property.order, property.required, false),
                            "display": this.toProperCase(property.type)
                        };

                        forArray = false;
                    } else {
                        tempProperty.type = {
                            "val": this.translateSubProperties(property.properties, property.order, property.required, false),
                            "display": this.toProperCase(property.type)
                        };
                    }
                    break;

                case "relationship":
                    if (forArray) {
                        tempProperty.itemType = {
                            "val": this.translateRelationship(property),
                            "display": this.toProperCase(property.type)
                        };

                        forArray = false;
                    } else {
                        tempProperty.type = {
                            "val": this.translateRelationship(property),
                            "display": this.toProperCase(property.type)
                        };
                    }
                    break;
            }

            return this.translatePolicies(tempProperty);
        },

        translateRelationship: function (property) {
            var refProps = [];

            _.map(property.properties._refProperties.properties, function (val,key) {
                refProps.push({
                    "propertyName": key,
                    "type": val.type,
                    "label": val.label
                });
            });

            property.properties._refProperties.properties = refProps;

            return property;
        },
        translatePolicies: function (property) {
            if (property.policies) {
                _.map(property.policies, function (policy) {
                    var policyParams = [];
                    _.each(policy.params, function (val,key) {
                        policyParams.push({
                            "paramName": key,
                            "value": val
                        });
                    });

                    policy.params = policyParams;
                });
            }

            return property;
        },

        toProperCase: function(toConvert) {
            return toConvert.charAt(0).toUpperCase() + toConvert.slice(1);
        },

        getObjectProperties: function(props, node) {
            var data = {
                "properties": {},
                "required": [],
                "order": []
            };
            _.each(props, function(property, index) {
                var convertPolicyParams = function (policies) {
                    _.each(policies, function (policy) {
                        var policyParams = {};
                        _.each(policy.params, function (param, key) {
                            var val;
                            try {
                                val = $.parseJSON(param.value);
                            } catch (e) {
                                val = param.value;
                            }

                            if (val && val.paramName) {
                                policyParams[val.paramName] = val.value;
                            } else if (!val) {
                                policyParams[key] = param;
                            } else {
                                policyParams[param.paramName] = val;
                            }
                        });
                        policy.params = policyParams;
                    });

                    return policies;
                };

                //handle the minLength property
                if (property.minLength === null || !property.minLength.length || !parseInt(property.minLength, 10)) {
                    property.minLength = null;
                } else {
                    property.minLength = parseInt(property.minLength, 10);
                }

                data.properties[property.propertyName] = {
                    "description": property.description,
                    "title": property.title,
                    "viewable": property.viewable,
                    "searchable": property.searchable,
                    "userEditable": property.userEditable,
                    "policies": convertPolicyParams(property.policies),
                    "returnByDefault": property.returnByDefault,
                    "minLength": property.minLength,
                    "pattern": property.pattern
                };

                if (!node) {
                    this.addType("root.properties."+ index +".type", data.properties[property.propertyName], property.type);
                } else {
                    this.addType(node + "." + index + ".type", data.properties[property.propertyName], property.type);
                }


                if (property.required) {
                    data.required.push(property.propertyName);
                }

                data.order.push(property.propertyName);
            }, this);

            return data;
        },

        /**
         * Recursively retrieves the type of a value
         *
         * @param node (root.properties.0.type or root.properties.0.type.itemType)
         * @param property (schema.properties.myPropertyName or schema.properties.myPropertyName.items
         * @param value - If this is an object leaf node or resource collection there is a value to provide
         * @param editor
         */
        addType: function(node, property, value) {
            var nodeSelect = $("[data-schemapath='"+node+"'] select:first"),
                nodeType =  nodeSelect.val();

            switch (nodeType) {
                case "Array":
                    property.type = "array";
                    property.items = {};
                    this.addType(node + ".itemType", property.items, this.data.managedObjectSchema.editors[node + ".itemType"].value);
                    break;

                case "Object":
                    property.type = "object";
                    _.extend(property, this.getObjectProperties(value, node));
                    break;

                case "Relationship":
                    property.type = "relationship";
                    _.extend(property, this.getRelationshipProperties(value));
                    break;
                default:
                    property.type = nodeType.toLowerCase();
                    break;
            }
        },

        getRelationshipProperties: function(props) {
            var refProps = {};

            _.each(props.properties._refProperties.properties, function (prop) {
                refProps[prop.propertyName] = {
                    "type": prop.type,
                    "label": prop.label
                };
            });

            props.properties._refProperties.properties = refProps;

            return props;
        }
    });

    return new SchemaEditorView();
});
