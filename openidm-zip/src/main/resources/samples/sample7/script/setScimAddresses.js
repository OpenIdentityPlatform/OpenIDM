
/*global source */

(function () {
    var addressList = source.addresses,
        addresses = [],
        i = 0,
        array,
        thisFormatted;
    
    for (i = 0; i < addressList.length; i += 1) {
        array = addressList[i].split("|");
        thisFormatted = array[1] + "\n" + array[2] + ", " + array[3] + " "  + array[4] + " " + array[5];
        addresses.push({type: array[0],
          streetAddress : array[1],
          locality :array[2],
          region : array[3],
          postalCode : array[4],
          country : array[5],
          formatted : thisFormatted,
          primary : array[6]});
    }
    
    return addresses;
}());