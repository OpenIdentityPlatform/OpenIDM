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
 *          "authorization": {
 *              "id": "jsmith",
 *              "component": "managed/user",
 *              "roles": [ "openidm-authorized" ]
 *          },
 *          "authenticationId": "jsmith",
 *      }
 */
