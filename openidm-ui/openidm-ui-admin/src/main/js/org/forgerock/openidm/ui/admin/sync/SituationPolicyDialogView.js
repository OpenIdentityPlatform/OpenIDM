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

/*global define, $, _, require, window */

define("org/forgerock/openidm/ui/admin/sync/SituationPolicyDialogView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/admin/mapping/MappingBaseView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor",
    "org/forgerock/openidm/ui/admin/sync/LinkQualifierFilterEditor",
    "bootstrap-dialog"
], function(AbstractView, MappingBaseView, conf, uiUtils, InlineScriptEditor, LinkQualifierFilterEditor, BootstrapDialog) {
    var SituationPolicyDialogView = AbstractView.extend({
        template: "templates/admin/sync/SituationPolicyDialogTemplate.html",
        el: "#dialogs",
        events: {
            "click .tabButtons .btn": "sectionControl"
        },
        model: {},
        data: {
            star: "&#9733;",
            hollowStar: "&#9734;"
        },

        render: function(args, callback) {
            var _this = this,
                title = "";

            this.data.mappingName = args.mappingName;
            this.data.mappingProperties = args.mapProps;
            this.data.situation = args.situation;
            this.data.edit = args.edit;
            this.data.defaultAction = false;
            this.data.defaultWorkflow = false;
            this.data.defaultScript = false;
            this.data.defaultConditionScript = false;
            this.data.defaultConditionFilter = true;
            this.data.note = args.basePolicy.note;
            this.data.workflows = args.workflows;

            this.model.policy = args.policy;
            this.model.basePolicy = args.basePolicy;
            this.model.saveCallback = args.saveCallback;
            this.model.lookup = args.lookup;
            this.model.currentDialog = $('<div id="SituationPolicyDialog"></div>');

            if (this.data.edit) {
                title = "Edit Policy for Situation: " + this.model.lookup[this.data.situation];
                this.model.action = this.model.policy.action;
                this.model.condition = this.model.policy.condition;
                this.model.postAction = this.model.policy.postAction;
            } else {
                title = "Add Policy for Situation: " + this.model.lookup[this.data.situation];
                this.model.action = this.model.basePolicy.action;
                this.model.condition = this.model.basePolicy.condition;
                this.model.postAction = this.model.basePolicy.postAction;
            }

            if (_(this.model.action).isObject() && _(this.model.action).has("file") && this.model.action.file === "workflow/triggerWorkflowFromSync.js") {
                this.data.defaultWorkflow = true;
            } else if (_(this.model.action ).isObject() && _(this.model.action ).has("type")) {
                this.data.defaultScript = true;
            } else {
                this.data.defaultAction = true;
            }

            if (_(this.model.condition).isObject() && _(this.model.condition).has("type")) {
                this.data.defaultConditionScript = true;
                this.data.defaultConditionFilter = false;
            }

            this.setElement(this.model.currentDialog);

            $('#dialogs').append(this.model.currentDialog);
            BootstrapDialog.show({
                title: title,
                size: BootstrapDialog.SIZE_WIDE,
                type: BootstrapDialog.TYPE_DEFAULT,
                message: this.model.currentDialog,
                onshown : function (dialogRef) {
                    uiUtils.renderTemplate(
                        _this.template,
                        _this.$el,
                        _.extend({}, conf.globalData, _this.data),
                        _.bind(function() {
                            var tempSelector,
                                actionScriptData = {},
                                conditionScriptData = {},
                                conditionFilter = "";

                            this.$el.find(".nav-tabs").tabdrop();

                            // Set viable options stars
                            _(this.model.basePolicy.options).each(function(action) {
                                tempSelector = $("#defaultActionPane select option[value='"+action+"']");
                                tempSelector.html(tempSelector.text() + " " + this.data.hollowStar);
                            }, this);

                            // Set default option star
                            tempSelector = $("#defaultActionPane select option[value='"+this.model.basePolicy.action+"']");
                            tempSelector.html(tempSelector.text() + " " + this.data.star);

                            this.$el.find("#defaultActionPane select").val(this.model.basePolicy.action);

                            // Set action value
                            if (this.data.defaultWorkflow) {
                                this.$el.find("#workflowPane select").val(this.model.action.workflowName);
                            } else if (this.data.defaultScript) {
                                actionScriptData = this.model.action;
                            } else if (this.data.edit) {
                                this.$el.find("#defaultActionPane select").val(this.model.action);

                            }

                            // Set condition value
                            if (this.data.defaultConditionScript) {
                                conditionScriptData = this.model.condition;
                            } else {
                                conditionFilter = this.model.condition;
                            }

                            // Load Script Editors
                            this.model.scriptPaneEditor = InlineScriptEditor.generateScriptEditor({
                                "element": $("#scriptPane"),
                                "eventName": "scriptPane",
                                "noValidation": true,
                                "scriptData": actionScriptData
                            });

                            this.model.conditionScriptPaneEditor = InlineScriptEditor.generateScriptEditor({
                                "element": $("#conditionScriptPane"),
                                "eventName": "conditionScriptPane",
                                "noValidation": true,
                                "scriptData": conditionScriptData
                            });

                            this.model.actionScriptPaneEditor = InlineScriptEditor.generateScriptEditor({
                                "element": $("#postActionScript"),
                                "eventName": "postActionScript",
                                "noValidation": true,
                                "scriptData": this.model.postAction
                            });

                            // Load Query Filter Editor
                            this.model.conditionFilterEditor = new LinkQualifierFilterEditor();

                            this.model.conditionFilterEditor.render({
                                "mappingName" : this.data.mappingName,
                                "mapProps": this.data.mappingProperties,
                                "queryFilter": conditionFilter || "",
                                "element": "#conditionFilterPane",
                                "resource" : ""
                            });

                            this.$el.find('a[data-toggle="tab"]').on('shown.bs.tab', _.bind(function (e) {
                                this.model.actionScriptPaneEditor.refresh();
                                this.model.conditionScriptPaneEditor.refresh();
                                this.model.scriptPaneEditor.refresh();
                            }, this));
                        }, _this),
                        "replace");
                },
                buttons: [
                    {
                        label: $.t("common.form.cancel"),
                        action: function(dialogRef) {
                            dialogRef.close();
                        }
                    },
                    {
                        label: $.t("common.form.submit"),
                        id: "submitPolicyDialog",
                        cssClass: "btn-primary",
                        action: _.bind(function(dialogRef) {
                            var temp,
                                returnPolicy = {
                                    "situation": this.data.situation
                                };

                            // Get action
                            if (this.$el.find("#action .tabButtons .active").attr("id") === "defaultActionTab") {
                                returnPolicy.action = this.$el.find("#defaultActionPane select").val();
                            } else if (this.$el.find("#action .tabButtons .active").attr("id") === "workflowTab") {
                                returnPolicy.action = {
                                    "workflowName": this.$el.find("#workflowPane select").val(),
                                    "type": "text/javascript",
                                    "file": "workflow/triggerWorkflowFromSync.js"
                                };
                            } else {
                                returnPolicy.action = this.model.scriptPaneEditor.generateScript();
                            }

                            // Get Condition
                            temp = this.$el.find("#restrict .tabButtons .active").attr("id");
                            if (temp === "conditionFilterTab" && this.model.conditionFilterEditor.getFilterString().length > 0) {
                                returnPolicy.condition = this.model.conditionFilterEditor.getFilterString();
                            } else if (temp === "conditionScriptTab" && this.model.conditionScriptPaneEditor.generateScript() !== null) {
                                returnPolicy.condition = this.model.conditionScriptPaneEditor.generateScript();
                            }

                            temp = this.model.actionScriptPaneEditor.generateScript();
                            if (temp !== null) {
                                returnPolicy.postAction = temp;
                            }

                            this.model.saveCallback(returnPolicy);
                            dialogRef.close();
                        }, this)
                    }]
            });
        },

        sectionControl: function(event) {
            var selected = $(event.target);
            selected.parent().find('.active').removeClass('active');
        }
    });

    return new SituationPolicyDialogView();
});