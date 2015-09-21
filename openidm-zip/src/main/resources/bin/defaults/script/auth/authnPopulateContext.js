/**
 * This context population script is called after the auth filter
 * has successfully authenticated a user.  It is a global augmentation
 * script in the sense that it is called regardless of which auth mechanism
 * was invoked.
 *
 * global context.security - map of security context details as have been determined thus far
 *
 *      {
 *          "authorization": {
 *              "id": "jsmith",
 *              "component": "passthrough",
 *              "roles": [ "openidm-authorized" ]
 *          },
 *          "authenticationId": "jsmith",
 *      }
 */

// logger.debug("Augment context for: {}", security.authenticationId);

