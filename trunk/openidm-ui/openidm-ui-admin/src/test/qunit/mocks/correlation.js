/*global require, define*/
define([
    "text!templates/admin/mapping/MappingTemplate.html",
    "text!templates/admin/sync/CorrelationTemplate.html",
    "text!templates/admin/sync/AnalysisTemplate.html",
    "text!templates/admin/sync/CorrelationQueryTemplate.html",
    "text!templates/admin/sync/MappingScriptsTemplate.html",
    "text!templates/admin/sync/ReconQueriesTemplate.html",
    "text!templates/admin/util/AbstractScriptView.html",
    "text!templates/admin/util/SetupFilter.html",
    "text!templates/admin/sync/ChangeAssociationDialogTemplate.html"
],function () {
    var staticFiles = [
            "templates/admin/mapping/MappingTemplate.html",
            "templates/admin/sync/CorrelationTemplate.html",
            "templates/admin/sync/AnalysisTemplate.html",
            "templates/admin/sync/CorrelationQueryTemplate.html",
            "templates/admin/sync/MappingScriptsTemplate.html",
            "templates/admin/sync/ReconQueriesTemplate.html",
            "templates/admin/util/AbstractScriptView.html",
            "templates/admin/util/SetupFilter.html",
            "templates/admin/sync/ChangeAssociationDialogTemplate.html"
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
            "GET",
            "/openidm/endpoint/mappingDetails?mapping=systemXmlfileAccounts_managedUser",
            [
                200,
                { },
                "{\"mappings\":[{\"name\":\"systemXmlfileAccounts_managedUser\",\"source\":\"system/xmlfile/account\",\"target\":\"managed/user\",\"correlationQuery\":{\"type\":\"text/javascript\",\"source\":\"var query = {'_queryId' : 'for-userName', 'uid' : source.name};query;\"},\"properties\":[{\"source\":\"email\",\"target\":\"mail\"},{\"source\":\"firstname\",\"target\":\"givenName\"},{\"source\":\"lastname\",\"target\":\"sn\"},{\"source\":\"description\",\"target\":\"description\"},{\"source\":\"_id\",\"target\":\"_id\"},{\"source\":\"name\",\"target\":\"userName\"},{\"source\":\"password\",\"target\":\"password\"},{\"source\":\"mobileTelephoneNumber\",\"target\":\"telephoneNumber\"},{\"source\":\"roles\",\"transform\":{\"type\":\"text/javascript\",\"source\":\"source.split(',')\"},\"target\":\"roles\"}],\"policies\":[{\"situation\":\"CONFIRMED\",\"action\":\"UPDATE\"},{\"situation\":\"FOUND\",\"action\":\"IGNORE\"},{\"situation\":\"ABSENT\",\"action\":\"CREATE\"},{\"situation\":\"AMBIGUOUS\",\"action\":\"IGNORE\"},{\"situation\":\"MISSING\",\"action\":\"IGNORE\"},{\"situation\":\"SOURCE_MISSING\",\"action\":\"IGNORE\"},{\"situation\":\"UNQUALIFIED\",\"action\":\"IGNORE\"},{\"situation\":\"UNASSIGNED\",\"action\":\"IGNORE\"}],\"recon\":{\"_id\":\"8afcffb2-d17d-490c-9782-487b36be3f7d\",\"mapping\":\"systemXmlfileAccounts_managedUser\",\"state\":\"SUCCESS\",\"stage\":\"COMPLETED_SUCCESS\",\"stageDescription\":\"reconciliation completed.\",\"progress\":{\"source\":{\"existing\":{\"processed\":2,\"total\":\"2\"}},\"target\":{\"existing\":{\"processed\":0,\"total\":\"0\"},\"created\":2},\"links\":{\"existing\":{\"processed\":0,\"total\":\"0\"},\"created\":2}},\"situationSummary\":{\"SOURCE_IGNORED\":0,\"MISSING\":0,\"FOUND\":0,\"AMBIGUOUS\":0,\"UNQUALIFIED\":0,\"CONFIRMED\":0,\"SOURCE_MISSING\":0,\"ABSENT\":2,\"TARGET_IGNORED\":0,\"UNASSIGNED\":0,\"FOUND_ALREADY_LINKED\":0},\"statusSummary\":{\"FAILURE\":0,\"SUCCESS\":2},\"parameters\":{\"object\":{\"sourceQuery\":{\"resourceName\":\"system/xmlfile/account\",\"_queryId\":\"query-all-ids\"},\"targetQuery\":{\"resourceName\":\"managed/user\",\"_queryId\":\"query-all-ids\"}},\"pointer\":{\"empty\":true},\"transformers\":[],\"set\":false,\"map\":true,\"string\":false,\"collection\":false,\"wrappedObject\":{\"sourceQuery\":{\"resourceName\":\"system/xmlfile/account\",\"_queryId\":\"query-all-ids\"},\"targetQuery\":{\"resourceName\":\"managed/user\",\"_queryId\":\"query-all-ids\"}},\"list\":false,\"number\":false,\"boolean\":false,\"null\":false},\"started\":\"2015-01-20T22:51:43.001Z\",\"ended\":\"2015-01-20T22:51:43.295Z\",\"duration\":294}}],\"_id\":\"sync\"}"
            ]
        );

        server.respondWith(
            "GET",
            "/openidm/repo/audit/recon?_queryFilter=reconId%20eq%20%22cba4a732-b648-47fc-a93f-1f8855a50727%22%20and%20!(entryType%20eq%20%22summary%22)%20and%20timestamp%20gt%20%222015-01-22T17%3A38%3A17.588Z%22",
            [
                200,
                { },
                "{\"result\":[],\"resultCount\":0,\"pagedResultsCookie\":null,\"remainingPagedResults\":-1}"
            ]
        );

        server.respondWith(
            "GET",
            /\/openidm\/endpoint\/reconResults.*/,
            //"/openidm/endpoint/reconResults?_queryId=reconResults&source=system/xmlfile/account&target=managed/user&sourceProps=email,firstname,lastname,description&targetProps=mail,givenName,sn,description&reconId=cba4a732-b648-47fc-a93f-1f8855a50727&situations=SOURCE_IGNORED,MISSING,FOUND,AMBIGUOUS,UNQUALIFIED,CONFIRMED,SOURCE_MISSING,ABSENT,TARGET_IGNORED,UNASSIGNED,FOUND_ALREADY_LINKED&mapping=systemXmlfileAccounts_managedUser&nd=1421794308225&rows=10&page=1&sidx=&sord=asc&search=false",
            [
                200,
                { },
                "{\"result\":[{\"id\":\"_id\",\"limit\":10,\"page\":1,\"rows\":[{\"sourceObjectId\":\"system/xmlfile/account/bjensen\",\"targetObjectId\":\"managed/user/bjensen\",\"ambiguousTargetObjectIds\":\"\",\"timestamp\":\"2015-01-20T22:51:43.273Z\",\"situation\":\"ABSENT\",\"sourceObject\":{\"firstname\":\"Barbara\",\"description\":\"Created By XML1\",\"_id\":\"bjensen\",\"mobileTelephoneNumber\":\"1234567\",\"password\":\"TestPassw0rd#\",\"lastname\":\"Jensen\",\"roles\":\"openidm-authorized\",\"email\":\"bjensen@example.com\",\"name\":\"bjensen@example.com\"},\"targetObject\":{\"_id\":\"bjensen\",\"_rev\":\"1\",\"mail\":\"bjensen@example.com\",\"sn\":\"Jensen\",\"description\":\"Created By XML1\",\"roles\":[\"openidm-authorized\"],\"telephoneNumber\":\"1234567\",\"userName\":\"bjensen@example.com\",\"givenName\":\"Barbara\",\"password\":{\"$crypto\":{\"value\":{\"iv\":\"/4RSRnPywXegeBVcBtuvJQ==\",\"data\":\"b7WowHq6jnePEXj+6D1ptg==\",\"cipher\":\"AES/CBC/PKCS5Padding\",\"key\":\"openidm-sym-default\"},\"type\":\"x-simple-encryption\"}},\"accountStatus\":\"active\",\"lastPasswordSet\":\"\",\"postalCode\":\"\",\"stateProvince\":\"\",\"passwordAttempts\":\"0\",\"lastPasswordAttempt\":\"Tue Jan 20 2015 14:51:43 GMT-0800 (PST)\",\"postalAddress\":\"\",\"address2\":\"\",\"country\":\"\",\"city\":\"\",\"effectiveRoles\":[\"openidm-authorized\"],\"effectiveAssignments\":null},\"hasLink\":true},{\"sourceObjectId\":\"system/xmlfile/account/scarter\",\"targetObjectId\":\"managed/user/scarter\",\"ambiguousTargetObjectIds\":\"\",\"timestamp\":\"2015-01-20T22:51:43.273Z\",\"situation\":\"ABSENT\",\"sourceObject\":{\"firstname\":\"Steven\",\"description\":\"Created By XML1\",\"_id\":\"scarter\",\"mobileTelephoneNumber\":\"1234567\",\"password\":\"TestPassw0rd#\",\"lastname\":\"Carter\",\"roles\":\"openidm-admin,openidm-authorized\",\"email\":\"scarter@example.com\",\"name\":\"scarter@example.com\"},\"targetObject\":{\"_id\":\"scarter\",\"_rev\":\"1\",\"mail\":\"scarter@example.com\",\"sn\":\"Carter\",\"description\":\"Created By XML1\",\"roles\":[\"openidm-admin\",\"openidm-authorized\"],\"telephoneNumber\":\"1234567\",\"userName\":\"scarter@example.com\",\"givenName\":\"Steven\",\"password\":{\"$crypto\":{\"value\":{\"iv\":\"6gbRAQQPb4RTAWYSOCP2mA==\",\"data\":\"2a6ODCHCBn/oZVwsZ/um6g==\",\"cipher\":\"AES/CBC/PKCS5Padding\",\"key\":\"openidm-sym-default\"},\"type\":\"x-simple-encryption\"}},\"accountStatus\":\"active\",\"lastPasswordSet\":\"\",\"postalCode\":\"\",\"stateProvince\":\"\",\"passwordAttempts\":\"0\",\"lastPasswordAttempt\":\"Tue Jan 20 2015 14:51:43 GMT-0800 (PST)\",\"postalAddress\":\"\",\"address2\":\"\",\"country\":\"\",\"city\":\"\",\"effectiveRoles\":[\"openidm-admin\",\"openidm-authorized\"],\"effectiveAssignments\":null},\"hasLink\":true}]}],\"resultCount\":1,\"pagedResultsCookie\":null,\"remainingPagedResults\":-1}"
            ]
        );


        server.respondWith(
            "POST",
            "/openidm/system?_action=test",
            [
                200,
                { },
                "[{\"name\":\"xmlfile\",\"enabled\":true,\"config\":\"config/provisioner.openicf/xml\",\"objectTypes\":[\"__ALL__\",\"account\"],\"connectorRef\":{\"connectorName\":\"org.forgerock.openicf.connectors.xml.XMLConnector\",\"bundleName\":\"org.forgerock.openicf.connectors.xml-connector\",\"bundleVersion\":\"1.1.0.2\"},\"ok\":true}]"
            ]
        );

        server.respondWith(
            "GET",
            "/openidm/config/managed",
            [
                200,
                { },
                "{\"objects\":[{\"name\":\"user\",\"onCreate\":{\"type\":\"text/javascript\",\"file\":\"ui/onCreate-user-set-default-fields.js\"},\"onDelete\":{\"type\":\"text/javascript\",\"file\":\"ui/onDelete-user-cleanup.js\"},\"properties\":[{\"name\":\"securityAnswer\",\"encryption\":{\"key\":\"openidm-sym-default\"},\"scope\":\"private\"},{\"name\":\"password\",\"encryption\":{\"key\":\"openidm-sym-default\"},\"scope\":\"private\"},{\"name\":\"effectiveRoles\",\"type\":\"virtual\",\"onRetrieve\":{\"type\":\"text/javascript\",\"file\":\"roles/effectiveRoles.js\",\"rolesPropName\":\"roles\"}},{\"name\":\"effectiveAssignments\",\"type\":\"virtual\",\"onRetrieve\":{\"type\":\"text/javascript\",\"file\":\"roles/effectiveAssignments.js\",\"effectiveRolesPropName\":\"effectiveRoles\"}}]},{\"name\":\"role\",\"postCreate\":{\"type\":\"text/javascript\",\"file\":\"roles/update-users-of-role.js\"},\"postUpdate\":{\"type\":\"text/javascript\",\"file\":\"roles/update-users-of-role.js\"},\"postDelete\":{\"type\":\"text/javascript\",\"file\":\"roles/update-users-of-role.js\"},\"onDelete\":{\"type\":\"text/javascript\",\"file\":\"roles/onDelete-roles.js\"}}],\"_id\":\"managed\"}"
            ]
        );

        server.respondWith(
            "GET",
            "/openidm/config/provisioner.openicf/xml",
            [
                200,
                { },
                "{\"name\":\"xmlfile\",\"connectorRef\":{\"bundleName\":\"org.forgerock.openicf.connectors.xml-connector\",\"bundleVersion\":\"1.1.0.2\",\"connectorName\":\"org.forgerock.openicf.connectors.xml.XMLConnector\"},\"producerBufferSize\":100,\"connectorPoolingSupported\":true,\"poolConfigOption\":{\"maxObjects\":1,\"maxIdle\":1,\"maxWait\":150000,\"minEvictableIdleTimeMillis\":120000,\"minIdle\":1},\"operationTimeout\":{\"CREATE\":-1,\"TEST\":-1,\"AUTHENTICATE\":-1,\"SEARCH\":-1,\"VALIDATE\":-1,\"GET\":-1,\"UPDATE\":-1,\"DELETE\":-1,\"SCRIPT_ON_CONNECTOR\":-1,\"SCRIPT_ON_RESOURCE\":-1,\"SYNC\":-1,\"SCHEMA\":-1},\"configurationProperties\":{\"xsdIcfFilePath\":\"/Users/forgerock/Desktop/openIDM2/openidm-zip/target/openidm/samples/sample1/data/resource-schema-1.xsd\",\"xsdFilePath\":\"/Users/forgerock/Desktop/openIDM2/openidm-zip/target/openidm/samples/sample1/data/resource-schema-extension.xsd\",\"xmlFilePath\":\"/Users/forgerock/Desktop/openIDM2/openidm-zip/target/openidm/samples/sample1/data/xmlConnectorData.xml\"},\"syncFailureHandler\":{\"maxRetries\":5,\"postRetryAction\":\"logged-ignore\"},\"objectTypes\":{\"account\":{\"$schema\":\"http://json-schema.org/draft-03/schema\",\"id\":\"__ACCOUNT__\",\"type\":\"object\",\"nativeType\":\"__ACCOUNT__\",\"properties\":{\"description\":{\"type\":\"string\",\"nativeName\":\"__DESCRIPTION__\",\"nativeType\":\"string\"},\"firstname\":{\"type\":\"string\",\"nativeName\":\"firstname\",\"nativeType\":\"string\"},\"email\":{\"type\":\"string\",\"nativeName\":\"email\",\"nativeType\":\"string\"},\"_id\":{\"type\":\"string\",\"nativeName\":\"__UID__\"},\"password\":{\"type\":\"string\",\"nativeName\":\"password\",\"nativeType\":\"string\"},\"name\":{\"type\":\"string\",\"required\":true,\"nativeName\":\"__NAME__\",\"nativeType\":\"string\"},\"lastname\":{\"type\":\"string\",\"required\":true,\"nativeName\":\"lastname\",\"nativeType\":\"string\"},\"mobileTelephoneNumber\":{\"type\":\"string\",\"required\":true,\"nativeName\":\"mobileTelephoneNumber\",\"nativeType\":\"string\"},\"roles\":{\"type\":\"string\",\"required\":false,\"nativeName\":\"roles\",\"nativeType\":\"string\"}}}},\"operationOptions\":{},\"_id\":\"provisioner.openicf/xml\"}"
            ]
        );

        server.respondWith(
            "GET",
            "/openidm/config/ui/iconlist",
            [
                200,
                {},
                "{\"_id\":\"ui/iconlist\",\"icons\":[{\"type\":\"org.identityconnectors.ldap.LdapConnector\",\"iconClass\":\"connector-icon-ldap\",\"src\":\"img/icon-ldap.png\"},{\"type\":\"org.forgerock.openicf.connectors.xml.XMLConnector\",\"iconClass\":\"connector-icon-xml\",\"src\":\"img/icon-xml.png\"},{\"type\":\"org.forgerock.openidm.salesforce\",\"iconClass\":\"connector-icon-cloud\",\"src\":\"img/icon-cloud.png\"},{\"type\":\"org.identityconnectors.databasetable.DatabaseTableConnector\",\"iconClass\":\"connector-icon-databasetable\",\"src\":\"img/icon-db.png\"},{\"type\":\"org.forgerock.openicf.csvfile.CSVFileConnector\",\"iconClass\":\"connector-icon-csv\",\"src\":\"img/icon-csv.png\"},{\"type\":\"org.forgerock.openicf.connectors.googleapps.GoogleAppsConnector\",\"iconClass\":\"connector-icon-cloud\",\"src\":\"img/icon-cloud.png\"},{\"type\":\"org.forgerock.openidm.salesforce.Salesforce\",\"iconClass\":\"connector-icon-cloud\",\"src\":\"img/icon-cloud.png\"},{\"type\":\"org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConnector\",\"iconClass\":\"connector-icon-scriptedsql\",\"src\":\"img/icon-scriptedsql.png\"},{\"type\":\"managedobject\",\"iconClass\":\"connector-icon-managedobject\",\"src\":\"img/icon-managedobject.png\"}]}"
            ]
        );
    };

});
