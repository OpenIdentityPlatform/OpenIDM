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
==============================================================

This sample shows you historical account linking between OpenIDM's managed users and a local LDAP directory server, 
such as OpenDJ. OpenIDM is the source of records and drives all changes to downstream systems. Managed users in OpenIDM 
maintain a list of all the accounts that have been linked to, on the local LDAP server. This list is stored in the 
"historicalAccounts" field of the managed user entry, and contains all past and current linked accounts. Each account 
is represented as a relationship and contains information about the date linked/unlinked and whether the account is 
currently active.

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
*   conf/schedule-liveSync contains the configuration for the scheduled live sync job.

This sample includes the following scripts:

*   script/onLink-managedUser_systemLdapAccounts.js  Creates the relationship entry in the "historicalAccounts" list of
    the managed user for the newly linked target object.  Adds two relationship properties: "linkDate" specifying the 
    date the link was created, and "active" which is set to true to indicate that the target is currently linked.  
*   script/onUnlink-managedUser_systemLdapAccounts.js  Updates the relationship entry's properties representing the 
    linked target object with an "unlinkDate" specifying the date the target was unlinked, and sets "active" to false,
    indicating that the target is no longer linked.
*   script/check_account_state_change.js  On a live sync or recon event this script will check if the ldap account state
    has changed. If the state has changed it will update the historical account properties to indicate the new state 
    (enabled/disabled) and the date that the state was changed.  The date can only be approximated and is set to the
    time that the change was detected by the script.

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
          --ldifFile /path/to/openidm/samples/historicalaccountlinking/data/Example.ldif \
          --acceptLicense \
          --no-prompt

After you import the data, ou=People,dc=example,dc=com contains two user entries. Although all attributes to synchronize 
can be multi-valued in LDAP, this sample defines only mail as a multi-valued attribute in OpenIDM.

3.  Enable replication on the OpenDJ server.
    Although there is only one LDAP server in this example, you must enable replication so that the server has an 
    external change log. The change log is required for LiveSync between OpenDJ and OpenIDM.
    
        $ opendj/bin/dsconfig create-replication-server -h localhost -p 4444 \
          -D "cn=directory manager" -w password -X -n \
          --provider-name "Multimaster Synchronization" --set replication-port:8989 \
          --set replication-server-id:2 --type generic
      
        $ opendj/bin/dsconfig create-replication-domain -h localhost -p 4444 \
          -D "cn=directory manager" -w password -X -n \
          --provider-name "Multimaster Synchronization" --set base-dn:dc=example,dc=com \
          --set replication-server:localhost:8989 --set server-id:3 \
          --type generic --domain-name example_com

This sample includes the script script/ldapBackCorrelationQuery.js which correlates entries in the directory with 
identities in OpenIDM.

Run The Sample In OpenIDM
-------------------------

1.  Launch OpenIDM with the sample configuration as follows.

        $ /path/to/openidm/startup.sh -p samples/historicalaccountlinking

2.  Create a user in OpenIDM.

        $ curl -k \
        --header "Content-Type: application/json" \
        --header "X-OpenIDM-Username: openidm-admin" \
        --header "X-OpenIDM-Password: openidm-admin" \
        --request POST \
        --data '{
        "userName": "joe.smith",
        "givenName": "Joe",
        "sn" : "Smith",
        "password" : "TestPassw0rd",
        "displayName" : "Joe Smith",
        "description" : "Test User",
        "telephoneNumber" : "1234567890",        
        "mail" : "joe.smith@example.com",
        "_id" : "joesmith"
        }' \
        https://localhost:8443/openidm/managed/user?_action=create

3.  Request all identifiers in OpenDJ to verify that joe.smith0 was created by the implicit sync from OpenIDM to OpenDJ.

        $ curl -k \
        --header "X-OpenIDM-Username: openidm-admin" \
        --header "X-OpenIDM-Password: openidm-admin" \
        "https://localhost:8443/openidm/system/ldap/account?_queryId=query-all-ids&_prettyPrint=true"
        
        {
          "result" : [ {
            "dn" : "uid=jdoe,ou=People,dc=example,dc=com",
            "_id" : "uid=jdoe,ou=People,dc=example,dc=com"
          }, {
            "dn" : "uid=bjensen,ou=People,dc=example,dc=com",
            "_id" : "uid=bjensen,ou=People,dc=example,dc=com"
          }, {
            "dn" : "uid=joe.smith0,ou=People,dc=example,dc=com",
            "_id" : "uid=joe.smith0,ou=People,dc=example,dc=com"
          } ],
          "resultCount" : 3,
          "pagedResultsCookie" : null,
          "remainingPagedResults" : -1
        }

4.  Request all "historicalAccounts" of the newly created managed user. See the relationship that was just created 
and the linkDate that was set in the properties, along with "active" set to true.

        $ curl -k \
        --header "X-OpenIDM-Username: openidm-admin" \
        --header "X-OpenIDM-Password: openidm-admin" \
        "https://localhost:8443/openidm/managed/user/joesmith/historicalAccounts?_queryId=query-all"
        
        {
          "pagedResultsCookie": null,
          "remainingPagedResults": -1,
          "result": [
            {
              "_ref": "system/ldap/account/uid=joe.smith0,ou=People,dc=example,dc=com",
              "_refProperties": {
                "_id": "087cbd37-e086-42ba-bda6-65c5f88a5992",
                "_rev": "1",
                "active": true,
                "linkDate": "Wed Oct 07 2015 15:26:45 GMT-0700 (PDT)",
                "state": "enabled",
                "stateLastChanged": "Fri Nov 13 2015 09:55:46 GMT-0800 (PST)"
              }
            }
          ],
          "resultCount": 1,
          "totalPagedResults": -1,
          "totalPagedResultsPolicy": "NONE"
        }

5.  Now start the liveSync schedule so that changes in the ldap server are picked up.  This can be done by editing the
file samples/historicalaccountlinking/conf/schedule-liveSync.json to set "enabled" : true.

6.  Use the OpenDJ manage-account script to disable the account within OpenDJ.  This will result in liveSync picking up
the change and the "state" changing in the historical account properties of the managed user.

        $ ./bin/manage-account set-account-is-disabled --port 4444 --bindDN "cn=Directory Manager" \
        --bindPassword password --operationValue true --targetDN uid=joe.smith0,ou=people,dc=example,dc=com --trustAll

        Account Is Disabled:  true
        
7.  Wait a few seconds for liveSync to pick up the change then request all "historicalAccounts" of the managed user.
Note that the state of the account is now disabled and the date that the state changed has been recorded.

        $ curl -k \
        --header "X-OpenIDM-Username: openidm-admin" \
        --header "X-OpenIDM-Password: openidm-admin" \        
        "https://localhost:8443/openidm/managed/user/joesmith/historicalAccounts?_queryId=query-all"
        
        {
            "pagedResultsCookie": null,
            "remainingPagedResults": -1,
            "result": [
                {
                    "_ref": "system/ldap/account/uid=joe.smith0,ou=People,dc=example,dc=com",
                    "_refProperties": {
                        "_id": "893d5c33-e4aa-41fe-91c9-37098b442ce7",
                        "_rev": "2",
                        "active": true,
                        "linkDate": "Fri Nov 13 2015 09:55:46 GMT-0800 (PST)",
                        "state": "disabled",
                        "stateLastChanged": "Fri Nov 13 2015 10:01:30 GMT-0800 (PST)"
                    }
                }
            ],
            "resultCount": 1,
            "totalPagedResults": -1,
            "totalPagedResultsPolicy": "NONE"
        }

8.  Deactivate the managed user.

        $ curl -k \
        --header "X-OpenIDM-Username: openidm-admin" \
        --header "X-OpenIDM-Password: openidm-admin" \        
        --header "Content-type: application/json" \
        --request PATCH \
        --data '[ { "operation" : "replace", "field" : "accountStatus", "value" : "inactive" } ]' \
        'https://localhost:8443/openidm/managed/user/joesmith'
        {
          "_id": "joesmith",
          "_rev": "2",
          "accountStatus": "inactive",
          "address2": "",
          ...
          "userName": "joe.smith"
        }

9.  Request all "historicalAccounts" of the managed user. Note that the relationship has been updated - the
"unlinkDate was set in the properties and "active" was set to false.

        $ curl -k \
        --header "X-OpenIDM-Username: openidm-admin" \
        --header "X-OpenIDM-Password: openidm-admin" \        
        "https://localhost:8443/openidm/managed/user/joesmith/historicalAccounts?_queryId=query-all"
        
        {
            "pagedResultsCookie": null,
            "remainingPagedResults": -1,
            "result": [
                {
                    "_ref": "system/ldap/account/uid=joe.smith0,ou=People,dc=example,dc=com",
                    "_refProperties": {
                        "_id": "893d5c33-e4aa-41fe-91c9-37098b442ce7",
                        "_rev": "3",
                        "active": false,
                        "linkDate": "Fri Nov 13 2015 09:55:46 GMT-0800 (PST)",
                        "state": "disabled",
                        "stateLastChanged": "Fri Nov 13 2015 10:01:30 GMT-0800 (PST)",
                        "unlinkDate": "Fri Nov 13 2015 10:10:38 GMT-0800 (PST)"
                    }
                }
            ],
            "resultCount": 1,
            "totalPagedResults": -1,
            "totalPagedResultsPolicy": "NONE"
        }

10.  Activate the managed user.

        $ curl -k \
        --header "X-OpenIDM-Username: openidm-admin" \
        --header "X-OpenIDM-Password: openidm-admin" \
        --header "Content-type: application/json" \                
        --request PATCH \
        --data '[ { "operation" : "replace", "field" : "accountStatus", "value" : "active" } ]' \
        'https://localhost:8443/openidm/managed/user/joesmith'
        {
          "_id": "joesmith",
          "_rev": "2",
          "accountStatus": "active",
          "address2": "",
          ...
          "userName": "joe.smith"
        }

11.  Request all identifiers in OpenDJ. Verify that a new user joe.smith1 was created.

        $ curl -k \
        --header "X-OpenIDM-Username: openidm-admin" \
        --header "X-OpenIDM-Password: openidm-admin" \        
        "https://localhost:8443/openidm/system/ldap/account?_queryId=query-all-ids&_prettyPrint=true"
        {
          "result" : [ {
            "dn" : "uid=jdoe,ou=People,dc=example,dc=com",
            "_id" : "uid=jdoe,ou=People,dc=example,dc=com"
          }, {
            "dn" : "uid=bjensen,ou=People,dc=example,dc=com",
            "_id" : "uid=bjensen,ou=People,dc=example,dc=com"
          }, {
            "dn" : "uid=joe.smith0,ou=People,dc=example,dc=com",
            "_id" : "uid=joe.smith0,ou=People,dc=example,dc=com"
          }, {
            "dn" : "uid=joe.smith1,ou=People,dc=example,dc=com",
            "_id" : "uid=joe.smith1,ou=People,dc=example,dc=com"
          } ],
          "resultCount" : 3,
          "pagedResultsCookie" : null,
          "remainingPagedResults" : -1
        }

12.  Request all "historicalAccounts" of the managed user and note that a new relationship was created for the newly 
linked user in OpenDJ. The linkDate was set in the properties and "active" was set to true.

        $ curl -k \
        --header "X-OpenIDM-Username: openidm-admin" \
        --header "X-OpenIDM-Password: openidm-admin" \        
        "https://localhost:8443/openidm/managed/user/joesmith/historicalAccounts?_queryId=query-all"
        {
          "pagedResultsCookie": null,
          "remainingPagedResults": -1,
          "result": [
            {
              "_ref": "system/ldap/account/uid=joe.smith0,ou=People,dc=example,dc=com",
              "_refProperties": {
                "_id": "087cbd37-e086-42ba-bda6-65c5f88a5992",
                "_rev": "1",
                "active": true,
                "linkDate": "Wed Oct 07 2015 15:26:45 GMT-0700 (PDT)"
              }
            },
            {
              "_ref": "system/ldap/account/uid=joe.smith1,ou=People,dc=example,dc=com",
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


Repeating steps 8) through 12) will create more and more historical accounts for the user.
