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

/*global define */

define("org/forgerock/openidm/ui/admin/role/TemporalConstraintsFormView", [
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
            "change .enableTemporalConstraintsCheckbox": "toggleForm"
        },
        partials: [
            "partials/role/_temporalConstraint.html"
        ],
        data: {},
        model: {},
        render: function(args, callback) {
            this.element = args.element;
            this.toggleCallback = args.toggleCallback;
            this.data.temporalConstraints = args.temporalConstraints;

            //if the view is displayed in a dialog change the column sizes to fit the dialog's form
            this.data.dialogView = args.dialogView;

            this.parentRender(_.bind(function(){
                this.showForm();

                if(callback) {
                    callback();
                }
            },this));
        },
        toggleForm : function (e) {
            if (e) {
                e.preventDefault();
            }

            if (!this.$el.find(".enableTemporalConstraintsCheckbox").attr("checked")) {
                this.$el.find(".enableTemporalConstraintsCheckbox").attr("checked", true);
                this.$el.find(".temporalConstraintsFields").show();
            } else {
                this.$el.find(".enableTemporalConstraintsCheckbox").removeAttr("checked");
                this.$el.find(".temporalConstraintsFields").find(".datetimepicker").val("");
                this.$el.find(".temporalConstraintsFields").hide();
            }

            if (this.toggleCallback) {
                this.toggleCallback();
            }
        },
        showForm : function () {
            var temporalConstraintsContent;

            temporalConstraintsContent = Handlebars.compile("{{> role/_temporalConstraint}}")({ temporalConstraint : this.data.temporalConstraints[0] });

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

            if (this.data.temporalConstraints && this.data.temporalConstraints.length) {
                this.toggleForm();
            }
        }
    });

    return TemporalConstraintsFormView;
});
