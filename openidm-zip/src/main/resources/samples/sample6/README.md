    /**
     * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
     *
     * Copyright 2014 ForgeRock AS. All rights reserved.
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

Sample 6 - LiveSync Between Two LDAP Servers
============================================

This sample demonstrates use of two real LDAP connections, and both
reconciliation and LiveSync. The configurations provided are tailored
for working with Microsoft Active Directory and ForgeRock OpenDJ, however
they could be easily changed to work with any standard LDAP servers.

For documentation pertaining to this example see:
http://openidm.forgerock.org/doc/install-guide/index.html#more-sample6


Setup OpenDJ
------------
1.  Extract OpenDJ to a folder called opendj.

2.  Run the following command to initialize OpenDJ.

        $ opendj/setup --cli --hostname localhost --ldapPort 1389 --rootUserDN "cn=Directory Manager" \
        --rootUserPassword password --adminConnectorPort 4444 --baseDN dc=com --acceptLicense --addBaseEntry \
        --no-prompt --quiet

3.  Load the Example.ldif file supplied in the data folder into OpenDJ.

        $ opendj/bin/ldapmodify --bindDN "cn=Directory Manager" --bindPassword password --hostname localhost \
        --port 1389 --filename /path/to/openidm/samples/sample6/data/Example.ldif

The directory server should now show two users under dc=example,dc=com.


Active Directory Configuration Alternatives
-------------------------------------------

There are two different configuration options. Within the samples/sample6/alternatives folder,
depending on the external resources you have to work with. Within the 
samples/sample6/alternatives folder, you will find two provisioner configurations - 
one for a "real" AD server and one for a "fake" AD server. 

### Option 1 (real)
If you have access to a real Microsoft Active Directory server that you
would like to use for this sample, choose the "provisioner.openicf-realad.json".
Note that the configuration for this sample is one-way, from AD to DJ, so there
is no risk in configuring a real AD server as part of this sample - changes won't
be made on that server.

      $ cp samples/sample6/alternatives/provisioner.openicf-realad.json samples/sample6/conf

Using a text editor, open samples/sample6/conf/provisioner.openicf-realad.json and
make the following updates:

    "configurationProperties" : {
        "host" : "",          // Enter the hostname or IP address of your Active Directory server
        "port" : "389",       // Default non-SSL port. If using SSL (below), change to 636
        "ssl" : false,        // To use, you may need to import the server's public key into OpenIDM's truststore
        "principal" : "",     // Full DN of the account to bind with (ex: "CN=Administrator,CN=Users,DC=example,DC=com")
        "credentials" : null, // Password for account to bind (replace null with string value; it will be encrypted upon startup)
        "baseContexts" : [ ], // List of DNs for the containers of accounts. (ex: "CN=Users,DC=example,DC=com")
        "baseContextsToSynchronize" : [ ], // Set to be the same values as "baseContexts"

        // Additional options to further limit the accounts returned. Defaults to active accounts which aren't Computers
        "accountSearchFilter" : "(&(!(userAccountControl:1.2.840.113556.1.4.803:=2))(!(objectClass=Computer)))",
        "accountSynchronizationFilter" : "(&(!(userAccountControl:1.2.840.113556.1.4.803:=2))(!(objectClass=Computer)))",

### Option 2 (fake)
If you do not have a real Microsoft Active Directory server available, you can
simulate one using the "fake" AD configuration. This configuration uses the same OpenDJ
server that you installed above, but uses a different base DN for the "AD" users. 

1.  Load the AD.ldif supplied in the data folder into OpenDJ.

        $ opendj/bin/ldapmodify --bindDN "cn=Directory Manager" --bindPassword password --hostname localhost \
        --port 1389 --filename /path/to/openidm/samples/sample6/data/AD.ldif

2.  Setup Replication on OpenDJ for fake ad.

        $ opendj/bin/dsconfig create-replication-server --hostname localhost --port 4444 --bindDN "cn=Directory Manager" \
        --bindPassword password --provider-name "Multimaster Synchronization" --set replication-port:8989 \
        --set replication-server-id:2 --type generic --trustAll --no-prompt

        $ opendj/bin/dsconfig create-replication-domain --hostname localhost --port 4444 --bindDN "cn=Directory Manager" \
        --bindPassword password --provider-name "Multimaster Synchronization" --domain-name fakead_com \
        --set base-dn:dc=fakead,dc=com --set replication-server:localhost:8989 --set server-id:3 --type generic \
        --trustAll --no-prompt

3. Copy the fake ad configuration file into your conf folder:

        $ cp samples/sample6/alternatives/provisioner.openicf-fakead.json samples/sample6/conf

Edit samples/sample6/conf/provisioner.openicf-fakead.json and review the configuration details,
being sure to set the connection values to match however you have installed OpenDJ.

Running the Sample in OpenIDM
-----------------------------

To run the sample in OpenIDM, follow these steps.

1. Start OpenIDM with the configuration for sample 6.

        $ cd /path/to/openidm
        $ ./startup.sh -p samples/sample6

2. Run reconciliation.

        $ curl -k -H "Content-type: application/json" -u "openidm-admin:openidm-admin" -X POST \
        "https://localhost:8443/openidm/recon?_action=recon&mapping=systemAdAccounts_managedUser"
        {"_id":"d88ca423-d5f2-4eb5-a451-a229399f92af","state":"ACTIVE"}

3. Check that the users from Active Directory were added to OpenDJ:

        $ curl -k -H "Content-type: application/json" -u "openidm-admin:openidm-admin" \
        "https://localhost:8443/openidm/system/ldap/account?_queryId=query-all-ids&_prettyPrint=true"

    The way this works is that the reconciliation from step 2 imports the data into managed/user.
    Each change on managed/user triggers a "sync" action for the other mappings which use managed/user
    as a source; in this case, the managedUser_systemLdapAccounts mapping. This mapping updates
    OpenDJ.

4. Edit samples/sample6/conf/schedule-activeSynchroniser_systemAdAccount.json
to set "enabled" : true. LiveSync causes synchronization to happen as you
make changes in the source system (Active Directory in this case).

5. Make a change within the (real or fake) Active Directory server, and observe the change in managed/user and in OpenDJ.

    *  If you are using a real Active Directory server, you can use the graphical tool
"Active Directory Users and Computers" on the server hosting the directory. Open
this, find a user that you know has been synced to OpenDJ, and make some property
change. Livesync should detect that change within 15 seconds (as per the configuration
in schedule-activeSynchroniser_systemAdAccount.json) and update both the managed/user
and OpenDJ records accordingly.

    *  If you are using the fake Active Directory configuration, you can use ldapmodify to
create a new user in ou=People,dc=fakead,dc=com and then check the result. An example would be to create a bdobbs.ldif
file and paste the following in it.

        dn: uid=bdobbs,ou=People,dc=fakead,dc=com
        objectClass: person
        objectClass: inetOrgPerson
        objectClass: organizationalPerson
        objectClass: top
        givenName: Bob
        description: Created to see LiveSync work
        uid: bdobbs
        cn: Bob Dobbs
        sn: Dobbs
        mail: bdobbs@example.com
        telephoneNumber: 1-555-111-2222
        userPassword: password

        Then use ./ldapmodify -p 1389 -a -D "cn=Directory Manager" -w password -f ~/path/to/bdobbs.ldif

        $ ./ldapsearch -p 1389 -b dc=example,dc=com "(uid=*)" cn description
        dn: uid=jdoe,dc=example,dc=com
        description: Created for OpenIDM
        cn: John Doe

        dn: uid=bdobbs,dc=example,dc=com
        description: Created to see LiveSync work
        cn: Bob Dobbs

6. You can login to the OpenIDM UI with any of the Active Directory user credentials. Changes
made within the OpenIDM UI will only be persisted within managed/user and OpenDJ, since we
do not have a bidirectional mapping between Active Directory and managed/user.

