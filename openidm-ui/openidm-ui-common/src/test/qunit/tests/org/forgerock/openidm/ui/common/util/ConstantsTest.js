define([
    "org/forgerock/openidm/ui/common/util/Constants"
], function (Constants) {
    QUnit.module('Constants Tests');

    QUnit.test("Constants.context defaults to 'openidm'", function (assert) {
        assert.equal(Constants.context, "openidm",
            "The default context should be 'openidm'");
    });

    QUnit.test("Constants.context produces correct REST API path prefix", function (assert) {
        var contextPath = "/" + Constants.context;
        assert.equal(contextPath, "/openidm",
            "The default REST API path prefix should be '/openidm'");
    });

    QUnit.test("Constants.context can be overridden for custom REST path", function (assert) {
        var originalContext = Constants.context;
        Constants.context = "myidm";
        assert.equal(Constants.context, "myidm",
            "Constants.context should be overridable to a custom value");
        assert.equal("/" + Constants.context, "/myidm",
            "Custom context should produce the correct REST API path prefix");
        Constants.context = originalContext;
    });
});