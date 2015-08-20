(function() {
    if (request.content.resourceOperation.operation.method === 'create') {
        // log creates
        return true;
    } else if (request.content.resourceOperation.operation.method === 'update') {
        // do not log updates
        return false;
    } else {
        // log everything else
        return true;
    }
}());