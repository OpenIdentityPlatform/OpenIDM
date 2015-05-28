/*global require, define*/
define([ ],function () {
    return function (server) {
    
        server.respondWith(
            "GET",
            "/openidm/info/login",
            [
                200,
                {},
                "{\"authorizationId\":{\"id\":\"openidm-admin\",\"component\":\"repo/internal/user\",\"ipAddress\":\"0:0:0:0:0:0:0:1\",\"roles\":[\"openidm-admin\",\"openidm-authorized\"]},\"parent\":{\"id\":\"fcedcc88-c362-4d73-b18b-0a5d5d875a5c\",\"parent\":null,\"class\":\"org.forgerock.json.resource.RootContext\"},\"class\":\"org.forgerock.json.resource.SecurityContext\",\"authenticationId\":\"openidm-admin\"}"
            ]
        );

        server.respondWith(
            "GET",
            "/openidm/repo/internal/user/openidm-admin",
            [
                200,
                {},
                "{\"_id\":\"openidm-admin\",\"_rev\":\"2\",\"roles\":\"openidm-admin,openidm-authorized\",\"userName\":\"openidm-admin\",\"password\":{\"$crypto\":{\"type\":\"x-simple-encryption\",\"value\":{\"data\":\"vB8UlvY4aslX6yH7wGy19g==\",\"cipher\":\"AES/CBC/PKCS5Padding\",\"iv\":\"zMB6f/P++Z1ppu20Eb+k4g==\",\"key\":\"openidm-sym-default\"}}}}"
            ]
        );

        server.respondWith(
            "GET",
            "/openidm/endpoint/mappingDetails?mapping=systemLdapAccounts_managedUser",
            [
                200,
                {},
                "{\"_id\":\"sync\",\"mappings\":[{\"target\":\"managed/user\",\"properties\":[{\"target\":\"displayName\",\"source\":\"cn\"},{\"target\" : \"test\", \"default\" : \"test\"}, {\"target\":\"description\",\"source\":\"description\"},{\"target\":\"givenName\",\"source\":\"givenName\"},{\"target\":\"mail\",\"source\":\"mail\"},{\"target\":\"telephoneNumber\",\"source\":\"telephoneNumber\"},{\"target\":\"sn\",\"source\":\"sn\"},{\"target\":\"userName\",\"source\":\"uid\"}],\"source\":\"system/ldap/account\",\"policies\":[{\"action\":\"UPDATE\",\"situation\":\"CONFIRMED\"},{\"action\":\"UPDATE\",\"situation\":\"FOUND\"},{\"action\":\"CREATE\",\"situation\":\"ABSENT\"},{\"action\":\"EXCEPTION\",\"situation\":\"AMBIGUOUS\"},{\"action\":\"CREATE\",\"situation\":\"MISSING\"},{\"action\":\"DELETE\",\"situation\":\"SOURCE_MISSING\"},{\"action\":\"IGNORE\",\"situation\":\"UNQUALIFIED\"},{\"action\":\"IGNORE\",\"situation\":\"UNASSIGNED\"}],\"name\":\"systemLdapAccounts_managedUser\"},{\"target\":\"system/ldap/account\",\"links\":\"systemLdapAccounts_managedUser\",\"properties\":[{\"target\":\"givenName\",\"source\":\"givenName\"},{\"target\":\"sn\",\"source\":\"sn\"},{\"target\":\"cn\",\"source\":\"\",\"transform\":{\"type\":\"text/javascript\",\"source\":\"source.displayName || (source.givenName + ' ' + source.sn);\"}},{\"target\":\"uid\",\"source\":\"userName\"},{\"target\":\"description\",\"source\":\"description\",\"condition\":{\"type\":\"text/javascript\",\"source\":\"!!object.description\"}},{\"target\":\"mail\",\"source\":\"mail\"},{\"target\":\"userPassword\",\"transform\":{\"type\":\"text/javascript\",\"source\":\"openidm.decrypt(source);\"},\"source\":\"password\",\"condition\":{\"type\":\"text/javascript\",\"source\":\"object.password != null\"}},{\"target\":\"telephoneNumber\",\"source\":\"telephoneNumber\",\"condition\":{\"type\":\"text/javascript\",\"source\":\"!!object.telephoneNumber\"}}],\"source\":\"managed/user\",\"onCreate\":{\"type\":\"text/javascript\",\"source\":\"target.dn = 'uid=' + source.userName + ',ou=People,dc=example,dc=com';\"},\"policies\":[{\"action\":\"UPDATE\",\"situation\":\"CONFIRMED\"},{\"action\":\"LINK\",\"situation\":\"FOUND\"},{\"action\":\"CREATE\",\"situation\":\"ABSENT\"},{\"action\":\"IGNORE\",\"situation\":\"AMBIGUOUS\"},{\"action\":\"IGNORE\",\"situation\":\"MISSING\"},{\"action\":\"DELETE\",\"situation\":\"SOURCE_MISSING\"},{\"action\":\"IGNORE\",\"situation\":\"UNQUALIFIED\"},{\"action\":\"IGNORE\",\"situation\":\"UNASSIGNED\"}],\"name\":\"managedUser_systemLdapAccounts\"}]}"
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

        server.respondWith(
            "POST",
            "/openidm/system?_action=test",
            [
                200,
                {},
                "[{\"name\":\"ldap\",\"enabled\":true,\"config\":\"config/provisioner.openicf/ldap\",\"objectTypes\":[\"__ALL__\",\"group\",\"account\"],\"connectorRef\":{\"connectorName\":\"org.identityconnectors.ldap.LdapConnector\",\"bundleName\":\"org.forgerock.openicf.connectors.ldap-connector\",\"bundleVersion\":\"[1.4.0.0,2.0.0.0)\"},\"ok\":true}]"
            ]
        );

        server.respondWith(
            "GET",
            "/openidm/config/managed",
            [
                200,
                {},
                "{\"_id\":\"managed\",\"objects\":[{\"onDelete\":{\"type\":\"text/javascript\",\"file\":\"ui/onDelete-user-cleanup.js\"},\"onCreate\":{\"type\":\"text/javascript\",\"file\":\"ui/onCreate-user-set-default-fields.js\"},\"properties\":[{\"encryption\":{\"key\":\"openidm-sym-default\"},\"name\":\"securityAnswer\",\"scope\":\"private\"},{\"encryption\":{\"key\":\"openidm-sym-default\"},\"name\":\"password\",\"scope\":\"private\"},{\"type\":\"virtual\",\"onRetrieve\":{\"type\":\"text/javascript\",\"rolesPropName\":\"roles\",\"file\":\"roles/effectiveRoles.js\"},\"name\":\"effectiveRoles\"},{\"type\":\"virtual\",\"onRetrieve\":{\"type\":\"text/javascript\",\"effectiveRolesPropName\":\"effectiveRoles\",\"file\":\"roles/effectiveAssignments.js\"},\"name\":\"effectiveAssignments\"}],\"name\":\"user\"},{\"postUpdate\":{\"type\":\"text/javascript\",\"file\":\"roles/update-users-of-role.js\"},\"postCreate\":{\"type\":\"text/javascript\",\"file\":\"roles/update-users-of-role.js\"},\"onDelete\":{\"type\":\"text/javascript\",\"file\":\"roles/onDelete-roles.js\"},\"postDelete\":{\"type\":\"text/javascript\",\"file\":\"roles/update-users-of-role.js\"},\"name\":\"role\"}]}"
            ]
        );

        server.respondWith(
            "GET",
            "/openidm/config/provisioner.openicf/ldap",
            [
                200,
                {},
                "{\"syncFailureHandler\":{\"postRetryAction\":\"logged-ignore\",\"maxRetries\":5},\"configurationProperties\":{\"port\":1389,\"vlvSortAttribute\":\"uid\",\"accountSynchronizationFilter\":null,\"passwordAttributeToSynchronize\":null,\"passwordAttribute\":\"userPassword\",\"useBlocks\":false,\"synchronizePasswords\":false,\"maintainPosixGroupMembership\":false,\"failover\":[],\"ssl\":false,\"modifiersNamesToFilterOut\":[],\"removeLogEntryObjectClassFromFilter\":true,\"passwordDecryptionKey\":null,\"groupSynchronizationFilter\":null,\"principal\":\"cn=Directory Manager\",\"baseContexts\":[\"dc=example,dc=com\"],\"credentials\":{\"$crypto\":{\"type\":\"x-simple-encryption\",\"value\":{\"data\":\"otNGMeuujRiJ8q3KTpTvDA==\",\"cipher\":\"AES/CBC/PKCS5Padding\",\"iv\":\"PR3HTJJpBj7WTnK3VI06Tw==\",\"key\":\"openidm-sym-default\"}}},\"readSchema\":true,\"accountObjectClasses\":[\"top\",\"person\",\"organizationalPerson\",\"inetOrgPerson\"],\"changeLogBlockSize\":100,\"host\":\"localhost\",\"accountUserNameAttributes\":[\"uid\"],\"baseContextsToSynchronize\":[\"dc=example,dc=com\"],\"attributesToSynchronize\":[],\"groupMemberAttribute\":\"uniqueMember\",\"changeNumberAttribute\":\"changeNumber\",\"accountSearchFilter\":null,\"passwordHashAlgorithm\":null,\"usePagedResultControl\":false,\"blockSize\":100,\"passwordDecryptionInitializationVector\":null,\"uidAttribute\":\"dn\",\"groupSearchFilter\":null,\"maintainLdapGroupMembership\":false,\"filterWithOrInsteadOfAnd\":false,\"respectResourcePasswordPolicyChangeAfterReset\":false,\"objectClassesToSynchronize\":[\"inetOrgPerson\"]},\"_id\":\"provisioner.openicf/ldap\",\"objectTypes\":{\"group\":{\"id\":\"__GROUP__\",\"properties\":{\"ou\":{\"type\":\"array\",\"nativeName\":\"ou\",\"items\":{\"type\":\"string\",\"nativeType\":\"string\"},\"nativeType\":\"string\"},\"cn\":{\"nativeName\":\"cn\",\"type\":\"array\",\"required\":true,\"items\":{\"type\":\"string\",\"nativeType\":\"string\"},\"nativeType\":\"string\"},\"description\":{\"type\":\"array\",\"nativeName\":\"description\",\"items\":{\"type\":\"string\",\"nativeType\":\"string\"},\"nativeType\":\"string\"},\"owner\":{\"type\":\"array\",\"nativeName\":\"owner\",\"items\":{\"type\":\"string\",\"nativeType\":\"string\"},\"nativeType\":\"string\"},\"o\":{\"type\":\"array\",\"nativeName\":\"o\",\"items\":{\"type\":\"string\",\"nativeType\":\"string\"},\"nativeType\":\"string\"},\"seeAlso\":{\"type\":\"array\",\"nativeName\":\"seeAlso\",\"items\":{\"type\":\"string\",\"nativeType\":\"string\"},\"nativeType\":\"string\"},\"dn\":{\"type\":\"string\",\"required\":true,\"nativeName\":\"__NAME__\",\"nativeType\":\"string\"},\"businessCategory\":{\"type\":\"array\",\"nativeName\":\"businessCategory\",\"items\":{\"type\":\"string\",\"nativeType\":\"string\"},\"nativeType\":\"string\"},\"uniqueMember\":{\"type\":\"array\",\"nativeName\":\"uniqueMember\",\"items\":{\"type\":\"string\",\"nativeType\":\"string\"},\"nativeType\":\"string\"}},\"type\":\"object\",\"$schema\":\"http://json-schema.org/draft-03/schema\",\"nativeType\":\"__GROUP__\"},\"account\":{\"id\":\"__ACCOUNT__\",\"properties\":{\"uid\":{\"type\":\"string\",\"nativeName\":\"uid\",\"nativeType\":\"string\"},\"mail\":{\"type\":\"string\",\"nativeName\":\"mail\",\"nativeType\":\"string\"},\"sn\":{\"type\":\"string\",\"nativeName\":\"sn\",\"nativeType\":\"string\"},\"cn\":{\"type\":\"string\",\"nativeName\":\"cn\",\"nativeType\":\"string\"},\"description\":{\"type\":\"string\",\"nativeName\":\"description\",\"nativeType\":\"string\"},\"telephoneNumber\":{\"type\":\"string\",\"nativeName\":\"telephoneNumber\",\"nativeType\":\"string\"},\"userPassword\":{\"nativeName\":\"userPassword\",\"flags\":[\"NOT_READABLE\",\"NOT_RETURNED_BY_DEFAULT\"],\"type\":\"string\",\"nativeType\":\"string\"},\"ldapGroups\":{\"type\":\"array\",\"nativeName\":\"ldapGroups\",\"items\":{\"type\":\"string\",\"nativeType\":\"string\"},\"nativeType\":\"string\"},\"givenName\":{\"type\":\"string\",\"nativeName\":\"givenName\",\"nativeType\":\"string\"},\"dn\":{\"type\":\"string\",\"required\":true,\"nativeName\":\"__NAME__\",\"nativeType\":\"string\"}},\"type\":\"object\",\"$schema\":\"http://json-schema.org/draft-03/schema\",\"nativeType\":\"__ACCOUNT__\"}},\"operationOptions\":{\"DELETE\":{\"onDeny\":\"DO_NOTHING\",\"denied\":false},\"UPDATE\":{\"onDeny\":\"DO_NOTHING\",\"denied\":false},\"CREATE\":{\"onDeny\":\"DO_NOTHING\",\"denied\":false}},\"poolConfigOption\":{\"maxWait\":150000,\"minIdle\":1,\"maxIdle\":10,\"minEvictableIdleTimeMillis\":120000,\"maxObjects\":10},\"name\":\"ldap\",\"connectorRef\":{\"bundleName\":\"org.forgerock.openicf.connectors.ldap-connector\",\"bundleVersion\":\"[1.4.0.0,2.0.0.0)\",\"connectorName\":\"org.identityconnectors.ldap.LdapConnector\"},\"operationTimeout\":{\"TEST\":-1,\"UPDATE\":-1,\"CREATE\":-1,\"AUTHENTICATE\":-1,\"GET\":-1,\"SCRIPT_ON_RESOURCE\":-1,\"SEARCH\":-1,\"SCHEMA\":-1,\"DELETE\":-1,\"SYNC\":-1,\"VALIDATE\":-1,\"SCRIPT_ON_CONNECTOR\":-1}}"
            ]
        );

    };

});
