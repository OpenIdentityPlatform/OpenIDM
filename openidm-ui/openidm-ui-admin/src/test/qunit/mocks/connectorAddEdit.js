/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/*global require, define*/
define([
    "text!templates/admin/connector/AddEditConnectorTemplate.html",
    "text!templates/admin/util/ScriptEditor.html",
    "text!templates/admin/connector/org.forgerock.openicf.csvfile.CSVFileConnector_1.1.html",
    "text!templates/admin/connector/org.identityconnectors.databasetable.DatabaseTableConnector_1.1.html"
], function () {

    /* an unfortunate need to duplicate the file names here, but I haven't
     yet found a way to fool requirejs into doing dynamic dependencies */
    var staticFiles = [
            "templates/admin/connector/AddEditConnectorTemplate.html",
            "templates/admin/util/ScriptEditor.html",
            "templates/admin/connector/org.forgerock.openicf.csvfile.CSVFileConnector_1.1.html",
            "templates/admin/connector/org.identityconnectors.databasetable.DatabaseTableConnector_1.1.html"
        ],
        deps = arguments;

    return function (server) {

        _.each(staticFiles, function (file, i) {
            server.respondWith(
                "GET",
                new RegExp(file.replace(/([\/\.\-])/g, "\\$1") + "$"),
                [
                    200,
                    { },
                    deps[i]
                ]
            );
        });

        server.respondWith(
            "POST",
            "/openidm/system?_action=availableConnectors",
            [
                200,
                { },
                "{\"connectorRef\":[{\"connectorName\":\"org.forgerock.openicf.csvfile.CSVFileConnector\",\"displayName\":\"CSV File Connector\",\"bundleName\":\"org.forgerock.openicf.connectors.csvfile-connector\",\"systemType\":\"provisioner.openicf\",\"bundleVersion\":\"1.1.0.1\"},{\"connectorName\":\"org.identityconnectors.databasetable.DatabaseTableConnector\",\"displayName\":\"Database Table Connector\",\"bundleName\":\"org.forgerock.openicf.connectors.databasetable-connector\",\"systemType\":\"provisioner.openicf\",\"bundleVersion\":\"1.1.0.0\"},{\"connectorName\":\"org.forgerock.openicf.connectors.googleapps.GoogleAppsConnector\",\"displayName\":\"GoogleApps Connector\",\"bundleName\":\"org.forgerock.openicf.connectors.googleapps-connector\",\"systemType\":\"provisioner.openicf\",\"bundleVersion\":\"1.4.0.0\"},{\"connectorName\":\"org.forgerock.openicf.connectors.groovy.ScriptedPoolableConnector\",\"displayName\":\"Scripted Poolable Groovy Connector\",\"bundleName\":\"org.forgerock.openicf.connectors.groovy-connector\",\"systemType\":\"provisioner.openicf\",\"bundleVersion\":\"1.4.1.0\"},{\"connectorName\":\"org.forgerock.openicf.connectors.groovy.ScriptedConnector\",\"displayName\":\"Scripted Groovy Connector\",\"bundleName\":\"org.forgerock.openicf.connectors.groovy-connector\",\"systemType\":\"provisioner.openicf\",\"bundleVersion\":\"1.4.1.0\"},{\"connectorName\":\"org.forgerock.openicf.connectors.scriptedcrest.ScriptedCRESTConnector\",\"displayName\":\"Scripted CREST Connector\",\"bundleName\":\"org.forgerock.openicf.connectors.groovy-connector\",\"systemType\":\"provisioner.openicf\",\"bundleVersion\":\"1.4.1.0\"},{\"connectorName\":\"org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConnector\",\"displayName\":\"Scripted SQL Connector\",\"bundleName\":\"org.forgerock.openicf.connectors.groovy-connector\",\"systemType\":\"provisioner.openicf\",\"bundleVersion\":\"1.4.1.0\"},{\"connectorName\":\"org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConnector\",\"displayName\":\"Scripted REST Connector\",\"bundleName\":\"org.forgerock.openicf.connectors.groovy-connector\",\"systemType\":\"provisioner.openicf\",\"bundleVersion\":\"1.4.1.0\"},{\"connectorName\":\"org.identityconnectors.ldap.LdapConnector\",\"displayName\":\"LDAP Connector\",\"bundleName\":\"org.forgerock.openicf.connectors.ldap-connector\",\"systemType\":\"provisioner.openicf\",\"bundleVersion\":\"1.4.0.0-RC4\"},{\"connectorName\":\"org.forgerock.openicf.connectors.xml.XMLConnector\",\"displayName\":\"XML Connector\",\"bundleName\":\"org.forgerock.openicf.connectors.xml-connector\",\"systemType\":\"provisioner.openicf\",\"bundleVersion\":\"1.1.0.1\"},{\"bundleName\":\"org.forgerock.openidm.salesforce\",\"bundleVersion\":\"2.0.29.2-SNAPSHOT\",\"displayName\":\"Salesforce Connector\",\"connectorName\":\"org.forgerock.openidm.salesforce.Salesforce\",\"systemType\":\"provisioner.salesforce\"}]}"
            ]
        );

        server.respondWith(
            "POST",
            "/openidm/system?_action=createCoreConfig",
            [
                200,
                { },
                "{\"connectorRef\":{\"connectorName\":\"org.forgerock.openicf.csvfile.CSVFileConnector\",\"displayName\":\"CSV Connector\",\"bundleName\":\"org.forgerock.openicf.connectors.csvfile-connector\",\"systemType\":\"provisioner.openicf\",\"bundleVersion\":\"1.1.0.1\"},\"configurationProperties\":{\"preserveLastTokens\":10,\"passwordAttribute\":null,\"alwaysQualify\":true,\"fieldDelimiter\":\",\",\"filePath\":null,\"encoding\":\"utf-8\",\"usingMultivalue\":false,\"valueQualifier\":\"\\\"\",\"nameAttribute\":null,\"multivalueDelimiter\":\";\",\"uniqueAttribute\":null},\"operationTimeout\":{\"CREATE\":-1,\"UPDATE\":-1,\"DELETE\":-1,\"TEST\":-1,\"SCRIPT_ON_CONNECTOR\":-1,\"SCRIPT_ON_RESOURCE\":-1,\"GET\":-1,\"RESOLVEUSERNAME\":-1,\"AUTHENTICATE\":-1,\"SEARCH\":-1,\"VALIDATE\":-1,\"SYNC\":-1,\"SCHEMA\":-1},\"resultsHandlerConfig\":{\"enableNormalizingResultsHandler\":true,\"enableFilteredResultsHandler\":true,\"enableCaseInsensitiveFilter\":false,\"enableAttributesToGetSearchResultsHandler\":true},\"poolConfigOption\":{\"maxObjects\":10,\"maxIdle\":10,\"maxWait\":150000,\"minEvictableIdleTimeMillis\":120000,\"minIdle\":1}}"
            ]
        );

        server.respondWith(
            "GET",
            "/openidm/scheduler?_queryId=query-all-ids",
            [
                200,
                { },
                "{\"result\":[{\"_id\":\"recon\"}],\"resultCount\":1,\"pagedResultsCookie\":null,\"remainingPagedResults\":-1}"
            ]
        );

        server.respondWith(
            "GET",
            "/openidm/scheduler/recon",
            [
                200,
                { },
                "{\"enabled\":false,\"persisted\":false,\"misfirePolicy\":\"fireAndProceed\",\"schedule\":\"30 0/1 * * * ?\",\"type\":\"cron\",\"invokeService\":\"org.forgerock.openidm.sync\",\"invokeContext\":{\"action\":\"reconcile\",\"mapping\":\"systemLdapAccounts_managedUser\"},\"invokeLogLevel\":\"info\",\"timeZone\":null,\"startTime\":null,\"endTime\":null,\"concurrentExecution\":false,\"_id\":\"recon\"}"
            ]
        );
    };

});
