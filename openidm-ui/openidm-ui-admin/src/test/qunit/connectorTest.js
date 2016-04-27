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

define([
        "org/forgerock/openidm/ui/admin/connector/EditConnectorView",
        "lodash"
    ],
    function (EditConnectorView, _) {
        QUnit.module('Connectors');

        QUnit.test("Advanced Connector Save", function () {
            var oldConnector = {
                    "resultsHandlerConfig" : {
                        "enableNormalizingResultsHandler" : true,
                        "enableFilteredResultsHandler" : true,
                        "enableCaseInsensitiveFilter" : true,
                        "enableAttributesToGetSearchResultsHandler" : true
                    },
                    "poolConfigOption" : {
                        "maxObjects" : 10,
                        "maxIdle" : 10,
                        "maxWait" : 150000,
                        "minEvictableIdleTimeMillis" : 120000,
                        "minIdle" : 1
                    },
                    "operationTimeout" : {
                        "CREATE" : -1,
                        "VALIDATE" : -1,
                        "TEST" : -1,
                        "SCRIPT_ON_CONNECTOR" : -1,
                        "SCHEMA" : -1,
                        "DELETE" : -1,
                        "UPDATE" : -1,
                        "SYNC" : -1,
                        "AUTHENTICATE" : -1,
                        "GET" : -1,
                        "SCRIPT_ON_RESOURCE" : -1,
                        "SEARCH" : -1
                    }
                },
                newConnector = {
                    "resultsHandlerConfig" : {
                        "enableNormalizingResultsHandler" : "false",
                        "enableFilteredResultsHandler" : "false"
                    },
                    "poolConfigOption" : {
                        "maxObjects" : 25
                    },
                    "operationTimeout" : {
                        "CREATE" : 5,
                        "AUTHENTICATE" : "5"
                    }
                },
                results = EditConnectorView.advancedDetailsGenerate(oldConnector, newConnector);

            QUnit.equal(results.resultsHandlerConfig.enableNormalizingResultsHandler, false, "Boolean value successfully converted and saved");
            QUnit.equal(results.poolConfigOption.maxObjects, 25, "Pool Config successfully saved");
            QUnit.equal(results.operationTimeout.CREATE, 5, "Operation Timeout successfully saved");
            QUnit.equal(results.poolConfigOption.maxIdle, 10, "Original maxIdle exists");
        });
    });
