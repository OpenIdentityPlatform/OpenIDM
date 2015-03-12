/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All rights reserved.
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

/*global define, $, _, require, JSONEditor, window */

define("org/forgerock/openidm/ui/admin/objectTypes/ObjectTypesDialog", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate",
    "bootstrap-dialog"
], function(AbstractView, validatorsManager, conf, eventManager, constants, uiUtils, ConnectorDelegate, BootstrapDialog) {
    var ObjectTypesDialog = AbstractView.extend({
        template: "templates/admin/objectTypes/ObjectTypesTemplate.html",
        el: "#dialogs",
        editor: null,
        objectTypes: {},
        events: {
            "click #newObjectType": "addNewObjectType",
            "change #objectTypesList": "switchObjectType",
            "change #selectObjectConfig" : "changeObjectTypeConfig",
            "click #deleteMultiple": "deleteObjectType",
            "click #debug": "loadObjectTypeData",
            "click .deleteObject": "deleteObjectType",
            "click .saveObject": "saveObjectType"
        },
        currentObjectTypeLoaded: "savedConfig",
        objectTypeConfigs: {
            "org.forgerock.openicf.connectors.ldap-connector" : [
                {
                    "displayName" : "Saved Configuration",
                    "fileName" : "savedConfig",
                    "type": "ldap"
                },
                {
                    "displayName" : "AD LDAP Configuration",
                    "fileName" : "provisioner.openicf-adldap",
                    "type": "ldap"
                },
                {
                    "displayName" : "ADLDS LDAP Configuration",
                    "fileName" : "provisioner.openicf-adldsldap",
                    "type": "ldap"
                },
                {
                    "displayName" : "DJ LDAP Configuration",
                    "fileName" : "provisioner.openicf-ldap",
                    "type": "ldap"
                },
                {
                    "displayName" : "Full LDAP Configuration",
                    "fileName" : "fullConfig",
                    "type": "ldap"
                },
                {
                    "displayName" : "IBM LDAP Configuration",
                    "fileName" : "provisioner.openicf-racfldap",
                    "type": "ldap"
                }
            ]
        },
        data: {

        },

        changeObjectTypeConfig: function(event) {
            var value = $(event.target).val(),
                type = $(event.target).attr("data-type");

            $(event.target).val(this.currentObjectTypeLoaded);

            uiUtils.jqConfirm($.t('templates.connector.objectTypes.changeConfiguration'), _.bind(function(){
                if(this.editor) {
                    this.editor.destroy();
                }
                this.editor = null;

                this.$el.find("#objectTypeButtons").hide();
                this.$el.find("#objectTypesList").empty();

                if(value === "fullConfig") {
                    this.connectorDetails.configurationProperties.readSchema = true;

                    ConnectorDelegate.testConnector(this.connectorDetails).then(_.bind(function (result) {
                            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "objectTypeLoaded");

                            this.loadObjectTypeData(result.objectTypes);
                            this.$el.find("#objectTypeButtons").show();
                        }, this), _.bind(function () {
                            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "objectTypeFailedToLoad");
                        }, this)
                    );
                } else if(value === "savedConfig") {
                    this.loadObjectTypeData(this.connectorDetails.objectTypes);
                } else {
                    ConnectorDelegate.connectorDefault(value, type).then(_.bind(function (result) {
                            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "objectTypeLoaded");

                            this.loadObjectTypeData(result.objectTypes);
                            this.$el.find("#objectTypeButtons").show();
                        }, this), _.bind(function () {
                            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "objectTypeFailedToLoad");
                        }, this)
                    );
                }

                this.currentObjectTypeLoaded = value;
                $(event.target).val(this.currentObjectTypeLoaded);

            }, this), "330px");
        },

        render: function(defaultObjectType, connectorDetails, callback) {
            var _this = this,
                btns = {};

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

            $('#dialogs').append(this.currentDialog);

            this.data.defaultConfigs = this.objectTypeConfigs[connectorDetails.connectorRef.bundleName];

            //change dialog
            BootstrapDialog.show({
                title: $.t('templates.connector.objectTypes.objectTypeGenerator'),
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
         * When the option in the object type selector changes, if multiple items are
         * selected show the delete multiple items form, otherwise convert that key to JE format and display it.
         */
        switchObjectType: function() {
            var selected = this.$el.find("#objectTypesList :selected"),
                JE_format;

            if (selected.length > 1) {
                this.$el.find("#objectType").hide();
                this.$el.find("#multiSelectOptions").show();

            } else if (selected.length === 1) {
                this.$el.find("#objectType").show();
                this.$el.find("#multiSelectOptions").hide();

                JE_format = this.get_JE_format(selected[0].value);

                this.addNewObjectType(false);

                if (JE_format) {
                    this.editor.setValue(JE_format);
                }
            }
        },

        /**
         *  Saves the existing objectType if there is one, destroys and recreates the editor.
         *
         *  Adds buttons and binds functions for deleting and saving the object
         */
        addNewObjectType: function(removeSelection) {
            if (removeSelection) {
                this.$el.find("#objectTypesList option:selected").removeAttr("selected");
            }

            if (this.editor && !this.editor.destroyed) {
                this.saveObjectType(false);
                this.editor.destroy();
            }

            this.newObjectType();
            this.$el.find("#objectType h3 .json-editor-btn-edit:contains('Object Properties')").after($("#objectTypeButtons").html());
        },

        /**
         *  Remove the current object
         */
        deleteObjectType: function() {
            if (this.editor) {
                var toRemove = _.chain(this.$el.find("#objectTypesList :selected"))
                    .map(function(el) {
                        return el.value;
                    })
                    .sortBy(function(key) {
                        return key.toLowerCase();
                    })
                    .value();

                this.objectTypes = _.omit(this.objectTypes, toRemove);
                this.editor.destroy();
                this.updateObjectTypeSelector();

                this.$el.find("#objectTypesList").val(this.$el.find("#objectTypesList option:first").val()).change();
                this.$el.find("#objectType").show();
                this.$el.find("#multiSelectOptions").hide();
            }
        },

        /**
         * Saves the current object type
         * @param select {boolean}  Used after saving to determine is the object should be 'selected'
         */
        saveObjectType: function(select) {
            var objectType = this.editor.getValue();

            if (this.editor && !_(objectType.objectName).isEmpty()) {
                this.objectTypes[objectType.objectName] = this.get_OT_format(objectType);
                this.updateObjectTypeSelector();

                if (select) {
                    this.$el.find("#objectTypesList").val(objectType.objectName);
                }
            }
        },

        /**
         *  Updates the values in the select after one has been added or removed
         */
        updateObjectTypeSelector: function(replaceAll) {
            var keys = _.sortBy(_.keys(this.objectTypes), function(key){ return key.toLowerCase(); }),
                select = this.$el.find("#objectTypesList"),
                existingOptions =  select.find("option"),
                prev = null,
                current,
                _this = this;

            // Remove from the select any options that were deleted from the objectType
            _(existingOptions).each(function(option) {
                if (!_.contains(keys, option.value)) {
                    $(option).remove();

                }
            });

            if (replaceAll === true) {
                select.empty();
                _(keys).each(function(key) {
                    select.append("<option value="+key+">"+key+"</option>");
                });

                select.val(select.find("option:first").val()).change();

            } else {
                // Add an option to the select if it is not there
                _(keys).each(function(key) {
                    current = select.find("option[value='"+ key+"']");
                    if (current.length === 0) {
                        if (prev === null) {
                            select.prepend("<option value="+key+">"+key+"</option>");
                        } else {
                            prev.after("<option value="+key+">"+key+"</option>");
                        }
                    }
                    prev = current;
                });
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

            _(objectType.properties).each(function(property){
                OT_value.properties[property.propertyName] = {
                    type: property.propertyType,
                    nativeType: property.nativeType,
                    nativeName: property.nativeName,
                    required: property.required
                };

                if (property.customProperties && property.customProperties.length > 0) {
                    _(property.customProperties).each(function(customProperty){
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
                _(this.objectTypes[key].properties).each(function(property, key) {
                    tempProperty = {};
                    tempProperty.propertyName = key;
                    tempProperty.propertyType = property.type;
                    tempProperty.nativeName = property.nativeName;
                    tempProperty.nativeType = property.nativeType;
                    tempProperty.required = property.required || false;
                    tempProperty.customProperties = [];

                    // Any additional properties are added to the customProperties object
                    _(_.omit(property,["type", "nativeName", "nativeType", "required"])).each(function(customProperty, key){
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
            this.objectTypes = objectTypes;
            this.updateObjectTypeSelector(true);
        }
    });

    return new ObjectTypesDialog();
});