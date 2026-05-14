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

var ref = mappingConfig.source + "/" + sourceId,
    historicalAccountsCollection = mappingConfig.target + "/" + targetId + "/historicalAccounts",
    query = {
            "_queryFilter": '_ref eq "' + ref +'"'
    },
    disabled = source.disabled,
    newState, id, rev, account, result, queryResult;

//Query the historical account collection on this user to find the relationship specific to this account
queryResult = openidm.query(historicalAccountsCollection, query).result;

//Check if a result was found
if (typeof queryResult !== 'undefined' && queryResult !== null) {
    account = queryResult[0];
    if (typeof account !== 'undefined' && account !== null) {
        // A result was found, check if the state has changed;
        newState = disabled ? "disabled" : "enabled";
        if (newState !== account._refProperties.state) {
            // State has changed
            logger.debug("Setting historical account state to " + newState);
            account._refProperties.state = newState;
            account._refProperties.stateLastChanged = (new Date()).toString();

            logger.debug("Updating historical account relationship for " + ref + " on managed user " + sourceId);
            // Update the relationship object
            result = openidm.update(historicalAccountsCollection + "/" + account._refProperties._id, 
                    account._refProperties._rev, account);
        }
    } else {
        logger.debug("account is undefined");
    }
} else {
    logger.debug("queryResult is undefined");
}