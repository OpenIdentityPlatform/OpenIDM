/*
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
 * Copyright 2015 ForgeRock AS.
 */

/**
 * When a target is unlinked from a managed/user, this updates the historicalAccounts relationship on the managed/user 
 * with the date of the unlink event and sets the "active" flag to false.
 * 
 * The updated relationship is of the form:
 * 
 * {
 *     "_ref" : "system/ldap/accounts/uid=jdoe,ou=People,dc=example,dc=com"
 *     "_refProperties" : {
 *         "_id": "b6580bf0-7ece-4856-b716-64f16f8cb6a7",
 *         "_rev": "2",
 *         "linkedDate" : "Sun Oct 04 2015 19:18:31 GMT-0700 (PDT)",
 *         "unlinkedDate" : "Sun Oct 04 2015 19:28:56 GMT-0700 (PDT)",
 *         "active" : false 
 *     }
 * }
 */

var ref = mappingConfig.target + "/" + targetId,
    historicalAccountsCollection = mappingConfig.source + "/" + sourceId + "/historicalAccounts",
    query = {
        "_queryFilter": '_ref eq "' + ref +'"'
    },
    id, rev, account, result, queryResult;
    
// Query the historical account collection on this user to find the relationship specific to this account
queryResult = openidm.query(historicalAccountsCollection, query).result;

// Check if a result was found
if (typeof queryResult !== 'undefined' && queryResult !== null) {
    account = queryResult[0];
    if (typeof account !== 'undefined' && account !== null) {
        // A result was found
        // Set "active" to false and add an "unlinkDate"
        account._refProperties.active = false;
        account._refProperties.unlinkDate = (new Date()).toString();

        id = account._refProperties._id;
        rev = account._refProperties._rev;
        logger.debug("Updating historical account relationship for " + ref + " on managed user " + sourceId);
        // Update the relationship object
        result = openidm.update(historicalAccountsCollection + "/" + id, rev, account);
        logger.debug("Deactivated historical account: " + result);
    } else {
        logger.debug("account is undefined");
    }
} else {
    logger.debug("queryResult is undefined");
}