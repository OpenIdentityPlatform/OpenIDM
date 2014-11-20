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

define("org/forgerock/openidm/ui/admin/util/ScriptDialog", [
    "org/forgerock/openidm/ui/admin/util/AbstractScriptEditor",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "libs/codemirror/lib/codemirror",
    "libs/codemirror/mode/groovy/groovy",
    "libs/codemirror/mode/javascript/javascript"
], function(AbstractScriptEditor, constants, conf, uiUtils, validatorsManager, codeMirror, groovyMode, jsMode) {
    var ScriptDialog= AbstractScriptEditor.extend({
        element: "#dialogs",
        events: {
            "onValidate": "onValidate",
            "change input[type='radio']" : "scriptSelect",
            "click #addPassedVariables" : "addPassedVariable",
            "click #passedVariablesHolder .remove-btn" : "deletePassedVariable",
            "customValidate": "customValidate",
            "change .event-select" : "changeRenderMode"
        },
        model : {
            setScript: null,
            AscriptData: null,
            noValidation: false,
            disablePassedVariable: false,
            placeHolder: null,
            codeMirrorHeight: "240px",
            codeMirrorWidth: "600px"
        },


        render: function (args, callback) {
            var btns;

            this.data = {};

            this.model = _.extend(this.model, args);

            this.data = _.pick(this.model, "setScript", "scriptData");

            if(this.model.scriptData) {
                this.data.passedVariables = _.chain(args.scriptData)
                    .omit("file", "name", "source", "type")
                    .pairs()
                    .value();
            }

            if (args.saveCallback) {
                this.saveCallback = args.saveCallback;
            }

            btns = [
                {
                    id:"scriptDialogCancel",
                    text: $.t('common.form.cancel'),
                    click: _.bind(function() {
                        this.currentDialog.dialog('close');
                    }, this)
                },
                {
                    id:"scriptDialogOkay",
                    text: $.t('common.form.ok'),
                    click: _.bind(function() {
                        this.generateScript();

                        if (this.saveCallback) {
                            this.saveCallback();
                        }

                        this.currentDialog.dialog('close');
                    }, this)
                }
            ];

            this.currentDialog = $('<form id="scriptManagerDialogForm"></form>');
            this.setElement(this.currentDialog);

            $('#dialogs').append(this.currentDialog);

            this.currentDialog.dialog({
                title: "Script Manager",
                width:"650",
                modal: true,
                resizable: true,
                draggable: true,
                buttons: btns,
                position: { my: "center", at: "center", of: window },
                close: _.bind(function () {
                    if(this.currentDialog) {
                        this.currentDialog.dialog('destroy').remove();
                    }
                }, this),
                open: _.bind(function() {
                    uiUtils.renderTemplate(this.template, this.$el, _.extend({}, conf.globalData, this.data), _.bind(function(){
                        var mode;

                        mode = this.$el.find("select").val();

                        if(mode === "text/javascript") {
                            mode = "javascript";
                        }

                        this.cmBox = codeMirror.fromTextArea(this.$el.find(".scriptSourceCode")[0], {
                            lineNumbers: true,
                            mode: mode
                        });

                        this.cmBox.setSize(this.model.codeMirrorWidth, this.model.codeMirrorHeight);

                        this.cmBox.on("changes", _.bind(function() {
                            this.cmBox.save();
                            this.$el.find(".scriptSourceCode").trigger("blur");
                        }, this));

                        if(this.$el.find("input[name=scriptType]:checked").val() !== "inline-code") {
                            this.cmBox.setOption("readOnly", "nocursor");
                            this.$el.find(".inline-code").toggleClass("code-mirror-disabled");
                        }

                        validatorsManager.bindValidators(this.$el);
                        validatorsManager.validateAllFields(this.$el);
                    }, this), "replace");
                }, this)
            });

            if(callback) {
                callback();
            }
        },

        generateScript: function() {
            var currentSelection = this.$el.find("input[name=scriptType]:checked").val(),
                scriptObject = {},
                inputs;

            scriptObject.type = this.$el.find("select").val();

            if(currentSelection === "file-code") {
                scriptObject.file = this.$el.find("input[type='text']").val();
            } else {
                scriptObject.source =  this.$el.find("textarea").val();
            }

            _.each(this.$el.find(".passed-variable-block:visible"), function(passedBlock) {
                inputs = $(passedBlock).find("input[type=text]");

                scriptObject[$(inputs[0]).val()] = $(inputs[1]).val();
            }, this);

            if(this.data.setScript) {
                this.data.setScript({"scriptObject":scriptObject, "hookType":currentSelection});
            }
        },

        customValidate: function () {
            if(validatorsManager.formValidated(this.$el)) {
                $("#scriptDialogOkay").prop("disabled", false);
            } else {
                $("#scriptDialogOkay").prop("disabled", true);
            }
        }
    });

    return new ScriptDialog();
});