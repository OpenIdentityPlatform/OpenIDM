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
 * Copyright 2016 ForgeRock AS.
 */

Roles Sample: Adding Temporal Constraints to Roles
==================================================
This sample provides all the information you need to manage temporal constraints on managed Roles in OpenIDM, via
either REST or via the Administrative UI.

Let's take a concrete example and continue with our Employee and Contractor roles example that was provided in both
_crudops_ and _provroles_ samples. This example will also build on _sample2b_ to view the temporal constraint on the
entries that have the contractor role.

For this sample, we will add a temporal constraint to one of the contractor users from the previous sample and view how
the role is automatically granted and removed based on the temporal constraint duration.

Prerequisites: we assume that you are familiar with _sample2b_ and already have installed OpenDJ according to the
instructions and configuration provided in that sample. We also assume here that you have reconciled the entries as
explained in that sample's section 2 & 4, but for this current sample. This sample builds on the completed provrole
sample. There should be two users, two roles (employee and contractor), and two assignments (employee and contractor).

Note: the Example.ldif provided with this sample should be loaded to OpenDJ, if that wasn't done previously.

        $ opendj/bin/ldapmodify -a -c --bindDN "cn=Directory Manager" --bindPassword password --hostname localhost --port 1389 --filename openidm/samples/roles/temporalConstraints/data/Example.ldif

To run this sample, start OpenIDM the following command:

        $ ./startup.sh -p samples/roles/temporalConstraints

Then create the Employee and Contractor roles, as you learned in the previous (crudops) sample. You can use the Admin
UI or the REST interface to do this.


The following cases are covered:
* Update a role with a temporal constraint
* Add the role to a User
* Query the role to view different stages of temporal constraint
* Delete a temporal constraint on a role

Usecase:
The previous sample left off with jdoe's contract ending because he finished up the work that was required for the
company. The company now has additional work that requires jdoe's expertise and has rehired him as a contractor. This
time however, the company has gone through some changes and will require that jdoe's contractor role be terminated as
soon as his contract is set to expire.

1) To start, patch the contractor role with a temporal constraint by selecting a start and end date. To do this, it is
necessary to select a start and end date. For the start date, use the current time plus 5 minutes. This will demonstrate
that we can add a future temporal constraint on a user before their contract even starts. For the end date, use 10
minutes from the current time.

        Note: In this sample command, the current time was 22:50:00. Adding 5 minutes results in a start time of
              22:55:00. Adding 10 minutes results in an end time 23:05:00. Adjust these values for the current time that
              you run the sample.

        $ curl --insecure \
               --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request PATCH \
               --data '[
                   {
                       "operation" : "add",
                       "field" : "/temporalConstraints",
                       "value" : [{"duration": "2016-05-05T22:55:00.000Z/2016-05-05T23:05:00.000Z"}]
                   }
                 ]' \
               'https://localhost:8443/openidm/managed/role/Contractor'

               {
                 "_id": "Contractor",
                 "_rev": "6",
                 "name": "Contractor",
                 "description": "Role assigned to contract workers",
                 "temporalConstraints": [
                   {
                     "duration": "2016-05-05T22:55:00.000Z/2016-05-05T23:05:00.000Z"
                   }
                 ]
               }

2) Grant the Contractor role to the managed user jdoe.

        $ curl --insecure \
               --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request PATCH \
               --data '[
                   {
                       "operation" : "add",
                       "field" : "/roles/-",
                       "value" : {
                         "_ref": "managed/role/Contractor"
                       }
                   }
                 ]' \
               'https://localhost:8443/openidm/managed/user/a240d21b-b602-48f3-bc0b-a2e8de4b061e'

                {
                  "_id": "a240d21b-b602-48f3-bc0b-a2e8de4b061e",
                  "_rev": "12",
                  "displayName": "John Doe",
                  "description": "Created for OpenIDM",
                  "givenName": "John",
                  "mail": "jdoe@example.com",
                  "telephoneNumber": "1-415-599-1100",
                  "sn": "Doe",
                  "userName": "jdoe",
                  "accountStatus": "active",
                  "effectiveRoles": [],
                  "effectiveAssignments": []
                }

As you can see from the response, jdoe does not have any effective roles or assignments because his contract hasn't
started yet. Check jdoe's entry again between the start and end date of the temporal constraint to observe the assignment.

3) During the contract period, query jdoe again to see he now has the Contractor role in his list of effective roles.

        $ curl --insecure \
                       --header "X-OpenIDM-Username: openidm-admin" \
                       --header "X-OpenIDM-Password: openidm-admin" \
                       --request GET \
                       'https://localhost:8443/openidm/managed/user?_queryFilter=/userName+sw+"jdoe"&_fields=roles,effectiveRoles,effectiveAssignments&_prettyPrint=true'

                {
                  "result": [
                    {
                      "_id": "a240d21b-b602-48f3-bc0b-a2e8de4b061e",
                      "_rev": "14",
                      "roles": [
                        {
                          "_ref": "managed/role/Contractor",
                          "_refProperties": {
                            "_id": "7edcb917-8d02-42b8-a4c6-77b97e936cbe",
                            "_rev": "6"
                          }
                        }
                      ],
                      "effectiveRoles": [
                        {
                          "_ref": "managed/role/Contractor"
                        }
                      ],
                      "effectiveAssignments": [
                        {
                          "name": "Contractor",
                          "description": "Contractor assignment for contract workers.",
                          "mapping": "managedUser_systemLdapAccounts",
                          "attributes": [
                            {
                              "name": "ldapGroups",
                              "value": [
                                "cn=Contractors,ou=Groups,dc=example,dc=com"
                              ],
                              "assignmentOperation": "mergeWithTarget",
                              "unassignmentOperation": "removeFromTarget"
                            },
                            {
                              "name": "employeeType",
                              "value": "Contractor",
                              "assignmentOperation": "mergeWithTarget",
                              "unassignmentOperation": "removeFromTarget"
                            }
                          ],
                          "_id": "cf034bbc-e1eb-44b2-9b16-d15627acc914",
                          "_rev": "1"
                        }
                      ]
                    }
                  ],
                  "resultCount": 1,
                  "pagedResultsCookie": null,
                  "totalPagedResultsPolicy": "NONE",
                  "totalPagedResults": -1,
                  "remainingPagedResults": -1
                }

4) During this time period, if we look at our OpenDJ Contractor groups we should see jdoe listed as one of the
contractors given membership to that group.

        $ curl --insecure \
                       --header "X-OpenIDM-Username: openidm-admin" \
                       --header "X-OpenIDM-Password: openidm-admin" \
                       --request GET \
                       'https://localhost:8443/openidm/system/ldap/account?_queryFilter=/uid+sw+"jdoe"&_fields=dn,uid,employeeType,ldapGroups&_prettyPrint=true'


                {
                  "result": [
                    {
                      "_id": "uid=jdoe,ou=People,dc=example,dc=com",
                      "dn": "uid=jdoe,ou=People,dc=example,dc=com",
                      "uid": "jdoe",
                      "employeeType": "Contractor",
                      "ldapGroups": [
                        "cn=openidm,ou=Groups,dc=example,dc=com",
                        "cn=Contractors,ou=Groups,dc=example,dc=com"
                      ]
                    }
                  ],
                  "resultCount": 1,
                  "pagedResultsCookie": null,
                  "totalPagedResultsPolicy": "NONE",
                  "totalPagedResults": -1,
                  "remainingPagedResults": -1
                }

5) Jdoe's contract expires when the current date is greater than the end date. Query jdoe's entry at that point to see
that he no longer has the Contractor roles and assignments in his effective roles and assignments lists.

        $ curl --insecure \
                               --header "X-OpenIDM-Username: openidm-admin" \
                               --header "X-OpenIDM-Password: openidm-admin" \
                               --request GET \
                               'https://localhost:8443/openidm/managed/user?_queryFilter=/userName+sw+"jdoe"&_fields=roles,effectiveRoles,effectiveAssignments&_prettyPrint=true'
                {
                  "result": [
                    {
                      "_id": "a240d21b-b602-48f3-bc0b-a2e8de4b061e",
                      "_rev": "16",
                      "roles": [
                        {
                          "_ref": "managed/role/Contractor",
                          "_refProperties": {
                            "_id": "7edcb917-8d02-42b8-a4c6-77b97e936cbe",
                            "_rev": "8"
                          }
                        }
                      ],
                      "effectiveRoles": [],
                      "effectiveAssignments": []
                    }
                  ],
                  "resultCount": 1,
                  "pagedResultsCookie": null,
                  "totalPagedResultsPolicy": "NONE",
                  "totalPagedResults": -1,
                  "remainingPagedResults": -1
                }

6) Now verify that jdoe is no longer part of the Contractors group in OpenDJ.

        $ curl --insecure \
                       --header "X-OpenIDM-Username: openidm-admin" \
                       --header "X-OpenIDM-Password: openidm-admin" \
                       --request GET \
                       'https://localhost:8443/openidm/system/ldap/account?_queryFilter=/uid+sw+"bjensen"&_fields=dn,uid,employeeType,ldapGroups&_prettyPrint=true'

                {
                  "result" : [ {
                    "_id" : "uid=jdoe,ou=People,dc=example,dc=com",
                    "dn" : "uid=jdoe,ou=People,dc=example,dc=com",
                    "uid" : "jdoe",
                    "employeeType" : null,
                    "ldapGroups" : [ "cn=openidm,ou=Groups,dc=example,dc=com" ]
                  } ],
                  "resultCount" : 1,
                  "pagedResultsCookie" : null,
                  "totalPagedResultsPolicy" : "NONE",
                  "totalPagedResults" : -1,
                  "remainingPagedResults" : -1
                }

7) Removing the Contractor role from jdoe's entry.

        $ curl --insecure \
                       --header "Content-type: application/json" \
                       --header "X-OpenIDM-Username: openidm-admin" \
                       --header "X-OpenIDM-Password: openidm-admin" \
                       --request PATCH \
                       --data '[
                           {
                               "operation" : "remove",
                               "field" : "/roles/0"
                           }
                         ]' \
                       'https://localhost:8443/openidm/managed/user/a240d21b-b602-48f3-bc0b-a2e8de4b061e'

                {
                  "_id": "a240d21b-b602-48f3-bc0b-a2e8de4b061e",
                  "_rev": "22",
                  "displayName": "John Doe",
                  "description": "Created for OpenIDM",
                  "givenName": "John",
                  "mail": "jdoe@example.com",
                  "telephoneNumber": "1-415-599-1100",
                  "sn": "Doe",
                  "userName": "jdoe",
                  "accountStatus": "active",
                  "effectiveRoles": [],
                  "effectiveAssignments": []
                }

Managing Temporal Constraints on a Role via the UI
--------------------------------------------------

1) Modify the Contractor role with a Temporal Constraint

Log in to the Admin UI at:
https://localhost:8443/admin
(or whatever applies for the hostname where OpenIDM is deployed.)

If you are using the default credentials provided with the product for the Administrative User, you must use:
username : openidm-admin
password : openidm-admin

When you have authenticated, select Manage > Role

You should now see a "Role List" page that contains a table with the Employee and Contractor Role.

Click on the Contractor Role.

You should now see under the Details of the Contractor role that there is section for Temporal Constraint.

Enable the Temporal Constraint option and add a start date. For the start date for this sample we will use the
current time plus 5 min. This will demonstrate that we can add a future temporal constraint on a user before their
contract even starts. For the end time, we will use 10 minutes from the current time calculation.

Click "Save". You are done creating a temporal constraint.

2) Assign the Contractor role to jdoe.

Navigate to Manage > User

From the list of users click on jdoe.

Click on the Provisioning Roles tab.

Click on Add Provisioning Roles.

In the Provisioning Roles field, select the Contractor role.

Click Add.

You should now see that jdoe has the Contractor role.

3) Verify that jdoe is not in the Contractors group.

From the previous step, you should be on the jdoe's profile page.

Click on the Linked Systems to see the ldapGroups that jdoe is part of in the OpenDJ system.

You should see that jdoe is part of cn=openidm,ou=Groups,dc=example,dc=com only at this time.

Wait until the time is between the start and end date to verify that jdoe has been added to the contractors group.

4) Verify that jdoe is part of the Contractors group.

Now refresh the tab for Linked Systems on the user profile and verify now that jdoe is part of the ldap group
Contractors.

cn=Contractors,ou=Groups,dc=example,dc=com

Wait until the end time has passed. In the next step we will verify that jdoe no longer has the Contractor role.

5) Verify that jdoe no longer has the Contractor role.

Now refresh the tab for Linked Systems on the user profile and verify now that jdoe is no longer part of the ldap group
Contractors.

You should now see that jdoe only has one group listed in ldapGroups.

6) Remove the Contractor role from jdoe.

From the user edit view page you are in for jdoe, click on the Provisioning Roles tab.

Select the Contractor role from the list of provisioning roles list.

Click on Remove Selected Provisioning Roles.

Click Ok to confirm the deletion.

