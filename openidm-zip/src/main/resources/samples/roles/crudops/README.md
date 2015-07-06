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

Roles Sample: Managing Roles via REST and via the UI
=====================================================

This sample provides all the information you need to manage Roles in
OpenIDM, via either REST or via the Administrative UI. The following use
cases are covered:
* Create a role
* Update a role
* Query all roles
* Assign a role to a user
* Query all users and their roles
* Delete a role

Note: this sample doesn't contain any particular configuration. So OpenIDM
can just be started with the following command

        $ nohup ./startup.sh > logs/console.out 2>&1&


CRUD operations for Roles
-------------------------

1. Creating the Employee and Contractor roles

There are a couple of methods to create a new role. You can either use a PUT
or a POST. The PUT request will allow you to specify the resource id for the
object you create.

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
                   }
               }' \
               https://localhost:8443/openidm/managed/role/Employee

               {"properties":{"name":"Employee","description":"Role assigned to workers on the payroll."},"_id":"Employee","_rev":"1"}


In this case the role's identifier, as shown by the response above, will be:
"Employee".

But you can also use a POST request, in which case the server will
automatically generate an identifier for the new object.

        $ curl --insecure \
               --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request POST \
               --data '{
               "properties" : {
                   "name" : "Contractor",
                   "description": "Role assigned to contract workers."
                   }
               }' \
               https://localhost:8443/openidm/managed/role?_action=create

               {"properties":{"name":"Contractor","description":"Role assigned to contract workers."},"_id":"c22316ba-2096-4272-af10-9b17c17e555b","_rev":"1"}

The system generated identifier is not human readable.

So, while a PUT request might result in a role id that is very easy to read
and manipulate (via curl for example) it is a _mutable_ identifier. Therefore
any renaming will potentially create conflicts and referential integrity issues.

2. Reading (and searching) the Employee and Contractor roles

It is a simple process to read the Employee role:

        $ curl --insecure \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request GET \
               https://localhost:8443/openidm/managed/role/Employee

               {"properties":{"name":"Employee","description":"Role assigned to workers on the payroll."},"_id":"Employee","_rev":"1"}

... but it is a little more complicated to read the Contractor role. Of course
the same query can be applied by using the system-generated identifier, but it
is rather unsightly:

        $ curl --insecure \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request GET \
               https://localhost:8443/openidm/managed/role/c22316ba-2096-4272-af10-9b17c17e555b

               {"properties":{"name":"Contractor","description":"Role assigned to contract workers."},"_id":"c22316ba-2096-4272-af10-9b17c17e555b","_rev":"1"}

An easier way to retrieve a role with a server side (or system) generated
identifier is to use the query filter facility (see the section "Common Filter Expressions" 
in the Integrator's guide: http://openidm.forgerock.org/doc/bootstrap/integrators-guide/index.html#query-filters).

The following query uses a filter expression to retrieve a role with a name
equal to "Contractor", and displays the result in a clear format:

        $ curl --insecure \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request GET \
               'https://localhost:8443/openidm/managed/role?_queryFilter=/properties/name+eq+"Contractor"&_prettyPrint=true'

               {
                 "result" : [ {
                   "properties" : {
                     "name" : "Contractor",
                     "description" : "Role assigned to contract workers."
                   },
                   "_id" : "c22316ba-2096-4272-af10-9b17c17e555b",
                   "_rev" : "1"
                 } ],
                 "resultCount" : 1,
                 "pagedResultsCookie" : null,
                 "remainingPagedResults" : -1
               }

In addition we can retrieve all the managed roles currently available by using the
following query filter request:

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
                   "_id" : "c22316ba-2096-4272-af10-9b17c17e555b",
                   "_rev" : "1"
                 } ],
                 "resultCount" : 2,
                 "pagedResultsCookie" : null,
                 "remainingPagedResults" : -1
               }

3. Assigning the Employee role to a user

The queries above allowed us to see how easy it is to create roles through
the REST interface, but by themselves they don't do very much. A role becomes
somewhat more meaningful when a user is assigned that role.

Let's use Felicitas Doe, the user we encounter in sample2b, as our test user:

        $ curl --insecure \
               --header "Content-type: application/json" \
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
               'https://localhost:8443/openidm/managed/user?_action=create'

               {"mail":"fdoe@example.com","sn":"Doe","telephoneNumber":"555-1234","userName":"fdoe","givenName":"Felicitas","description":"Felicitas Doe","displayName":"fdoe","accountStatus":"active","roles":["openidm-authorized"],"lastPasswordSet":"","postalCode":"","stateProvince":"","passwordAttempts":"0","lastPasswordAttempt":"Thu Apr 16 2015 02:45:17 GMT-0000 (UTC)","postalAddress":"","address2":"","country":"","city":"","effectiveRoles":[],"effectiveAssignments":{},"_id":"be30da0b-4c84-42c9-81bd-348a7779e840","_rev":"1"}

As can be seen from the result output, our new user has only one role: openidm-
authorized; this role is assigned automatically when new users are created.
But it is slightly different than the managed roles we've dealt with so far.
It is an internal role used for authorization decisions inside OpenIDM.

So, how do we assign the Employee role to this new user who is now on the
payroll? We need to update the user entry via a PATCH request and add
a pointer to the role itself:

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
               'https://localhost:8443/openidm/managed/user/be30da0b-4c84-42c9-81bd-348a7779e840'

               {"mail":"fdoe@example.com","sn":"Doe","telephoneNumber":"555-1234","userName":"fdoe","givenName":"Felicitas","description":"Felicitas Doe","displayName":"fdoe","accountStatus":"active","roles":["openidm-authorized",["managed/role/Employee"]],"lastPasswordSet":"","postalCode":"","stateProvince":"","passwordAttempts":"0","lastPasswordAttempt":"Thu Apr 16 2015 02:45:17 GMT-0000 (UTC)","postalAddress":"","address2":"","country":"","city":"","effectiveRoles":[{"_ref","managed/role/Employee"}],"effectiveAssignments":{},"_id":"be30da0b-4c84-42c9-81bd-348a7779e840","_rev":"2"}

Let's take a closer look at Felicitas' entry using the query filter mechanism:

        $ curl --insecure \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request GET \
               'https://localhost:8443/openidm/managed/user?_queryFilter=/givenName+eq+"Felicitas"&_fields=_id,userName,roles,effectiveRoles&_prettyPrint=true'

               {
                 "result" : [ {
                   "_id" : "be30da0b-4c84-42c9-81bd-348a7779e840",
                   "userName" : "fdoe",
                   "roles" : [ "openidm-authorized", "managed/role/Employee" ],
                   "effectiveRoles" : [ { "_ref" : "managed/role/Employee" } ]
                 } ],
                 "resultCount" : 1,
                 "pagedResultsCookie" : null,
                 "remainingPagedResults" : -1
               }

We can see that the Employee role is now part of Felicitas' _roles_ attribute.
But we also observe that the same role is present in the _effectiveRoles_
attribute as well. This attribute is calculated based on the roles assigned
to the user.

4. Removing the Employee role from a user

Let's imagine that the Employee role was erroneously assigned to Felicitas.
We now need to remove this role from her entry. To do that we need to use
a JSON Patch _remove_ operation. Since the role is part of an array, and is
actually the 2nd element of that array (see output in section 3 above) the
resulting PATCH request would look as follows: 

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
               'https://localhost:8443/openidm/managed/user/be30da0b-4c84-42c9-81bd-348a7779e840'

               {"mail":"fdoe@example.com","sn":"Doe","telephoneNumber":"555-1234","userName":"fdoe","givenName":"Felicitas","description":"Felicitas Doe","displayName":"fdoe","accountStatus":"active","roles":["openidm-authorized"],"lastPasswordSet":"","postalCode":"","stateProvince":"","passwordAttempts":"0","lastPasswordAttempt":"Thu Apr 16 2015 02:45:17 GMT-0000 (UTC)","postalAddress":"","address2":"","country":"","city":"","effectiveRoles":[],"_id":"be30da0b-4c84-42c9-81bd-348a7779e840","_rev":"5","effectiveAssignments":{}}

Our user now only has the original _openidm-authorized_ role and has lost the
_Employee_ role.

5. Deleting the Contractor role

The final step in role management is to remove an existing role. This is what
we will do with the Contractor role. But first let's take a look at what would
happen if we tried to remove a role already assigned to a user.

Let's, temporarily, add the Contractor role to Felicitas -- notice the role
with server-side generated identifier:

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
                       "value" : "managed/role/c22316ba-2096-4272-af10-9b17c17e555b"
                   }
                 ]' \
               'https://localhost:8443/openidm/managed/user/be30da0b-4c84-42c9-81bd-348a7779e840'

               {"mail":"fdoe@example.com","sn":"Doe","telephoneNumber":"555-1234","userName":"fdoe","givenName":"Felicitas","description":"Felicitas Doe","displayName":"fdoe","accountStatus":"active","roles":["openidm-authorized","managed/role/c22316ba-2096-4272-af10-9b17c17e555b"],"lastPasswordSet":"","postalCode":"","stateProvince":"","passwordAttempts":"0","lastPasswordAttempt":"Thu Apr 16 2015 02:45:17 GMT-0000 (UTC)","postalAddress":"","address2":"","country":"","city":"","effectiveRoles":[{"_ref":"managed/role/c22316ba-2096-4272-af10-9b17c17e555b"}],"_id":"be30da0b-4c84-42c9-81bd-348a7779e840","_rev":"6","effectiveAssignments":{}}

Now let's try and delete the Contractor role:

        $ curl --insecure \
               --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request DELETE \
               'https://localhost:8443/openidm/managed/role/c22316ba-2096-4272-af10-9b17c17e555b'

               {"code":409,"reason":"Conflict","message":"Cannot delete a role that is currently assigned"}

So, we cannot delete a role that has been assigned to one or more users. 
We must first deallocate that role from the user entry:


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
               'https://localhost:8443/openidm/managed/user/be30da0b-4c84-42c9-81bd-348a7779e840'

               {"mail":"fdoe@example.com","sn":"Doe","telephoneNumber":"555-1234","userName":"fdoe","givenName":"Felicitas","description":"Felicitas Doe","displayName":"fdoe","accountStatus":"active","roles":["openidm-authorized"],"lastPasswordSet":"","postalCode":"","stateProvince":"","passwordAttempts":"0","lastPasswordAttempt":"Thu Apr 16 2015 02:45:17 GMT-0000 (UTC)","postalAddress":"","address2":"","country":"","city":"","effectiveRoles":[],"_id":"be30da0b-4c84-42c9-81bd-348a7779e840","_rev":"7","effectiveAssignments":{}}

Now let's try and delete the Contractor role again:

        $ curl --insecure \
               --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request DELETE \
               'https://localhost:8443/openidm/managed/role/c22316ba-2096-4272-af10-9b17c17e555b'

               {"properties":{"name":"Contractor","description":"Role assigned to contract workers."},"_id":"c22316ba-2096-4272-af10-9b17c17e555b","_rev":"1"}

Let's verify that the Contractor role has been deleted:

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
                 } ],
                 "resultCount" : 1,
                 "pagedResultsCookie" : null,
                 "remainingPagedResults" : -1
               }


Managing Roles from the Admin UI
--------------------------------

All management operations on Roles performed above via REST are possible via
the Admin UI.

1. Create the Contractor role again

Login to the Admin UI at:
https://localhost:8443/admin
(or whatever applies for the hostname where OpenIDM is deployed.)

If you are using the default credentials provided with the product for the
Administrative User, you must use:
username : openidm-admin 
password : openidm-admin

Once authenticated, on the Resources page you will scroll down until you see
the "Managed Objects" section. A "Role - Managed Object" card should be
present. Click on three vertical dots and select "Data".

You should now see a "Role List" page that contains a table with the Employee
role listed as one of the items (only one if you've followed all the steps
above).

To create the Contractor role again, click the "Add Role" button on the right
corner. You should now be able to enter the new role name: Contractor, and
the role description: Role assigned to contract workers.

Then simply click the "Create" button, and voilà! You're done creating a role.

2. Reading (and searching) roles

The "Role List" gives you the ability to see all the roles that have been
created and also to filter based on the name of the role.

By clicking on each role name, you're able to see the details of each role.

Selecting "Back to Roles" under a role properties page brings you back to the
table that lists all the roles.

3. Assigning the Contractor role to a user

This time we will assign the Contractor role to Felicitas. From the list of
roles, click on the Contractor role, then click on the "Users" tab. The list
of users allows you to select the user(s) for which the role should be
assigned.

In our case we want to select fdoe (Felicitas entry) and we need to click on
"Add Users". Felicitas entry will now be showing with a checkmark in the
"Has Role" column.

You can also verify that Felicitas has the Contractor role by selecting
the "Data Management View" from the top right corner menu. Click on the user tab
to see the list of users present in the system. Select Felicitas' entry. 
The profile should show the Contractor role with a checkmark.

Another way to add the Contractor role to the user is simply to select the 
required role from the user's profile page, and then click Update.

For example, select the "Employee" role from Felicitas' profile page and click
"Save". She now has both the Contractor and the Employee role. Surely we must
be mistaken!

4. Removing the Employee role from a user

By unchecking the Employee role from Felicitas' profile page we can easily
remove the Employee role from her entry (click "Save"!)

We can also go back to the Admin View (top right corner) and select "Data" from
the 3 vertical dots of the managed roles card.

At this point, just click on the Contractor role (for example) from the list of
roles and once on the Contractor Role page, click on the "Users" tab.

By selecting "fdoe" entry and clicking on "Remove Users" you will remove the
Contractor role from Felicitas' entry as well.

5. Deleting the Contractor role.

Go back to the "Role List" page by clicking on "Back to Roles" and select the
Contractor role (checkmark, don't click on the role itself). Click on "Delete
Selected" and the Contractor role will be deleted.


See the other samples for more information on roles! 
