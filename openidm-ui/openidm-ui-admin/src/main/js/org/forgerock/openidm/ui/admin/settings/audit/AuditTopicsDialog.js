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
    "org/forgerock/openidm/ui/admin/settings/audit/AuditAdminAbstractView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "bootstrap-dialog",
    "jsonEditor",
    "bootstrap-tabdrop"
], function($, _,
            AuditAdminAbstractView,
            uiUtils,
            conf,
            InlineScriptEditor,
            constants,
            ValidatorsManager,
            BootstrapDialog,
            JSONEditor) {

    var AuditTopicsDialog = AuditAdminAbstractView.extend({
        template: "templates/admin/settings/audit/AuditTopicsDialogTemplate.html",
        el: "#dialogs",
        events: {
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

            this.model.currentDialog = $('<div id="AuditEventsDialog"></div>');
            this.setElement(this.model.currentDialog);
            $('#dialogs').append(this.model.currentDialog);

            BootstrapDialog.show({
                title: title,
                size: BootstrapDialog.SIZE_WIDE,
                type: BootstrapDialog.TYPE_DEFAULT,
                message: this.model.currentDialog,
                onshown: function() {
                    _this.renderTemplate(_this.data, function() {
                        _this.$el.find(".nav-tabs").tabdrop();

                        _this.$el.find(".nav-tabs").on("shown.bs.tab", function (e) {
                            _this.model.filterScript.refresh();
                        });
                    });
                },
                buttons: [
                    {
                        label: $.t("common.form.cancel"),
                        action: function(dialogRef) {
                            dialogRef.close();
                        }
                    }, {
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

        renderTemplate: function(data, callback) {
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
                    ValidatorsManager.bindValidators(this.$el.find("#auditEventsForm"));
                    ValidatorsManager.validateAllFields(this.$el.find("#auditEventsForm"));

                    var jsonOptions = {};

                    if (this.data.defaults.limitedEdits) {
                        jsonOptions =  {
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
                        };
                    } else {
                        jsonOptions = {
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
                        };
                    }

                    if (!this.data.defaults.isDefault || this.data.defaults.event.schema) {
                        this.data.schemaEditor = new JSONEditor(this.$el.find("#schemaPane .jsonEditorContainer")[0], _.extend({
                            schema: {}
                        }, jsonOptions));

                        this.data.schemaEditor.setValue(schema);


                        if (this.data.defaults.limitedEdits) {
                            this.data.schemaEditor.disable();
                        }

                        this.data.schemaEditor.on('change', _.bind(function() {
                            this.data.defaults.event.schema = this.data.schemaEditor.getValue();
                        }, this));
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

                    if (callback) {
                        callback();
                    }

                }, this),
                "replace"
            );
        },

        reRender: function(tab) {
            this.preRenderSetup(this.data.defaults, tab);
            this.renderTemplate(this.data);
        },

        validationSuccessful: function (event) {
            AuditAdminAbstractView.prototype.validationSuccessful(event);

            if(ValidatorsManager.formValidated(this.$el.find("#auditEventsForm"))) {
                this.$el.parentsUntil(".model-content").find("#submitAuditEvent").prop('disabled', false);

                this.data.defaults.eventName = this.$el.find("#eventName").val().trim();
            }
        },

        validationFailed: function (event, details) {
            AuditAdminAbstractView.prototype.validationFailed(event, details);

            this.$el.parentsUntil(".model-content").find("#submitAuditEvent").prop('disabled', true);
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

    return new AuditTopicsDialog();
});
