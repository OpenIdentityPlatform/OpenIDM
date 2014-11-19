# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2014 ForgeRock AS. All Rights Reserved
#
# The contents of this file are subject to the terms
# of the Common Development and Distribution License
# (the License). You may not use this file except in
# compliance with the License.
#
# You can obtain a copy of the License at
# http://forgerock.org/license/CDDLv1.0.html
# See the License for the specific language governing
# permission and limitations under the License.
#
# When distributing Covered Code, include this CDDL
# Header Notice in each file and include the License file
# at http://forgerock.org/license/CDDLv1.0.html
# If applicable, add the following below the CDDL Header,
# with the fields enclosed by brackets [] replaced by
# your own identifying information:
# " Portions Copyrighted [year] [name of copyright owner]"
#
# @author Gael Allioux <gael.allioux@forgerock.com>
#
#REQUIRES -Version 2.0

<#  
.SYNOPSIS  
    This is a sample Update script        

.DESCRIPTION
	This script leverages Set-ADUser and Set-ADGroup cmdlets from Active Directory module.
	
.INPUT VARIABLES
	The connector sends us the following:
	- <prefix>.Configuration : handler to the connector's configuration object
	- <prefix>.Options: a handler to the Operation Options
	- <prefix>.Operation: an OperationType correponding to the action ("UPDATE" here)
	- <prefix>.ObjectClass: an ObjectClass describing the Object class (__ACCOUNT__ / __GROUP__ / other)
	- <prefix>.Attributes: A collection of ConnectorAttributes to update
	- <prefix>.Uid: Corresponds to the OpenICF __UID__ attribute 
	
.RETURNS
	Must return the user unique ID (__UID__) if either UID or Revision has been modified 
	To do so, set the <prefix>.Result.Uid property with the modified Uid object

.NOTES  
    File Name      : ADUpdate.ps1  
    Author         : Gael Allioux (gael.allioux@forgerock.com)
    Prerequisite   : PowerShell V2 - AD module loaded by the connector
    Copyright 2014 - ForgeRock AS    

	.LINK  
    Script posted over:  
    http://openicf.forgerock.org
		
	Active Directory Administration with Windows PowerShell
	http://technet.microsoft.com/en-us/library/dd378937(v=ws.10).aspx
#>

$attrutil = [Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeUtil]
$secutil = [Org.IdentityConnectors.Common.Security.SecurityUtil]

function Update-Group ($attributes)
{
	$accessor = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributesAccessor($attributes)
	$basic = [Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeUtil]::GetBasicAttributes($attributes)
	$dic = [Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeUtil]::ToMap($basic)

	# Set-ADGroup 
	# See http://technet.microsoft.com/en-us/library/ee617199.aspx
	
	# First get the current instance
	$groupInstance = Get-ADGroup -Identity $Connector.Uid.GetUidValue() -Properties "*"
	$changes = $FALSE
	
	# Standard attributes - single valued
	$StandardSingle = @("wWWHomePage","name","telephoneNumber","mail","displayNamePrintable","displayName","managedBy","info","description")
	foreach ($attr in $StandardSingle)
	{
		$str = $accessor.FindString($attr)
		if ($str -ne $null) 
		{
			$groupInstance.$attr = $str
			$changes = $TRUE
		}
	}
	
	#[-SamAccountName] <string> 
	$val = $accessor.FindString("sAMAccountName")
	if ($val -ne $null) 
	{ 
		$groupInstance.SamAccountName = $val
		$changes = $TRUE
	}

	#[-GroupScope] <System.Nullable[Microsoft.ActiveDirectory.Management.ADGroupScope]> 
	# group scope is either 'DomainLocal', 'Universal', 'Global'
	$val = $accessor.FindString("groupScope")
	if ($val -ne $null) 
	{ 
		$groupInstance.GroupScope = $val
		$changes = $TRUE
	}
	
	#[-GroupCategory <System.Nullable[Microsoft.ActiveDirectory.Management.ADGroupCategory]>] 
	# group category is either 'security' or 'distribution'
	$val = $accessor.FindString("groupCategory")
	if ($val -ne $null) 
	{ 
		$groupInstance.GroupCategory = $val
		$changes = $TRUE
	}
	
	# DistinguishedName
	$val = $accessor.GetName()
	if ($val -ne $null) 
	{ 
		$groupInstance.DistinguishedName = $val
		$changes = $TRUE
	}

	if ($changes)
	{
		Set-ADGroup -Instance $groupInstance
	}
	
	# Members changes
	$val = $accessor.FindString("member")
	if ($val -ne $null) 
	{
		# Put the members in a hashtable
		$toAdd = @{};
		foreach ($h in $val)
		{
			$toAdd[$h] = 1
		}
		
		# let's get current members.
		$current = @()
		Get-ADGroupMember  $Connector.Uid.GetUidValue() | ForEach-Object { $current += $_.distinguishedName.ToLower()}
		
		# calculate the diff
		$toRemove = @()
		foreach($h in $current)
		{
			if ( $toAdd.Contains($h) )
			{
				$toAdd.Remove($h)
			} 
			else
			{
				$toRemove += $h
			}
		}
		$newMember = @()
		foreach($h in $toAdd.GetEnumerator())
		{
			$newMember += $h.Key
		}
		if ( $newMember.Count -ne 0) 
		{
			Add-ADGroupMember $Connector.Uid.GetUidValue() -Members $newMember -Confirm:$false
		}

		if ( $toRemove.Count -ne 0)
		{
			Remove-ADGroupMember $Connector.Uid.GetUidValue() -Members $toRemove -Confirm:$false
		}
	}
	
	$Connector.Uid
}

function Update-User ($attributes)
{
	$accessor = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributesAccessor($attributes)
	
	# Get the current instance of the user
	$aduser = Get-ADUser -Identity $Connector.Uid.GetUidValue() -Properties "*"

	# [-AccountExpirationDate <System.Nullable[System.DateTime]>] 
	# This corresponds to the AccountExpires attribute
	# 0 means never expire
	# other values must be in the format of System.DateTime
	# like: "05/06/2014 17:02:23"
	
	$val = $accessor.FindString("accountExpirationDate")
	if ($val -ne $null) 
	{ 
		$aduser.AccountExpirationDate = [System.DateTime]::Parse($val)
	}

	# [-Enabled <System.Nullable[bool]>] 
	$aduser.Enabled = $accessor.GetEnabled($false)
	
	# [-AccountPassword <SecureString>] 
	$password = $accessor.GetPassword()
	If ($password -ne $null)
	{
		$dec = $secutil::Decrypt($password)
		$password = ConvertTo-SecureString -String $dec -AsPlainText -Force
		Write-Verbose -verbose "Updating password"
	}

	# handle UAC boolean attributes
	$uac = ("allowReversiblePasswordEncryption", "cannotChangePassword", "changePasswordAtLogon",
	"passwordNeverExpires","passwordNotRequired","smartcardLogonRequired","trustedForDelegation")
	foreach ($control in $uac)
	{
		$bool = $accessor.FindBoolean($control)
		if ($bool -ne $null) 
		{
			$aduser.$control = $bool
		}
	}
	
	# Standard attributes - single valued
	$StandardSingle = @("division","primaryInternationalISDNNumber","c","l","department","givenName","telephoneNumber","employeeNumber","displayName",
	"personalTitle","homeDirectory","postalCode","manager","st","initials","employeeType","streetAddress","co","title","middleName","wWWHomePage","company",
	"name","comment","scriptPath","mail","displayNamePrintable","ipPhone","homePostalAddress","facsimileTelephoneNumber","homePhone","street","homeDrive",
	"info","assistant","mobile","employeeID","logonWorkstation","logonHours","userWorkstations","userSharedFolder","description")
	foreach ($attr in $StandardSingle)
	{
		$str = $accessor.FindString($attr)
		if ($str -ne $null) 
		{
			$aduser.$attr = $str
		}
	}
	
	# Standard attributes - multi valued
	$StandardMulti = @("otherMailbox","otherLoginWorkstations","o","postOfficeBox","otherTelephone",
	"otherMobile","seeAlso","url","ou","postalAddress","otherHomePhone","internationalISDNNumber")
	foreach ($attr in $StandardMulti)
	{
		$strs = $accessor.FindStringList($attr)
		if ($strs -ne $null)
		{
			$aduser.$attr = $strs
		}
	}
	
	$sam = $accessor.FindString("sAMAccountName") 
	if ($sam -ne $null)
	{
		Set-ADUser -Instance $aduser -SamAccountName $sam
	}
	else
	{
		Set-ADUser -Instance $aduser
	}
	
	If ($password -ne $null)
	{
		Set-ADAccountPassword $Connector.Uid.GetUidValue() -Reset -NewPassword $password
		Write-Verbose -verbose "Password updated"
	}

	$Connector.Uid
}

try
{
if ($Connector.Operation -eq "UPDATE")
{
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
	throw new Org.IdentityConnectors.Framework.Common.Exceptions.ConnectorException("CreateScript can not handle operation: $($Connector.Operation)")
}
}
catch #Re-throw the original exception
{
	throw
}
