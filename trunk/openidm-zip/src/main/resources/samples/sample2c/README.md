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

Sample 2c - Synchronizing LDAP Group Membership
===============================================

This sample is the same as sample 2b except that it focuses on one special 
attribute, ldapGroups, which is used to synchronize LDAP group membership.

Unlike sample 2, this sample sync.json configuration contains two mappings from 
OpenDJ to OpenIDM and back. The number of attributes mapped are limited. The 
sample contains a schedule configuration which can be used to schedule 
reconciliation.

New users are created from LDAP and existing users are updated and back-linked 
from OpenIDM to OpenDJ. Changes on OpenIDM are now pushed into the LDAP server.

In addition to sample 2b this sample synchronizes LDAP group membership:

Setup OpenDJ
------------

1.  Extract OpenDJ to a folder called opendj.

2.  Run the following command to initialize OpenDJ.

        $ opendj/setup --cli --hostname localhost --ldapPort 1389 --rootUserDN "cn=Directory Manager" \
        --rootUserPassword password --adminConnectorPort 4444 --baseDN dc=com --acceptLicense --addBaseEntry \
        --no-prompt --quiet

3.  Load the Example.ldif file supplied in the data folder into OpenDJ.

        $ opendj/bin/ldapmodify -a -c --bindDN "cn=Directory Manager" --bindPassword password --hostname localhost \
        --port 1389 --filename /path/to/openidm/samples/sample2c/data/Example.ldif

Run The Sample In OpenIDM
-------------------------

1.  Launch OpenIDM with the sample configuration as follows.

        $ /path/to/openidm/startup.sh -p samples/sample2c

2.  Run reconciliation once, creating users defined in OpenDJ in OpenIDM's internal repository.

        $ curl -k -H "Content-type: application/json" -u "openidm-admin:openidm-admin" -X POST \
        "https://localhost:8443/openidm/recon?_action=recon&mapping=systemLdapAccounts_managedUser"

3.  Request all identifiers in OpenIDM's internal repository. Use this command to see the results after reconciliation.

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

4.  Request a user in OpenIDM's internal repository.

        $ curl -k -u "openidm-admin:openidm-admin" "https://localhost:8443/openidm/managed/user/455b1cd5-ae51-41c0-ade9-0dfcc6dee265?_prettyPrint=true"
        {
          "mail" : "bjensen@example.com",
          "sn" : "Jensen",
          "passwordAttempts" : "0",
          "lastPasswordAttempt" : "Wed Nov 19 2014 15:12:14 GMT-0800 (PST)",
          "address2" : "",
          "givenName" : "Barbara",
          "effectiveRoles" : [ "openidm-authorized" ],
          "country" : "",
          "city" : "",
          "lastPasswordSet" : "",
          "postalCode" : "",
          "_id" : "9e64ca24-fac8-40e3-8a1c-52ba14ae6017",
          "_rev" : "1",
          "description" : "Created for OpenIDM",
          "accountStatus" : "active",
          "telephoneNumber" : "1-360-229-7105",
          "roles" : [ "openidm-authorized" ],
          "effectiveAssignments" : { },
          "ldapGroups" : [ "cn=openidm2,ou=Groups,dc=example,dc=com" ],
          "postalAddress" : "",
          "stateProvince" : "",
          "userName" : "bjensen",
          "displayName" : "Barbara Jensen"
        }

    You will see the user is in group "openidm2".

        "ldapGroups" : [ "cn=openidm2,ou=Groups,dc=example,dc=com" ],

Now you can login to the UI with the credentials from any of the DJ users. They
can update their profile or their password; the changes will be synced back to LDAP.