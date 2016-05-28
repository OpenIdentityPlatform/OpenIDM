define([
    "lodash",
    "sinon",
    "org/forgerock/openidm/ui/common/resource/ResourceCollection"
], function (_, sinon, ResourceCollection) {
    QUnit.module('ResourceCollection Functions');

    QUnit.test("initialize properly", function () {
        var rc = new ResourceCollection([], {
            url: "/testEndpoint",
            _queryFilter: 'true',
            isSystemResource: true
        });
        QUnit.equal(rc.queryParams.sortKey, "_sortKeys", "queryParams uses _sortKeys");
        QUnit.equal(rc.queryParams.pageSize, "_pageSize", "queryParams uses _pageSize");
        QUnit.equal(rc.queryParams._totalPagedResultsPolicy, "ESTIMATE", "queryParams uses _totalPagedResultsPolicy=ESTIMATE");
    });

});
