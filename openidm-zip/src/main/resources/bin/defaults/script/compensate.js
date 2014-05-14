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
