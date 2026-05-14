/*global objectID */

(function () {
    var params = {
 "userId" : objectID,
 "_key": "sunrise"
};
 
openidm.create('workflow/processinstance', null, params);
    
    return true; // return true to indicate successful completion
}());