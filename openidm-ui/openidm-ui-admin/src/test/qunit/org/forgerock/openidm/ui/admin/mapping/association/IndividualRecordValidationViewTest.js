define([
    "org/forgerock/openidm/ui/admin/mapping/association/IndividualRecordValidationView"
], function (IndividualRecordValidationView) {
    QUnit.module('IndividualRecordValidationView Tests');

    QUnit.test("Generate correct rendering information for validSource", function (assert) {
        var preferenceTest = {
            "type" : "text/javascript",
            "globals" : {
                "preferences" : [
                    "marketing"
                ]
            },
            "file" : "ui/preferenceCheck.js"
        },
        scriptTest  =  {
            "type" : "text/javascript",
            "globals" : {},
            "file" : "ui/test.js"
        },
        results;

        results = IndividualRecordValidationView.getDisplayDetails(undefined);

        assert.equal(results.hasScript, false, "Correctly detect no validSource");

        results = IndividualRecordValidationView.getDisplayDetails(preferenceTest);

        assert.equal(results.hasScript, true, "Correctly detect a script is set for preferences");
        assert.equal(results.hasPreferences, true, "Correctly detect a preference script vs a custom script");

        results = IndividualRecordValidationView.getDisplayDetails(scriptTest);

        assert.equal(results.hasScript, true, "Correctly detect a script is set");
        assert.equal(results.hasPreferences, false, "Correctly detect a script vs a preference script");
    });

    QUnit.test("Generate correct rendering information for validTarget", function (assert) {
        var scriptTest  =  {
                "type" : "text/javascript",
                "globals" : {},
                "file" : "ui/test.js"
            },
            results;

        results = IndividualRecordValidationView.getDisplayDetails(undefined);

        assert.equal(results.hasScript, false, "Correctly detect no targetSource");

        results = IndividualRecordValidationView.getDisplayDetails(scriptTest);

        assert.equal(results.hasScript, true, "Correctly detect a script is set");
    });

    QUnit.test("Change mapping object based on selection type", function (assert) {
        var fakeMapping = {
            "validSource" : {
                "type" : "text/javascript",
                "globals" : {
                    "preferences" : [
                        "marketing"
                    ]
                },
                "file" : "ui/preferenceCheck.js"
            },
            "validTarget" : {
                "type" : "text/javascript",
                "globals" : {},
                "file" : "ui/test.js"
            }
        };

        fakeMapping = IndividualRecordValidationView.changeMappingEventConfig(fakeMapping, "customScript", "source");

        assert.equal(fakeMapping.validSource.file, undefined, "Correctly set a default custom script");

        fakeMapping = IndividualRecordValidationView.changeMappingEventConfig(fakeMapping, "allRecords", "source");

        assert.equal(fakeMapping.validSource, undefined, "Correctly remove validSource");

        fakeMapping = IndividualRecordValidationView.changeMappingEventConfig(fakeMapping, "userPreferences", "source");

        assert.equal(fakeMapping.validSource.file, "ui/preferenceCheck.js", "Correctly set preference script");

    });

    QUnit.test("Set checkboxes based on current preferences", function (assert) {
        var checkbox = $("<input class='preference-check' type='checkbox' value='marketing'>"),
            preferences = ["marketing"];

        IndividualRecordValidationView.setPreferences(preferences, checkbox);

        assert.equal(checkbox.is(":checked"), true, "Correctly checked preference check box");

        checkbox = $("<input class='preference-check' type='checkbox' value='update'>")

        IndividualRecordValidationView.setPreferences(preferences, checkbox);

        assert.equal(checkbox.is(":checked"), false, "Correctly did not check preference check box");
    });


    QUnit.test("Set edited script to correct mapping validation event", function (assert) {
        var testScript = {
            "type" : "text/javascript",
            "globals" : {},
            "source" : "test"
        },
        testScript2 = {
            "type" : "text/javascript",
            "globals" : {},
            "file" : "fake"
        },
        mapping = {};

        mapping = IndividualRecordValidationView.setScript(testScript, "target", mapping);

        assert.equal(mapping.validTarget.source, "test", "Correctly set validTarget script");

        mapping = IndividualRecordValidationView.setScript(testScript, "source", mapping);

        assert.equal(mapping.validTarget.source, "test", "validTarget script still set after setting source script");
        assert.equal(mapping.validSource.source, "test", "Correctly set validSource script");

        mapping = IndividualRecordValidationView.setScript(testScript2, "source", mapping);

        assert.equal(mapping.validSource.file, "fake", "Updated validSource script");

    });

});