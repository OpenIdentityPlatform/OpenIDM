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
    "org/forgerock/openidm/ui/admin/delegates/SchedulerDelegate"
], function ($, sinon, SchedulerDelegate) {
    QUnit.module('SchedulerDelegate Tests');

    function capturedQueryFilters(call) {
        var stub = sinon.stub(SchedulerDelegate, "serviceCall", function () {
                return $.Deferred().promise();
            }),
            urls;

        try {
            call();
            urls = stub.args.map(function (args) { return args[0].url; });
        } finally {
            stub.restore();
        }
        return urls.map(function (url) {
            return decodeURIComponent(url.substring("?_queryFilter=".length));
        });
    }

    QUnit.test("getReconSchedulesByMappingName escapes the mapping name", function (assert) {
        var filters = capturedQueryFilters(function () {
            SchedulerDelegate.getReconSchedulesByMappingName("o'neil\"\\");
        });

        assert.deepEqual(filters, ["invokeContext/action/ eq 'reconcile' and invokeContext/mapping/ eq \"o'neil\\\"\\\\\""]);
    });

    QUnit.test("getSchedulerTriggersByNodeIds escapes each node id", function (assert) {
        var filters = capturedQueryFilters(function () {
            SchedulerDelegate.getSchedulerTriggersByNodeIds(["node1", "n'2\""]);
        });

        assert.deepEqual(filters, [
            "persisted eq true and triggers/0/nodeId pr and triggers/0/state gt 0 and triggers/0/nodeId eq \"node1\"",
            "persisted eq true and triggers/0/nodeId pr and triggers/0/state gt 0 and triggers/0/nodeId eq \"n'2\\\"\""
        ]);
    });
});
