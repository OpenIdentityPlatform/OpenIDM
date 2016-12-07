define([
    "org/forgerock/openidm/ui/admin/settings/audit/AuditTopicsView",
    "lodash"
], function (AuditTopicsView, _) {
    QUnit.module('AuditTopicsView Tests');

    QUnit.test("When retrieving dialog configuration details...", function(assert) {
        var DEFAULT_TOPICS_LIST = ["authentication", "access", "activity", "recon", "sync", "config"],
            CRUDPAQ_ACTIONS = ["action", "create", "delete", "patch", "query", "read", "update"],
            CUSTOM_CRUDPAQ_ACTIONS = ["create", "delete", "update", "link", "unlink", "exception", "ignore"],
            EVENT_ACTIONS = {
                "authentication": [],
                "access": [],
                "activity": CRUDPAQ_ACTIONS,
                "custom": "test",
                "config": CRUDPAQ_ACTIONS,
                "recon": CUSTOM_CRUDPAQ_ACTIONS,
                "sync": CUSTOM_CRUDPAQ_ACTIONS
            },
            TOPICS = {
                "authentication":{},
                "access":{},
                "activity": {
                    "filter": {
                        "actions":["create","update","delete","patch","action"]
                    },
                    "watchedFields":[],
                    "passwordFields":["password"]
                },
                "recon":{},
                "sync":{},
                "config": {
                    "filter": {
                        "actions":["create","update","delete","patch","action"]
                    }
                }
            },
            testResults;

        testResults = AuditTopicsView.getDialogConfig("sync", TOPICS, DEFAULT_TOPICS_LIST, EVENT_ACTIONS);
        assert.equal(testResults.isDefault, true, "A default event topic is designated as such.");
        assert.equal(testResults.limitedEdits, true, "A default event topic has limited edit capabilities.");
        assert.equal(_.has(testResults, "triggers"), false, "Sync event topics do not have triggers");

        testResults = AuditTopicsView.getDialogConfig("custom", TOPICS, DEFAULT_TOPICS_LIST, EVENT_ACTIONS);
        assert.equal(testResults.isDefault, false, "A custom event topic is not designated as default.");
        assert.equal(testResults.eventDeclarativeActions, "test", "A custom event topic is assigned custom event actions.");

        testResults = AuditTopicsView.getDialogConfig("activity", TOPICS, DEFAULT_TOPICS_LIST, EVENT_ACTIONS);
        assert.equal(_.has(testResults, "triggers"), true, "Activity event topics have triggers");

        testResults = AuditTopicsView.getDialogConfig("recon", TOPICS, DEFAULT_TOPICS_LIST, EVENT_ACTIONS);
        assert.equal(_.has(testResults, "triggers"), true, "Recon event topics have triggers");

    });
});

