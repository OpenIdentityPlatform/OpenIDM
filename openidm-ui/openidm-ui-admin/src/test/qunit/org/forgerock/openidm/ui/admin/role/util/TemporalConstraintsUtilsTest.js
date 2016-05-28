define([
    "org/forgerock/openidm/ui/admin/role/util/TemporalConstraintsUtils"
], function (TemporalConstraintsUtils) {
    QUnit.module('TemporalConstraintsUtils Tests');

    QUnit.test("convertFromIntervalString", () => {
        var intervalString = "2016-04-25T07:00:00.000Z/2016-04-30T07:00:00.000Z",
            convertedValue = TemporalConstraintsUtils.convertFromIntervalString(intervalString, 0);

        QUnit.equal(convertedValue.start, '04/25/2016 7:00 AM', "startDate is correct");
        QUnit.equal(convertedValue.end, '04/30/2016 7:00 AM', "endDate is correct");
    });

    QUnit.test("convertFromIntervalString with timezone offset", () => {
        var intervalString = "2016-04-25T07:00:00.000Z/2016-04-30T07:00:00.000Z",
            convertedValue = TemporalConstraintsUtils.convertFromIntervalString(intervalString, 420);

        QUnit.equal(convertedValue.start, '04/25/2016 12:00 AM', "startDate is correct");
        QUnit.equal(convertedValue.end, '04/30/2016 12:00 AM', "endDate is correct");
    });

    QUnit.test("convertToIntervalString", () => {
        var startDate = "04/25/2016 7:00 AM",
            endDate = "04/30/2016 7:00 AM",
            intervalString = TemporalConstraintsUtils.convertToIntervalString(startDate, endDate, 0);

        QUnit.equal(intervalString, '2016-04-25T07:00:00.000Z/2016-04-30T07:00:00.000Z', "start and end dates are correctly converted to an invervalString");
    });

    QUnit.test("convertToIntervalString with timezone offset", () => {
        var startDate = "04/25/2016 12:00 AM",
            endDate = "04/30/2016 12:00 AM",
            intervalString = TemporalConstraintsUtils.convertToIntervalString(startDate, endDate, 420);

        QUnit.equal(intervalString, '2016-04-25T07:00:00.000Z/2016-04-30T07:00:00.000Z', "start and end dates are correctly converted to an invervalString with offset");
    });
});
