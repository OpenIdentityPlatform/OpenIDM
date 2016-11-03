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
 * Copyright 2014-2016 ForgeRock AS.
 */

define([
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
    "org/forgerock/openidm/ui/admin/delegates/RepoDelegate",
    "org/forgerock/openidm/ui/admin/util/ScriptList",
    "org/forgerock/commons/ui/common/util/ModuleLoader",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "faiconpicker",
    "bootstrap-tabdrop",
    "org/forgerock/openidm/ui/admin/managed/schema/SchemaEditorView"
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
            RepoDelegate,
            ScriptList,
            ModuleLoader,
            UIUtils,
            faiconpicker,
            tabdrop,
            SchemaEditorView) {

    var EditManagedView = AbstractManagedView.extend({
        template: "templates/admin/managed/EditManagedTemplate.html",
        events: {
            "submit #managedObjectDetailsForm" : "saveManagedDetails",
            "submit #managedObjectScriptsForm" : "saveManagedScripts",
            "submit #managedObjectPropertiesForm" : "saveManagedProperties",
            "click #saveManagedPreferences" : "saveManagedPreferences",
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
            var eventKeys,
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

            $.when(
                ConfigDelegate.readEntity("managed"),
                RepoDelegate.findRepoConfig()
            ).then(_.bind(function(managedObjects, repoConfig){
                this.data.managedObjects = managedObjects;
                this.data.repoConfig = repoConfig;

                _.each(managedObjects.objects, _.bind(function(managedObject, iterator) {
                    if(managedObject.name === args[0]) {
                        this.data.currentManagedObject = managedObject;
                        this.data.currentManagedObjectIndex = iterator;
                        this.splitSchemaAndProperties();

                        if(this.data.currentManagedObject.schema) {
                            this.data.noSchema = _.isEmpty(this.data.currentManagedObject.schema.properties);
                        } else {
                            this.data.noSchema = true;
                        }
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

                this.managedRender(callback);

            }, this));
        },

        managedRender: function(callback) {
            this.parentRender(_.bind(function () {
                validatorsManager.bindValidators(this.$el);
                validatorsManager.validateAllFields(this.$el);

                this.data.preferencesSchema = new JSONEditor(this.$el.find("#managedPreferencesWrapper")[0], _.extend({
                    schema: {
                        "type": "array",
                        "format": "table",
                        "title": "Preferences",
                        "items": {
                            "type": "object",
                            "title": "Preference",
                            "properties": {
                                "key": {
                                    "type": "string"
                                },
                                "description": {
                                    "type": "string"
                                }
                            }
                        }
                    }
                }, {
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
                } ));

                let formattedProps = _.map(_.get(this.data.currentManagedObject, "schema.properties.preferences.properties"), (property, key) => {
                    return {
                        "key": key,
                        "description": property.description
                    };
                });

                this.data.preferencesSchema.setValue(formattedProps || []);

                this.propertiesCounter = this.$el.find(".add-remove-block").length;

                SchemaEditorView.render([this], () => {
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
                        workflowContext: _.pluck(SchemaEditorView.data.managedObjectSchema.getValue().properties, "propertyName")
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
                            workflowContext: _.pluck(SchemaEditorView.data.managedObjectSchema.getValue().properties, "propertyName")
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
                });

            }, this));
        },

        saveManagedDetails: function(event) {
            event.preventDefault();

            var data = form2js('managedForm2JS', '.', true),
                nameCheck;

            nameCheck = this.checkManagedName(data.name, this.data.managedObjects.objects);

            if(!nameCheck || data.name === this.data.currentManagedObject.name) {

                if(_.isUndefined(this.data.currentManagedObject.schema)) {
                    this.data.currentManagedObject.schema = {};
                }

                this.data.currentManagedObject.name = data.name;
                this.data.currentManagedObject.schema.icon = this.$el.find("#managedObjectIcon").val();
                this.$el.find("#managedErrorMessage").hide();

                this.saveManagedObject(this.data.currentManagedObject, this.data.managedObjects);
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

            this.data.currentManagedObject.schema = SchemaEditorView.getManagedSchema();

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

            this.saveManagedObject(this.data.currentManagedObject, this.data.managedObjects);
        },

        getManagedPreferences: function() {
            var preferences = {};

            _.each(this.data.preferencesSchema.getValue(), (item) => {
                if (item.key.length > 0 && item.description.length > 0) {
                    preferences[item.key] = {
                        "description": item.description,
                        "type": "boolean"
                    };
                }
            });

            return preferences;
        },

        saveManagedPreferences: function(event) {
            this.saveManagedObject(this.data.currentManagedObject, this.data.managedObjects);
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

            this.saveManagedObject(this.data.currentManagedObject, this.data.managedObjects);
        },

        deleteManaged: function(event) {
            event.preventDefault();

            var promises = [];

            UIUtils.confirmDialog($.t("templates.managed.managedDelete"), "danger", _.bind(function(){
                this.data.managedObjects.objects = _.reject(this.data.managedObjects.objects, function(managedObject){
                    return managedObject.name === this.data.currentManagedObject.name;
                }, this);

                promises.push(ConfigDelegate.updateEntity("managed", {"objects" : this.data.managedObjects.objects}));
                promises.push(RepoDelegate.deleteManagedObject(this.data.repoConfig, this.data.currentManagedObject.name));

                $.when.apply($, promises).then(
                    function(){
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
