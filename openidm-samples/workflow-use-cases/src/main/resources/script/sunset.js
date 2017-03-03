/*global objectID */

(function () {
    var params = {
 "userId" : objectID,
 "_key": "sunset"
};
 
openidm.create('workflow/processinstance', null, params);
    
    return true; // return true to indicate successful completion
}());
