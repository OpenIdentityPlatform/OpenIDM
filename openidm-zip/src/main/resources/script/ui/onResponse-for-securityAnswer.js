/*! @license 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2012 ForgeRock AS. All rights reserved.
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
 * @author yaromin
 * 
 * This script checks if security answer passed in parameters
 * equals user's security answer.
 * 
 * It is run on response to for-securityAnswer query.
 */
user = openidm.read("managed/user/" + response.result[0]._id);
user.securityAnswerAttempts++;

try {

    // This could throw a policy violation if there is one in place enforcing a maximum number of attempts 
    openidm.update("managed/user/" + response.result[0]._id, user._rev, user); 
    
    if(!user.securityAnswer || openidm.decrypt(user.securityAnswer) !== request.params['securityAnswer']) {
        throw "Incorrect Answer";
    } else {

        user = openidm.read("managed/user/" + response.result[0]._id);
        user.securityAnswerAttempts=0;
        openidm.update("managed/user/" + response.result[0]._id, user._rev, user);
        response.result = user['_id'];
        
    }
    
} 
catch (err) {
    
    user = openidm.read("managed/user/" + response.result[0]._id);
    user.lastSecurityAnswerAttempt = (new Date()).toString();
    openidm.update("managed/user/" + response.result[0]._id, user._rev, user);
    
    response.detail = err;
    delete response.result;
    
}
