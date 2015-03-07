/**
 * This context population script is called when the internal user auth module was used
 * to successfully authenticate a user
 *
 * global properties - auth module-specific properties from authentication.json for the
 *                     internal user auth module
 *
 *      {
 *          "propertyMapping": {
 *              "userRoles": "roles",
 *              "userCredential": "password",
 *              "userId": "_id"
 *          },
 *          "authnPopulateContextScript": "auth/internalPopulateContext.js",
 *          "defaultUserRoles": [  ]
 *      }
 *
 * global security - map of security context details as have been determined thus far
 *
 *      {
 *          "authorizationId": {
 *              "id": "jsmith",
 *              "component": "repo/internal/user",
 *              "roles": [ "openidm-authorized" ]
 *          },
 *          "authenticationId": "jsmith",
 *      }
 */
