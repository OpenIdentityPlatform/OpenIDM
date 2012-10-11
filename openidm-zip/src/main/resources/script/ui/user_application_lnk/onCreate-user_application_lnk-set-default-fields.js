/** 
 * This script generates user UUID and sets default fields. 
 * It forces that user role is openidm-authorized and account status
 * is active.
 * 
 * It is run every time new user is created.
 */  
  
var userApplicationLnk = openidm.decrypt(object);
var params = {
    "_query-id": "get-user-app-link-by-user-and-app",
    "uid": userApplicationLnk.userId,
    "applicationId": userApplicationLnk.applicationId
};
      
result = openidm.query("managed/user_application_lnk", params);
      
if ((result.result && result.result.length!=0) || (result.results && result.results.length!=0)) {
    throw "Failed to create user application link. User already has this application";
}