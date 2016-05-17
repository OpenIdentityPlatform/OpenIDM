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

Roles Sample: Managing Roles via REST and via the UI
=====================================================

This sample provides all the information you need to manage Roles in OpenIDM, via either REST or via the Administrative 
UI. The following use cases are covered:
* Create a role
* Update a role
* Query all roles
* Grant a role to a user
* Delete a role

Note: this sample doesn't contain any particular configuration. So OpenIDM can just be started with the following 
command

        $ nohup ./startup.sh > logs/console.out 2>&1&


CRUD operations for Roles
-------------------------

1. Creating the Employee and Contractor roles

Like any managed object, you can use either a PUT or a POST request to 
create a role. A PUT request allows you to specify a human-readable 
resource ID for the object you create. With a POST request, the server 
generates an identifier for the new object. This system-generated ID is 
not human-readable. Although a PUT request results in a role ID that is 
easy to read and manipulate (via curl for example) it is a _mutable_ 
identifier. Therefore any renaming will potentially create conflicts and 
referential integrity issues.

In production systems, you should use POST requests with a 
system-generated ID. The examples in this sample use system-generated IDs 
as best practice.

Create a role named "Employee":

        $ curl --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request POST \
               --data '{
                 "name" : "Employee",
                 "description": "Role assigned to workers on the payroll."
               }' \
               http://localhost:8080/openidm/managed/role?_action=create

               {
                 "_id": "ad19979e-adbb-4d35-8320-6db50646b432",
                 "_rev": "1",
                 "name": "Employee",
                 "description": "Role assigned to workers on the payroll."
               }

Note the generated role ID (ad19979e-adbb-4d35-8320-6db50646b432 in this 
example).

Now create a role named "Contractor":

        $ curl --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request POST \
               --data '{
                 "name" : "Contractor",
                 "description": "Role assigned to contract workers."
               }' \
               http://localhost:8080/openidm/managed/role?_action=create

               {
                 "_id": "b02d2531-5066-415e-bc90-31fe57e02322",
                 "_rev": "1",
                 "name": "Contractor",
                 "description": "Role assigned to contract workers."
               }

Again, note the generated role ID (b02d2531-5066-415e-bc90-31fe57e02322 
in this example).

2. Reading (and searching) the Employee and Contractor roles

To read the Employee or Contractor roles, you can include the role ID in 
the URL. To read the Employee role:

        $ curl --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request GET \
               http://localhost:8080/openidm/managed/role/ad19979e-adbb-4d35-8320-6db50646b432

               {
                 "_id": "ad19979e-adbb-4d35-8320-6db50646b432",
                 "_rev": "1",
                 "name": "Employee",
                 "description": "Role assigned to workers on the payroll."
               }

To read the Contractor role:

        $ curl --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request GET \
               http://localhost:8080/openidm/managed/role/b02d2531-5066-415e-bc90-31fe57e02322

               {
                 "_id": "b02d2531-5066-415e-bc90-31fe57e02322",
                 "_rev": "1",
                 "name": "Contractor",
                 "description": "Role assigned to contract workers."
               }

An easier way to retrieve a role with a server side (or system) generated 
identifier is to use the query filter facility (see the section 
"Common Filter Expressions" in the Integrator's guide: 
https://forgerock.org/openidm/doc/bootstrap/integrators-guide/index.html#query-filters).

The following query uses a filter expression to retrieve a role with a 
name equal to "Contractor", and uses the prettyPrint parameter to 
display the result in a clear format:

        $ curl --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request GET \
               'http://localhost:8080/openidm/managed/role?_queryFilter=/name+eq+"Contractor"&_prettyPrint=true'

              {
                "result": [
                  {
                    "_id": "b02d2531-5066-415e-bc90-31fe57e02322",
                    "_rev": "1",
                    "name": "Contractor",
                    "description": "Role assigned to contract workers."
                  }
                ],
                "resultCount": 1,
                "pagedResultsCookie": null,
                "totalPagedResultsPolicy": "NONE",
                "totalPagedResults": -1,
                "remainingPagedResults": -1
              }

In addition we can retrieve all the managed roles currently available by 
using the following query filter request:

        $ curl --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request GET \
               'http://localhost:8080/openidm/managed/role?_queryFilter=true&_prettyPrint=true'

               {
                 "result" : [ {
                   "_id" : "ad19979e-adbb-4d35-8320-6db50646b432",
                   "_rev" : "1",
                   "name" : "Employee",
                   "description" : "Role assigned to workers on the payroll."
                 }, {
                   "_id" : "b02d2531-5066-415e-bc90-31fe57e02322",
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

3. Grant the Employee role to a user

The queries above allowed us to see how easy it is to create roles 
through the REST interface, but by themselves they don't do very much. A 
role becomes somewhat more meaningful when a user is granted that role.

Let's create a new user, Felicitas Doe (the user we encounter in 
sample2b), as our test user:

        $ curl --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request POST \
               --data '{
                  "mail":"fdoe@example.com",
                  "sn":"Doe",
                  "telephoneNumber":"555-1234",
                  "userName":"fdoe",
                  "givenName":"Felicitas",
                  "description":"Felicitas Doe",
                  "displayName":"fdoe"
                }' \
               'http://localhost:8080/openidm/managed/user?_action=create'
               
               {
                 "_id": "837085ae-766e-417c-9b7e-c36eee4352a3",
                 "_rev": "1",
                 "mail": "fdoe@example.com",
                 "sn": "Doe",
                 "telephoneNumber": "555-1234",
                 "userName": "fdoe",
                 "givenName": "Felicitas",
                 "description": "Felicitas Doe",
                 "displayName": "fdoe",
                 "accountStatus": "active",
                 "effectiveRoles": [],
                 "effectiveAssignments": []
               }

As can be seen from the result output, our new user has not been granted 
any roles, so her "effectiveRoles" attribute is empty.

So, how do we grant the Employee role to this new user who is now on the 
payroll? We need to update the user entry via a PATCH request and add a 
pointer to the role ID:

        $ curl --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request PATCH \
               --data '[
                   {
                       "operation" : "add",
                       "field" : "/roles/-",
                       "value" : {"_ref": "managed/role/ad19979e-adbb-4d35-8320-6db50646b432"}
                   }
                 ]' \
               'http://localhost:8080/openidm/managed/user/837085ae-766e-417c-9b7e-c36eee4352a3'

               {
                 "_id": "837085ae-766e-417c-9b7e-c36eee4352a3",
                 "_rev": "2",
                 "mail": "fdoe@example.com",
                 "sn": "Doe",
                 "telephoneNumber": "555-1234",
                 "userName": "fdoe",
                 "givenName": "Felicitas",
                 "description": "Felicitas Doe",
                 "displayName": "fdoe",
                 "accountStatus": "active",
                 "effectiveRoles": [
                   {
                     "_ref": "managed/role/ad19979e-adbb-4d35-8320-6db50646b432"
                   }
                 ],
                 "effectiveAssignments": []
               }

Let's take a closer look at Felicitas' entry using the query filter mechanism:

        $ curl --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request GET \
               'http://localhost:8080/openidm/managed/user?_queryFilter=/givenName+eq+"Felicitas"&_fields=_id,userName,roles,effectiveRoles&_prettyPrint=true'

               {
                 "result" : [ {
                   "_id" : "837085ae-766e-417c-9b7e-c36eee4352a3",
                   "_rev" : "2",
                   "userName" : "fdoe",
                   "roles" : [ {
                     "_ref" : "managed/role/ad19979e-adbb-4d35-8320-6db50646b432",
                     "_refProperties" : {
                       "_id" : "4a42cd0b-d5d0-47e9-81e7-513aed74f6bc",
                       "_rev" : "1"
                     }
                   } ],
                   "effectiveRoles" : [ {
                     "_ref" : "managed/role/ad19979e-adbb-4d35-8320-6db50646b432"
                   } ]
                 } ],
                 "resultCount" : 1,
                 "pagedResultsCookie" : null,
                 "totalPagedResultsPolicy" : "NONE",
                 "totalPagedResults" : -1,
                 "remainingPagedResults" : -1
               }

We can see that the Employee role (with ID ad19979e-adbb-4d35-8320-6db50646b432) 
is now part of Felicitas' _roles_ attribute. But we also observe that 
the same role is present in the _effectiveRoles_ attribute. This attribute 
is calculated based on the roles that have been granted to the user.

4. Removing the Employee role from a user

Let's imagine that the Employee role was erroneously assigned to Felicitas. 
We now need to remove this role from her entry. To do that we need to use 
a JSON Patch _remove_ operation. Since the role is part of an array, and 
is the first element in that array, the resulting PATCH request would 
look as follows:

        $ curl --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request PATCH \
               --data '[
                   {
                       "operation" : "remove",
                       "field" : "/roles/0"
                   }
                 ]' \
               'http://localhost:8080/openidm/managed/user/837085ae-766e-417c-9b7e-c36eee4352a3'

               {
                 "_id": "837085ae-766e-417c-9b7e-c36eee4352a3",
                 "_rev": "3",
                 "mail": "fdoe@example.com",
                 "sn": "Doe",
                 "telephoneNumber": "555-1234",
                 "userName": "fdoe",
                 "givenName": "Felicitas",
                 "description": "Felicitas Doe",
                 "displayName": "fdoe",
                 "accountStatus": "active",
                 "effectiveRoles": [],
                 "effectiveAssignments": []
               }
               
Our user no longer has the _Employee_ role in her _effectiveRoles_ 
attribute.

5. Deleting the Contractor role

The final step in role management is to remove an existing role. This is 
what we will do with the Contractor role. But first let's take a look at 
what would happen if we tried to remove a role already granted to a user.

Let's, temporarily, add the Contractor role (ID b02d2531-5066-415e-bc90-31fe57e02322) 
to Felicitas:

        $ curl --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request PATCH \
               --data '[
                   {
                       "operation" : "add",
                       "field" : "/roles/-",
                       "value" : {"_ref":"managed/role/b02d2531-5066-415e-bc90-31fe57e02322"}
                   }
                 ]' \
               'http://localhost:8080/openidm/managed/user/837085ae-766e-417c-9b7e-c36eee4352a3'
               
               {
                 "_id": "837085ae-766e-417c-9b7e-c36eee4352a3",
                 "_rev": "4",
                 "mail": "fdoe@example.com",
                 "sn": "Doe",
                 "telephoneNumber": "555-1234",
                 "userName": "fdoe",
                 "givenName": "Felicitas",
                 "description": "Felicitas Doe",
                 "displayName": "fdoe",
                 "accountStatus": "active",
                 "effectiveRoles": [
                   {
                     "_ref": "managed/role/b02d2531-5066-415e-bc90-31fe57e02322"
                   }
                 ],
                 "effectiveAssignments": []
               }

Now let's try and delete the Contractor role:

        $ curl --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request DELETE \
               'http://localhost:8080/openidm/managed/role/b02d2531-5066-415e-bc90-31fe57e02322'

               {
                 "code": 409,
                 "reason": "Conflict",
                 "message": "Cannot delete a role that is currently granted"
               }

So, we cannot delete a role that has been granted to one or more users. 
We must first deallocate that role from the user entry:

        $ curl --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request PATCH \
               --data '[
                   {
                       "operation" : "remove",
                       "field" : "/roles/0"
                   }
                 ]' \
               'http://localhost:8080/openidm/managed/user/837085ae-766e-417c-9b7e-c36eee4352a3'

               {
                 "_id": "837085ae-766e-417c-9b7e-c36eee4352a3",
                 "_rev": "5",
                 "mail": "fdoe@example.com",
                 "sn": "Doe",
                 "telephoneNumber": "555-1234",
                 "userName": "fdoe",
                 "givenName": "Felicitas",
                 "description": "Felicitas Doe",
                 "displayName": "fdoe",
                 "accountStatus": "active",
                 "effectiveRoles": [],
                 "effectiveAssignments": []
               }
               
Now let's try and delete the Contractor role again:

        $ curl --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request DELETE \
               'http://localhost:8080/openidm/managed/role/b02d2531-5066-415e-bc90-31fe57e02322'

               {
                 "_id": "b02d2531-5066-415e-bc90-31fe57e02322",
                 "_rev": "1",
                 "name": "Contractor",
                 "description": "Role assigned to contract workers."
               }

Let's verify that the Contractor role has been deleted:

        $ curl --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request GET \
               'http://localhost:8080/openidm/managed/role?_queryFilter=true&_prettyPrint=true'

               {
                 "result": [
                   {
                     "_id": "ad19979e-adbb-4d35-8320-6db50646b432",
                     "_rev": "1",
                     "name": "Employee",
                     "description": "Role assigned to workers on the payroll."
                   }
                 ],
                 "resultCount": 1,
                 "pagedResultsCookie": null,
                 "totalPagedResultsPolicy": "NONE",
                 "totalPagedResults": -1,
                 "remainingPagedResults": -1
               }

Only the Employee role remains.


Managing Roles from the Admin UI
--------------------------------

All management operations on Roles performed above via REST are possible via the Admin UI.

1. Create the Contractor role again

Log in to the Admin UI at:
https://localhost:8443/admin
(or whatever applies for the hostname where OpenIDM is deployed.)

If you are using the default credentials provided with the product for the Administrative User, you must use:
username : openidm-admin
password : openidm-admin

When you have authenticated, select Manage > Role.

You should now see a "Role List" page that contains a table with the Employee role listed as one of the items (only one 
if you've followed all the steps above).

To create the Contractor role again, click "New Role". You should now be able to enter the new role name: Contractor, 
and the role description: Role assigned to contract workers.

Click "Save" and voilà! You're done creating a role.

2. Reading (and searching) roles

The "Role List" gives you the ability to see all the roles that have been created and also to filter based on the name 
or the description of the role.

By clicking on each role name, you're able to see the details of each role.

Selecting "Back to Role List" under a role properties page brings you back to the table that lists all the roles.

3. Granting the Contractor role to a user

This time we will grant the Contractor role to Felicitas. Select Manage > User 
and click fdoe's entry. On fdoe's profile page, select the Provisioning Roles 
tab. Click Add Provisioning Roles, select the Contractor role from the dropdown 
list and click Add.  

Felicitas now has the Contractor role in her list of roles.

4. Removing the Contractor role from a user

To remove the Contractor role from Felicitas's entry, select Manage > User 
and click fdoe's entry. On fdoe's profile page, select the Provisioning Roles 
tab. Select the checkbox next to the Contractor role, then click 
Remove Selected Provisioning Roles.

5. Deleting the Contractor role.

Select Manage Role to display the "Role List" page. Select the checkbox 
next to Contractor (don't click on the role itself). Click on "Delete Selected" 
and the Contractor role will be deleted.

See the other samples for more information on roles!