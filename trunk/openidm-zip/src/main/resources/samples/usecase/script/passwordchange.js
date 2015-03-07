/*global  objectID*/

(function () {
    var params = {
 "userId" : objectID,
 "emailEnabled" : "false",
 "_key": "passwordChangeReminder"
};
 
openidm.create('workflow/processinstance', null, params);
    
    return true; // return true to indicate successful completion
}());
