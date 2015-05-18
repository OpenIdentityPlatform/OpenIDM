/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

/*global define, $, _, Handlebars, form2js, window */

define("org/forgerock/openidm/ui/admin/util/InlineScriptEditor", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "libs/codemirror/lib/codemirror",
    "libs/codemirror/mode/groovy/groovy",
    "libs/codemirror/mode/javascript/javascript",
    "libs/codemirror/addon/display/placeholder",
    "org/forgerock/openidm/ui/admin/delegates/ScriptDelegate",
    "org/forgerock/openidm/ui/admin/util/WorkflowWidget"
], function(AbstractView, validatorsManager, codeMirror, groovyMode, jsMode, placeHolder, ScriptDelegate, WorkflowWidget) {
    var seInstance = {},
        InlineScriptEditor = AbstractView.extend({
            template: "templates/admin/util/ScriptEditorView.html",
            noBaseTemplate: true,
            events: {
                "change input[type='radio']" : "localScriptChange",
                "change .event-select" : "changeRenderMode",
                "click .add-passed-variables" : "addPassedVariable",
                "click .passed-variables-holder .remove-btn" : "deletePassedVariable",
                "blur .passed-variables-holder input" : "passedVariableBlur",
                "onValidate": "onValidate",
                "click .script-tabs button" : "changeScriptTab",
                "customValidate": "customValidate"
            },
            model : {
                scriptData: null,
                eventName: null,
                disablePassedVariable: false,
                setScriptHook: null,
                onBlur: null,
                onChange: null,
                onFocus:null,
                validationCallback: null,
                placeHolder: null,
                codeMirrorHeight: "240px",
                disableValidation: true,
                hasWorkflow: false,
                workflowActive: false
            },

            /*
             Properties that can be set through args:

             scriptData - Set if you have script data from a previous save or want a default
             eventName - Name to display also needed as a unique ID when more then one editor is on the page
             disablePassedVariable - Flag to turn on and off passed variables
             disableValidation - turn off validation
             placeHolder - A placeholder for code mirror
             onBlur - Blur event for code mirror and file
             onChange - Change event for code mirror and file
             onFocus - focus event for code mirror and file
             onKeypress - keypress event for code mirror and file
             onLoadComplete - Load complete event
             onBlurPassedVariable - Event fired when blurring from a passed variable
             onDeletePassedVariable - Event fired when a passed variable is deleted
             onAddPassedVariable - Event fired when a passed variable is added
             showPreview - enable preview code pane
             extendedPassVariables - Passed variables to extend when saving.
             hasWorkflow - boolean to turn on/off workflow piece
             workflowContext - a JSON object depicting the context for the workflow scripts(optional)
             */
            render: function (args, callback) {
                this.element = args.element;

                this.model = _.extend(this.model, args);

                this.data = _.pick(this.model, 'hasWorkflow', 'disableValidation', 'showPreview', 'scriptData', 'eventName', 'disablePassedVariable', 'placeHolder');
                this.data.defaultToScript = false;

                if (this.model.hasWorkflow && this.data.scriptData && this.data.scriptData.file === "workflow/triggerWorkflowGeneric.js") {
                    this.model.workflowActive = true;
                } else {
                    this.data.defaultToScript = true;
                }

                if (!this.model.disablePassedVariable && this.model.scriptData) {
                    if(args.scriptData.globals === null) {
                        args.scriptData.globals = {};
                    }
                    this.data.passedVariables = args.scriptData.globals ||
                    _.omit(args.scriptData, "file", "source", "type");
                }

                this.parentRender(_.bind(function() {
                    var mode,
                        workflowName,
                        workflowParams;

                    mode = this.$el.find("select").val();

                    if (mode === "text/javascript") {
                        mode = "javascript";
                    }

                    if(this.model.showPreview) {
                        this.$el.find(".preview-pane .preview-button").bind("click", _.bind(this.previewScript, this));
                    }

                    this.cmBox = codeMirror.fromTextArea(this.$el.find(".scriptSourceCode")[0], {
                        lineNumbers: true,
                        autofocus: true,
                        viewportMargin: Infinity,
                        mode: mode
                    });


                    if (this.data.scriptData && this.data.scriptData.file === "workflow/triggerWorkflowGeneric.js") {
                        workflowName = this.data.scriptData.globals.workflowName;
                        workflowParams = this.data.scriptData.globals.params;
                    }
                    if (this.model.hasWorkflow) {
                        this.workflow = WorkflowWidget.generateWorkflowWidget({
                            "element": this.$el.find(".workflow-body"),
                            "key": workflowName,
                            "params": workflowParams,
                            "sync": false,
                            "context": this.model.workflowContext,
                            "changeCallback": _.noop
                        }, _.bind(function() {
                            this.customValidate();
                        }, this));
                    }
                    this.cmBox.setSize(this.model.codeMirrorWidth, this.model.codeMirrorHeight);

                    if (this.$el.find("input[name=scriptType]:checked").val() !== "inline-code") {
                        this.cmBox.setOption("readOnly", "nocursor");
                        this.$el.find(".inline-code").toggleClass("code-mirror-disabled");
                    }

                    if (this.data.scriptData && this.data.scriptData.source) {
                        this.$el.find(".inline-heading input[type='radio']").trigger("change");
                    }

                    if (!this.data.disableValidation) {
                        validatorsManager.bindValidators(this.$el);
                        this.$el.find(":input").trigger("check");
                    }

                    this.cmBox.on("focus", _.bind(function (cm, changeObject) {
                        this.saveEvent(this.model.onFocus, cm, changeObject);
                    }, this));

                    this.cmBox.on("change", _.bind(function (cm, changeObject) {
                        this.saveEvent(this.model.onChange, cm, changeObject);

                        if (!this.data.disableValidation) {
                            this.$el.find(".scriptSourceCode").trigger("blur");
                        }
                    }, this));

                    this.cmBox.on("blur", _.bind(function (cm, changeObject) {
                        this.saveEvent(this.model.onBlur, cm, changeObject);
                    }, this));

                    this.cmBox.on("keypress", _.bind(function (cm, changeObject) {
                        this.saveEvent(this.model.onKeypress, cm, changeObject);
                    }, this));

                    if (this.model.onKeypress) {
                        this.$el.find("input:radio, .scriptFilePath").bind("keypress", _.bind(function () {
                            this.model.onKeypress();
                        }, this));
                    }

                    if (this.model.onFocus) {
                        this.$el.find("input:radio, .scriptFilePath").bind("focus", _.bind(function () {
                            this.model.onFocus();
                        }, this));
                    }

                    if (this.model.onChange) {
                        this.$el.find("input:radio, .scriptFilePath").bind("change", _.bind(function () {
                            this.model.onChange();
                        }, this));
                    }

                    if (this.model.onBlur) {
                        this.$el.find("input:radio, .scriptFilePath").bind("blur", _.bind(function () {
                            this.model.onBlur();
                        }, this));
                    }

                    if (this.model.onChange){
                        this.$el.find(".event-select").bind("change", _.bind(function(){

                            this.model.onChange();

                        }, this));
                    }

                    if (this.model.onLoadComplete) {
                        this.model.onLoadComplete();
                    }

                    if (callback) {
                        callback();
                    }
                }, this));
            },

            changeScriptTab: function(event) {
                var currentTab = $(event.currentTarget),
                    tabType = currentTab.attr("aria-controls");

                currentTab.parent().find("button").toggleClass("active", false);
                currentTab.toggleClass("active", true);

                if (tabType === "Workflow") {
                    this.model.workflowActive = true;

                    this.$el.find(".script-body").hide();
                    this.$el.find(".workflow-body").show();
                } else {
                    this.model.workflowActive = false;

                    this.$el.find(".script-body").show();
                    this.$el.find(".workflow-body").hide();
                }

                this.customValidate();
            },

            previewScript : function() {
                var script = this.generateScript();

                this.$el.find(".preview-pane .preview-results").html("");

                if (script !== null) {
                    this.$el.find(".script-eval-message").hide();

                    ScriptDelegate.evalScript(this.generateScript()).then(_.bind(function(result){
                            this.$el.find(".preview-pane .preview-results").html("<pre>" +result +"</pre>");
                        }, this),
                        _.bind(function(result){
                            this.$el.find(".script-eval-message").show();
                            this.$el.find(".script-eval-message .message").html(result.responseJSON.message);
                        }, this));
                } else {
                    this.$el.find(".script-eval-message").show();
                    this.$el.find(".script-eval-message .message").html("Please enter script details");
                }
            },

            saveEvent: function(callback, cm, changeObject) {
                this.cmBox.save();

                if (callback) {
                    callback(cm, changeObject);
                }
            },

            localScriptChange: function (event) {
                var currentSelection;

                this.scriptSelect(event, this.cmBox);

                if (!this.data.disableValidation) {

                    if (this.data.eventName) {
                        currentSelection = this.$el.find("input[name=" + this.data.eventName + "_scriptType]:checked").val();
                    } else {
                        currentSelection = this.$el.find("input[name=scriptType]:checked").val();
                    }

                    if (currentSelection === "file-code") {
                        this.$el.find(".scriptFilePath").attr("data-validator-event", "keyup blur focus check").attr("data-validator", "required");

                        this.$el.find(".scriptSourceCode").removeAttr("data-validation-status").removeAttr("data-validator-event").removeAttr("data-validator").unbind("blur").unbind("check");
                    } else {
                        this.$el.find(".scriptSourceCode").attr("data-validator-event", "keyup blur focus check").attr("data-validator", "required");

                        this.$el.find(".scriptFilePath").removeAttr("data-validator-event").removeAttr("data-validation-status").removeAttr("data-validator").unbind("blur").unbind("focus").unbind("check").unbind("keyup");
                    }

                    validatorsManager.bindValidators(this.$el);
                    this.$el.find(":input").trigger("check");
                }
            },

            refresh: function() {
                this.cmBox.refresh();
            },

            //If either the file name or inline script are empty this function will return null
            generateScript: function() {
                var currentSelection,
                    scriptObject = {},
                    inputs,
                    emptyCheck = false;

                if (this.data.eventName) {
                    currentSelection = this.$el.find("input[name=" + this.data.eventName + "_scriptType]:checked").val();
                } else {
                    currentSelection = this.$el.find("input[name=scriptType]:checked").val();
                }

                if (currentSelection === "none") {
                    return null;
                } else if (this.model.hasWorkflow && this.model.workflowActive) {

                    if (this.model.setScript) {
                        this.model.setScript({"scriptObject":this.workflow.getConfiguration(), "hookType":currentSelection});
                    }

                    return this.workflow.getConfiguration();
                } else {
                    scriptObject.type = this.$el.find("select").val();
                    scriptObject.globals = {};

                    if (currentSelection === "file-code") {
                        scriptObject.file = this.$el.find("input[type='text']").val();

                        if(scriptObject.file.length === 0) {
                            emptyCheck = true;
                        }

                    } else {
                        scriptObject.source = this.$el.find("textarea").val();

                        if(scriptObject.source.length === 0) {
                            emptyCheck = true;
                        }
                    }

                    _.each(this.$el.find(".passed-variable-block:visible"), function (passedBlock) {
                        inputs = $(passedBlock).find("input[type=text]");
                        if($(inputs[0]).val().length > 0) {
                            scriptObject.globals[$(inputs[0]).val()] = $(inputs[1]).val();
                        }
                    }, this);

                    scriptObject.globals = _.extend(scriptObject.globals, this.extendedPassVariables);

                    if (emptyCheck) {
                        return null;
                    } else {
                        if(this.model.setScript) {
                            this.model.setScript({"scriptObject":scriptObject, "hookType":currentSelection});
                        }

                        return scriptObject;
                    }
                }
            },

            passedVariableBlur: function() {
                if (this.model.onBlurPassedVariable) {
                    this.model.onBlurPassedVariable(event);
                }
            },

            customValidate: function() {
                this.validationResult = validatorsManager.formValidated(this.$el.find(".script-body"));

                if (this.model.hasWorkflow && this.model.workflowActive) {
                    this.validationResult = this.workflow.isValid();
                }

                if (this.model.validationCallback) {
                    this.model.validationCallback(this.validationResult);
                }
            },

            getValidation: function() {
                return this.validationResult;
            },

            changeRenderMode : function(event) {
                var mode = $(event.target).val();

                if (mode === "text/javascript") {
                    mode = "javascript";
                }

                if (this.model.onChange){
                    this.model.onChange();
                }

                this.cmBox.setOption("mode", mode);
            },

            scriptSelect: function(event, codeMirror) {
                event.preventDefault();

                var targetEle = event.target,
                    filePath = this.$el.find(".scriptFilePath"),
                    sourceCode = this.$el.find(".scriptSourceCode");

                if ($(targetEle).val() === "inline-code") {
                    this.setSelectedScript(filePath, sourceCode);
                    this.cmBox.setOption("readOnly", "");
                    this.$el.find(".inline-code").toggleClass("code-mirror-disabled", false);
                    codeMirror.refresh();
                    codeMirror.focus();
                } else {
                    this.setSelectedScript(sourceCode, filePath);
                    this.cmBox.setOption("readOnly", "nocursor");
                    this.$el.find(".inline-code").toggleClass("code-mirror-disabled", true);
                    filePath.focus();
                }
            },

            setSelectedScript: function(disabledScript, enabledScript) {
                disabledScript.closest(".panel-body").slideToggle();
                disabledScript.prop("disabled", true);
                disabledScript.toggleClass("invalid", false);

                enabledScript.prop("disabled", false);
                enabledScript.closest(".panel-body").slideToggle();
            },

            addPassedVariable: function() {
                var field,
                    inputs;

                field = this.$el.find(".hidden-passed-variable:first").clone();

                inputs = field.find('input[type=text]');
                inputs.val("");

                field.find(".passed-key").attr("data-validator-event", "keyup blur check").attr("data-validator", "required");
                field.show();

                this.$el.find('.passed-variables-holder').append(field);

                if (!this.data.disableValidation) {
                    validatorsManager.bindValidators(field);
                    this.$el.find(":input").trigger("check");
                }

                if (this.model.onAddPassedVariable) {
                    this.model.onAddPassedVariable();
                }
            },

            deletePassedVariable: function(event) {
                var clickedEle = $(event.target).parents(".passed-variable-block");

                clickedEle.remove();

                if (!this.data.disableValidation) {
                    validatorsManager.bindValidators(this.$el.find(".passed-variables-holder"));
                    this.$el.find(":input").trigger("check");

                    this.validationResult = validatorsManager.formValidated(this.$el);
                }

                if (this.model.onDeletePassedVariable) {
                    this.model.onDeletePassedVariable();
                }
            }
        });

    seInstance.generateScriptEditor = function(loadingObject, callback) {
        var editor = {};

        $.extend(true, editor, new InlineScriptEditor());

        editor.render(loadingObject, callback);

        return editor;
    };

    return seInstance;
});