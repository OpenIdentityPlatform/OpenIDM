define([
    "lodash",
    "org/forgerock/openidm/ui/admin/util/Scheduler"
], function (_, Scheduler) {
    QUnit.module('Scheduler Tests');

    QUnit.test("cronToHumanReadable", function(assert) {
        var scheduleObjArray = [
            {
                schedule: "1 1 * * * ?",
                expectedResult: "Every hour at 01 minutes past the hour at 01 seconds past the min"
            },
            {
                schedule: "*/1 * * * * ?",
                expectedResult: "Every (01) seconds"
            },
            {
                schedule: "0 0 3 1 * ?",
                expectedResult: "Every month on the 1st at 03 :00 :00"
            },
            {
                //this is an example of a cron string that is too complex for jquer-cron to parse
                schedule: "30 */1 * * * ?",
                expectedResult: "30 */1 * * * ?"
            }
        ];

        _.each(scheduleObjArray, function (obj) {
            assert.equal(Scheduler.cronToHumanReadable(obj.schedule).replace(/\s\s+/g," ").trim(), obj.expectedResult);
        });
    });
});
