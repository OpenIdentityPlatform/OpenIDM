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

Sample 2d - Synchronizing LDAP Groups
=====================================

This sample is the same as sample 2c except that it focuses on synchronizing 
LDAP groups.

Unlike sample2c, this sample sync.json configuration contains three mappings - 
two for user objects and one for group objects. The number of attributes mapped 
is limited.

New groups are created from LDAP by running a reconciliation against the LDAP 
groups. Reconciliation synchronizes the cn and dn of the groups as well as the 
description and the uniqueMember attribute which contains a list of all the 
member DNs of this group.

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
          --ldifFile /path/to/openidm/samples/sample2d/data/Example.ldif \
          --acceptLicense \
          --no-prompt

Run The Sample In OpenIDM
-------------------------

1.  Launch OpenIDM with the sample configuration as follows.

        $ /path/to/openidm/startup.sh -p samples/sample2d

2.  Run reconciliation once, creating users defined in OpenDJ in OpenIDM's internal repository.

        $ curl -k -H "Content-type: application/json" -u "openidm-admin:openidm-admin" -X POST \
        "https://localhost:8443/openidm/recon?_action=recon&mapping=systemLdapAccounts_managedUser"

3.  Run reconciliation once, creating groups defined in OpenDJ in OpenIDM's internal repository.

        $ curl -k -H "Content-type: application/json" -u "openidm-admin:openidm-admin" -X POST \
        "https://localhost:8443/openidm/recon?_action=recon&mapping=systemLdapGroups_managedGroup"

4.  Request all user identifiers in OpenIDM's internal repository. Use this command to see the results after reconciliation.

        $ curl -k -u "openidm-admin:openidm-admin" "https://localhost:8443/openidm/managed/user?_queryId=query-all-ids&_prettyPrint=true"
        {
          "result" : [ {
            "_id" : "455b1cd5-ae51-41c0-ade9-0dfcc6dee265",
            "_rev" : "0"
          }, {
            "_id" : "bdd64b1a-015b-4a00-a979-1d699bca2f6b",
            "_rev" : "0"
          },
          "resultCount" : 2,
          "pagedResultsCookie" : null,
          "remainingPagedResults" : -1
        }

5.  Request all group identifiers in OpenIDM's internal repository. Use this command to see the results after reconciliation.

        $ curl -k -u "openidm-admin:openidm-admin" "https://localhost:8443/openidm/managed/group?_queryId=query-all-ids&_prettyPrint=true"
        {
          "result" : [ {
            "_id" : "037e3cc0-91b8-47b0-b925-7cfe4b0c7637",
            "_rev" : "0"
          }, {
            "_id" : "d4f047bc-0405-4cbe-9867-c0c793477b23",
            "_rev" : "0"
          } ],
          "resultCount" : 2,
          "pagedResultsCookie" : null,
          "remainingPagedResults" : -1
        }

Now you can login to the UI with the credentials from any of the DJ users. They
can update their profile or their password; the changes will be synced back to LDAP.