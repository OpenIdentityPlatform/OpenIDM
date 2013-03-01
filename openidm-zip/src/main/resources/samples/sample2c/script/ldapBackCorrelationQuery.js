
/*global source */

(function () {
    var myArray = [ source.userName ],
        map = {"query": { "Equals": {"field" : "uid", "values" : myArray}}};
    
    return map;
}());