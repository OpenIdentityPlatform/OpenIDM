++++
<!-- TODO: after conversion (AC) delete all info above, except xml version
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
TODO: AC, delete section xml:id line and section closing about 10 lines below
then in the last line sub article > chapter
-->
<chapter xml:id='sample-multiaccount-linking'
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

The sample provided in the `samples/multiaccountlinking` directory illustrates
how OpenIDM addresses links from multiple accounts to one identity.

This sample is based on a common use case in the insurance industry, where
a company (Example.com) employs agents to sell policies to their customers.
Most of their agents are also insured customers. These different roles are
sometimes known as the multi-account linking conundrum.

With minor changes, this sample works for other use cases. For example,
you may have a hospital that employs doctors who treat patients. Some of
their doctors are also patients of that hospital.

We have subdivided the documentation of this sample into several
"big picture" sections:

. Configure OpenDJ as an external LDAP server.<<external-ldap-config-multiaccount>>
. Start OpenIDM with the Multi-Account Linking sample.<<install-sample-multiaccount>>
. Create two identities in OpenIDM's user repository (Managed User).<<multiaccount-create-users>>
. Create two roles OpenIDM's role repository.<<multiaccount-create-roles>>
. Assign roles to appropriate users.<<multiaccount-assign-roles>>
. Review background information. <<sample-multiaccount-background>>
. Update Roles with desired LDAP group attributes.<<multiaccount-roles-update>>
. Reconcile Managed User to the LDAP server.<<sample-multiaccountlinking-recon>>
. Review the result in the LDAP server.<<sample-multilinking-review>>

We include detail in each linked section.


[[external-ldap-config-multiaccount]]
External LDAP Configuration
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Configure the LDAP server as for sample 2,
http://openidm.forgerock.org/doc/bootstrap/install-guide/index.html#external-ldap-config-2[External LDAP Configuration]

The LDAP user must have write access to create users from OpenIDM on the LDAP
server. When you configure the LDAP server, import the appropriate LDIF file,
in this case, _openidm/samples/multiaccountlinking/data/Example.ldif_.


[[install-sample-multiaccount]]
Install the Sample
------------------

Prepare OpenIDM as described in
http://openidm.forgerock.org/doc/bootstrap/install-guide/index.html#preparing-openidm[Preparing OpenIDM],
then start OpenIDM with the following configuration for the
Multi-Account Linking sample.

++++
<screen>$ <userinput>cd /path/to/openidm</userinput></screen>
<screen>$ <userinput>./startup.sh -p samples/multiaccountlinking</userinput></screen>
++++

[[multiaccount-create-users]]
Create New Identities for the Sample
------------------------------------

For the purpose of this sample, create identities for users John Doe
and Barbara Jensen. To create these identities from the Admin UI, navigate to
`https://localhost:8443/admin` and click Manage &gt; User &gt; New User.

Alternatively, use the following REST calls to set up identities for the noted
users:

++++

<screen>$ <userinput> curl \
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
"https://localhost:8443/openidm/managed/user?_action=create"</userinput></screen>

<screen>$ <userinput> curl \
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
"https://localhost:8443/openidm/managed/user?_action=create"</userinput></screen>

++++

In the output, you will see an ID number associated with each user, in the
following format:

++++
<screen> "_id" : "35d0a49d-2571-401f-b429-96c66b23a1c0",</screen>
++++

Record the `_id` number for each user. You will use that number to
assign desired roles for each users.

[[multiaccount-create-roles]]
Create New Roles for the Sample
-------------------------------

To set up links for multiple accounts on OpenIDM, you need to set up roles.
For this sample, you will set up roles for `Agent` and `Customer`. To create
these roles in the Admin UI, navigate to `https://localhost:8443/admin` and
click Manage &gt; Role &gt; New Role.

Alternatively, use the following REST calls to set up the `Agent` and `Customer`
roles:

++++

<screen>$ <userinput>curl \
--cacert self-signed.crt \
--header "Content-Type: application/json" \
--header "X-OpenIDM-Username: openidm-admin" \
--header "X-OpenIDM-Password: openidm-admin" \
--header "If-None-Match: *" \
--request PUT \
--data '{
  "properties" : {
    "name" : "Agent",
    "description" : "Role assigned to insurance agents."
  }
}' \
"https://localhost:8443/openidm/managed/role/Agent"</userinput></screen>

<screen>$ <userinput>curl \
--cacert self-signed.crt \
--header "Content-Type: application/json" \
--header "X-OpenIDM-Username: openidm-admin" \
--header "X-OpenIDM-Password: openidm-admin" \
--header "If-None-Match: *" \
--request PUT \
--data '{
  "properties" : {
    "name" : "Customer",
    "description" : "Role assigned to insured customers."
  }
}' \
"https://localhost:8443/openidm/managed/role/Customer"</userinput></screen>

++++

[[multiaccount-assign-roles]]
Assign Roles to Appropriate Users
---------------------------------

Now you can assign roles to appropriate users. To review, user `jdoe` is an
`Agent` and user `bjensen` is a `Customer`.

You will need the `_id` value for each user. The `_id` values shown in the
following commands are random; substitute the `_id` values that you collected
when creating users.

With that in mind, the following command is intended to add the `Agent` role
to user `jdoe`:

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
      "value" : "managed/role/Agent"
    }
  ]' \
"https://localhost:8443/openidm/managed/user/9736f2c6-0103-4c48-98b5-a7c189297107"</userinput></screen>
++++

To confirm, you should see output that includes two roles for user
`jdoe`, in this case:

++++
<screen>"roles":["openidm-authorized","managed/role/Agent"],</screen>
++++

And this next command adds the `Customer` role to user `bjensen`:

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
      "value" : "managed/role/Customer"
    }
  ]' \
"https://localhost:8443/openidm/managed/user/9d38b5bf-b81e-4aee-b31e-7797efdef8b5"</userinput></screen>
++++

To confirm, you should see output that includes two roles for user
`bjensen`, in this case:

++++
<screen>"roles":["openidm-authorized","managed/role/Customer"],</screen>
++++

Now assign the `customer` role to user `jdoe`, as that user is a customer and
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
      "value" : "managed/role/Customer"
    }
  ]' \
"https://localhost:8443/openidm/managed/user/9736f2c6-0103-4c48-98b5-a7c189297107"</userinput></screen>
++++

Now user `jdoe` should have three roles:

++++
<screen>"roles":["openidm-authorized", "managed/role/Agent", "managed/role/Customer" ],</screen>
++++

[[sample-multiaccount-background]]
Background: Link Qualifiers, Agents, and Customers
--------------------------------------------------

This is a good moment to take a step back, to see how this sample works, based
on custom options in the `sync.json` configuration file.

OpenIDM maintains a table of links between source and target accounts in the
`sync.json` file. This table allows you to create a link between one source
entry and multiple target entries using a concept known as a "link qualifier".

For more information on resource mappings and link qualifiers, see
the following sections of the Integrator's Guide:

http://openidm.forgerock.org/doc/bootstrap/integrators-guide/#mapping-link-qualifiers[Using Link Qualifiers in a Mapping]

http://openidm.forgerock.org/doc/bootstrap/integrators-guide/#admin-ui-resource-mapping[Configuring a Resource Mapping from the UI]

In this sample, we use two link qualifiers:

* `insured` represents the customer accounts associated with Example.com,
    created under the following container: `ou=Customers,dc=example,dc=com`
* `agent`, represents agent accounts, considered independent contractors,
    and created under the following container: `ou=Contractors,dc=example,dc=com`

Assume that agents and customers connect via two different portals. Each group
gets access to different features, depending on the portal.

Agents may have two different accounts; one each for professional and personal
use. While the accounts are different, the identity information for each agent
should be the same for both accounts.

To that end, this sample sets up link qualifiers for two categories of users:
`insured` and `agent`, under the `managedUser_systemLdapAccounts` mapping:

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

You can verify this in the Admin UI. Click Configure &gt; Mappings &gt;
`managedUser_systemLdapAccounts` &gt; Properties &gt; Link Qualifiers. You
should see `insured` and `agent` in the list of configured Link Qualifiers.

In addition, this sample also includes a transformation script between an LDAP
Distinguished Name (`dn`) and the two categories of users. The following
excerpt of the `sync.json` file includes that script:

++++
<programlisting language="javascript">{
   "target" : "dn",
   "transform" : {
      "type" : "text/javascript",
      "globals" : { },
      "source" : "if (linkQualifier === 'agent') {
         'uid=' + source.userName + ',ou=Contractors,dc=example,dc=com'; }
         else if (linkQualifier === 'insured') { 'uid=' + source.userName +
         ',ou=Customers,dc=example,dc=com'; }"
},</programlisting>
++++

In the Admin UI, you can confirm that the `insured` and `agent` roles are
configured as link qualifers. To do so, navigate to the
`managedUser_systemLdapAccounts` mapping, go to the Properties tab, and scroll
down to the section on Link Qualifiers.

You can also review the transformation script under the same Properties tab, in
the Attributes Grid. When you select the `dn` target, you will see the inline
script, in the Transformation Script section.

++++
<!-- Skipped discussion of validSource script -->
++++

You can see how correlation queries are confiugred in the `sync.json` file.
Note how it recognizes accounts from each LDAP category in case they already
exist on the target system.

++++
<programlisting>"correlationQuery" : [
  {
    "linkQualifier" : "insured",
    "type" : "text/javascript",
    "globals" : { },
    "source" : "var map = {'_queryFilter': 'dn eq \\\"uid=' + source.userName +
      ',ou=Customers,dc=example,dc=com\\\"'}; map;"
  },
  {
    "linkQualifier" : "agent",
    "type" : "text/javascript",
    "globals" : { },
    "source" : "var map = {'_queryFilter': 'dn eq \\\"uid=' + source.userName +
      ',ou=Contractors,dc=example,dc=com\\\"'}; map;"
  }
],......</programlisting>
++++

That source may seem complex for a `sync.json` file. You can set up the same
information in a correlation script. In the following code excerpt, you
would include that information in the `correlateTreeToQueryFilter.js` file:

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

The following code snippet shows how the `validSource` script segregates
accounts based on link qualifiers and roles:

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

The `validSource` script looks through the effective roles of a user to
identify whether the user has the `Agent` or the `Insured` role.

OpenIDM needs to associate the assignments to the mapping. So the sample
version of `sync.json` includes the following element as part of the
`managedUser_systemLdapAccounts` mapping :

++++
<programlisting language="javascript">"assignmentsToMap" : [
  "ldap"
]</programlisting>
++++

[[multiaccount-roles-update]]
Update Roles With Desired LDAP Attributes
-----------------------------------------

This use case illustrates how accounts frequently have different functions on
target systems. For example, while agents may be members of a Contractor group,
insured customers may be part of a Chat Users group (possibly for access
to customer service).

While an agent may also be an insured customer, you do not want other `customer`
accounts to have the same properties (or memberships) as the `agent`
account. In this sample, we ensure that that OpenIDM limits role based
assignments to the correct account.

With the following commands, you will add a condition to the assignment of
attributes to the `agent` and `customer` roles. Note how these commands
`PATCH` the `agent` and `customer` roles with appropriate LDAP attributes.

++++
<screen>$ <userinput>curl \
--cacert self-signed.crt \
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
"https://localhost:8443/openidm/managed/role/Agent"</userinput></screen>
++++

Now repeat the process for the Customer role, with the value set to the
`Chat Users` group:

++++

<screen>$ <userinput>curl \
--cacert self-signed.crt \
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
      "linkQualifiers": ["customer"]
    }
  }
}]' \
https://localhost:8443/openidm/managed/role/e96a7e59-d819-468e-9973-f73b61b0fe0b</userinput></screen>
++++


[[sample-multiaccountlinking-recon]]
Reconciling Managed Users to the External LDAP Server
-----------------------------------------------------

Now that you have loaded `Example.ldif` into OpenDJ, and have started OpenIDM,
you can perform a reconciliation from the internal Managed Users repository
to the external OpenDJ data store:

++++
<screen>$ <userinput>curl \
 --cacert self-signed.crt \
 --header "Content-Type: application/json" \
 --header "X-OpenIDM-Username: openidm-admin" \
 --header "X-OpenIDM-Password: openidm-admin" \
 --request POST \
 "https://localhost:8443/openidm/recon?_action=recon&amp;mapping=managedUser_systemLdapAccounts"</userinput>
<computeroutput>{
   "_id" : "b36a9f92-b4f4-4c04-9c16-2784ee14bd77",
   "state" : "ACTIVE"
}</computeroutput></screen>
++++

With all of the preparation work that you have done, this reconciliation will
create three new accounts on the external LDAP server:

 * Two accounts under the `ou=Customers,dc=example,dc=com` branch `dn` for
 insured customers, `bjensen` and `jdoe`.
 * One account under the `ou=Contractors,dc=example,dc=com` branch `dn` for
 the insurance agents, `jdoe`.

Congratulations, you have just created accounts in two different areas of the
LDAP Directory Information Tree.

[[sample-multilinking-review]]
Reviewing the Result
--------------------

You have already confirmed that user `bjensen` has a `customer` role,
and user `jdoe` has both a `customer` and `agent` role. You can confirm the
same result in the Admin UI:

. Click Manage &gt; Role.
. You should see both `Agent` and `Customer` in the Role List window that
appears.
. Click Agent &gt; Users. You should see that only user `jdoe` is included
as an Agent.
. Click Back to Roles &gt; Customer &gt; Users. You should see that both users,
`bjensen` and `jdoe` are included as Customers.

