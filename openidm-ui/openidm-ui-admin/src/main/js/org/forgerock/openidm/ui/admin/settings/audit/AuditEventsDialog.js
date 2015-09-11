/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
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

/*global define*/

define("org/forgerock/openidm/ui/admin/settings/audit/AuditEventsDialog", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/settings/audit/AuditAdminAbstractView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "bootstrap-dialog",
    "jsonEditor"

], function($, _,
            AuditAdminAbstractView,
            uiUtils,
            conf,
            InlineScriptEditor,
            constants,
            validatorsManager,
            BootstrapDialog,
            JSONEditor) {

    var AuditEventsDialog = AuditAdminAbstractView.extend({
        template: "templates/admin/settings/audit/AuditEventsDialogTemplate.html",
        el: "#dialogs",
        events: {
            "onValidate": "onValidate",
            "customValidate": "customValidate",
            "change .watchedFields": "updateWatchedFields",
            "change .passwordFields": "updatePasswordFields",
            "change .filterActions": "updateFilterAction",
            "change .triggerActions": "updateFilterTriggers",
            "click .deleteFilterField": "deleteFilterField",
            "click .addFilterField": "addFilterField",
            "change .filterFieldName": "updateFilterFieldName",
            "change .filterFieldValues": "updateFilterFieldValues"
        },
        model: {},

        /**
         * Sets data and model values before the render
         *
         * Updated which declarative events should be selected
         *
         * Chooses a tab for default selection
         *
         * @param args
         * @param tab
         */
        preRenderSetup: function(args, tab) {
            this.data = {
                setActions: {},
                schemaTab: false,
                actionsTab: false,
                fieldsTab: false,
                scriptTab: false,
                triggerTab: false,
                watchedTab: false,
                passwordTab: false,
                defaultToScript: false,
                docHelpUrl: constants.DOC_URL,
                defaults: args,
                usedNames:args.definedEvents
            };

            if (tab) {
                this.data[tab] = true;
            } else {
                this.data.actionsTab = true;
            }
        },

        /**
         * Opens the dialog
         *
         * @param args
         * @param callback
         */
        render: function(args, callback) {
            var _this = this,
                title = "";

            if (args.newEvent) {
                title = $.t("templates.audit.events.dialog.addEvent");
            } else {
                title = $.t("templates.audit.events.dialog.editEvent") + ": " + args.eventName.charAt(0).toUpperCase() + args.eventName.slice(1);
            }

            this.preRenderSetup(_.clone(args, true));


            if (this.data.defaults.limitedEdits) {
                _(JSONEditor.defaults.options).extend({
                    ajax: false,
                    disable_array_add: true,
                    disable_array_delete: true,
                    disable_array_reorder: true,
                    disable_collapse: false,
                    disable_edit_json: true,
                    disable_properties: true,
                    iconlib: "fontawesome4",
                    no_additional_properties: false,
                    object_layout: "normal",
                    required_by_default: true,
                    show_errors: "never",
                    template: "handlebars",
                    theme: "bootstrap3"
                });
            } else {
                _(JSONEditor.defaults.options).extend({
                    ajax: false,
                    disable_array_add: false,
                    disable_array_delete: false,
                    disable_array_reorder: false,
                    disable_collapse: false,
                    disable_edit_json: false,
                    disable_properties: false,
                    iconlib: "fontawesome4",
                    no_additional_properties: false,
                    object_layout: "normal",
                    required_by_default: true,
                    show_errors: "never",
                    template: "handlebars",
                    theme: "bootstrap3"
                });
            }

            this.model.currentDialog = $('<div id="AuditEventsDialog"></div>');
            this.setElement(this.model.currentDialog);
            $('#dialogs').append(this.model.currentDialog);

            BootstrapDialog.show({
                title: title,
                size: BootstrapDialog.SIZE_WIDE,
                type: BootstrapDialog.TYPE_DEFAULT,
                message: this.model.currentDialog,
                onshown: _this.renderTemplate(_this.data),
                buttons: [
                    {
                        label: $.t("common.form.cancel"),
                        action: function(dialogRef) {
                            dialogRef.close();
                        }
                    },
                    {
                        label: $.t("common.form.submit"),
                        id: "submitAuditEvent",
                        cssClass: "btn-primary",
                        action: _.bind(function(dialogRef) {
                            var returnData = {
                                    eventName: this.data.defaults.eventName,
                                    data: {}
                                },
                                event = this.data.defaults.event;

                            if (_.has(event, "filter") && _.has(event.filter, "fields")) {
                                _.each(event.filter.fields, function(field, index) {
                                    if(field.name === "") {
                                        event.filter.fields.splice(index, 1);
                                    }
                                }, this);
                            }

                            returnData.data = event;

                            if (callback) {
                                callback(returnData);
                            }
                            dialogRef.close();
                        }, _this)
                    }
                ]
            });
        },

        renderTemplate: function(data) {
            var script = {},
                schema = {},
                triggers = [],
                actions = [],
                defaultTriggers = [],
                fields = {};

            if (_.has(this.data.defaults.event, "filter") && _.has(this.data.defaults.event.filter, "actions")) {
                actions = this.data.defaults.event.filter.actions;
            }

            if (_.has(this.data.defaults.event, "filter") && _.has(this.data.defaults.event.filter, "script")) {
                script = this.data.defaults.event.filter.script;
            }

            if (_.has(this.data.defaults.event, "filter") && _.has(this.data.defaults.event.filter, "triggers")) {
                triggers = this.data.defaults.event.filter.triggers;
            }

            if (_.has(this.data.defaults.event, "filter") && _.has(this.data.defaults.event.filter, "fields")) {
                fields = this.data.defaults.event.filter.fields;
            }

            if (_.has(this.data.defaults, "triggers")) {
                defaultTriggers = this.data.defaults.triggers;
            }

            if (_.has(this.data.defaults.event, "schema")){
                schema = this.data.defaults.event.schema;
            }


            uiUtils.renderTemplate(
                this.template,
                this.$el,
                _.extend({}, conf.globalData, data),
                _.bind(function(data) {
                    validatorsManager.bindValidators(this.$el.find("#auditEventsForm"));
                    validatorsManager.validateAllFields(this.$el.find("#auditEventsForm"));

                    this.$el.find(".nav-tabs").tabdrop();

                    this.data.schemaEditor = new JSONEditor(this.$el.find("#schemaPane .jsonEditorContainer")[0], {schema:{}});
                    this.data.schemaEditor.setValue(schema);

                    if (this.data.defaults.limitedEdits) {
                        this.data.schemaEditor.disable();
                    }

                    this.$el.find(".filterActions").selectize({
                        delimiter: ',',
                        persist: false,
                        create: false,
                        items: actions
                    });

                    this.$el.find(".watchedFields").selectize({
                        delimiter: ',',
                        persist: false,
                        create: true,
                        items: this.data.defaults.event.watchedFields || []
                    });

                    this.$el.find(".passwordFields").selectize({
                        delimiter: ',',
                        persist: false,
                        create: true,
                        items: this.data.defaults.event.passwordFields || []
                    });

                    _.each(fields, function(data) {
                        this.$el.find(".filterFieldValues-"+data.name).selectize({
                            delimiter: ',',
                            persist: false,
                            create: true,
                            items: data.values || []
                        });
                    }, this);

                    _.each(defaultTriggers, function(values, key) {
                        this.$el.find(".trigger-"+key).selectize({
                            delimiter: ',',
                            persist: false,
                            create: false,
                            items: triggers[key] || []
                        });
                    }, this);

                    this.data.schemaEditor.on('change', _.bind(function() {
                        this.data.defaults.event.schema = this.data.schemaEditor.getValue();
                    }, this));

                    this.model.filterScript = InlineScriptEditor.generateScriptEditor({
                        "element": this.$el.find("#filterScript"),
                        "eventName": "filterScript",
                        "disableValidation": true,
                        "onChange": _.bind(function(e) {
                            var script = this.model.filterScript.generateScript();

                            if (!_.isNull(script)) {
                                if (!_.has(this.data.defaults.event, "filter")) {
                                    this.data.defaults.event.filter = {};
                                }

                                this.data.defaults.event.filter.script = script;
                            } else if (_.has(this.data.defaults.event.filter, "script")) {
                                delete this.data.defaults.event.filter.script;

                                if (_.isEmpty(this.data.defaults.event.filter)) {
                                    delete this.data.defaults.event.filter;
                                }
                            }
                        }, this),
                        "scriptData": script
                    });
                }, this),
                "replace"
            );
        },

        reRender: function(tab) {
            this.preRenderSetup(this.data.defaults, tab);
            this.renderTemplate(this.data);
        },

        customValidate: function() {
            this.validationResult = validatorsManager.formValidated(this.$el.find("#auditEventsForm"));
            this.$el.parentsUntil(".model-content").find("#submitAuditEvent").prop('disabled', !this.validationResult);
            if (this.validationResult) {
                this.data.defaults.eventName = this.$el.find("#eventName").val().trim();
            }
        },

        updateFilter: function(prop, value) {
            if (!_.isEmpty(value) && !_.has(this.data.defaults.event, "filter")) {
                this.data.defaults.event.filter = {};
            }

            if (_.isEmpty(value) && _.keys(this.data.defaults.event.filter).length === 0) {
                delete this.data.defaults.event.filter;
            } else {
                this.data.defaults.event.filter[prop] = value;
            }
        },

        updateFilterAction: function(e) {
            this.updateFilter("actions", this.$el.find(".filterActions").val());
        },

        updateFilterTriggers: function(e) {
            var triggers = {};

            _.each(this.data.defaults.triggers, function(values, key) {
                triggers[key] = this.$el.find(".trigger-"+key).val() || [];
            }, this);

            this.updateFilter("triggers", triggers);
        },

        addFilterField: function(e) {
            e.preventDefault();

            if (!_.has(this.data.defaults.event, "filter")) {
                this.data.defaults.event.filter = {};
                this.data.defaults.event.filter.fields = [];

            } else if(!_.has(this.data.defaults.event.filter, "fields")) {
                this.data.defaults.event.filter.fields = [];
            }

            this.data.defaults.event.filter.fields.push({
                "name": "",
                "values": []
            });

            this.reRender("fieldsTab");
        },

        deleteFilterField: function(e) {
            var container = $(e.currentTarget).closest(".well"),
                filterName = container.attr("data-filter-name"),
                fields = this.data.defaults.event.filter.fields,
                i = _.indexOf(fields, _.findWhere(fields, {"name": filterName}));

            if (i !== -1) {
                fields.splice(i, 1);

                if (fields.length === 0) {
                    delete this.data.defaults.event.filter.fields;

                    if (_.isEmpty(this.data.defaults.event.filter)) {
                        delete this.data.defaults.event.filter;
                    }
                }

                this.reRender("fieldsTab");
            }
        },

        updateFilterFieldName: function(e) {
            var filterName = e.currentTarget.value,
                oldFilterName = $(e.currentTarget).closest(".well").attr("data-filter-name"),
                fields = this.data.defaults.event.filter.fields,
                i = _.indexOf(fields, _.findWhere(fields, {"name": oldFilterName}));

            if (i !== -1) {
                if (_.find(this.data.defaults.event.filter.fields, function (field) { return field.name === filterName; })) {
                    $(e.currentTarget).closest(".well").find(".uniqueName").show();
                } else {
                    fields[i].name = filterName;
                    fields[i].values = $(e.currentTarget).closest(".well").find(".filterFieldValues").val();
                    this.reRender("fieldsTab");
                }
            }
        },

        updateFilterFieldValues: function(e) {
            var container = $(e.currentTarget).closest(".well"),
                filterName = container.attr("data-filter-name"),
                fields = this.data.defaults.event.filter.fields,
                i = _.indexOf(fields, _.findWhere(fields, {"name": filterName}));

            if (i !== -1) {
                fields[i].values = this.$el.find(".filterFieldValues-" + filterName).val();
                this.reRender("fieldsTab");
            }
        },

        updateWatchedFields: function(e) {
            this.data.defaults.event.watchedFields = this.$el.find(".watchedFields").val();
        },

        updatePasswordFields: function(e) {
            this.data.defaults.event.passwordFields = this.$el.find(".passwordFields").val();
        }

    });

    return new AuditEventsDialog();
});
