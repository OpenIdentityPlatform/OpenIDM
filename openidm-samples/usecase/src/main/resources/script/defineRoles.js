/*global source */

(function () {
    var roles = [ {
        "_ref" : "repo/internal/role/openidm-authorized"
    } ];

    if (source !== undefined && 'manager' === source) {
        roles.push({
            "_ref" : "repo/internal/role/openidm-admin"
        });
    }

    return roles;
}());
