
/*global source */

(function () {
    var imAddresses = source.ims,
        ims = [],
        imArray,
        i = 0;
    
    for (i = 0; i < imAddresses.length; i += 1) {
        imArray = imAddresses[i].split(":");
        ims.push({value: imArray[1], type : imArray[0]});
    }
    
    return ims;
}());