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
    "underscore",
    "form2js",
    "org/forgerock/commons/ui/common/util/ModuleLoader",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/workflow/WorkflowDelegate",
    "org/forgerock/openidm/ui/common/workflow/FormManager",
    "org/forgerock/openidm/ui/common/workflow/tasks/TemplateTaskForm",
    "org/forgerock/openidm/ui/common/util/FormGenerationUtils"
], function(_, form2js, ModuleLoader, AbstractView, validatorsManager, eventManager, constants, workflowManager, tasksFormManager, templateTaskForm, formGenerationUtils) {
    var TaskDetailsView = AbstractView.extend({
        template: "templates/workflow/tasks/TaskDetailsTemplate.html",

        element: "#taskDetails",

        events: {
            "onValidate": "onValidate",
            "click input[name=saveButton]": "formSubmit"
        },

        formSubmit: function(event) {
            event.preventDefault();
            if(validatorsManager.formValidated(this.$el)) {
                var params = form2js(this.$el.attr("id"), '.', false), param;
                delete params.saveButton;
                delete params.requeueButton;
                for (param in params) {
                    if (_.isNull(params[param])) {
                        delete params[param];
                    }
                }

                if (this.definitionFormPropertyMap) {
                    formGenerationUtils.changeParamsToMeetTheirTypes(params, this.definitionFormPropertyMap);
                }

                workflowManager.completeTask(this.task._id, params, _.bind(function() {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "completedTask");
                    //eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "", trigger: true});
                    eventManager.sendEvent("refreshTasksMenu");
                }, this));
            }
        },

        render: function(task, definition, category, callback) {
            var formValidateOptions = {
                element: ".workflowFormContainer",
                attribute: "data-validationbaseentity"
            };

            this.data = _.extend(this.data, {category: category});
            this.data.showRequeue = (definition.taskCandidateGroup.length || definition.taskCandidateUser.length);

            this.parentRender(function() {
                this.task = task;

                var template = this.getGenerationTemplate(definition, task), view, passJSLint;
                delete this.definitionFormPropertyMap;

                if (template === false && definition.formResourceKey) {
                    ModuleLoader.load(tasksFormManager.getViewForForm(definition.formResourceKey)).then(function (view) {
                        view.render(task, category, null, callback);
                    });
                } else if (template !== false) {
                    templateTaskForm.render(
                        task,
                        category,
                        template,
                        _.bind(formGenerationUtils.validateForm, this, formValidateOptions, validatorsManager, callback)
                    );
                } else {
                    this.definitionFormPropertyMap = formGenerationUtils.buildPropertyTypeMap(definition.formProperties.formPropertyHandlers);
                    templateTaskForm.render(
                        task,
                        category,
                        formGenerationUtils.generateTemplateFromFormProperties(
                            {"formProperties": definition.formProperties.formPropertyHandlers},
                            task.formProperties
                        ),
                        _.bind(formGenerationUtils.validateForm, this, formValidateOptions, validatorsManager, callback)
                    );
                }
            });
        },

        getGenerationTemplate: function(definition, task) {
            var formPropertyHandlers = definition.formProperties.formPropertyHandlers, property, i;
            if (typeof definition.formGenerationTemplate === "string") {
                return definition.formGenerationTemplate;
            }
            for(i = 0; i < formPropertyHandlers.length; i++) {
                property = formPropertyHandlers[i];
                if(property._id === "_formGenerationTemplate") {
                    return task.formProperties[i][property._id];
                }
            }
            return false;
        }

    });

    return new TaskDetailsView();
});
