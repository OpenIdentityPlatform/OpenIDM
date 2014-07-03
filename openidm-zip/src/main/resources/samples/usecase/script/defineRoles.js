/*global source */

(function () {
    var roles = ['openidm-authorized'];
    if (source !== undefined && 'manager' === source) {
        roles.push('openidm-admin');
    }
    return roles;
}());