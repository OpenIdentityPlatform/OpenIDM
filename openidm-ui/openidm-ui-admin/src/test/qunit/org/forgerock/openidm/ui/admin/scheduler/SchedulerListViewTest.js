define([
    "jquery",
    "lodash",
    "sinon",
    "handlebars",
    "org/forgerock/openidm/ui/admin/scheduler/SchedulerListView",,
    "org/forgerock/commons/ui/common/util/UIUtils"
], function ($,_, sinon, handlebars, SchedulerListView, UIUtils) {
    QUnit.module('SchedulerListView Tests');
    var scheduleObjArray = [
        //liveSync
        {
            schedule:{
                "_id": "2915c559-0f82-40b0-badc-b0b40a36cc83",
                "invokeContext": {
                    "action": "liveSync",
                    "source": "system/ldap/group"
                }
            },
            expectedHtml: 'LiveSync<br><span class="text-muted">system/ldap/group</span>',
            expectedType: 'liveSync'
        },
        //recon
        {
            schedule:{
                "_id": "recon",
                "invokeContext": {
                    "action": "reconcile",
                    "mapping": "managedUser_systemLdapAccounts"
                }
            },
            expectedHtml: 'Reconciliation<br><span class="text-muted">managedUser_systemLdapAccounts</span>',
            expectedType: 'recon'
        },
        //script with file
        {
            schedule:{
                "_id": "e12e256f-7590-4780-9260-ddacafd5d408",
                "invokeContext": {
                    "script": {
                        "type": "text/javascript",
                        "file": "script/someJSFile.js"
                    }
                }
            },
            expectedHtml: 'Script<br><span class="text-muted">script/someJSFile.js</span>',
            expectedType: 'genericScript'
        },
        //script with source
        {
            schedule:{
                "_id": "e12e256f-7590-4780-9260-ddacafd5d408",
                "invokeContext": {
                    "script": {
                        "type": "text/javascript",
                        "source": "console.log(myVar)"
                    }
                }
            },
            expectedHtml: 'Script<br><span class="text-muted">console.log(myVar)</span>',
            expectedType: 'genericScript'
        },
        //taskscanner
        {
            schedule:{
                "_id": "1ffa8860-98ef-4f53-bf3a-99b2b3d4989b",
                "invokeService": "org.forgerock.openidm.taskscanner",
                "invokeContext": {
                    "scan": {
                        "object": "system/ldap/account"
                    },
                    "task" : true
                }
            },
            expectedHtml: 'Task Scanner<br><span class="text-muted">system/ldap/account</span>',
            expectedType: 'taskScanner'
        }
        ],
        queryFilterObjArray = [
            {
                filters: {
                    "typeFilter": "userDefined"
                },
                expectedResult: "persisted eq true and !(invokeContext/script/source co 'roles/onSync-roles') and !(invokeContext/script/source co 'triggerSyncCheck')"
            },
            {
                filters: {
                    "typeFilter": "reconciliation"
                },
                expectedResult: "persisted eq true and invokeContext/action/ eq 'reconcile'"
            },
            {
                filters: {
                    "typeFilter": "liveSync"
                },
                expectedResult: "persisted eq true and invokeContext/action/ eq 'liveSync'"
            },
            {
                filters: {
                    "typeFilter": "script"
                },
                expectedResult: "persisted eq true and invokeContext/script/ pr and !(invokeContext/script/source co 'roles/onSync-roles') and !(invokeContext/script/source co 'triggerSyncCheck')"
            },
            {
                filters: {
                    "typeFilter": "taskScanner"
                },
                expectedResult: "persisted eq true and invokeContext/task/ pr"
            },
            {
                filters: {
                    "typeFilter": "taskScanner",
                    "resourceSubfilter": "managed/user"
                },
                expectedResult: "persisted eq true and invokeContext/task/ pr and invokeContext/scan/object/ eq 'managed/user'"
            },
            {
                filters: {
                    "typeFilter": "temporalConstraintsOnRole"
                },
                expectedResult: "persisted eq true and invokeContext/script/source co 'roles/onSync-roles'"
            },
            {
                filters: {
                    "typeFilter": "temporalConstraintsOnGrant"
                },
                expectedResult: "persisted eq true and invokeContext/script/source co 'triggerSyncCheck'"
            },
            {
                filters: {
                    "typeFilter": "inMemory"
                },
                expectedResult: "persisted eq false"
            },
            {
                filters: {
                    "typeFilter": "liveSync",
                    "connectorTypeSubfilter": "org.forgerock.openicf.connectors.ldap-connector"
                },
                connectors: [
                    { name: "ldap" },
                    { name: "myOtherLdapConnector"}
                ],
                expectedResult: "persisted eq true and invokeContext/action/ eq 'liveSync' and (invokeContext/source/ co 'system/ldap/' or invokeContext/source/ co 'system/myOtherLdapConnector/')"
            }
        ];

    QUnit.test("getQueryFilterString", function(assert) {
        _.each(queryFilterObjArray, function (obj) {
            assert.equal(SchedulerListView.getQueryFilterString(obj.filters,obj.connectors),obj.expectedResult);
        });
    });

    QUnit.test("getScheduleTypeDisplay", function(assert) {
        //stop the test until ajax call complete
        stop();

        $.get("../www/partials/scheduler/_ScheduleTypeDisplay.html", function (partial) {

            sinon.stub(SchedulerListView, "renderTypePartial", function (type, descriptor) {
                return $(handlebars.compile(partial)({
                    type : type,
                    descriptor : descriptor
                }))
                .html()
                .toString()
                //get rid of excess whitespace
                .replace(/\s\s+/g,"").trim();
            });

            _.each(scheduleObjArray, function (obj) {
                assert.equal(SchedulerListView.getScheduleTypeDisplay(obj.schedule),obj.expectedHtml);
            });
            start();
        });


    });

    QUnit.test("getScheduleType", function(assert) {
        _.each(scheduleObjArray, function (obj) {
            assert.equal(SchedulerListView.getScheduleType(obj.schedule),obj.expectedType);
        });
    });
});
