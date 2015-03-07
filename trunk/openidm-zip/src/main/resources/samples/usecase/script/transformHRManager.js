/*global source */

(function (){
    var managerId, managerMap = {};
    if (source !== null) {
        managerId = source.substring(4, source.indexOf(','));

        managerMap = {
            "managerId" : managerId,
            "$ref" : "/managed/user/"+managerId,
            "displayName" : managerId
        };
    }
    return managerMap;
}());
