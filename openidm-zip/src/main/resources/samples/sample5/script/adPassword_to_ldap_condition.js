/*global object */
(function () {
    return  object.hasOwnProperty('password') && object.password !== null &&
            object.ldapPassword !== object.password;
}());