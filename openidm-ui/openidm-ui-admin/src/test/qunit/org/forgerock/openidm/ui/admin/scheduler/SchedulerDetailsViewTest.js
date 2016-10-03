define([
    "jquery",
    "lodash",
    "sinon",
    "handlebars",
    "org/forgerock/openidm/ui/admin/delegates/SchedulerDelegate",
    "org/forgerock/openidm/ui/admin/scheduler/AbstractSchedulerView"

], function (
    $, _, sinon, Handlebars, SchedulerDelegate, AbstractSchedulerView
) {
    QUnit.module('SchedulerDetailView Tests');

    QUnit.test("#updateInvokeContextVisibleElement calls the correct callback for each `invokeService` type", function(assert) {
        let abstractSchedulerView = new AbstractSchedulerView(),
            mappingOptionsSpy,
            liveSyncOptionsSpy,
            iseSpy;
        // overwrite methods so I don't actually make delegate calls that will most likely fail in the testing environment
        abstractSchedulerView.showMappingOptions = _.noop;
        abstractSchedulerView.showLiveSyncOptions = _.noop;
        abstractSchedulerView.showInlineScriptEditor = _.noop;

        // start spying
        mappingOptionsSpy = sinon.spy(abstractSchedulerView, "showMappingOptions");
        liveSyncOptionsSpy = sinon.spy(abstractSchedulerView, "showLiveSyncOptions");
        iseSpy = sinon.spy(abstractSchedulerView, "showInlineScriptEditor");

        // make calls and check spies
        assert.equal(mappingOptionsSpy.callCount, 0, "mapping options spy not called")
        abstractSchedulerView.updateInvokeContextVisibleElement("sync");
        assert.ok(mappingOptionsSpy.calledOnce, "mapping options");

        assert.equal(liveSyncOptionsSpy.callCount, 0, "mapping options spy not called")
        abstractSchedulerView.updateInvokeContextVisibleElement("provisioner");
        assert.ok(liveSyncOptionsSpy.calledOnce, "liveSync options");

        assert.equal(iseSpy.callCount, 0, "mapping options spy not called")
        abstractSchedulerView.updateInvokeContextVisibleElement("script");
        assert.ok(iseSpy.calledOnce, "ise");
    });

    QUnit.test("#resourceOptions properly forms the options from the delegate response", function(assert) {
        let abstractDelegateView, connectors, resourceOptionsObj;
        abstractDelegateView = new AbstractSchedulerView;
        connectors = [
            {
                "name": "ldap",
                "enabled": true,
                "config": "config/provisioner.openicf/ldap",
                "objectTypes": [ "__ALL__", "account", "group" ],
                "connectorRef": {
                    "bundleName": "org.forgerock.openicf.connectors.ldap-connector",
                    "connectorName": "org.identityconnectors.ldap.LdapConnector",
                    "bundleVersion": "[1.4.0.0,2.0.0.0)"
                },
                "displayName": "LDAP Connector", "ok": true
            }
        ];

        resourceOptionsObj = abstractDelegateView.formatSourceOptions(connectors);

        assert.deepEqual(
            resourceOptionsObj.options,
            [
                "system/ldap/__ALL__",
                "system/ldap/account",
                "system/ldap/group"
            ],
            'connectors processed into options'
        );
    });

    QUnit.test("#mappingOptions properly forms the options from the delegate response", function(assert) {
        let abstractDelegateView, syncConfig, mappingOptionsObj;
        abstractDelegateView = new AbstractSchedulerView;
        syncConfig = {
            "_id": "sync",
            "mappings": [
                {
                    "name": "systemLdapAccounts_managedUser",
                    "source": "system/ldap/account",
                    "target": "managed/user",
                    "properties": [],
                    "policies": []
                },
                {
                    "name": "managedUser_systemLdapAccounts",
                    "source": "managed/user",
                    "target": "system/ldap/account",
                    "links": "systemLdapAccounts_managedUser",
                    "onCreate": {
                        "type": "text/javascript",
                        "source": "target.dn = 'uid=' + source.userName + ',ou=People,dc=example,dc=com';"
                    },
                    "properties": [],
                    "policies": []
                }
            ]
        };

        mappingOptionsObj = abstractDelegateView.formatMappingOptions(syncConfig);

        assert.deepEqual(
            mappingOptionsObj.options,
            [
                "systemLdapAccounts_managedUser",
                "managedUser_systemLdapAccounts"
            ],
            'connectors processed into options'
        );
    });

    QUnit.test("#createSourceMappingData properly creates the data object", function(assert) {
        let abstractSchedulerView = new AbstractSchedulerView();
        _.bindAll(abstractSchedulerView);
        abstractSchedulerView.schedule = { invokeContext: { mapping: "foo", source :"bar" } };

        var data = [
                { type: "mapping", options:["m1", "m2"] },
                { type: "source", options:["s1", "s2"] }
            ].map( abstractSchedulerView.createSourceMappingData),
            mapping = data[0],
            source = data[1];

        assert.equal(mapping.action, "reconcile", "mapping.action");
        assert.equal(mapping.type, "mapping", "mapping.type");
        assert.equal(mapping.resourceMapping,"foo", "mapping.resourceMapping");
        assert.equal(source.action, "liveSync", "source.action");
        assert.equal(source.type, "source", "source.type");
        assert.equal(source.resourceMapping,"bar", "source.resourceMapping");
    });

    QUnit.test("#formChangeHandler calls #updateSchedule with correct args", function(assert) {
        // expect(0);
        let invokeContextInputSpyArgs,
            abstractSchedulerView = new AbstractSchedulerView(),
            // bind formChangeHandler to instance so I can use it
            formChangeHandler = _.bind(abstractSchedulerView.formChangeHandler, abstractSchedulerView),
            updateScheduleSpy = sinon.stub(abstractSchedulerView, "updateSchedule", _.noop),
            setInvokeContextObjectSpy = sinon.spy(abstractSchedulerView, "setInvokeContextObject"),
            // create fake elements to trigger events on
            genericInput = $("<input>", {
                "class": "schedule-form-element",
                name: "generic",
                value: "generic test value"
            }),
            invokeContextInput = $("<input>", {
                "class": "schedule-form-element",
                name: "mapping",
                value: "mapping test value"});

        // set up event handlers
        genericInput.on("change", formChangeHandler);
        invokeContextInput.on("change", formChangeHandler);

        // trigger generic input change event and spy the calls
        genericInput.trigger("change");

        // generic assertions
        assert.ok(updateScheduleSpy.calledOnce);
        assert.ok(updateScheduleSpy.calledWith("generic", "generic test value"), "generic input");

        // trigger generic input change event and spy the calls
        invokeContextInput.trigger("change");
        invokeContextInputSpyArgs = updateScheduleSpy.secondCall.args;

        // invoke context assertions
        assert.ok(updateScheduleSpy.calledTwice);
        assert.equal(invokeContextInputSpyArgs[0], "invokeContext", "invokeContext args");
        assert.deepEqual(invokeContextInputSpyArgs[1], {
            action: "reconcile", mapping: "mapping test value"
        });
    });

    QUnit.test("#setInvokeContextObject calls #updateSchedule with correct args", function(assert) {
        let spyArgs,
            abstractSchedulerView = new AbstractSchedulerView(),
            updateScheduleSpy = sinon.stub(abstractSchedulerView, "updateSchedule", _.noop);

        // send in prop/value for "source", "script", "mapping"
        abstractSchedulerView.setInvokeContextObject("source", "test source");
        spyArgs = updateScheduleSpy.firstCall.args

        assert.ok(updateScheduleSpy.calledOnce, "source");
        assert.equal(spyArgs[0], "invokeContext", "source");
        assert.deepEqual(spyArgs[1], {
            action: "liveSync", source: "test source"
        }, "source");

        abstractSchedulerView.setInvokeContextObject("script", "test script");
        spyArgs = updateScheduleSpy.secondCall.args;

        assert.ok(updateScheduleSpy.calledTwice, "script");
        assert.equal(spyArgs[0], "invokeContext", "script");
        assert.deepEqual(spyArgs[1], {
            script: "test script"
        }, "script");

        abstractSchedulerView.setInvokeContextObject("mapping", "test mapping");
        spyArgs = updateScheduleSpy.thirdCall.args;

        assert.ok(updateScheduleSpy.calledThrice, "mapping");
        assert.equal(spyArgs[0], "invokeContext", "mapping");
        assert.deepEqual(spyArgs[1], {
            action: "reconcile", mapping: "test mapping"
        }, "source");

    });

});
