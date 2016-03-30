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
    "bootstrap-datetimepicker",
    "org/forgerock/openidm/ui/admin/role/util/TemporalConstraintsUtils"
],
function ($, _, Handlebars,
    AbstractView,
    GenericEditResourceView,
    ValidatorsManager,
    UserQueryFilterEditor,
    Datetimepicker,
    TemporalConstraintsUtils
  ) {
    var EditRoleView = function () {
            return AbstractView.apply(this, arguments);
        };

    EditRoleView.prototype = Object.create(GenericEditResourceView);
    EditRoleView.prototype.events = _.extend({
        "change .expressionTree :input": "showPendingChanges",
        "blur :input.datetimepicker": "showPendingChanges",
        "change #enableDynamicRoleGrantCheckbox": "toggleQueryView",
        "change #enableTemporalConstraintsCheckbox": "toggleTemporalConstraintsView"
    }, GenericEditResourceView.events);

    EditRoleView.prototype.partials = GenericEditResourceView.partials.concat(["partials/role/_conditionForm.html", "partials/role/_temporalConstraintsForm.html"]);

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
        var resourceDetailsForm = this.$el.find("#resource-details form"),
            temporalConstraints = [],
            temporalConstraintsContent;

        if (this.oldObject.temporalConstraints) {
            temporalConstraints = _.map(this.oldObject.temporalConstraints, function (constraint) {
                return TemporalConstraintsUtils.convertFromIntervalString(constraint.duration);
            });
        }

        temporalConstraintsContent = Handlebars.compile("{{> role/_temporalConstraintsForm}}")({ temporalConstraints : temporalConstraints });

        resourceDetailsForm.append(temporalConstraintsContent);

        this.$el.find('.datetimepicker').datetimepicker({
            sideBySide: true,
            useCurrent: false,
            icons: {
                time: 'fa fa-clock-o',
                date: 'fa fa-calendar',
                up: 'fa fa-chevron-up',
                down: 'fa fa-chevron-down',
                previous: 'fa fa-chevron-left',
                next: 'fa fa-chevron-right',
                today: 'fa fa-crosshairs',
                clear: 'fa fa-trash',
                close: 'fa fa-remove'
            }
        });

        if (this.oldObject.temporalConstraints && this.oldObject.temporalConstraints.length) {
            this.toggleTemporalConstraintsView();
        }
    };

    EditRoleView.prototype.addConditionForm = function () {
        var resourceDetailsForm = this.$el.find("#resource-details form"),
            conditionContent = Handlebars.compile("{{> role/_conditionForm}}");

        resourceDetailsForm.append(conditionContent);

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

    EditRoleView.prototype.toggleTemporalConstraintsView = function (e) {
        if (e) {
            e.preventDefault();
        }

        if (!this.$el.find("#enableTemporalConstraintsCheckbox").attr("checked")) {
            this.$el.find("#enableTemporalConstraintsCheckbox").attr("checked", true);
            this.$el.find("#temporalConstraintsFields").show();
        } else {
            this.$el.find("#enableTemporalConstraintsCheckbox").removeAttr("checked");
            this.$el.find("#temporalConstraintsFields").find(".datetimepicker").val("");
            this.$el.find("#temporalConstraintsFields").hide();
        }
        this.showPendingChanges();
    };

    EditRoleView.prototype.getFormValue = function () {
        var conditionChecked = this.$el.find("#enableDynamicRoleGrantCheckbox").attr("checked"),
            temporalConstraintsChecked = this.$el.find("#enableTemporalConstraintsCheckbox").attr("checked"),
            condition = "",
            temporalConstraints = [],
            returnVal;

        if (conditionChecked && this.queryEditor) {
            condition = this.queryEditor.getFilterString();
        } else {
            condition = undefined;
        }

        if (temporalConstraintsChecked) {
            temporalConstraints = TemporalConstraintsUtils.getTemporalConstraintsValue(this.$el.find('#temporalConstraintsForm'));
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
