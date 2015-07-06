/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All rights reserved.
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
 * This script sets default fields.
 * It forces that user role is openidm-authorized and account status
 * is active.
 *
 * It is run every time new user is created.
 */

/*global object */

var uiConfig =  openidm.read("config/ui/configuration");

if (!object.accountStatus) {
    object.accountStatus = 'active';
}

if(!object.authzRoles) {
    object.authzRoles = [
        {
            "_ref" : "repo/internal/role/openidm-authorized"
        }
    ];
}

if(!object.roles) {
    object.roles = [ ];
}

if (!object.lastPasswordSet) {
    object.lastPasswordSet = "";
}

if (!object.postalCode) {
    object.postalCode = "";
}

if (!object.stateProvince) {
    object.stateProvince = "";
}

if (!object.passwordAttempts) {
    object.passwordAttempts = "0";
}

if (!object.lastPasswordAttempt) {
    object.lastPasswordAttempt = (new Date()).toString();
}

if (!object.postalAddress) {
    object.postalAddress = "";
}

if (!object.address2) {
    object.address2 = "";
}

if (!object.country) {
    object.country = "";
}

if (!object.city) {
    object.city = "";
}
if (!object.givenName) {
    object.givenName = "";
}

if (!object.sn) {
    object.sn = "";
}

if (!object.telephoneNumber) {
    object.telephoneNumber = "";
}

if (!object.mail) {
    object.mail = "";
}

if (uiConfig.configuration.siteIdentification) {

    if (!object.siteImage) {
        object.siteImage = "user.png";
    }

    if (!object.passPhrase) {
        object.passPhrase = "Welcome new user";
    }

}

if (uiConfig.configuration.securityQuestions) {
    if (!object.securityAnswer) {
        object.securityAnswer = java.util.UUID.randomUUID().toString();
    }

    if (!object.securityQuestion) {
        object.securityQuestion = "1";
    }

    if (!object.securityAnswerAttempts) {
        object.securityAnswerAttempts = "0";
    }

    if (!object.lastSecurityAnswerAttempt) {
        object.lastSecurityAnswerAttempt = (new Date()).toString();
    }
}
