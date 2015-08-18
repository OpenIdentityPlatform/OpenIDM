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

[[external-ldap-config-multiaccount]]
External LDAP Configuration
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Configure the LDAP server as for sample 2,
http://openidm.forgerock.org/doc/bootstrap/install-guide/index.html#external-ldap-config-2[External LDAP Configuration^]

The LDAP user must have write access to create users from OpenIDM on the LDAP
server. When you configure the LDAP server, import the appropriate LDIF file,
in this case, _openidm/samples/multiaccountlinking/data/Example.ldif_.


[[install-sample-multiaccount]]
Install the Sample
------------------

Prepare OpenIDM as described in
http://openidm.forgerock.org/doc/bootstrap/install-guide/index.html#preparing-openidm[Preparing OpenIDM^],
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
`https://localhost:8443/admin` and click Manage > User > New User.

Alternatively, use the following REST calls to set up identities for the noted
users:

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

For this sample, to set up links for multiple accounts on OpenIDM, you need to
set up roles. To do so, set up roles for `Agent` and `Customer`. To create
these roles in the Admin UI, navigate to `https://localhost:8443/admin` and
click Manage > Role > New Role.

Alternatively, use the following REST calls to set up the `Agent` and `Customer`
roles:

++++

<screen>$ <userinput>curl \
--cacert self-signed.crt \
--header "Content-Type: application/json" \
--header "X-OpenIDM-Username: openidm-admin" \
--header "X-OpenIDM-Password: openidm-admin" \
--request POST \
--data '{
  "properties" : {
    "name" : "Agent",
    "description" : "Role assigned to insurance agents."
  }
}' \
"https://localhost:8443/openidm/managed/role?_action=create"</userinput></screen>

<screen>$ <userinput>curl \
--cacert self-signed.crt \
--header "Content-Type: application/json" \
--header "X-OpenIDM-Username: openidm-admin" \
--header "X-OpenIDM-Password: openidm-admin" \
--request POST \
--data '{
  "properties" : {
    "name" : "Customer",
    "description" : "Role assigned to insured customers."
  }
}' \
"https://localhost:8443/openidm/managed/role?_action=create"</userinput></screen>

++++

Do record the `_id` output for the Agent and Customer roles. You will use those
numbers

NOTE: While you could use `PUT` to create roles with descriptive names,
we recommend that you use `POST` to create roles with immutable IDs.

[[multiaccount-assign-roles]]
Assign Roles to Appropriate Users
---------------------------------

Now you can assign roles to appropriate users. To review, user `jdoe` is an
`Agent` and user `bjensen` is a `Customer`.

You will need the `_id` value for each user. The `_id` values shown in the
following commands are random; substitute the `_id` values that you collected
when creating users.

The following command adds the `Agent` role to user `jdoe`, by their
`_id` values:

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
      "value" : "managed/role/287dc4b1-4b19-49ec-8b4c-28a6c12ede34"
    }
  ]' \
"https://localhost:8443/openidm/managed/user/8fae84ed-1f30-4542-8087-e7fa6e89541c"</userinput></screen>
++++

To confirm, you should see output that includes two roles for user
`jdoe`. The following output includes a unique Agent `_id` number; the number
that you see will be different.

++++
<screen>"roles":["openidm-authorized","managed/role/287dc4b1-4b19-49ec-8b4c-28a6c12ede34"],</screen>
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
      "value" : "managed/role/bb9302c4-5fc1-462c-8be2-b17c87175d1b"
    }
  ]' \
"https://localhost:8443/openidm/managed/user/d0b79f30-946f-413a-b7d1-d813034fa345"</userinput></screen>
++++

To confirm, you should see output that includes two roles for user
`bjensen`, in this case:

++++
<screen>"roles":["openidm-authorized","managed/role/bb9302c4-5fc1-462c-8be2-b17c87175d1b"],</screen>
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
      "value" : "managed/role/bb9302c4-5fc1-462c-8be2-b17c87175d1b"
    }
  ]' \
"https://localhost:8443/openidm/managed/user/8fae84ed-1f30-4542-8087-e7fa6e89541c"</userinput></screen>
++++

Now user `jdoe` should have three roles:

++++
<screen>"roles":["openidm-authorized", "managed/role/287dc4b1-4b19-49ec-8b4c-28a6c12ede34", "managed/role/bb9302c4-5fc1-462c-8be2-b17c87175d1b" ],</screen>
++++

[[multiaccount-background]]
Background: Link Qualifiers, Agents, and Customers
--------------------------------------------------

This is a good moment to take a step back, to see how this sample works, based
on custom options in the `sync.json` configuration file.

OpenIDM defines mappings between source and target accounts in the
`sync.json` file. This table allows you to create a link between one source
entry and multiple target entries using a concept known as a "link qualifier,"
which enables one-to-many relationships in mappings and policies.

For more information on resource mappings and link qualifiers, see
the following sections of the Integrator's Guide:

http://openidm.forgerock.org/doc/bootstrap/integrators-guide/#mapping-link-qualifiers[Using Link Qualifiers in a Mapping]

http://openidm.forgerock.org/doc/bootstrap/integrators-guide/#admin-ui-resource-mapping[Configuring a Resource Mapping from the UI]

http://openidm.forgerock.org/doc/bootstrap/integrators-guide/#link-qualifier[Link Qualifier definition]

In this sample, we use two link qualifiers:

* `insured` represents the customer accounts associated with Example.com,
    created under the following LDAP container: `ou=Customers,dc=example,dc=com`
* `agent` represents agent accounts, considered independent contractors,
    and created under the following LDAP container:
    `ou=Contractors,dc=example,dc=com`

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

You can verify this in the Admin UI. Click Configure > Mappings >
`managedUser_systemLdapAccounts` > Properties > Link Qualifiers. You
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
      "source" :
         "if (linkQualifier === 'agent') {
            'uid=' + source.userName + ',ou=Contractors,dc=example,dc=com';
         } else if (linkQualifier === 'insured') {
            'uid=' + source.userName + ',ou=Customers,dc=example,dc=com';
         }"
},</programlisting>
++++

The following validSource script looks through the effective roles of a user,
with two objectives:
* Determine whether the user has an `Agent` or `Insured` role.
* Ensures that OpenIDM looks through the source *only* for the specified role.

++++
<programlisting language="javascript"><![CDATA["validSource" : {
        "type" : "text/javascript",
        "globals" : { },
        "source" : "var res = false;\nvar i=0;\n\nwhile
          (!res && i < source.effectiveRoles.length) {\n
          var roleId = source.effectiveRoles[i];\n
            if (roleId != null && roleId.indexOf(\"/\") != -1) {\n
              var roleInfo = openidm.read(roleId);\n
                res = (((roleInfo.properties.name === 'Agent')\n
                  &&(linkQualifier ==='agent'))\n
                  || ((roleInfo.properties.name === 'Insured')\n
                  &&(linkQualifier ==='insured')));\n
                }\n
              i++;\n}\n\nres"
        }]]></programlisting>
++++

You can see how correlation queries are configured in the `sync.json` file.
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

The structure for the correlation query specifies one of two link qualifiers:
insured or agent) For each link qualifier, the correlation query
defines a script that verifies if the subject `dn` belongs in a specific
container. For this sample, the container (`ou`) may be Customers or
Contractors.

You can can avoid specifying the structure of the `dn` attribute in two places
in the `sync.json` file with the following code, which leverages the expression
builder to reuse the construct defined in the `dn` mapping:

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

You can also leverage the expression builder in the UI. Review how the UI
illustrates the expression builder. To do so, click Configure > Mapping >
select a mapping > Association > Association Rules. Edit either link qualifier.
You will see how the expression builder is configured for this sample.

The following code snippet shows how the `validSource` script segregates
accounts based on link qualifiers and roles:

++++
<programlisting language="javascript"><![CDATA["validSource" : {
  "type" : "text/javascript",
  "globals" : { },
  "source" : "var res = false;
    var i=0;

    while (!res && i &lt; source.effectiveRoles.length) {
      var roleId = source.effectiveRoles[i];
      if (roleId != null &amp;&amp; roleId.indexOf("/") != -1) {
        var roleInfo = openidm.read(roleId);
        logger.warn("Role Info : {}",roleInfo);
        res = (((roleInfo.properties.name === 'Agent')
          &&(linkQualifier ==='agent'))
        || ((roleInfo.properties.name === 'Insured')
          &&;(linkQualifier ==='insured')));
        }
        i++;
      }

      res"
}]]></programlisting>
++++

The `validSource` script uses the effectiveRoles property to determine whether
a user has the `Agent` or the `Insured` role, based on that user's effective
roles.

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
account. In this sample, we ensure that OpenIDM limits role based
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
  "https://localhost:8443/openidm/managed/role/287dc4b1-4b19-49ec-8b4c-28a6c12ede34"</userinput></screen>
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
"https://localhost:8443/openidm/managed/role/bb9302c4-5fc1-462c-8be2-b17c87175d1b</userinput></screen>
++++


[[multiaccountlinking-recon]]
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
</screen>
++++

With all of the preparation work that you have done, this reconciliation will
create three new accounts on the external LDAP server:

 * Two accounts under the `ou=Customers,dc=example,dc=com` branch `dn` under
 the insured customers role, `bjensen` and `jdoe`.
 * One account under the `ou=Contractors,dc=example,dc=com` branch `dn` under
 the insurance agents role, `jdoe`.

Congratulations, you have just created accounts in two different areas of the
LDAP Directory Information Tree.

[[multilinking-review]]
Reviewing the Result
--------------------

You have already confirmed that user `bjensen` has a `customer` role,
and user `jdoe` has both a `customer` and `agent` role. You can confirm the
same result in the Admin UI:

. Click Manage > Role.
. You should see both `Agent` and `Customer` in the Role List window that
appears.
. Click Agent > Users. You should see that user `jdoe` is included
as an Agent.
. Click Back to Roles > Customer > Users. You should see that users
`bjensen` and `jdoe` are included as Customers.

