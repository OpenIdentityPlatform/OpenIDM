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
 * @author jdabrowski
 * 
 * This script changes user password. It is run as a response to
 * set-newPassword-for-userName query.
 */

user = openidm.read("managed/user/" + response.result[0]._id);
securityAnswer = user.securityAnswer;
requestedUserNameMatchesReturnedUserName = (response.result[0].userName == request.params['username']);

if (securityAnswer && requestedUserNameMatchesReturnedUserName) {
    isRequestedSecurityEqualToReturned = (openidm.decrypt(user.securityAnswer) === request.params['securityAnswer']);
    if (isRequestedSecurityEqualToReturned) {
        logger.info("Setting new password for {}", request.params['username']);
        user.password = request.params['newpassword'];
        user.securityAnswer = request.params['securityAnswer'];
        openidm.update("managed/user/" + response.result[0]._id, user._rev, user);
        response.result = "correct";
    }
}