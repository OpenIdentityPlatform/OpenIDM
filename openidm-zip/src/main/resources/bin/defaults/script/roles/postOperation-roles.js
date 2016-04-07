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
 * Manages schedules associated with temporalConstraints that have been created, updated, or deleted.
 */
function  manageTemporalConstraintJobs() {
    switch (request.method) {
    case "create":
        if (hasConstraints(newObject)) {
            createJobsForConstraints(newObject.temporalConstraints);
        }
        break;
    case "patch":
    case "update":
        if (hasConstraints(newObject) && !hasConstraints(oldObject)) {
            createJobsForConstraints(newObject.temporalConstraints);
        } else if (!hasConstraints(newObject) && hasConstraints(oldObject)) {
            deleteJobsForConstraints(oldObject.temporalConstraints);
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
                        deleteJobsForConstraint(index);
                    }
                    if (newConstraint !== null) {
                        createJobsForConstraint(newConstraint, index);
                    }
                }
                index++;
            }
        }
        break;
    case "delete":
        if (hasConstraints(oldObject)) {
            deleteJobsForConstraints(oldObject.temporalConstraints);
        }
        break;
    }
};

/**
 * Creates new scheduled jobs that will fire when the time window of the temporal constraints start and end.
 * 
 * @param constraint an array of objects representing temporal constraints.
 */
function createJobsForConstraints(constraints) {
    for (index in constraints) {
        createJobsForConstraint(constraints[index], index);
    }
};

/**
 * Deletes all scheduled jobs for these temporal constraints.
 * 
 * @param constraint an array of objects representing temporal constraints.
 */
function deleteJobsForConstraints(constraints) {
    for (index in constraints) {
        deleteJobsForConstraint(index);
    }
};

/**
 * Creates new scheduled jobs that will fire when the time window of the temporal constraint starts and ends.
 * 
 * @param constraint an object representing a temporal constraint.
 * @param index the index of the constraint in the array of constraints defined in the role.
 */
function createJobsForConstraint(constraint, index) {
    logger.debug("creating new jobs for: " + constraint + ", index: " + index);
    var dateUtil = org.forgerock.openidm.util.DateUtil.getDateUtil(),
        startDate = dateUtil.getStartOfInterval(constraint.duration),
        endDate = dateUtil.getEndOfInterval(constraint.duration),
        startExpression = dateUtil.getSchedulerExpression(startDate.plusSeconds(1)),
        endExpression = dateUtil.getSchedulerExpression(endDate.plusSeconds(1));
    
    var invokeScript = { 
            "script" : { 
                "type" : "text/javascript", 
                "source" : "require('roles/onSync-roles').syncUsersOfRoles(resourceName, object, object, null);",
                "globals" : { 
                    "object" : newObject,
                    "resourceName" : resourceName.toString()
                } 
            } 
        },
        startJob = {
            "type" : "cron",
            "schedule" : startExpression, 
            "misfirePolicy" : "doNothing",
            "persisted" : true,
            "invokeService" : "script", 
            "invokeContext" : invokeScript
        },
        endJob = {
            "type" : "cron",
            "schedule" : endExpression, 
            "misfirePolicy" : "doNothing",
            "persisted" : true,
            "invokeService" : "script", 
            "invokeContext" : invokeScript
        },
        startJobId = resourceName.toString().replace('/', '-') + "-temporalConstraint-" + index + "-start",
        endJobId = resourceName.toString().replace('/', '-') + "-temporalConstraint-" + index + "-end";

    if (startDate.isAfterNow()) {
        logger.debug("create startJob: " + startJobId);
        openidm.create("scheduler", startJobId, startJob);
    } else {
        logger.debug("Not creating start job, is in the past");
    }
    if (endDate.isAfterNow()) {
        logger.debug("create endJob: " + endJobId);
        openidm.create("scheduler", endJobId, endJob);
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
function deleteJobsForConstraint(index) {
    var startJobId = resourceName.toString().replace('/', '-') + "-temporalConstraint-" + index + "-start",
        endJobId = resourceName.toString().replace('/', '-') + "-temporalConstraint-" + index + "-end";
    try {
        logger.debug("delete startJob: " + resourceName);
        openidm.delete("scheduler/" + startJobId, null);
    } catch (e) {
        // If 404 (not found) code is encountered, ignore, otherwise log error
        if (e.javaException.getCode() !== 404) {
            logger.error("Error while attempting to delete start schedule for temporal constraint of: " + resourceName);
        }
    }
    try {
        logger.debug("delete endJob:   " + resourceName);
        openidm.delete("scheduler/" + endJobId, null);
    } catch (e) {
        // If 404 (not found) code is encountered, ignore, otherwise log error
        if (e.javaException.getCode() !== 404) {
            logger.error("Error while attempting to delete end schedule for temporal constraint of: " + resourceName);
        }
    }
};

/**
 * Returns true if the role's temporalConstraints is defined and not null, false otherwise.
 * 
 * @param role a role object.
 * @returns true if the role's temporalConstraints is defined and not null, false otherwise.
 */
function hasConstraints(role) {
    return (role.temporalConstraints !== undefined && role.temporalConstraints !== null);
};

manageTemporalConstraintJobs();