/*global  objectID*/

(function () {
    var params = {
 "userId" : objectID,
 "_key": "passwordChangeReminder"
};
 
openidm.action('workflow/processinstance', {"_action" : "createProcessInstance"}, params);
    
    return true; // return true to indicate successful completion
}());
