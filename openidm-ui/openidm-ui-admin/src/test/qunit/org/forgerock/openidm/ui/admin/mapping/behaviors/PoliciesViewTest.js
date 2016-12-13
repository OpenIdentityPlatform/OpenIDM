define([
    "org/forgerock/openidm/ui/admin/mapping/behaviors/PoliciesView"
], function (PoliciesView) {
    QUnit.module('PoliciesView Tests');

    var lookup = {
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
            changes;

        changes = PoliciesView.detectChanges(oldPolicy, newPolicy, baseSituations, lookup);

        QUnit.equal(changes.newPoliciesFilledIn.length, 2, "Generated new policy list filled in with appropriate properties");
        QUnit.equal(changes.newPoliciesFilledIn[0].condition, null,  "Added condition");
    });

    // Using real data for 4 of 13 possible situations to keep the test trim.
    // Situations: AMBIGUOUS, SOURCE_MISSING, TARGET_IGNORED, CONFIRMED
    QUnit.test("Set policies with one newly added policy", function () {
        var baseSituations = {
            "AMBIGUOUS":{
                "severity":"failure-display",
                "situation":"Ambiguous",
                "action":"EXCEPTION",
                "displayAction":"Exception",
                "defaultActionStar":true,
                "defaultActionHollow":false,
                "emphasize":false,
                "condition":null,
                "displayCondition":"",
                "postAction":null,
                "displayPostAction":"",
                "note":"Source object correlates to multiple target objects, without a link.",
                "disabled":true,
                "options":[
                    "IGNORE",
                    "REPORT",
                    "NOREPORT",
                    "ASYNC"
                ]
            },
            "SOURCE_MISSING":{
                "severity":"failure-display",
                "situation":"Source Missing",
                "action":"EXCEPTION",
                "displayAction":"Exception",
                "defaultActionStar":true,
                "defaultActionHollow":false,
                "emphasize":false,
                "condition":null,
                "displayCondition":"",
                "postAction":null,
                "displayPostAction":"",
                "note":"Valid target found, link found.",
                "disabled":true,
                "options":[
                    "DELETE",
                    "UNLINK",
                    "IGNORE",
                    "REPORT",
                    "NOREPORT",
                    "ASYNC"
                ]
            },
            "TARGET_IGNORED":{
                "severity":"warning-display",
                "situation":"Target Ignored",
                "action":"IGNORE",
                "displayAction":"Ignore",
                "defaultActionStar":true,
                "defaultActionHollow":false,
                "emphasize":false,
                "condition":null,
                "displayCondition":"",
                "postAction":null,
                "displayPostAction":"",
                "note":"Does not pass validTarget script.",
                "disabled":true,
                "options":[
                    "DELETE",
                    "UNLINK",
                    "EXCEPTION",
                    "REPORT",
                    "NOREPORT",
                    "ASYNC"
                ]
            },
            "CONFIRMED":{
                "severity":"success-display",
                "situation":"Confirmed",
                "action":"UPDATE",
                "displayAction":"Update",
                "defaultActionStar":true,
                "defaultActionHollow":false,
                "emphasize":false,
                "condition":null,
                "displayCondition":"",
                "postAction":null,
                "displayPostAction":"",
                "note":"Valid source and target objects linked.",
                "disabled":true,
                "options":[
                    "IGNORE",
                    "REPORT",
                    "NOREPORT",
                    "ASYNC"
                ]
            }
        },
            patterns = {
                "Read-only":{
                    "policies":[
                        {
                            "situation":"AMBIGUOUS",
                            "action":"ASYNC"
                        },
                        {
                            "situation":"SOURCE_MISSING",
                            "action":"ASYNC"
                        },
                        {
                            "situation":"TARGET_IGNORED",
                            "action":"ASYNC"
                        },
                        {
                            "situation":"CONFIRMED",
                            "action":"ASYNC"
                        }
                    ],
                    "description":"This is the default for new mappings"
                },
                "Custom":{
                    "policies":[],
                    "description":"User defined situational policies."
                }
            },
            testPolicy1 = [
                {
                    "severity":"warning-display",
                    "situation":"TARGET_IGNORED",
                    "action":"IGNORE",
                    "displayAction":"Ignore",
                    "defaultActionStar":true,
                    "defaultActionHollow":false,
                    "emphasize":false,
                    "displayCondition":"",
                    "displayPostAction":"",
                    "note":"Does not pass validTarget script.",
                    "disabled":true
                },
                {
                    "severity":"success-display",
                    "situation":"CONFIRMED",
                    "action":"UPDATE",
                    "displayAction":"Update",
                    "defaultActionStar":true,
                    "defaultActionHollow":false,
                    "emphasize":false,
                    "displayCondition":"",
                    "displayPostAction":"",
                    "note":"Valid source and target objects linked.",
                    "disabled":true
                },
                {
                    "severity":"failure-display",
                    "situation":"AMBIGUOUS",
                    "action":"EXCEPTION",
                    "displayAction":"Exception",
                    "defaultActionStar":true,
                    "defaultActionHollow":false,
                    "emphasize":false,
                    "displayCondition":"",
                    "displayPostAction":"",
                    "note":"Source object correlates to multiple target objects, without a link.",
                    "disabled":true
                },
                {
                    "severity":"failure-display",
                    "situation":"SOURCE_MISSING",
                    "action":"DELETE",
                    "displayAction":"Delete",
                    "defaultActionStar":false,
                    "defaultActionHollow":true,
                    "emphasize":false,
                    "displayCondition":"",
                    "displayPostAction":"",
                    "note":"Valid target found, link found.",
                    "disabled":false
                },
                {
                    "situation":"CONFIRMED",
                    "action":"CREATE"
                }
            ],
            testPolicy2= [
                {
                    "situation":"AMBIGUOUS",
                    "action":"ASYNC"
                },
                {
                    "situation":"SOURCE_MISSING",
                    "action":"ASYNC"
                },
                {
                    "situation":"TARGET_IGNORED",
                    "action":"ASYNC"
                },
                {
                    "situation":"CONFIRMED",
                    "action":"ASYNC"
                }
            ],
            changes;

        changes = PoliciesView.setPolicies(testPolicy1, patterns, lookup, baseSituations);

        QUnit.equal(changes.transformedPolicies.length, 5, "All policies exist after being transformed");

        // Policies are reordered from TARGET_IGNORED, CONFIRMED, AMBIGUOUS, SOURCE_MISSING, CONFIRMED
        // to mirror the order of the base situations: AMBIGUOUS, SOURCE_MISSING, TARGET_IGNORED, CONFIRMED, CONFIRMED
        QUnit.equal(changes.transformedPolicies[0].situation, "Ambiguous", "First policy is reordered and reformatted to 'Ambiguous'");
        QUnit.equal(changes.transformedPolicies[1].situation, "Source Missing", "Second policy is reordered and reformatted to 'Source Missing'");
        QUnit.equal(changes.transformedPolicies[2].situation, "Target Ignored", "Third policy is reordered and reformatted to 'Target Ignored'");
        QUnit.equal(changes.transformedPolicies[3].situation, "Confirmed", "Fourth policy is reordered and reformatted to 'Confirmed'");
        QUnit.equal(changes.transformedPolicies[4].situation, "Confirmed", "Fifth policy is reordered and reformatted to 'Confirmed'");

        QUnit.equal(changes.matchedPattern, "Custom", "The pattern is seen as custom");

        changes = PoliciesView.setPolicies(testPolicy2, patterns, lookup, baseSituations);
        QUnit.equal(changes.matchedPattern, "Read-only", "The pattern is seen as read-only");

    });

    QUnit.test("Delete ", function () {
        var policies = [
            {
                "severity":"failure-display",
                "situation":"Ambiguous",
                "action":"CREATE",
                "displayAction":"Create",
                "defaultActionStar":false,
                "defaultActionHollow":false,
                "emphasize":false,
                "displayCondition":"",
                "displayPostAction":"",
                "note":"Source object correlates to multiple target objects, without a link.",
                "disabled":true
            },
            {
                "severity":"failure-display",
                "situation":"Source Missing",
                "action":"UPDATE",
                "displayAction":"Update",
                "defaultActionStar":false,
                "defaultActionHollow":false,
                "emphasize":false,
                "displayCondition":"",
                "displayPostAction":"",
                "note":"Valid target found, link found.",
                "disabled":true
            },
            {
                "severity":"warning-display",
                "situation":"Target Ignored",
                "action":"NOREPORT",
                "displayAction":"No Report",
                "defaultActionStar":false,
                "defaultActionHollow":true,
                "emphasize":false,
                "displayCondition":"",
                "displayPostAction":"",
                "note":"Does not pass validTarget script.",
                "disabled":false
            },
            {
                "severity":"warning-display",
                "situation":"Target Ignored",
                "action":"UPDATE",
                "displayAction":"No Report",
                "defaultActionStar":false,
                "defaultActionHollow":true,
                "emphasize":false,
                "displayCondition":"",
                "displayPostAction":"",
                "note":"Does not pass validTarget script.",
                "disabled":false
            },
            {
                "severity":"success-display",
                "situation":"Confirmed",
                "action":"UPDATE",
                "displayAction":"Update",
                "defaultActionStar":true,
                "defaultActionHollow":false,
                "emphasize":false,
                "displayCondition":"",
                "displayPostAction":"",
                "note":"Valid source and target objects linked.",
                "disabled":true
            }
        ],
            allEventHooks = $("<div>" +
                "<div class='event-hook'>0</div>" +
                "<div class='event-hook'>1</div>" +
                "<div class='event-hook'><span class='delete-policy'></span>2</div>" +
                "<div class='event-hook'><span class='delete-policy'></span>3</div>" +
                "<div class='event-hook'></div>4</div>").find(".event-hook"),
            clickedButton = allEventHooks.eq(3).find(".delete-policy")[0],
            newPolicy;

        newPolicy = PoliciesView.getDeleteUpdatedPolicies(policies, allEventHooks, clickedButton, lookup);

        QUnit.equal(newPolicy.length, 4, "A policy should have been removed");
        QUnit.equal(newPolicy[3].situation, "CONFIRMED", "Deleted policy should be replaced with the next available policy");
    });
});