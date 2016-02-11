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
    This is a sample Test script for Azure Active Directory

.DESCRIPTION
	The script tries to fetch the company-level information.

.INPUT
	The connector sends us the following:
	- <prefix>.Configuration : handler to the connector's configuration object
	- <prefix>.Operation: String correponding to the action ("TEST" here)

.RETURNS
	Nothing. Should throw an exception if test failed

.NOTES
    Prerequisite   : PowerShell V2 and later
#>

Write-Verbose -Verbose "Running Test script."

try
{
    $scriptPath = split-path -parent $Connector.Configuration.TestScriptFileName
    . $scriptPath\AzureADCommon.ps1

    if ($Connector.Operation -eq "TEST")
    {
        authenticateSession($Connector)
    }
    else
    {
 	    throw New-object Org.IdentityConnectors.Framework.Common.Exceptions.ConnectorException("TestScript can not handle operation: $Connector.Operation")
    }
}
catch #Rethrow the original exception
{
	throw
}
