
/*global source */

(function () {
        
    var phones = source.phoneNumbers,
        phoneNumbers = [],
        i = 0;
    
    for (i = 0; i < phones.length; i += 1) {
        if (i === 0) {
            phoneNumbers.push({value: phones[i], type : "work" }); 
        } else if (i === 1) {
            phoneNumbers.push({value: phones[i], type : "home"}); 
        } else {
            phoneNumbers.push({value: phones[i], type : "no type"}); 
        }
    }
    
    return phoneNumbers;

}());