/*global object */
(function () {
    return  object.hasOwnProperty('password') && object.password !== null &&
            object.adPassword !== object.password;
}());