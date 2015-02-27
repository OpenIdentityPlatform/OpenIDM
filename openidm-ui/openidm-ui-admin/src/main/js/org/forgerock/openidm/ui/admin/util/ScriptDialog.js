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
    "libs/codemirror/mode/javascript/javascript",
    "bootstrap-dialog"
], function(AbstractScriptEditor, constants, conf, uiUtils, validatorsManager, codeMirror, groovyMode, jsMode, BootstrapDialog) {
    var ScriptDialog= AbstractScriptEditor.extend({
        element: "#dialogs",
        events: {
            "onValidate": "onValidate",
            "change input[type='radio']" : "localScriptChange",
            "click #addPassedVariables" : "addPassedVariable",
            "click #passedVariablesHolder .remove-btn" : "deletePassedVariable",
            "customValidate": "customValidate",
            "change .event-select" : "changeRenderMode"
        },
        model : {
            setScript: null,
            scriptData: null,
            noValidation: false,
            disablePassedVariable: false,
            placeHolder: null,
            codeMirrorHeight: "240px"
        },

        render: function (args, callback) {
            var _this = this;

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

            this.currentDialog = $('<form id="scriptManagerDialogForm"></form>');

            $('#dialogs').append(this.currentDialog);
            this.setElement(this.currentDialog);

            BootstrapDialog.show({
                title: "Script Manager",
                type: BootstrapDialog.TYPE_DEFAULT,
                message: this.currentDialog,
                size: BootstrapDialog.SIZE_WIDE,
                onshown : function (dialogRef) {
                    uiUtils.renderTemplate(_this.template, _this.$el, _.extend({}, conf.globalData, _this.data), _.bind(function () {
                        var mode = this.$el.find("select").val();

                        if (mode === "text/javascript") {
                            mode = "javascript";
                        }

                        this.cmBox = codeMirror.fromTextArea(_this.$el.find(".scriptSourceCode")[0], {
                            lineNumbers: true,
                            mode: mode
                        });

                        this.cmBox.setSize(this.model.codeMirrorWidth, this.model.codeMirrorHeight);

                        this.cmBox.on("changes", _.bind(function () {
                            this.cmBox.save();
                            this.$el.find(".scriptSourceCode").trigger("blur");
                        }, this));

                        if (this.$el.find("input[name=scriptType]:checked").val() !== "inline-code") {
                            this.cmBox.setOption("readOnly", "nocursor");
                            this.$el.find(".inline-code").toggleClass("code-mirror-disabled");
                        }

                        validatorsManager.bindValidators(this.$el);
                        validatorsManager.validateAllFields(this.$el);

                        if (this.data.scriptData && this.data.scriptData.source) {
                            this.$el.find("#inlineHeading input[type='radio']").trigger("change");
                        }
                    }, _this), "replace");
                },
                buttons: [{
                    label: $.t("common.form.cancel"),
                    id:"scriptDialogCancel",
                    action: function(dialogRef) {
                        dialogRef.close();
                    }
                }, {
                    label: $.t('common.form.ok'),
                    id: "scriptDialogOkay",
                    cssClass: "btn-primary",
                    action: _.bind(function(dialogRef) {
                        this.generateScript();
                        if (this.saveCallback) {
                            this.saveCallback();
                        }
                        dialogRef.close();
                    }, _this)
                }]
            });

            if(callback) {
                callback();
            }
        },

        localScriptChange: function (event) {
            this.scriptSelect(event, this.cmBox);
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