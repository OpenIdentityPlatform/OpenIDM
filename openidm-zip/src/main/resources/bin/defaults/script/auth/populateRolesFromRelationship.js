/*global security, properties, openidm */


/**
 * This context population script is called when the managed user auth module was used
 * to successfully authenticate a user
 *
 * global properties - auth module-specific properties from authentication.json for the
 *                     managed user auth module
 *
 *      {
 *          "propertyMapping": {
 *              "userRoles": "roles",
 *              "userCredential": "password",
 *              "userId": "_id"
 *          },
 *          "authnPopulateContextScript": "auth/managedPopulateContext.js",
 *          "defaultUserRoles": [  ]
 *      }
 *
 * global security - map of security context details as have been determined thus far
 *
 *      {
 *          "authorizationId": {
 *              "id": "jsmith",
 *              "component": "managed/user",
 *              "roles": [ "openidm-authorized" ]
 *          },
 *          "authenticationId": "jsmith",
 *      }
 */

(function () {

    var _ = require("lib/lodash"),
        user = openidm.read(security.authorizationId.component + "/" + security.authorizationId.id);

    if (!_.has(properties.propertyMapping, 'internalRolesRelationship')) {
        throw {
            "code" : 500,
            "message" : "Authentication not properly configured; missing internalRolesRelationship propertyMapping entry"
        };
    }

    if (!user || !_.has(user, properties.propertyMapping.internalRolesRelationship)) {
        throw {
            "code" : 401,
            "message" : "Unable to find property " + properties.propertyMapping.internalRolesRelationship + " for user"
        };
    }

    security.authorizationId = {
        'id': security.authorizationId.id,
        'component': security.authorizationId.component,
        'roles': _.chain(user[properties.propertyMapping.internalRolesRelationship])
                    .filter(function (r) {
                        return org.forgerock.json.resource.ResourceName.valueOf(r._ref).startsWith("repo/internal/role");
                    })
                    .map(function (r) {
                        // appending empty string gets the value from java into a format more familiar to JS
                        return org.forgerock.json.resource.ResourceName.valueOf(r._ref).leaf() + "";
                    })
                    .value()
    };

    return security;
}());
