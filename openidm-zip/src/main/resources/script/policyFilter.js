var params =  new Object();
params._action = "validateObject";
params._caller = "filterEnforcer";

var result, i,j, message = [];

if (!(request.id.indexOf("policy/")==0)) {
    result = openidm.action("policy/" + request.id, params, request.value);
    
    if (!result.result) {
        for (i = 0;i<result.failedPolicyRequirements.length;i++) {
            for (j = 0;j<result.failedPolicyRequirements[i].policyRequirements.length;j++) {
                message.push(result.failedPolicyRequirements[i].policyRequirements[j].policyRequirement)
            }
        }
        throw "Policy validation failed: " + message.join(",");
    }
}
