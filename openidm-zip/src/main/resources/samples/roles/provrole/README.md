    /**
     * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
     *
     * Copyright 2015 ForgeRock AS. All rights reserved.
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

One of the great features of OpenIDM Roles is the ability to provision a set
of attributes based on role membership.

Let's take a concrete example and continue with our Employee and Contractor
roles example that was provided in the _crudops_ sample. This example will
also build on _sample2b_ to provision user entries from Managed User to OpenDJ.

As an employee of the company a worker should be added to a couple of groups in
OpenDJ (presumably to get access to some internal applications): the Employees
group and the Chat Users group. But as a Contractor, workers will be added
to the Contractors group only. We also want the type of employee to be set
properly in OpenDJ, based on the role allocated to each user.


Pre-requisites: we assume that you are familiar with _sample2b_ and already
have installed OpenDJ according to the instructions and configuration provided
in that sample. We also assume here that you have reconciled the entries as
explained in that sample's section 2 & 4, but for this current sample.

Note: the Example.ldif provided with this sample should be loaded to OpenDJ,
if that wasn't done previously.

        $ opendj/bin/ldapmodify -a -c --bindDN "cn=Directory Manager" --bindPassword password --hostname localhost --port 1389 --filename openidm/samples/roles/provrole/data/Example.ldif

This sample should be run like the others using the following command:

        $ nohup ./startup.sh -p samples/roles/provrole > logs/console.out 2>&1&

in order to pick up the configuration that's provided here. The reconciliation
of the external system (OpenDJ) can also performed easily via the UI by running
reconciliation for the first mapping (DJ --> Managed User) in order to populate
the user entries.


This sample provides all the information you need to cover the following use
cases:

* Update a role with an entitlement (called assignments in OpenIDM)
* Assign a role to a user and observe the entitlements for that user
* Specify how entitlements will be propagated to an external system (OpenDJ)
* Deallocate a role from a user and observe how the entitlements are withdrawn
  from the external system

Note: throughout this document we refer to entitlements and assignments
interchangeably, as they relate to roles.


1. Update the Employee role to add the correct groups and employee type

Let's take a look at the roles we created in the _crudops_ sample first:

        $ curl --insecure \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request GET \
               'https://localhost:8443/openidm/managed/role?_queryFilter=true&_prettyPrint=true'

               {
                 "result" : [ {
                   "properties" : {
                     "name" : "Employee",
                     "description" : "Role assigned to workers on the payroll."
                   },
                   "_id" : "Employee",
                   "_rev" : "1"
                 }, {
                   "properties" : {
                     "name" : "Contractor",
                     "description" : "Role assigned to contract workers."
                   },
                   "_id" : "Contractor",
                   "_rev" : "11"
                 } ],
                 "resultCount" : 2,
                 "pagedResultsCookie" : null,
                 "remainingPagedResults" : -1
               }

Now, according to our company's policy, we need to make sure that every employee
will have the correct _employeeType_ attribute in OpenDJ (corporate directory).

This is achieved in several steps. The first one is to add an _assignments_
property to the Employee role. Since we already have that role we will just
patch that entry:

        $ curl --insecure \
               --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --header "If-Match: *" \
               --request PATCH \
               --data '[
                    {
                        "operation" : "add",
                        "field" : "/assignments",
                        "value" : {
                          "ldap": {
                            "attributes": [
                              {
                                "name": "employeeType",
                                "value": "Employee",
                                "assignmentOperation" : "mergeWithTarget",
                                "unassignmentOperation" : "removeFromTarget"
                              }
                            ]
                          }
                        }
                    }
                 ]' \
               'https://localhost:8443/openidm/managed/role/Employee'

               {"properties":{"name":"Employee","description":"Role assigned to workers on the payroll."},"_id":"Employee","_rev":"2","assignments":{"ldap":{"attributes":[{"name":"employeeType","value":"Employee","assignmentOperation":"mergeWithTarget","unassignmentOperation":"removeFromTarget"}]}}}

2. Allocate the Employee role to bjensen

In order to fully leverage _sample2b_ we will use Barbara Jensen as the employee.
Let's take a look at the roles we should have right now:

        $ curl --insecure \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request GET \
               'https://localhost:8443/openidm/managed/role?_queryFilter=true&_prettyPrint=true'

               {
                 "result" : [ {
                   "properties" : {
                     "name" : "Employee",
                     "description" : "Role assigned to workers on the payroll."
                   },
                   "_id" : "Employee",
                   "_rev" : "1"
                 },
                 {
                   "properties" : {
                     "name" : "Contractor",
                     "description" : "Role assigned to contract workers."
                   },
                   "_id" : "Contractor",
                   "_rev" : "1"
                 } ],
                 "resultCount" : 2,
                 "pagedResultsCookie" : null,
                 "remainingPagedResults" : -1
               }

Or something along those lines.

Note: since the last step in the _crudops_ sample was to delete the Contractor
role via the Admin UI, you might have to issue the following request again to
populate the Contractor role:

        $ curl --insecure \
               --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --header "If-None-Match: *" \
               --request PUT \
               --data '{
               "properties" : {
                   "name" : "Contractor",
                   "description": "Role assigned to contract workers."
                   }
               }' \
               https://localhost:8443/openidm/managed/role/Contractor

Once you have both roles listed, you just need to assign the Employee role to
bjensen. But first you need to find out what the identifier is for bjensen's
entry:

        $ curl --insecure \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request GET \
               'https://localhost:8443/openidm/managed/user?_queryFilter=/userName+eq+"bjensen"&_fields=_id&_prettyPrint=true'

               {
                 "result" : [ {
                   "_id" : "8ff9639f-2a89-48a2-a0fd-9df4d5297eeb"
                 } ],
                 "resultCount" : 1,
                 "pagedResultsCookie" : null,
                 "remainingPagedResults" : -1
               }

Therefore you can assign the Employee role by using:

        $ curl --insecure \
               --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --header "If-Match: *" \
               --request PATCH \
               --data '[
                   {
                       "operation" : "add",
                       "field" : "/roles/-",
                       "value" : "managed/role/Employee"
                   }
                 ]' \
               'https://localhost:8443/openidm/managed/user/8ff9639f-2a89-48a2-a0fd-9df4d5297eeb'

               {"displayName":"Barbara Jensen","description":"Created for OpenIDM","givenName":"Barbara","mail":"bjensen@example.com","telephoneNumber":"1-360-229-7105","sn":"Jensen","userName":"bjensen","ldapGroups":["cn=openidm2,ou=Groups,dc=example,dc=com"],"accountStatus":"active","roles":["openidm-authorized","managed/role/Employee"],"lastPasswordSet":"","postalCode":"","stateProvince":"","passwordAttempts":"0","lastPasswordAttempt":"Fri Apr 17 2015 16:57:21 GMT-0000 (UTC)","postalAddress":"","address2":"","country":"","city":"","effectiveRoles":["openidm-authorized","managed/role/Employee"],"_id":"8ff9639f-2a89-48a2-a0fd-9df4d5297eeb","_rev":"4","effectiveAssignments":{"ldap":{"attributes":[{"name":"employeeType","value":"Employee","assignmentOperation":"mergeWithTarget","unassignmentOperation":"removeFromTarget","assignedThrough":"managed/role/Employee"}]}}}

Let's take a closer look at bjensen's entry for what we're really interested
in, i.e. the roles, effective roles and effective assignments:

        $ curl --insecure \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request GET \
               'https://localhost:8443/openidm/managed/user?_queryFilter=/userName+eq+"bjensen"&_fields=_id,userName,roles,effectiveRoles,effectiveAssignments&_prettyPrint=true'

{
  "result" : [ {
    "_id" : "8ff9639f-2a89-48a2-a0fd-9df4d5297eeb",
    "userName" : "bjensen",
    "roles" : [ "openidm-authorized", "managed/role/Employee" ],
    "effectiveRoles" : [ "openidm-authorized", "managed/role/Employee" ],
    "effectiveAssignments" : {
      "ldap" : {
        "attributes" : [ {
          "name" : "employeeType",
          "value" : "Employee",
          "assignmentOperation" : "mergeWithTarget",
          "unassignmentOperation" : "removeFromTarget",
          "assignedThrough" : "managed/role/Employee"
        } ]
      }
    }
  } ],
  "resultCount" : 1,
  "pagedResultsCookie" : null,
  "remainingPagedResults" : -1
}

We can now clearly see the impact of the new property we added to the role. The
user now has a new (calculated) property which includes the set of assignments
(or entitlements) that pertain to the user with that role. Currently this only
list the _employeeType_ attribute.

3. Pushing assignments out to OpenDJ (external system)

This sample's sync.json adds on to _sample2b_'s mapping by incorporating an
additional property, called _assignmentsToMap_:

            ....
            "name" : "managedUser_systemLdapAccounts",
            "source" : "managed/user",
            "target" : "system/ldap/account",
            "links" : "systemLdapAccounts_managedUser",
            "assignmentsToMap" : [
                "ldap"
            ],
            ....

Now if you take a look at bjensen directly in the directory you should see the
attribute _employeeType_ being populated properly:

            $ ldapsearch -p 1389 -h localhost -b "dc=example,dc=com" -D "cn=Directory Manager" -w - -s sub uid=bjensen dn uid employeeType

            # bjensen, People, example.com
            dn: uid=bjensen,ou=People,dc=example,dc=com
            uid: bjensen
            employeeType: Employee

Now let's make this a little more interesting by adding the groups that an
Employee should have in the corporate directory (OpenDJ).

We just need to update the Employee role with the appropriate entitlements.
First, let's look at the Employee role entry one more time:

        $ curl --insecure \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request GET \
               'https://localhost:8443/openidm/managed/role/Employee?_prettyPrint=true'

               {
                 "properties" : {
                   "name" : "Employee",
                   "description" : "Role assigned to workers on the payroll."
                 },
                 "_id" : "Employee",
                 "_rev" : "2",
                 "assignments" : {
                   "ldap" : {
                     "attributes" : [ {
                     "name" : "employeeType",
                     "value" : "Employee",
                       "assignmentOperation" : "mergeWithTarget",
                       "unassignmentOperation" : "removeFromTarget"
                     } ]
                   }
                 }
               }

We simply need to add the entitlement for groups under:
assignments/ldap/attributes

        $ curl --insecure \
               --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --header "If-Match: *" \
               --request PATCH \
               --data '[
                   {
                       "operation" : "add",
                       "field" : "/assignments/ldap/attributes/-",
                       "value" : {
                          "name": "ldapGroups",
                          "value": [
                              "cn=Employees,ou=Groups,dc=example,dc=com",
                              "cn=Chat User,ou=Groups,dc=example,dc=com"
                          ],
                          "assignmentOperation" : "mergeWithTarget",
                          "unassignmentOperation" : "removeFromTarget"
                      }
                   }
               ]' \
               'https://localhost:8443/openidm/managed/role/Employee'

               {"properties":{"name":"Employee","description":"Role assigned to workers on the payroll."},"_id":"Employee","_rev":"3","assignments":{"ldap":{"attributes":[{"name":"employeeType","value":"Employee","assignmentOperation":"mergeWithTarget","unassignmentOperation":"removeFromTarget"},{"name":"ldapGroups","value":["cn=Employees,ou=Groups,dc=example,dc=com","cn=Chat User,ou=Groups,dc=example,dc=com"],"assignmentOperation":"mergeWithTarget","unassignmentOperation":"removeFromTarget"}]}}}

After adding this new entitlement to the Employee role, bjensen should be
added to the Chat Users and Employees groups.

               $ ldapsearch -p 1389 -h localhost -b "dc=example,dc=com" -D "cn=Directory Manager" -w - -s sub uid=bjensen dn uid employeeType isMemberOf

               # bjensen, People, example.com
               dn: uid=bjensen,ou=People,dc=example,dc=com
               uid: bjensen
               employeeType: Employee
               isMemberOf: cn=Chat User,ou=Groups,dc=example,dc=com
               isMemberOf: cn=Employees,ou=Groups,dc=example,dc=com


        $ curl --insecure \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request GET \
               'https://localhost:8443/openidm/system/ldap/account?_queryFilter=/uid+sw+"bjensen"&_fields=dn,uid,employeeType,ldapGroups&_prettyPrint=true'

               {
                 "result" : [ {
                   "dn" : "uid=bjensen,ou=People,dc=example,dc=com",
                   "uid" : "bjensen",
                   "employeeType" : "Employee",
                   "ldapGroups" : [ "cn=Chat Users,ou=Groups,dc=example,dc=com", "cn=Employees,ou=Groups,dc=example,dc=com",  ]
                 } ],
                 "resultCount" : 1,
                 "pagedResultsCookie" : null,
                 "remainingPagedResults" : -1
               }

Let's continue with adding the appropriate entitlements to the Contractor role
and allocating that role to jdoe, who is a contractor and therefore not
entitled to access the internal chat application:

        $ curl --insecure \
               --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --header "If-Match: *" \
               --request PATCH \
               --data '[
                   {
                       "operation" : "add",
                       "field" : "/assignments/ldap/attributes",
                       "value" : [{
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
                          "assignmentOperation": "mergeWithTarget",
                          "unassignmentOperation": "removeFromTarget"
                       }]
                   }
               ]' \
               'https://localhost:8443/openidm/managed/role/Contractor'

               {"properties":{"name":"Contractor","description":"Role assigned to contract workers."},"_id":"Contractor","_rev":"2","assignments":{"ldap":{"attributes":[{"name":"ldapGroups","value":["cn=Contractors,ou=Groups,dc=example,dc=com"],"assignmentOperation":"mergeWithTarget","unassignmentOperation":"removeFromTarget"},{"name":"employeeType","value":"Contractor","assignmentOperation":"mergeWithTarget","unassignmentOperation":"removeFromTarget"}]}}}


Now we just need to allocate the Contractor role to jdoe and he should be
automatically added to the Contractors group in OpenDJ. Let's first take a look
at jdoe's entry to make sure we know the value of the identifier:

        $ curl --insecure \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request GET \
               'https://localhost:8443/openidm/managed/user?_queryFilter=/userName+eq+"jdoe"&_fields=_id&_prettyPrint=true'

               {
                 "result" : [ {
                   "_id" : "3f9ada28-2809-4909-aadf-815567b00a4d" 
                 } ],
                 "resultCount" : 1,
                 "pagedResultsCookie" : null,
                 "remainingPagedResults" : -1
               }

Now we can update jdoe's entry with the Contractor role:

        $ curl --insecure \
               --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --header "If-Match: *" \
               --request PATCH \
               --data '[
                   {
                       "operation" : "add",
                       "field" : "/roles/-",
                       "value" : "managed/role/Contractor"
                   }
                 ]' \
               'https://localhost:8443/openidm/managed/user/3f9ada28-2809-4909-aadf-815567b00a4d'

               {"displayName":"John Doe","description":"Created for OpenIDM","givenName":"John","mail":"jdoe@example.com","telephoneNumber":"1-415-599-1100","sn":"Doe","userName":"jdoe","ldapGroups":["cn=openidm,ou=Groups,dc=example,dc=com"],"accountStatus":"active","roles":["openidm-authorized","managed/role/Contractor"],"lastPasswordSet":"","postalCode":"","stateProvince":"","passwordAttempts":"0","lastPasswordAttempt":"Fri Apr 17 2015 16:57:21 GMT-0000 (UTC)","postalAddress":"","address2":"","country":"","city":"","effectiveRoles":["openidm-authorized","managed/role/Contractor"],"effectiveAssignments":{"ldap":{"attributes":[{"name":"ldapGroups","value":["cn=Contractors,ou=Groups,dc=example,dc=com"],"assignmentOperation":"mergeWithTarget","unassignmentOperation":"removeFromTarget","assignedThrough":"managed/role/Contractor"},{"name":"employeeType","value":"Contractor","assignmentOperation":"mergeWithTarget","unassignmentOperation":"removeFromTarget","assignedThrough":"managed/role/Contractor"}]}},"_id":"3f9ada28-2809-4909-aadf-815567b00a4d","_rev":"2"}

Let's now take a look at jdoe's entry in order to make sure that the proper
employee type has been set and that jdoe has been added to the Contractors
group.

        $ curl --insecure \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request GET \
               'https://localhost:8443/openidm/system/ldap/account?_queryFilter=/uid+sw+"jdoe"&_prettyPrint=true'

               {
                 "result" : [ {
                   "sn" : "Doe",
                   "telephoneNumber" : "1-415-599-1100",
                   "employeeType" : "Contractor",
                   "dn" : "uid=jdoe,ou=People,dc=example,dc=com",
                   "cn" : "John Doe",
                   "uid" : "jdoe",
                   "ldapGroups" : [ "cn=openidm,ou=Groups,dc=example,dc=com", "cn=Contractors,ou=Groups,dc=example,dc=com" ],
                   "givenName" : "John",
                   "mail" : "jdoe@example.com",
                   "description" : "Created for OpenIDM",
                   "_id" : "uid=jdoe,ou=People,dc=example,dc=com"
                 } ],
                 "resultCount" : 1,
                 "pagedResultsCookie" : null,
                 "remainingPagedResults" : -1
               }


4. Removing a role from a user

Now we know what happens with entitlements when a role is assigned to a user,
let's take a look at what happens when a role is deallocated from a user entry.

Again, we take a look at jdoe's entry to find out about its state:

        $ curl --insecure \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request GET \
               'https://localhost:8443/openidm/managed/user?_queryFilter=/userName+eq+"jdoe"&_fields=_id,roles&_prettyPrint=true'

               {
                 "result" : [ {
                   "_id" : "3f9ada28-2809-4909-aadf-815567b00a4d",
                   "roles" : [ "openidm-authorized", "managed/role/Contractor" ]
                 } ],
                 "resultCount" : 1,
                 "pagedResultsCookie" : null,
                 "remainingPagedResults" : -1
               }

We therefore need to remove the 2nd element of the roles array (index = 1) in
order to remove the Contractor role -- also please note the entry's identifier
that is used in the request's URL:

        $ curl --insecure \
               --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --header "If-Match: *" \
               --request PATCH \
               --data '[
                   {
                       "operation" : "remove",
                       "field" : "/roles/1"
                   }
                 ]' \
               'https://localhost:8443/openidm/managed/user/3f9ada28-2809-4909-aadf-815567b00a4d'

               {"displayName":"John Doe","description":"Created for OpenIDM","givenName":"John","mail":"jdoe@example.com","telephoneNumber":"1-415-599-1100","sn":"Doe","userName":"jdoe","ldapGroups":["cn=openidm,ou=Groups,dc=example,dc=com"],"accountStatus":"active","roles":["openidm-authorized"],"lastPasswordSet":"","postalCode":"","stateProvince":"","passwordAttempts":"0","lastPasswordAttempt":"Fri Apr 17 2015 16:57:21 GMT-0000 (UTC)","postalAddress":"","address2":"","country":"","city":"","effectiveRoles":["openidm-authorized"],"_id":"3f9ada28-2809-4909-aadf-815567b00a4d","_rev":"3","effectiveAssignments":{}}

This results in jdoe's entry in OpenDJ not belonging to the Contractors group
anymore and its employee type being undefined."


        $ curl --insecure \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request GET \
               'https://localhost:8443/openidm/system/ldap/account?_queryFilter=/uid+sw+"jdoe"&_prettyPrint=true'

               {
                 "result" : [ {
                   "sn" : "Doe",
                   "telephoneNumber" : "1-415-599-1100",
                   "employeeType" : null,
                   "dn" : "uid=jdoe,ou=People,dc=example,dc=com",
                   "cn" : "John Doe",
                   "uid" : "jdoe",
                   "ldapGroups" : [ "cn=openidm,ou=Groups,dc=example,dc=com" ],
                   "givenName" : "John",
                   "mail" : "jdoe@example.com",
                   "description" : "Created for OpenIDM",
                   "_id" : "uid=jdoe,ou=People,dc=example,dc=com"
                 } ],
                 "resultCount" : 1,
                 "pagedResultsCookie" : null,
                 "remainingPagedResults" : -1
               } 


Note: some additional samples might be provided to demonstrate the different
assignment operations (merge, replace, remove, etc.).

This is pretty much everything you need to know about roles and entitlements
and how to manipulate them via the REST API.

At this time entitlements are not available through the Admin UI, but they
will soon be.


Appendix
--------

If you need to reload the Employee and Contractor roles entirely without
going through each step in the samples, here are the REST requests
to do just that:

        $ curl --insecure \
               --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --header "If-None-Match: *" \
               --request PUT \
               --data '{
               "properties" : {
                   "name" : "Employee",
                   "description": "Role assigned to workers on the payroll."
               },
               "assignments": {
                   "ldap": {
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
                   }
                 }
               }' \
               'https://localhost:8443/openidm/managed/role/Employee'

        $ curl --insecure \
               --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --header "If-None-Match: *" \
               --request PUT \
               --data '{
               "properties" : {
                   "name" : "Contractor",
                   "description": "Role assigned to contract workers."
               },
               "assignments": {
                   "ldap": {
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
                   }
                 }
               }' \
               'https://localhost:8443/openidm/managed/role/Contractor'

