    /**
    * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
    *
    * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

sciptedcrest2dj Sample - Scripted CREST to OpenDJ
=================================================

This sample demonstrates how to use Scripted CREST to connect to OpenDJs REST API. This sample enables the basic create,
read, update, and delete (CRUD) operations for groups and users. This sample requires a fresh install of OpenIDM and
OpenDJ directory server 3.5. It does not work with earlier versions of OpenDJ REST to LDAP.

Scripted CREST is meant to be used with CREST based REST API. The main difference between a CREST based REST API and a
generic REST API is CREST is a well known API for ForgeRock products and we can leverage the CREST resources in the
groovy scripts to create CREST requests.

For example, in CREST to do a create request you would do the following.

    CreateRequest request = Requests.newCreateRequest(objectClassInfo.resourceContainer, new JsonValue(user))
    Resource resource = connection.create(new RootContext(), request)

This will create a formatted create REST request and do the HTTP request based on that format. With a generic REST API,
the groovy script will need to manually build an HTTP request based on some defined REST API. This manual creation
would require setting the method, path, content and other HTTP request properties to make the REST call.

Setup OpenDJ
------------
1.  Extract OpenDJ to a folder called opendj.

2.  Run the following command to initalize OpenDJ.

        $ opendj/setup --cli --hostname localhost --ldapPort 1389 --rootUserDN "cn=Directory Manager" \
        --rootUserPassword password --adminConnectorPort 4444 --baseDN dc=com --acceptLicense --addBaseEntry \
        --no-prompt --quiet

3.  Setup Replication on OpenDJ.

        $ opendj/bin/dsconfig create-replication-server --hostname localhost --port 4444 --bindDN "cn=Directory Manager" \
        --bindPassword password --provider-name "Multimaster Synchronization" --set replication-port:8989 \
        --set replication-server-id:2 --type generic --trustAll --no-prompt

        $ opendj/bin/dsconfig create-replication-domain --hostname localhost --port 4444 --bindDN "cn=Directory Manager" \
        --bindPassword password --provider-name "Multimaster Synchronization" --domain-name example_com \
        --set base-dn:dc=example,dc=com --set replication-server:localhost:8989 --set server-id:3 --type generic \
        --trustAll --no-prompt

4.  Enable OpenDJ HTTP Connection Handler.

        $ opendj/bin/dsconfig set-connection-handler-prop --hostname localhost --port 4444 \
        --bindDN "cn=Directory Manager" --bindPassword password --handler-name "HTTP Connection Handler" \
        --set enabled:true --set listen-port:8090 --no-prompt --trustAll

5.  Enable OpenDJ HTTP logs.

        $ opendj/bin/dsconfig set-log-publisher-prop --hostname localhost --port 4444 --bindDN "cn=Directory Manager" \
        --bindPassword password --publisher-name "File-Based HTTP Access Logger" --set enabled:true --no-prompt \
        --trustAll

6.  Load the ldif supplied in the data folder into OpenDJ.

        $ opendj/bin/ldapmodify --bindDN "cn=Directory Manager" --bindPassword password --hostname localhost \
        --port 1389 --filename /path/to/openidm/samples/scriptedcrest2dj/data/ldap.ldif

7. Allow the IDM administrator user to read the changelog.

        $ opendj/bin/dsconfig \
         set-access-control-handler-prop \
         --hostname localhost \
         --port 4444 \
         --bindDN "cn=Directory Manager" \
         --bindPassword password \
         --add global-aci:"(target=\"ldap:///cn=changelog\")(targetattr=\"*||+\")\
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
         --add global-aci:"(targetcontrol=\"1.3.6.1.4.1.26027.1.5.4\")\
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
1.  Start OpenIDM with the configuration for sample scriptedcrest2dj.

        $ /path/to/openidm/startup.sh -p samples/scriptedcrest2dj

2. Check the connector configuration is correct by obtaining the status of the connector, over REST.
        $ curl \
             --header "X-OpenIDM-Username: openidm-admin" \
             --header "X-OpenIDM-Password: openidm-admin" \
             --request POST \
             "http://localhost:8080/openidm/system/scriptedcrest?_action=test"
            {
              "ok": true,
              "connectorRef": {
                "bundleVersion": "[2.0.0.0,3)",
                "bundleName": "org.openidentityplatform.openicf.connectors.groovy-connector",
                "connectorName": "org.forgerock.openicf.connectors.scriptedcrest.ScriptedCRESTConnector"
              },
              "objectTypes": [
                "groups",
                "users"
              ],
              "config": "config/provisioner.openicf/scriptedcrest",
              "enabled": true,
              "name": "scriptedcrest"
            }

3.  Create a Group in openDJ.

        $ curl --header "Content-Type: application/json" \
          --header "X-OpenIDM-Username: openidm-admin" \
          --header "X-OpenIDM-Password: openidm-admin" \
          --request POST \
          --data '{
          "_id" : "group1"
          }' \
          http://localhost:8080/openidm/system/scriptedcrest/groups?_action=create

        {
          "_rev": "0000000025c93bdd",
          "meta": {
            "created": "2014-10-08T03:55:46Z"
          },
          "displayName": "group1",
          "_id": "group1"
        }

4.  Create a User in openDJ.

        $ curl --header "Content-Type: application/json" \
          --header "X-OpenIDM-Username: openidm-admin" \
          --header "X-OpenIDM-Password: openidm-admin" \
          --request POST \
          --data '{
          "name": {
            "familyName": "Smith",
            "givenName" : "User"
          },
          "contactInformation": {
            "emailAddress" : "user@example.com",
            "telephoneNumber" : "444-444-4444"
          },
          "password" : "TestPassw0rd",
          "displayName" : "User.Smith",
          "_id" : "user"
          }' \
          http://localhost:8080/openidm/system/scriptedcrest/users?_action=create

        {
          "_rev": "00000000ecf58034",
          "contactInformation": {
            "emailAddress": "user@example.com",
            "telephoneNumber": "444-444-4444"
          },
          "meta": {
            "created": "2014-10-08T03:56:13Z"
          },
          "name": {
            "givenName": "User",
            "familyName": "Smith"
          },
          "_id": "user",
          "userName": "user@example.com",
          "displayName": "User.Smith"
        }

5. Reconcile the openDJ user to managed/user

        $ curl -k -H "Content-type: application/json" -u "openidm-admin:openidm-admin" -X POST \
        "https://localhost:8443/openidm/recon?_action=recon&mapping=systemCrestLdapUser_managedUser"

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

7.  Read the user data from openDJ.

        $ curl --header "Content-Type: application/json" \
          --header "X-OpenIDM-Username: openidm-admin" \
          --header "X-OpenIDM-Password: openidm-admin" \
          --request GET \
          http://localhost:8080/openidm/system/scriptedcrest/users/user

        {
          "_rev": "00000000981a9f3f",
          "contactInformation": {
            "emailAddress": "user@example.com",
            "telephoneNumber": "555-555-5555"
          },
          "_id": "user",
          "meta": {
            "created": "2014-10-08T03:56:13Z",
            "lastModified": "2014-10-08T03:56:50Z"
          },
          "name": {
            "givenName": "User",
            "familyName": "Smith"
          },
          "displayName": "User.Smith",
          "groups": [
            {
              "_id": "group1"
            }
          ],
          "userName": "user@example.com"
        }

8.  Update group in openDJ by adding a user.

        $ curl --header "Content-Type: application/json" \
          --header "X-OpenIDM-Username: openidm-admin" \
          --header "X-OpenIDM-Password: openidm-admin" \
          --header "If-Match: *" \
          --request PUT \
          --data '{
          "_id" : "group1",
          "members" : [{"_id" : "user"}]
          }' \
          http://localhost:8080/openidm/system/scriptedcrest/groups/group1

        {
          "_rev": "00000000c07c6d6a",
          "meta": {
            "created": "2014-10-08T03:55:46Z",
            "lastModified": "2014-10-08T03:57:24Z"
          },
          "displayName": "group1",
          "members": [
            {
              "displayName": "User.Smith",
              "_id": "user"
            }
          ],
          "_id": "group1"
        }



9.  Read the group from openDJ.

        $ curl --header "Content-Type: application/json" \
          --header "X-OpenIDM-Username: openidm-admin" \
          --header "X-OpenIDM-Password: openidm-admin" \
          --request GET \
          http://localhost:8080/openidm/system/scriptedcrest/groups/group1

        {
          "_rev": "00000000c07c6d6a",
          "displayName": "group1",
          "_id": "group1",
          "members": [
            {
              "displayName": "User.Smith",
              "_id": "user"
            }
          ],
          "meta": {
            "created": "2014-10-08T03:55:46Z",
            "lastModified": "2014-10-08T03:57:24Z"
          }
        }

10. Reconcile the openDJ group to managed/group

        $ curl -k -H "Content-type: application/json" -u "openidm-admin:openidm-admin" -X POST \
        "https://localhost:8443/openidm/recon?_action=recon&mapping=systemCrestLdapGroup_managedGroup"

        {"_id":"5d887e40-250a-4da7-af48-d2c9bc6c0b00","state":"ACTIVE"}


11. Read the managed/group to see that the openDJ group has been added to managed/group.

        $ curl --header "Content-Type: application/json" \
          --header "X-OpenIDM-Username: openidm-admin" \
          --header "X-OpenIDM-Password: openidm-admin" \
          --request GET \
          "http://localhost:8080/openidm/managed/group/group1"

          {
            "_id": "group1",
            "_rev": "1",
            "members": [
                {
                    "displayName": "User.Smith",
                    "_id": "user"
                }
            ],
            "displayName": "group1"
          }




12.  Delete the user.

        $ curl --header "Content-Type: application/json" \
          --header "X-OpenIDM-Username: openidm-admin" \
          --header "X-OpenIDM-Password: openidm-admin" \
          --request DELETE \
          http://localhost:8080/openidm/system/scriptedcrest/users/user

        {
          "_id": "user"
        }

13.  Delete the group.

        $ curl --header "Content-Type: application/json" \
          --header "X-OpenIDM-Username: openidm-admin" \
          --header "X-OpenIDM-Password: openidm-admin" \
          --request DELETE \
          http://localhost:8080/openidm/system/scriptedcrest/groups/group1

        {
          "_id": "group1"
        }

This shows the basic CRUD operations on users and groups using ScriptedCREST and the OpenDJ rest2ldap API. To read
more documentation on the Groovy ScriptedCREST connector and for help with customization please see the
[Groovy Connector Framework Docs](http://openicf.forgerock.org/connectors/groovy-connector/doc/groovy-connector-1.4.2.0-SNAPSHOT/index.html)
