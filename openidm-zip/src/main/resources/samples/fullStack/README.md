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

1. An existing OpenAM 12 server running in an environment that OpenIDM can access via REST.

2. An OpenDJ installation which OpenAM has been configured to use as the user store. This OpenDJ server needs to be directly accessible to OpenIDM via a standard LDAP port. If you wish to enable livesync monitoring, be sure that OpenDJ has been installed with the Topology Option "This server will be part of a replication topology" enabled.

3. Use FQDNs for the systems with OpenIDM, OpenAM, and OpenDJ. OpenAM SSO cookies require FQDNs.

For simplicity in this demo, it is assumed that you are using a single, root-level realm in OpenAM. It is possible to use multiple realms, but that is a more complicated setup.

### Authentication Configuration

Configuring authentication is simple in most cases. Edit samples/fullStack/conf/authentication.json and update the "openamDeploymentUrl" property of the "OPENAM_SESSION" auth module. It should look something like this:

        "authModules" : [
            {
                "name" : "OPENAM_SESSION",
                "properties" : {
                    "openamDeploymentUrl" : "https://amserver.example.com:8443/openam",
                    "groupRoleMapping" : {
                        "openidm-admin" : [
                            "cn=idmAdmins,ou=Groups,dc=example,dc=com"
                        ]
                    },

Be sure to include "/openam" in the deployment url (or whatever servlet context you have configured OpenAM to run within).

Note the list under groupRoleMapping->"openidm-admin". You can optionally add group values to this list, and any user which is a member of one of those groups will be granted the associated role (in this case, "openidm-admin").

If you are using SSL (as in the above HTTPS example) then you will need to make sure that the SSL certificate is trusted by OpenIDM. If your LDAP server's SSL certificate has not been provided by a well-known certificate authority, you may need to import this ssl certificate into OpenIDM's truststore. See the "[Accessing the Security Management Service](http://openidm.forgerock.org/doc/integrators-guide/index.html#security-management-service)" section of the Integrator's guide for more details.

You must verify that the domain you will be using to access the OpenIDM UI is listed under "Configuration"->"System"->"Platform"->"Cookie Domains". For example, if you plan on accessing the UI like so:

    https://idm.example.com/
    
Then you should ensure that ".example.com" is listed there. This is necessary for the OpenIDM UI to set the OpenAM SSO token cookie.

### Provisioning Configuration

The provisioning portion of this sample is based primarily on sample2c. It includes a bi-directional mapping between OpenDJ (system/ldap/account) and managed/user. In this case, you need to update samples/fullStack/conf/provisioner-openicf.ldap.json:

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

Once configured, run this command to import the users from OpenDJ into managed/user:

    curl -k -H "Content-type: application/json" -u "openidm-admin:openidm-admin" -X POST "https://localhost:8443/openidm/recon?_action=recon&mapping=systemLdapAccounts_managedUser"

At this point you may also wish to enable scheduled reconcilation and livesync, to ensure that OpenDJ and managed/user stay in sync with each other over time. Edit samples/fullStack/conf/schedule-recon.json and schedule-livesync.json, setting the "enabled" value within each to true. This will ensure that any changes to the user store which do not originate from OpenIDM will still be visible to OpenIDM.

### Logging In

Once you have followed the above steps, you can access the OpenIDM UI as normal (https://domain:port/). The UI has been modified slightly for this sample; instead of the regular login process, we use a proxy service to communicate with OpenAM's REST-based Authentication service. This proxy service is implemented as a custom endpoint - read more details in the source at bin//defaults/script/ui/openamProxy.js.

If everything is setup properly, when the OpenIDM UI loads you will see one of two things happen: 

1. If you do not already have a valid SSO cookie in your browser, you will be redirected to the configured OpenAMLoginUrl for authentication. By default this is a standard-looking "User Name:" and "Password:" form. Submit this form to advance through the AM authentication process, and ultimately (if successfully logged in) you will receive a valid SSO token and be redirected back to OpenIDM. At this point...

2. If you have a valid SSO you will be logged into OpenIDM with the user associated with that session. You will be able to perform all of the normal end-user operations such as triggering workflow processes and updating your profile. Also, because you have an SSO token you will be authenticated to all other services OpenAM provides authentication for (including any on-premise or cloud application for which OpenAM is providing federated login).

### Possible Further Extensibility Options
Here are some ideas that you could implement with this new OpenIDM/OpenAM configuration:

#### Workflows to request access to applications
The hybrid OpenAM/OpenIDM UI can be extended in a number of powerful ways. For example, you could use a workflow to allow users to request access to new applications. Upon approval, OpenIDM could update the user's account to include whatever permissions OpenAM uses to control access to that application. If necessary, OpenIDM could also update the data store for the application itself to include whatever user information is needed there.

#### Single Sign On Dashboard
Extending that idea further, another simple enhancement to this UI would be a list of links to all of those applications a user has access to by virtue of their SSO token. This would essentially provide the user with a "Single Sign On" dashboard, which could be used to easily access all of their applications.

#### Integrated Services For Custom Applications
Based on the presence of a shared SSO token, any application secured by OpenAM could reliably invoke OpenIDM REST services in an integrated and transparent way. For example, an intranet portal could make AJAX requests to OpenIDM to start a workflow process or execute a query. The fact that the request was being made to OpenIDM would be hidden from the user - the SSO token that they used to access the intranet would also be used by OpenIDM, and everything would just work seamlessly.

