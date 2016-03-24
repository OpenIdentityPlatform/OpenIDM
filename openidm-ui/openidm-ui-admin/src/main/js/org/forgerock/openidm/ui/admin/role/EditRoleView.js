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

/*global define */

define("org/forgerock/openidm/ui/admin/role/EditRoleView", [
    "jquery",
    "lodash",
    "handlebars",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/common/resource/GenericEditResourceView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/openidm/ui/admin/role/util/UserQueryFilterEditor"
],
function ($, _, Handlebars,
    AbstractView,
    GenericEditResourceView,
    ValidatorsManager,
    UserQueryFilterEditor
  ) {
    var EditRoleView = function () {
        return AbstractView.apply(this, arguments);
    };

    EditRoleView.prototype = Object.create(GenericEditResourceView);
    EditRoleView.prototype.events = _.extend({
        "change .expressionTree :input": "showPendingChanges",
        "change #enableDynamicRoleGrantCheckbox": "toggleQueryView"
    }, GenericEditResourceView.events);

    EditRoleView.prototype.partials = GenericEditResourceView.partials.concat(["partials/role/_conditionForm.html"]);

    EditRoleView.prototype.render = function (args, callback) {
        GenericEditResourceView.render.call(this, args, _.bind(function () {
            if (_.has(this.data.schema.properties, "condition") && !this.$el.find("#condition").length ) {
                if (!this.data.newObject && !$('#conditionFilterForm').length) {
                    this.addConditionForm();
                }
            }
            if (callback) {
                callback();
            }
        }, this));
    };

    EditRoleView.prototype.addConditionForm = function () {
        var resourceDetailsForm = this.$el.find("#resource-details form"),
            conditionContent = Handlebars.compile("{{> role/_conditionForm}}");

        resourceDetailsForm.after(conditionContent);

        if (this.oldObject.condition && this.oldObject.condition !== "false") {
            this.toggleQueryView();
        }

        /*
         * get rid of any existing queryEditors that may be polluting this view
         * if this is not done pending changes does not work properly
         */
        delete this.queryEditor;

        this.queryEditor = this.renderEditor();
    };

    EditRoleView.prototype.renderEditor = function (clearFilter) {
        var _this = this,
            editor = new UserQueryFilterEditor(),
            filter = "";

        if (this.oldObject.condition !== undefined && this.oldObject.condition !== "false" && !clearFilter) {
            filter = _this.oldObject.condition;
        }

        editor.render(
                {
                    "queryFilter": filter,
                    "element": "#conditionFilterHolder",
                    "resource": "managed/role"
                },
                function () {
                    if (filter.length || clearFilter) {
                      _this.showPendingChanges();
                    }
                }
        );

        return editor;
    };

    EditRoleView.prototype.toggleQueryView = function (e) {
        if (e) {
            e.preventDefault();
        }

        if (!this.$el.find("#enableDynamicRoleGrantCheckbox").attr("checked")) {
            this.$el.find("#enableDynamicRoleGrantCheckbox").attr("checked", true);
            this.$el.find("#roleConditionQueryField").show();
            this.renderEditor();
        } else {
            this.$el.find("#enableDynamicRoleGrantCheckbox").removeAttr("checked");
            this.$el.find("#roleConditionQueryField").hide();
            this.renderEditor(true);
        }
    };

    EditRoleView.prototype.getFormValue = function () {
        var conditionChecked = this.$el.find("#enableDynamicRoleGrantCheckbox").attr("checked"),
            condition = "",
            returnVal;

        if (conditionChecked && this.queryEditor) {
            condition = this.queryEditor.getFilterString();
        } else {
            condition = undefined;
        }

        if (this.queryEditor) {
            returnVal = _.extend(
                {
                    "condition": condition
                },
                GenericEditResourceView.getFormValue.call(this)
            );
        }
        else {
            returnVal = GenericEditResourceView.getFormValue.call(this);
        }

        return returnVal;
    };

    return new EditRoleView();
});
