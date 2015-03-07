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
    "text!templates/admin/mapping/AddMappingTemplate.html",
    "text!templates/admin/mapping/MappingTemplate.html",
    "text!templates/admin/MapResourceView.html"
], function () {

    /* an unfortunate need to duplicate the file names here, but I haven't
     yet found a way to fool requirejs into doing dynamic dependencies */
    var staticFiles = [
            "templates/admin/mapping/AddMappingTemplate.html",
            "templates/admin/mapping/MappingTemplate.html",
            "templates/admin/MapResourceView.html"

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
            "/openidm/config/ui/iconlist",
            [
                200,
                { },
                "{\"_id\":\"ui/iconlist\",\"icons\":[{\"type\":\"org.identityconnectors.ldap.LdapConnector\",\"iconClass\":\"connector-icon-ldap\",\"src\":\"img/icon-ldap.png\"},{\"type\":\"org.forgerock.openicf.connectors.xml.XMLConnector\",\"iconClass\":\"connector-icon-xml\",\"src\":\"img/icon-xml.png\"},{\"type\":\"org.forgerock.openidm.salesforce\",\"iconClass\":\"connector-icon-cloud\",\"src\":\"img/icon-cloud.png\"},{\"type\":\"org.identityconnectors.databasetable.DatabaseTableConnector\",\"iconClass\":\"connector-icon-databasetable\",\"src\":\"img/icon-db.png\"},{\"type\":\"org.forgerock.openicf.csvfile.CSVFileConnector\",\"iconClass\":\"connector-icon-csv\",\"src\":\"img/icon-csv.png\"},{\"type\":\"org.forgerock.openicf.connectors.googleapps.GoogleAppsConnector\",\"iconClass\":\"connector-icon-cloud\",\"src\":\"img/icon-cloud.png\"},{\"type\":\"org.forgerock.openidm.salesforce.Salesforce\",\"iconClass\":\"connector-icon-cloud\",\"src\":\"img/icon-cloud.png\"},{\"type\":\"org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConnector\",\"iconClass\":\"connector-icon-scriptedsql\",\"src\":\"img/icon-scriptedsql.png\"},{\"type\":\"managedobject\",\"iconClass\":\"connector-icon-managedobject\",\"src\":\"img/icon-managedobject.png\"}]}"
            ]
        );

        server.respondWith(
            "GET",
            "/openidm/config/sync",
            [
                200,
                { },
                "{\"_id\":\"sync\",\"mappings\":[{\"target\":\"managed/user\",\"properties\":[{\"target\":\"displayName\",\"source\":\"cn\"},{\"target\":\"description\",\"source\":\"description\"},{\"target\":\"givenName\",\"source\":\"givenName\"},{\"target\":\"mail\",\"source\":\"mail\"},{\"target\":\"telephoneNumber\",\"source\":\"telephoneNumber\"},{\"target\":\"sn\",\"source\":\"sn\"},{\"target\":\"userName\",\"source\":\"uid\"}],\"source\":\"system/ldap/account\",\"policies\":[{\"action\":\"UPDATE\",\"situation\":\"CONFIRMED\"},{\"action\":\"UPDATE\",\"situation\":\"FOUND\"},{\"action\":\"CREATE\",\"situation\":\"ABSENT\"},{\"action\":\"EXCEPTION\",\"situation\":\"AMBIGUOUS\"},{\"action\":\"CREATE\",\"situation\":\"MISSING\"},{\"action\":\"DELETE\",\"situation\":\"SOURCE_MISSING\"},{\"action\":\"IGNORE\",\"situation\":\"UNQUALIFIED\"},{\"action\":\"IGNORE\",\"situation\":\"UNASSIGNED\"}],\"name\":\"systemLdapAccounts_managedUser\"},{\"target\":\"system/ldap/account\",\"links\":\"systemLdapAccounts_managedUser\",\"properties\":[{\"target\":\"givenName\",\"source\":\"givenName\"},{\"target\":\"sn\",\"source\":\"sn\"},{\"target\":\"cn\",\"source\":\"\",\"transform\":{\"type\":\"text/javascript\",\"source\":\"source.displayName || (source.givenName + ' ' + source.sn);\"}},{\"target\":\"uid\",\"source\":\"userName\"},{\"target\":\"description\",\"source\":\"description\",\"condition\":{\"type\":\"text/javascript\",\"source\":\"!!object.description\"}},{\"target\":\"mail\",\"source\":\"mail\"},{\"target\":\"userPassword\",\"transform\":{\"type\":\"text/javascript\",\"source\":\"openidm.decrypt(source);\"},\"source\":\"password\",\"condition\":{\"type\":\"text/javascript\",\"source\":\"object.password != null\"}},{\"target\":\"telephoneNumber\",\"source\":\"telephoneNumber\",\"condition\":{\"type\":\"text/javascript\",\"source\":\"!!object.telephoneNumber\"}}],\"source\":\"managed/user\",\"onCreate\":{\"type\":\"text/javascript\",\"source\":\"target.dn = 'uid=' + source.userName + ',ou=People,dc=example,dc=com';\"},\"policies\":[{\"action\":\"UPDATE\",\"situation\":\"CONFIRMED\"},{\"action\":\"LINK\",\"situation\":\"FOUND\"},{\"action\":\"CREATE\",\"situation\":\"ABSENT\"},{\"action\":\"IGNORE\",\"situation\":\"AMBIGUOUS\"},{\"action\":\"IGNORE\",\"situation\":\"MISSING\"},{\"action\":\"DELETE\",\"situation\":\"SOURCE_MISSING\"},{\"action\":\"IGNORE\",\"situation\":\"UNQUALIFIED\"},{\"action\":\"IGNORE\",\"situation\":\"UNASSIGNED\"}],\"name\":\"managedUser_systemLdapAccounts\"}]}"
            ]
        );

        server.respondWith(
            "GET",
            "/openidm/config/managed",
            [
                200,
                { },
                "{\"objects\":[{\"postUpdate\":{\"type\":\"text/javascript\",\"file\":\"roles/update-users-of-role.js\"},\"postCreate\":{\"type\":\"text/javascript\",\"file\":\"roles/update-users-of-role.js\"},\"postDelete\":{\"type\":\"text/javascript\",\"file\":\"roles/update-users-of-role.js\"},\"name\":\"role\"},{\"name\":\"apple\"},{\"onDelete\":{\"type\":\"text/javascript\",\"file\":\"ui/onDelete-user-cleanup.js\"},\"onCreate\":{\"type\":\"text/javascript\",\"file\":\"ui/onCreate-user-set-default-fields.js\"},\"properties\":[{\"encryption\":{\"key\":\"openidm-sym-default\"},\"name\":\"securityAnswer\",\"scope\":\"private\"},{\"encryption\":{\"key\":\"openidm-sym-default\"},\"name\":\"password\",\"scope\":\"private\"},{\"type\":\"virtual\",\"name\":\"effectiveRoles\"},{\"type\":\"virtual\",\"name\":\"effectiveAssignments\"},{\"type\":\"virtual\",\"name\":\"test\",\"scope\":\"private\"}],\"name\":\"user\"},{\"properties\":[{\"name\":\"test\"}],\"name\":\"test\"}]}"
            ]
        );

        server.respondWith(
            "POST",
            "/openidm/system?_action=test",
            [
                200,
                { },
                "[{\"name\":\"ldap\",\"enabled\":true,\"config\":\"config/provisioner.openicf/ldap\",\"objectTypes\":[\"__ALL__\",\"group\",\"account\"],\"connectorRef\":{\"connectorName\":\"org.identityconnectors.ldap.LdapConnector\",\"bundleName\":\"org.forgerock.openicf.connectors.ldap-connector\",\"bundleVersion\":\"[1.4.0.0,2.0.0.0)\"},\"ok\":true}]"
            ]
        );

        server.respondWith(
            "PUT",
            "/openidm/config/sync",
            [
                200,
                { },
                "{\"mappings\":[{\"target\":\"managed/user\",\"properties\":[{\"target\":\"displayName\",\"source\":\"cn\"},{\"target\":\"description\",\"source\":\"description\"},{\"target\":\"givenName\",\"source\":\"givenName\"},{\"target\":\"mail\",\"source\":\"mail\"},{\"target\":\"telephoneNumber\",\"source\":\"telephoneNumber\"},{\"target\":\"sn\",\"source\":\"sn\"},{\"target\":\"userName\",\"source\":\"uid\"}],\"source\":\"system/ldap/account\",\"policies\":[{\"action\":\"UPDATE\",\"situation\":\"CONFIRMED\"},{\"action\":\"UPDATE\",\"situation\":\"FOUND\"},{\"action\":\"CREATE\",\"situation\":\"ABSENT\"},{\"action\":\"EXCEPTION\",\"situation\":\"AMBIGUOUS\"},{\"action\":\"CREATE\",\"situation\":\"MISSING\"},{\"action\":\"DELETE\",\"situation\":\"SOURCE_MISSING\"},{\"action\":\"IGNORE\",\"situation\":\"UNQUALIFIED\"},{\"action\":\"IGNORE\",\"situation\":\"UNASSIGNED\"}],\"name\":\"systemLdapAccounts_managedUser\"},{\"target\":\"system/ldap/account\",\"links\":\"systemLdapAccounts_managedUser\",\"properties\":[{\"target\":\"givenName\",\"source\":\"givenName\"},{\"target\":\"sn\",\"source\":\"sn\"},{\"target\":\"cn\",\"source\":\"\",\"transform\":{\"type\":\"text/javascript\",\"source\":\"source.displayName || (source.givenName + ' ' + source.sn);\"}},{\"target\":\"uid\",\"source\":\"userName\"},{\"target\":\"description\",\"source\":\"description\",\"condition\":{\"type\":\"text/javascript\",\"source\":\"!!object.description\"}},{\"target\":\"mail\",\"source\":\"mail\"},{\"target\":\"userPassword\",\"transform\":{\"type\":\"text/javascript\",\"source\":\"openidm.decrypt(source);\"},\"source\":\"password\",\"condition\":{\"type\":\"text/javascript\",\"source\":\"object.password != null\"}},{\"target\":\"telephoneNumber\",\"source\":\"telephoneNumber\",\"condition\":{\"type\":\"text/javascript\",\"source\":\"!!object.telephoneNumber\"}}],\"source\":\"managed/user\",\"onCreate\":{\"type\":\"text/javascript\",\"source\":\"target.dn = 'uid=' + source.userName + ',ou=People,dc=example,dc=com';\"},\"policies\":[{\"action\":\"UPDATE\",\"situation\":\"CONFIRMED\"},{\"action\":\"LINK\",\"situation\":\"FOUND\"},{\"action\":\"CREATE\",\"situation\":\"ABSENT\"},{\"action\":\"IGNORE\",\"situation\":\"AMBIGUOUS\"},{\"action\":\"IGNORE\",\"situation\":\"MISSING\"},{\"action\":\"DELETE\",\"situation\":\"SOURCE_MISSING\"},{\"action\":\"IGNORE\",\"situation\":\"UNQUALIFIED\"},{\"action\":\"IGNORE\",\"situation\":\"UNASSIGNED\"}],\"name\":\"managedUser_systemLdapAccounts\"},{\"target\":\"managed/user\",\"properties\":[],\"source\":\"system/ldap/__ALL__\",\"policies\":[{\"action\":\"ASYNC\",\"situation\":\"ABSENT\"},{\"action\":\"ASYNC\",\"situation\":\"ALL_GONE\"},{\"action\":\"ASYNC\",\"situation\":\"AMBIGUOUS\"},{\"action\":\"ASYNC\",\"situation\":\"CONFIRMED\"},{\"action\":\"ASYNC\",\"situation\":\"FOUND\"},{\"action\":\"ASYNC\",\"situation\":\"FOUND_ALREADY_LINKED\"},{\"action\":\"ASYNC\",\"situation\":\"LINK_ONLY\"},{\"action\":\"ASYNC\",\"situation\":\"MISSING\"},{\"action\":\"ASYNC\",\"situation\":\"SOURCE_IGNORED\"},{\"action\":\"ASYNC\",\"situation\":\"SOURCE_MISSING\"},{\"action\":\"ASYNC\",\"situation\":\"TARGET_IGNORED\"},{\"action\":\"ASYNC\",\"situation\":\"UNASSIGNED\"},{\"action\":\"ASYNC\",\"situation\":\"UNQUALIFIED\"}],\"name\":\"sourceLdap__ALL___managedUser\"}]}"
            ]
        );

        server.respondWith(
            "GET",
            "/openidm/endpoint/mappingDetails?mapping=sourceLdap__ALL___managedUser",
            [
                200,
                { },
                "{\"_id\":\"sync\",\"mappings\":[{\"target\":\"managed/user\",\"properties\":[{\"target\":\"displayName\",\"source\":\"cn\"},{\"target\":\"description\",\"source\":\"description\"},{\"target\":\"givenName\",\"source\":\"givenName\"},{\"target\":\"mail\",\"source\":\"mail\"},{\"target\":\"telephoneNumber\",\"source\":\"telephoneNumber\"},{\"target\":\"sn\",\"source\":\"sn\"},{\"target\":\"userName\",\"source\":\"uid\"}],\"source\":\"system/ldap/account\",\"policies\":[{\"action\":\"UPDATE\",\"situation\":\"CONFIRMED\"},{\"action\":\"UPDATE\",\"situation\":\"FOUND\"},{\"action\":\"CREATE\",\"situation\":\"ABSENT\"},{\"action\":\"EXCEPTION\",\"situation\":\"AMBIGUOUS\"},{\"action\":\"CREATE\",\"situation\":\"MISSING\"},{\"action\":\"DELETE\",\"situation\":\"SOURCE_MISSING\"},{\"action\":\"IGNORE\",\"situation\":\"UNQUALIFIED\"},{\"action\":\"IGNORE\",\"situation\":\"UNASSIGNED\"}],\"name\":\"systemLdapAccounts_managedUser\"},{\"target\":\"system/ldap/account\",\"links\":\"systemLdapAccounts_managedUser\",\"properties\":[{\"target\":\"givenName\",\"source\":\"givenName\"},{\"target\":\"sn\",\"source\":\"sn\"},{\"target\":\"cn\",\"source\":\"\",\"transform\":{\"type\":\"text/javascript\",\"source\":\"source.displayName || (source.givenName + ' ' + source.sn);\"}},{\"target\":\"uid\",\"source\":\"userName\"},{\"target\":\"description\",\"source\":\"description\",\"condition\":{\"type\":\"text/javascript\",\"source\":\"!!object.description\"}},{\"target\":\"mail\",\"source\":\"mail\"},{\"target\":\"userPassword\",\"transform\":{\"type\":\"text/javascript\",\"source\":\"openidm.decrypt(source);\"},\"source\":\"password\",\"condition\":{\"type\":\"text/javascript\",\"source\":\"object.password != null\"}},{\"target\":\"telephoneNumber\",\"source\":\"telephoneNumber\",\"condition\":{\"type\":\"text/javascript\",\"source\":\"!!object.telephoneNumber\"}}],\"source\":\"managed/user\",\"onCreate\":{\"type\":\"text/javascript\",\"source\":\"target.dn = 'uid=' + source.userName + ',ou=People,dc=example,dc=com';\"},\"policies\":[{\"action\":\"UPDATE\",\"situation\":\"CONFIRMED\"},{\"action\":\"LINK\",\"situation\":\"FOUND\"},{\"action\":\"CREATE\",\"situation\":\"ABSENT\"},{\"action\":\"IGNORE\",\"situation\":\"AMBIGUOUS\"},{\"action\":\"IGNORE\",\"situation\":\"MISSING\"},{\"action\":\"DELETE\",\"situation\":\"SOURCE_MISSING\"},{\"action\":\"IGNORE\",\"situation\":\"UNQUALIFIED\"},{\"action\":\"IGNORE\",\"situation\":\"UNASSIGNED\"}],\"name\":\"managedUser_systemLdapAccounts\"},{\"target\":\"managed/user\",\"properties\":[],\"source\":\"system/ldap/__ALL__\",\"policies\":[{\"action\":\"ASYNC\",\"situation\":\"ABSENT\"},{\"action\":\"ASYNC\",\"situation\":\"ALL_GONE\"},{\"action\":\"ASYNC\",\"situation\":\"AMBIGUOUS\"},{\"action\":\"ASYNC\",\"situation\":\"CONFIRMED\"},{\"action\":\"ASYNC\",\"situation\":\"FOUND\"},{\"action\":\"ASYNC\",\"situation\":\"FOUND_ALREADY_LINKED\"},{\"action\":\"ASYNC\",\"situation\":\"LINK_ONLY\"},{\"action\":\"ASYNC\",\"situation\":\"MISSING\"},{\"action\":\"ASYNC\",\"situation\":\"SOURCE_IGNORED\"},{\"action\":\"ASYNC\",\"situation\":\"SOURCE_MISSING\"},{\"action\":\"ASYNC\",\"situation\":\"TARGET_IGNORED\"},{\"action\":\"ASYNC\",\"situation\":\"UNASSIGNED\"},{\"action\":\"ASYNC\",\"situation\":\"UNQUALIFIED\"}],\"name\":\"sourceLdap__ALL___managedUser\"}]}"
            ]
        );
    };
});