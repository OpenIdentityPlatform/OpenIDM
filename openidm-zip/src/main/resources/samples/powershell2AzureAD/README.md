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

The azureADScripts/ folder contains several sample scripts for the OpenICF PowerShell Connector.
This sample uses the MS Azure Active Directory PowerShell module. See:
[http://technet.microsoft.com/en-us/library/hh852274.aspx](http://technet.microsoft.com/en-us/library/hh852274.aspx).

A Microsoft Azure Account is required to setup an Azure Active Directory.  If you don't have one, create a Microsoft
Azure account. See: [http://azure.microsoft.com](http://azure.microsoft.com)

Follow these steps to use this sample:

1.	Set up an Azure AD Cloud service.
    1.	[http://azure.microsoft.com](http://azure.microsoft.com)
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
    1.	Navigate to [https://portal.azure.com/](https://portal.azure.com/) and log in if needed.
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
        For example: `admin@your_domain.onmicrosoft.com`.
    1.	You will be asked to change the password.  Click "Update password and sign in" when ready.

1.  Install the Windows Azure Active Directory Module for Windows PowerShell on a Windows machine with connectivity to
    Azure.  https://msdn.microsoft.com/library/azure/jj151815.aspx

1.  Verify connectivity between this Windows box and AzureAD:
    1.	Open a PowerShell window.
    1.	At the command prompt type `Connect-Msolservice`.
    1.	A popup should appear asking for the user credentials.  Enter the credentials of the admin account created
        for the directory service.
    1.	If no error occurs, you are connected.

1.  Install the OpenICF .NET connector server on the Windows box.
    See: [https://forgerock.org/openidm/doc/bootstrap/integrators-guide/index.html#install-.net-connector](https://forgerock.org/openidm/doc/bootstrap/integrators-guide/index.html#install-.net-connector)

1.  Install the PowerShell connector on the Windows box. See:
    [https://forgerock.org/openidm/doc/bootstrap/integrators-guide/index.html#powershell-connector](https://forgerock.org/openidm/doc/bootstrap/integrators-guide/index.html#powershell-connector)

1.  Copy all the azureADScripts/*.ps1 scripts to the Windows box where you installed the .NET connector server and the
    PowerShell connector.  In the sample provisioner configuration file
    (`samples/provisioners/provisioner.openicf-azureadpowershell.json`), files are expected to be in the folder
    `C:/openidm/samples/powershell2AzureAD/azureADScripts/`

1.  Copy the remote connector provisioner file to openidm/conf/ from
    openidm/samples/provisioners/provisioner.openicf.connectorinfoprovider.json
    *  Adapt to your local configuration of the Windows Box with the installed connector server.
    *  Remove the "protocol" configuration, to be compatible with a 1.4 connector version.

1.  Edit the sample PowerShell connector provisioner file to suit your installation.
    * The provisioner file is samples/provisioners/provisioner.openicf-azureadpowershell.json. You may need to 
        change the file paths. For instance, the following may not reflect your Windows folder location. The 
        other settings should not be changed:

        "configurationProperties" : {
            "AuthenticateScriptFileName" : null,
            "CreateScriptFileName" : "C:/openidm/samples/powershell2AzureAD/azureADScripts/AzureADCreate.ps1",
            "DeleteScriptFileName" : "C:/openidm/samples/powershell2AzureAD/azureADScripts/AzureADDelete.ps1",
            "ResolveUsernameScriptFileName" : null,
            "SchemaScriptFileName" : "C:/openidm/samples/powershell2AzureAD/azureADScripts/AzureADSchema.ps1",
            "SearchScriptFileName" : "C:/openidm/samples/powershell2AzureAD/azureADScripts/AzureADSearch.ps1",
            "SyncScriptFileName" : null,
            "TestScriptFileName" : "C:/openidm/samples/powershell2AzureAD/azureADScripts/AzureADTest.ps1",
            "UpdateScriptFileName" : "C:/openidm/samples/powershell2AzureAD/azureADScripts/AzureADUpdate.ps1",
                
    * Login and Password should be set to the credentials of the user admin of your Azure Directory.  For example:
    `admin@your_domain.onmicrosoft.com`.  Unless your azure domain is fully customized, the admin account will
    have `onmicrosoft.com` in the domain.
1. Copy the provisioner file to the openidm/conf/ folder.

OpenIDM REST Calls
====================================================================

Note: In addition to the calls below, you may also be interested in looking at the 
test/src/main/java/AzureADDemonstrationTest class. 
This class runs through variations of the calls below and validates that the Powershell scripts are deployed correctly.

1. Validate that the connection with the Azure connector is OK:

        curl --request POST \
         --url 'http://localhost:8080/openidm/system/azureadpowershell?_action=test' \
         --header 'x-openidm-username: openidm-admin' \
         --header 'x-openidm-password: openidm-admin' 
    Returns:

        {
            "name" : "azureadpowershell",
            "enabled" : true,
            "config" : "config/provisioner.openicf/azureadpowershell",
            "objectTypes" : [
                "__ALL__",
                "account",
                "group"
            ],
            "connectorRef" : {
                "bundleName" : "MsPowerShell.Connector",
                "connectorName" : "Org.ForgeRock.OpenICF.Connectors.MsPowerShell.MsPowerShellConnector",
                "bundleVersion" : "1.4.2.0"
            },
            "displayName" : "PowerShell Connector ",
            "ok" : true
        }

1. Get users by query filter: 

        curl --request GET \
         --url 'http://localhost:8080/openidm/system/azureadpowershell/account?_queryFilter=UserPrincipalName%20sw%20%22test_%22' \
         --header 'x-openidm-username: openidm-admin' \
         --header 'x-openidm-password: openidm-admin' 
    Returns:

        {
            "result" : [
                {
                    "_id" : "c4948742-7d3d-4d8c-b19c-585abec42d0b",
                    "DisplayName" : "Mr. Smith",
                    "LastPasswordChangeTimestamp" : "5/3/2016 4:16:13 PM",
                    "PasswordNeverExpires" : false,
                    "UserPrincipalName" : "test_Roger.Smith@openidm.onmicrosoft.com",
                    "FirstName" : "ModifiedFirstName",
                    "LiveId" : "10033FFF97A2104D",
                    "LastName" : "Smith"
                }
            ],
            "resultCount" : 1,
            "pagedResultsCookie" : null,
            "totalPagedResultsPolicy" : "NONE",
            "totalPagedResults" : -1,
            "remainingPagedResults" : -1
        }

1. Delete a user: 

        curl --request DELETE \
         --url 'http://localhost:8080/openidm/system/azureadpowershell/account/c4948742-7d3d-4d8c-b19c-585abec42d0b' \
         --header 'x-openidm-username: openidm-admin' \
         --header 'x-openidm-password: openidm-admin' 
    Returns:

        {
            "_id" : "c4948742-7d3d-4d8c-b19c-585abec42d0b",
            "DisplayName" : "Mr. Smith",
            "LastPasswordChangeTimestamp" : "5/3/2016 4:16:13 PM",
            "PasswordNeverExpires" : false,
            "UserPrincipalName" : "test_Roger.Smith@openidm.onmicrosoft.com",
            "FirstName" : "ModifiedFirstName",
            "LiveId" : "10033FFF97A2104D",
            "LastName" : "Smith"
        }

1. Get users by query filter: 

        curl --request GET \
         --url 'http://localhost:8080/openidm/system/azureadpowershell/account?_queryFilter=UserPrincipalName%20sw%20%22test_%22' \
         --header 'x-openidm-username: openidm-admin' \
         --header 'x-openidm-password: openidm-admin' 
    Returns:

        {
            "result" : [ ],
            "resultCount" : 0,
            "pagedResultsCookie" : null,
            "totalPagedResultsPolicy" : "NONE",
            "totalPagedResults" : -1,
            "remainingPagedResults" : -1
        }

1. Get groups by filter: 

        curl --request GET \
         --url 'http://localhost:8080/openidm/system/azureadpowershell/group?_queryFilter=DisplayName%20sw%20%22test_%22' \
         --header 'x-openidm-username: openidm-admin' \
         --header 'x-openidm-password: openidm-admin' 
    Returns:

        {
            "result" : [
                {
                    "_id" : "4998d6cd-860b-4d42-8e3c-610472ee3a87",
                    "Description" : "Alpha Group Description_updated",
                    "Members" : [ ],
                    "DisplayName" : "test_alpha group",
                    "GroupType" : "Security",
                    "objectId" : "4998d6cd-860b-4d42-8e3c-610472ee3a87"
                }
            ],
            "resultCount" : 1,
            "pagedResultsCookie" : null,
            "totalPagedResultsPolicy" : "NONE",
            "totalPagedResults" : -1,
            "remainingPagedResults" : -1
        }

1. Delete a group:

        curl --request DELETE \
         --url 'http://localhost:8080/openidm/system/azureadpowershell/group/4998d6cd-860b-4d42-8e3c-610472ee3a87' \
         --header 'x-openidm-username: openidm-admin' \
         --header 'x-openidm-password: openidm-admin' 
    Returns:

        {
            "_id" : "4998d6cd-860b-4d42-8e3c-610472ee3a87",
            "Description" : "Alpha Group Description_updated",
            "Members" : [ ],
            "DisplayName" : "test_alpha group",
            "GroupType" : "Security",
            "objectId" : "4998d6cd-860b-4d42-8e3c-610472ee3a87"
        }

1. Get groups by filter: 

        curl --request GET \
         --url 'http://localhost:8080/openidm/system/azureadpowershell/group?_queryFilter=DisplayName%20sw%20%22test_%22' \
         --header 'x-openidm-username: openidm-admin' \
         --header 'x-openidm-password: openidm-admin' 
    Returns:

        {
            "result" : [ ],
            "resultCount" : 0,
            "pagedResultsCookie" : null,
            "totalPagedResultsPolicy" : "NONE",
            "totalPagedResults" : -1,
            "remainingPagedResults" : -1
        }

1. Create a user: 

        curl --request POST \
         --url 'http://localhost:8080/openidm/system/azureadpowershell/account?_action=create' \
         --header 'x-openidm-username: openidm-admin' \
         --header 'x-openidm-password: openidm-admin' \
         --header 'content-type: application/json' \
         --data '{ \
             "UserPrincipalName" : "test_Roger.Smith@openidm.onmicrosoft.com", \
             "LastName" : "Smith", \
             "FirstName" : "Roger", \
             "DisplayName" : "Mr. Smith", \
             "PasswordNeverExpires" : false \
         }'

    Returns:

        {
            "_id" : "314ab35c-4148-48a1-b028-9d63f8edf208",
            "DisplayName" : "Mr. Smith",
            "LastPasswordChangeTimestamp" : "5/3/2016 4:19:05 PM",
            "PasswordNeverExpires" : false,
            "UserPrincipalName" : "test_Roger.Smith@openidm.onmicrosoft.com",
            "FirstName" : "Roger",
            "LiveId" : "1003000097B46C5A",
            "LastName" : "Smith"
        }

1. Create a user: 

        curl --request POST \
         --url 'http://localhost:8080/openidm/system/azureadpowershell/account?_action=create' \
         --header 'x-openidm-username: openidm-admin' \
         --header 'x-openidm-password: openidm-admin' \
         --header 'content-type: application/json' \
         --data '{ \
             "UserPrincipalName" : "test_Roger.Smith@openidm.onmicrosoft.com", \
             "LastName" : "Smith", \
             "FirstName" : "Roger", \
             "DisplayName" : "Mr. Smith", \
             "PasswordNeverExpires" : false \
         }'

    Returns:

        {
            "code" : 500,
            "reason" : "Internal Server Error",
            "message" : "Operation CREATE failed with ConnectorException on system object: test_Roger.Smith@openidm.onmicrosoft.com"
        }

1. Get User By ID: 

        curl --request GET \
         --url 'http://localhost:8080/openidm/system/azureadpowershell/account/314ab35c-4148-48a1-b028-9d63f8edf208' \
         --header 'x-openidm-username: openidm-admin' \
         --header 'x-openidm-password: openidm-admin' 
    Returns:

        {
            "_id" : "314ab35c-4148-48a1-b028-9d63f8edf208",
            "DisplayName" : "Mr. Smith",
            "LastPasswordChangeTimestamp" : "5/3/2016 4:19:05 PM",
            "PasswordNeverExpires" : false,
            "UserPrincipalName" : "test_Roger.Smith@openidm.onmicrosoft.com",
            "FirstName" : "Roger",
            "LiveId" : "1003000097B46C5A",
            "LastName" : "Smith"
        }

1. Patch a user: 

        curl --request PATCH \
         --url 'http://localhost:8080/openidm/system/azureadpowershell/account/314ab35c-4148-48a1-b028-9d63f8edf208' \
         --header 'x-openidm-username: openidm-admin' \
         --header 'x-openidm-password: openidm-admin' \
         --header 'content-type: application/json' \
         --header 'if-match: *' \
         --data '[ \
             { \
                 "operation" : "replace", \
                 "field" : "FirstName", \
                 "value" : "ModifiedFirstName" \
             } \
         ]'

    Returns:

        {
            "_id" : "314ab35c-4148-48a1-b028-9d63f8edf208",
            "DisplayName" : "Mr. Smith",
            "LastPasswordChangeTimestamp" : "5/3/2016 4:19:05 PM",
            "PasswordNeverExpires" : false,
            "UserPrincipalName" : "test_Roger.Smith@openidm.onmicrosoft.com",
            "FirstName" : "ModifiedFirstName",
            "LiveId" : "1003000097B46C5A",
            "LastName" : "Smith"
        }

1. Get User By ID: 

        curl --request GET \
         --url 'http://localhost:8080/openidm/system/azureadpowershell/account/314ab35c-4148-48a1-b028-9d63f8edf208' \
         --header 'x-openidm-username: openidm-admin' \
         --header 'x-openidm-password: openidm-admin' 
    Returns:

        {
            "_id" : "314ab35c-4148-48a1-b028-9d63f8edf208",
            "DisplayName" : "Mr. Smith",
            "LastPasswordChangeTimestamp" : "5/3/2016 4:19:05 PM",
            "PasswordNeverExpires" : false,
            "UserPrincipalName" : "test_Roger.Smith@openidm.onmicrosoft.com",
            "FirstName" : "ModifiedFirstName",
            "LiveId" : "1003000097B46C5A",
            "LastName" : "Smith"
        }

1. Create a group: 

        curl --request POST \
         --url 'http://localhost:8080/openidm/system/azureadpowershell/group?_action=create' \
         --header 'x-openidm-username: openidm-admin' \
         --header 'x-openidm-password: openidm-admin' \
         --header 'content-type: application/json' \
         --data '{ \
             "DisplayName" : "test_alpha group", \
             "Description" : "Alpha Group Description" \
         }'

    Returns:

        {
            "_id" : "fcf8237d-0197-4de9-8f36-aaf0ba344091",
            "Description" : "Alpha Group Description",
            "Members" : [ ],
            "DisplayName" : "test_alpha group",
            "GroupType" : "Security",
            "objectId" : "fcf8237d-0197-4de9-8f36-aaf0ba344091"
        }

1. Add a member to a group by writing entire group with updated Members list:

        curl --request PUT \
         --url 'http://localhost:8080/openidm/system/azureadpowershell/group/fcf8237d-0197-4de9-8f36-aaf0ba344091' \
         --header 'x-openidm-username: openidm-admin' \
         --header 'x-openidm-password: openidm-admin' \
         --header 'content-type: application/json' \
         --header 'if-match: *' \
         --data '{ \
             "Members" : [ \
                 { \
                     "ObjectId" : "314ab35c-4148-48a1-b028-9d63f8edf208" \
                 } \
             ] \
         }'

    Returns:

        {
            "_id" : "fcf8237d-0197-4de9-8f36-aaf0ba344091",
            "Description" : "Alpha Group Description",
            "Members" : [
                {
                    "ObjectId" : "314ab35c-4148-48a1-b028-9d63f8edf208",
                    "DisplayName" : "Mr. Smith",
                    "GroupMemberType" : "User",
                    "EmailAddress" : "test_Roger.Smith@openidm.onmicrosoft.com"
                }
            ],
            "DisplayName" : "test_alpha group",
            "GroupType" : "Security",
            "objectId" : "fcf8237d-0197-4de9-8f36-aaf0ba344091"
        }

1. Get Group by ID:

        curl --request GET \
         --url 'http://localhost:8080/openidm/system/azureadpowershell/group/fcf8237d-0197-4de9-8f36-aaf0ba344091' \
         --header 'x-openidm-username: openidm-admin' \
         --header 'x-openidm-password: openidm-admin' 
    Returns:

        {
            "_id" : "fcf8237d-0197-4de9-8f36-aaf0ba344091",
            "Description" : "Alpha Group Description",
            "Members" : [
                {
                    "ObjectId" : "314ab35c-4148-48a1-b028-9d63f8edf208",
                    "DisplayName" : "Mr. Smith",
                    "GroupMemberType" : "User",
                    "EmailAddress" : "test_Roger.Smith@openidm.onmicrosoft.com"
                }
            ],
            "DisplayName" : "test_alpha group",
            "GroupType" : "Security",
            "objectId" : "fcf8237d-0197-4de9-8f36-aaf0ba344091"
        }

1. Update a group: 

        curl --request PUT \
         --url 'http://localhost:8080/openidm/system/azureadpowershell/group/fcf8237d-0197-4de9-8f36-aaf0ba344091' \
         --header 'x-openidm-username: openidm-admin' \
         --header 'x-openidm-password: openidm-admin' \
         --header 'content-type: application/json' \
         --header 'if-match: *' \
         --data '{ \
             "_id" : "fcf8237d-0197-4de9-8f36-aaf0ba344091", \
             "Description" : "Alpha Group Description_updated", \
             "Members" : [ \
                 { \
                     "ObjectId" : "314ab35c-4148-48a1-b028-9d63f8edf208", \
                     "DisplayName" : "Mr. Smith", \
                     "GroupMemberType" : "User", \
                     "EmailAddress" : "test_Roger.Smith@openidm.onmicrosoft.com" \
                 } \
             ], \
             "DisplayName" : "test_alpha group", \
             "GroupType" : "Security", \
             "objectId" : "fcf8237d-0197-4de9-8f36-aaf0ba344091" \
         }'

    Returns:

        {
            "_id" : "fcf8237d-0197-4de9-8f36-aaf0ba344091",
            "Description" : "Alpha Group Description_updated",
            "Members" : [
                {
                    "ObjectId" : "314ab35c-4148-48a1-b028-9d63f8edf208",
                    "DisplayName" : "Mr. Smith",
                    "GroupMemberType" : "User",
                    "EmailAddress" : "test_Roger.Smith@openidm.onmicrosoft.com"
                }
            ],
            "DisplayName" : "test_alpha group",
            "GroupType" : "Security",
            "objectId" : "fcf8237d-0197-4de9-8f36-aaf0ba344091"
        }

1. Remove member from a group with update: (get, then update with modified members list)

        curl --request PUT \
         --url 'http://localhost:8080/openidm/system/azureadpowershell/group/fcf8237d-0197-4de9-8f36-aaf0ba344091' \
         --header 'x-openidm-username: openidm-admin' \
         --header 'x-openidm-password: openidm-admin' \
         --header 'content-type: application/json' \
         --header 'if-match: *' \
         --data '{ \
             "_id" : "fcf8237d-0197-4de9-8f36-aaf0ba344091", \
             "Description" : "Alpha Group Description_updated", \
             "Members" : [ ], \
             "DisplayName" : "test_alpha group", \
             "GroupType" : "Security", \
             "objectId" : "fcf8237d-0197-4de9-8f36-aaf0ba344091" \
         }'

    Returns:

        {
            "_id" : "fcf8237d-0197-4de9-8f36-aaf0ba344091",
            "Description" : "Alpha Group Description_updated",
            "Members" : [ ],
            "DisplayName" : "test_alpha group",
            "GroupType" : "Security",
            "objectId" : "fcf8237d-0197-4de9-8f36-aaf0ba344091"
        }
