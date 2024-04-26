define([
    "org/forgerock/openidm/ui/admin/connector/EditConnectorView",
    "lodash",
    "sinon",
    "org/forgerock/openidm/ui/admin/connector/oauth/GoogleTypeView"
],
function (EditConnectorView, _,
          sinon,
          GoogleTypeView) {
    QUnit.module('Connectors');

    QUnit.test("Advanced Connector Save", function (assert) {
        var oldConnector = {
                "resultsHandlerConfig" : {
                    "enableNormalizingResultsHandler" : true,
                    "enableFilteredResultsHandler" : true,
                    "enableCaseInsensitiveFilter" : true,
                    "enableAttributesToGetSearchResultsHandler" : true
                },
                "poolConfigOption" : {
                    "maxObjects" : 10,
                    "maxIdle" : 10,
                    "maxWait" : 150000,
                    "minEvictableIdleTimeMillis" : 120000,
                    "minIdle" : 1
                },
                "operationTimeout" : {
                    "CREATE" : -1,
                    "VALIDATE" : -1,
                    "TEST" : -1,
                    "SCRIPT_ON_CONNECTOR" : -1,
                    "SCHEMA" : -1,
                    "DELETE" : -1,
                    "UPDATE" : -1,
                    "SYNC" : -1,
                    "AUTHENTICATE" : -1,
                    "GET" : -1,
                    "SCRIPT_ON_RESOURCE" : -1,
                    "SEARCH" : -1
                }
            },
            newConnector = {
                "resultsHandlerConfig" : {
                    "enableNormalizingResultsHandler" : false,
                    "enableFilteredResultsHandler" : false
                },
                "poolConfigOption" : {
                    "maxObjects" : 25
                },
                "operationTimeout" : {
                    "CREATE" : 5,
                    "AUTHENTICATE" : "5"
                }
            },
            results = EditConnectorView.advancedDetailsGenerate(oldConnector, newConnector);

        assert.equal(results.resultsHandlerConfig.enableNormalizingResultsHandler, false, "Boolean value successfully converted and saved");
        assert.equal(results.poolConfigOption.maxObjects, 25, "Pool Config successfully saved");
        assert.equal(results.operationTimeout.CREATE, 5, "Operation Timeout successfully saved");
        assert.equal(results.poolConfigOption.maxIdle, 10, "Original maxIdle exists");
    });

    QUnit.test("OAuth Connector Whitespace Trimming", function (assert) {
        var whiteSpaceMerge = {
                "configurationProperties" : {
                    "clientSecret" : "   test  ",
                    "clientId" : "  test     "
                }
            };

        whiteSpaceMerge = GoogleTypeView.cleanSpacing(whiteSpaceMerge);

        assert.equal(whiteSpaceMerge.configurationProperties.clientSecret, "test", "ClientSecret white space trimmed");
        assert.equal(whiteSpaceMerge.configurationProperties.clientId, "test", "ClientId white space trimmed");
    });

    QUnit.test("Generate correct connector patch", function (assert) {
        var connector = {
            "test" : "stuff"
        },
        change = {
            "test" : "new stuff"
        },
        patch = EditConnectorView.generateConnectorPatch(connector, change, null);

        assert.equal(patch[0].value, "new stuff", "Correctly generated patch value");
        assert.equal(patch[1].field, "/enabled", "Correctly disable connector for testing");
    });

    QUnit.test("Testing a connector with pass result", function (assert) {
        var done = assert.async();

        var connectorPassStub = sinon.stub(EditConnectorView, "connectorPass", function(preTestResult, updatedForm){
                done();

                assert.equal(preTestResult, true, "Promise correctly resolved and called pass function");

                connectorPassStub.restore();
            }),
            testPromise = $.Deferred();

        EditConnectorView.connectorTest(testPromise, true, true);

        testPromise.resolve();
    });

    QUnit.test("Testing a connector with fail result", function (assert) {
        var done = assert.async();

        var connectorFailstub = sinon.stub(EditConnectorView, "connectorFail", function(preTestResult, updatedForm, message){
                done();

                assert.equal(preTestResult, false, "Promise correctly resolved and called fail function");

                connectorFailstub.restore();
            }),
            testPromise = $.Deferred();

        EditConnectorView.connectorTest(testPromise, false, false);

        testPromise.reject();
    });

});
