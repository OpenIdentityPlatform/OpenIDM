/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All rights reserved.
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

/**
 * @author mbilski
 *
 * Endpoint for managing user notifications
 *
 */
(function () {
    var userId = context.security.authorizationId.id, res, ret, params, notification;

    if (request.method === "read") {
        res = {};
        params = {
            "_queryId": "get-notifications-for-user",
            "userId": userId
        };
        ret = openidm.query("repo/ui/notification", params);

        if(ret && ret.result) {
            res = ret.result;
        }

        return res;

    } else if (request.method === "delete") {
        notification = openidm.read("repo/ui/notification/"+request.resourcePath);

        if(notification !== null) {
            if (notification.receiverId === userId) {
                openidm['delete']('repo/ui/notification/' + notification._id, notification._rev);
            } else {
                throw {
                    "code": 403,
                    "message": "Access denied"
                };
            }
        }
    } else {
        throw {
            "code" : 403,
            "message" : "Access denied"
        };
    }

}());