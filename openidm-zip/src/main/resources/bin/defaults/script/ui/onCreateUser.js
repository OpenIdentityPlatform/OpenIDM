/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

/*global require, openidm, exports */

(function () {
    exports.setDefaultFields = function (object) {

        if (!object.accountStatus) {
            object.accountStatus = 'active';
        }

        if (!object.authzRoles) {
            object.authzRoles = [
                {
                    "_ref": "repo/internal/role/openidm-authorized"
                }
            ];
        }

        /* uncomment to randomly generate passwords for new users
         if (!object.password) {

         // generate random password that aligns with policy requirements
         object.password = require("crypto").generateRandomString([
         { "rule": "UPPERCASE", "minimum": 1 },
         { "rule": "LOWERCASE", "minimum": 1 },
         { "rule": "INTEGERS", "minimum": 1 },
         { "rule": "SPECIAL", "minimum": 1 }
         ], 16);

         }
         */
    };

    /**
     * Creates a relationship between an managed/user/{id} and managed/idpData.
     *
     * @param object managed user
     */
    exports.createIdpRelationships = function (object) {
        // if managed object contains identity provider data
        if (object.idpData) {
            object.idps = Object.keys(object.idpData).filter(function (provider) {
                // filter on which identity providers have been enabled
                return object.idpData[provider].enabled !== false;
            }).map(function (provider) {
                // creates an entry in managed/idpData, returns entry ref to be associated with a managed user
                // so that the relationship can be created
                openidm.create("managed/" + provider, object.idpData[provider].subject, object.idpData[provider].rawProfile);
                return {
                    _ref: "managed/" + provider + "/" + object.idpData[provider].subject
                };
            });
        }
    };
    
    /**
     * Sends an email to the passed object's provided `mail` address via idm system email (if configured). 
     *  
     * @param object -- managed user
     * @param string subject -- email subject
     * @param Handlebars template string -- email body
     */ 
    exports.emailUser = function (object, subject, message) {
    	// if there is a configuration found, assume that it has been properly configured
    	var emailConfig = openidm.read("config/external.email"), Handlebars = require('lib/handlebars');

    	if (emailConfig) {
    	    var email,
    	        template;

    	    email =  {
    	        "from": emailConfig.from,
    	        "to": object.mail,
    	        "subject": subject,
    	        "type": "text/html"
    	    };

    	    template = Handlebars.compile(message);

    	    email.body = template({
    	        "object": object
    	    });

    	    try {
    	        openidm.action("external/email", "sendEmail", email);
    	    } catch (e) {
    	        logger.info("There was an error with the outbound email service configuration. The user was created but hasn't been notified.");
    	        throw {"code": 400}
    	    }
    	} else {
    	    logger.info("Email service not configured; user notification  not sent. ");
    	}	
    };

}());
