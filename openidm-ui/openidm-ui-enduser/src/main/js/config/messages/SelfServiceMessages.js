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

/*global define*/

/**
 * @author jdabrowski
 */
define("config/messages/SelfServiceMessages", [
], function() {
    
    var obj = {
            //admin
            "cannotDeleteYourself": {
                msg: "config.messages.AdminMessages.cannotDeleteYourself",
                type: "error"
            },
            "userDeleted": {
                msg: "config.messages.AdminMessages.userDeleted",
                type: "info"
            },
            "userDeleteError": {
                msg: "config.messages.AdminMessages.userDeleteError",
                type: "error"
            },
            "userValidationError": {
                msg: "config.messages.AdminMessages.userValidationError",
                type: "error"
            },

            //applications
            "userApplicationsUpdate": {
                msg: "config.messages.AdminMessages.userApplicationsUpdate",
                type: "info"
            },

            //tasks
            "completedTask": {
                msg: "config.messages.AdminMessages.completedTask",
                type: "info"
            },
            "claimedTask": {
                msg: "config.messages.AdminMessages.claimedTask",
                type: "info"
            },
            "unclaimedTask": {
                msg: "config.messages.AdminMessages.unclaimedTask",
                type: "info"
            },
            "startedProcess": {
                msg: "config.messages.AdminMessages.startedProcess",
                type: "info"
            },
            "authenticationUnavailable" : {
                msg: "config.messages.AuthenticationMessages.authenticationUnavailable",
                type: "error"
            }
    };
    
    return obj;
});