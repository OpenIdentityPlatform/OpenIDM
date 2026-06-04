define([
    "jquery",
    "sinon",
    "org/forgerock/openidm/ui/admin/mapping/util/MappingUtils",
    "org/forgerock/openidm/ui/common/delegates/SearchDelegate"
], function ($, sinon, MappingUtils, SearchDelegate) {
    QUnit.module('MappingUtils Tests');

    QUnit.test("setupSampleSearch passes a flat array of records to the selectize load callback", function (assert) {
        var done = assert.async(),
            records = [{ email: "jsanchez@example.com", lastName: "Sanchez", firstName: "Jane" }],
            capturedConfig,
            selectizeStub = sinon.stub($.fn, "selectize", function (config) {
                capturedConfig = config;
                return this;
            }),
            searchStub = sinon.stub(SearchDelegate, "searchResults", function () {
                return $.Deferred().resolve(records).promise();
            });

        try {
            MappingUtils.setupSampleSearch(
                $("<input>"),
                { source: "system/hr/account" },
                ["email", "lastName", "firstName"],
                function () {}
            );

            assert.equal(capturedConfig.valueField, "email", "valueField is the first non-empty prop");

            capturedConfig.load("Sanchez", function (options) {
                // Regression test: options must be the flat array of records, not [[...]].
                assert.deepEqual(options, records, "load callback receives a flat array of records");
                done();
            });
        } finally {
            selectizeStub.restore();
            searchStub.restore();
        }
    });

    QUnit.test("setupSampleSearch ignores props without a source (compacts valueField/searchField)", function (assert) {
        var capturedConfig,
            selectizeStub = sinon.stub($.fn, "selectize", function (config) {
                capturedConfig = config;
                return this;
            });

        try {
            MappingUtils.setupSampleSearch(
                $("<input>"),
                { source: "managed/user" },
                [undefined, "userName", "sn"],
                function () {}
            );

            assert.equal(capturedConfig.valueField, "userName", "valueField falls back to first non-empty prop");
            assert.deepEqual(capturedConfig.searchField, ["userName", "sn"], "searchField has no undefined entries");
        } finally {
            selectizeStub.restore();
        }
    });
});