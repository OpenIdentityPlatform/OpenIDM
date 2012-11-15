/** 
 * This script sets default fields. 
 * It forces that user role is openidm-authorized and account status
 * is active.
 * 
 * It is run every time new user is created.
 */  

uiConfig =  openidm.read("config/ui/configuration");

object.accountStatus = 'active';

if(!object.roles) {
    object.roles = 'openidm-authorized';    
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

if (!object.address1) {
    object.address1 = "";
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

if (!object.familyName) {
    object.familyName = "";
}

if (!object.phoneNumber) {
    object.phoneNumber = "";
}

//password and security answer are generated if missing just to keep those attributes filled
if (!object.password) {
    object.password = java.util.UUID.randomUUID().toString();
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
