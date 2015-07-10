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

Sample 2 - One-way LDAP -> Internal Repository
==============================================

The sample shows you reconciliation between the OpenIDM internal repository
and a local LDAP directory server, such as OpenDJ, with data flowing from
OpenDJ into the internal repository. No changes are pushed from OpenIDM
to OpenDJ.

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

Setup OpenDJ
------------

1.  Extract OpenDJ to a folder called opendj.

2.  Run the following command to initialize OpenDJ.

        $ opendj/setup --cli --hostname localhost --ldapPort 1389 --rootUserDN "cn=Directory Manager" \
        --rootUserPassword password --adminConnectorPort 4444 --baseDN dc=com --acceptLicense --addBaseEntry \
        --no-prompt --quiet

3.  Load the Example.ldif file supplied in the data folder into OpenDJ.

        $ opendj/bin/ldapmodify -a -c --bindDN "cn=Directory Manager" --bindPassword password --hostname localhost \
        --port 1389 --filename /path/to/openidm/samples/sample2/data/Example.ldif

After you import the data, ou=People,dc=example,dc=com contains two user entries. Although all attributes to synchronize
can be multi-valued in LDAP, this sample defines only mail as a multi-valued attribute in OpenIDM, in order to match
the definition in the first sample.

Run The Sample In OpenIDM
-------------------------

1.  Start the sample.

        $ ./startup.sh -p samples/sample2

2.  Run reconciliation once, creating users defined in OpenDJ in OpenIDM's internal repository.

        $ curl -k -H "Content-type: application/json" -u "openidm-admin:openidm-admin" -X POST \
        "https://localhost:8443/openidm/recon?_action=recon&mapping=systemLdapAccounts_managedUser"

    Alternatively, edit conf/scheduler-recon.json to enable scheduled
    reconciliation:

        "enabled" : true,

3.  Request all identifiers in OpenIDM's internal repository. Use this command to see the results after reconciliation.

        $ curl -k -u "openidm-admin:openidm-admin" "https://localhost:8443/openidm/managed/user?_queryId=query-all-ids&_prettyPrint=true"
        {
          "result" : [ {
            "_id" : "678eb8f7-5e3f-4bef-b001-bc0f01353dae",
            "_rev" : "0"
          }, {
            "_id" : "5e9534cb-c37e-48c0-9c1f-0782a7e2a9c0",
            "_rev" : "0"
          } ],
          "resultCount" : 2,
          "pagedResultsCookie" : null,
          "remainingPagedResults" : -1
        }

After you have created the managed/user entries you can use them to login to the UI at https://localhost:8443/
