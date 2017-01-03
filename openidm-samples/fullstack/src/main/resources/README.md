/** 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
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

# OpenAM Sample:
## Integration with OpenAM for strong, flexible authentication and single-sign on

This sample demonstrates how to use OpenIDM together with OpenAM and OpenDJ, 
showing how the entire ForgeRock Open Identity Stack can be used to maximize 
identity and access management (IAM) functionality. Any application for which
your users need access can be secured with OpenAM, and the data for those 
users can be maintained with OpenIDM.

OpenAM and OpenIDM are both powerful products on their own, but even more so 
when used together. Each has their core use-cases - access management and 
provisioning - both of which are necessary to build a complete IAM solution. 
It is perfectly reasonable to setup both products in an organization, each 
doing their separate job but having no direct interaction with each other. 
However, using OpenAM to secure OpenIDM improves the available functionality in
both OpenIDM and OpenAM, above and beyond what is available when they run 
separate from each other.

OpenIDM benefits from the strong access management that OpenAM provides - 
more authentication options, multi-factor authentication, powerful authentication chaining, and single-sign on. OpenAM also benefits when used this way - the OpenIDM user profile management functionality is far more feature-rich than what is provided by default in OpenAM. OpenIDM provides a sophisticated data validation service and has an easily-extensible, flexible environment to build upon.

### Prerequisites

1. An existing OpenAM 14 server running in an environment that OpenIDM can access via REST.  Refer to the following [OpenIDM Samples Guide chapter](https://backstage.forgerock.com/docs/openidm/5/samples-guide#chap-integrated-sample) for guidance on how to prepare OpenAM 14 with OpenID Connect.


Chapter 11 of the OpenIDM Samples Guide outlines how to install and configure OpenAM for integration via OpenID Connect with OpenIDM. http://abondance-uk.internal.forgerock.com/public/openidm-docs/701/target/docbkx/bootstrap/samples-guide/#chap-integrated-sample

2. An OpenDJ installation which OpenAM has been configured to use as the user store. This OpenDJ server needs to be directly accessible to OpenIDM via a standard LDAP port. If you wish to enable livesync monitoring, be sure that OpenDJ has been installed with the Topology Option "This server will be part of a replication topology" enabled.

3. Use FQDNs for the systems with OpenIDM, OpenAM, and OpenDJ. OpenAM SSO cookies require FQDNs.

For simplicity in this demo, it is assumed that you are using a single, root-level realm in OpenAM. It is possible to use multiple realms, but that is a more complicated setup.

### Authentication Configuration

Configuring authentication is simple using the UI.

To run the sample, start OpenIDM with the configuration for the fullStack sample:
$ cd /path/to/openidm
$ ./startup.sh -p samples/fullStack

After starting up the fullStack sample navigate to the deployment URL's admin context. e.g. https://localhost:8443/admin

After logging in click on the CONFIGURE navigation item, then click on the AUTHENTICATION submenu.

By default authentication is configured to use local authentication modules, for this sample we are going to make use of the ForgeRock Identity Provider.  To begin the configuration click on the radio button adjacent to the "ForgeRock Identity Provider" label.

A dialog will open where you will provide configuration details.  The first section provides two OpenAM OpenID Connect Client Agent redirection URI's you will need to provide to your OpenAM instance.

For the second section you only need provide the Well-Known URL, Client ID and Client Secret. An example of the Well-Known Endpoint if provided.  The UI will use the Well-Known endpoint to fetch and generate relevant configuration details. Before you are allowed to save, this Well-Known endpoint will be validated.

Click the save button once the form is completed.  This will disable all authentication modules with the exception of the STATIC USER and INTERNAL USER modules so that OpenIDM may be accessible via rest.

No other changes will be necessary.  You will be prompted to logout and re-authenticate as your session may be invalid with these changes.  You will re-authenticate with OpenAM if configured properly.  Use the amadmin user in place of openidm-admin.

Should you encounter issues logging in, you may have a bad configuration.  You can revert or manually update the authentication configuration by editing the samples/fullStack/conf/authentication.json file.


### Provisioning Configuration

The provisioning portion of this sample is based primarily on sample2b. It includes a bi-directional mapping between OpenDJ (system/ldap/account) and managed/user. In this case, you need to update samples/fullStack/conf/provisioner-openicf.ldap.json:

    "configurationProperties" : {
        "host" : "opendj.example.com",
        "port" : 389,
        "ssl" : false,
        "principal" : "cn=Directory Manager",
        "credentials" : "password",
        "baseContexts" : [
            "dc=example,dc=com"
        ],
        "baseContextsToSynchronize" : [
            "dc=example,dc=com"
        ],

These will need the same values that you have entered into OpenAM for the default user store. In OpenAM, the "LDAP Bind DN" and "LDAP Bind Password" values are the account credentials used to connect to the server; the equivalent fields in the provisioner configuration are "principal" and "credentials", respectively. "LDAP Organization DN" maps to the first entry in the "baseContexts" list; copy this value into the "baseContextsToSynchronize" list as well. 

If you are using SSL, change the "ssl" property here to true; you will also need to make sure the port is the correct SSL port. Just as with OpenAM's SSL certificate, the SSL certificate used by OpenDJ needs to be trusted by OpenIDM. Follow the same process used to trust the OpenAM SSL certificate in order to trust the OpenDJ certificate.

Once configured, navigate to the CONFIGURE navigation item and click on the MAPPINGS submenu item.  Here on the Mappings page you will import the users from OpenDJ into managed/user. To do that simply click on the "Reconcile" button belonging to the mapping with a source of "System/Ldap/Account" and a target of "Managed User."  Now if you visit MANAGE and the submenu USER you should see entries matching the entries you have in your OpenDJ user store.

At this point you may also wish to enable a scheduled liveSync, to ensure that OpenDJ and managed/user stay in sync with each other over time.

To schedule a LiveSync at a given time interval click on the CONFIGURE navigation, and the SCHEDULES submenu item. Click the "Add Schedules" button.  Enable the new schedule and provide the schedule with an interval, for this example set the schedule to fire every 30 seconds. Set the action to "trigger a liveSync", make sure the mapping selected is "system/ldap/account".  Click save.  Now every thirty seconds the OpenIDM will look for changes made to the remote system and copy them over to the managed store.  Implicit Sync is automatically enabled for the "managedUser_systemLdapAccounts" mapping, so changes made to managed will also be propagated to LDAP.

### Possible Further Extensibility Options
Here are some ideas that you could implement with this new OpenIDM/OpenAM configuration:

#### Workflows to request access to applications
The hybrid OpenAM/OpenIDM UI can be extended in a number of powerful ways. For example, you could use a workflow to allow users to request access to new applications. Upon approval, OpenIDM could update the user's account to include whatever permissions OpenAM uses to control access to that application. If necessary, OpenIDM could also update the data store for the application itself to include whatever user information is needed there.

#### Single Sign On Dashboard
Extending that idea further, another simple enhancement to this UI would be a list of links to all of those applications a user has access to by virtue of their SSO token. This would essentially provide the user with a "Single Sign On" dashboard, which could be used to easily access all of their applications.

