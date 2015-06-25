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

Multi-Account Linking : Solving the multiple identity connundrum
================================================================

The sample provided in this directory illustrates how OpenIDM addresses the
issue of linking multiple accounts back to one identity.

This use case is very common in the insurance business where a company,
let's name it Example.com, employs agents to sell insurance policies to their
customers and most, if not all, of their agents are also insured customers.

The first section below will guide you through the steps required to generate
2 different accounts in the ldap directory service (OpenDJ here) from one
identity created in Managed User (OpenIDM's own user repository) based on the
roles assigned to that identity.

The second use case will allow you to understand how roles and multi-account
linking can operate together to assign attributes to only one category of
accounts ; after all you wouldn't want your insured customers to be able to
write their own policies, would you ?


Generating 2 user spaces : `insured` and `agent` qualifiers
-----------------------------------------------------------

In order to maintain a link between source identities and accounts created
in target resources, OpenIDM maintains a "links" table. This table also allows
the same source entry to be linked to multiple target entries via something
called a "link qualifier".

For more in this please refer to the documentation :

http://openidm.forgerock.org/doc/bootstrap/integrators-guide/#mapping-link-qualifiers
http://openidm.forgerock.org/doc/bootstrap/integrators-guide/#admin-ui-resource-mapping

Let's go back to our example. We will use 2 link qualifiers :
  * `insured`, to represent the accounts created for **customers** of
    Example.com created in the container : `ou=Customers,dc=example,dc=com`
  * `agent`, to represent **agents**, considered independent contractors,
    and created under the container : `ou=Contractors,dc=example,dc=com`

Agents might use one portal and insured customers another ; each get access to a
different set of features based on the portal. While the accounts are different,
some of the information that pertains to the identity should be consistent
between the 2 accounts.

1.  Running the sample

Let's see how we can populate agents and insured customers accounts in the ldap
directory server from MU.

Because we don't want to create a customer and an agent account for each
identity stored in MU we will base our decision on what type of role was
assigned to that identity :
  * Insured role assigned --> an insured customer account will be created
  * Agent role assigned --> an agent account will be created 
  * both roles assigned ??? --> both accounts will be created
  
We've compiled all the necessary information in the provided _sync.json_ , the
_provisioner.openicf-ldap.json_ and the _Example.ldif_. You can run the sample,
after loading the ldif into your directory (follow the steps in sample 2) by
providing the following command :

    $ ./startup.sh -p samples/multiaccountlinking

Once the ldif is loaded and OpenIDM started, we need to populate several entries
to accomplish the scenario described above.

a. Creating Roles

First off let's create the 2 roles that will allow us to select which "category"
an identity falls in :

    $ curl --insecure \
           --header "Content-type: application/json" \
           --header "X-OpenIDM-Username: openidm-admin" \
           --header "X-OpenIDM-Password: openidm-admin" \
           --request POST \
           --data '{
               "properties" : {
                 "name" : "Agent",
                 "description" : "Role assigned to insurance agents."
               }
           }' \
           https://localhost:8443/openidm/managed/role?_action=create
    
    {"properties":{"name":"Agent","description":"Role assigned to insurance agents."},"_id":"e26b6d30-121c-479c-b094-7b02e166447c","_rev":"1"}


    $ curl --insecure \
           --header "Content-type: application/json" \
           --header "X-OpenIDM-Username: openidm-admin" \
           --header "X-OpenIDM-Password: openidm-admin" \
           --request POST \
           --data '{
               "properties" : {
                 "name" : "Insured",
                 "description" : "Role assigned to insured customers."
               }
           }' \
           https://localhost:8443/openidm/managed/role?_action=create

    {"properties":{"name":"Insured","description":"Role assigned to insured customers."},"_id":"2d11b41b-d77d-4cac-b6c2-9049c635d930","_rev":"1"}

**Note: make sure you pay attention to the returned _id_ of the roles created
as we will use those in the following steps.**

Those role definition are pretty simple for now. We will enhance those later
when we use them to provision additional attributes to the ldap directory
server.

b. Creating Users

We are now going to create our 2 favorite users in MU via the following curl
commands :

    $ curl --insecure \
           --header "Content-type: application/json" \
           --header "X-OpenIDM-Username: openidm-admin" \
           --header "X-OpenIDM-Password: openidm-admin" \
           --request POST \
           --data '{
               "displayName" : "Barbara Jensen",
               "description" : "Created for OpenIDM",
               "givenName" : "Barbara",
               "mail" : "bjensen@example.com",
               "telephoneNumber" : "1-360-229-7105",
               "sn" : "Jensen",
               "userName" : "bjensen",
               "accountStatus" : "active",
               "roles" : [
                 "openidm-authorized"
               ],
               "postalCode" : "",
               "stateProvince" : "",
               "postalAddress" : "",
               "address2" : "",
               "country" : "",
               "city" : ""
           }' \
           https://localhost:8443/openidm/managed/user?_action=create

    {"displayName":"Barbara Jensen","description":"Created for OpenIDM","givenName":"Barbara","mail":"bjensen@example.com","telephoneNumber":"1-360-229-7105","sn":"Jensen","userName":"bjensen","accountStatus":"active","roles":["openidm-authorized"],"postalCode":"","stateProvince":"","postalAddress":"","address2":"","country":"","city":"","lastPasswordSet":"","passwordAttempts":"0","lastPasswordAttempt":"Fri May 29 2015 11:19:40 GMT-0500 (CDT)","effectiveRoles":["openidm-authorized"],"effectiveAssignments":{},"_id":"9d38b5bf-b81e-4aee-b31e-7797efdef8b5","_rev":"1"}

    $ curl --insecure \
           --header "Content-type: application/json" \
           --header "X-OpenIDM-Username: openidm-admin" \
           --header "X-OpenIDM-Password: openidm-admin" \
           --request POST \
           --data '{
               "displayName": "John Doe",
               "description": "Created for OpenIDM",
               "givenName": "John",
               "mail": "jdoe@example.com",
               "telephoneNumber": "1-415-599-1100",
               "sn": "Doe",
               "userName": "jdoe",
               "accountStatus": "active",
               "roles": [
                 "openidm-authorized"
               ],
               "postalCode": "",
               "stateProvince": "",
               "postalAddress": "",
               "address2": "",
               "country": "",
               "city": ""
           }' \
           https://localhost:8443/openidm/managed/user?_action=create

    {"displayName":"John Doe","description":"Created for OpenIDM","givenName":"John","mail":"jdoe@example.com","telephoneNumber":"1-415-599-1100","sn":"Doe","userName":"jdoe","accountStatus":"active","roles":["openidm-authorized"],"postalCode":"","stateProvince":"","postalAddress":"","address2":"","country":"","city":"","lastPasswordSet":"","passwordAttempts":"0","lastPasswordAttempt":"Fri May 29 2015 11:22:08 GMT-0500 (CDT)","effectiveRoles":["openidm-authorized"],"effectiveAssignments":{},"_id":"9736f2c6-0103-4c48-98b5-a7c189297107","_rev":"1"}

c. Granting Roles to Users

This is the step where things will start happening. We are first granting the
role `Agent` (e26b6d30-121c-479c-b094-7b02e166447c) to the user `jdoe`
(9736f2c6-0103-4c48-98b5-a7c189297107) :

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
                       "value" : "managed/role/e26b6d30-121c-479c-b094-7b02e166447c"
                   }
                 ]' \
               'https://localhost:8443/openidm/managed/user/9736f2c6-0103-4c48-98b5-a7c189297107'
               
    {"displayName":"John Doe","description":"Created for OpenIDM","givenName":"John","mail":"jdoe@example.com","telephoneNumber":"1-415-599-1100","sn":"Doe","userName":"jdoe","accountStatus":"active","roles":["openidm-authorized","managed/role/e26b6d30-121c-479c-b094-7b02e166447c"],"postalCode":"","stateProvince":"","postalAddress":"","address2":"","country":"","city":"","lastPasswordSet":"","passwordAttempts":"0","lastPasswordAttempt":"Fri May 29 2015 11:22:08 GMT-0500 (CDT)","effectiveRoles":["openidm-authorized","managed/role/e26b6d30-121c-479c-b094-7b02e166447c"],"effectiveAssignments":{},"_id":"9736f2c6-0103-4c48-98b5-a7c189297107","_rev":"2"}
    
and now we're granting the role `Insured` (2d11b41b-d77d-4cac-b6c2-9049c635d930)
to the user `bjensen` (9d38b5bf-b81e-4aee-b31e-7797efdef8b5) :

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
                       "value" : "managed/role/2d11b41b-d77d-4cac-b6c2-9049c635d930"
                   }
                 ]' \
               'https://localhost:8443/openidm/managed/user/9d38b5bf-b81e-4aee-b31e-7797efdef8b5'

    {"displayName":"Barbara Jensen","description":"Created for OpenIDM","givenName":"Barbara","mail":"bjensen@example.com","telephoneNumber":"1-360-229-7105","sn":"Jensen","userName":"bjensen","accountStatus":"active","roles":["openidm-authorized","managed/role/2d11b41b-d77d-4cac-b6c2-9049c635d930"],"postalCode":"","stateProvince":"","postalAddress":"","address2":"","country":"","city":"","lastPasswordSet":"","passwordAttempts":"0","lastPasswordAttempt":"Fri May 29 2015 11:19:40 GMT-0500 (CDT)","effectiveRoles":["openidm-authorized","managed/role/2d11b41b-d77d-4cac-b6c2-9049c635d930"],"effectiveAssignments":{},"_id":"9d38b5bf-b81e-4aee-b31e-7797efdef8b5","_rev":"2"}


As a result, you should now have 2 new accounts created in the ldap directory
server. A sync event pushed out those new accounts, based on the multi-account
linking rules we've specified in the _sync.json_ :
  * one accounts under the `ou=Customers,dc=example,dc=com` container for the
    insured customer `bjensen`
  * one accounts under the `ou=Contractors,dc=example,dc=com` container for the
    insurance agents `jdoe`.

If you want to produce the necessary configuration step-by-step, then follow
the next section. Otherwise, you're done with the first part of this sample.

Congratulations, you've just created accounts in different areas of the DIT
(Directory Information Tree) based on the role(s) granted to source identities !


2. Building the sample : Link qualifiers and transformation 

The first part in establishing multi-account linking is to **define** the link
qualifiers that correspond to the categories of account you wish to create.

In our insurance example, we decided on :
  * insured
  * agent

You can create those by adding the `linkQualifiers` element in the _sync.json_,
under the `managedUser_systemLdapAccounts` mapping :

    {
      "name" : "managedUser_systemLdapAccounts",
      "source" : "managed/user",
      "target" : "system/ldap/account",
      "linkQualifiers" : [
        "insured",
        "agent"
      ],
      .....
    }

or you can use the Administration UI, under the `managedUser_systemLdapAccounts`
mapping, in the _Properties_ tab, click on _Link Qualifiers_ and add each tag
one at a time in the text box (don't forget to save !).

The second step is to define in the _Attributes Grid_ a transformation script
for the `dn` attribute. Click _Add Property_, then type `dn` and enter the
transformation script in the inline text box :

    if (linkQualifier === 'agent') { 'uid=' + source.userName + ',ou=Contractors,dc=example,dc=com'; } else if (linkQualifier === 'insured') { 'uid=' + source.userName + ',ou=Customers,dc=example,dc=com'; }

Similar to what was done before though, you can also add the attribute
definition to the _sync.json_ directly as shown by the snippet below :

    {
      "target" : "dn",
      "transform" : {
        "type" : "text/javascript",
        "globals" : { },
        "source" : "if (linkQualifier === 'agent') { 'uid=' + source.userName + ',ou=Contractors,dc=example,dc=com'; } else if (linkQualifier === 'insured') { 'uid=' + source.userName + ',ou=Customers,dc=example,dc=com'; }"
      },
      "source" : ""
    }

This is the logic that defines where each account will be created in the DIT,
based on the category of account we need to consider : agent, insured or both.

Because we want the following pattern to be applied :
  * identity with role `Agent` --> account created in container `ou=Contractors`
  * identity with role `Insured` --> account created in container
    `ou=Customers`
we need to define a validSource script that will sort through the identities
to consider ; we also need to define a proper correlation query that will
recognize accounts from each category in case they already exist in the target
system.

            "correlationQuery" : [
                {
                    "linkQualifier" : "insured",
                    "type" : "text/javascript",
                    "globals" : { },
                    "source" : "var map = {'_queryFilter': 'dn eq \\\"uid=' + source.userName + ',ou=Customers,dc=example,dc=com\\\"'}; map;"
                },
                {
                    "linkQualifier" : "agent",
                    "type" : "text/javascript",
                    "globals" : { },
                    "source" : "var map = {'_queryFilter': 'dn eq \\\"uid=' + source.userName + ',ou=Contractors,dc=example,dc=com\\\"'}; map;"
                }
            ],......
            
The structure for the correlation query is fairly simple : it specifies a link
qualifier (insured or agent) and for each one it defines a script that verifies
if the DN of the entry fits the right container (Customers or Contractors,
respectively).

Now you might be wondering how we can avoid specifying the structure of the `dn`
attribute in 2 places in the _sync.json_. One way to achieve that (which is
also possible to define through the UI) is to leverage the expression builder
to reuse the construct defined in the `dn` mapping :

            "correlationQuery" : [
                {
                    "linkQualifier" : "insured",
                    "expressionTree" : {
                        "all" : [
                            "dn"
                        ]
                    },
                    "mapping" : "managedUser_systemLdapAccounts",
                    "type" : "text/javascript",
                    "file" : "ui/correlateTreeToQueryFilter.js"
                },
                {
                    "linkQualifier" : "agent",
                    "expressionTree" : {
                        "all" : [
                            "dn"
                        ]
                    },
                    "mapping" : "managedUser_systemLdapAccounts",
                    "type" : "text/javascript",
                    "file" : "ui/correlateTreeToQueryFilter.js"
                }
            ],

This allows us to rely on the how the `dn` structure is built in the attribute
mapping. In effect this is equivalent to saying : dn must match the value as
defined in the properties definition.

Note : with the current implementation of multi-account linking, we need to
make sure we can segregate which account will get provisioned based on the
current link qualifier and role. This is where the _validSource_ script comes
into play :

            "validSource" : {
                "type" : "text/javascript",
                "globals" : { },
                "source" : "var res = false;
                            var i=0;
                            
                            while (!res && i < source.effectiveRoles.length) {
                                var roleId = source.effectiveRoles[i];
                                if (roleId != null && roleId.indexOf("/") != -1) {
                                    var roleInfo = openidm.read(roleId);
                                    logger.warn("Role Info : {}",roleInfo);
                                    res = (((roleInfo.properties.name === 'Agent')
                                            &&(linkQualifier ==='agent'))
                                          || ((roleInfo.properties.name === 'Insured')
                                            &&(linkQualifier ==='insured')));
                                }
                                i++;
                            }
                            
                            res"
            }


The validSource script looks through the effective roles of a user and figures
out if the user has the `Agent` or the `Insured` role, while at the same time
making sure that the source is considered **only** for the matching category.

At this point we have all we need to create our users in MU and see the impact
in the ldap directory server. Just run the commands, in the previous chapter,
which created the 2 roles (1.a.), the 2 users (1.b.) and the associated
grants (1.c.).


Using Roles in conjunction with multi-account linking
-----------------------------------------------------

This use case illustrates the fact that different accounts will most likely have
different functions in the target systems. For example, agents might be part
of a Contractor group, while insured customers will not : they might be part
of a Chat Users group (to get in touch with their favorite insurance agent).

While an agent might also be an insured customer, it is not desirable that the
`customer` account inherits the same properties (or memberships) as the `agent`
account. Therefore we need to insure that role based assignments are only
given to the correct account.

Roles and link qualifiers allow the addition of a condition in the assignment
of attributes.

Let's update the `Agent` and `Insured` roles to include those specific
assignments  :

        $ curl --insecure \
               --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --header "If-Match: *" \
               --request PATCH \
               --data '[{
                   "operation" : "add",
                   "field" : "/assignments",
                   "value" : {
                     "ldap": {
                       "attributes": [
                         {
                           "name": "ldapGroups",
                           "value": [
                               "cn=Contractors,ou=Groups,dc=example,dc=com"
                               ],
                           "assignmentOperation" : "mergeWithTarget",
                           "unassignmentOperation" : "removeFromTarget"
                         }
                       ],
                       "linkQualifiers": ["agent"]
                     }
                   }
               }]' \
               https://localhost:8443/openidm/managed/role/e26b6d30-121c-479c-b094-7b02e166447c

    {"properties":{"name":"Agent","description":"Role assigned to insurance agents."},"_id":"e26b6d30-121c-479c-b094-7b02e166447c","_rev":"2","assignments":{"ldap":{"attributes":[{"name":"ldapGroups","value":["cn=Contractors,ou=Groups,dc=example,dc=com"],"assignmentOperation":"mergeWithTarget","unassignmentOperation":"removeFromTarget"}],"linkQualifiers":["agent"]}}}

Notice the `linkQualifiers` element in the ldap assignment. This acts like
a condition on the assignment of the _Contractors_ group. If the sync or recon
is happening for the link qualifier _agent_ then the target account for `jdoe`
will be assigned the group _Contractor_. If not, then no attribute assignment
will be effective.

        $ curl --insecure \
               --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --header "If-Match: *" \
               --request PATCH \
               --data '[{
                   "operation" : "add",
                   "field" : "/assignments",
                   "value" : {
                     "ldap": {
                       "attributes": [
                         {
                           "name": "ldapGroups",
                           "value": [
                               "cn=Chat Users,ou=Groups,dc=example,dc=com"
                               ],
                           "assignmentOperation" : "mergeWithTarget",
                           "unassignmentOperation" : "removeFromTarget"
                         }
                       ],
                       "linkQualifiers": ["insured"]
                     }
                   }
               }]' \
               https://localhost:8443/openidm/managed/role/2d11b41b-d77d-4cac-b6c2-9049c635d930

     {"properties":{"name":"Insured","description":"Role assigned to insured customers."},"_id":"2d11b41b-d77d-4cac-b6c2-9049c635d930","_rev":"2","assignments":{"ldap":{"attributes":[{"name":"ldapGroups","value":["cn=Chat Users,ou=Groups,dc=example,dc=com"],"assignmentOperation":"mergeWithTarget","unassignmentOperation":"removeFromTarget"}],"linkQualifiers":["insured"]}}}
        
        
While the assignment now shows up in jdoe's and bjensen's entry, with the link
qualifier, we need to do one last thing to make all this work before we run a
reconciliation.

We need to associate the assignments to the mapping. So in the _sync.json_ we
will add the following element as part of the `managedUser_systemLdapAccounts`
mapping :

        "assignmentsToMap" : [
           "ldap"
        ]

You can now launch a reconciliation for this mapping :

        $ curl --insecure \
               --header "Content-type: application/json" \
               --header "X-OpenIDM-Username: openidm-admin" \
               --header "X-OpenIDM-Password: openidm-admin" \
               --request POST \
               'https://localhost:8443/openidm/recon?_action=recon&mapping=managedUser_systemLdapAccounts'
        
        
        {"_id":"a378413c-8f88-473c-8cd3-847a7db80f2c","state":"ACTIVE"}
        

**Note :** adding the assignmensToMap element is only necessary if you followed
the steps to build the sample from scratch. Otherwise if you're using the
_multiaccountlinking_ project to start openidm all of this is already done and
the entries should have been added to the proper groups immediately.

Summary
-------

The account with the dn `uid=jdoe,ou=Contractors,dc=example,dc=com` should now
be part of the Contractors group in the ldap directory and the account with the
dn `uid=bjensen,ou=Customers,dc=example,dc=com` should be part of the Chat Users
group.

Now you should try and play with different combinations of the role grants and
see the impact on the ldap directory server entries.

And you can do that through the UI as well.

Enjoy !