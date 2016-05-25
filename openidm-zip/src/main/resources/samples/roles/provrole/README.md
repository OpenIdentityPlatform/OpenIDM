    /**
     * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
     *
     * Copyright 2015-2016 ForgeRock AS. All rights reserved.
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

Roles Sample: Roles and Provisioning
=====================================

One of the great features of OpenIDM Roles is the ability to provision a set of attributes based on role membership.

Let's take a concrete example and continue with our Employee and Contractor roles example that was provided in the 
_crudops_ sample. This example will also build on _sample2b_ to provision user entries from Managed User to OpenDJ.

As an employee of the company a worker should be added to a couple of groups in OpenDJ (presumably to get access to some 
internal applications): the Employees group and the Chat Users group. But as a Contractor, workers will be added to the 
Contractors group only. We also want the type of employee to be set properly in OpenDJ, based on the role allocated to 
each user.

Pre-requisites: we assume that you are familiar with _sample2b_ and already have installed OpenDJ according to the 
instructions and configuration provided in that sample. We also assume here that you have reconciled the entries as
explained in that sample's section 2 & 4, but for this current sample.

Note: the Example.ldif provided with this sample should be loaded to OpenDJ, if that wasn't done previously.

        $ opendj/bin/ldapmodify -a -c --bindDN "cn=Directory Manager" --bindPassword password --hostname localhost --port 1389 --filename openidm/samples/roles/provrole/data/Example.ldif

To run this sample, use the following command:

        $ ./startup.sh -p samples/roles/provrole

Then create the Employee and Contractor roles, as you learned in the previous (crudops) sample. You can use the Admin 
UI or the REST interface to do this.

When you have created the roles, reconcile the managed user repository with the external system (OpenDJ). The easiest 
way to do this is by using the Admin UI. From the Dashboard, click Select a Mapping. Click on the first mapping 
(System/Ldap/Account --> Managed User) and click Reconcile Now. This populates the managed user repository with the 
entries in OpenDJ.

This sample provides all the information you need to cover the following use cases:

* Update a role with an assignment (sometimes called an entitlement)
* Grant a role to a user and observe the assignments for that user
* Specify how assignments will be propagated to an external system (OpenDJ)
* Deallocate a role from a user and observe how the assignments are withdrawn from the external system


1. Update the Employee role to add the correct groups and employee type

Let's take a look at the two roles you have just created:

        $ curl --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request GET \
               'http://localhost:8080/openidm/managed/role?_queryFilter=true&_prettyPrint=true'

        {
          "result" : [ {
            "_id" : "2902afd5-155a-49c0-9dd9-7e6bfcf1708a",
            "_rev" : "1",
            "name" : "Employee",
            "description" : "Role assigned to workers on the payroll."
          }, {
            "_id" : "e7f649ad-8013-4673-a52a-bdcac7483111",
            "_rev" : "1",
            "name" : "Contractor",
            "description" : "Role assigned to contract workers."
          } ],
          "resultCount" : 2,
          "pagedResultsCookie" : null,
          "totalPagedResultsPolicy" : "NONE",
          "totalPagedResults" : -1,
          "remainingPagedResults" : -1
        }

Note the IDs of these two roles because you will use them in the commands  
that follow.

Now, according to our company's policy, we need to make sure that every employee will have the correct _employeeType_ 
attribute in OpenDJ (corporate directory).

This is achieved in several steps. The first one is to create a new managed assignment:

        $ curl --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request POST \
               --data '{
                 "name" : "Employee",
                 "description": "Assignment for employees.",
                 "mapping" : "managedUser_systemLdapAccounts",
                 "attributes": [
                   {
                     "name": "employeeType",
                     "value": "Employee",
                     "assignmentOperation" : "mergeWithTarget",
                     "unassignmentOperation" : "removeFromTarget"
                   }
                 ]
               }' \
               http://localhost:8080/openidm/managed/assignment?_action=create
               
               {
                 "_id": "f2830b80-6ab8-416b-b219-d3bf2efd0ed3",
                 "_rev": "1",
                 "name": "Employee",
                 "description": "Assignment for employees.",
                 "mapping": "managedUser_systemLdapAccounts",
                 "attributes": [
                   {
                     "name": "employeeType",
                     "value": "Employee",
                     "assignmentOperation": "mergeWithTarget",
                     "unassignmentOperation": "removeFromTarget"
                   }
                 ]
               }

The next step is to add the assignment to the Employee role. Since we already have that role 
(with ID 2902afd5-155a-49c0-9dd9-7e6bfcf1708a in our example) we will just patch that entry:

        $ curl --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request PATCH \
               --data '[
                   {
                       "operation" : "add",
                       "field" : "/assignments/-",
                       "value" : { "_ref": "managed/assignment/f2830b80-6ab8-416b-b219-d3bf2efd0ed3" }
                   }
                 ]' \
               'http://localhost:8080/openidm/managed/role/2902afd5-155a-49c0-9dd9-7e6bfcf1708a'

               {
                 "_id": "2902afd5-155a-49c0-9dd9-7e6bfcf1708a",
                 "_rev": "2",
                 "name": "Employee",
                 "description": "Role assigned to workers on the payroll."
               }
               
2. Grant the Employee role to bjensen

In order to fully leverage _sample2b_ we will use Barbara Jensen as the employee. 
Let's take another look at the roles we have right now, to obtain their 
IDs:

        $ curl --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request GET \
               'http://localhost:8080/openidm/managed/role?_queryFilter=true&_prettyPrint=true'

               {
                 "result" : [ {
                   "_id" : "2902afd5-155a-49c0-9dd9-7e6bfcf1708a",
                   "_rev" : "2",
                   "name" : "Employee",
                   "description" : "Role assigned to workers on the payroll."
                 }, {
                   "_id" : "e7f649ad-8013-4673-a52a-bdcac7483111",
                   "_rev" : "1",
                   "name" : "Contractor",
                   "description" : "Role assigned to contract workers."
                 } ],
                 "resultCount" : 2,
                 "pagedResultsCookie" : null,
                 "totalPagedResultsPolicy" : "NONE",
                 "totalPagedResults" : -1,
                 "remainingPagedResults" : -1
               }

When you have both roles listed, you just need to grant the Employee role to bjensen. But first you need to find out 
what the identifier is for bjensen's entry:

        $ curl --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request GET \
               'http://localhost:8080/openidm/managed/user?_queryFilter=/userName+eq+"bjensen"&_fields=_id&_prettyPrint=true'

               {
                 "result" : [ {
                   "_id" : "2c7daf46-d3ce-4bc5-9790-b44113bca8e7",
                   "_rev" : "2"
                 } ],
               ...

Then you can grant the Employee role (ID 2902afd5-155a-49c0-9dd9-7e6bfcf1708a) 
to bjensen's entry (ID 2c7daf46-d3ce-4bc5-9790-b44113bca8e7) as follows:

        $ curl --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request PATCH \
               --data '[
                   {
                       "operation" : "add",
                       "field" : "/roles/-",
                       "value" : { "_ref": "managed/role/2902afd5-155a-49c0-9dd9-7e6bfcf1708a" }
                   }
                 ]' \
               'http://localhost:8080/openidm/managed/user/2c7daf46-d3ce-4bc5-9790-b44113bca8e7'

               {
                 "_id": "2c7daf46-d3ce-4bc5-9790-b44113bca8e7",
                 "_rev": "4",
                 "displayName": "Barbara Jensen",
                 "description": "Created for OpenIDM",
                 "givenName": "Barbara",
                 "mail": "bjensen@example.com",
                 "telephoneNumber": "1-360-229-7105",
                 "sn": "Jensen",
                 "userName": "bjensen",
                 "accountStatus": "active",
                 "effectiveRoles": [
                   {
                     "_ref": "managed/role/2902afd5-155a-49c0-9dd9-7e6bfcf1708a"
                   }
                 ],
                 "effectiveAssignments": [
                   {
                     "name": "Employee",
                     "description": "Assignment for employees.",
                     "mapping": "managedUser_systemLdapAccounts",
                     "attributes": [
                       {
                         "name": "employeeType",
                         "value": "Employee",
                         "assignmentOperation": "mergeWithTarget",
                         "unassignmentOperation": "removeFromTarget"
                       }
                     ],
                     "_id": "f2830b80-6ab8-416b-b219-d3bf2efd0ed3",
                     "_rev": "1"
                   }
                 ]
               }

Let's take a closer look at bjensen's entry for what we're really interested in, i.e. the roles, effective roles and 
effective assignments:

        $ curl --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request GET \
               'http://localhost:8080/openidm/managed/user?_queryFilter=/userName+eq+"bjensen"&_fields=_id,userName,roles,effectiveRoles,effectiveAssignments&_prettyPrint=true'
               
               { 
               "result" : [ {
                    "_id" : "2c7daf46-d3ce-4bc5-9790-b44113bca8e7",
                    "_rev" : "4",
                    "userName" : "bjensen",
                    "roles" : [ {
                      "_ref" : "managed/role/2902afd5-155a-49c0-9dd9-7e6bfcf1708a",
                      "_refProperties" : {
                        "_id" : "b1c29213-e726-466a-9051-e9bb4e593331",
                        "_rev" : "2"
                      }
                    } ],
                    "effectiveRoles" : [ {
                      "_ref" : "managed/role/2902afd5-155a-49c0-9dd9-7e6bfcf1708a"
                    } ],
                    "effectiveAssignments" : [ {
                      "name" : "Employee",
                      "description" : "Assignment for employees.",
                      "mapping" : "managedUser_systemLdapAccounts",
                      "attributes" : [ {
                        "name" : "employeeType",
                        "value" : "Employee",
                        "assignmentOperation" : "mergeWithTarget",
                        "unassignmentOperation" : "removeFromTarget"
                      } ],
                      "_id" : "f2830b80-6ab8-416b-b219-d3bf2efd0ed3",
                      "_rev" : "1"
                    } ]
                  } ],
                  "resultCount" : 1,
                  "pagedResultsCookie" : null,
                  "totalPagedResultsPolicy" : "NONE",
                  "totalPagedResults" : -1,
                  "remainingPagedResults" : -1
               }
    
Take note of the "effectiveAssignments" attribute in the result.
 
We can now clearly see the impact of the new property we added to the role. The user now has a new (calculated) property
which includes the set of assignments (or entitlements) that pertain to the user with that role. Currently this only 
lists the _employeeType_ attribute.

3. Pushing assignments out to OpenDJ (external system)

If you take a look at bjensen directly in the LDAP directory you should see the attribute _employeeType_ being populated 
properly:

            $ ldapsearch -p 1389 -h localhost -b "dc=example,dc=com" -D "cn=Directory Manager" -w - -s sub uid=bjensen dn uid employeeType

            # bjensen, People, example.com
            dn: uid=bjensen,ou=People,dc=example,dc=com
            uid: bjensen
            employeeType: Employee

Now let's make this a little more interesting by adding the groups that an Employee should have in the corporate 
directory (OpenDJ).

We just need to update the Employee role with the appropriate assignments. First, look at the current assignments of the 
Employee role:

        $ curl --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request GET \
               'http://localhost:8080/openidm/managed/role/2902afd5-155a-49c0-9dd9-7e6bfcf1708a?_fields=assignments,name&_prettyPrint=true'

               {
                 "_id" : "2902afd5-155a-49c0-9dd9-7e6bfcf1708a",
                 "_rev" : "2",
                 "assignments" : [ {
                   "_ref" : "managed/assignment/f2830b80-6ab8-416b-b219-d3bf2efd0ed3",
                   "_refProperties" : {
                     "_id" : "c0005ecb-9dda-4db1-8660-a723b8237f16",
                     "_rev" : "1"
                   }
                 } ],
                 "name" : "Employee"
               }

We simply need to add the attribute for groups to the assignment with ID 
f2830b80-6ab8-416b-b219-d3bf2efd0ed3:

        $ curl --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request PATCH \
               --data '[
                   {
                       "operation" : "add",
                       "field" : "/attributes/-",
                       "value" : {
                          "name": "ldapGroups",
                          "value": [
                              "cn=Employees,ou=Groups,dc=example,dc=com",
                              "cn=Chat Users,ou=Groups,dc=example,dc=com"
                          ],
                          "assignmentOperation" : "mergeWithTarget",
                          "unassignmentOperation" : "removeFromTarget"
                      }
                   }
               ]' \
               'http://localhost:8080/openidm/managed/assignment/f2830b80-6ab8-416b-b219-d3bf2efd0ed3'

               {
                 "_id": "f2830b80-6ab8-416b-b219-d3bf2efd0ed3",
                 "_rev": "2",
                 "name": "Employee",
                 "description": "Assignment for employees.",
                 "mapping": "managedUser_systemLdapAccounts",
                 "attributes": [
                   {
                     "name": "employeeType",
                     "value": "Employee",
                     "assignmentOperation": "mergeWithTarget",
                     "unassignmentOperation": "removeFromTarget"
                   },
                   {
                     "name": "ldapGroups",
                     "value": [
                       "cn=Employees,ou=Groups,dc=example,dc=com",
                       "cn=Chat Users,ou=Groups,dc=example,dc=com"
                     ],
                     "assignmentOperation": "mergeWithTarget",
                     "unassignmentOperation": "removeFromTarget"
                   }
                 ]
               }
               
After adding this new attribute to the assignment, bjensen should be added to the 
Chat Users and Employees groups. In the original LDIF file, bjensen was already a 
member of the openidm2 group you can ignore this group for the purposes of this sample. 

               $ ldapsearch -p 1389 -h localhost -b "dc=example,dc=com" -D "cn=Directory Manager" -w - -s sub uid=bjensen dn uid employeeType isMemberOf

               # bjensen, People, example.com
               dn: uid=bjensen,ou=People,dc=example,dc=com
               uid: bjensen
               employeeType: Employee
               isMemberOf: cn=openidm2,ou=Groups,dc=example,dc=com
               isMemberOf: cn=Chat Users,ou=Groups,dc=example,dc=com
               isMemberOf: cn=Employees,ou=Groups,dc=example,dc=com


        $ curl --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request GET \
               'http://localhost:8080/openidm/system/ldap/account?_queryFilter=/uid+sw+"bjensen"&_fields=dn,uid,employeeType,ldapGroups&_prettyPrint=true'

               {
                 "result" : [ {
                   "_id" : "uid=bjensen,ou=People,dc=example,dc=com",
                   "dn" : "uid=bjensen,ou=People,dc=example,dc=com",
                   "uid" : "bjensen",
                   "employeeType" : "Employee",
                   "ldapGroups" : [ "cn=openidm2,ou=Groups,dc=example,dc=com", "cn=Employees,ou=Groups,dc=example,dc=com", "cn=Chat Users,ou=Groups,dc=example,dc=com" ]
                 } ],
                 "resultCount" : 1,
                 "pagedResultsCookie" : null,
                 "totalPagedResultsPolicy" : "NONE",
                 "totalPagedResults" : -1,
                 "remainingPagedResults" : -1
               }

Let's continue with adding the appropriate attribute to the Contractor assignment 
and granting that role to jdoe, who is a contractor and therefore not entitled to 
access the internal chat application.

First create the Contractor assignment:

        $ curl --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request POST \
               --data '{
                 "name" : "Contractor",
                 "description": "Contractor assignment for contract workers.",
                 "mapping": "managedUser_systemLdapAccounts",
                 "attributes": [
                   {
                     "name": "ldapGroups",
                     "value": [
                       "cn=Contractors,ou=Groups,dc=example,dc=com"
                     ],
                     "assignmentOperation" : "mergeWithTarget",
                     "unassignmentOperation" : "removeFromTarget"
                   },
                   {
                     "name": "employeeType",
                     "value": "Contractor",
                     "assignmentOperation" : "mergeWithTarget",
                     "unassignmentOperation" : "removeFromTarget"
                   }
                 ]
               }' \
               http://localhost:8080/openidm/managed/assignment?_action=create
               
               {
                 "_id": "7536e234-1268-482d-8459-24c8ef832def",
                 "_rev": "1",
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
                 ]
               }

Note the ID of the Contractor assignment (7536e234-1268-482d-8459-24c8ef832def 
in this example). Now add the Contractor assignment to the Contractor role 
(ID e7f649ad-8013-4673-a52a-bdcac7483111 in this example):

        $ curl --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request PATCH \
               --data '[
                 {
                   "operation" : "add",
                   "field" : "/assignments/-",
                   "value" : {
                      "_ref" : "managed/assignment/7536e234-1268-482d-8459-24c8ef832def"
                   }
                 }
               ]' \
               'http://localhost:8080/openidm/managed/role/e7f649ad-8013-4673-a52a-bdcac7483111'

               {
                 "_id": "e7f649ad-8013-4673-a52a-bdcac7483111",
                 "_rev": "2",
                 "name": "Contractor",
                 "description": "Role assigned to contract workers."
               }
               
Now we just need to allocate the Contractor role to jdoe and he should be automatically added to the Contractors group 
in OpenDJ. Let's first take a look at jdoe's entry to make sure we know the value of the identifier:

        $ curl --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request GET \
               'http://localhost:8080/openidm/managed/user?_queryFilter=/userName+eq+"jdoe"&_fields=_id&_prettyPrint=true'

               {
                 "result" : [ {
                   "_id" : "92680be0-82f9-4297-9e00-c35c7cf700d2",
                   "_rev" : "2"
                 } ],
                 "resultCount" : 1,
                 "pagedResultsCookie" : null,
                 "totalPagedResultsPolicy" : "NONE",
                 "totalPagedResults" : -1,
                 "remainingPagedResults" : -1
               }

Now we can update jdoe's entry with the Contractor role:

        $ curl --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request PATCH \
               --data '[
                   {
                       "operation" : "add",
                       "field" : "/roles/-",
                       "value" : { 
                         "_ref": "managed/role/e7f649ad-8013-4673-a52a-bdcac7483111"
                       }
                   }
                 ]' \
               'http://localhost:8080/openidm/managed/user/92680be0-82f9-4297-9e00-c35c7cf700d2'

               {
                 "_id": "92680be0-82f9-4297-9e00-c35c7cf700d2",
                 "_rev": "4",
                 "displayName": "John Doe",
                 "description": "Created for OpenIDM",
                 "givenName": "John",
                 "mail": "jdoe@example.com",
                 "telephoneNumber": "1-415-599-1100",
                 "sn": "Doe",
                 "userName": "jdoe",
                 "accountStatus": "active",
                 "effectiveRoles": [
                   {
                     "_ref": "managed/role/e7f649ad-8013-4673-a52a-bdcac7483111"
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
                     "_id": "7536e234-1268-482d-8459-24c8ef832def",
                     "_rev": "1"
                   }
                 ]
               }

Let's now take a look at jdoe's entry in order to make sure that the proper employee type has been set and that jdoe has 
been added to the Contractors group.

        $ curl --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request GET \
               'http://localhost:8080/openidm/system/ldap/account?_queryFilter=/uid+sw+"jdoe"&_prettyPrint=true'

               {
                 "result" : [ {
                   "_id" : "uid=jdoe,ou=People,dc=example,dc=com",
                   "givenName" : "John",
                   "ldapGroups" : [ "cn=openidm,ou=Groups,dc=example,dc=com", "cn=Contractors,ou=Groups,dc=example,dc=com" ],
                   "mail" : "jdoe@example.com",
                   "employeeType" : "Contractor",
                   "uid" : "jdoe",
                   "telephoneNumber" : "1-415-599-1100",
                   "sn" : "Doe",
                   "disabled" : null,
                   "cn" : "John Doe",
                   "description" : "Created for OpenIDM",
                   "dn" : "uid=jdoe,ou=People,dc=example,dc=com"
                 } ],
                 "resultCount" : 1,
                 "pagedResultsCookie" : null,
                 "totalPagedResultsPolicy" : "NONE",
                 "totalPagedResults" : -1,
                 "remainingPagedResults" : -1
               }


4. Removing a role from a user

Now we know what happens with entitlements when a role is granted to a user, let's take a look at what happens when a 
role is deallocated from a user entry.

Again, we take a look at jdoe's entry to find out about its state:

        $ curl --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request GET \
               'http://localhost:8080/openidm/managed/user?_queryFilter=/userName+eq+"jdoe"&_fields=_id,roles&_prettyPrint=true'

               {
                 "result" : [ {
                   "_id" : "92680be0-82f9-4297-9e00-c35c7cf700d2",
                   "_rev" : "4",
                   "roles" : [ {
                     "_ref" : "managed/role/e7f649ad-8013-4673-a52a-bdcac7483111",
                     "_refProperties" : {
                       "_id" : "093fc34b-0694-478e-952e-98d0a828b1ac",
                       "_rev" : "2"
                     }
                   } ]
                 } ],
                 "resultCount" : 1,
                 "pagedResultsCookie" : null,
                 "totalPagedResultsPolicy" : "NONE",
                 "totalPagedResults" : -1,
                 "remainingPagedResults" : -1
               }

To remove the Contractor role from jdoe's entry, we can use a DELETE 
operation on the _roles_ property with the ID of the relationship 
retrieved in the previous step:

        $ curl --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request DELETE \
               'http://localhost:8080/openidm/managed/user/92680be0-82f9-4297-9e00-c35c7cf700d2/roles/093fc34b-0694-478e-952e-98d0a828b1ac'

               {
                 "_ref": "managed/role/e7f649ad-8013-4673-a52a-bdcac7483111",
                 "_refProperties": {
                     "_id": "093fc34b-0694-478e-952e-98d0a828b1ac",
                     "_rev": "0"
                 }
               }

This results in jdoe's entry in OpenDJ not belonging to the Contractors group anymore and in his employee type being 
undefined. Check that with a query on his entry in OpenDJ:


        $ curl --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request GET \
               'http://localhost:8080/openidm/system/ldap/account?_queryFilter=/uid+sw+"jdoe"&_prettyPrint=true'

               {
                 "result": [
                   {
                     "_id": "uid=jdoe,ou=People,dc=example,dc=com",
                     "givenName": "John",
                     "ldapGroups": [
                       "cn=openidm,ou=Groups,dc=example,dc=com"
                     ],
                     "mail": "jdoe@example.com",
                     "employeeType": null,
                     "uid": "jdoe",
                     "telephoneNumber": "1-415-599-1100",
                     "sn": "Doe",
                     "disabled": null,
                     "cn": "John Doe",
                     "description": "Created for OpenIDM",
                     "dn": "uid=jdoe,ou=People,dc=example,dc=com"
                   }
                 ],
                 "resultCount": 1,
                 "pagedResultsCookie": null,
                 "totalPagedResultsPolicy": "NONE",
                 "totalPagedResults": -1,
                 "remainingPagedResults": -1
               }

This is pretty much everything you need to know about roles, assignments and entitlements and how to manipulate them via 
the REST API.

Appendix
--------

If you need to reload the Employee and Contractor roles and assignments entirely without going through each step in the 
samples, here are the REST requests to do just that:

        $ curl --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request POST \
               --data '{
                 "name" : "Employee",
                 "description": "Employee assignment for workers on the payroll.",
                 "mapping": "managedUser_systemLdapAccounts",
                 "attributes": [
                   {
                     "name": "ldapGroups",
                     "value": [
                       "cn=Employees,ou=Groups,dc=example,dc=com",
                       "cn=Chat Users,ou=Groups,dc=example,dc=com"
                     ],
                     "assignmentOperation" : "mergeWithTarget",
                     "unassignmentOperation" : "removeFromTarget"
                   },
                   {
                     "name": "employeeType",
                     "value": "Employee",
                     "assignmentOperation" : "mergeWithTarget",
                     "unassignmentOperation" : "removeFromTarget"
                   }
                 ]
               }' \
               'http://localhost:8080/openidm/managed/assignment?_action=create'

        $ curl --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request POST \
               --data '{
                 "name" : "Contractor",
                 "description": "Contractor assignment for contract workers.",
                 "mapping": "managedUser_systemLdapAccounts",                 
                 "attributes": [
                   {
                     "name": "ldapGroups",
                     "value": [
                       "cn=Contractors,ou=Groups,dc=example,dc=com"
                     ],
                     "assignmentOperation" : "mergeWithTarget",
                     "unassignmentOperation" : "removeFromTarget"
                   },
                   {
                     "name": "employeeType",
                     "value": "Contractor",
                     "assignmentOperation" : "mergeWithTarget",
                     "unassignmentOperation" : "removeFromTarget"
                   }
                 ]
               }' \
               'http://localhost:8080/openidm/managed/assignment?_action=create'

        $ curl --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request POST \
               --data '{
                 "name" : "Employee",
                 "description": "Role assigned to workers on the payroll.",
                 "assignments": [
                   {
                     "_ref": "managed/assignment/<ID-of-employee-assignment>"
                   }
                 ]
               }' \
               'http://localhost:8080/openidm/managed/role?_action=create'

        $ curl --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request POST \
               --data '{
                 "name" : "Contractor",
                 "description": "Role assigned to contract workers.",
                 "assignments": [
                   {
                     "_ref": "managed/assignment/<ID-of-contractor-assignment>"
                   }
                 ]
               }' \
               'http://localhost:8080/openidm/managed/role?_action=create'
