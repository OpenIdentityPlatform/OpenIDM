function pushIfNotContains(list, item) {
    if (list.indexOf(item) === -1) {
        list.push(item);
    }
}

function setISO8601(dateInput, string) {
    var regexp = "([0-9]{4})(-([0-9]{2})(-([0-9]{2})" +
    "(T([0-9]{2}):([0-9]{2})(:([0-9]{2})(\\.([0-9]+))?)?" +
    "(Z|(([-+])([0-9]{2}):([0-9]{2})))?)?)?)?",
    d = string.match(new RegExp(regexp)),
    offset = 0, date = new Date(d[1], 0, 1), time;

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
        offset *= ((d[15] === '-') ? 1 : -1);
    }

    offset -= date.getTimezoneOffset();
    time = (Number(date) + (offset * 60 * 1000));
    dateInput.setTime(Number(time));
    return dateInput;
}

function processDelegates(delegate, candidates) {
   var startDate = setISO8601(new Date(), delegate.startDate),
    endDate = setISO8601(new Date(), delegate.endDate),
    now = new Date();
    
    if (startDate < now && now < endDate ) {
        pushIfNotContains(candidates, delegate.to);
    }
}

(function () {
    if (request.method === "query") {
        if (!request.additionalParameters.userId) {
        throw "Parameter userId is required";   
        }
        var userId = request.additionalParameters.userId,
        user = openidm.read("managed/user/"+userId), i,
        managerCandidates = ["superadmin"], manager;

        if (!user) {
            throw "User not found: " + userId;
        }

        if (user.manager && user.manager.managerId) {
            pushIfNotContains(managerCandidates, user.manager.managerId);
            manager = openidm.read("managed/user/"+user.manager.managerId);
            if (manager.delegates !== undefined) {
                for (i = 0; i < manager.delegates.length; i++) {
                    processDelegates(manager.delegates[i], managerCandidates);
                }
            }
        }

        return managerCandidates;
    } else {
        throw "Unsupported operation: " + request.method;
    }
}());
