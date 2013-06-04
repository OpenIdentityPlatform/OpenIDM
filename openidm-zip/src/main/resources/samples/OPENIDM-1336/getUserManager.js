if (request.method == "query") {
    userId = request.params.userId
    user = openidm.read("managed/user/"+userId)
    manager = openidm.read("managed/user/"+user.manager.managerId)
    delegates = []
    if (manager.delegates !== undefined) {
        for (i = 0; i < manager.delegates.length; i++) {
            processDelegates(manager.delegates[i])
        }
    }
    if (delegates.length > 0) {
        delegates.toString()
    } else {
        if (user.manager) {
            user.manager.managerId
        } else {
            "superadmin"
        }
    }
} else {
    throw "Unsupported operation: " + request.method;
}

function processDelegates(delegate) {
    startDate = setISO8601(new Date(), delegate.startDate);
    endDate = setISO8601(new Date(), delegate.endDate);
    var now = new Date();
    
    if (startDate < now && now < endDate ) {
        delegates.push(delegate.to)
    }
}

function setISO8601(dateInput, string) {
    var regexp = "([0-9]{4})(-([0-9]{2})(-([0-9]{2})" +
    "(T([0-9]{2}):([0-9]{2})(:([0-9]{2})(\.([0-9]+))?)?" +
    "(Z|(([-+])([0-9]{2}):([0-9]{2})))?)?)?)?";
    var d = string.match(new RegExp(regexp));

    var offset = 0;
    var date = new Date(d[1], 0, 1);

    if (d[3]) {
        date.setMonth(d[3] - 1);
    }
    if (d[5]) {
        date.setDate(d[5]);
    }
    if (d[7]) {
        date.setHours(d[7]);
    }
    if (d[8]) {
        date.setMinutes(d[8]);
    }
    if (d[10]) {
        date.setSeconds(d[10]);
    }
    if (d[12]) {
        date.setMilliseconds(Number("0." + d[12]) * 1000);
    }
    if (d[14]) {
        offset = (Number(d[16]) * 60) + Number(d[17]);
        offset *= ((d[15] == '-') ? 1 : -1);
    }

    offset -= date.getTimezoneOffset();
    time = (Number(date) + (offset * 60 * 1000));
    dateInput.setTime(Number(time));
    return dateInput
}