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
 * Portions Copyright 2026 3A Systems, LLC.
 */

define([
    "jquery",
    "sinon",
    "org/forgerock/openidm/ui/admin/mapping/util/MappingUtils",
    "org/forgerock/openidm/ui/common/delegates/SearchDelegate",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate",
    "org/forgerock/openidm/ui/admin/delegates/SchedulerDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function ($, sinon, MappingUtils, SearchDelegate, ResourceDelegate, SchedulerDelegate, UIUtils) {
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

    QUnit.test("getMappingChildren escapes the mapping name in the assignment query", function (assert) {
        var pending = function () { return $.Deferred().promise(); },
            stubs = [
                sinon.stub(SchedulerDelegate, "getReconSchedulesByMappingName", pending),
                sinon.stub(UIUtils, "preloadPartial", pending),
                sinon.stub(ResourceDelegate, "searchResource", pending)
            ],
            searchStub = stubs[2];

        try {
            MappingUtils.getMappingChildren('m"1\\');

            assert.equal(decodeURIComponent(searchStub.firstCall.args[0]), 'mapping eq "m\\"1\\\\"');
            assert.equal(searchStub.firstCall.args[1], "managed/assignment");
        } finally {
            stubs.forEach(function (stub) { stub.restore(); });
        }
    });
});
