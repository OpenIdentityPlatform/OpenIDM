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
     * Copyright 2014-2016 ForgeRock AS.
     */

sciptedrest2dj Sample - Scripted REST to OpenDJ
===============================================

This sample demonstrates how to use Scripted REST to connect to OpenDJs REST API. This sample enables the basic create,
read, update, and delete (CRUD) operations for groups and users. This sample requires a fresh install of OpenIDM and
OpenDJ directory server 3.5. It does not work with earlier versions of OpenDJ REST to LDAP.

Setup OpenDJ
------------
1. Extract OpenDJ to a folder called opendj.

2. Run the following command to initalize OpenDJ.

        $ opendj/setup --cli --hostname localhost --ldapPort 1389 --rootUserDN "cn=Directory Manager" \
        --rootUserPassword password --adminConnectorPort 4444 --baseDN dc=com --acceptLicense --addBaseEntry \
        --no-prompt --quiet

3. Setup Replication on OpenDJ.

        $ opendj/bin/dsconfig create-replication-server --hostname localhost --port 4444 --bindDN "cn=Directory Manager" \
        --bindPassword password --provider-name "Multimaster Synchronization" --set replication-port:8989 \
        --set replication-server-id:2 --type generic --trustAll --no-prompt

        $ opendj/bin/dsconfig create-replication-domain --hostname localhost --port 4444 --bindDN "cn=Directory Manager" \
        --bindPassword password --provider-name "Multimaster Synchronization" --domain-name example_com \
        --set base-dn:dc=example,dc=com --set replication-server:localhost:8989 --set server-id:3 --type generic \
        --trustAll --no-prompt

4. Enable OpenDJ HTTP Connection Handler.

        $ opendj/bin/dsconfig set-connection-handler-prop --hostname localhost --port 4444 \
        --bindDN "cn=Directory Manager" --bindPassword password --handler-name "HTTP Connection Handler" \
        --set enabled:true --set listen-port:8090 --no-prompt --trustAll

5. Enable OpenDJ HTTP logs.

        $ opendj/bin/dsconfig set-log-publisher-prop --hostname localhost --port 4444 --bindDN "cn=Directory Manager" \
        --bindPassword password --publisher-name "File-Based HTTP Access Logger" --set enabled:true --no-prompt \
        --trustAll

6. Load the ldif supplied in the data folder into OpenDJ.

        $ opendj/bin/ldapmodify --bindDN "cn=Directory Manager" --bindPassword password --hostname localhost \
        --port 1389 --filename /path/to/openidm/samples/scriptedrest2dj/data/ldap.ldif

7. Allow the IDM administrator user to read the changelog.

        $ opendj/bin/dsconfig \
          set-access-control-handler-prop \
          --hostname localhost \
          --port 4444 \
          --bindDN "cn=Directory Manager" \
          --bindPassword password \
          --add global-aci:"(target=\"ldap:///cn=changelog\")(targetattr=\"*||+\") \
          (version 3.0; acl \"IDM can access cn=changelog\"; \
          allow (read,search,compare) \
          userdn=\"ldap:///uid=idm,ou=Administrators,dc=example,dc=com\";)" \
          --trustAll \
          --no-prompt

        $ opendj/bin/dsconfig \
          set-access-control-handler-prop \
          --hostname localhost \
          --port 4444 \
          --bindDN "cn=Directory Manager" \
          --bindPassword password \
          --add global-aci:"(targetcontrol=\"1.3.6.1.4.1.26027.1.5.4\") \
          (version 3.0; acl \"IDM changelog control access\"; \
          allow (read) \
          userdn=\"ldap:///uid=idm,ou=Administrators,dc=example,dc=com\";)" \
          --trustAll \
          --no-prompt

8. Replace the default OpenDJ REST to LDAP configuration with the configuration for this sample.

        $ cp /path/to/openidm/samples/scriptedrest2dj/data/example-v1.json opendj/config/rest2ldap/endpoints/api/

9. Restart OpenDJ.

        $ opendj/bin/stop-ds --restart

OpenIDM Instructions
--------------------

1. Start OpenIDM with the configuration for sample scriptedrest2dj.

        $ /path/to/openidm/startup.sh -p samples/scriptedrest2dj

2. Check the connector configuration is correct by obtaining the status of the connector, over REST.
        $ curl \
          --header "X-OpenIDM-Username: openidm-admin" \
          --header "X-OpenIDM-Password: openidm-admin" \
          --request POST \
          "http://localhost:8080/openidm/system/scriptedrest?_action=test"

            {
              "name": "scriptedrest",
              "enabled": true,
              "config": "config/provisioner.openicf/scriptedrest",
              "objectTypes": [
                "__ALL__",
                "account",
                "group"
              ],
              "connectorRef": {
                "bundleName": "org.openidentityplatform.openicf.connectors.groovy-connector",
                "connectorName": "org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConnector",
                "bundleVersion": "[2.0.0.0,3)"
              },
              "displayName": "Scripted REST Connector",
              "ok": true
            }

3. Create a Group in openDJ.

        $ curl --header "Content-Type: application/json" \
          --header "X-OpenIDM-Username: openidm-admin" \
          --header "X-OpenIDM-Password: openidm-admin" \
          --request POST \
          --data '{
          "cn" : "group1"
          }' \
          http://localhost:8080/openidm/system/scriptedrest/group?_action=create

        {
          "_id": "group1",
          "cn": "group1",
          "members": null,
          "lastModified": null,
          "created": "2014-09-24T17:34:27Z",
          "displayName": "group1"
        }

4. Create a User in openDJ.

        $ curl --header "Content-Type: application/json" \
          --header "X-OpenIDM-Username: openidm-admin" \
          --header "X-OpenIDM-Password: openidm-admin" \
          --request POST \
          --data '{
          "givenName" : "User",
          "familyName" : "Smith",
          "emailAddress" : "user@example.com",
          "telephoneNumber" : "444-444-4444",
          "password" : "TestPassw0rd",
          "displayName" : "User.Smith",
          "uid" : "user"
          }' \
          http://localhost:8080/openidm/system/scriptedrest/account?_action=create

        {
          "_id": "user",
          "displayName": "User.Smith",
          "uid": "user",
          "groups": null,
          "familyName": "Smith",
          "emailAddress": "user@example.com",
          "givenName": "User",
          "created": "2014-09-24T17:35:46Z",
          "telephoneNumber": "444-444-4444"
        }

5. Reconcile the openDJ user to managed/user

        $ curl -k -H "Content-type: application/json" -u "openidm-admin:openidm-admin" -X POST \
        "https://localhost:8443/openidm/recon?_action=recon&mapping=systemRestLdapUser_managedUser"

        {"_id":"a72d6c6f-c019-4598-a199-52d9dcca9dae","state":"ACTIVE"}

6. Update managed/user telephone number which will sync to openDJ and make the change there.

        $ curl --header "Content-Type: application/json" \
          --header "X-OpenIDM-Username: openidm-admin" \
          --header "X-OpenIDM-Password: openidm-admin" \
          --request PATCH \
          --data '[
            {
              "operation" : "replace",
              "field" : "telephoneNumber",
              "value" : "555-555-5555"
            }
         ]' \
          "http://localhost:8080/openidm/managed/user/user"

        {
            "userName":"user",
            "mail":"user@example.com",
            "displayName":"User.Smith",
            "telephoneNumber":"555-555-5555",
            "givenName":"User",
            "sn":"Smith",
            "_id":"user",
            "_rev":"9",
            "accountStatus":"active",
            "effectiveRoles":[],
            "effectiveAssignments":[]
        }

7. Read the user data from openDJ.

        $ curl --header "Content-Type: application/json" \
          --header "X-OpenIDM-Username: openidm-admin" \
          --header "X-OpenIDM-Password: openidm-admin" \
          --request GET \
          http://localhost:8080/openidm/system/scriptedrest/account/user

        {
          "_id": "user",
          "displayName": "User.Smith",
          "uid": "user",
          "groups": null,
          "familyName": "Smith",
          "emailAddress": "user@example.com",
          "givenName": "User",
          "created": "2014-09-24T17:31:04Z",
          "telephoneNumber": "555-555-5555"
        }

8. Update group in openDJ by adding a user.

        $ curl --header "Content-Type: application/json" \
          --header "X-OpenIDM-Username: openidm-admin" \
          --header "X-OpenIDM-Password: openidm-admin" \
          --header "If-Match: *" \
          --request PUT \
          --data '{
          "_id" : "group1",
          "members" : [{"_id" : "user"}]
          }' \
          http://localhost:8080/openidm/system/scriptedrest/group/group1

        {
          "_id": "group1",
          "cn": "group1",
          "members": [
            {
              "displayName": "User.Smith",
              "_id": "user"
            }
          ],
          "lastModified": "2014-09-24T17:31:42Z",
          "created": "2014-09-24T17:27:37Z",
          "displayName": "group1"
        }

9. Read the group from openDJ.

        $ curl --header "Content-Type: application/json" \
          --header "X-OpenIDM-Username: openidm-admin" \
          --header "X-OpenIDM-Password: openidm-admin" \
          --request GET \
          http://localhost:8080/openidm/system/scriptedrest/group/group1

        {
          "_id": "group1",
          "cn": "group1",
          "members": [
            {
              "displayName": "User.Smith",
              "_id": "user"
            }
          ],
          "lastModified": "2014-09-24T17:31:42Z",
          "created": "2014-09-24T17:27:37Z",
          "displayName": "group1"
        }

10. Reconcile the openDJ group to managed/group

        $ curl -k -H "Content-type: application/json" -u "openidm-admin:openidm-admin" -X POST \
        "https://localhost:8443/openidm/recon?_action=recon&mapping=systemRestLdapGroup_managedGroup"

        {"_id":"5d887e40-250a-4da7-af48-d2c9bc6c0b00","state":"ACTIVE"}

11. Read the managed/group to see that the openDJ group has been added to managed/group.

        $ curl --header "Content-Type: application/json" \
          --header "X-OpenIDM-Username: openidm-admin" \
          --header "X-OpenIDM-Password: openidm-admin" \
          --request GET \
          "http://localhost:8080/openidm/managed/group/group1"

          {
            "_id": "group1",
            "_rev": "3",
            "members": [
                {
                    "displayName": "User.Smith",
                    "_id": "user"
                }
            ],
            "displayName": "group1"
          }



12. Delete the user.

        $ curl --header "Content-Type: application/json" \
          --header "X-OpenIDM-Username: openidm-admin" \
          --header "X-OpenIDM-Password: openidm-admin" \
          --request DELETE \
          http://localhost:8080/openidm/system/scriptedrest/account/user

        {
          "_id": "user",
          "givenName": "User",
          "telephoneNumber": "555-555-5555",
          "emailAddress": null,
          "created": "2014-09-24T17:31:42Z",
          "familyName": "Smith",
          "groups": [
            {
              "_id": "group1"
            }
          ],
          "uid": "user",
          "displayName": "User.Smith"
        }

13. Delete the group.

        $ curl --header "Content-Type: application/json" \
          --header "X-OpenIDM-Username: openidm-admin" \
          --header "X-OpenIDM-Password: openidm-admin" \
          --request DELETE \
          http://localhost:8080/openidm/system/scriptedrest/group/group1

        {
          "_id": "group1",
          "displayName": "group1",
          "cn": "group1",
          "members": null,
          "lastModified": "2014-09-24T17:31:42Z",
          "created": "2014-09-24T17:27:37Z"
        }

This shows the basic CRUD operations on users and groups using ScriptedREST and the OpenDJ rest2ldap API. To read
more documentation on the groovy scriptedrest connector and for help with customization please see the
[Groovy Connector Framework Docs](http://openicf.forgerock.org/connectors/groovy-connector/doc/groovy-connector-1.4.2.0-SNAPSHOT/index.html)
