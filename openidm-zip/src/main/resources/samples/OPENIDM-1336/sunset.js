
(function () {
    var params = {
 "userId" : objectID,
 "_key": "sunset"
};
 
openidm.action('workflow/processinstance', {"_action" : "createProcessInstance"}, params);
    
    return true; // return true to indicate successful completion
}());
