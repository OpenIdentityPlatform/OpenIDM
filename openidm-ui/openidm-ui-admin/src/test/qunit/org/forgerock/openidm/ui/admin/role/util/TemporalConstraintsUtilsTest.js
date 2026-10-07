/*
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
 * Portions Copyright 2026 3A Systems, LLC.
 */

define([
    "jquery",
    "org/forgerock/openidm/ui/admin/role/util/TemporalConstraintsUtils"
], function ($, TemporalConstraintsUtils) {
    QUnit.module('TemporalConstraintsUtils Tests');

    QUnit.test("convertFromIntervalString", (assert) => {
        var intervalString = "2016-04-25T07:00:00.000Z/2016-04-30T07:00:00.000Z",
            convertedValue = TemporalConstraintsUtils.convertFromIntervalString(intervalString, 0);

        assert.equal(convertedValue.start, '04/25/2016 7:00 AM', "startDate is correct");
        assert.equal(convertedValue.end, '04/30/2016 7:00 AM', "endDate is correct");
    });

    QUnit.test("convertFromIntervalString with timezone offset", (assert) => {
        var intervalString = "2016-04-25T07:00:00.000Z/2016-04-30T07:00:00.000Z",
            convertedValue = TemporalConstraintsUtils.convertFromIntervalString(intervalString, 420);

        assert.equal(convertedValue.start, '04/25/2016 12:00 AM', "startDate is correct");
        assert.equal(convertedValue.end, '04/30/2016 12:00 AM', "endDate is correct");
    });

    QUnit.test("convertToIntervalString", (assert) => {
        var startDate = "04/25/2016 7:00 AM",
            endDate = "04/30/2016 7:00 AM",
            intervalString = TemporalConstraintsUtils.convertToIntervalString(startDate, endDate, 0);

        assert.equal(intervalString, '2016-04-25T07:00:00.000Z/2016-04-30T07:00:00.000Z', "start and end dates are correctly converted to an invervalString");
    });

    QUnit.test("convertToIntervalString with timezone offset", (assert) => {
        var startDate = "04/25/2016 12:00 AM",
            endDate = "04/30/2016 12:00 AM",
            intervalString = TemporalConstraintsUtils.convertToIntervalString(startDate, endDate, 420);

        assert.equal(intervalString, '2016-04-25T07:00:00.000Z/2016-04-30T07:00:00.000Z', "start and end dates are correctly converted to an invervalString with offset");
    });

    QUnit.test("isValidInterval", (assert) => {
        assert.ok(TemporalConstraintsUtils.isValidInterval("04/25/2016 7:00 AM", "04/30/2016 7:00 AM"), "end after start is valid");
        assert.notOk(TemporalConstraintsUtils.isValidInterval("04/30/2016 7:00 AM", "04/25/2016 7:00 AM"), "end before start is invalid");
        assert.notOk(TemporalConstraintsUtils.isValidInterval("04/25/2016 7:00 AM", "04/25/2016 7:00 AM"), "end equal to start is invalid");
        assert.notOk(TemporalConstraintsUtils.isValidInterval("04/25/2016 7:00 AM", ""), "an empty end date is invalid");
        assert.notOk(TemporalConstraintsUtils.isValidInterval("", "04/30/2016 7:00 AM"), "an empty start date is invalid");
        assert.notOk(TemporalConstraintsUtils.isValidInterval("04/25/2016 7:00 AM", "not a date"), "an unparseable end date is invalid");
    });

    QUnit.test("isTemporalConstraintsFormValid", (assert) => {
        const constraint = (start, end) => "<div class='temporalConstraint'>"
                + "<input class='temporalConstraintStartDate' value='" + start + "'>"
                + "<input class='temporalConstraintEndDate' value='" + end + "'></div>",
            form = (...constraints) => $("<div>" + constraints.join("") + "</div>");

        assert.ok(TemporalConstraintsUtils.isTemporalConstraintsFormValid(form()), "a form without constraints is valid");
        assert.ok(TemporalConstraintsUtils.isTemporalConstraintsFormValid(
            form(constraint("04/25/2016 7:00 AM", "04/30/2016 7:00 AM"))), "end after start is valid");
        assert.notOk(TemporalConstraintsUtils.isTemporalConstraintsFormValid(
            form(constraint("04/25/2016 7:00 AM", ""))), "an empty end date is invalid");
        assert.notOk(TemporalConstraintsUtils.isTemporalConstraintsFormValid(
            form(constraint("04/25/2016 7:00 AM", "04/30/2016 7:00 AM"), constraint("04/30/2016 7:00 AM", "04/25/2016 7:00 AM"))),
            "one constraint with the end before the start makes the form invalid");
    });
});
