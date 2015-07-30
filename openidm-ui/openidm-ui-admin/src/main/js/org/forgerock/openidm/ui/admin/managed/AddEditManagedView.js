/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 ForgeRock AS. All rights reserved.
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

define("org/forgerock/openidm/ui/admin/managed/AddEditManagedView", [
    "jquery",
    "underscore",
    "form2js",
    "jsonEditor",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/admin/util/ScriptList",
    "org/forgerock/commons/ui/common/util/ModuleLoader"
], function($, _, form2js, JSONEditor, AdminAbstractView, eventManager, validatorsManager, constants, router, ConfigDelegate, uiUtils, ScriptList, ModuleLoader) {

    var AddEditManagedView = AdminAbstractView.extend({
        template: "templates/admin/managed/AddEditManagedTemplate.html",
        events: {
            "click input[type=submit]": "formSubmit",
            "onValidate": "onValidate",
            "click #managedObjectForm fieldset legend" : "sectionHideShow",
            "click #addManagedProperties": "addProperty",
            "click .property-remove" : "removeProperty",
            "click #deleteManaged": "deleteManaged"
        },
        eventHooks: [],
        propertyHooks: [],
        propertiesCounter: 0,
        model: {
            propertyScripts: []
        },

        render: function(args, callback) {
            var managedPromise,
                repoCheckPromise,
                eventKeys,
                eventList = ["onCreate", "postCreate", "onRead", "onUpdate", "postUpdate", "onDelete", "postDelete", "onValidate", "onRetrieve", "onStore", "onSync"],
                propertiesEventList = ["onValidate", "onRetrieve", "onStore"];

            this.data = {
                selectEvents: [],
                addedEvents: [],
                propertiesEventList: propertiesEventList,
                docHelpUrl : constants.DOC_URL
            };

            this.eventHooks = [];
            this.propertyHooks = [];
            this.propertiesCounter = 0;

            managedPromise = ConfigDelegate.readEntity("managed");
            repoCheckPromise = ConfigDelegate.getConfigList();

            $.when(managedPromise, repoCheckPromise, ModuleLoader.load("faiconpicker")).then(_.bind(function(managedObjects, configFiles, faiconpicker){
                this.data.managedObjects = managedObjects;

                if(args.length === 0 || args[0] === null) {
                    this.data.addEditSubmitTitle = $.t("common.form.add");
                    this.data.addEditTitle = $.t("templates.managed.addManagedTitle");
                    this.data.currentManagedObject = {};
                    this.data.addState = true;

                } else {
                    this.data.addEditTitle = $.t("templates.managed.editTitle");
                    this.data.addEditSubmitTitle = $.t("common.form.update");
                    this.data.addState = false;

                    _.each(managedObjects.objects, _.bind(function(managedObject, iterator) {
                        if(managedObject.name === args[0]) {
                            this.data.currentManagedObject = managedObject;
                            this.data.currentManagedObjectIndex = iterator;
                        }
                    }, this));
                }

                eventKeys =
                    _.chain(this.data.currentManagedObject)
                        .keys()
                        .without("name", "properties")
                        .value();

                //Added events are used for the events that are currently set by the managed object
                this.data.addedEvents = _.intersection(eventKeys, eventList);

                //Select events are the events currently available for the select
                this.data.selectEvents = _.difference(eventList, eventKeys);

                _.each(this.data.currentManagedObject.properties, function(property){
                    eventKeys = _.chain(property)
                        .keys()
                        .without("name", "encryption", "scope", "type")
                        .value();

                    property.addedEvents = _.intersection(eventKeys, propertiesEventList);
                    property.selectEvents = _.difference(propertiesEventList, eventKeys);
                }, this);

                this.data.currentRepo = _.find(configFiles[0].configurations, function(file){
                    return file.pid.search("repo.") !== -1;
                }, this).pid;

                if(this.data.currentRepo === "repo.orientdb") {
                    ConfigDelegate.readEntity(this.data.currentRepo).then(_.bind(function (repo) {
                        this.data.repoObject = repo;
                        this.managedRender(callback);
                    }, this));
                } else {
                    this.managedRender(callback);
                }
            }, this));
        },

        loadSchema: function() {
            // Set JSONEditor defaults
            _(JSONEditor.defaults.options).extend({
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
            });

            // This function needs to be re-written for the ability to set the value of oneOf type
            // enumerations.  Before "type" would take a value and never set the title(dropdown)
            // Now the setValue val is an object containing a value and a title.

            // This function needs to be restored for other instances of JSONEditor after load.

            this.data.setValue = _.clone(JSONEditor.defaults.editors.multiple.prototype.setValue);

            JSONEditor.defaults.editors.multiple.prototype.setValue = function(val,initial) {
                if (_.isObject(val) && !_.isNull(val)) {
                    this.switcher.value = val.display;
                    this.type = _.indexOf(this.display_text, val.display);
                    this.switchEditor(this.type);
                    this.editors[this.type].setValue(val.val, initial);
                    this.refreshValue();
                }
            };

            this.data.managedObjectSchema = new JSONEditor(this.$el.find(".schemaEditor")[0], {
                schema: {
                    "title": "Managed Object",
                    "type": "object",
                    "headerTemplate": "{{self.objTitle}}",
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
                                }, {
                                    "title": "Resource Collection",
                                    "type": "object",
                                    "properties": {
                                        "path": {
                                            "type": "string",
                                            "title": "Path"
                                        },
                                        "label": {
                                            "type": "string",
                                            "title": "Label",
                                            "description": $.t("templates.managed.resourceCollectionRelationshipDescription")
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
                                        "type": "string"
                                    },
                                    "title": {
                                        "title": "Readable Title",
                                        "type": "string"
                                    },
                                    "description": {
                                        "title": "Description",
                                        "type": "string",
                                        "format": "textarea"
                                    },
                                    "viewable": {
                                        "title": "Viewable",
                                        "type": "boolean",
                                        "required": true,
                                        "default": true
                                    },
                                    "searchable": {
                                        "title": "Searchable",
                                        "type": "boolean",
                                        "required": true,
                                        "default": false
                                    },
                                    "required": {
                                        "title": "Required",
                                        "type": "boolean",
                                        "required": true,
                                        "default": false
                                    },
                                    "type": {
                                        "title": "Type",
                                        "$ref": "#/definitions/oneOfTypes"
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
                }

            });

            this.data.managedObjectSchema.on("change", _.bind(function () {
                $(".schemaEditor input[type='hidden']").parent().hide();
            }, this));

            this.setDefaultData();

            JSONEditor.defaults.editors.multiple.prototype.setValue = this.data.setValue;
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
                    if (_.has(property, "resourceCollection")) {
                        if (forArray) {
                            tempProperty.itemType = {
                                "display": "Resource Collection",
                                "val": property.resourceCollection
                            };
                        } else {
                            tempProperty.type = {
                                "display": "Resource Collection",
                                "val": property.resourceCollection
                            };
                        }
                    } else {
                        tempProperty.type = {
                            "val": property.type,
                            "display": this.toProperCase(property.type)
                        };
                    }
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
            }

            return tempProperty;
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

                case "Resource Collection":
                    property.resourceCollection = value;
                    property.type = "string";
                    break;
            }
        },

        getObjectProperties: function(props, node) {
            var data = {
                "properties": {},
                "required": [],
                "order": []
            };
            _.each(props, function(property, index) {
                data.properties[property.propertyName] = {
                    "description": property.description,
                    "title": property.title,
                    "viewable": property.viewable,
                    "searchable": property.searchable
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
                "description": this.data.managedObjectSchema.getValue().description
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
                    workflowContext: _.pluck(this.data.managedObjectSchema.getValue().properties, "propertyName")                });

                _.each(this.$el.find("#managedPropertyWrapper .small-field-block:visible"), function(managedProperty, index) {
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

                if (callback) {
                    callback();
                }

            }, this));
        },

        formSubmit: function(event) {
            event.preventDefault();

            var managedObject = this.setManagedObject(form2js('managedForm2JS', '.', true)),
                nameCheck;

            managedObject.schema = this.getManagedSchema();

            managedObject.schema.icon = this.$el.find("#managedObjectIcon").val();

            if(this.data.addState) {
                nameCheck = this.checkManagedName(managedObject.name);

                if(!nameCheck) {
                    this.data.managedObjects.objects.push(managedObject);

                    this.saveManagedObject(managedObject);
                } else {
                    this.$el.find("#managedErrorMessage .message").html($.t("templates.managed.duplicateNameError"));
                    this.$el.find("#managedErrorMessage").show();
                    this.$el.find("#addEditManaged").prop("disabled", true);
                }
            } else {
                if (managedObject.name === this.data.managedObjects.objects[this.data.currentManagedObjectIndex].name) {
                    this.data.managedObjects.objects[this.data.currentManagedObjectIndex] = managedObject;

                    this.saveManagedObject(managedObject);
                } else {
                    nameCheck = this.checkManagedName(managedObject.name);

                    if(!nameCheck) {
                        this.data.managedObjects.objects[this.data.currentManagedObjectIndex] = managedObject;

                        this.saveManagedObject(managedObject);
                    } else {
                        this.$el.find("#managedErrorMessage .message").html($.t("templates.managed.duplicateNameError"));
                        this.$el.find("#managedErrorMessage").show();
                        this.$el.find("#addEditManaged").prop("disabled", true);
                    }
                }
            }
        },

        checkManagedName: function(name) {
            var currentObjects = this.data.managedObjects.objects,
                found = false;

            _.each(currentObjects, function(managedObject){
                if(managedObject.name === name){
                    found = true;
                }
            }, this);

            return found;
        },

        saveManagedObject: function(managedObject) {
            var promises = [];

            if(managedObject.properties.length === 0) {
                delete managedObject.properties;
            }

            promises.push(ConfigDelegate.updateEntity("managed", {"objects" : this.data.managedObjects.objects}));

            if(this.data.currentRepo === "repo.orientdb") {
                this.orientRepoChange(managedObject);
                promises.push(ConfigDelegate.updateEntity(this.data.currentRepo, this.data.repoObject));
            }

            $.when.apply($, promises).then(function() {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "managedObjectSaveSuccess");

                _.delay(function () {
                    eventManager.sendEvent(constants.EVENT_UPDATE_NAVIGATION);
                    eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.managedListView});
                }, 1500);
            });
        },

        deleteManaged: function(event) {
            event.preventDefault();

            var promises = [];

            uiUtils.jqConfirm($.t("templates.managed.managedDelete"), _.bind(function(){
                _.each(this.data.managedObjects.objects, function(managedObject, index){
                    if(managedObject.name === this.data.currentManagedObject.name) {
                        this.data.managedObjects.objects.splice(index, 1);
                    }
                }, this);

                if(this.data.currentRepo === "repo.orientdb") {
                    if(this.data.repoObject.dbStructure.orientdbClass["managed_"+this.data.currentManagedObject.name] !== undefined){
                        delete this.data.repoObject.dbStructure.orientdbClass["managed_"+this.data.currentManagedObject.name];
                    }

                    promises.push(ConfigDelegate.updateEntity(this.data.currentRepo, this.data.repoObject));
                }

                promises.push(ConfigDelegate.updateEntity("managed", {"objects" : this.data.managedObjects.objects}));

                $.when.apply($, promises).then(function(){
                        eventManager.sendEvent(constants.EVENT_UPDATE_NAVIGATION);
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteManagedSuccess");

                        eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.managedListView});
                    },
                    function(){
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteManagedFail");
                    });

            },this));
        },

        setManagedObject: function(managedObject) {
            _.extend(managedObject, this.model.managedScripts.getScripts());

            if(managedObject.properties !== undefined) {
                _.each(managedObject.properties, function (prop, index) {
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

                    _.extend(prop, this.model.propertyScripts[index].getScripts());
                }, this);
            } else {
                managedObject.properties = [];
            }

            return managedObject;
        },

        addProperty: function(event) {
            event.preventDefault();

            var checkboxes,
                field,
                input,
                propIndex;

            field = this.$el.find("#managed-object-hidden-property").clone();
            field.removeAttr("id");
            input = field.find('input[type=text]');
            input.val("");
            input.attr("data-validator-event","keyup blur");
            input.attr("data-validator","required");

            checkboxes = field.find(".checkbox");

            this.propertiesCounter = this.propertiesCounter + 1;

            input.prop( "name", "properties[" +this.propertiesCounter  +"].name");
            $(checkboxes[0]).prop( "name", "properties[" +this.propertiesCounter  +"].encryption");
            $(checkboxes[1]).prop( "name", "properties[" +this.propertiesCounter  +"].scope");
            $(checkboxes[2]).prop( "name", "properties[" +this.propertiesCounter  +"].type");

            field.show();

            this.propertyHooks.push([]);

            propIndex = this.propertyHooks.length - 1;

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

            validatorsManager.bindValidators(this.$el.find('#managedPropertyWrapper'));
            validatorsManager.validateAllFields(this.$el.find('#managedPropertyWrapper'));
        },

        removeProperty: function(event) {
            event.preventDefault();

            uiUtils.jqConfirm($.t("templates.managed.propertyDelete"), _.bind(function() {
                var clickedEle = event.target,
                    targetIndex;

                clickedEle = $(clickedEle).parents(".managed-property");
                targetIndex = this.$el.find(".managed-property:visible").index(clickedEle);

                this.propertyHooks.splice(targetIndex, 1);
                this.model.propertyScripts.splice(targetIndex, 1);

                clickedEle.remove();

                validatorsManager.bindValidators(this.$el.find('#managedPropertyWrapper'));
                validatorsManager.validateAllFields(this.$el);
            },this), "310px");
        },

        orientRepoChange: function(managedObject) {
            var orientClasses = this.data.repoObject.dbStructure.orientdbClass;

            if(orientClasses["managed_" +managedObject.name] === undefined) {
                orientClasses["managed_" +managedObject.name] = {
                    "index" : [
                        {
                            "propertyName" : "_openidm_id",
                            "propertyType" : "string",
                            "indexType" : "unique"
                        }
                    ]
                };
            }
        }
    });

    return new AddEditManagedView();
});
