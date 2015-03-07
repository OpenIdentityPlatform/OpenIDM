(function() {
    if (request.content.action === 'create') {
        // log creates
        return true;
    } else if (request.content.action === 'update') {
        // do not log updates
        return false;
    } else {
        // log everything else
        return true;
    }
}());