
allUsers = openidm.query("managed/user", {
    "_queryId" : "query-all-ids"
})

for (i = 0; i < allUsers.result.length; i++) {
    startCertificationWorkflow(allUsers.result[i])
}

function startCertificationWorkflow(user) {
    var params = {
        "userId" : user._id,
        "_key": "certification"
    };
 
    openidm.action('workflow/processinstance', {
        "_action" : "createProcessInstance"
    }, params);
}