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
    "form2js",
    "jsonEditor",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/admin/util/AdminUtils",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate",
    "org/forgerock/openidm/ui/common/resource/RelationshipArrayView",
    "selectize"
], function($, _, handlebars,
            form2js,
            JSONEditor,
            AdminAbstractView,
            ValidatorsManager,
            ConfigDelegate,
            ResourceDelegate,
            ConnectorDelegate,
            EventManager,
            Constants,
            InlineScriptEditor,
            UIUtils,
            AdminUtils,
            resourceDelegate,
            RelationshipArrayView,
            selectize) {
    var EditAssignmentView = AdminAbstractView.extend({
        template: "templates/admin/assignment/EditAssignmentViewTemplate.html",
        element: "#assignmentHolder",
        events: {
            "click #saveAssignmentsDetails" :"saveAssignmentDetails",
            "click #saveAssignmentScripts" : "saveAssignmentScripts",
            "click #saveAssignmentAttributes" : "saveAssignmentAttribute",
            "click #deleteAssignment" : "deleteAssignment",
            "click #addAttribute": "eventAddAttribute",
            "click .delete-attribute": "deleteAttribute",
            "click .save-operations" : "saveOperations",
            "change .select-attribute" : "changeSchema",
            "onValidate": "onValidate"
        },
        partials: [
            "partials/assignment/_AssignmentAttribute.html",
            "partials/assignment/_OperationsPopover.html",
            "partials/assignment/_LDAPGroup.html"
        ],
        data: {
            showLinkQualifier: false,
            displayLinkQualifiers: [],
            assignmentAttributes: []
        },
        model: {

        },
        render: function(args, callback) {
            var resourcePromise,
                configPromise = ConfigDelegate.readEntity("sync"),
                systemType,
                connectorUrl;

            this.model.serviceUrl = ResourceDelegate.getServiceUrl(args);
            this.model.args = args;
            this.model.schemaEditors = [];
            this.model.assignmentAttributes = [];

            resourcePromise = ResourceDelegate.readResource(this.model.serviceUrl, args[2]);

            $.when(configPromise, resourcePromise).then(_.bind(function(sync, resource) {
                this.data.resource = resource[0];

                this.data.mapping = _.find(sync.mappings, function(mapping) {
                    return this.data.resource.mapping === mapping.name;
                }, this);

                if(this.data.mapping.linkQualifiers) {
                    this.data.showLinkQualifier = true;
                }

                systemType = this.data.mapping.target.split("/");


                AdminUtils.findPropertiesList(systemType).then(_.bind(function(properties, connector){
                    this.data.resourceSchema = properties;

                    this.data.resourcePropertiesList = _.chain(properties).keys().sortBy().value();

                    if(connector) {
                        this.model.connector = connector;
                    }

                    this.attributeRender(callback);
                }, this));
            }, this));
        },

        attributeRender: function(callback) {
            this.parentRender(_.bind(function() {
                ValidatorsManager.bindValidators(this.$el.find("#assignmentDetailsForm"));
                ValidatorsManager.validateAllFields(this.$el);

                if(this.data.showLinkQualifier) {
                    this.model.linkQualifierSelectize = this.$el.find('#linkQualifiers').selectize({
                        persist: false,
                        create: false,
                        maxItems: null
                    });

                    this.model.linkQualifierSelectize[0].selectize.clear();

                    if(this.data.resource.linkQualifiers) {
                        this.model.linkQualifierSelectize[0].selectize.setValue(this.data.resource.linkQualifiers);
                    }
                }

                this.model.onAssignment = InlineScriptEditor.generateScriptEditor({
                    "element": this.$el.find("#onAssignmentHolder"),
                    "eventName": "onAssignment",
                    "noValidation": true,
                    "scriptData": this.data.resource.onAssignment,
                    "disablePassedVariable": false
                });

                this.model.unAssignment = InlineScriptEditor.generateScriptEditor({
                    "element": this.$el.find("#onUnassignmentHolder"),
                    "eventName": "onUnassignment",
                    "noValidation": true,
                    "scriptData": this.data.resource.unAssignment,
                    "disablePassedVariable": false
                });

                this.$el.find("#assignmentEventsTab").on("shown.bs.tab", _.bind(function (e) {
                    this.model.onAssignment.refresh();
                    this.model.unAssignment.refresh();
                }, this));

                _.each(this.data.resource.attributes, _.bind(function(attribute) {
                    this.addAttribute(attribute);
                }, this));

                this.showRolesTab();

                if(callback) {
                    callback();
                }
            },this));
        },

        changeSchema: function(event) {
            var container = $(event.target).parents(".list-group-item"),
                value = $(event.target).val(),
                ldapGroup = null,
                index = this.$el.find("#assignmentAttributesList .list-group-item").index(container);

            if(value !== "ldapGroups") {
                this.model.schemaEditors[index] = this.createJSONEditor($(event.target));
            } else {
                _.each(this.model.connector.objectTypes, function(objectType, key) {
                    if(objectType.id === "__GROUP__") {
                        ldapGroup = key;
                    }
                });

                if(ldapGroup) {
                    ConnectorDelegate.queryConnector(this.model.connector.name +"/" +ldapGroup).then(_.bind(function(groups){
                        container.find(".attribute-value").empty();
                        container.find(".attribute-value").append($(handlebars.compile("{{> assignment/_LDAPGroup}}")(groups)));

                        this.model.schemaEditors[index] = container.find(".ldap-group-select").selectize({
                            persist: false,
                            create: false,
                            maxItems: null
                        });

                        container.find(".ldap-group-select")[0].selectize.clear();
                    }, this));
                } else {
                    this.model.schemaEditors[index] = this.createJSONEditor($(event.target));
                }
            }
        },

        eventAddAttribute: function(event) {
            event.preventDefault();

            this.addAttribute(null);
        },

        addAttribute: function(attribute) {
            var newAttr =  $(handlebars.compile("{{> assignment/_AssignmentAttribute}}")(this.data)),
                defaultAttribute = {
                    "assignmentOperation": "mergeWithTarget",
                    "name": "",
                    "unassignmentOperation": "removeFromTarget",
                    "value": ""
                },
                currentAttribute,
                ldapGroup,
                tempIndex;


            if(attribute) {
                this.model.assignmentAttributes.push(attribute);
                currentAttribute = attribute.value;
                newAttr.find(".select-attribute").val(attribute.name);
            } else {
                this.model.assignmentAttributes.push(defaultAttribute);
                currentAttribute = defaultAttribute.value;
            }

            this.$el.find("#assignmentAttributesList").append(newAttr);

            if(attribute && attribute.name === "ldapGroups") {
                _.each(this.model.connector.objectTypes, function(objectType, key) {
                    if(objectType.id === "__GROUP__") {
                        ldapGroup = key;
                    }
                });

                if(ldapGroup) {
                    tempIndex = this.model.schemaEditors.length;

                    this.model.schemaEditors.push({});

                    ConnectorDelegate.queryConnector(this.model.connector.name +"/" +ldapGroup).then(_.bind(function(groups){
                        newAttr.find(".attribute-value").empty();
                        newAttr.find(".attribute-value").append($(handlebars.compile("{{> assignment/_LDAPGroup}}")(groups)));

                        this.model.schemaEditors[tempIndex] = newAttr.find(".ldap-group-select").selectize({
                            persist: false,
                            create: false,
                            maxItems: null
                        });

                        newAttr.find(".ldap-group-select")[0].selectize.clear();
                        newAttr.find(".ldap-group-select")[0].selectize.setValue(currentAttribute);
                    }, this));
                } else {
                    this.model.schemaEditors.push(this.createJSONEditor(newAttr.find(".select-attribute"), currentAttribute));
                }
            } else {
                this.model.schemaEditors.push(this.createJSONEditor(newAttr.find(".select-attribute"), currentAttribute));
            }

            this.setPopover(newAttr.find(".btn-toggle-attribute-operations"));
        },

        createJSONEditor: function (element, jsonEditorValue) {
            var container = element.parents(".list-group-item"),
                value = element.val(),
                schema = {
                    "type" : this.data.resourceSchema[value].type
                },
                editor;

            if (schema.type === "relationship") {
                schema.type = "object";

                if (!_.isObject(schema.properties)) {
                    schema.properties = {};
                }

                schema.properties._ref = {
                    "type": "string"
                };
            }

            container.find(".attribute-value").empty();

            editor = new JSONEditor(container.find(".attribute-value")[0], {
                disable_array_reorder: true,
                disable_collapse: true,
                disable_edit_json: true,
                disable_properties: false,
                iconlib: "fontawesome4",
                no_additional_properties: false,
                theme: "bootstrap3",
                schema: schema
            });

            editor.on('change', _.bind(function () {
                this.$el.find(".compactJSON div.form-control>:input").addClass("form-control");
            }, this));

            if (jsonEditorValue) {
                editor.setValue(jsonEditorValue);
            }

            return editor;
        },

        setPopover: function(button) {
            var container = $(button).parents(".list-group-item"),
                index = this.$el.find("#assignmentAttributesList .list-group-item").index(container),
                attributeDetails = this.model.assignmentAttributes[index];

            $(button).popover({
                trigger: 'click',
                placement:'bottom',
                html: true,
                content:  $(handlebars.compile("{{> assignment/_OperationsPopover}}")(attributeDetails))
            });
        },

        saveOperations : function(event) {
            event.preventDefault();

            var container = $(event.target).parents(".list-group-item"),
                button = $(container).find(".btn-toggle-attribute-operations"),
                index = this.$el.find("#assignmentAttributesList .list-group-item").index(container),
                attributeDetails = this.model.assignmentAttributes[index];

            attributeDetails.assignmentOperation = container.find(".onAssignment-select").val();
            attributeDetails.unassignmentOperation =  container.find(".unAssignment-select").val();

            $(button).trigger("click");
        },

        deleteAttribute: function(event) {
            event.preventDefault();

            console.log(($(event.target).parents(".list-group-item")));
            console.log(this.$el.find("#assignmentAttributesList .list-group-item"));

            var editorIndex = this.$el.find("#assignmentAttributesList .list-group-item").index($(event.target).parents(".list-group-item"));

            this.model.schemaEditors.splice(editorIndex, 1);
            this.model.assignmentAttributes.splice(editorIndex, 1);

            $(event.target).closest(".list-group-item").remove();
        },

        saveAssignmentDetails: function(event) {
            event.preventDefault();

            var formVal = form2js(this.$el.find('#assignmentDetailsForm')[0], '.', true);

            this.$el.find("#assignmentHeaderName").html(formVal.name);

            ResourceDelegate.patchResourceDifferences(this.model.serviceUrl, {id: this.data.resource._id, rev: this.data.resource._rev}, this.data.resource, formVal, _.bind(function(result){
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "assignmentSaveSuccess");

                this.data.resource = result;
            }, this));
        },

        saveAssignmentScripts: function(event) {
            event.preventDefault();

            var onAssignment = this.model.onAssignment.generateScript(),
                unAssignment = this.model.unAssignment.generateScript(),
                resourceObject = _.clone(this.data.resource);

            if(_.isNull(onAssignment)) {
                delete resourceObject.onAssignment;
            } else {
                resourceObject.onAssignment = onAssignment;
            }

            if(_.isNull(unAssignment)) {
                delete resourceObject.unAssignment;
            } else {
                resourceObject.unAssignment = unAssignment;
            }

            ResourceDelegate.updateResource(this.model.serviceUrl,  this.data.resource._id, resourceObject, _.bind(function(result){
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "assignmentSaveSuccess");

                this.data.resource = result;
            }, this));
        },

        saveAssignmentAttribute: function(event) {
            event.preventDefault();

            var selectAttribute = $(".select-attribute"),
                resourceObject = _.clone(this.data.resource);

            _.each(this.model.schemaEditors, _.bind(function(editor, index){
                this.model.assignmentAttributes[index].name = $(selectAttribute[index]).val();

                if(editor.schema) {
                    this.model.assignmentAttributes[index].value = editor.getValue();
                } else {
                    this.model.assignmentAttributes[index].value = editor.val();
                }
            }, this));

            resourceObject.attributes = this.model.assignmentAttributes;

            ResourceDelegate.updateResource(this.model.serviceUrl,  this.data.resource._id, resourceObject, _.bind(function(result){
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "assignmentSaveSuccess");

                this.data.resource = result;
            }, this));
        },

        deleteAssignment: function(event) {
            event.preventDefault();

            UIUtils.confirmDialog($.t("templates.admin.AssignmentTemplate.deleteMessage"), "danger", _.bind(function(){
                ResourceDelegate.deleteResource(this.model.serviceUrl, this.data.resource._id, _.bind(function(){
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteAssignmentSuccess");

                    EventManager.sendEvent(Constants.ROUTE_REQUEST, {routeName: "adminListManagedObjectView", args: ["managed","assignment"]});
                }, this),
                function(){
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteAssignmentFail");
                });
            }, this));
        },

        showRolesTab: function () {
            var tabView = new RelationshipArrayView();

            resourceDelegate.getSchema(this.model.args).then(_.bind(function (schema) {
                var opts = {
                    element: ".assignmentRoles",
                    prop: schema.properties.roles,
                    schema: schema
                };

                opts.prop.propName = "roles";
                opts.prop.selector = "\\.roles";
                opts.prop.relationshipUrl = "managed/assignment/" + this.data.resource._id + "/roles";

                tabView.render(opts);

            }, this));
        }
    });

    return new EditAssignmentView();
});
