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
    "handlebars",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/AbstractModel",
    "org/forgerock/openidm/ui/admin/util/WorkflowUtils"
], function($, _, Handlebars, AbstractView, eventManager, constants, UIUtils, AbstractModel, WorkflowUtils) {
    var TaskModel = AbstractModel.extend({ url: "/openidm/workflow/taskinstance" }),
        ProcessModel = AbstractModel.extend({ url: "/openidm/workflow/processdefinition" }),
        UserModel = AbstractModel.extend({ url: "/openidm/managed/user" }),
        TaskInstanceView = AbstractView.extend({
            template: "templates/admin/workflow/TaskInstanceViewTemplate.html",

            events: {
                "click .assignTask" : "showCandidateUserSelection"
            },
            render: function(args, callback) {
                var process = new ProcessModel(),
                    assignee = new UserModel();

                this.data = {
                    showForm: false,
                    canAssign: false
                };

                this.model = new TaskModel();

                this.model.id = args[0];

                this.model.fetch().then(_.bind(function () {
                    var fetchArr = [];

                    this.data.task = this.model.toJSON();

                    if (this.data.task.assignee) {
                        assignee.id = this.data.task.assignee;
                        fetchArr.push(assignee.fetch());
                    }

                    process.id = this.data.task.processDefinitionId;
                    fetchArr.push(process.fetch());

                    $.when.apply($, fetchArr).done(_.bind(function(){
                        var formTemplate = _.filter(this.data.task.formProperties, function(p) { return _.has(p,"_formGenerationTemplate"); });

                        this.data.process = process.toJSON();
                        this.data.assignee = assignee.toJSON();

                        if(formTemplate.length) {
                            this.data.showForm = true;

                            this.data.taskForm = Handlebars.compile(formTemplate[0]._formGenerationTemplate)(this.data.task);
                        }

                        if(!this.data.showForm && this.data.process.formGenerationTemplate) {
                            this.data.showForm = true;

                            this.data.taskForm = Handlebars.compile(this.data.process.formGenerationTemplate)(this.data.task);
                        }

                        this.parentRender(_.bind(function(){

                            if (this.data.showForm) {
                                this.populateTaskForm();
                            }

                            if (callback) {
                                callback();
                            }
                        },this));

                    },this));

                },this));
            },
            showCandidateUserSelection: function (e) {
                if (e) {
                    e.preventDefault();
                }

                WorkflowUtils.showCandidateUserSelection(this);
            },
            populateTaskForm: function () {
                /*
                 * sometimes form input fields have no replacement tokens like:
                 *    <input type="text" value="" name="userName"/>
                 * in this case form values will not be filled in when doing Handlebars.compile(html)(data)
                 *
                 * if there are replacement tokens like:
                 *    <input type="text" value="{{variables.userName}}" name="userName"/>
                 * it will work fine
                 *
                 * this loop is a fail safe so all forms are filled in with the task's variable values
                 */
                _.each(_.keys(this.data.task.variables), _.bind(function (key) {
                    this.$el.find("[name=" + key + "]").val(this.data.task.variables[key]);
                }, this));

                this.$el.find("#taskForm :input").prop("disabled", true);

                this.$el.find("#taskForm .error").hide();
            }
        });

    return new TaskInstanceView();
});
