/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All Rights Reserved
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
define([ ], function () {

    return function (server) {


        server.respondWith(
            "POST",
            "/openidm/system?_action=availableConnectors",
            [
                200,
                { },
                "{\"connectorRef\":[{\"connectorName\":\"org.forgerock.openicf.csvfile.CSVFileConnector\",\"displayName\":\"CSV File Connector\",\"bundleName\":\"org.forgerock.openicf.connectors.csvfile-connector\",\"systemType\":\"provisioner.openicf\",\"bundleVersion\":\"1.1.0.2\"},{\"connectorName\":\"org.identityconnectors.databasetable.DatabaseTableConnector\",\"displayName\":\"Database Table Connector\",\"bundleName\":\"org.forgerock.openicf.connectors.databasetable-connector\",\"systemType\":\"provisioner.openicf\",\"bundleVersion\":\"1.1.0.1\"},{\"connectorName\":\"org.forgerock.openicf.connectors.groovy.ScriptedPoolableConnector\",\"displayName\":\"Scripted Poolable Groovy Connector\",\"bundleName\":\"org.forgerock.openicf.connectors.groovy-connector\",\"systemType\":\"provisioner.openicf\",\"bundleVersion\":\"1.4.2.0-SNAPSHOT\"},{\"connectorName\":\"org.forgerock.openicf.connectors.groovy.ScriptedConnector\",\"displayName\":\"Scripted Groovy Connector\",\"bundleName\":\"org.forgerock.openicf.connectors.groovy-connector\",\"systemType\":\"provisioner.openicf\",\"bundleVersion\":\"1.4.2.0-SNAPSHOT\"},{\"connectorName\":\"org.forgerock.openicf.connectors.scriptedcrest.ScriptedCRESTConnector\",\"displayName\":\"Scripted CREST Connector\",\"bundleName\":\"org.forgerock.openicf.connectors.groovy-connector\",\"systemType\":\"provisioner.openicf\",\"bundleVersion\":\"1.4.2.0-SNAPSHOT\"},{\"connectorName\":\"org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConnector\",\"displayName\":\"Scripted SQL Connector\",\"bundleName\":\"org.forgerock.openicf.connectors.groovy-connector\",\"systemType\":\"provisioner.openicf\",\"bundleVersion\":\"1.4.2.0-SNAPSHOT\"},{\"connectorName\":\"org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConnector\",\"displayName\":\"Scripted REST Connector\",\"bundleName\":\"org.forgerock.openicf.connectors.groovy-connector\",\"systemType\":\"provisioner.openicf\",\"bundleVersion\":\"1.4.2.0-SNAPSHOT\"},{\"connectorName\":\"org.identityconnectors.ldap.LdapConnector\",\"displayName\":\"LDAP Connector\",\"bundleName\":\"org.forgerock.openicf.connectors.ldap-connector\",\"systemType\":\"provisioner.openicf\",\"bundleVersion\":\"1.4.0.0\"},{\"connectorName\":\"org.forgerock.openicf.connectors.xml.XMLConnector\",\"displayName\":\"XML Connector\",\"bundleName\":\"org.forgerock.openicf.connectors.xml-connector\",\"systemType\":\"provisioner.openicf\",\"bundleVersion\":\"1.1.0.2\"}]}"
            ]
        );

        server.respondWith(
            "GET",
            "/openidm/config/provisioner.openicf/ldap",
            [
                200,
                { },
                "{\"name\":\"ldap\",\"connectorRef\":{\"bundleName\":\"org.forgerock.openicf.connectors.ldap-connector\",\"bundleVersion\":\"[1.4.0.0,2.0.0.0)\",\"connectorName\":\"org.identityconnectors.ldap.LdapConnector\"},\"configurationProperties\":{\"host\":\"localhost\",\"port\":1389,\"ssl\":false,\"principal\":\"cn=Directory Manager\",\"credentials\":{\"$crypto\":{\"value\":{\"iv\":\"Ux3GWSp/4BVnYzzX4JNAwA==\",\"data\":\"MgGN34w3xPz56bUhpGqmQQ==\",\"cipher\":\"AES/CBC/PKCS5Padding\",\"key\":\"openidm-sym-default\"},\"type\":\"x-simple-encryption\"}},\"baseContexts\":[\"dc=example,dc=com\"],\"baseContextsToSynchronize\":[\"dc=example,dc=com\"],\"accountSearchFilter\":null,\"accountSynchronizationFilter\":null,\"groupSearchFilter\":null,\"groupSynchronizationFilter\":null,\"passwordAttributeToSynchronize\":null,\"synchronizePasswords\":false,\"removeLogEntryObjectClassFromFilter\":true,\"modifiersNamesToFilterOut\":[],\"passwordDecryptionKey\":null,\"changeLogBlockSize\":100,\"attributesToSynchronize\":[],\"changeNumberAttribute\":\"changeNumber\",\"passwordDecryptionInitializationVector\":null,\"filterWithOrInsteadOfAnd\":false,\"objectClassesToSynchronize\":[\"inetOrgPerson\"],\"vlvSortAttribute\":\"uid\",\"passwordAttribute\":\"userPassword\",\"useBlocks\":false,\"maintainPosixGroupMembership\":false,\"failover\":[],\"readSchema\":true,\"accountObjectClasses\":[\"top\",\"person\",\"organizationalPerson\",\"inetOrgPerson\"],\"accountUserNameAttributes\":[\"uid\"],\"groupMemberAttribute\":\"uniqueMember\",\"passwordHashAlgorithm\":null,\"usePagedResultControl\":true,\"blockSize\":100,\"uidAttribute\":\"dn\",\"maintainLdapGroupMembership\":false,\"respectResourcePasswordPolicyChangeAfterReset\":false},\"resultsHandlerConfig\":{\"enableNormalizingResultsHandler\":true,\"enableFilteredResultsHandler\":false,\"enableCaseInsensitiveFilter\":false,\"enableAttributesToGetSearchResultsHandler\":true},\"poolConfigOption\":{\"maxObjects\":10,\"maxIdle\":10,\"maxWait\":150000,\"minEvictableIdleTimeMillis\":120000,\"minIdle\":1},\"operationTimeout\":{\"CREATE\":-1,\"VALIDATE\":-1,\"TEST\":-1,\"SCRIPT_ON_CONNECTOR\":-1,\"SCHEMA\":-1,\"DELETE\":-1,\"UPDATE\":-1,\"SYNC\":-1,\"AUTHENTICATE\":-1,\"GET\":-1,\"SCRIPT_ON_RESOURCE\":-1,\"SEARCH\":-1},\"syncFailureHandler\":{\"maxRetries\":5,\"postRetryAction\":\"logged-ignore\"},\"objectTypes\":{\"account\":{\"$schema\":\"http://json-schema.org/draft-03/schema\",\"id\":\"__ACCOUNT__\",\"type\":\"object\",\"nativeType\":\"__ACCOUNT__\",\"properties\":{\"cn\":{\"type\":\"string\",\"nativeName\":\"cn\",\"nativeType\":\"string\"},\"employeeType\":{\"type\":\"string\",\"nativeName\":\"employeeType\",\"nativeType\":\"string\"},\"description\":{\"type\":\"string\",\"nativeName\":\"description\",\"nativeType\":\"string\"},\"givenName\":{\"type\":\"string\",\"nativeName\":\"givenName\",\"nativeType\":\"string\"},\"mail\":{\"type\":\"string\",\"nativeName\":\"mail\",\"nativeType\":\"string\"},\"telephoneNumber\":{\"type\":\"string\",\"nativeName\":\"telephoneNumber\",\"nativeType\":\"string\"},\"sn\":{\"type\":\"string\",\"nativeName\":\"sn\",\"nativeType\":\"string\"},\"uid\":{\"type\":\"string\",\"nativeName\":\"uid\",\"nativeType\":\"string\"},\"dn\":{\"type\":\"string\",\"nativeName\":\"__NAME__\",\"nativeType\":\"string\",\"required\":true},\"userPassword\":{\"type\":\"string\",\"nativeName\":\"userPassword\",\"nativeType\":\"string\",\"flags\":[\"NOT_READABLE\",\"NOT_RETURNED_BY_DEFAULT\"]},\"ldapGroups\":{\"type\":\"array\",\"items\":{\"type\":\"string\",\"nativeType\":\"string\"},\"nativeName\":\"ldapGroups\",\"nativeType\":\"string\"}}},\"group\":{\"$schema\":\"http://json-schema.org/draft-03/schema\",\"id\":\"__GROUP__\",\"type\":\"object\",\"nativeType\":\"__GROUP__\",\"properties\":{\"seeAlso\":{\"type\":\"array\",\"items\":{\"type\":\"string\",\"nativeType\":\"string\"},\"nativeName\":\"seeAlso\",\"nativeType\":\"string\"},\"description\":{\"type\":\"array\",\"items\":{\"type\":\"string\",\"nativeType\":\"string\"},\"nativeName\":\"description\",\"nativeType\":\"string\"},\"uniqueMember\":{\"type\":\"array\",\"items\":{\"type\":\"string\",\"nativeType\":\"string\"},\"nativeName\":\"uniqueMember\",\"nativeType\":\"string\"},\"dn\":{\"type\":\"string\",\"required\":true,\"nativeName\":\"__NAME__\",\"nativeType\":\"string\"},\"o\":{\"type\":\"array\",\"items\":{\"type\":\"string\",\"nativeType\":\"string\"},\"nativeName\":\"o\",\"nativeType\":\"string\"},\"ou\":{\"type\":\"array\",\"items\":{\"type\":\"string\",\"nativeType\":\"string\"},\"nativeName\":\"ou\",\"nativeType\":\"string\"},\"businessCategory\":{\"type\":\"array\",\"items\":{\"type\":\"string\",\"nativeType\":\"string\"},\"nativeName\":\"businessCategory\",\"nativeType\":\"string\"},\"owner\":{\"type\":\"array\",\"items\":{\"type\":\"string\",\"nativeType\":\"string\"},\"nativeName\":\"owner\",\"nativeType\":\"string\"},\"cn\":{\"type\":\"array\",\"items\":{\"type\":\"string\",\"nativeType\":\"string\"},\"required\":true,\"nativeName\":\"cn\",\"nativeType\":\"string\"}}}},\"operationOptions\":{\"DELETE\":{\"denied\":false,\"onDeny\":\"DO_NOTHING\"},\"UPDATE\":{\"denied\":false,\"onDeny\":\"DO_NOTHING\"},\"CREATE\":{\"denied\":false,\"onDeny\":\"DO_NOTHING\"}},\"_id\":\"provisioner.openicf/ldap\"}"
            ]
        );

        server.respondWith(
            "GET",
            "/openidm/security/truststore/cert/openidm_ldap",
            [
                404,
                { },
                "{\"code\":404,\"reason\":\"Not Found\",\"message\":\"Not Found\"}"
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
            "/openidm/policy/config/provisioner.openicf/ldap",
            [
                200,
                { },
                "{\"resource\":\"config/provisioner.openicf/ldap\",\"properties\":[]}"
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

        server.respondWith(
            "POST",
            "/openidm/system?_action=createCoreConfig",
            [
                200,
                { },
                "{\"connectorRef\":{\"connectorName\":\"org.forgerock.openicf.csvfile.CSVFileConnector\",\"displayName\":\"CSV File Connector\",\"bundleName\":\"org.forgerock.openicf.connectors.csvfile-connector\",\"systemType\":\"provisioner.openicf\",\"bundleVersion\":\"1.1.0.2\"},\"poolConfigOption\":{\"maxObjects\":10,\"maxIdle\":10,\"maxWait\":150000,\"minEvictableIdleTimeMillis\":120000,\"minIdle\":1},\"resultsHandlerConfig\":{\"enableNormalizingResultsHandler\":true,\"enableFilteredResultsHandler\":true,\"enableCaseInsensitiveFilter\":false,\"enableAttributesToGetSearchResultsHandler\":true},\"operationTimeout\":{\"CREATE\":-1,\"UPDATE\":-1,\"DELETE\":-1,\"TEST\":-1,\"SCRIPT_ON_CONNECTOR\":-1,\"SCRIPT_ON_RESOURCE\":-1,\"GET\":-1,\"RESOLVEUSERNAME\":-1,\"AUTHENTICATE\":-1,\"SEARCH\":-1,\"VALIDATE\":-1,\"SYNC\":-1,\"SCHEMA\":-1},\"configurationProperties\":{\"preserveLastTokens\":10,\"passwordAttribute\":null,\"alwaysQualify\":true,\"fieldDelimiter\":\",\",\"filePath\":null,\"encoding\":\"utf-8\",\"usingMultivalue\":false,\"valueQualifier\":\"\\\"\",\"nameAttribute\":null,\"multivalueDelimiter\":\";\",\"uniqueAttribute\":null}}"
            ]
        );
    };

});