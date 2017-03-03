/*global object */

function pad(number) {
    var r = String(number);
    if ( r.length === 1 ) {
        r = '0' + r;
    }
    return r;
}
 
function toISOString(date) {
    return date.getUTCFullYear()
    + '-' + pad( date.getUTCMonth() + 1 )
    + '-' + pad( date.getUTCDate() )
    + 'T' + pad( date.getUTCHours() )
    + ':' + pad( date.getUTCMinutes() )
    + ':' + pad( date.getUTCSeconds() )
    + '.' + String( (date.getUTCMilliseconds()/1000).toFixed(3) ).slice( 2, 5 )
    + 'Z';
}

function maintainLastPasswordSet(userObject) {
    userObject.lastPasswordSet = toISOString(new Date());
    if (userObject.passwordChange !== undefined) {
        delete userObject.passwordChange;
    }
}

if (object.password !== undefined){
    var params = {
        "_queryId": "for-userName",
        "uid": object.userName
    },
    result = openidm.query("managed/user", params), user, oldPassword, newPassword;
    
    if (result.result && result.result.length === 1) {
        user = result.result[0];
    }
    
    if (user === undefined || (user !== undefined && user.password === undefined)) {
        maintainLastPasswordSet(object);
    } else {
        oldPassword = openidm.decrypt(user.password);
        newPassword = openidm.decrypt(object.password);
        if (oldPassword !== newPassword) {
            maintainLastPasswordSet(object);
        }
    }
}