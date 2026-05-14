function startCertificationWorkflow(role) {
    var params = {
        "roleId" : role._id,
        "_key": "certificationEntitlements",
        "_businessKey" : "RoleId: " + role._id
    };
 
    openidm.create('workflow/processinstance', null,  params);
}

var allRoles = openidm.query("managed/role", {
    "_queryId" : "query-all-ids"
}), i;

for (i = 0; i < allRoles.result.length; i++) {
    startCertificationWorkflow(allRoles.result[i]);
}

