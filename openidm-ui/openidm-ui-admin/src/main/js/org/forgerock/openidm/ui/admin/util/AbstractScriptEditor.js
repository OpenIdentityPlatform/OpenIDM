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

define("org/forgerock/openidm/ui/admin/util/AbstractScriptEditor", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/ValidatorsManager"
], function(AbstractView, constants, conf, uiUtils, validatorsManager) {
    var AbstractScriptEditor = AbstractView.extend({
        template: "templates/admin/util/AbstractScriptView.html",
        noBaseTemplate: true,

        changeRenderMode : function(event) {
            var mode = $(event.target).val();

            if(mode === "text/javascript") {
                mode = "javascript";
            }

            this.cmBox.setOption("mode", mode);
        },

        scriptSelect: function(event) {
            event.preventDefault();

            var targetEle = event.target,
                filePath = this.$el.find(".scriptFilePath"),
                sourceCode = this.$el.find(".scriptSourceCode");

            if($(targetEle).val() === "inline-code") {
                this.setSelectedScript(filePath, sourceCode);
                this.cmBox.setOption("readOnly", "");
                this.$el.find(".inline-code").toggleClass("code-mirror-disabled", false);
                this.cmBox.focus();
            } else {
                this.setSelectedScript(sourceCode, filePath);
                this.cmBox.setOption("readOnly", "nocursor");
                this.$el.find(".inline-code").toggleClass("code-mirror-disabled", true);
                filePath.focus();
            }
        },

        setSelectedScript: function(disabledScript, enabledScript) {
            disabledScript.prop("disabled", true);
            disabledScript.toggleClass("invalid", false);

            enabledScript.prop("disabled", false);

            if(!this.data.noValidation) {
                disabledScript.removeAttr("data-validator-event");
                disabledScript.removeAttr("data-validation-status");
                disabledScript.removeAttr("data-validator");
                disabledScript.unbind("blur");
                disabledScript.parent().find(".error").hide();
                disabledScript.parent().find(".validation-message").hide();

                enabledScript.attr("data-validator-event","keyup blur");
                enabledScript.attr("data-validator","required");

                validatorsManager.bindValidators(this.$el);
                validatorsManager.validateAllFields(this.$el);
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
        }
    });

    return AbstractScriptEditor;
});


