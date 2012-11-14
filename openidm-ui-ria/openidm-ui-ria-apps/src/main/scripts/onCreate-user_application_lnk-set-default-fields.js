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
 * This script generates user UUID and sets default fields. 
 * It forces that user role is openidm-authorized and account status
 * is active.
 * 
 * It is run every time new user is created.
 */  
  
var userApplicationLnk = openidm.decrypt(object);
var params = {
    "_queryId": "get-user-app-link-by-user-and-app",
    "uid": userApplicationLnk.userId,
    "applicationId": userApplicationLnk.applicationId
};
      
result = openidm.query("managed/user_application_lnk", params);
      
if ((result.result && result.result.length!=0) || (result.results && result.results.length!=0)) {
    throw "Failed to create user application link. User already has this application";
}