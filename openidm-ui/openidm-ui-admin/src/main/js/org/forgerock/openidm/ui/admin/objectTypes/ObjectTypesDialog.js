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
 * Copyright 2011-2016 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "jsonEditor",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate",
    "bootstrap-dialog"
], function($, _, JSONEditor, AbstractView, validatorsManager, conf, eventManager, constants, uiUtils, ConnectorDelegate, BootstrapDialog) {
    var ObjectTypesDialog = AbstractView.extend({
        template: "templates/admin/objectTypes/ObjectTypesTemplate.html",
        el: "#dialogs",
        editor: null,
        objectTypes: {},
        currentObjectTypeLoaded: "savedConfig",
        data: {

        },

        render: function(defaultObjectType, selectedObjectType, connectorDetails, callback) {
            var _this = this;

            JSONEditor.defaults.options.disable_edit_json = true;
            JSONEditor.defaults.options.disable_array_reorder = true;
            JSONEditor.defaults.options.disable_collapse = true;
            JSONEditor.defaults.options.show_errors = "never";
            JSONEditor.defaults.options.template = 'handlebars';
            JSONEditor.defaults.options.theme = 'bootstrap3';
            JSONEditor.defaults.options.iconlib = 'fontawesome4';

            this.currentObjectTypeLoaded = "savedConfig";
            this.defaultObjectType = $.extend(true, {}, defaultObjectType);
            this.callback = callback;
            this.currentDialog = $('<div id="objectTypesForm"></div>');
            this.setElement(this.currentDialog);
            this.connectorDetails = connectorDetails;
            this.objectTypes = {};
            this.editor = null;
            this.selectedObjectType = selectedObjectType;
            this.title = "New Object Type";

            $('#dialogs').append(this.currentDialog);

            if(this.selectedObjectType !== null) {
                this.title = this.selectedObjectType +" Object Type Edit";
            }

            //change dialog
            this.dialog = BootstrapDialog.show({
                title: this.title,
                type: BootstrapDialog.TYPE_DEFAULT,
                message: this.currentDialog,
                size: BootstrapDialog.SIZE_WIDE,
                cssClass : "objecttype-window",
                onshown : _.bind(function (dialogRef) {
                    uiUtils.renderTemplate(
                        _this.template,
                        _this.$el,
                        _.extend({}, conf.globalData, this.data),
                        _.bind(function(){
                            if (_.size(this.defaultObjectType) > 0) {
                                this.loadObjectTypeData(this.defaultObjectType);
                            }
                        }, _this),
                        "replace"
                    );
                }, _this),
                buttons: [{
                    label: $.t('common.form.close'),
                    action: function(dialogRef){
                        dialogRef.close();
                    }
                },
                    {
                        label: $.t('templates.connector.objectTypes.saveObjectType'),
                        cssClass: "btn-primary",
                        id: "saveObjectTypeDialog",
                        action: function(dialogRef) {
                            if (callback) {
                                _this.saveObjectType();
                                _this.editor.destroy();

                                dialogRef.close();

                                callback(_this.objectTypes);
                            }

                            dialogRef.close();
                        }
                    }]
            });
        },

        /**
         * Saves the current object type
         * @param select {boolean}  Used after saving to determine is the object should be 'selected'
         */
        saveObjectType: function() {
            var objectType = this.editor.getValue();

            if (this.editor && !_.isEmpty(objectType.objectName)) {

                if(this.selectedObjectType !== null) {
                    delete this.objectTypes[this.selectedObjectType];
                }

                this.objectTypes[objectType.objectName] = this.get_OT_format(objectType);
            }
        },

        /**
         *   Creates a blank JSON-Editor form for an Object Type
         */
        newObjectType: function() {
            this.editor = new JSONEditor(document.getElementById('objectType'), {
                "schema": {
                    "title": "Object Type",
                    "headerTemplate": "{{self.objectName}}",
                    "format": "grid",
                    "type": "object",
                    "properties": {
                        "objectName": {
                            "title": "Object Name",
                            "type": "string"
                        },
                        "$schema": {
                            "title": "Schema",
                            "type": "string",
                            "default": "http://json-schema.org/draft-04/schema"
                        },
                        "id": {
                            "title": "ID",
                            "type": "string"
                        },
                        "type": {
                            "title": "Type",
                            "type": "string",
                            "enum": ["string", "object", "array", "number", "boolean", "null", "undefined"],
                            "required": true,
                            "default": "object"
                        },
                        "nativeType": {
                            "title": "Native Type",
                            "type": "string"
                        },
                        "properties": {
                            "type": "array",
                            "format": "tabs",
                            "title": "Object Type Properties",
                            "items": {
                                "type": "object",
                                "title": "Property", "format": "grid",
                                "headerTemplate": "{{self.propertyName}}",
                                "properties": {
                                    "propertyName": {
                                        "title": "Property Name",
                                        "type": "string"
                                    },
                                    "propertyType": {
                                        "title": "Type",
                                        "type": "string",
                                        "enum": ["string", "object", "array", "number", "boolean", "null", "undefined"],
                                        "required": true,
                                        "default": "string"
                                    },
                                    "nativeName": {
                                        "title": "Native Name",
                                        "type": "string"
                                    },
                                    "nativeType": {
                                        "title": "Native Type",
                                        "type": "string",
                                        "enum": ["string", "object", "array", "number", "boolean", "null", "undefined", "JAVA_TYPE_GUARDEDSTRING"],
                                        "required": true,
                                        "default": "string"
                                    },
                                    "required": {
                                        "title": "Required",
                                        "type": "boolean",
                                        "required": true
                                    },
                                    "customProperties": {
                                        "title": "Custom Properties",
                                        "type": "array",
                                        "format": "tabs",
                                        "items": {
                                            "$ref": "#/definitions/objectValue"
                                        }
                                    }

                                }
                            }
                        }
                    },

                    "definitions": {
                        "objectValue": {
                            "type": "object",
                            "title": "Custom Property",
                            "headerTemplate": "{{self.propertyName}}",
                            "properties": {
                                "propertyName": {
                                    "title": "Property Name",
                                    "type": "string"
                                },
                                "values": {
                                    "title": "Property Value",
                                    "description": "Below, when adding an value type of object you must click on 'Object Properties' button to specify key names"
                                }
                            }
                        }
                    }
                }
            });
        },

        /**
         * Converts a provided object from JSON-Editor structure to an ObjectType format
         */
        get_OT_format: function(objectType) {
            var OT_value = _.omit(objectType,["objectName", "properties"]);

            OT_value.properties = {};

            _.each(objectType.properties, function(property){
                OT_value.properties[property.propertyName] = {
                    type: property.propertyType,
                    nativeType: property.nativeType,
                    nativeName: property.nativeName,
                    required: property.required
                };

                if (property.customProperties && property.customProperties.length > 0) {
                    _.each(property.customProperties, function(customProperty){
                        OT_value.properties[property.propertyName][customProperty.propertyName] = customProperty.values;
                    });
                }
            });

            return OT_value;
        },


        /**
         *  Load the given objectType
         */
        get_JE_format: function(key) {
            var jsonEditorFormat = {},
                properties = [],
                customProperties = [],
                tempProperty = {};

            if (this.objectTypes[key]) {
                // Root level properties
                jsonEditorFormat.objectName = key;
                jsonEditorFormat.$schema = this.objectTypes[key].$schema;
                jsonEditorFormat.id = this.objectTypes[key].id;
                jsonEditorFormat.type = this.objectTypes[key].type;
                jsonEditorFormat.nativeType = this.objectTypes[key].nativeType;

                // Properties of Object Types
                _.each(this.objectTypes[key].properties, function(property, key) {
                    tempProperty = {};
                    tempProperty.propertyName = key;
                    tempProperty.propertyType = property.type;
                    tempProperty.nativeName = property.nativeName;
                    tempProperty.nativeType = property.nativeType;
                    tempProperty.required = property.required || false;
                    tempProperty.customProperties = [];

                    // Any additional properties are added to the customProperties object
                    _.each(_.omit(property,["type", "nativeName", "nativeType", "required"]), function(customProperty, key){
                        customProperties.push({
                            "propertyName": key,
                            "values": customProperty
                        });
                    });

                    tempProperty.customProperties = customProperties;
                    customProperties = [];

                    properties.push(tempProperty);
                });
                jsonEditorFormat.properties = properties;
            }
            return jsonEditorFormat || false;
        },

        loadObjectTypeData: function(objectTypes) {
            var JE_format;

            this.objectTypes = objectTypes;

            JE_format = this.get_JE_format(this.selectedObjectType);

            this.newObjectType();

            if (JE_format && this.selectedObjectType !== null) {
                this.editor.setValue(JE_format);
            }
        }
    });

    return new ObjectTypesDialog();
});
