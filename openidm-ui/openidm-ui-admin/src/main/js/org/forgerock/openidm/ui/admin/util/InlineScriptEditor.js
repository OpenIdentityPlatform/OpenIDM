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
    "org/forgerock/openidm/ui/admin/util/AbstractScriptEditor",
    "libs/codemirror/lib/codemirror",
    "libs/codemirror/mode/groovy/groovy",
    "libs/codemirror/mode/javascript/javascript",
    "libs/codemirror/addon/display/placeholder"
], function(AbstractScriptEditor, codeMirror, groovyMode, jsMode, placeHolder) {
    var seInstance = {},
        InlineScriptEditor = AbstractScriptEditor.extend({
            noBaseTemplate: true,
            events: {
                "change input[type='radio']" : "localScriptChange",
                "change .event-select" : "changeRenderMode",
                "click #addPassedVariables" : "addPassedVariable",
                "click #passedVariablesHolder .remove-btn" : "deletePassedVariable"
            },
            model : {
                scriptData: null,
                eventName: null,
                noValidation: false,
                disablePassedVariable: false,
                onBlur: null,
                onChange: null,
                onFocus:null,
                placeHolder: null,
                codeMirrorHeight: "240px"
            },

            /*
             Args takes several properties

             scriptData - Set if you have script data from a previous save or want a default
             eventName - Name to display
             disablePassedVariable - Flag to turn on and off passed variables
             onBlur - Blur event for code mirror
             onChange - Change event for code mirror
             onFocus - focus event for code mirror
             placeHolder - A placeholder for code mirror
             */
            render: function (args, callback) {
                this.data.inlineEditor = true;

                this.element = args.element;

                this.model = _.extend(this.model, args);

                this.data = _.pick(this.model, 'scriptData', 'eventName', 'noValidation', 'disablePassedVariable', 'placeHolder');

                if (!this.model.disablePassedVariable && this.model.scriptData) {
                    this.data.passedVariables = args.scriptData.globals ||
                    _.omit(args.scriptData, "file", "source", "type");
                }

                this.parentRender(_.bind(function() {
                    var mode;

                    mode = this.$el.find("select").val();

                    if (mode === "text/javascript") {
                        mode = "javascript";
                    }

                    this.cmBox = codeMirror.fromTextArea(this.$el.find(".scriptSourceCode")[0], {
                        lineNumbers: true,
                        autofocus: true,
                        viewportMargin: Infinity,
                        mode: mode
                    });

                    this.cmBox.setSize(this.model.codeMirrorWidth, this.model.codeMirrorHeight);

                    this.cmBox.on("focus", _.bind(function (cm, changeObject) {
                        this.saveEvent(this.model.onFocus, cm, changeObject);
                    }, this));
                    this.cmBox.on("change", _.bind(function (cm, changeObject) {
                        this.saveEvent(this.model.onChange, cm, changeObject);
                    }, this));
                    this.cmBox.on("blur", _.bind(function (cm, changeObject) {
                        this.saveEvent(this.model.onBlur, cm, changeObject);
                    }, this));

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

                    if(this.model.onChange){
                        this.$el.find(".event-select").bind("change", _.bind(function(){
                            this.model.onChange();
                        }, this));
                    }

                    if(this.$el.find("input[name=scriptType]:checked").val() !== "inline-code") {
                        this.cmBox.setOption("readOnly", "nocursor");
                        this.$el.find(".inline-code").toggleClass("code-mirror-disabled");
                    }


                    if (this.data.scriptData && this.data.scriptData.source) {
                        this.$el.find("#inlineHeading input[type='radio']").trigger("change");
                    }

                    if(callback) {
                        callback();
                    }
                }, this));
            },

            saveEvent: function(callback, cm, changeObject) {
                this.cmBox.save();

                if (callback) {
                    callback(cm, changeObject);
                }
            },

            localScriptChange: function (event) {
                this.scriptSelect(event, this.cmBox);
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

                if(this.data.eventName) {
                    currentSelection = this.$el.find("input[name=" + this.data.eventName + "_scriptType]:checked").val();
                } else {
                    currentSelection = this.$el.find("input[name=scriptType]:checked").val();
                }

                if(currentSelection === "none") {
                    return null;
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

                        scriptObject.globals[$(inputs[0]).val()] = $(inputs[1]).val();
                    }, this);

                    if(emptyCheck) {
                        return null;
                    } else {
                        return scriptObject;
                    }
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