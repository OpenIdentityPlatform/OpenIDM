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
 * Copyright 2011-2016 ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "handlebars",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/common/resource/RelationshipArrayView",
    "org/forgerock/openidm/ui/common/resource/ResourceCollectionSearchDialog",
    "org/forgerock/openidm/ui/admin/role/MembersDialog"
],
function ($, _, Handlebars,
    AbstractView,
    RelationshipArrayView,
    ResourceCollectionSearchDialog,
    MembersDialog
  ) {
    var MembersView = new RelationshipArrayView();

    //overriding the render function here to remove the checkboxes from grid rows
    //that have the _grantType set to conditional
    //accomplished by adding the onGridChange arg
    MembersView.render = function (args, callback) {
        args.onGridChange = _.bind(function () {
            var membersList = this.$el.find("#relationshipArray-members tbody, #relationshipArray-roles tbody");

            this.removeConditionalGrantCheckboxes(membersList);

            if (callback) {
                callback();
            }
        }, this);

        RelationshipArrayView.prototype.render.call(this, args, callback);
    };
    /**
     * @param memberList {object} - a jquery object representing the data rows from the members list grid
     */
    MembersView.removeConditionalGrantCheckboxes = function (membersList) {
        _.each(membersList.find("tr"), function (row) {
            var rowIsConditional = $(row).find("td:contains('conditional')").length;

            if (rowIsConditional) {
                $(row).find(".select-row-cell input[type=checkbox]").remove();
            }
        });
    };

    MembersView.openResourceCollectionDialog = function (propertyValue) {
        var opts = this.getResourceCollectionDialogOptions(propertyValue);

        new MembersDialog().renderDialog(opts);
    };

    return MembersView;
});
