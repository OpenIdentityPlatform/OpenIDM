define([
    "org/forgerock/openidm/ui/common/delegates/SearchDelegate"
], function (SearchDelegate) {
    QUnit.module('Search Delegate Tests');

    QUnit.test("query Filter Builder", function (assert) {
        var props = ["userName", "givenName", "sn"],
            search = "test",
            additionalQuery = true,
            comparisonOperator = null;

        assert.equal(SearchDelegate.generateQueryFilter(props, search), '(userName sw "test" or (givenName sw "test" or (sn sw "test")))', "Basic Query Filter Generated");
        assert.equal(SearchDelegate.generateQueryFilter(props, search, additionalQuery, comparisonOperator), '((userName sw "test" or (givenName sw "test" or (sn sw "test"))) and (true))', "Complex Query Filter Generated");
    });
});
