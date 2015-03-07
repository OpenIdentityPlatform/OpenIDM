/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All rights reserved.
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

/*global define, $, form2js, _, js2form, document, require */

/**
 * @author mbilski
 */
define("org/forgerock/openidm/ui/admin/workflow/processes/StartProcessView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/workflow/WorkflowDelegate",
    "org/forgerock/openidm/ui/admin/workflow/FormManager",
    "org/forgerock/openidm/ui/admin/workflow/processes/TemplateStartProcessForm",
    "org/forgerock/commons/ui/common/util/FormGenerationUtils",
    "org/forgerock/commons/ui/common/util/DateUtil"
], function(AbstractView, validatorsManager, eventManager, constants, workflowManager, formManager, templateStartProcessForm, formGenerationUtils, dateUtil) {
    var StartProcessView = AbstractView.extend({
        template: "templates/admin/workflow/processes/StartProcessTemplate.html",

        element: "#processDetails",
        
        events: {
            "click input[name=startProcessButton]": "formSubmit",
            "onValidate": "onValidate"
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
                            view = require(formManager.getViewForForm(definition.formResourceKey));
                            if (view.render) {
                                view.render(definition, {}, {}, callback);
                                return;
                            } else {
                                console.log("There is no view defined for " + definition.formResourceKey);
                            }
                        } 
                        
                        if(template !== false) {
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


