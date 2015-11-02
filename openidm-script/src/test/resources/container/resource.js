/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
var scimUser1 = {
    "externalId": "701984",
    "userName": "bjensen@example.com",
    "name": {
        "formatted": "Ms. Barbara J Jensen III",
        "familyName": "Jensen",
        "givenName": "Barbara",
        "middleName": "Jane",
        "honorificPrefix": "Ms.",
        "honorificSuffix": "III"
    },
    "assignedDashboard": ["Salesforce", "Google", "ConstantContact"],
    "displayName": "Babs Jensen",
    "nickName": "Babs",
    "profileUrl": "https://login.example.com/bjensen"
}
/**
 * <pre>
 * create(String resourceContainer, String newResourceId, Map content[, List fieldFilter][,Map context])
 * </pre>
 */
var userC1 = router.create("Users", null, scimUser1)

var userC2 = router.create("Users", "bjensen@example.com", scimUser1)

var userC3 = router.create("Users", "paramTest", scimUser1, { "x" : "marksTheSpot" })

try {
    var userC4 = router.create("Users", "badParamTest", scimUser1, { "_x" : "badParam" })
    throw { "error" : "Assertion failed: router.create should have disallowed unknown parameter" } ;
} catch (e) {
    if (e.error) {
        throw e;
    }
}

/**
 * <pre>
 * read(String resourceName[, List fieldFilter][,Map context])
 * </pre>
 */
var userR1 = router.read("Users/" + userC1._id)

var userR2 = router.read("Users/bjensen@example.com")

var userR3 = router.read("Users/paramTest", { "x" : "marksTheSpot" })

try {
    var userR4 = router.read("Users/badParamTest", { "_x" : "badParam" })
    throw { "error" : "Assertion failed: router.read should have disallowed unknown parameter" } ;
} catch (e) {
    if (e.error) {
        throw e;
    }
}

/**
 * <pre>
 * update(String resourceName, String revision, Map content [, List fieldFilter][,Map context])
 * </pre>
 */
var scimUser1Updated = scimUser1
scimUser1Updated.userType = "Employee"
scimUser1Updated.title = "Tour Guide"
scimUser1Updated.preferredLanguage = "en_US"
scimUser1Updated.locale = "en_US"
scimUser1Updated.timezone = "America/Los_Angeles"
scimUser1Updated.active = true

var userU1 = router.update("Users/" + userC1._id, userR1._rev, scimUser1Updated)

var userU2 = router.update("Users/bjensen@example.com", userR2._rev, scimUser1Updated)

var userU3 = router.update("Users/paramTest", userR3._rev, scimUser1Updated, { "x" : "marksTheSpot" })

try {
    var userU4 = router.update("Users/paramTest", userR4._rev, scimUser1Updated, { "_x" : "badParam" })
    throw { "error" : "Assertion failed: router.update should have disallowed unknown parameter" } ;
} catch (e) {
    if (e.error) {
        throw e;
    }
}

/**
 * <pre>
 * patch(String resourceName, String revision, Map patch [, List fieldFilter][,Map context])
 * </pre>
 */
// TODO test patch

/**
 * <pre>
 * query(String resourceContainer, Map params [, List fieldFilter][,Map context])
 * </pre>
 */
var queryParams1 = {
    "_queryId": "query-all-ids"
}
var queryParams2 = {
    "_queryFilter": "nickName eq \"Babs\""
}

try {
    var userQ1 = router.query("Users", queryParams1)
} catch (e) {
    //expected
}
var userQ2 = router.query("Users", queryParams2)
var userQ3 = router.query("Users", queryParams2, callback)

var printResult = function (resource, error) {
    if (error == null) {
        java.lang.System.out.println("Result: " + resource)
    } else {
        java.lang.System.out.println("Error: " + error)
    }
}
var userQ4 = router.query("Users", queryParams2, printResult)

queryParams2.x = "marksTheSpot"
var userQ5 = router.query("Users", queryParams2, printResult)

try {
    queryParams2._x = "badParam"
    var userQ6 = router.query("Users", queryParams2, printResult)
    throw { "error" : "Assertion failed: router.query should have disallowed unknown parameter" } ;
} catch (e) {
    if (e.error) {
        throw e;
    }
}

/**
 * <pre>
 * delete(String resourceName, String revision [, params][, List fieldFilter][,Map context])
 * </pre>
 */
try {
    var userD2 = router.delete("Users", null, { "_illegal" : "parameter"})
    throw { "error" : "Assertion failed: router.delete should have disallowed unknown parameter" } ;
} catch (e) {
    if (e.error) {
        throw e;
    }
}

/**
 * <pre>
 * action(String resourceName, [String actionId,] Map params, Map content[, List fieldFilter][,Map context])
 * </pre>
 */
var userA1 = router.action("Users", "clear", {}, {})

// _parameters are allowed on action requests
var userA2 = router.action("Users", "clear", {}, {"_allowThis" : "parameter"})

var arrayVar = ["Salesforce", "Google", "ConstantContact"]

var hasGoogle = (userR1.assignedDashboard.indexOf("Google") !== -1);
var ccPosition = userR1.assignedDashboard.indexOf("ConstantContact");

if (!userR1.hasOwnProperty("profileUrl")) {
    throw { "message": "Property 'profileUrl' expected but not found" };
}

if (!hasGoogle) {
    throw { "message": "Google expected but not present" };
}

if (ccPosition !== 2) {
    throw { "message": "Constant Contact expected at position 2, indexOf returned " + ccPosition };
}

if (userR1.assignedDashboard.join("+") !== "Salesforce+Google+ConstantContact") {
    throw { "message": "AssignedDashboard array expected to be joined together into a '+' delimited string"};
}

if (userR1.name.formatted.split(' ').length !== 5) {
    throw { "message": "formatted name '" + userR1.name.formatted + "' expected to be split into a 5 element array, based on spaces."};
}

if (!createRequest instanceof org.forgerock.json.resource.CreateRequest) {
    throw { "message": "createRequest type is not match"};
}
if (!readRequest instanceof org.forgerock.json.resource.ReadRequest) {
    throw { "message": "readRequest type is not match"};
}
if (!updateRequest instanceof org.forgerock.json.resource.UpdateRequest) {
    throw { "message": "updateRequest type is not match"};
}
if (!patchRequest instanceof org.forgerock.json.resource.PatchRequest) {
    throw { "message": "patchRequest type is not match"};
}
if (!queryRequest instanceof org.forgerock.json.resource.QueryRequest) {
    throw { "message": "queryRequest type is not match"};
}
if (!deleteRequest instanceof org.forgerock.json.resource.DeleteRequest) {
    throw { "message": "deleteRequest type is not match"};
}
if (!actionRequest instanceof org.forgerock.json.resource.ActionRequest) {
    throw { "message": "actionRequest type is not match"};
}

if (context.security) {
    var securityContext = context.security;
    if (!securityContext.authenticationId == "bjensen@example.com") {
        throw {
            "code": 401,
            "reason": "Unauthorized",
            "message": "context has not SecurityContext",
            "cause": new Packages.org.forgerock.json.resource.NotFoundException("bjensen@example.com")
        };
    }
} else {
    throw { "message": "context has not SecurityContext"};
}
