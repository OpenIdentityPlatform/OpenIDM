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

/*global define */

define("org/forgerock/openidm/ui/admin/settings/AuditEventsDialog", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor",
    "org/forgerock/commons/ui/common/util/Constants",
    "bootstrap-dialog",
    "bootstrap-tabdrop"
], function($, _,
            AdminAbstractView,
            uiUtils,
            conf,
            InlineScriptEditor,
            constants,
            BootstrapDialog,
            tabdrop) {

    var AuditEventsDialog = AdminAbstractView.extend({
        template: "templates/admin/settings/AuditEventsDialogTemplate.html",
        el: "#dialogs",
        events: {
            "click .addWatchedField": "addWatchedField",
            "click .deleteWatchedField": "deleteWatchedField",
            "click .addPasswordField": "addPasswordField",
            "click .deletePasswordField": "deletePasswordField",
            "change .filterAction": "updateFilterAction",
            "click .filterFieldsCollapse": "filterFieldsCollapse",
            "click .deleteFilterField": "deleteFilterField",
            "change .filterFieldName": "updateFilterFieldName",
            "click .addFilterFieldValue": "addFilterFieldValue",
            "click .deleteFilterFieldValue": "deleteFilterFieldValue",
            "click .addFilterField": "addFilterField"
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
                actionsTab: false,
                fieldsTab: false,
                scriptTab: false,
                watchedTab: false,
                passwordTab: false,
                defaultToScript: false,
                docHelpUrl: constants.DOC_URL,
                defaults: args
            };

            _.each(this.data.defaults.eventDeclarativeActions, function(action) {
                if (_.has(this.data.defaults.event, "filter") && _.has(this.data.defaults.event.filter, "actions") &&  _.indexOf(this.data.defaults.event.filter.actions, action) >= 0){
                    this.data.setActions[action] = true;
                } else {
                    this.data.setActions[action] = false;
                }
            }, this);

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
         * @param titlePrefix
         * @param callback
         */
        render: function(args, callback) {
            var _this = this,
                title = args.titlePrefix + ": " + args.eventName.charAt(0).toUpperCase() + args.eventName.slice(1);

            this.preRenderSetup(_.omit(_.clone(args, true), "titlePrefix"));

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
                                    event.filter.fields[index] = _.omit(field, "collapsed");
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
            var script = {};

            if (_.has(this.data.defaults.event, "filter") && _.has(this.data.defaults.event.filter, "script")) {
                script = this.data.defaults.event.filter.script;
            }

            uiUtils.renderTemplate(
                this.template,
                this.$el,
                _.extend({}, conf.globalData, data),
                _.bind(function(data) {
                    this.$el.find(".nav-tabs").tabdrop();

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

        updateFilterAction: function(e) {
            var newVal = $(e.currentTarget),
                i;

            if (newVal.prop("checked")) {
                if (!_.has(this.data.defaults.event, "filter")) {
                    this.data.defaults.event.filter = {};
                    this.data.defaults.event.filter.actions = [];

                } else if (!_.has(this.data.defaults.event.filter, "actions")) {
                    this.data.defaults.event.filter.actions = [];
                }

                this.data.defaults.event.filter.actions.push(newVal.val());
            } else {
                i = _.indexOf(this.data.defaults.event.filter.actions, newVal.val());

                if (i > -1) {
                    this.data.defaults.event.filter.actions.splice(i, 1);

                    if (this.data.defaults.event.filter.actions.length === 0) {
                        delete this.data.defaults.event.filter.actions;

                        if (_.isEmpty(this.data.defaults.event.filter)) {
                            delete this.data.defaults.event.filter;
                        }
                    }
                }
            }
        },

        filterFieldsCollapse: function(e) {
            var container = $(e.currentTarget).closest(".well"),
                filterName = container.attr("data-filter-name"),
                fields = this.data.defaults.event.filter.fields,
                i = _.indexOf(fields, _.findWhere(fields, {"name": filterName}));

            container.find(".filterFieldsCollapse i").toggleClass("fa-caret-down");
            container.find(".filterFieldsCollapse i").toggleClass("fa-caret-up");
            container.find(".filterFieldValues").toggle();

            if (i !== -1) {
                fields[i].collapsed = container.find(".filterFieldsCollapse i").hasClass("fa-caret-up");
            }
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
                fields[i].name = filterName;
                this.reRender("fieldsTab");
            }
        },

        addFilterFieldValue: function(e) {
            var container = $(e.currentTarget).closest(".well"),
                filterName = container.attr("data-filter-name"),
                fields = this.data.defaults.event.filter.fields,
                i = _.indexOf(fields, _.findWhere(fields, {"name": filterName})),
                newValue = container.find(".newFilterValue").val();

            if (i !== -1) {
                fields[i].values.push(newValue);
                this.reRender("fieldsTab");
            }
        },

        deleteFilterFieldValue: function(e) {
            e.preventDefault();

            var fields = this.data.defaults.event.filter.fields,
                filterName = $(e.currentTarget).closest(".well").attr("data-filter-name"),
                filterValue = $(e.currentTarget).closest(".list-group-item").attr("data-filter-value"),
                filterIndex = _.indexOf(fields, _.findWhere(fields, {"name": filterName})),
                valueIndex;

            if (filterIndex !== -1) {
                valueIndex = _.indexOf(fields[filterIndex].values, filterValue);

                if (valueIndex !== -1) {
                    fields[filterIndex].values.splice(valueIndex, 1);
                    this.reRender("fieldsTab");
                }
            }
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

        addWatchedField: function(e) {
            var newVal = $(e.currentTarget).closest(".input-group").find(".watchedField").val();

            if (!this.data.defaults.event.watchedFields) {
                this.data.defaults.event.watchedFields = [];
            }

            if (newVal.length > 0) {
                this.data.defaults.event.watchedFields.push(newVal);
                this.reRender("watchedTab");
            }
        },

        deleteWatchedField: function(e) {
            var removeVal = $(e.currentTarget).closest(".list-group-item").find("label").text(),
                i = this.data.defaults.event.watchedFields.indexOf(removeVal);

            if (i > -1) {
                this.data.defaults.event.watchedFields.splice(i, 1);
                if (this.data.defaults.event.watchedFields.length === 0) {
                    delete this.data.defaults.event.watchedFields;
                }
                this.reRender("watchedTab");
            }
        },

        addPasswordField: function(e) {
            var newVal = $(e.currentTarget).closest(".input-group").find(".passwordField").val();

            if (!this.data.defaults.event.passwordFields) {
                this.data.defaults.event.passwordFields = [];
            }

            if (newVal.length > 0) {
                this.data.defaults.event.passwordFields.push(newVal);
                this.reRender("passwordTab");
            }
        },

        deletePasswordField: function(e) {
            var removeVal = $(e.currentTarget).closest(".list-group-item").find("label").text(),
                i = this.data.defaults.event.passwordFields.indexOf(removeVal);

            if (i > -1) {
                this.data.defaults.event.passwordFields.splice(i, 1);
                if (this.data.defaults.event.passwordFields.length === 0) {
                    delete this.data.defaults.event.passwordFields;
                }
                this.reRender("passwordTab");
            }
        }
    });

    return new AuditEventsDialog();
});
