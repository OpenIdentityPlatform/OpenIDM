/*global object */
(function () {
    return  typeof object.password !== "undefined" && object.password !== null &&
            object.ldapPassword !== object.password;
}());