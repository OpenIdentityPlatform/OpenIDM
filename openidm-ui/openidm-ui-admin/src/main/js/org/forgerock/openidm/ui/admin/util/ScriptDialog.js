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
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/ValidatorsManager"
], function(AbstractView, constants, conf, uiUtils, validatorsManager) {
    var ScriptDialog= AbstractView.extend({
        template: "templates/admin/util/ScriptDialog.html",
        noBaseTemplate: true,
        element: "#dialogs",
        events: {
            "onValidate": "onValidate",
            "change input[type='radio']" : "scriptSelect",
            "click #addPassedVariables" : "addPassedVariable",
            "click #passedVariablesHolder .remove-btn" : "deletePassedVariable",
            "customValidate": "customValidate"
        },

        render: function (args, callback) {
            var btns;

            this.data = {};

            if(args.scriptData) {
                this.data.scriptData = args.scriptData;
                this.data.passedVariables = _.chain(args.scriptData)
                    .omit("file", "name", "source", "type")
                    .pairs()
                    .value();
            } else {
                this.data.scriptData = null;
            }

            if(args.setScript) {
                this.data.setScript = args.setScript;
            } else {
                this.data.setScript = null;
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
                height:"600",
                modal: true,
                resizable: false,
                draggable: false,
                buttons: btns,
                position: { my: "center", at: "center", of: window },
                close: _.bind(function () {
                    if(this.currentDialog) {
                        this.currentDialog.dialog('destroy').remove();
                    }
                }, this),
                open: _.bind(function() {
                    uiUtils.renderTemplate(this.template, this.$el, _.extend({}, conf.globalData, this.data), _.bind(function(){
                        validatorsManager.bindValidators(this.$el);
                        validatorsManager.validateAllFields(this.$el);
                    }, this), "replace");
                }, this)
            });

            if(callback) {
                callback();
            }
        },

        scriptSelect: function(event) {
            event.preventDefault();

            var targetEle = event.target,
                filePath = this.$el.find("#scriptFilePath"),
                sourceCode = this.$el.find("#scriptSourceCode");

            if($(targetEle).val() === "inline-code") {
                this.setSelectedScript(filePath, sourceCode);
                sourceCode.focus();
            } else {
                this.setSelectedScript(sourceCode, filePath);
                filePath.focus();
            }
        },

        setSelectedScript: function(disabledScript, enabledScript) {
            disabledScript.prop("disabled", true);
            disabledScript.removeAttr("data-validator-event");
            disabledScript.removeAttr("data-validation-status");
            disabledScript.removeAttr("data-validator");
            disabledScript.unbind("blur");
            disabledScript.parent().find(".error").hide();
            disabledScript.parent().find(".validation-message").hide();
            disabledScript.toggleClass("invalid", false);

            enabledScript.prop("disabled", false);
            enabledScript.attr("data-validator-event","keyup blur");
            enabledScript.attr("data-validator","required");


            validatorsManager.bindValidators(this.$el);
            validatorsManager.validateAllFields(this.$el);
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

        addPassedVariable: function() {
            var field,
                inputs;

            field = this.$el.find("#hiddenPassedVariable").clone();
            field.removeAttr("id");

            inputs = field.find('input[type=text]');
            inputs.val("");
            $(inputs).attr("data-validator-event","keyup blur");
            $(inputs).attr("data-validator","bothRequired");

            field.show();

            this.$el.find('#passedVariablesHolder').append(field);

            validatorsManager.bindValidators(this.$el);
            validatorsManager.validateAllFields(this.$el);
        },

        deletePassedVariable: function(event) {
            var clickedEle = $(event.target).parents(".passed-variable-block");

            clickedEle.remove();

            validatorsManager.bindValidators(this.$el);
            validatorsManager.validateAllFields(this.$el);
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

