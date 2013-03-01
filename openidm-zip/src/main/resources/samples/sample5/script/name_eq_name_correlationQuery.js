
/*global source */

(function () {
    var myArray = [ source.userName ],
        map = {"query": { "Equals": {"field" : "name","values" : myArray}}};
    
    return map;
}());