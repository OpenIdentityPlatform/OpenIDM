(function() {
    //TODO-crest3 note that the script dot notation no longer seems to work.

    var method = request.content.asMap().get("resourceOperation").get("operation").get("method");
    if (method == 'create') {
        // log creates
        return true;
    } else if (method == 'update') {
        // do not log updates
        return false;
    } else {
        // log everything else
        return true;
    }

    //if (request.content.resourceOperation.operation.method === 'create') {
    //    // log creates
    //    return true;
    //} else if (request.content.resourceOperation.operation.method === 'update') {
    //    // do not log updates
    //    return false;
    //} else {
    //    // log everything else
    //    return true;
    //}
}());