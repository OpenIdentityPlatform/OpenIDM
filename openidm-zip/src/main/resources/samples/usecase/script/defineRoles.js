/*global source */

(function () {
    var roles = [ {
        "_ref" : "repo/internal/roles/openidm-authorized"
    } ];

    if (source !== undefined && 'manager' === source) {
        roles.push({
            "_ref" : "repo/internal/roles/openidm-admin"
        });
    }

    return roles;
}());