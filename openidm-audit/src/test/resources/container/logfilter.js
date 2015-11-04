(function() {
    if (request.content.operation === 'create') {
        // log creates
        return true;
    } else if (request.content.operation === 'update') {
        // do not log updates
        return false;
    } else {
        // log everything else
        return true;
    }
}());