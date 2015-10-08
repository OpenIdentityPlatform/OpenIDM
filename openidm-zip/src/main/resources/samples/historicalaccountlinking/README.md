/*
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
 * Copyright 2015 ForgeRock AS.
 */

Sample Historical Account Linking LDAP <-> Internal Repository
=======================================================

This sample shows you historical account linking between the OpenIDM's managed users and a local LDAP directory server, 
such as OpenDJ.  Managed users in OpenIDM will maintain a list of historically linked accounts on the local LDAP server. 
This list will be stored in the "historicalAccounts" field of a managed user and will contain all past and current 
accounts. Each account will be represented as a relationship and will contain information about the date linked/unlinked 
and whether the account is currently active.

The sample includes the following customized configuration files:

*   conf/provisioner.openicf-ldap.json: configures the LDAP connector. The LDAP connector uses the following parameters:
    "host" : "localhost",
    "port" : 1389,
    "principal" : "cn=Directory Manager",
    "credentials" : "password",
    "baseContextsToSynchronize" : [ "ou=People,dc=example,dc=com" ],
    "attributesToSynchronize" : [ "uid", "sn", "cn", "givenName", "mail", "description", "telephoneNumber" ],
*   conf/sync.json describes how accounts in the directory server map to managed users in OpenIDM.
*   conf/managed.json contains the updated schema for managed users which includes the "historicalAccounts" property.

This sample includes the following scripts:

*   script/onLink-managedUser_systemLdapAccounts.js  Creates the relationship entry in the "historicalAccounts" list of
    the managed user for the newly linked target object.  Adds two relationship properties: "linkDate" specifying the 
    date the link was created, and "active" which is set to true to indicate that the target is currently linked.  
*   script/onUnlink-managedUser_systemLdapAccounts.js  Updates the relationship entry's properties representing the 
    linked target object with a "unlinkDate" specifying the date the target was unlinked, and sets "active" to false
    indicating that the target is no longer linked.
*   script/onUnlink-systemLdapAccounts_managedUser.js  Updates the relationship entry's properties representing the 
    linked source object with a "unlinkDate" specifying the date the source was unlinked, and sets "active" to false
    indicating that the source is no longer linked. 

Setup OpenDJ
------------

1.  Extract OpenDJ to a folder called opendj.

2.  Run the following command to initialize OpenDJ.

        $ opendj/setup --cli --hostname localhost --ldapPort 1389 --rootUserDN "cn=Directory Manager" \
        --rootUserPassword password --adminConnectorPort 4444 --baseDN dc=com --acceptLicense --addBaseEntry \
        --no-prompt --quiet

3.  Load the Example.ldif file supplied in the data folder into OpenDJ.

        $ opendj/bin/ldapmodify -a -c --bindDN "cn=Directory Manager" --bindPassword password --hostname localhost \
        --port 1389 --filename /path/to/openidm/samples/sample2b/data/Example.ldif

After you import the data, ou=People,dc=example,dc=com contains two user entries. Although all attributes to synchronize 
can be multi-valued in LDAP, this sample defines only mail as a multi-valued attribute in OpenIDM.

This sample includes the script script/ldapBackCorrelationQuery.js which correlates entries in the directory with 
identities in OpenIDM.

Run The Sample In OpenIDM
-------------------------

1.  Launch OpenIDM with the sample configuration as follows.

        $ /path/to/openidm/startup.sh -p samples/historicalaccountlinking

2.  Create a user in OpenIDM.

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

3.  Request all identifiers in OpenDJ. Verifying that user.smith0 was created.

        $ curl -k -u "openidm-admin:openidm-admin" \
        "https://localhost:8443/openidm/system/ldap/account?_queryId=query-all-ids&_prettyPrint=true"
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

4.  Request all "historicalAccounts" of the newly created managed user and see the relationship that was just created 
and the linkDate was set in the properties, along with "active" set to true.

        $ curl -k -u "openidm-admin:openidm-admin" \
        "http://localhost:8080/openidm/managed/user/user/historicalAccounts?_queryId=query-all"
        {
          "pagedResultsCookie": null,
          "remainingPagedResults": -1,
          "result": [
            {
              "_ref": "system/ldap/account/uid=user.smith0,ou=People,dc=example,dc=com",
              "_refProperties": {
                "_id": "087cbd37-e086-42ba-bda6-65c5f88a5992",
                "_rev": "1",
                "active": true,
                "linkDate": "Wed Oct 07 2015 15:26:45 GMT-0700 (PDT)"
              }
            }
          ],
          "resultCount": 1,
          "totalPagedResults": -1,
          "totalPagedResultsPolicy": "NONE"
        }

5.  Deactivate the managed user.

        $ curl -k -u "openidm-admin:openidm-admin" --header "If-Match: *" --request PATCH \
        --data '[ { "operation" : "replace", "field" : "accountStatus", "value" : "inactive" } ]' \
        'https://localhost:8443/openidm/managed/user/user'
        {
          "_id": "user",
          "_rev": "2",
          "accountStatus": "inactive",
          "address2": "",
          ...
          "userName": "user.smith"
        }

6.  Request all "historicalAccounts" of the newly created managed user and see the relationship has been updated and the
"unlinkDate was set in the properties, along with "active" set to false.

        $ curl -k -u "openidm-admin:openidm-admin" \
        "http://localhost:8080/openidm/managed/user/user/historicalAccounts?_queryId=query-all"
        {
          "pagedResultsCookie": null,
          "remainingPagedResults": -1,
          "result": [
            {
              "_ref": "system/ldap/account/uid=user.smith0,ou=People,dc=example,dc=com",
              "_refProperties": {
                "_id": "087cbd37-e086-42ba-bda6-65c5f88a5992",
                "_rev": "3",
                "active": false,
                "linkDate": "Wed Oct 07 2015 15:26:45 GMT-0700 (PDT)",
                "unlinkDate": "Wed Oct 07 2015 15:36:55 GMT-0700 (PDT)"
              }
            }
          ],
          "resultCount": 1,
          "totalPagedResults": -1,
          "totalPagedResultsPolicy": "NONE"
        }

7.  Activate the managed user.

        $ curl -k -u "openidm-admin:openidm-admin" --header "If-Match: *" --request PATCH \
        --data '[ { "operation" : "replace", "field" : "accountStatus", "value" : "active" } ]' \
        'https://localhost:8443/openidm/managed/user/user'
        {
          "_id": "user",
          "_rev": "2",
          "accountStatus": "active",
          "address2": "",
          ...
          "userName": "user.smith"
        }

8.  Request all identifiers in OpenDJ. Verifying that a new user user.smith1 was created.

        $ curl -k -u "openidm-admin:openidm-admin" \
        "https://localhost:8443/openidm/system/ldap/account?_queryId=query-all-ids&_prettyPrint=true"
        {
          "result" : [ {
            "dn" : "uid=jdoe,ou=People,dc=example,dc=com",
            "_id" : "uid=jdoe,ou=People,dc=example,dc=com"
          }, {
            "dn" : "uid=bjensen,ou=People,dc=example,dc=com",
            "_id" : "uid=bjensen,ou=People,dc=example,dc=com"
          }, {
            "dn" : "uid=user.smith0,ou=People,dc=example,dc=com",
            "_id" : "uid=user.smith0,ou=People,dc=example,dc=com"
          }, {
            "dn" : "uid=user.smith1,ou=People,dc=example,dc=com",
            "_id" : "uid=user.smith1,ou=People,dc=example,dc=com"
          } ],
          "resultCount" : 3,
          "pagedResultsCookie" : null,
          "remainingPagedResults" : -1
        }

9.  Request all "historicalAccounts" of the newly created managed user and see a new relationship was just created for
the newly linked user in OpenDJ and the linkDate was set in the properties, along with "active" set to true.

        $ curl -k -u "openidm-admin:openidm-admin" \
        "http://localhost:8080/openidm/managed/user/user/historicalAccounts?_queryId=query-all"
        {
          "pagedResultsCookie": null,
          "remainingPagedResults": -1,
          "result": [
            {
              "_ref": "system/ldap/account/uid=user.smith0,ou=People,dc=example,dc=com",
              "_refProperties": {
                "_id": "087cbd37-e086-42ba-bda6-65c5f88a5992",
                "_rev": "1",
                "active": true,
                "linkDate": "Wed Oct 07 2015 15:26:45 GMT-0700 (PDT)"
              }
            },
            {
              "_ref": "system/ldap/account/uid=user.smith1,ou=People,dc=example,dc=com",
              "_refProperties": {
                "_id": "b79e838a-c79d-40fd-8393-87650ddf1465",
                "_rev": "1",
                "active": true,
                "linkDate": "Wed Oct 07 2015 15:42:52 GMT-0700 (PDT)"
              }
            }
          ],
          "resultCount": 1,
          "totalPagedResults": -1,
          "totalPagedResultsPolicy": "NONE"


Repeating steps 5) through 9) will create more and more historical accounts for the user.
