/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
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

/*global define */

define("org/forgerock/openidm/ui/admin/mapping/association/correlationQuery/CorrelationQueryDialog", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/admin/mapping/association/correlationQuery/CorrelationQueryBuilderView",
    "bootstrap-dialog"
], function($, _,
            AbstractView,
            conf,
            uiUtils,
            CorrelationQueryBuilderView,
            BootstrapDialog) {

    var CorrelationQueryDialog = AbstractView.extend({
        template: "templates/admin/mapping/association/correlationQuery/CorrelationQueryDialogTemplate.html",
        el: "#dialogs",
        events: {
        },
        model: {},

        render: function(args, callback) {
            var _this = this;

            this.model.saveCallback = callback;
            this.model.currentDialog = $('<div id="CorrelationQueryDialog"></div>');
            this.setElement(this.model.currentDialog);
            $('#dialogs').append(this.model.currentDialog);

            BootstrapDialog.show({
                title: "Correlation Query",
                size: BootstrapDialog.SIZE_WIDE,
                type: BootstrapDialog.TYPE_DEFAULT,
                message: this.model.currentDialog,
                onshown : function (dialogRef) {
                    uiUtils.renderTemplate(
                        _this.template,
                        _this.$el,
                        _.extend({}, conf.globalData, this.data),
                        _.bind(function() {
                            args.validation = _.bind(function(valid) {
                                if (valid) {
                                    dialogRef.getButton('submitQueryDialog').enable();
                                    this.$el.parent().find("#correlationQueryWarning").hide();
                                } else {
                                    dialogRef.getButton('submitQueryDialog').disable();
                                    this.$el.parent().find("#correlationQueryWarning").show();
                                }
                            }, this);

                            CorrelationQueryBuilderView.render(args);

                            var copy = this.$el.find("#correlationQueryWarning").clone();
                            this.$el.find("#correlationQueryWarning").remove();
                            this.$el.parent().find(".ui-dialog-buttonpane").prepend(copy);

                        }, _this),
                        "replace");

                    dialogRef.getButton('submitQueryDialog').disable();
                },
                buttons: [
                    {
                        label: $.t("common.form.cancel"),
                        action: function(dialogRef) {
                            dialogRef.close();
                        }
                    },
                    {
                        label: $.t("common.form.submit"),
                        id: "submitQueryDialog",
                        cssClass: "btn-primary",
                        action: function(dialogRef){
                            if (_this.model.saveCallback) {
                                _this.model.saveCallback(CorrelationQueryBuilderView.getQuery());
                            }

                            dialogRef.close();
                        }
                    }]
            });
        }
    });

    return new CorrelationQueryDialog();
});
