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

// Get the current session's user information
var val;
if (request.method == "read") {
    var secCtx = request.parent.security;
    if (secCtx && secCtx["userid"]) {
        val = {"username" : secCtx["username"], 
               "userid" : {
                    "component" : secCtx["userid"]["component"], 
                    "id" : secCtx["userid"]["id"]
               }
        }; 
    } else if (secCtx) {
        val = {"username" : secCtx["user"]};
    } else {
        throw "Invalid security context, can not retrieve user information associated with the session.";
    }
} else {
    throw "Unsupported operation on info login service: " + request.method;
}
val;
