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

define("org/forgerock/openidm/ui/admin/util/ScriptDialog", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor",
    "bootstrap-dialog"
], function($, _, AbstractView, conf, uiUtils, validatorsManager, InlineScriptEditor,  BootstrapDialog) {
    var ScriptDialog= AbstractView.extend({
        element: "#dialogs",
        events: {

        },
        data : {

        },

        render: function (args, callback) {
            var _this = this;

            this.currentDialog = $('<div id="scriptManagerDialogForm"></div>');

            $('#dialogs').append(this.currentDialog);

            this.setElement(this.currentDialog);

            BootstrapDialog.show({
                title: "Script Manager",
                type: BootstrapDialog.TYPE_DEFAULT,
                message: this.currentDialog,
                size: BootstrapDialog.SIZE_WIDE,
                cssClass: "script-large-dialog",
                onshown : function (dialogRef) {
                    args.element = _this.$el;
                    args.validationCallback = _.bind(function(result){
                        if(result) {
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

                        if (args.saveCallback) {
                            args.saveCallback();
                        }

                        dialogRef.close();
                    }, _this)
                }]
            });
        },

        getInlineEditor: function() {
            return this.scriptEditor.getInlineEditor();
        },

        generateScript: function() {
            return this.scriptEditor.generateScript();
        }
    });

    return new ScriptDialog();
});
