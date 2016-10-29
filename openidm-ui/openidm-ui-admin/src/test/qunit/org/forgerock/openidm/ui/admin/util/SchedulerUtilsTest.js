define([
    "lodash",
    "org/forgerock/openidm/ui/admin/util/SchedulerUtils"
], function (_, SchedulerUtils) {
    QUnit.module('SchedulerUtils Tests');
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
            expectedResult: {
                type : "liveSync",
                display: "LiveSync",
                meta: "system/ldap/group",
                metaSource: "source"
            }
        },
        //liveSync with no source set
        {
            schedule:{
                "_id": "liveSyncNoSource",
                "invokeContext": {
                    "action": "liveSync"
                }
            },
            expectedResult: {
                type : "liveSync",
                display: "LiveSync",
                meta: "liveSyncNoSource",
                metaSource: "source"
            }
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
            expectedResult: {
                type : "recon",
                display: "Reconciliation",
                meta: "managedUser_systemLdapAccounts",
                metaSource: "mapping"
            }
        },
        //recon with no mapping set
        {
            schedule:{
                "_id": "reconWithNoMapping",
                "invokeContext": {
                    "action": "reconcile"
                }
            },
            expectedResult: {
                type : "recon",
                display: "Reconciliation",
                meta: "reconWithNoMapping",
                metaSource: "mapping"
            }
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
            expectedResult: {
                type : "genericScript",
                display: "Script",
                meta: "script/someJSFile.js",
                metaSource: "script.file"
            }
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
            expectedResult: {
                type : "genericScript",
                display: "Script",
                meta: "console.log(myVar)",
                metaSource: "script.source"
            }
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
            expectedResult: {
                type : "taskScanner",
                display: "Task Scanner",
                meta: "system/ldap/account",
                metaSource: "scan.object"
            }
        },
        //taskscanner with no scan.object
        {
            schedule:{
                "_id": "taskscannerWithNoScanObject",
                "invokeService": "org.forgerock.openidm.taskscanner",
                "invokeContext": {
                    "task" : true
                }
            },
            expectedResult: {
                type : "taskScanner",
                display: "Task Scanner",
                meta: "taskscannerWithNoScanObject",
                metaSource: "scan.object"
            }
        }
    ];


    QUnit.test("getScheduleTypeData", function(assert) {
        _.each(scheduleObjArray, function (obj) {
            var data = JSON.stringify(SchedulerUtils.getScheduleTypeData(obj.schedule)),
                expected = JSON.stringify(obj.expectedResult);

            assert.equal(data,expected);
        });
    });
});
