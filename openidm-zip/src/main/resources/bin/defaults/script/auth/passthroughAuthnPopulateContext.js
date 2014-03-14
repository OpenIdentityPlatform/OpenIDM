/**
 * This context population script is called when the passthrough auth module was used
 * to successfully authenticate a user
 *
 * global properties - auth module-specific properties from authentication.json for the
 *                     passthrough user auth module
 *
 *      {
 *          "authnPopulateContextScript" : "auth/passthroughAuthnPopulateContext.js",
 *          "passThroughAuth" : "system/AD/account",
 *          "propertyMapping" : {
 *              "userRoles" : "roles"
 *          },
 *          "defaultUserRoles" : [ ]
 *      }
 *
 * global security - map of security context details as have been determined thus far
 *
 *      {
 *          "authorizationId": {
 *              "id": "jsmith",
 *              "component": "passthrough",
 *              "roles": [ "openidm-authorized" ]
 *          },
 *          "authenticationId": "jsmith",
 *      }
 */
