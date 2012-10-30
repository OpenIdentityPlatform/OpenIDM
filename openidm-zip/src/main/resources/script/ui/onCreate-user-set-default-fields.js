/** 
 * This script sets default fields. 
 * It forces that user role is openidm-authorized and account status
 * is active.
 * 
 * It is run every time new user is created.
 */  
  
function requiredUniqeUserName(userName) {
    var params = {
            "_query-id": "check-userName-availability",
            "uid": userName
    };
    result = openidm.query("managed/user", params);
    if ((result.result && result.result.length!=0) || (result.results && result.results.length!=0)) {
        throw "Failed to create user. User with userName " + userName + " exists";
    }
}

requiredUniqeUserName(object.userName);

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

if (!object.siteImage) {
    object.siteImage = "1";
}

if (!object.passPhrase) {
    object.passPhrase = "";
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

if (!object.frequentlyUsedApplications) {
    object.frequentlyUsedApplications = "";
}

if (!object.userApplicationsOrder) {
    object.userApplicationsOrder = "";
}

//password and security answer are generated if missing just to keep those attributes filled
if (!object.password) {
    object.password = java.util.UUID.randomUUID().toString();
}

if (!object.securityAnswer) {
    object.securityAnswer = java.util.UUID.randomUUID().toString();
}
