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
    This is a sample Create script for users and groups in Azure AD

.DESCRIPTION
	This script leverages New-MsolUser and New-MsolGroup cmdlets from MsOnline module.
	
.INPUT VARIABLES
	The connector sends us the following:
	- <prefix>.Configuration : handler to the connector's configuration object
	- <prefix>.Options: a handler to the Operation Options
	- <prefix>.Operation: String correponding to the action ("CREATE" here)
	- <prefix>.ObjectClass: the Object class object (__ACCOUNT__ / __GROUP__ / other)
	- <prefix>.Attributes: A collection of ConnectorAttributes representing the entry attributes
	- <prefix>.Id: Corresponds to the OpenICF __NAME__ atribute if it is provided as part of the attribute set,
	 otherwise null
	
.RETURNS
	Must return the user unique ID (OpenICF __UID__).
	To do so, set the <prefix>.Result.Uid property

.NOTES
    Prerequisite   : PowerShell V2 and later
#>


$secutil = [Org.IdentityConnectors.Common.Security.SecurityUtil]

function Create-NewGroup ($attributes)
{
	$accessor = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributesAccessor($attributes)
	# According to https://msdn.microsoft.com/en-us/library/azure/dn194083
	#
	# New-MsolGroup [-Description <string>] [-DisplayName <string>] [-ManagedBy <string>] [-TenantId <Guid>] [<CommonParameters>]
	
	# We're going to use PowerShell Splatting
	$param = @{"DisplayName" = $accessor.GetName().GetNameValue()}

	# Standard attributes - single value String
	$standardSingle = @("Description", "ManagedBy")
	foreach ($name in $standardSingle)
	{
		$val = $accessor.FindString($name)
		if ($val -ne $null) 
		{	 
			$param.Add($name,$val)
		}
	}

	try 
	{
		$group = New-MsolGroup @param 
		$group.ObjectId.ToString()
	}
	catch
	{
		throw
	}
}

function Create-NewUser ($attributes)
{
	$accessor = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributesAccessor($attributes)

	# According to https://msdn.microsoft.com/en-us/library/azure/dn194096
	# 
	# New-MsolUser -DisplayName <string> -UserPrincipalName <string> 
	# [-AlternateEmailAddresses <string[]>] [-BlockCredential <Boolean>] 
	# [-City <string>] [-Country <string>] [-Department <string>] 
	# [-Fax <string>] [-FirstName <string>] [-ForceChangePassword <Boolean>] 
	# [-ImmutableId <string>] [-LastName <string>] [-LicenseAssignment <string[]>] 
	# [-LicenseOptions <LicenseOption[]>] [-MobilePhone <string>] [-Office <string>] 
	# [-Password <string>] [-PasswordNeverExpires <Boolean>] [-PhoneNumber <string>] 
	# [-PostalCode <string>] [-PreferredLanguage <string>] [-State <string>] 
	# [-StreetAddress <string>] 
	# [-StrongPasswordRequired <Boolean>] 
	# [-TenantId <Guid>] [-Title <string>] 
	# [-UsageLocation <string>] [<CommonParameters>]
	
	# UPN and DisplayName are mandatory on create
	# We're going to use PowerShell Splatting
	$param = @{"UserPrincipalName" = $accessor.GetName().GetNameValue()}
	$param.Add("DisplayName", $accessor.FindString("DisplayName"))
	
	# Standard attributes - single value String
	$standardSingle = @("City","Country ","Department","Fax","FirstName","LastName","MobilePhone","Office", "ImmutableId",
						"PhoneNumber","PostalCode","PreferredLanguage","State","StreetAddress","Title","UsageLocation")
	
	foreach ($name in $standardSingle)
	{
		$val = $accessor.FindString($name)
		if ($val -ne $null) 
		{	 
			$param.Add($name,$val)
		}
	}
	
	# Standard attributes - multi valued String
	$standardMulti = @("AlternateEmailAddresses", "LicenseAssignment")
	foreach ($name in $standardMulti)
	{
		$val = $accessor.FindStringList($name)
		if (($val -ne $null) -and ($val.Count -gt 0))
		{	
			$param.Add($name,$val)
		}
	}
	
	# Control attributes - boolean
	$controlAttr = @("BlockCredential", "ForceChangePassword", "PasswordNeverExpires", "StrongPasswordRequired" )
	foreach ($name in $controlAttr)
	{
		$val = $accessor.FindBoolean($name)
		if ($val -ne $null) 
		{	 
			if ($val)
			{
				$param.Add($name,$true)
			}
			else
			{
				$param.Add($name,$false)
			}
		}
	}
	
	# Password - need to be passed in clear text...
	# If the user is set to require a strong 
    #    password, then all of the following rules must be met:
    #    - The password must contain at least one lowercase letter
    #    - The password must contain at least one uppercase letter
    #    - The password must contain at least one non-alphanumeric character
    #    - The password cannot contain any spaces, tabs, or line breaks
    #    - The length of the password must be 8-16 characters
    #    - The user name cannot be contained in the password
    #    
    #    If this value is omitted, then a random password will be assigned to 
    #    the user.
	$password = $accessor.GetPassword()
	If ($password -ne $null)
	{
		$param.Add("Password",$secutil::Decrypt($password))
	}
	
	
	# What's left? 
    # LicenseOptions: License options for license assignment. Used to selectively disable individual service plans within a SKU.
	# TenantId
	try
	{
		$user = New-MsolUser @param
		$user.ObjectId.ToString()
	}
	catch
	{
		throw
	}
}

try
{

    $scriptPath = split-path -parent $Connector.Configuration.CreateScriptFileName
    . $scriptPath\AzureADCommon.ps1

    if ($Connector.Operation -eq "CREATE")
    {
        authenticateSession($Connector)

        switch ($Connector.ObjectClass.Type)
        {
            "__ACCOUNT__"
            {
                $Connector.Result.Uid = Create-NewUser $Connector.Attributes
            }
            "__GROUP__"
            {
                $Connector.Result.Uid = Create-NewGroup $Connector.Attributes
            }
            default {throw "Unsupported type: $($Connector.ObjectClass.Type)"}
        }
    }
    else
    {
	    throw new Org.IdentityConnectors.Framework.Common.Exceptions.ConnectorException("CreateScript can not handle operation: $($Connector.Operation)")
    }
}
catch #Re-throw the original exception
{
	throw
}
