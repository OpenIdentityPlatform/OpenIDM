var userId = object._id;

var findUserApplicationLnksParams = {
    "_query-id": "user_application_lnk-for-user",
    "userId": userId
};

userApplicationLnksQueryResult = openidm.query("managed/user_application_lnk", findUserApplicationLnksParams);
if (userApplicationLnksQueryResult.result && userApplicationLnksQueryResult.result.length!=0) {
    
    for (userApplicationLnksPointer in userApplicationLnksQueryResult.result) {
        var userApplicationLnk = userApplicationLnksQueryResult.result[userApplicationLnksPointer];
        openidm['delete']('managed/user_application_lnk/' + userApplicationLnk._id, userApplicationLnk._rev);
    }
    
}


var findUserNotificationsParams = {
    "_query-id": "notifications-for-user",
    "userId": userId
};

notificationQueryResult = openidm.query("repo/ui/notification", findUserNotificationsParams);
if (notificationQueryResult.result && notificationQueryResult.result.length!=0) {
        
    for (notificationPointer in notificationQueryResult.result) {
        var notification = notificationQueryResult.result[notificationPointer];
        openidm['delete']('repo/ui/notification/' + notification._id, notification._rev);
    }
        
}