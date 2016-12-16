define([
    "org/forgerock/openidm/ui/admin/settings/audit/AuditFilterPoliciesView"
], function (AuditFilterPoliciesView) {
    QUnit.module('AuditFilterPoliciesView Tests');

    var filterPolicies = {
        "field" : {
            "excludeIf" : [ ],
            "includeIf" : ["/access/filter/field1", "/access/filter/field2"]
        },
        "value" : {
            "excludeIf": ["/access/filter/value1", "/access/filter/value2", "/access/filter/value3"],
            "includeIf": ["/access/filter/value4"]
        }
    };

    QUnit.test("Format filter data", function () {
        var changes = AuditFilterPoliciesView.formatData(filterPolicies);
        QUnit.equal(changes.length, 6, "Generated data should have a length of 6");
    });

    QUnit.test("Delete a filter", function () {
        var allEls = $("<tr class='filter'>/access/filter/field1</tr>" +
                "<tr class='filter'>/access/filter/field2</tr>" +
                "<tr class='filter'>/access/filter/value1</tr>" +
                "<tr class='filter'>/access/filter/value2</tr>" +
                "<tr class='filter'>/access/filter/value3</tr>" +
                "<tr class='filter'>/access/filter/value4</tr>"),
            clicked = allEls[3],
            uiFormattedFilters = AuditFilterPoliciesView.formatData(filterPolicies);

        var changes = AuditFilterPoliciesView.deleteFilter(clicked, allEls, uiFormattedFilters, filterPolicies);

        QUnit.equal(changes.value.excludeIf.length, 2, "A value was removed from the filter policies value excludeIf category");
        QUnit.equal($.inArray("/access/filter/value2", changes.value.excludeIf), -1, "The value of the third clicked element is removed form the filter policies.");
    });

});