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
    "org/forgerock/openidm/ui/admin/role/util/TemporalConstraintsUtils"
], function (TemporalConstraintsUtils) {
    QUnit.module('TemporalConstraintsUtils Tests');

    QUnit.test("convertFromIntervalString", function () {
        var intervalString = "2016-04-25T07:00:00.000Z/2016-04-30T07:00:00.000Z",
            convertedValue = TemporalConstraintsUtils.convertFromIntervalString(intervalString, 0);

        QUnit.equal(convertedValue.start, '04/25/2016 7:00 AM', "startDate is correct");
        QUnit.equal(convertedValue.end, '04/30/2016 7:00 AM', "endDate is correct");
    });

    QUnit.test("convertFromIntervalString with timezone offset", function () {
        var intervalString = "2016-04-25T07:00:00.000Z/2016-04-30T07:00:00.000Z",
            convertedValue = TemporalConstraintsUtils.convertFromIntervalString(intervalString, 420);

        QUnit.equal(convertedValue.start, '04/25/2016 12:00 AM', "startDate is correct");
        QUnit.equal(convertedValue.end, '04/30/2016 12:00 AM', "endDate is correct");
    });

    QUnit.test("convertToIntervalString", function () {
        var startDate = "04/25/2016 7:00 AM",
            endDate = "04/30/2016 7:00 AM",
            intervalString = TemporalConstraintsUtils.convertToIntervalString(startDate, endDate, 0);

        QUnit.equal(intervalString, '2016-04-25T07:00:00.000Z/2016-04-30T07:00:00.000Z', "start and end dates are correctly converted to an invervalString");
    });

    QUnit.test("convertToIntervalString with timezone offset", function () {
        var startDate = "04/25/2016 12:00 AM",
            endDate = "04/30/2016 12:00 AM",
            intervalString = TemporalConstraintsUtils.convertToIntervalString(startDate, endDate, 420);

        QUnit.equal(intervalString, '2016-04-25T07:00:00.000Z/2016-04-30T07:00:00.000Z', "start and end dates are correctly converted to an invervalString with offset");
    });
});
