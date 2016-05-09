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
 * Copyright 2016 ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "handlebars",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/admin/role/util/TemporalConstraintsUtils",
    "bootstrap-datetimepicker"
], function($, _, Handlebars,
            AdminAbstractView,
            TemporalConstraintsUtils,
            Datetimepicker) {
    var TemporalConstraintsFormView = AdminAbstractView.extend({
        template: "templates/admin/role/TemporalConstraintsFormView.html",
        events: {
            "change .enableTemporalConstraintsCheckbox": "toggleForm",
            "change .temporalConstraintTimezone": "adjustDateToTimezone"
        },
        partials: [
            "partials/role/_temporalConstraint.html"
        ],
        data: {},
        model: {},
        /*
        * the args object passed to this render function include these properties:
        *    element: the id of the form's container element
        *    temporalConstraints: an array of temporalConstraint objects produced by looping over temporal constraint
        *                         duration strings and passing each one of them TemporalConstraintsUtils.convertFromIntervalString
        *    toggleCallback: a function to call when the enable temporal constraints toggle switch is changed
        *    dialogView: a boolean value telling the view whether the display is in a dialog or notAssigned
        *
        * example of how to call this view:
        *    temporalConstraintsView.render({
        *        element: "#" + formContainerId,
        *        temporalConstraints: temporalConstraints,
        *        toggleCallback: function () {
        *            _this.showPendingChanges();
        *        },
        *        dialogView: false
        *    });
        */
        render: function(args, callback) {
            this.element = args.element;
            this.toggleCallback = args.toggleCallback;
            this.data.temporalConstraints = args.temporalConstraints;
            this.data.hasTemporalConstraints = args.temporalConstraints.length > 0;
            this.data.timezone = TemporalConstraintsUtils.getDefaultTimezone();

            //if the view is displayed in a dialog this flag tells
            //the template to change the column sizes to fit the dialog's form
            this.data.dialogView = args.dialogView;

            this.parentRender(_.bind(function(){
                this.showForm(true);

                if(callback) {
                    callback();
                }
            },this));
        },
        showForm : function (isOnRender) {
            var temporalConstraintsContent;

            //if there are any temporalConstraints already on the dom get rid of them
            this.$el.find(".temporalConstraintsFields").empty();

            //if there were multiple constraints we would loop over all of them here
            //since there is only one we directly access [0] in the array
            temporalConstraintsContent = Handlebars.compile("{{> role/_temporalConstraint}}")({
                temporalConstraint : this.data.temporalConstraints[0],
                timezone: this.data.timezone,
                timezones: TemporalConstraintsUtils.getTimezones()
            });

            this.$el.find(".temporalConstraintsFields").append(temporalConstraintsContent);

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

            //set the endDate datepicker to only allow for dates after the startDate
            this.$el.find('.temporalConstraintStartDate').on("dp.change", _.bind(function (e) {
                var startInput = $(e.target),
                    endInput = this.$el.find('.temporalConstraintEndDate');

                //if the startDate is set to a later date than endDate
                //set endDate to an empty string
                if (endInput.val().length && new Date(startInput.val()) > new Date(endInput.val())) {
                    endInput.val("");
                }

                endInput.data("DateTimePicker").minDate(e.date);
            }, this));

            this.$el.find(".temporalConstraintTimezone").selectize();

            if (isOnRender && this.data.temporalConstraints && this.data.temporalConstraints.length) {
                this.toggleForm();
            }
        },
        toggleForm : function (e) {
            if (e) {
                e.preventDefault();
            }

            if (this.$el.find(".enableTemporalConstraintsCheckbox").prop("checked")) {
                this.$el.find(".temporalConstraintsFields").show();
            } else {
                this.$el.find(".temporalConstraintsFields").find(".datetimepicker").val("");
                this.$el.find(".temporalConstraintsFields").hide();
            }

            if (this.toggleCallback) {
                this.toggleCallback();
            }
        },
        adjustDateToTimezone : function (e) {
            var newTimezone = $(e.target).val(),
                constraint = $(e.target).closest(".temporalConstraint"),
                startDate = constraint.find(".temporalConstraintStartDate"),
                endDate = constraint.find(".temporalConstraintEndDate"),
                formValue;

            this.data.timezone = newTimezone;

            //newTimezone will be an empty string when the value of the selectize is blank
            //in that case do nothing
            if (newTimezone.length) {
                //set the defaultTimezone for this browser in localStorage
                localStorage.setItem("temporalConstraintsDefaultTimezone", newTimezone);
                //get the form's value with the previousTimezone applied
                formValue = TemporalConstraintsUtils.getTemporalConstraintsValue(this.$el.find('.temporalConstraintsForm'), true);
                //loop over the formValue array and return new temporal constraint objects based on the newTimezone selection
                this.data.temporalConstraints = _.map(formValue, _.bind(function (constraint) {
                    var startDate = constraint.duration.split("/")[0],
                        offset = TemporalConstraintsUtils.getTimezoneOffset(this.data.timezone, new Date(startDate));

                    return TemporalConstraintsUtils.convertFromIntervalString(constraint.duration, offset);
                }, this));
                //set the previousTimezone attribute to the newly selected timezone
                $(e.target).attr("previousTimezone", this.data.timezone);

                this.showForm();
            }
        }
    });

    return TemporalConstraintsFormView;
});
