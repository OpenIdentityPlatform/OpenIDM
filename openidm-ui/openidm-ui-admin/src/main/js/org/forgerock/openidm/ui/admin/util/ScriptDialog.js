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
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor",
    "bootstrap-dialog"
], function(AbstractView, conf, uiUtils, validatorsManager, InlineScriptEditor,  BootstrapDialog) {
    var ScriptDialog= AbstractView.extend({
        element: "#dialogs",
        events: {

        },
        data : {

        },

        render: function (args, callback) {
            var _this = this;

            this.currentDialog = $('<form id="scriptManagerDialogForm"></form>');

            $('#dialogs').append(this.currentDialog);

            this.setElement(this.currentDialog);

            BootstrapDialog.show({
                title: "Script Manager",
                type: BootstrapDialog.TYPE_DEFAULT,
                message: this.currentDialog,
                size: BootstrapDialog.SIZE_WIDE,
                onshown : function (dialogRef) {
                    args.element = _this.$el;
                    args.validationCallback = _.bind(function(){

                        if(validatorsManager.formValidated(this.$el)) {
                            $("#scriptDialogOkay").prop("disabled", false);
                        } else {
                            $("#scriptDialogOkay").prop("disabled", true);
                        }
                    }, _this);

                    _this.scriptEditor = InlineScriptEditor.generateScriptEditor(args, _.bind(function(){
                        if(callback) {
                            callback();
                        }
                    },  _this));
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
        },

        generateScript: function() {
            return this.scriptEditor.generateScript();
        }
    });

    return new ScriptDialog();
});