++++
<!-- please include docs in any mod of this file
It's designed for "single sourcing" with the published docs -->

<!--
  ! CCPL HEADER START
  !
  ! This work is licensed under the Creative Commons
  ! Attribution-NonCommercial-NoDerivs 3.0 Unported License.
  ! To view a copy of this license, visit
  ! http://creativecommons.org/licenses/by-nc-nd/3.0/
  ! or send a letter to Creative Commons, 444 Castro Street,
  ! Suite 900, Mountain View, California, 94041, USA.
  !
  ! You can also obtain a copy of the license at
  ! legal/CC-BY-NC-ND.txt.
  ! See the License for the specific language governing permissions
  ! and limitations under the License.
  !
  ! If applicable, add the following below this CCPL HEADER, with the fields
  ! enclosed by brackets "[]" replaced with your own identifying information:
  !      Portions Copyright [yyyy] [name of copyright owner]
  !
  ! CCPL HEADER END
  !
  !      Copyright 2015 ForgeRock AS
  !
-->
<chapter xml:id='chap-multiaccount-sample'
         xmlns='http://docbook.org/ns/docbook'
         version='5.0' xml:lang='en'
         xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'
         xsi:schemaLocation='http://docbook.org/ns/docbook
         http://docbook.org/xml/5.0/xsd/docbook.xsd'
         xmlns:xlink='http://www.w3.org/1999/xlink'>
++++

[[sample-multiaccount-linking]]
The Multi-Account Linking Sample
--------------------------------

The sample provided in the `samples/multiaccountlinking` directory illustrates how OpenIDM addresses links from multiple 
accounts to one identity.

This sample is based on a common use case in the insurance industry, where a company (Example.com) employs agents to 
sell policies to their insured customers. Most of their agents are also insured. These different roles are sometimes
known as the multi-account linking conundrum.

With minor changes, this sample works for other use cases. For example, you may have a hospital that employs doctors who 
treat patients. Some of their doctors are also patients of that hospital.

[[external-ldap-config-multiaccount]]
External LDAP Configuration
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Configure the LDAP server as for sample 2,
https://forgerock.org/openidm/doc/bootstrap/samples-guide/index.html#external-ldap-config-2[External LDAP Configuration^]

The LDAP user must have write access to create users from OpenIDM on the LDAP server. When you configure the LDAP 
server, import the appropriate LDIF file, in this case, _openidm/samples/multiaccountlinking/data/Example.ldif_.

[[install-sample-multiaccount]]
Install the Sample
------------------

Prepare OpenIDM as described in
https://forgerock.org/openidm/doc/bootstrap/samples-guide/index.html#preparing-openidm[Preparing OpenIDM^],
then start OpenIDM with the following configuration for the Multi-Account Linking sample.

++++
<screen>$ <userinput>cd /path/to/openidm</userinput></screen>
<screen>$ <userinput>./startup.sh -p samples/multiaccountlinking</userinput></screen>
++++

[[multiaccount-create-users]]
Create New Identities for the Sample
------------------------------------

For the purpose of this sample, create identities for users John Doe and Barbara Jensen. To create these identities from 
the Admin UI, navigate to `https://localhost:8443/admin` and click Manage > User > New User.

Alternatively, use the following REST calls to set up identities for the noted users:

++++

<screen>$ <userinput>curl \
--cacert self-signed.crt \
--header "Content-Type: application/json" \
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
  "accountStatus" : "active"
}' \
"https://localhost:8443/openidm/managed/user?_action=create"</userinput></screen>

<screen>$ <userinput>curl \
--cacert self-signed.crt \
--header "Content-Type: application/json" \
--header "X-OpenIDM-Username: openidm-admin" \
--header "X-OpenIDM-Password: openidm-admin" \
--request POST \
--data '{
  "displayName" : "John Doe",
  "description" : "Created for OpenIDM",
  "givenName" : "John",
  "mail" : "jdoe@example.com",
  "telephoneNumber" : "1-415-599-1100",
  "sn" : "Doe",
  "userName" : "jdoe",
  "accountStatus" : "active"
}' \
"https://localhost:8443/openidm/managed/user?_action=create"</userinput></screen>

++++

In the output, you will see an ID number associated with each user, in the following format:

++++
<screen> "_id" : "35d0a49d-2571-401f-b429-96c66b23a1c0",</screen>
++++

Record the `_id` number for each user. You will use those numbers to assign desired roles for each user.

[[multiaccount-create-roles]]
Create New Roles for the Sample
-------------------------------

For this sample, to set up links for multiple accounts on OpenIDM, you need to set up roles. To do so, set up roles for 
`Agent` and `Insured`. To create these roles in the Admin UI, navigate to `https://localhost:8443/admin` and click
Manage > Role > New Role.

Alternatively, use the following REST calls to set up the `Agent` and `Insured` roles:

++++

<screen>$ <userinput>curl \
--cacert self-signed.crt \
--header "Content-Type: application/json" \
--header "X-OpenIDM-Username: openidm-admin" \
--header "X-OpenIDM-Password: openidm-admin" \
--request POST \
--data '{
  "name" : "Agent",
  "description" : "Role assigned to insurance agents."
}' \
"https://localhost:8443/openidm/managed/role?_action=create"</userinput></screen>

<screen>$ <userinput>curl \
--cacert self-signed.crt \
--header "Content-Type: application/json" \
--header "X-OpenIDM-Username: openidm-admin" \
--header "X-OpenIDM-Password: openidm-admin" \
--request POST \
--data '{
  "name" : "Insured",
  "description" : "Role assigned to insured customers."
}' \
"https://localhost:8443/openidm/managed/role?_action=create"</userinput></screen>

++++

Do record the `_id` output for the `Agent` and `Insured` roles. You will use those numbers to assign desired roles for each
user.

NOTE: While you could use `PUT` to create roles with descriptive names, we recommend that you use `POST` to create roles 
with immutable IDs.

[[multiaccount-assign-roles]]
Assign Roles to Appropriate Users
---------------------------------

Now you can assign roles to appropriate users. To review, user `jdoe` is an `Agent` and user `bjensen` is `Insured`.

You will need the `_id` value for each user. The `_id` values shown in the following commands are random; substitute the
`_id` values that you collected when creating users.

The following command adds the `Agent` role to user `jdoe`, by their `_id` values:

++++
<screen>$ <userinput>curl \
--cacert self-signed.crt \
--header "Content-type: application/json" \
--header "X-OpenIDM-Username: openidm-admin" \
--header "X-OpenIDM-Password: openidm-admin" \
--header "If-Match: *" \
--request PATCH \
--data '[
    {
      "operation" : "add",
      "field" : "/roles/-",
      "value" : {
        "_ref" : "managed/role/287dc4b1-4b19-49ec-8b4c-28a6c12ede34"
      }
    }
  ]' \
"https://localhost:8443/openidm/managed/user/8fae84ed-1f30-4542-8087-e7fa6e89541c"</userinput></screen>
++++

And this next command adds the `Insured` role to user `bjensen`:

++++
<screen>$ <userinput>curl \
--cacert self-signed.crt \
--header "Content-type: application/json" \
--header "X-OpenIDM-Username: openidm-admin" \
--header "X-OpenIDM-Password: openidm-admin" \
--header "If-Match: *" \
--request PATCH \
--data '[
    {
      "operation" : "add",
      "field" : "/roles/-",
      "value" : {
        "_ref" : "managed/role/bb9302c4-5fc1-462c-8be2-b17c87175d1b"
      }
    }
  ]' \
"https://localhost:8443/openidm/managed/user/d0b79f30-946f-413a-b7d1-d813034fa345"</userinput></screen>
++++

Now assign the `Insured` role to user `jdoe`, as that user is both an insured customer and
an agent:

++++
<screen>$ <userinput>curl \
--cacert self-signed.crt \
--header "Content-type: application/json" \
--header "X-OpenIDM-Username: openidm-admin" \
--header "X-OpenIDM-Password: openidm-admin" \
--header "If-Match: *" \
--request PATCH \
--data '[
    {
      "operation" : "add",
      "field" : "/roles/-",
      "value" : {
        "_ref" : "managed/role/006935c2-b080-45cd-8347-881df42cae0c"
      }
    }
  ]' \
"https://localhost:8443/openidm/managed/user/a3335177-7366-4656-a66c-8d6e77a5786f"</userinput></screen>
++++

User `jdoe` should now have two managed roles:

++++
<screen>...
"effectiveRoles" : [ {
  "_ref" : "managed/role/6aabe990-ec05-403e-bc5d-ff9b217ba571",
  "_refProperties" : {
    "_id" : "687714b8-5854-42c7-a190-c781ea5174c5",
    "_rev" : "1"
  }
}, {
  "_ref" : "managed/role/844110ce-3686-43bb-aabf-46b17a14abaa"
} ],
...</screen>
++++

[[multiaccount-background]]
Background: Link Qualifiers, Agents, and Insured Customers
----------------------------------------------------------

This is a good moment to take a step back, to see how this sample works, based on custom options in the `sync.json` 
configuration file.

OpenIDM defines mappings between source and target accounts in the `sync.json` file, which allows you to create a link 
between one source entry and multiple target entries using a concept known as a "link qualifier," which enables 
one-to-many relationships in mappings and policies.

For more information on resource mappings and link qualifiers, see the following sections of the Integrator's Guide:

https://forgerock.org/openidm/doc/bootstrap/integrators-guide/index.html#synchronization-mappings-file[Configuring the Synchronization Mapping]

https://forgerock.org/openidm/doc/bootstrap/integrators-guide/index.html#mapping-link-qualifiers[Adding Link Qualifiers to a Mapping]

In this sample, we use two link qualifiers:

* `insured` represents the insured customer accounts associated with Example.com, created under the following LDAP container:
    `ou=Customers,dc=example,dc=com`
* `agent` represents agent accounts, considered independent contractors, and created under the following LDAP container:
    `ou=Contractors,dc=example,dc=com`

Assume that agents and insured customers connect via two different portals. Each group gets access to different features,
depending on the portal.

Agents may have two different accounts; one each for professional and personal use. While the accounts are different, 
the identity information for each agent should be the same for both accounts.

To that end, this sample sets up link qualifiers for two categories of users: `insured` and `agent`, under the 
`managedUser_systemLdapAccounts` mapping:

++++
<programlisting language="javascript">{
  "name" : "managedUser_systemLdapAccounts",
  "source" : "managed/user",
  "target" : "system/ldap/account",
  "linkQualifiers" : [
    "insured",
    "agent"
  ],
  .....
}</programlisting>
++++

You can verify this in the Admin UI. Click Configure > Mappings > `managedUser_systemLdapAccounts` > Properties > 
Link Qualifiers. You should see `insured` and `agent` in the list of configured Link Qualifiers.

In addition, this sample also includes a transformation script between an LDAP Distinguished Name (`dn`) and the two 
categories of users. The following excerpt of the `sync.json` file includes that script:

++++
<programlisting language="javascript">{
   "target" : "dn",
   "transform" : {
      "type" : "text/javascript",
      "globals" : { },
      "source" :
         "if (linkQualifier === 'agent') {
            'uid=' + source.userName + ',ou=Contractors,dc=example,dc=com';
         } else if (linkQualifier === 'insured') {
            'uid=' + source.userName + ',ou=Customers,dc=example,dc=com';
         }"
},</programlisting>
++++

The following validSource script looks through the effective roles of a user, with two objectives:

* Determine whether the user has an `Agent` or `Insured` role, and segregates accounts
based on link qualfiers and roles.
* Uses the `effectiveRoles` property to determine whether a user has an
`Agent` or `Insured` role, based on the effective roles of that user.

++++
<programlisting language="javascript">"validSource" : {
  "type" : "text/javascript",
  "globals" : { },
  "source" : "var res = false;
    var i=0;

    while (!res &amp;&amp; i &lt; source.effectiveRoles.length) {
      var roleId = source.effectiveRoles[i];
      if (roleId != null &amp;&amp; roleId.indexOf("/") != -1) {
        var roleInfo = openidm.read(roleId);
        logger.warn("Role Info : {}",roleInfo);
        res = (((roleInfo.properties.name === 'Agent')
          &amp;&amp;(linkQualifier ==='agent'))
        || ((roleInfo.properties.name === 'Insured')
          &amp;&amp;(linkQualifier ==='insured')));
        }
        i++;
      }
      res"
}</programlisting>
++++

You can see how correlation queries are configured in the `sync.json` file.

The structure for the correlation query specifies one of two link qualifiers: insured or agent. For each link qualifier, 
the correlation query defines a script that verifies if the subject `dn` belongs in a specific container. For this 
sample, the container (`ou`) may be Customers or Contractors.

You can can avoid specifying the structure of the `dn` attribute in two places in the `sync.json` file with the 
following code, which leverages the expression builder to reuse the construct defined in the `dn` mapping:

++++
<programlisting language="javascript">"correlationQuery" : [
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
],</programlisting>
++++

You can also leverage the expression builder in the UI. Review how the UI illustrates the expression builder. To do so, 
click Configure > Mapping > select a mapping > Association > Association Rules. Edit either link qualifier. You will see 
how the expression builder is configured for this sample.

[[multiaccount-roles-update]]
Update Roles With Desired LDAP Attributes
-----------------------------------------

This use case illustrates how accounts frequently have different functions on target systems. For example, while agents 
may be members of a Contractor group, insured customers may be part of a Chat Users group (possibly for access to 
customer service).

While an agent may also be an insured customer, you do not want other `insured` accounts to have the same properties
(or memberships) as the `agent` account. In this sample, we ensure that OpenIDM limits role based assignments to the 
correct account.

With the following commands you will create two managed assignments which will be used by the `agent` and `insured`
roles.

Record the `_id` number for each user. You will use those numbers to assign desired roles for each user.

The following command will create an `agent` assignment.

++++
<screen>$ <userinput>curl \
  --cacert self-signed.crt \
  --header "Content-Type: application/json" \
  --header "X-OpenIDM-Username: openidm-admin" \
  --header "X-OpenIDM-Password: openidm-admin" \
  --request POST \
  --data '{
    "name" : "ldapAgent",
    "description" : "LDAP Agent Assignment",
    "mapping" : "managedUser_systemLdapAccounts",
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
  }' \
"https://localhost:8443/openidm/managed/assignment?_action=create"</userinput></screen>
++++

Now repeat the process for the `insured` assignment, with the value set to the `Chat Users` group:

++++
<screen>$ <userinput>curl \
  --cacert self-signed.crt \
  --header "Content-Type: application/json" \
  --header "X-OpenIDM-Username: openidm-admin" \
  --header "X-OpenIDM-Password: openidm-admin" \
  --request POST \
  --data '{
    "name" : "ldapCustomer",
    "description" : "LDAP Customer Assignment",
    "mapping" : "managedUser_systemLdapAccounts",
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
  }' \
"https://localhost:8443/openidm/managed/assignment?_action=create"</userinput></screen>
++++

Now you can add the created assignments to their respective roles.

Add the `insured` assignment to the insured customer role:

++++
<screen>$ <userinput>curl \
--cacert self-signed.crt \
--header "Content-type: application/json" \
--header "X-OpenIDM-Username: openidm-admin" \
--header "X-OpenIDM-Password: openidm-admin" \
--header "If-Match: *" \
--request PATCH \
--data '[
    {
      "operation" : "add",
      "field" : "/assignments/-",
      "value" : {
        "_ref" : "managed/assignment/ee5241b2-e571-4736-8fb2-6b9caa9d0554"
      }
    }
  ]' \
"https://localhost:8443/openidm/managed/role/287dc4b1-4b19-49ec-8b4c-28a6c12ede34"</userinput></screen>
++++

Add the agent assignment to the agent role:

++++
<screen>$ <userinput>curl \
--cacert self-signed.crt \
--header "Content-type: application/json" \
--header "X-OpenIDM-Username: openidm-admin" \
--header "X-OpenIDM-Password: openidm-admin" \
--header "If-Match: *" \
--request PATCH \
--data '[
    {
      "operation" : "add",
      "field" : "/assignments/-",
      "value" : {
        "_ref" : "managed/assignment/12927c5d-576f-491e-ba65-e228cd218947"
      }
    }
  ]' \
"https://localhost:8443/openidm/managed/role/bb9302c4-5fc1-462c-8be2-b17c87175d1b"</userinput></screen>
++++

[[multiaccountlinking-recon]]
Reconciling Managed Users to the External LDAP Server
-----------------------------------------------------

Now that you have loaded `Example.ldif` into OpenDJ, and have started OpenIDM, you can perform a reconciliation from 
the internal Managed Users repository to the external OpenDJ data store:

++++
<screen>$ <userinput>curl \
 --cacert self-signed.crt \
 --header "Content-Type: application/json" \
 --header "X-OpenIDM-Username: openidm-admin" \
 --header "X-OpenIDM-Password: openidm-admin" \
 --request POST \
 "https://localhost:8443/openidm/recon?_action=recon&amp;mapping=managedUser_systemLdapAccounts"</userinput>
</screen>
++++

With all of the preparation work that you have done, this reconciliation will create three new accounts on the external 
LDAP server:

 * Two accounts under the `ou=Customers,dc=example,dc=com` branch `dn` under the insured customers role, `bjensen` and 
    `jdoe`.
 * One account under the `ou=Contractors,dc=example,dc=com` branch `dn` under the insurance agents role, `jdoe`.

Congratulations, you have just created accounts in two different areas of the LDAP Directory Information Tree.

[[multilinking-review]]
Reviewing the Result
--------------------

You have already confirmed that user `bjensen` has a `insured` role, and user `jdoe` has both a `insured` and `agent`
role. You can confirm the same result in the Admin UI:

. Click Manage > Role. You should see both Agent and Insured in the Role List window that appears.
. Click Agent > Users. You should see that user `jdoe` is included as an Agent.
. Click Back to Roles > Insured > Users. You should see that users `bjensen` and
`jdoe` are included in the Insured role.
