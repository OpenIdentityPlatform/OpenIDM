    /*
     * The contents of this file are subject to the terms of the Common Development and
     * Distribution License (the License). You may not use this file except in compliance with the
     * License.
     *
     * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
     * specific language governing permission and limitations under the License.
     *
     * When distributing Covered Software, include this CDDL Header Notice in each file and include
     * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
     * Header, with the fields enclosed by brackets [] replaced by your own identifying
     * information: "Portions copyright [year] [name of copyright owner]".
     *
     * Copyright 2016 ForgeRock AS.
     */

PowerShell Connector - Azure samples
=========================================================================================

The tools/ folder contains several samples for the OpenICF PowerShell Connector.
This sample uses the MS Azure Active Directory PowerShell module. See:
http://technet.microsoft.com/en-us/library/hh852274.aspx.

A Microsoft Azure Account is required to setup an Azure Active Directory.  If you don't have one, create a Microsoft
Azure account. See: http://azure.microsoft.com

Follow these steps to use this sample:

1.	Set up an Azure AD Cloud service.
    1.	http://azure.microsoft.com
    1.	Click on the "PORTAL" link at the top of the page.
    1.	Sign in using the credentials of your Microsoft account.
    1.	Click the "New" link at the top left.
    1.	Select "Security + Identity".
    1.	Select "Active Directory"
    1.	Fill in the form titled "Add Directory".
    1.	Click the check mark on the bottom right of the form to submit the form.
    1.	Your directory should now be created and listed.

1.	Create an admin user account that can administer the directory.  By default your directory will have a single
identity, your Microsoft Azure Account.  However, this account cannot be used to run the PowerShell Connector scripts
that administer the Azure Active Directory.  You must create a local administrative identity that is native to your
newly created directory.
    1.	Navigate to https://portal.azure.com/ and log in if needed.
    1.	Click Browse on the last item of the left navigation bar.
    1.	Click “Active Directory”.
    1.	Click your directory name.
    1.	Click “USERS” on the top navigation bar.
    1.	On the bottom grey navigation bar, click "ADD USER".
    1.	Select "New user in your organization".
    1.	Enter the username for the administrator account.
    1.	Click the arrow to submit step 1.
    1.	Enter the first name, last name, and display name.
    1.	Be sure to change the user's role to, at least, "User Admin".
    1.	Multi-Factor Authentication should not be checked.
    1.	Click the arrow to submit step 2.
    1.	Click the green "create" button. This will create the user with a temporary password.
    1.	Since the user will be forced to change the password on the first login, we must log in as the user to change
        it. Copy the password and click the check mark to close the form.
    1.	Sign out from your Microsoft Azure Account by clicking on the account icon in the upper right and choosing
        "Sign-Out".
    1.	Click on the "SIGN IN" button.
    1.	Choose the "User another account?" option.
    1.	Enter the email and the password of the user admin account created above.
        For example: admin@[your_domain].onmicrosoft.com.
    1.	You will be asked to change the password.  Click "Update password and sign in" when ready.

1.  Install the Windows Azure Active Directory Module for Windows PowerShell on a Windows machine with connectivity to
    Azure.  https://msdn.microsoft.com/library/azure/jj151815.aspx

1.  Verify connectivity between this Windows box and AzureAD:
    1.	Open a PowerShell window.
    1.	At the command prompt type “Connect-Msolservice”.
    1.	A popup should appear asking for the user credentials.  Enter the credentials of the admin account created
        for the directory service.
    1.	If no error occurs, you are connected.

1.  Install the OpenICF .NET connector server on the Windows box.
    See: https://forgerock.org/openidm/doc/bootstrap/integrators-guide/index.html#install-.net-connector

1.  Install the PowerShell connector on the Windows box. See:
    https://forgerock.org/openidm/doc/bootstrap/integrators-guide/index.html#powershell-connector

1.  Copy all the tools/*.ps1 scripts to the Windows box where you installed the .NET connector server and the
    PowerShell connector.  In the sample provisioner configuration file
    (samples/provisioners/provisioner.openicf-azureadpowershell.json), files are expected to be in the folder
    C:/openidm/samples/powershell2AzureAD/tools/
    *  If your Windows system requires that scripts be digitally signed for execution, AzureADCommon.ps1 will need to
        be digitally signed as it is utilized from the other scripts.

1.  Copy the remote connector provisioner file to openidm/conf/ from
    openidm/samples/provisioners/provisioner.openicf.connectorinfoprovider.json
    *  Adapt to your local configuration of the Windows Box with the installed connector server.
    *  Remove the "protocol" configuration, to be compatible with a 1.4 connector version.

1.  Edit the sample PowerShell connector provisioner file to suit your installation.
    * The file is samples/provisioners/provisioner.openicf-azureadpowershell.json. You may need to change the file
        paths. For instance, the following may not reflect your Windows folder. The other settings should not be 
        changed:

            "configurationProperties" : {
                "CreateScriptFileName" : "C:/openidm/samples/powershell2AzureAD/tools/AzureADCreate.ps1",
                "DeleteScriptFileName" : "C:/openidm/samples/powershell2AzureAD/tools/AzureADDelete.ps1",
                
    * Login and Password should be set to the credentials of the user admin of your Azure Directory.
1. Copy the provisioner file to the openidm/conf/ folder.

OpenIDM instructions:
====================================================================

1. Test that the connector has been configured correctly and that the Azure AD resource can be reached:

        $ curl --request POST \
            --url 'http://localhost:8080/openidm/system/azureadpowershell?_action=test' \
            --header 'cache-control: no-cache' \
            --header 'content-type: application/json' \
            --header 'x-openidm-password: openidm-admin' \
            --header 'x-openidm-username: openidm-admin'
    Returns:

        {
          "name": "azureadpowershell",
          "enabled": true,
          "config": "config/provisioner.openicf/azureadpowershell",
          "objectTypes": [
            "__ALL__",
            "account",
            "group"
          ],
          "connectorRef": {
            "bundleName": "MsPowerShell.Connector",
            "connectorName": "Org.ForgeRock.OpenICF.Connectors.MsPowerShell.MsPowerShellConnector",
            "bundleVersion": "1.4.2.0"
          },
          "displayName": "PowerShell Connector ",
          "ok": true
        }

1. Query All User Ids:

        $ curl --request GET \
            --url 'http://localhost:8080/openidm/system/azureadpowershell/account?_queryId=query-all-ids' \
            --header 'cache-control: no-cache' \
            --header 'content-type: application/json' \
            --header 'x-openidm-password: openidm-admin' \
            --header 'x-openidm-username: openidm-admin'

    Returns:

        {
          "result": [
            {
              "_id": "abcfdf2b-be65-42ef-95bd-d2caab46cb1d",
              "UserPrincipalName": "joe.blow@directorydomain.onmicrosoft.com"
            },
            {
              "_id": "f37902f1-55fd-4fe9-981f-571baf427806",
              "UserPrincipalName": "admin@directorydomain.onmicrosoft.com"
            }
            ...
          ],
          "resultCount": 4,
          "pagedResultsCookie": null,
          "totalPagedResultsPolicy": "NONE",
          "totalPagedResults": -1,
          "remainingPagedResults": -1
        }

1. Use Query Filter:

        $ curl --request GET \
          --url 'http://localhost:8080/openidm/system/azureadpowershell/account?_queryFilter=true \
          --header 'cache-control: no-cache' \
          --header 'content-type: application/json' \
          --header 'x-openidm-password: openidm-admin' \
          --header 'x-openidm-username: openidm-admin'

    Returns all users matching the filter:

        {
          "result": [
            {
              "_id": "abcfdf2b-be65-42ef-95bd-d2caab46cb1d",
              "DisplayName": "Joe Bloe",
              "AlternateEmailAddresses": [
                "joe.blow@domain.com"
              ],
              "UserPrincipalName": "joe.blow@directorydomain.onmicrosoft.com",
              "LastPasswordChangeTimestamp": "2/5/2016 11:22:04 PM",
              "FirstName": "ModifiedFirstName",
              "PasswordNeverExpires": false,
              "LastName": "blow",
              "LiveId": "10037FFE95FEF065",
              "PreferredLanguage": "en-US"
            },
            {
              "_id": "f37902f1-55fd-4fe9-981f-571baf427806",
              "DisplayName": "admin",
              "UserPrincipalName": "admin@directorydomain.onmicrosoft.com",
              "LastPasswordChangeTimestamp": "2/10/2016 6:54:27 PM",
              "FirstName": "admin",
              "PasswordNeverExpires": false,
              "LastName": "admin",
              "LiveId": "10033FFF9627C2B2"
            }
            ...
          ],
          "resultCount": 4,
          "pagedResultsCookie": null,
          "totalPagedResultsPolicy": "NONE",
          "totalPagedResults": -1,
          "remainingPagedResults": -1
        }
1. Read a specific user (pick up an _id from the Search ALL):

        $ curl --request GET \
           --url http://localhost:8080/openidm/system/azureadpowershell/account/f37902f1-55fd-4fe9-981f-571baf427806 \
           --header 'cache-control: no-cache' \
           --header 'content-type: application/json' \
           --header 'x-openidm-password: openidm-admin' \
           --header 'x-openidm-username: openidm-admin'

    Returns for instance:

        {
          "_id": "f37902f1-55fd-4fe9-981f-571baf427806",
          "DisplayName": "admin",
          "UserPrincipalName": "admin@directorydomain.onmicrosoft.com",
          "LastPasswordChangeTimestamp": "2/10/2016 6:54:27 PM",
          "FirstName": "admin",
          "PasswordNeverExpires": false,
          "LastName": "admin",
          "LiveId": "10033FFF9627C2B2"
        }
1. Create a user:

        $ curl --request POST \
            --url 'http://localhost:8080/openidm/system/azureadpowershell/account?_action=create' \
            --header 'cache-control: no-cache' \
            --header 'content-type: application/json' \
            --header 'x-openidm-password: openidm-admin' \
            --header 'x-openidm-username: openidm-admin' \
            --data '{  \
                "PasswordNeverExpires": false, \
                "AlternateEmailAddresses": [ \
                "Roger.Smith@domain.com"  \
                ], \
                "LastName": "Smith", \
                "PreferredLanguage": "en-US", \
                "FirstName": "Roger", \
                "UserPrincipalName": "Roger.Smith@directorydomain.onmicrosoft.com", \
                "DisplayName": "Mr. Smith"
            }

    Returns:

        {
            "_id": "f0ae59a3-2e70-416c-bf10-5d801c55d36b",
            "DisplayName": "Mr. Smith",
            "AlternateEmailAddresses": [
                "Roger.Smith@domain.com"
            ],
            "UserPrincipalName": "Roger.Smith@directorydomain.onmicrosoft.com",
            "LastPasswordChangeTimestamp": "2/10/2016 10:51:19 PM",
            "FirstName": "Roger",
            "PasswordNeverExpires": false,
            "LastName": "Smith",
            "LiveId": "1003BFFD962C90F7",
            "PreferredLanguage": "en-US"
        }

1. Patch the user:

        $ curl --request PATCH \
            --url http://localhost:8080/openidm/system/azureadpowershell/account/f0ae59a3-2e70-416c-bf10-5d801c55d36b \
            --header 'cache-control: no-cache' \
            --header 'content-type: application/json' \
            --header 'if-match: *' \
            --header 'x-openidm-password: openidm-admin' \
            --header 'x-openidm-username: openidm-admin' \
            --data '[ \
                        {  \
                            "operation": "replace", \
                            "field": "FirstName", \
                            "value": "ModifiedFirstName" \
                        }  \
                ]'

    Returns:

        {
          "_id": "f0ae59a3-2e70-416c-bf10-5d801c55d36b",
          "DisplayName": "Mr. Smith",
          "AlternateEmailAddresses": [
            "Roger.Smith@fake.com"
          ],
          "UserPrincipalName": "Roger.Smith@directorydomain.onmicrosoft.com",
          "LastPasswordChangeTimestamp": "2/10/2016 10:57:29 PM",
          "FirstName": "ModifiedFirstName",
          "PasswordNeverExpires": false,
          "LastName": "Smith",
          "LiveId": "10030000962B0237",
          "PreferredLanguage": "en-US"
        }

1. Delete the created user:

        $ curl --request DELETE \
            --url http://localhost:8080/openidm/system/azureadpowershell/account/f0ae59a3-2e70-416c-bf10-5d801c55d36b \
            --header 'cache-control: no-cache' \
            --header 'content-type: application/json' \
            --header 'x-openidm-password: openidm-admin' \
            --header 'x-openidm-username: openidm-admin'

    Returns:

        {
          "_id": "f0ae59a3-2e70-416c-bf10-5d801c55d36b",
          "DisplayName": "Mr. Smith",
          "AlternateEmailAddresses": [
            "Roger.Smith@domain.com"
          ],
          "UserPrincipalName": "Roger.Smith@directorydomain.onmicrosoft.com",
          "LastPasswordChangeTimestamp": "2/10/2016 10:51:19 PM",
          "FirstName": "ModifiedFirstName",
          "PasswordNeverExpires": false,
          "LastName": "Smith",
          "LiveId": "1003BFFD962C90F7",
          "PreferredLanguage": "en-US"
        }

