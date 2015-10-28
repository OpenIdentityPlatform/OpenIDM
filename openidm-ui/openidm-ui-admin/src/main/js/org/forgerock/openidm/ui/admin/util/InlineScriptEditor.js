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
 * Copyright 2014-2015 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/admin/util/InlineScriptEditor", [
    "jquery",
    "underscore",
    "jsonEditor",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "libs/codemirror/lib/codemirror",
    "libs/codemirror/mode/groovy/groovy",
    "libs/codemirror/mode/javascript/javascript",
    "libs/codemirror/addon/display/placeholder",
    "org/forgerock/openidm/ui/admin/delegates/ScriptDelegate",
    "org/forgerock/openidm/ui/admin/util/WorkflowWidget"
], function($, _, JSONEditor,
            AbstractView,
            validatorsManager,
            codeMirror,
            groovyMode,
            jsMode,
            placeHolder,
            ScriptDelegate,
            WorkflowWidget) {
    var seInstance = {},
        InlineScriptEditor = AbstractView.extend({
            template: "templates/admin/util/ScriptEditorView.html",
            noBaseTemplate: true,
            events: {
                "change input[type='radio']" : "localScriptChange",
                "change .event-select" : "changeRenderMode",
                "click .add-passed-variables" : "addEmptyPassedVariable",
                "click .passed-variables-holder .btn-delete-attribute" : "deletePassedVariable",
                "blur .passed-variables-holder input" : "passedVariableBlur",
                "onValidate": "onValidate",
                "click .script-tabs button" : "changeScriptTab",
                "customValidate": "customValidate"
            },
            model : {
                autoFocus: true,
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
                workflowActive: false,
                editors: [],
                passedVariables: [],
                codeMirrorValid: false
            },

            /*
             Properties that can be set through args:

             autoFocus - Tells the script widget to focus itself once loaded (defaults to true)
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

                    this.model.passedVariables = args.scriptData.globals || _.omit(args.scriptData, "file", "source", "type");
                }

                this.parentRender(_.bind(function() {
                    var mode,
                        workflowName,
                        workflowParams,
                        currentScriptSelection;

                    mode = this.$el.find("select").val();

                    if (mode === "text/javascript") {
                        mode = "javascript";
                    }

                    if(this.model.showPreview) {
                        this.$el.find(".preview-pane .preview-button").bind("click", _.bind(this.previewScript, this));
                    }

                    this.cmBox = codeMirror.fromTextArea(this.$el.find(".scriptSourceCode")[0], {
                        lineNumbers: true,
                        autofocus: this.model.autoFocus,
                        viewportMargin: Infinity,
                        theme: "forgerock",
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

                    if (this.data.eventName) {
                        currentScriptSelection = this.$el.find("input[name=" + this.data.eventName + "_scriptType]:checked").val();
                    } else {
                        currentScriptSelection = this.$el.find("input[name=scriptType]:checked").val();
                    }

                    if (currentScriptSelection !== "inline-code") {
                        this.cmBox.setOption("readOnly", "nocursor");
                        this.$el.find(".inline-code").toggleClass("code-mirror-disabled");
                    }

                    if (this.data.scriptData && this.data.scriptData.file) {
                        this.$el.find(".inline-heading input[type='radio']").trigger("change");
                    }

                    if (!this.data.disableValidation) {
                        validatorsManager.bindValidators(this.$el.find("form"));
                        this.$el.find(":input").trigger("check");
                    }

                    this.cmBox.on("focus", _.bind(function (cm, changeObject) {
                        this.saveEvent(this.model.onFocus, cm, changeObject);
                    }, this));

                    this.cmBox.on("change", _.bind(function (cm, changeObject) {
                        this.saveEvent(this.model.onChange, cm, changeObject);

                        if (!this.data.disableValidation) {

                            if(cm.getValue().length > 0) {
                                this.model.codeMirrorValid = true;
                            } else {
                                this.model.codeMirrorValid = false;
                            }

                            this.customValidate();
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

                    //Load up passed variables
                    _.each(this.model.passedVariables, function(value, key){
                        this.addPassedVariable(key, value);
                    }, this);

                    if(this.cmBox.getValue().length > 0) {
                        this.model.codeMirrorValid = true;
                    }

                    this.customValidate();

                    this.model.autoFocus = true;

                    if (callback) {
                        callback();
                    }
                }, this));
            },

            getInlineEditor: function() {
                return this.cmBox;
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
                        this.model.codeMirrorValid = false;

                        if (this.model.onBlur) {
                            this.$el.find(".scriptFilePath").bind("blur", _.bind(function () {
                                this.model.onBlur();
                            }, this));
                        }
                    } else {
                        if (this.cmBox.getValue().length > 0) {
                            this.model.codeMirrorValid = true;
                        } else {
                            this.model.codeMirrorValid = false;
                        }
                    }

                    this.customValidate();
                }
            },

            refresh: function() {
                this.cmBox.refresh();
            },

            //If either the file name or inline script are empty this function will return null
            generateScript: function() {
                var currentSelection,
                    scriptObject = {},
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

                    _.each(this.model.editors, function (jsonEditor) {
                        if(jsonEditor.row.find(".passed-variable-name input").val().length > 0) {
                            scriptObject.globals[jsonEditor.row.find(".passed-variable-name input").val()] = jsonEditor.editor.getValue();
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

                if(this.model.validationCallback) {
                    if (this.$el.find(".scriptFilePath:visible").length > 0 && this.validationResult) {
                        this.model.validationCallback(true);
                    } else if (this.model.codeMirrorValid) {
                        this.model.validationCallback(true);
                    } else {
                        this.model.validationCallback(false);
                    }
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

                var currentSelection,
                    filePath = this.$el.find(".scriptFilePath"),
                    sourceCode = this.$el.find(".scriptSourceCode");

                if (this.data.eventName) {
                    currentSelection = this.$el.find("input[name=" + this.data.eventName + "_scriptType]:checked").val();
                } else {
                    currentSelection = this.$el.find("input[name=scriptType]:checked").val();
                }

                if (currentSelection === "inline-code") {
                    this.setSelectedScript(filePath, sourceCode);
                    this.cmBox.setOption("readOnly", "");
                    this.$el.find(".inline-code").toggleClass("code-mirror-disabled", false);
                    codeMirror.refresh();

                    if(this.model.autoFocus) {
                        codeMirror.focus();
                    }
                } else {
                    this.setSelectedScript(sourceCode, filePath);
                    this.cmBox.setOption("readOnly", "nocursor");
                    this.$el.find(".inline-code").toggleClass("code-mirror-disabled", true);

                    if(this.model.autoFocus) {
                        filePath.focus();
                    }
                }
            },

            setSelectedScript: function(disabledScript, enabledScript) {
                disabledScript.closest(".panel-body").hide();
                disabledScript.prop("disabled", true);
                disabledScript.toggleClass("invalid", false);

                enabledScript.prop("disabled", false);
                enabledScript.closest(".panel-body").show();


                if (!this.data.disableValidation) {
                    this.customValidate();
                }
            },

            addEmptyPassedVariable: function(){
                this.addPassedVariable(null, null);
            },

            addPassedVariable: function(name, value) {
                var field,
                    editor;

                field = this.$el.find(".empty-group-item").clone();

                field.toggleClass("empty-group-item", false);

                field.show();

                this.$el.find('.passed-variables-holder .list-table-form').append(field);

                field.attr("style", "");

                if(name) {
                    field.find(".passed-variable-name input").val(name);
                }

                editor = new JSONEditor(field.find(".editor-body")[0], {
                    disable_array_reorder: true,
                    disable_collapse: true,
                    disable_edit_json: false,
                    disable_properties: false,
                    iconlib: "fontawesome4",
                    no_additional_properties: false,
                    theme: "bootstrap3",
                    schema:{}
                });

                editor.on('change', _.bind(function () {
                    this.$el.find(".compactJSON div.form-control>:input").addClass("form-control");
                }, this));

                if(value) {
                    editor.setValue(value);
                }

                this.model.editors.push({
                    row : field,
                    editor : editor
                });

                if (this.model.onAddPassedVariable) {
                    this.model.onAddPassedVariable();
                }
            },

            deletePassedVariable: function(event) {
                event.preventDefault();

                var clickedEle = $(event.target).parents(".list-group-item"),
                    index = this.$el.find(".list-group-item").index(clickedEle) - 1;

                clickedEle.remove();

                this.model.editors.splice(index, 1);

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