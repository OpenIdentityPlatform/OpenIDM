/**
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

/**
 * Processes post create/updated/delete logic for roles.
 * 
 * Globals: request, context, oldObject, newObject, resourceName
 */

/**
 * Manages schedules associated with temporalConstraints on roles that have been created, updated, or deleted.
 */
function  manageTemporalConstraintJobsForRoles() {
    switch (request.method) {
    case "create":
        if (hasConstraints(newObject)) {
            createJobsForRoleConstraints(newObject.temporalConstraints);
        }
        break;
    case "patch":
    case "update":
        if (hasConstraints(newObject) && !hasConstraints(oldObject)) {
            createJobsForRoleConstraints(newObject.temporalConstraints);
        } else if (!hasConstraints(newObject) && hasConstraints(oldObject)) {
            deleteJobsForRoleConstraints(oldObject.temporalConstraints);
        } else if (hasConstraints(newObject) && hasConstraints(oldObject)) {
            var index = 0,
            oldConstraints = oldObject.temporalConstraints,
            newConstraints = newObject.temporalConstraints;
            while (index < oldConstraints.length || index < newConstraints.length) {
                var oldConstraint = index < oldConstraints.length 
                ? oldConstraints[index]
                : null,
                newConstraint = index < newConstraints.length 
                ? newConstraints[index]
                : null;
                if (JSON.stringify(oldConstraint) !== JSON.stringify(newConstraint)) {
                    if (oldConstraint !== null) {
                        deleteJobsForRoleConstraint(index);
                    }
                    if (newConstraint !== null) {
                        createJobForRoleConstraint(newConstraint, index);
                    }
                }
                index++;
            }
        }
        break;
    case "delete":
        if (hasConstraints(oldObject)) {
            deleteJobsForRoleConstraints(oldObject.temporalConstraints);
        }
        break;
    }
};

/**
 * Manages schedules associated with temporalConstraints on grants for users or roles that have been created, updated, 
 * or deleted.
 */
function  manageTemporalConstraintJobsForGrants(field) {
    switch (request.method) {
    case "create":
        if (isNonEmptyList(newObject[field])) {
            newObject[field].forEach(createJobsForGrantConstraints);
        }
        break;
    case "patch":
    case "update":
        if (isNonEmptyList(newObject[field]) && !isNonEmptyList(oldObject[field])) {
            newObject[field].forEach(createJobsForGrantConstraints);
        } else if (!isNonEmptyList(newObject[field]) && isNonEmptyList(oldObject[field])) {
            oldObject[field].forEach(deleteJobsForGrantConstraints);
        } else if (isNonEmptyList(newObject[field]) && isNonEmptyList(oldObject[field])) {
            var newGrants = newObject[field].reduce(reduceGrants, {}),
                oldGrants = oldObject[field].reduce(reduceGrants, {}),
                addedGrants = [];
                // Schedule jobs for removed and changed grants
                for (var grantId in oldGrants) {
                    var newGrant = newGrants[grantId],
                        oldGrant = oldGrants[grantId];
                    if (newGrant !== undefined) { // grant exists in newGrants map
                        // Check if grant has changed
                        if (JSON.stringify(oldGrant) !== JSON.stringify(newGrant)) {
                            // Delete old jobs and create new ones
                            deleteJobsForGrantConstraints(oldGrant);
                            createJobsForGrantConstraints(newGrant);
                        }
                        // Remove the grant from the newGrants map so that we are left with only new grants
                        delete newGrants[grantId];
                    } else {  // The grant does not exist in newGrants map, meaning it was removed
                        // Delete old jobs
                        deleteJobsForGrantConstraints(oldGrant);
                    }
                }
                // Schedule jobs for newly added grants
                for (var grantId in newGrants) {
                    // Create new jobs
                    createJobsForGrantConstraints(newGrants[grantId]);
                }
        }
        break;
    case "delete":
        if (isNonEmptyList(oldObject[field])) {
            oldObject[field].forEach(deleteJobsForGrantConstraints);
        }
        break;
    }
};

/**
 * A reduce function which adds a grant to a map using the relationship _id as the key.
 *
 * @param map the map to add the grant to.
 * @param grant the grant to add to the map
 */
function reduceGrants(map, grant) {
    var refProperties = grant._refProperties;
    if (refProperties !== undefined && refProperties !== null) {
        map[refProperties._id] = grant;
    }
    return map;
}

/**
 * Creates new scheduled jobs that will fire when the time window of temporal constraints on a grant start and end.
 * 
 * @param grant a grant object.
 */
function createJobsForGrantConstraints(grant) {
    if (hasConstraints(grant._refProperties)) {
        var userId = resourceName.startsWith('managed/user/') 
                ? resourceName.toString() 
                : grant._ref,
            // The script to enforce grant temporal constraints will issue triggerSyncCheck action and set fields to "*"
            // to indicate all default fields plus any virtual fields on the managed user, which will pick up changes
            // to "effectiveAssignments" and "effectiveRoles". Also add the roles field, so that user roles are
            // also populated, so that sync checks take user roles into consideration.
            scriptConfig = {
                "type" : "text/javascript", 
                "source" : "openidm.action(userId, 'triggerSyncCheck', {}, {}, ['*', 'roles']);",
                "globals" : { 
                    "userId" : userId
                }
            };
                
        for (index in grant._refProperties.temporalConstraints) {
            createJobsForConstraint(
                    grant._refProperties.temporalConstraints[index], 
                    grant._refProperties._id + "-temporalConstraint-" + index + "-start",
                    grant._refProperties._id + "-temporalConstraint-" + index + "-end",
                    scriptConfig);
        }
    }
}

/**
 * Deletes all scheduled jobs for temporal constraints on a grant.
 * 
 * @param grant a grant object.
 */
function deleteJobsForGrantConstraints(grant) {
    if (hasConstraints(grant._refProperties)) {
        for (index in grant._refProperties.temporalConstraints) {
            deleteJobsForConstraint(
                    grant._refProperties._id + "-temporalConstraint-" + index + "-start",
                    grant._refProperties._id + "-temporalConstraint-" + index + "-end",
                    index);
        }
    }
}

/**
 * Creates new scheduled jobs that will fire when the time window of temporal constraints on a role start and end.
 * 
 * @param constraint an array of objects representing temporal constraints.
 */
function createJobsForRoleConstraints(constraints) {
    for (index in constraints) {
        createJobForRoleConstraint(constraints[index], index);
    }
};

/**
 * Creates new scheduled jobs that will fire when the time window of the temporal constraint on a role starts and ends.
 * 
 * @param constraint an array of objects representing temporal constraints.
 */
function createJobForRoleConstraint(constraint, index) {
    createJobsForConstraint(
            constraint, 
            resourceName.toString().replace('/', '-') + "-temporalConstraint-" + index + "-start",
            resourceName.toString().replace('/', '-') + "-temporalConstraint-" + index + "-end",
            { 
                "type" : "text/javascript", 
                "source" : "require('roles/onSync-roles').syncUsersOfRoles(resourceName, object, object, null);",
                "globals" : { 
                    "object" : newObject,
                    "resourceName" : resourceName.toString()
                }
            });
} 

/**
 * Deletes all scheduled jobs for all temporal constraints on a role.
 * 
 * @param constraint an array of objects representing temporal constraints.
 */
function deleteJobsForRoleConstraints(constraints) {
    for (index in constraints) {
        deleteJobsForRoleConstraint(index);
    }
};

/**
 * Deletes all scheduled jobs for a temporal constraints on a role.
 * 
 * @param constraint an array of objects representing temporal constraints.
 */
function deleteJobsForRoleConstraint(index) {
    deleteJobsForConstraint(
            resourceName.toString().replace('/', '-') + "-temporalConstraint-" + index + "-start",
            resourceName.toString().replace('/', '-') + "-temporalConstraint-" + index + "-end",
            index);
};

/**
 * Creates new scheduled jobs that will fire when the time window of the temporal constraint starts and ends.
 * 
 * @param constraint an object representing a temporal constraint.
 * @param index the index of the constraint in the array of constraints defined in the role.
 */
function createJobsForConstraint(constraint, startJobId, endJobId, script) {
    logger.debug("creating new jobs for: " + constraint);
    var dateUtil = org.forgerock.openidm.util.DateUtil.getDateUtil(),
        startDate = dateUtil.getStartOfInterval(constraint.duration),
        endDate = dateUtil.getEndOfInterval(constraint.duration),
        startExpression = dateUtil.getSchedulerExpression(startDate.plusSeconds(1)),
        endExpression = dateUtil.getSchedulerExpression(endDate.plusSeconds(1));
    
    var startJob = {
            "type" : "cron",
            "schedule" : startExpression, 
            "misfirePolicy" : "doNothing",
            "persisted" : true,
            "invokeService" : "script", 
            "invokeContext" : {
                "script" : script
            }
        },
        endJob = {
            "type" : "cron",
            "schedule" : endExpression, 
            "misfirePolicy" : "doNothing",
            "persisted" : true,
            "invokeService" : "script", 
            "invokeContext" : {
                "script" : script
            }
        };

    if (startDate.isAfterNow()) {
        logger.debug("create startJob: " + startJobId);
        try {
            openidm.create("scheduler", startJobId, startJob);
        } catch (e) {
            logger.error("Error while attempting to create start schedule for temporal constraint on resource "
                + resourceName + " with id of: " + startJobId);
        }
    } else {
        logger.debug("Not creating start job, is in the past");
    }
    if (endDate.isAfterNow()) {
        logger.debug("create endJob: " + endJobId);
        try {
            openidm.create("scheduler", endJobId, endJob);
        } catch (e) {
            logger.error("Error while attempting to create end schedule for temporal constraint on resource "
                + resourceName + " with id of: " + endJobId);
        }
    } else {
        logger.debug("Not creating end job, is in the past");
    }
    return true;
};

/**
 * Deletes all scheduled jobs for this temporal constraint.
 * 
 * @param constraint an object representing a temporal constraint
 */
function deleteJobsForConstraint(startJobId, endJobId, index) {
    try {
        logger.debug("delete startJob: " + resourceName);
        openidm.delete("scheduler/" + startJobId, null);
    } catch (e) {
        if (e.javaException.getCode() !== 404) {
            logger.error("Error while attempting to delete start schedule for temporal constraint on resource "
                + resourceName + " with id of: " + startJobId);
        } else {
            //only at debug as removing a temporal constraint which has passed should fail, as trigger has already fired.
            logger.debug("Error while attempting to delete start schedule for temporal constraint on resource "
                + resourceName + " with id of: " + startJobId);
        }
    }
    try {
        logger.debug("delete endJob:   " + resourceName);
        openidm.delete("scheduler/" + endJobId, null);
    } catch (e) {
        if (e.javaException.getCode() !== 404) {
            logger.error("Error while attempting to delete end schedule for temporal constraint on resource "
                + resourceName + " with id of: " + endJobId);
        } else {
            //only at debug as removing a temporal constraint which has passed should fail, as trigger has already fired.
            logger.debug("Error while attempting to delete end schedule for temporal constraint on resource "
                + resourceName + " with id of: " + endJobId);
        }
    }
};

/**
 * Returns true if the object's temporalConstraints is defined and not null, false otherwise.
 * 
 * @param object a object.
 * @returns true if the object's temporalConstraints is defined and not null, false otherwise.
 */
function hasConstraints(object) {
    return (object.temporalConstraints !== undefined && object.temporalConstraints !== null);
};

/**
 * Returns true if the supplied list is defined, not null, and not empty.
 * 
 * @param list a list.
 * @returns true if the supplied list is defined, not null, and not empty.
 */
function isNonEmptyList(list) {
    if (list !== undefined && list !== null && list.length > 0) {
        return true;
    }
    return false;
}

// Check if the object is a managed role or managed user
if (resourceName.startsWith('managed/role/')) {
    // manage the temporal constraints defined on the role
    manageTemporalConstraintJobsForRoles();
    // manage the temporal constraints defined in the grants
    manageTemporalConstraintJobsForGrants("members");
} else if (resourceName.startsWith('managed/user/')) {
    // manage the temporal constraints defined in the grants
    manageTemporalConstraintJobsForGrants("roles");
}