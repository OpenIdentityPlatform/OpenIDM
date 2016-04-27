/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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
 * Prevents roles from being deleted that are currently assigned to users
 */

/*global object */

// Query members of a role
var resourcePath = "managed/role/" +  org.forgerock.http.util.Uris.urlEncodePathElement(object._id) + "/members";
var users = openidm.query(resourcePath, {"_queryFilter": "true"}, ["*"]).result;

if (users.length > 0) {
    throw {
        "code" : 409,
        "message" : "Cannot delete a role that is currently assigned"
    };
}
