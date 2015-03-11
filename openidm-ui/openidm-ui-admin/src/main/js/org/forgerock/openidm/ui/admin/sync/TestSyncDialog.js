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

/*global define, $, _, Handlebars, window*/

define("org/forgerock/openidm/ui/admin/sync/TestSyncDialog", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/admin/delegates/SearchDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/admin/sync/TestSyncGridView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "bootstrap-dialog"
], function (AbstractView, searchDelegate, conf, TestSyncGridView, uiUtils, BootstrapDialog) {
    var TestSyncDialog = AbstractView.extend({
        template: "templates/admin/sync/TestSyncDialogTemplate.html",
        data: {},
        element: "#dialogs",
        events: {},
        render: function (args, callback) {
            var btns = [],
                _this = this;

            this.data = _.extend(this.data,args);

            this.dialogContent = $('<div id="testSyncDialog"></div>');
            this.setElement(this.dialogContent);
            $('#dialogs').append(this.dialogContent);

            this.currentDialog = new BootstrapDialog({
                title: $.t("templates.sync.testSync.title"),
                type: BootstrapDialog.TYPE_DEFAULT,
                size: BootstrapDialog.SIZE_WIDE,
                message: this.dialogContent,
                onshown : _.bind(function() {
                    uiUtils.renderTemplate(
                        this.template,
                        this.$el,
                        _.extend({}, conf.globalData, this.data),
                        _.bind(function() {
                            TestSyncGridView.render(this.data);
                        }, this),
                        "replace"
                    );
                }, _this),
                onhide : _.bind(function () {
                    if (this.currentDialog) {
                        delete conf.globalData.testSyncSource;
                    }
                }, _this),
                buttons: [
                    {
                        label: $.t("common.form.cancel"),
                        action: function(dialogRef) {
                            if (callback) {
                                callback(false);
                            }

                            delete conf.globalData.testSyncSource;
                            dialogRef.close();
                        }
                    }
                ]
            });

            this.currentDialog.realize();
            this.currentDialog.open();
        }
    });

    return new TestSyncDialog();
});