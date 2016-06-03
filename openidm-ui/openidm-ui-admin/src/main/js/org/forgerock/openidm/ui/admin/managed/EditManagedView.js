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

define("org/forgerock/openidm/ui/admin/managed/EditManagedView", [
    "jquery",
    "underscore",
    "handlebars",
    "form2js",
    "jsonEditor",
    "org/forgerock/openidm/ui/admin/managed/AbstractManagedView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/util/ScriptList",
    "org/forgerock/commons/ui/common/util/ModuleLoader",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "faiconpicker",
    "bootstrap-tabdrop"
], function($, _,
            handlebars,
            form2js,
            JSONEditor,
            AbstractManagedView,
            EventManager,
            validatorsManager,
            Constants,
            Router,
            ConfigDelegate,
            ScriptList,
            ModuleLoader,
            UIUtils,
            faiconpicker,
            tabdrop) {

    var EditManagedView = AbstractManagedView.extend({
        template: "templates/admin/managed/EditManagedTemplate.html",
        events: {
            "submit #managedObjectDetailsForm" : "saveManagedDetails",
            "submit #managedObjectScriptsForm" : "saveManagedScripts",
            "submit #managedObjectPropertiesForm" : "saveManagedProperties",
            "submit #managedObjectSchemaForm" : "saveManagedSchema",
            "onValidate": "onValidate",
            "click #addManagedProperties": "addProperty",
            "click .property-remove" : "removeProperty",
            "click #deleteManaged": "deleteManaged",
            "click .tab-menu li": "changeTabs"
        },
        eventHooks: [],
        propertyHooks: [],
        propertiesCounter: 0,
        partials: [
            "partials/managed/_property.html"
        ],
        model: {
            propertyScripts: [],
            eventList : ["onCreate", "postCreate", "onRead", "onUpdate", "postUpdate", "onDelete", "postDelete", "onValidate", "onRetrieve", "onStore", "onSync"]
        },

        render: function(args, callback) {
            var managedPromise,
                repoCheckPromise,
                eventKeys,
                propertiesEventList = ["onValidate", "onRetrieve", "onStore"];

            this.args = args;
            this.data = {
                selectEvents: [],
                addedEvents: [],
                propertiesEventList: propertiesEventList,
                docHelpUrl : Constants.DOC_URL,
                noSchema: true
            };
            
            if (this.args[1] && this.args[1] === "showSchema") {
                this.data.showSchema = true;
            }

            this.eventHooks = [];
            this.propertyHooks = [];
            this.propertiesCounter = 0;

            managedPromise = ConfigDelegate.readEntity("managed");
            repoCheckPromise = ConfigDelegate.getConfigList();

            $.when(managedPromise, repoCheckPromise).then(_.bind(function(managedObjects, configFiles){
                this.data.managedObjects = managedObjects;

                _.each(managedObjects.objects, _.bind(function(managedObject, iterator) {
                    if(managedObject.name === args[0]) {
                        this.data.currentManagedObject = managedObject;
                        this.data.currentManagedObjectIndex = iterator;
                        this.splitSchemaAndProperties();

                        this.data.noSchema = _.isEmpty(this.data.currentManagedObject.schema.properties);
                    }
                }, this));

                eventKeys =
                    _.chain(this.data.currentManagedObject)
                        .keys()
                        .without("name", "properties")
                        .value();

                //Added events are used for the events that are currently set by the managed object
                this.data.addedEvents = _.intersection(eventKeys, this.model.eventList);

                //Select events are the events currently available for the select
                this.data.selectEvents = _.difference(this.model.eventList, eventKeys);

                if(this.data.currentManagedObject.properties) {
                    _.each(this.data.currentManagedObject.properties, function (property) {
                        eventKeys = _.chain(property)
                            .keys()
                            .without("name", "encryption", "scope", "type", "secureHash")
                            .value();

                        property.addedEvents = _.intersection(eventKeys, propertiesEventList);
                        property.selectEvents = _.difference(propertiesEventList, eventKeys);
                    }, this);

                    this.data.availableProperties = _.keys(_.omit(this.data.currentManagedObject.schema.properties,"_id"));
                    this.data.availableHashes = ["MD5","SHA-1","SHA-256","SHA-384","SHA-512"];
                }

                this.checkRepo(configFiles[0], _.bind(function(){
                    this.managedRender(callback);
                }, this));
            }, this));
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
            var managedSchema = this.data.currentManagedObject.schema,
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
                case "String":
                case "Boolean":
                case "Integer":
                case "Number":
                    property.type = nodeType.toLowerCase();
                    break;

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

        getManagedSchema: function() {
            return _.extend({
                "$schema": "http://forgerock.org/json-schema#",
                "type": "object",
                "title": this.data.managedObjectSchema.getValue().title,
                "description": this.data.managedObjectSchema.getValue().description,
                "icon": this.$el.find("#managedObjectIcon").val()
            }, this.getObjectProperties(this.data.managedObjectSchema.getValue().properties));
        },

        managedRender: function(callback) {
            this.parentRender(_.bind(function () {
                validatorsManager.bindValidators(this.$el);
                validatorsManager.validateAllFields(this.$el);

                this.propertiesCounter = this.$el.find(".add-remove-block").length;

                this.loadSchema();

                this.$el.find('#managedObjectIcon').iconpicker({
                    hideOnSelect: true
                });

                this.model.managedScripts = ScriptList.generateScriptList({
                    element: this.$el.find("#managedScripts"),
                    label: $.t("templates.managed.addManagedScript"),
                    selectEvents: this.data.selectEvents,
                    addedEvents:this.data.addedEvents,
                    currentObject: this.data.currentManagedObject,
                    hasWorkflow: true,
                    workflowContext: _.pluck(this.data.managedObjectSchema.getValue().properties, "propertyName")                
                });

                _.each(this.$el.find("#managedPropertyWrapper .small-field-block"), function(managedProperty, index) {
                    this.propertyHooks.push([]);

                    this.model.propertyScripts.push(ScriptList.generateScriptList({
                        element: $(managedProperty).find(".managedPropertyEvents"),
                        label: $.t("templates.managed.addPropertyScript"),
                        selectEvents: this.data.currentManagedObject.properties[index].selectEvents,
                        addedEvents:this.data.currentManagedObject.properties[index].addedEvents,
                        currentObject: this.data.currentManagedObject.properties[index],
                        hasWorkflow: true,
                        workflowContext: _.pluck(this.data.managedObjectSchema.getValue().properties, "propertyName")
                    }));

                }, this);

                this.$el.find(".nav-tabs").tabdrop();
                
                if(this.data.currentManagedObject.properties) {
                    _.each(this.data.currentManagedObject.properties, function (property, index) {
                        this.setPropertyHashToggle(index);
                    }, this);
                }

                if (callback) {
                    callback();
                }
            }, this));
        },

        saveManagedDetails: function(event) {
            event.preventDefault();

            var data = form2js('managedForm2JS', '.', true),
                nameCheck;

            nameCheck = this.checkManagedName(data.name, this.data.managedObjects.objects);

            if(!nameCheck || data.name === this.data.currentManagedObject.name) {
                this.data.currentManagedObject.name = data.name;
                this.data.currentManagedObject.schema.icon = this.$el.find("#managedObjectIcon").val();
                this.$el.find("#managedErrorMessage").hide();

                this.saveManagedObject(this.data.currentManagedObject, this.data.managedObjects, _.bind(function() {
                    this.$el.find("#managedObjectDisplayName").html(this.data.currentManagedObject.name);

                    if(this.data.currentManagedObject.schema.icon) {
                        this.$el.find(".header-icon i").prop("class", "fa " + this.data.currentManagedObject.schema.icon);
                    } else {
                        this.$el.find(".header-icon i").prop("class", "fa fa-cube");
                    }
                }, this));
            } else {
                this.$el.find("#managedErrorMessage .message").html($.t("templates.managed.duplicateNameError"));
                this.$el.find("#managedErrorMessage").show();
                this.$el.find("#saveManagedDetails").prop("disabled", true);
            }
        },

        saveManagedProperties: function(event) {
            event.preventDefault();
            
            var data = form2js('managedObjectPropertiesForm', '.', true),
                properties = [];

            this.data.currentManagedObject.schema = this.getManagedSchema();
            
            _.each(data.properties, function (prop, index) {
                if (prop.encryption) {
                    prop.encryption = {
                        key: "openidm-sym-default"
                    };
                } else {
                    delete prop.encryption;
                }

                if (prop.scope) {
                    prop.scope = "private";
                } else {
                    delete prop.scope;
                }

                if (prop.type) {
                    prop.type = "virtual";
                } else {
                    delete prop.type;
                }

                if (prop.secureHash) {
                    prop.secureHash = {
                        algorithm: prop.algorithm
                    };
                } else {
                    delete prop.secureHash;
                }
                
                delete prop.algorithm;

                _.extend(prop, this.model.propertyScripts[index].getScripts());
                
                if (prop.name) {
                    properties.push(prop);
                }
            }, this);
            
            this.data.currentManagedObject.properties = properties;

            this.saveManagedObject(this.data.currentManagedObject, this.data.managedObjects, _.noop);
        },

        saveManagedScripts: function(event) {
            event.preventDefault();

            var scriptList = this.model.managedScripts.getScripts();

            _.each(this.model.eventList, function(val) {
                if(scriptList[val]) {
                    this.data.currentManagedObject[val] = scriptList[val];
                } else {
                    delete this.data.currentManagedObject[val];
                }
            }, this);

            this.saveManagedObject(this.data.currentManagedObject, this.data.managedObjects, _.noop);
        },

        saveManagedSchema: function(event) {
            event.preventDefault();

            this.saveManagedObject(this.data.currentManagedObject, this.data.managedObjects, _.bind(function () {
                this.args.push("showSchema");
                this.render(this.args);
            }, this));
        },

        deleteManaged: function(event) {
            event.preventDefault();

            var promises = [];

            UIUtils.confirmDialog($.t("templates.managed.managedDelete"), "danger", _.bind(function(){
                _.each(this.data.managedObjects.objects, function(managedObject, index){
                    if(managedObject.name === this.data.currentManagedObject.name) {
                        this.data.managedObjects.objects.splice(index, 1);
                    }
                }, this);

                if(this.data.currentRepo === "repo.orientdb") {
                    if(this.data.repoObject.dbStructure.orientdbClass["managed_" + this.data.currentManagedObject.name] !== undefined){
                        delete this.data.repoObject.dbStructure.orientdbClass["managed_" + this.data.currentManagedObject.name];
                    }

                    promises.push(ConfigDelegate.updateEntity(this.data.currentRepo, this.data.repoObject));
                }

                promises.push(ConfigDelegate.updateEntity("managed", {"objects" : this.data.managedObjects.objects}));

                $.when.apply($, promises).then(function(){
                        EventManager.sendEvent(Constants.EVENT_UPDATE_NAVIGATION);
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteManagedSuccess");
                        EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {route: Router.configuration.routes.managedListView});
                    },
                    function(){
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteManagedFail");
                    });

            },this));
        },

        addProperty: function(event) {
            event.preventDefault();

            var checkboxes,
                secureHashSelection,
                field,
                input;

            field = $(handlebars.compile("{{> managed/_property}}")({ 
                availableProperties : this.data.availableProperties,
                availableHashes : this.data.availableHashes 
            }));
            field.removeAttr("id");
            input = field.find('.properties_name_selection');
            input.val("");
            input.attr("data-validator-event","keyup blur");
            input.attr("data-validator","required");

            checkboxes = field.find(".checkbox");
            secureHashSelection = field.find(".secureHash_selection");

            this.propertiesCounter = this.propertiesCounter + 1;

            input.prop( "name", "properties[" +this.propertiesCounter  +"].name");
            $(checkboxes[0]).prop( "name", "properties[" +this.propertiesCounter  +"].encryption");
            $(checkboxes[0]).prop( "id", this.propertiesCounter  +"_encryption_cb");
            $(checkboxes[1]).prop( "name", "properties[" +this.propertiesCounter  +"].scope");
            $(checkboxes[2]).prop( "name", "properties[" +this.propertiesCounter  +"].type");
            $(checkboxes[3]).prop( "name", "properties[" +this.propertiesCounter  +"].secureHash");
            $(checkboxes[3]).prop( "id", this.propertiesCounter  + "_secureHash_cb");
            $(secureHashSelection[0]).prop( "name", "properties[" +this.propertiesCounter  +"].algorithm");
            $(secureHashSelection[0]).prop( "id", this.propertiesCounter  + "_secureHash_selection");

            field.show();

            this.propertyHooks.push([]);

            if(!this.data.currentManagedObject.properties) {
                this.data.currentManagedObject.properties = [];
            }

            this.model.propertyScripts.push(ScriptList.generateScriptList({
                element: field.find(".managedPropertyEvents"),
                label: $.t("templates.managed.addPropertyScript"),
                selectEvents: this.data.propertiesEventList,
                addedEvents:[],
                currentObject: {},
                hasWorkflow: true
            }));

            this.$el.find('#managedPropertyWrapper').append(field);
            this.setPropertyHashToggle(this.propertiesCounter);
        },
        
        setPropertyHashToggle: function (index) {
            var encryption_cb = this.$el.find("#" + index + "_encryption_cb"),
                secureHash_cb = this.$el.find("#" + index + "_secureHash_cb"),
                secureHash_selection = this.$el.find("#" + index + "_secureHash_selection");
            
            encryption_cb.click(function(){
                if ($(this).is(":checked")) {
                    secureHash_selection.hide();
                    secureHash_cb.attr("checked",false);
                }
            });
            
            secureHash_cb.click(function(){
                if ($(this).is(":checked")) {
                    secureHash_selection.show();
                    encryption_cb.attr("checked",false);
                } else {
                    secureHash_selection.hide();
                }
            });
        },

        removeProperty: function(event) {
            event.preventDefault();

            UIUtils.confirmDialog($.t("templates.managed.propertyDelete"), "danger", _.bind(function() {
                var clickedEle = event.target,
                    targetIndex;

                clickedEle = $(clickedEle).parents(".managed-property");
                targetIndex = this.$el.find(".managed-property:visible").index(clickedEle);

                this.propertyHooks.splice(targetIndex, 1);
                this.model.propertyScripts.splice(targetIndex, 1);

                clickedEle.remove();
            },this));
        },

        changeTabs: function(e) {
            if ($(e.currentTarget).hasClass("disabled")) {
                return false;
            }
        }
    });

    return new EditManagedView();
});
