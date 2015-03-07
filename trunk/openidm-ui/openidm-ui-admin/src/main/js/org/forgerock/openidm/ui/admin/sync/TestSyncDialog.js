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
    "org/forgerock/commons/ui/common/util/UIUtils"
], function (AbstractView, searchDelegate, conf, TestSyncGridView, uiUtils) {
    var TestSyncDialog = AbstractView.extend({
        template: "templates/admin/sync/TestSyncDialogTemplate.html",
        data: {},
        element: "#dialogs",
        events: {},    
        render: function (args, callback) {
            var btns = [];
            this.data = _.extend(this.data,args);
            
            this.currentDialog = $('<div id="testSyncDialog"></div>');
            this.setElement(this.currentDialog);
            $('#dialogs').append(this.currentDialog);

            btns.push({
                text: $.t("common.form.cancel"),
                click: function() {
                    if (callback) {
                        callback(false);
                    }
                    delete conf.globalData.testSyncSource;
                    $("#testSyncDialog").dialog('destroy').remove();
                }
            });
            
            this.currentDialog.dialog({
                title: $.t("templates.sync.testSync.title"),
                modal: true,
                resizable: false,
                draggable: true,
                dialogClass: "testSyncDialog",
                width: 870,
                position: { my: "center top+25", at: "center top+25", of: window },
                buttons: btns,
                close: _.bind(function () {
                    if (this.currentDialog) {
                        delete conf.globalData.testSyncSource;
                        this.currentDialog.dialog('destroy').remove();
                    }
                }, this)
            });

            uiUtils.renderTemplate(
                this.template,
                this.$el,
                _.extend({}, conf.globalData, this.data),
                _.bind(function() {
                    TestSyncGridView.render(this.data);
                }, this),
                "replace"
            );
        }
    }); 
    
    return new TestSyncDialog();
});