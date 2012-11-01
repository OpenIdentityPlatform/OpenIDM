// Get the current session's user information
var val;
if (request.method == "read") {
    var secCtx = request.parent.security;
    if (secCtx && secCtx["userid"]) {
        val = {"username" : secCtx["username"], 
               "userid" : {
                    "component" : secCtx["userid"]["component"], 
                    "id" : secCtx["userid"]["id"]
               }
        }; 
    } else if (secCtx) {
        val = {"username" : secCtx["user"]};
    } else {
        throw "Invalid security context, can not retrieve user information associated with the session.";
    }
} else {
    throw "Unsupported operation on info login service: " + request.method;
}
val;
