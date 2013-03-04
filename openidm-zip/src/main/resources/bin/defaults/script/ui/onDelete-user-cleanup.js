/** 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/*global object */

var userId = object._id,
    notificationPointer,
    findUserNotificationsParams = {
        "_queryId": "get-notifications-for-user",
        "userId": userId
    },
    notificationQueryResult;

notificationQueryResult = openidm.query("/repo/ui/notification", findUserNotificationsParams);
if (notificationQueryResult.result && notificationQueryResult.result.length!==0) {
        
    for (notificationPointer=0;notificationPointer<notificationQueryResult.result.length;notificationPointer++) {
        var notification = notificationQueryResult.result[notificationPointer];
        openidm['delete']('repo/ui/notification/' + notification._id, notification._rev);
    }
        
}