    /**
     * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
     *
     * Copyright 2014 ForgeRock AS. All rights reserved.
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

Sample 2b - Bi-directional LDAP <-> Internal Repository
=======================================================

The sample shows you reconciliation between the OpenIDM internal repository
and a local LDAP directory server, such as OpenDJ, with data flowing from
OpenDJ into the internal repository, and from the internal repository into
OpenDJ.

The sample includes these configuration files.

*   conf/provisioner.openicf-ldap.json configures the LDAP connector.
    By default, the LDAP connector uses the following parameters:
    "host" : "localhost",
    "port" : 1389,
    "principal" : "cn=Directory Manager",
    "credentials" : "password",
    "baseContextsToSynchronize" : [ "ou=People,dc=example,dc=com" ],
    "attributesToSynchronize" : [ "uid", "sn", "cn", "givenName", "mail", "description", "telephoneNumber" ],
*   conf/scheduler-recon.json configures a scheduler you can use to run
    reconciliation periodically.
*   conf/sync.json describes how identities in the directory server map to
    identities in the internal repository target.
*   conf/authentication.json specifies an additional "authModule" entry for "PASSTHROUGH"
    authentication. This is used to allow the managed/user entries created from LDAP to
    login with the credentials which remain in LDAP.

This sample includes the script script/ldapBackCorrelationQuery.js which
correlates entries in the directory with identities in OpenIDM.

Setup OpenDJ
------------

1.  Extract OpenDJ to a folder called opendj.

2.  Run the following command to initialize OpenDJ and import the LDIF data for the sample.

        $ opendj/setup --cli \
          --hostname localhost \
          --ldapPort 1389 \
          --rootUserDN "cn=Directory Manager" \
          --rootUserPassword password \
          --adminConnectorPort 4444 \
          --baseDN dc=com \
          --ldifFile /path/to/openidm/samples/sample2b/data/Example.ldif \
          --acceptLicense \
          --no-prompt

After you import the data, ou=People,dc=example,dc=com contains two user entries. Although
all attributes to synchronize can be multi-valued in LDAP, this sample defines only mail as a multi-valued attribute
in OpenIDM.

Run The Sample In OpenIDM
-------------------------

1.  Launch OpenIDM with the sample configuration as follows.

        $ /path/to/openidm/startup.sh -p samples/sample2b

2.  Run reconciliation once, creating users defined in OpenDJ in OpenIDM's internal repository.

        $ curl -k -H "Content-type: application/json" -u "openidm-admin:openidm-admin" -X POST \
        "https://localhost:8443/openidm/recon?_action=recon&mapping=systemLdapAccounts_managedUser"

3.  Create or update a user in OpenIDM.

        $ curl --header "Content-Type: application/json" \
        --header "X-OpenIDM-Username: openidm-admin" \
        --header "X-OpenIDM-Password: openidm-admin" \
        --request POST \
        --data '{
        "userName": "user.smith",
        "givenName": "User",
        "sn" : "Smith",
        "password" : "TestPassw0rd",
        "displayName" : "User Smith",
        "mail" : "user.smith@example.com",
        "_id" : "user"
        }' \
        http://localhost:8080/openidm/managed/user?_action=create

4.  Run reconciliation, creating users defined in OpenIDM in OpenDJ.

        $ curl -k -H "Content-type: application/json" -u "openidm-admin:openidm-admin" -X POST \
        "https://localhost:8443/openidm/recon?_action=recon&mapping=managedUser_systemLdapAccounts"

5.  Request all identifiers in OpenDJ. Use it to see the results after reconciliation.

        $ curl -k -u "openidm-admin:openidm-admin" "https://localhost:8443/openidm/system/ldap/account?_queryId=query-all-ids&_prettyPrint=true"
        {
          "result" : [ {
            "dn" : "uid=jdoe,ou=People,dc=example,dc=com",
            "_id" : "uid=jdoe,ou=People,dc=example,dc=com"
          }, {
            "dn" : "uid=bjensen,ou=People,dc=example,dc=com",
            "_id" : "uid=bjensen,ou=People,dc=example,dc=com"
          }, {
            "dn" : "uid=user.smith,ou=People,dc=example,dc=com",
            "_id" : "uid=user.smith,ou=People,dc=example,dc=com"
          } ],
          "resultCount" : 3,
          "pagedResultsCookie" : null,
          "remainingPagedResults" : -1
        }

6.  Request all identifiers in OpenIDM's internal repository. Use this command to see the results after reconciliation.

        $ curl -k -u "openidm-admin:openidm-admin" "https://localhost:8443/openidm/managed/user?_queryId=query-all-ids&_prettyPrint=true"
        {
          "result" : [ {
            "_id" : "455b1cd5-ae51-41c0-ade9-0dfcc6dee265",
            "_rev" : "0"
          }, {
            "_id" : "bdd64b1a-015b-4a00-a979-1d699bca2f6b",
            "_rev" : "0"
          }, {
            "_id" : "user",
            "_rev" : "0"
          } ],
          "resultCount" : 3,
          "pagedResultsCookie" : null,
          "remainingPagedResults" : -1
        }

Now you can login to the UI with the credentials from any of the DJ users. They 
can update their profile or their password; the changes will be synced back to LDAP.
