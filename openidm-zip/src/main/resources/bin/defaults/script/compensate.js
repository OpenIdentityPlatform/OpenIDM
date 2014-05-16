/**
 * The following objects are available in an onSync script handler.
 *
 * syncResults contains the information about
 *  - the synchronized targets as syncDetails
 *  - the synchronization action being performed
 *  - the success of the synchronization
 *
 * syncResults: {
 *     "syncDetails": [
 *         {
 *             "result": "SUCCESSFUL",
 *             "oldTargetValue": null,
 *             "reconId": null,
 *             "action": "CREATE",
 *             "targetId": "f601fe2d-c0bc-49c3-a95c-6f64addc33d5",
 *             "mapping": "managedUser_systemAdAccounts",
 *             "targetObjectSet": "system/ad/account",
 *             "situation": "ABSENT",
 *             "sourceId": "efb53d24-599d-4d43-9e56-12f2e13dc91c"
 *         },
 *         {
 *             "result": "FAILED",
 *             "oldTargetValue": null,
 *             "cause": {
 *                 "code": 409,
 *                 "reason": "Conflict",
 *                 "message": "Missing required field: __PASSWORD__"
 *             },
 *             "reconId": null,
 *             "action": "CREATE",
 *             "mapping": "managedUser_systemLdapAccounts",
 *             "targetObjectSet": "system/ldap/account",
 *             "situation": "ABSENT",
 *             "sourceId": "efb53d24-599d-4d43-9e56-12f2e13dc91c"
 *         }
 *     ],
 *     "action": "notifyCreate",
 *     "success": false
 * }
 *
 * The request object is available.  A sample create request is shown:
 *
 * request: {
 *     "method": "create",
 *     "fields": {},
 *     "resourceName": "",
 *     "content": {
 *         "_rev": "20",
 *         "mail": "mail2@example.com",
 *         "sn": "Doe",
 *         "_id": "efb53d24-599d-4d43-9e56-12f2e13dc91c",
 *         "description": "Created By XML1",
 *         "accountStatus": "active",
 *         "roles": {},
 *         "userName": "DDOE1",
 *         "givenName": "Darth"
 *     },
 *     "newResourceId": "efb53d24-599d-4d43-9e56-12f2e13dc91c"
 * }
 *
 * Additionally, request will contain the additional parameter "compensating" when this script is invoked the
 * second time through when reverting a change as part of a failed operation:
 *
 *     "additionalParameters": {
 *         "compensating": "true"
 *     },
 *
 * The previous state of the object before the change is given as 'oldObject':
 *
 * oldObject: null
 *
 * The updated or new state of the object after the change is given as 'newObject':
 *
 * newObject: {
 *     "_rev": "21",
 *     "mail": "mail2@example.com",
 *     "sn": "Doe",
 *     "_id": "efb53d24-599d-4d43-9e56-12f2e13dc91c",
 *     "description": "Created By XML1",
 *     "accountStatus": "active",
 *     "roles": {},
 *     "effectiveAssignments": {},
 *     "userName": "DDOE1",
 *     "givenName": "Darth",
 *     "effectiveRoles": {}
 * }
 *
 * resourceName: an object representing the resource name URI full path
 * resourceName.toString():             managed/user/efb53d24-599d-4d43-9e56-12f2e13dc91c
 * resourceName.leaf().toString():      efb53d24-599d-4d43-9e56-12f2e13dc91c
 * resourceName.parent().toString():    managed/user
 */
(function() {
    var _ = require("lib/lodash.js");

    if (syncResults.success) {
        logger.debug("sync was a success; no compensation necessary");
        return;
    }

    if (request !== null
            && request.additionalParameters !== null
            && request.additionalParameters.compensating === "true") {
        logger.debug("already compensating, returning");
        return;
    }

    logger.debug("compensating for " + resourceName);

    var params = { "compensating" : true };
    switch (syncResults.action) {
        case "notifyCreate":
            try {
                openidm.delete(resourceName.toString(), newObject._rev, params);
            } catch (e) {
                logger.warn("Was not able to delete {} from compensation script", resourceName.toString(), e);
            }
            break;
        case "notifyUpdate":
            try {
                openidm.update(resourceName.toString(), newObject._rev, oldObject, params);
            } catch (e) {
                logger.warn("Was not able to update {} from compensation script", resourceName.toString(), e);
            }
            break;
        case "notifyDelete":
            try {
                openidm.create(resourceName.parent().toString(), resourceName.leaf().toString(), oldObject, params);
            } catch (e) {
                logger.warn("Was not able to create {} from compensation script", resourceName.toString(), e);
            }
            break;
    } 
    logger.debug(resourceName + " sync failure compensation complete");

    // throw the error that caused the sync failure
    var firstFailure = _.find(syncResults.syncDetails,
            function (r) { 
                return r.result === "FAILED" && r.cause !== undefined; 
            });

    if (firstFailure !== null) {
          throw firstFailure.cause;
    }

}());
