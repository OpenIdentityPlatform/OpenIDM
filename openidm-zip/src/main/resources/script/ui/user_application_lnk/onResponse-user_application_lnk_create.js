var workflow = {   
    "_action" : "createProcessInstance"
};
  
var params = {
    "key" : "UserApplicationAcceptance",
    "lnkId" : response._id
};
 
openidm.action("workflow/processinstance", workflow, params);

