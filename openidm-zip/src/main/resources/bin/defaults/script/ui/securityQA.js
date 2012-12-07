/** 
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
 * @author jfeasel
 * 
 * This script checks if security answer passed in parameters
 * equals user's security answer.
 * 
 * It may reset the password as part of the request, if a new one is present
 * 
 * This endpoint expects these parameters:
 * 
 *  _action: one of (securityQuestionForUserName|checkSecurityAnswerForUserName|setNewPasswordForUserName)
 *  uid: userName of the managed/user record
 *  securityAnswer : answer to security question; used by actions checkSecurityAnswerForUserName and setNewPasswordForUserName
 *  newPassword: new password to assign to user; used by action setNewPasswordForUserName
 * 
 */


if (request.method !== "query") {
    throw { 
        "openidmCode" : 403, 
        "message" : "Access denied"
    } 
}

var response    = {},
    userQuery   = {},
    user        = {},
    patch       = [];

if (
        request.params["_action"] === "securityQuestionForUserName" ||
        request.params["_action"] === "checkSecurityAnswerForUserName" ||
        request.params["_action"] === "setNewPasswordForUserName" ) {
    
    if (request.params.uid) {
        userQuery = openidm.query("managed/user", {"_queryId": "for-userName", "uid": request.params.uid } );
        
        if (userQuery.result.length) {
            
            user = userQuery.result[0];
            
            if (request.params["_action"] === "securityQuestionForUserName") {
                response.securityQuestion = user.securityQuestion;
            }
            else if ( 
                        request.params["_action"] === "checkSecurityAnswerForUserName" ||
                        request.params["_action"] === "setNewPasswordForUserName") {
                try {
                    user.securityAnswerAttempts = (typeof (user.securityAnswerAttempts) === "number") ? user.securityAnswerAttempts+1 : 1;
                    
                    // This could throw a policy violation if there is one in place enforcing a maximum number of attempts 
                    openidm.patch("managed/user/" + user._id, null, [{"replace": "securityAnswerAttempts", "value": user.securityAnswerAttempts}]); 

                    if(!user.securityAnswer || openidm.decrypt(user.securityAnswer) !== request.params['securityAnswer']) {
                        throw "Incorrect Answer";
                    } else {

                        user = openidm.read("managed/user/" + userQuery.result[0]._id);
                        patch.push({"replace": "securityAnswerAttempts", "value":0});
                        
                        if (request.params["_action"] === "setNewPasswordForUserName") {
                            logger.info("Setting new password for {}", request.params['username']);
                            patch.push({"replace": "password", "value":request.params["newPassword"]});
                        } else {
                            // used by the UI to validate passwords before actually submitting them to be changed
                            response._id = user._id;
                        }
                        
                        openidm.patch("managed/user/" + user._id, null, patch);
                        
                        response.result = "correct";
                        
                    }
                    
                
                }
                catch (err) {
                    user = openidm.read("managed/user/" + userQuery.result[0]._id);
                    openidm.patch("managed/user/" + user._id, null, [{"replace": "lastSecurityAnswerAttempt", "value": (new Date()).toString()}]);
                    
                    response.errorDetail = err;
                    response.result = "error";
                }
                
                
            }
        }
        
    }
}
else { // apparently they have provided an unsupported action
    throw { 
        "openidmCode" : 403, 
        "message" : "Access denied"
    } 
}


response;