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
 * Copyright 2011-2015 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/common/workflow/processes/StartProcessView", [
    "jquery",
    "underscore",
    "form2js",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/workflow/WorkflowDelegate",
    "org/forgerock/openidm/ui/common/workflow/FormManager",
    "org/forgerock/openidm/ui/common/workflow/processes/TemplateStartProcessForm",
    "org/forgerock/openidm/ui/common/util/FormGenerationUtils",
    "org/forgerock/commons/ui/common/util/DateUtil",
    "org/forgerock/commons/ui/common/util/ModuleLoader"
], function($, _,
            form2js,
            AbstractView,
            validatorsManager,
            eventManager,
            constants,
            workflowManager,
            formManager,
            templateStartProcessForm,
            formGenerationUtils,
            dateUtil,
            ModuleLoader) {
    var StartProcessView = AbstractView.extend({
        template: "templates/workflow/processes/StartProcessTemplate.html",

        element: "#processDetails",

        events: {
            "click input[name=startProcessButton]": "formSubmit",
            "onValidate": "onValidate",
            "click .closeLink" : "hideDetails"
        },

        hideDetails: function(event) {
            if(event) {
                event.preventDefault();
            }

            //since this view is limited have to go above to set arrows correct
            $("#processes").find(".details-link .fa").toggleClass("fa-caret-right", true);
            $("#processes").find(".details-link .fa").toggleClass("fa-caret-down", false);

            this.$el.empty();
        },


        formSubmit: function(event) {
            event.preventDefault();

            if(validatorsManager.formNotInvalid(this.$el)) {
                var params = form2js(this.$el.attr("id"), '.', false), param, typeName, paramValue, date, dateFormat;
                delete params.startProcessButton;
                for (param in params) {
                    if (_.isNull(params[param])) {
                        delete params[param];
                    }
                }

                if (this.definitionFormPropertyMap) {
                    formGenerationUtils.changeParamsToMeetTheirTypes(params, this.definitionFormPropertyMap);
                }

                workflowManager.startProcessById(this.processDefinition._id, params, _.bind(function() {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "startedProcess");
                    //eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "", trigger: true});
                    eventManager.sendEvent("refreshTasksMenu");
                }, this));
            }
        },

        render: function(id, category, callback) {
            this.parentRender(function() {
                validatorsManager.bindValidators(this.$el);
                    workflowManager.getProcessDefinition(id, _.bind(function(definition) {
                        var template = this.getGenerationTemplate(definition), view, passJSLint;
                        this.processDefinition = definition;
                        delete this.definitionFormPropertyMap;

                        if(template === false && definition.formResourceKey) {
                            ModuleLoader.load(formManager.getViewForForm(definition.formResourceKey)).then(function (view) {
                                view.render(definition, {}, {}, callback);
                            });
                        } else if(template !== false) {
                            templateStartProcessForm.render(definition, {}, template, _.bind(function() {
                                validatorsManager.bindValidators(this.$el);
                                validatorsManager.validateAllFields(this.$el);

                                if(callback) {
                                    callback();
                                }
                            }, this));
                            return;
                        } else {
                            this.definitionFormPropertyMap = formGenerationUtils.buildPropertyTypeMap(definition.formProperties);
                            templateStartProcessForm.render({"formProperties": definition.formProperties.formPropertyHandlers}, {}, formGenerationUtils.generateTemplateFromFormProperties(definition), _.bind(function() {
                                validatorsManager.bindValidators(this.$el);
                                validatorsManager.validateAllFields(this.$el);

                                this.$el.find("select").toggleClass("form-control", true);

                                if(callback) {
                                    callback();
                                }
                            }, this));
                            return;
                        }
                    }, this));
            });
        },

        getGenerationTemplate: function(definition) {
            var property, i;
            if (typeof definition.formGenerationTemplate === "string") {
                return definition.formGenerationTemplate;
            }
            for(i = 0; i < definition.formProperties.length; i++) {
                property = definition.formProperties[i];
                if(property._id === "_formGenerationTemplate") {
                    return property.defaultExpression.expressionText;
                }
            }
            return false;
        }

    });

    return new StartProcessView();
});
