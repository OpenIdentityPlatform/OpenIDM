var workflow = {   
    "_action" : "createProcessInstance"
};
  
var params = {
    "_key" : "UserApplicationAcceptance",
    "lnkId" : response._id
};
 
openidm.action("workflow/processinstance", workflow, params);

