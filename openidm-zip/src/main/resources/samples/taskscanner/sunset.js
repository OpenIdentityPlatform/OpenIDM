logger.debug("Performing Sunset Task on {} ({})", input.email, objectID);

// Perform sunsetting task - mark inactive, trigger workflow, etc.
input['active'] = false;
// Perform update on the object to complete the sunsetting task
openidm.update(objectID, input['_rev'], input);

true; // return true to indicate successful completion
