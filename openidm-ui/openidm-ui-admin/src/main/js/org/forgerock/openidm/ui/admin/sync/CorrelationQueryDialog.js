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

/*global define, $, _, require, window */

define("org/forgerock/openidm/ui/admin/sync/CorrelationQueryDialog", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/admin/mapping/MappingBaseView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/admin/sync/CorrelationQueryBuilderView"
], function(AbstractView, MappingBaseView, conf, uiUtils, CorrelationQueryBuilderView) {
    var CorrelationQueryDialog = AbstractView.extend({
        template: "templates/admin/sync/CorrelationQueryDialogTemplate.html",
        el: "#dialogs",
        events: {
        },
        model: {},

        render: function(args, callback) {
            this.model.saveCallback = callback;
            this.model.currentDialog = $('<div id="CorrelationQueryDialog"></div>');
            this.setElement(this.model.currentDialog);
            $('#dialogs').append(this.model.currentDialog);

            this.model.currentDialog.dialog({
                title: "Correlation Query",
                modal: true,
                resizable: false,
                draggable: true,
                dialogClass: "test",
                buttons: [
                    {
                        text: $.t('common.form.cancel'),
                        click: _.bind(function() {
                            this.model.currentDialog.dialog('close');
                        }, this)
                    },
                    {
                        text: $.t('common.form.submit'),
                        id: "correlationQuerySubmit",
                        click: _.bind(function() {
                            if (this.model.saveCallback) {
                                this.model.saveCallback(CorrelationQueryBuilderView.getQuery());
                            }

                            this.model.currentDialog.dialog('close');
                        }, this),
                        disabled: true
                    }
                ],
                maxHeight: 600,
                minHeight: 420,
                width: 700,
                position: { my: "center top+25", at: "center top+25", of: window },
                close: _.bind(function () {
                    if (this.model.currentDialog) {
                        this.data = {};
                        this.model.currentDialog.dialog('destroy').remove();
                        CorrelationQueryBuilderView.clear();
                    }
                }, this),
                open: _.bind(function(){

                    uiUtils.renderTemplate(
                        this.template,
                        this.$el,
                        _.extend({}, conf.globalData, this.data),
                        _.bind(function() {
                            args.validation = _.bind(function(valid) {
                                if (valid) {
                                    this.$el.parent().find("#correlationQuerySubmit").prop('disabled', false);
                                    this.$el.parent().find("#correlationQuerySubmit").prop('opacity', 1);
                                    this.$el.parent().find("#correlationQueryWarning").hide();
                                } else {
                                    this.$el.parent().find("#correlationQuerySubmit").prop('disabled', true);
                                    this.$el.parent().find("#correlationQueryWarning").show();
                                }
                            }, this);

                            CorrelationQueryBuilderView.render(args);

                            var copy = this.$el.find("#correlationQueryWarning").clone();
                            this.$el.find("#correlationQueryWarning").remove();
                            this.$el.parent().find(".ui-dialog-buttonpane").prepend(copy);

                        }, this),
                        "replace");
                }, this)
            });
        }
    });

    return new CorrelationQueryDialog();
});

