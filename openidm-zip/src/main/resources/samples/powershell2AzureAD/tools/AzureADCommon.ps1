# The contents of this file are subject to the terms of the Common Development and
# Distribution License (the License). You may not use this file except in compliance with the
# License.
#
# You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
# specific language governing permission and limitations under the License.
#
# When distributing Covered Software, include this CDDL Header Notice in each file and include
# the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
# Header, with the fields enclosed by brackets [] replaced by your own identifying
# information: "Portions copyright [year] [name of copyright owner]".
#
# Copyright 2016 ForgeRock AS.
#
#REQUIRES -Version 2.0

<#
.SYNOPSIS
    Given the Connector user credentials, this will establish a Azure Active Directory session.
    If the username of the last authenticated user of the session is different from the credentials,
    the request is denied.

.DESCRIPTION
    First this tests if the session is active by calling Get-MsolDomain.
    If active then this compares the last Login ($Env:OpenICF_AAD_Login) to the current login
        ($Connector.Configuration.Login).
    If they are different then it exits with a ConnectorSecurityException as the session needs to reestablished by
        restarting the powersell connector.
    If the last login is the same as before, this will exit with exception.
    If the last login is null and the session is not active then connect-msolservice is used to authenticate the user
        credentials, and the login is stored as ($Env:OpenICF_AAD_Login) to be used on the next run.
    As of this writing, there is no disconnect-msolservice or get-session-user that helpd support connect-msolservice.
.INPUT
	- <prefix>.Configuration : handler to the connector's configuration object.

.RETURNS
	Nothing. Should throw an exception if authentication failed

.NOTES
    Prerequisite   : PowerShell V2 and later
#>
function authenticateSession($Connector) {

    $login = $Connector.Configuration.Login
    try {
        Get-MsolDomain -ErrorAction Stop > $null
        $isSessionActive = $true;
    } catch {
        $isSessionActive = $false;
    }
    if ($isSessionActive -and $login -ne $Env:OpenICF_AAD_Login) {
        throw New-object Org.IdentityConnectors.Framework.Common.Exceptions.ConnectorSecurityException(
            "Username does not match last authenticated user.  Change the username, or the ConnectorServer needs to be restarted to reset the session.")
    }

    if ($isSessionActive -eq $false) {
        $msolcred = New-object System.Management.Automation.PSCredential $login,
            $Connector.Configuration.Password.ToSecureString()
        connect-msolservice -credential $msolcred
        try {
            # Test if the credentials were successful.
            Get-MsolDomain -ErrorAction Stop > $null
            Write-Verbose -verbose "New session created"
            $Env:OpenICF_AAD_Login = $login
        } catch {
            # If unable to get the domain, then authentication failed.
            throw New-object Org.IdentityConnectors.Framework.Common.Exceptions.InvalidCredentialException(
                "ERROR Authentication failed. Check the user credentials and that the user's domain is local to the AzureAD tenant. Login=$login")
        }
    }
}