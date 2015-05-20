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

/*global define, window, $, _ , Handlebars*/

define("org/forgerock/openidm/ui/admin/connector/ldap/LDAPFilterDialog", [
    "org/forgerock/openidm/ui/admin/util/FilterEditor",
    "ldapjs-filter",
    "bootstrap-dialog"
], function (FilterEditor, ldapjs, BootstrapDialog) {
    var LDAPFilterDialog = FilterEditor.extend({
        el: "#dialogs",
        getFilterString: function () {
            return ldapjs.serializeFilterTree(this.data.filter);
        },
        returnFilterString: function (e) {
            e.preventDefault();
            if (_.has(this.data.filter, "op") && this.data.filter.op === "none") {
                this.updatePromise.resolve("");
            } else {
                this.updatePromise.resolve(this.getFilterString());
            }

            this.currentDialog.close();
        },
        render: function (params) {
            if (typeof params.filterString === "string" && params.filterString.length) {
                this.data.filter = ldapjs.buildFilterTree(params.filterString);
            } else {
                this.data.filter = { "op": "none", "children": []};
            }
            this.data.filterString = params.filterString;
            this.updatePromise = params.promise;

            this.dialogContent = $('<div id="attributeDialog"></div>');
            this.setElement(this.dialogContent);
            $('#dialogs').append(this.dialogContent);

            this.events["click input[type=submit]"] = _.bind(this.returnFilterString, this);
            this.delegateEvents(this.events);

            this.data.config.tags = _.uniq(this.data.config.tags.concat(["extensibleMatchAND","extensibleMatchOR"]));

            this.currentDialog = new BootstrapDialog({
                title:  $.t("templates.connector.ldapConnector.filterTitle", {type: params.type}),
                size: BootstrapDialog.SIZE_WIDE,
                type: BootstrapDialog.TYPE_DEFAULT,
                message: this.dialogContent,
                onshown : _.bind(function() {
                    this.renderExpressionTree();
                }, this)
            });

            this.currentDialog.realize();
            this.currentDialog.open();
        }
    });

    return new LDAPFilterDialog();

});