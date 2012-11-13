var params =  new Object();
params._action = "validateObject";
params._caller = "filterEnforcer";

var result;

if (!(request.id.indexOf("policy/")==0)) {
    result = openidm.action("policy/" + request.id, params, request.value);
    
    if (!result.result) {
        throw { 
            "openidmCode" : 403, 
            "message" : "Policy validation failed",
            "detail" : result 
        }  
    }
}
