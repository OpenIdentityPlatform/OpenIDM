var params =  new Object();
params._action = "validateObject";

var result;

if (!(request.id.indexOf("policy/")==0)) {
    result = openidm.action("policy/" + request.id, params, request.value);
    
    if (!result.result) {
        throw "Policy validation failed on " + request.id;
    }
}


