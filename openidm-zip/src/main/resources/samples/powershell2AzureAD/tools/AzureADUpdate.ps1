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
    This is a sample Update script for users and groups on Azure AD

.DESCRIPTION
	This script leverages standard cmdlets from MsOnline module.
	For the users: Set-MsolUser, Set-MsolUserPassword and Set-MsolUserPrincipalName
	For the groups: Set-MsolGroup
	
.INPUT VARIABLES
	The connector sends us the following:
	- <prefix>.Configuration : handler to the connector's configuration object
	- <prefix>.Options: a handler to the Operation Options
	- <prefix>.Operation: an OperationType correponding to the action ("UPDATE" here)
	- <prefix>.ObjectClass: an ObjectClass describing the Object class (__ACCOUNT__ / __GROUP__ / other)
	- <prefix>.Attributes: A collection of ConnectorAttributes to update
	- <prefix>.Uid: Corresponds to the OpenICF __UID__ attribute for the entry to update
	
.RETURNS
	Must return the user unique ID (__UID__) if either UID or Revision has been modified 
	To do so, set the <prefix>.Result.Uid property with the modified Uid object

.NOTES
    Prerequisite   : PowerShell V2 and later
#>
$attrutil = [Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeUtil]
$secutil = [Org.IdentityConnectors.Common.Security.SecurityUtil]

function Update-Group ($attributes)
{
	$accessor = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributesAccessor($attributes)
	
	# According to https://msdn.microsoft.com/en-us/library/azure/dn194086
	#
	# Set-MsolGroup [-Description <string>] [-DisplayName <string>] [-ManagedBy <string>] [-ObjectId <Guid>] [-TenantId <Guid>]
	
	# We're going to use PowerShell Splatting
	$param = @{"ObjectId" = $Connector.Uid.GetUidValue()}
	$modification = $false
	
	# Most likely, this is a membership update
	$addMembers = $accessor.FindStringList("__ADD_MEMBERS__")
	$removeMembers = $accessor.FindStringList("__REMOVE_MEMBERS__")
	
	if (($addMembers -ne $null) -and ($addMembers.Count -gt 0))
	{
		try
		{
			foreach($id in $addMembers)
			{
				$res = Add-MsolGroupMember -GroupMemberObjectId $id -GroupObjectId $Connector.Uid.GetUidValue()
			}
		}
		catch
		{
			throw
		}
	}
	
	if (($removeMembers -ne $null) -and ($removeMembers.Count -gt 0))
	{
		try
		{
			foreach($id in $removeMembers)
			{
				$res = Remove-MsolGroupMember -GroupMemberObjectId $id -GroupObjectId $Connector.Uid.GetUidValue()
			}
		}
		catch
		{
			throw
		}
	}
	
	if ($accessor.GetName() -ne $null)
	{
		$param.Add("DisplayName", $accessor.GetName().GetNameValue())
		$modification = $true
	}

	# Standard attributes - single value String
	$standardSingle = @("Description", "ManagedBy")
	foreach ($name in $standardSingle)
	{
		$val = $accessor.FindString($name)
		if ($val -ne $null) 
		{	 
			$param.Add($name,$val)
			$modification = $true
		}
	}

	if ($modification)
	{
		try 
		{
			$group = Set-MsolGroup @param 
		}
		catch
		{
			throw
		}
	}
	# We return the original __UID__ since no change
	$Connector.Uid
}

function Update-User ($attributes)
{
	$accessor = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributesAccessor($attributes)

	# According to https://msdn.microsoft.com/en-us/library/azure/dn194136
	#
	# Set-MsolUser [-AlternateEmailAddresses <string[]>] [-BlockCredential <Boolean>] [-City <string>] 
	# [-Country <string>] [-Department <string>] [-DisplayName <string>] [-Fax <string>] [-FirstName <string>] 
	# [-ImmutableId <string>] [-LastName <string>] [-MobilePhone <string>] [-ObjectId <Guid>] [-Office <string>] 
	# [-PasswordNeverExpires <Boolean>] [-PhoneNumber <string>] [-PostalCode <string>] [-PreferredLanguage <string>] 
	# [-State <string>] [-StreetAddress <string>] [-StrongPasswordRequired <Boolean>] [-TenantId <Guid>] 
	# [-Title <string>] [-UsageLocation <string>] [-UserPrincipalName <string>] [<CommonParameters>]
	
	# We're going to use PowerShell Splatting
	$param = @{"ObjectId" = $Connector.Uid.GetUidValue()}
	$modification = $false
	
	# Standard attributes - single value String
	$standardSingle = @("City","Country ","Department","DisplayName", "Fax","FirstName","LastName","MobilePhone","Office", "ImmutableId",
						"PhoneNumber","PostalCode","PreferredLanguage","State","StreetAddress","Title","UsageLocation")
	
	foreach ($name in $standardSingle)
	{
		$val = $accessor.FindString($name)
		if ($val -ne $null) 
		{	 
			$param.Add($name,$val)
			$modification = $true
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
			$modification = $true
		}
	}
	
	# Control attributes - boolean
	$controlAttr = @("BlockCredential", "PasswordNeverExpires", "StrongPasswordRequired" )
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
			$modification = $true
		}
	}
	
	# Password change - has a dedicated cmdlet
	# According to: https://msdn.microsoft.com/en-us/library/azure/dn194140
	#
	# Set-MsolUserPassword -ObjectId <Guid> [-ForceChangePassword <Boolean>] 
	# [-NewPassword <string>] [-TenantId <Guid>] [<CommonParameters>]
	
	$password = $accessor.GetPassword()
	If ($password -ne $null)
	{
		$pparm = @{"ObjectId" = $Connector.Uid.GetUidValue()}
		$pparm.Add("NewPassword",$secutil::Decrypt($password))
		$val = $accessor.FindBoolean("ForceChangePassword")
		if ($val -ne $null) 
		{	 
			if ($val)
			{
				$param.Add("ForceChangePassword",$true)
			}
			else
			{
				$param.Add("ForceChangePassword",$false)
			}
		}
		try
		{
			$user = Set-MsolUserPassword @pparm
		}
		catch
		{
			throw
		}
	}
	
	# UserPrincipalName change - has a dedicated cmdlet
	# According to: https://msdn.microsoft.com/en-us/library/azure/dn194135
	#
	# Set-MsolUserPrincipalName -NewUserPrincipalName <string> -ObjectId <Guid> 
	# [-ImmutableId <string>] [-NewPassword <string>] [-TenantId <Guid>] [<CommonParameters>]
	
	$upn = $accessor.GetName()
	if ( $upn -ne $null)
	{
		$uparm = @{"ObjectId" = $Connector.Uid.GetUidValue()}
		$uparm.Add("NewUserPrincipalName", $upn.GetNameValue())
		try
		{
			$user = Set-MsolUserPrincipalName @uparm
		}
		catch
		{
			throw
		}
	}
	
	# What's left? 
    # LicenseOptions: License options for license assignment. Used to selectively disable individual service plans within a SKU.
	# TenantId
	
	if($modification)
	{
		try
		{
			$user = Set-MsolUser @param
		}
		catch
		{
			throw
		}
	}
	# We return the original __UID__ since we did not change it
	$Connector.Uid
}

try
{
    $scriptPath = split-path -parent $Connector.Configuration.UpdateScriptFileName
    . $scriptPath\AzureADCommon.ps1

    if ($Connector.Operation -eq "UPDATE")
    {
        authenticateSession($Connector)

        switch ($Connector.ObjectClass.Type)
        {
            "__ACCOUNT__"
            {
                $Connector.Result.Uid = Update-User $Connector.Attributes
            }
            "__GROUP__"
            {
                $Connector.Result.Uid = Update-Group $Connector.Attributes
            }
            default {throw "Unsupported type: $($Connector.ObjectClass.Type)"}
        }
    }
    else
    {
        throw new Org.IdentityConnectors.Framework.Common.Exceptions.ConnectorException(
                "UpdateScript can not handle operation: $($Connector.Operation)")
    }
}
catch #Re-throw the original exception
{
	throw
}
