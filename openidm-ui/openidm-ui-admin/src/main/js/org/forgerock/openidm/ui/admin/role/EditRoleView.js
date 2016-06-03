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
    "org/forgerock/openidm/ui/admin/role/util/UserQueryFilterEditor",
    "org/forgerock/openidm/ui/admin/role/util/TemporalConstraintsUtils",
    "org/forgerock/openidm/ui/admin/role/TemporalConstraintsFormView",
    "org/forgerock/openidm/ui/admin/role/MembersView"
],
function ($, _, Handlebars,
    AbstractView,
    GenericEditResourceView,
    ValidatorsManager,
    UserQueryFilterEditor,
    TemporalConstraintsUtils,
    TemporalConstraintsFormView,
    MembersView
  ) {
    var EditRoleView = function () {
            return AbstractView.apply(this, arguments);
        };

    EditRoleView.prototype = Object.create(GenericEditResourceView);
    EditRoleView.prototype.tabViewOverrides.members = MembersView;
    EditRoleView.prototype.events = _.extend({
        "change .expressionTree :input": "showPendingChanges",
        "blur :input.datetimepicker": "showPendingChanges",
        "change #enableDynamicRoleGrantCheckbox": "toggleQueryView"
    }, GenericEditResourceView.events);

    EditRoleView.prototype.partials = GenericEditResourceView.partials.concat(["partials/role/_conditionForm.html"]);

    EditRoleView.prototype.render = function (args, callback) {
        GenericEditResourceView.render.call(this, args, _.bind(function () {
        if (_.has(this.data.schema.properties, "temporalConstraints")) {
            if (!this.data.newObject && !this.$el.find('#temporalConstraintsForm').length) {
                    this.addTemporalConstraintsForm();
                }
            }
            if (_.has(this.data.schema.properties, "condition") && !this.$el.find("#condition").length ) {
                if (!this.data.newObject && !this.$el.find('#conditionFilterForm').length) {
                    this.addConditionForm();
                }
            }


            if (callback) {
                callback();
            }
        }, this));
    };

    EditRoleView.prototype.addTemporalConstraintsForm = function () {
        var _this = this,
            resourceDetailsForm = this.$el.find("#resource-details form"),
            formContainerId = "temporalContstraintsFormContainer",
            formContainer = $("<div id='" + formContainerId + "'></div>"),
            temporalConstraints = [],
            temporalConstraintsView = new TemporalConstraintsFormView();

        if (this.oldObject.temporalConstraints) {
            temporalConstraints = _.map(this.oldObject.temporalConstraints, function (constraint) {
                return TemporalConstraintsUtils.convertFromIntervalString(constraint.duration);
            });
        }

        resourceDetailsForm.append(formContainer);

        temporalConstraintsView.render({
            element: "#" + formContainerId,
            toggleCallback: function () {
                _this.showPendingChanges();
            },
            temporalConstraints: temporalConstraints
        });
    };

    EditRoleView.prototype.addConditionForm = function () {
        var resourceDetailsForm = this.$el.find("#resource-details form"),
            conditionContent = Handlebars.compile("{{> role/_conditionForm}}");

        resourceDetailsForm.append(conditionContent);

        if (this.oldObject.condition && this.oldObject.condition !== "false") {
            if (this.oldObject.condition.length) {
                this.$el.find("#enableDynamicRoleGrantCheckbox").prop("checked",true);
            }
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

        if (this.$el.find("#enableDynamicRoleGrantCheckbox").prop("checked")) {
            this.$el.find("#roleConditionQueryField").show();
            this.renderEditor();
        } else {
            this.$el.find("#roleConditionQueryField").hide();
            this.renderEditor(true);
        }
    };

    EditRoleView.prototype.getFormValue = function () {
        var conditionChecked = this.$el.find("#enableDynamicRoleGrantCheckbox").prop("checked"),
            temporalConstraintsChecked = this.$el.find(".enableTemporalConstraintsCheckbox").prop("checked"),
            condition = "",
            temporalConstraints = [],
            returnVal;

        if (conditionChecked && this.queryEditor) {
            condition = this.queryEditor.getFilterString();
        } else {
            condition = undefined;
        }

        if (temporalConstraintsChecked) {
            temporalConstraints = TemporalConstraintsUtils.getTemporalConstraintsValue(this.$el.find('.temporalConstraintsForm'));
        } else {
            temporalConstraints = undefined;
        }

            returnVal = _.extend(
                {
                    "condition": condition,
                    "temporalConstraints": temporalConstraints
                },
                GenericEditResourceView.getFormValue.call(this)
            );

        return returnVal;
    };

    return new EditRoleView();
});
