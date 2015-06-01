/*global source */

(function (){
    var managerId, manager = "";
    if (source !== null) {
        managerId = source.substring(4, source.indexOf(','));
        
        manager = "managed/user/"+managerId;
    }
    return manager;
}());
