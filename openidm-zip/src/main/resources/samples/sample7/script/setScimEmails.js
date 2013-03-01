
/*global source */

(function () {
    var mails = source.emails,
        emails = [],
        i = 0;
    
    for (i = 0; i < mails.length; i += 1) {
        if (i === 0) {
            emails.push({value: mails[i], type : "work", primary : true }); 
        } else if (i === 1) {
            emails.push({value: mails[i], type : "home"}); 
        } else {
            emails.push({value: mails[i], type : "no type"}); 
        }
    }
    return emails;
}());
