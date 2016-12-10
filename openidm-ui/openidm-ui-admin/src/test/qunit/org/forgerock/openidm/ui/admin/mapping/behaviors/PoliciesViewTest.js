define([
    "org/forgerock/openidm/ui/admin/mapping/behaviors/PoliciesView"
], function (PoliciesView) {
    QUnit.module('PoliciesView Tests');

    QUnit.test("Load default patterns", function () {
        var oldPolicy = [{
                action : "EXCEPTION",
                situation : "AMBIGUOUS"
            }],
            newPolicy = [{
                action : "EXCEPTION",
                situation : "AMBIGUOUS"
            }, {
                action : "ABSENT",
                situation : "FOUND"
            }],
            baseSituations = {
                "FOUND" : {},
                "AMBIGUOUS" : {}
            },
            changes,
            lookup = {
                "SOURCE_MISSING": "Source Missing",
                "ALL_GONE": "All Gone",
                "SOURCE_IGNORED": "Source Ignored",
                "UNQUALIFIED": "Unqualified",
                "AMBIGUOUS": "Ambiguous",
                "FOUND_ALREADY_LINKED": "Found Already Linked",
                "CONFIRMED": "Confirmed",
                "UNASSIGNED": "Unassigned",
                "LINK_ONLY": "Link Only",
                "TARGET_IGNORED": "Target Ignored",
                "MISSING": "Missing",
                "ABSENT": "Absent",
                "FOUND": "Found",
                "IGNORE": "Ignore",
                "DELETE": "Delete",
                "UNLINK": "Unlink",
                "EXCEPTION": "Exception",
                "REPORT": "Report",
                "NOREPORT": "No Report",
                "ASYNC": "Async",
                "CREATE": "Create",
                "UPDATE": "Update"

        };

        changes = PoliciesView.detectChanges(oldPolicy, newPolicy, baseSituations, lookup);

        QUnit.equal(changes.newPoliciesFilledIn.length, 2, "Generated new policy list filled in with appropriate properties");
        QUnit.equal(changes.newPoliciesFilledIn[0].condition, null,  "Added condition");
    });
});