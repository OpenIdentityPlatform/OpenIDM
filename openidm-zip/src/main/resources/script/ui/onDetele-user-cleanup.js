var userId = object._id;

var findUserNotificationsParams = {
    "_queryId": "notifications-for-user",
    "userId": userId
};

notificationQueryResult = openidm.query("repo/ui/notification", findUserNotificationsParams);
if (notificationQueryResult.result && notificationQueryResult.result.length!=0) {
        
    for (notificationPointer in notificationQueryResult.result) {
        var notification = notificationQueryResult.result[notificationPointer];
        openidm['delete']('repo/ui/notification/' + notification._id, notification._rev);
    }
        
}