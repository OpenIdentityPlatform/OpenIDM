if (request.method == "query") {
    userId = request.params.userId
    user = openidm.read("managed/user/"+userId)
    //    manager = openidm.read("managed/user/"+user.manager.managerId)
    //    if (manager.delegates !== undefined) {
    //        delegates = []
    //        for (i = 0; i < manager.delegates.length; i++) {
    //            processDelegates(manager.delegates[i])
    //        }
    //    }
    if (user.manager) {
        user.manager.managerId
    } else {
        "superadmin"
    }
} else {
    throw "Unsupported operation: " + request.method;
}

function processDelegates(delegate) {
    delegates.push(delegate.to)
}