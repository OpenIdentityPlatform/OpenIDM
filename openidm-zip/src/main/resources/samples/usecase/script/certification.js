function startCertificationWorkflow(user) {
    var params = {
        "userId" : user._id,
        "_key": "certification",
        "_businessKey" : "UserId: " + user._id
    };
 
    openidm.action('workflow/processinstance', {
        "_action" : "createProcessInstance"
    }, params);
}

var allUsers = openidm.query("managed/user", {
    "_queryId" : "query-all-ids"
}), i;

for (i = 0; i < allUsers.result.length; i++) {
    startCertificationWorkflow(allUsers.result[i]);
}

