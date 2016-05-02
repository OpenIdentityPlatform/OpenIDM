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
    File Name      : AzureADUpdate.ps1  
    Author         : Gael Allioux (gael.allioux@forgerock.com)
    Prerequisite   : PowerShell V2 and later
    Copyright      : 2015-2016 - ForgeRock AS    

	.LINK  
    Script posted over:  
    http://openicf.forgerock.org
		
	Azure Active Directory Module for Windows PowerShell
	https://msdn.microsoft.com/en-us/library/azure/jj151815.aspx
#>

$ErrorActionPreference = "Stop"
$VerbosePreference = "Continue"

$secutil    = [Org.IdentityConnectors.Common.Security.SecurityUtil]

function Update-Group ($attributes)
{
	$accessor = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributesAccessor($attributes)
	$gid = $Connector.Uid.GetUidValue()
	
	# Most likely, this is a membership update
	# According to https://msdn.microsoft.com/en-us/library/dn194129.aspx
	# Add-MsolGroupMember -GroupMemberObjectId <Guid> -GroupObjectId <Guid> [-GroupMemberType <string>] [-TenantId <Guid>] [<CommonParameters>]
	#
	# According to https://msdn.microsoft.com/en-us/library/dn194107.aspx
	# Remove-MsolGroupMember -GroupObjectId <Guid> [-GroupMemberObjectId <Guid>] [-GroupMemberType <string>] [-TenantId <Guid>] [<CommonParameters>]
	$members = $accessor.FindList("__MEMBERS__")
	
	if ($null -ne $members)
	{
		# Convert members to a more convenient HashMap
		# Need to be sure that ObjectId and GroupMemberType have been set correctly.
		$newMembers = @{}
		foreach ($member in $members)
		{
			if ($member.ContainsKey('ObjectId'))
			{
				if ($member.ContainsKey('GroupMemberType'))
				{
					if (($member["GroupMemberType"] -eq "User" ) -or ($member["GroupMemberType"] -eq "Group" ))
					{
						$newMembers.Add($member["ObjectId"], $member["GroupMemberType"])
					}
					else
					{
						Write-warning "Member has a bad GroupMemberType defined: $($member.GroupMemberType)"
						throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.InvalidAttributeValueException("Member has a bad GroupMemberType defined: $($member.GroupMemberType)")
					}
				}
				else 
				{
					# We assume User as the default
					$newMembers.Add($member["ObjectId"], "User")
				}
			}
			else 
			{
				Write-warning "Member has no ObjectId defined"
				throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.InvalidAttributeValueException("Member has no ObjectId defined")
			}
		}
		# First we need to get the current list of members
		# and track their type (user/group) as well
		$currentMembers = @{}
		$toAdd = @()
		$toRemove = @()
		Get-MsolGroupMember -All -GroupObjectId $gid | foreach { $currentMembers.Add($_.ObjectId.ToString(), $_.GroupMemberType) }
		
		# Compare the 2 lists to add/remove members from the group
		if ($currentMembers.Count -gt 0)
		{
			Write-verbose "Group contains $($currentMembers.Count) member(s)"
			# To remove
			$currentMembers.Keys | ? { ! $newMembers.ContainsKey($_) } | % { $toRemove += $_ } 
			Write-verbose "$($toRemove.Count) member(s) to remove"
			$toRemove | % { Remove-MsolGroupMember -GroupObjectId $gid -GroupMemberObjectId $_ -GroupMemberType $currentMembers[$_]  }
			
			# To add
			$newMembers.Keys | ? { ! $currentMembers.ContainsKey($_) } | % { $toAdd += $_ }
		}
		# No current members, add all the new members
		else
		{
			$toAdd += $newMembers.Keys
		}
		Write-verbose "$($toAdd.Count) member(s) to add"
		$toAdd | % { Add-MsolGroupMember -GroupObjectId $gid -GroupMemberObjectId $_ -GroupMemberType $newMembers[$_] }
	}
	
	# According to https://msdn.microsoft.com/en-us/library/azure/dn194086
	#
	# Set-MsolGroup [-Description <string>] [-DisplayName <string>] [-ManagedBy <string>] [-ObjectId <Guid>] [-TenantId <Guid>]
	# We're going to use PowerShell Splatting
	$param = @{"ObjectId" = $gid}
	$modification = $false
	
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
		$group = Set-MsolGroup @param 
	}
	# We return the original __UID__ since no change
	$Connector.Uid
}

function Update-User ($attributes)
{
	$accessor = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributesAccessor($attributes)
	$oid = $Connector.Uid.GetUidValue()

	# According to https://msdn.microsoft.com/en-us/library/azure/dn194136
	#
	# Set-MsolUser [-AlternateEmailAddresses <string[]>] [-BlockCredential <Boolean>] [-City <string>] 
	# [-Country <string>] [-Department <string>] [-DisplayName <string>] [-Fax <string>] [-FirstName <string>] 
	# [-ImmutableId <string>] [-LastName <string>] [-MobilePhone <string>] [-ObjectId <Guid>] [-Office <string>] 
	# [-PasswordNeverExpires <Boolean>] [-PhoneNumber <string>] [-PostalCode <string>] [-PreferredLanguage <string>] 
	# [-State <string>] [-StreetAddress <string>] [-StrongPasswordRequired <Boolean>] [-TenantId <Guid>] 
	# [-Title <string>] [-UsageLocation <string>] [-UserPrincipalName <string>] [<CommonParameters>]
	
	# We're going to use PowerShell Splatting
	$param = @{"ObjectId" = $oid}
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
		if ($null -ne $val)
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
	# We call it first before other changes
	# Password change is the most likely to fail.
	# According to: https://msdn.microsoft.com/en-us/library/azure/dn194140
	#
	# Set-MsolUserPassword -ObjectId <Guid> [-ForceChangePassword <Boolean>] 
	# [-NewPassword <string>] [-TenantId <Guid>] [<CommonParameters>]
	
	$password = $accessor.GetPassword()
	If ($password -ne $null)
	{
		$pparm = @{"ObjectId" = $oid}
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
		$user = Set-MsolUserPassword @pparm
		Write-Verbose "Password updated"
	}
	
	# UserPrincipalName change - has a dedicated cmdlet
	# According to: https://msdn.microsoft.com/en-us/library/azure/dn194135
	#
	# Set-MsolUserPrincipalName -NewUserPrincipalName <string> -ObjectId <Guid> 
	# [-ImmutableId <string>] [-NewPassword <string>] [-TenantId <Guid>] [<CommonParameters>]
	
	$upn = $accessor.GetName()
	if ( $upn -ne $null)
	{
		$uparm = @{"ObjectId" = $oid}
		$uparm.Add("NewUserPrincipalName", $upn.GetNameValue())
		$user = Set-MsolUserPrincipalName @uparm
		Write-Verbose "UserPrincipalName updated"
	}
	
	# What's left? 
    # LicenseOptions: License options for license assignment. Used to selectively disable individual service plans within a SKU.
	# TenantId
	
	if($modification)
	{
		$user = Set-MsolUser @param
		Write-Verbose "User updated"
	}
	# We return the original __UID__ since we did not change it
	$Connector.Uid
}

try
{
if ($Connector.Operation -eq "UPDATE")
{
	if (!$Env:OpenICF_AAD) {
		$msolcred = New-object System.Management.Automation.PSCredential $Connector.Configuration.Login, $Connector.Configuration.Password.ToSecureString()
		connect-msolservice -credential $msolcred
		$Env:OpenICF_AAD = $true
		Write-Verbose -verbose "New session created"
	}

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
		default
		{
			throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.ConnectorException("Unsupported type: $($Connector.ObjectClass.Type)")	
		}
	}
}
else
{
	throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.ConnectorException("UpdateScript can not handle operation: $($Connector.Operation)")
}
}
catch #Re-throw the original exception message within a connector exception
{
	throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.ConnectorException($_.Exception.Message)
}
