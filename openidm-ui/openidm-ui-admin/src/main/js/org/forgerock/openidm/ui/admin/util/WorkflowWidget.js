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

define("org/forgerock/openidm/ui/admin/util/WorkflowWidget", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/admin/delegates/WorkflowDelegate",
    "org/forgerock/commons/ui/common/main/ValidatorsManager"

], function($, _, AbstractView, WorkflowDelegate, ValidatorsManager) {

    var workflowInstance = {},
        WorkflowWidget = AbstractView.extend({
            template: "templates/admin/util/WorkflowWidgetTemplate.html",
            noBaseTemplate: true,
            events: {
                "change #workflowList": "changeSchema"
            },
            model: {
                changeCallback: _.noop(),
                key: "",
                params: {}
            },

            /**
             * @param args
             *   {
             *       params: {
             *           name1: "value1",
             *           name2: "script2"
             *       },
             *       sync: boolean,
             *       key: string,
             *       changeCallback: function(){},
             *       element: $.el,
             *       context: {}
             *   }
             *
             * @param callback
             */
            render: function (args, callback) {
                var selected = false;
                this.model.callback = callback;

                function myParentRender() {
                    this.parentRender(_.bind(function() {
                        if (selected !== false) {
                            _.each(selected.params, function(param, id) {
                                this.$el.find("#" + id).find("textarea").val(param);
                            }, this);

                            this.$el.find("#workflowList").val(selected._id);
                        }

                        ValidatorsManager.bindValidators(this.$el);
                        ValidatorsManager.validateAllFields(this.$el);

                        if (callback) {
                            callback(this.getValid());
                        }
                    }, this));
                }

                this.data = {
                    workflows: [],
                    sync: false,
                    selectedWorkflow: {},
                    context: ""
                };

                this.element = args.element;

                if (args.changeCallback) {
                    this.model.changeCallback = args.changeCallback;
                }

                if (args.key) {
                    this.model.key = args.key;
                }

                if (args.context && !_.isString(args.context)) {
                    this.data.context = JSON.stringify(args.context, null, '\t');
                } else {
                    this.data.context = args.context;
                }

                if (args.params) {
                    this.model.params = args.params;
                }

                if (args.sync) {
                    this.data.sync = args.sync;
                }

                if (args.workflows) {
                    this.data.workflows = args.workflows;
                    selected = _.findWhere(this.data.workflows, {"key": this.model.key});
                    this.getWorkflowSchema(selected).then(_.bind(function(properties) {
                        this.data.properties = properties;
                        _.bind(myParentRender, this)();
                    }, this));

                } else {
                    this.getWorkflowList(this.model.key, this.model.params).then(_.bind(function(workflows) {
                        if (workflows.workflowList.length > 0) {
                            selected = workflows.selectedWorkflow || workflows.workflowList[0];
                            this.data.workflows = workflows.workflowList;
                            this.getWorkflowSchema(selected).then(_.bind(function(properties) {
                                this.data.properties = properties;
                                _.bind(myParentRender, this)();
                            }, this));

                        } else {
                            this.data.properties = [];
                            this.data.workflows = [];
                            _.bind(myParentRender, this)();
                        }
                    }, this));
                }
            },

            /**
             * Gets a list of all workflows and all necessary properties
             *
             * @param selectedKey
             * @param params
             * @returns {array}
             *      "workflowList": A list of workflow objects
             *      "selectedWorkflow": The selected workflow object
             *
             */
            getWorkflowList: function(selectedKey, params) {
                var workflowList = [],
                    promise = $.Deferred(),
                    selectedWorkflow = null;

                WorkflowDelegate.availableWorkflows().then(_.bind(function(workflowData) {

                    _(workflowData.result).each(function (workflow) {
                        workflowList.push({
                            "key": workflow.key,
                            "name": workflow.name,
                            "_id": workflow._id
                        });

                        if (workflow.key === selectedKey) {
                            selectedWorkflow = _.clone(_.last(workflowList));
                            selectedWorkflow.params = params;
                        }

                    }, this);

                    promise.resolve({
                        "workflowList": workflowList,
                        "selectedWorkflow": selectedWorkflow
                    });
                }, this));

                return promise;
            },

            /**
             * Gets a detailed list of properties for a given workflow.
             *
             * @param workflow
             * @returns workflowProperties {array}
             */
            getWorkflowSchema: function(workflow) {
                var promise = $.Deferred(),
                    workflowProperties = [];

                WorkflowDelegate.workflowFormProperties(workflow._id).then(_.bind(function (result) {

                    if (_.has(result, "startFormHandler") && _.has(result.startFormHandler, "formPropertyHandlers")) {

                        _.each(result.startFormHandler.formPropertyHandlers, function(property) {
                            if (property.id[0] !== "_") {
                                workflowProperties.push({
                                    "id": property.id,
                                    "name": property.name,
                                    "required": property.required,
                                    "type": property.type.name
                                });
                            }
                        }, this);
                    }

                    promise.resolve(workflowProperties);

                }, this));

                return promise;
            },

            isValid: function() {
                return $(".workflow-body textarea[data-validator]").length === 0 || ValidatorsManager.formValidated(this.$el);
            },

            changeSchema: function() {
                this.render({
                    "params": {},
                    "sync": this.data.sync,
                    "key": _.findWhere(this.data.workflows, {"_id": this.$el.find("#workflowList").val()}).key,
                    "changeCallback":  this.model.changeCallback,
                    "workflows": this.data.workflows,
                    "element": this.element,
                    "context": this.data.context
                }, this.model.callback);

                if (this.model.changeCallback) {
                    this.model.changeCallback();
                }
            },

            getConfiguration: function () {
                var config = _.findWhere(this.data.workflows, {"_id": this.$el.find("#workflowList").val()}),
                    properties = {},
                    file = "workflow/triggerWorkflowGeneric.js";

                if (this.data.sync) {
                    file = "workflow/triggerWorkflowFromSync.js";
                }
                _.each(this.$el.find(".workflowProperty"), function(property) {
                    properties[property.id] = $(property).find("textarea").val();
                });

                return {
                    "type": "text/javascript",
                    "file": file,
                    "globals": {
                        "workflowReadable": config.name,
                        "workflowName": config.key,
                        "params": properties
                    }
                };
            },

            getValid: function () {
                return this.data.workflows.length > 0;
            }
        });

    workflowInstance.generateWorkflowWidget = function(loadingObject, callback) {
        var widget = {};
        $.extend(true, widget, new WorkflowWidget());
        widget.render(loadingObject,callback);
        return widget;
    };

    return workflowInstance;
});
