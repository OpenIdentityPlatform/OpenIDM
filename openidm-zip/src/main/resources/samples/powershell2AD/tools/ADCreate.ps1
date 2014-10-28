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
    This is a sample Create script        

.DESCRIPTION
	This script leverages Create-NewUser and Create-NewGroup cmdlets from Active Directory module.
	
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
    File Name      : ADCreate.ps1  
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

function Create-NewGroup ($attributes)
{
	$accessor = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributesAccessor($attributes)
	$basic = [Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeUtil]::GetBasicAttributes($attributes)
	$dic = [Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeUtil]::ToMap($basic)

	# New-ADGroup 
	# See http://technet.microsoft.com/en-us/library/ee617258.aspx
	
	#[-Name] <string> 
	# Name is cn, cn is required. Could be name as well
	$r = $dic.Remove("cn")
	$r = $dic.Remove("name")
	$r = $dic.Remove("sAMAccountName")

	#[-GroupScope] <System.Nullable[Microsoft.ActiveDirectory.Management.ADGroupScope]> 
	# group scope is either 'DomainLocal', 'Universal', 'Global'
	$val = $accessor.FindString("groupScope")
	$groupscope = "Global"
	if ($val -ne $null) 
	{ 
		$groupScope = $val
		$r = $dic.Remove("groupScope")	
	}
	
	#[-GroupCategory <System.Nullable[Microsoft.ActiveDirectory.Management.ADGroupCategory]>] 
	# group category is either 'security' or 'distribution'
	$val = $accessor.FindString("groupCategory")
	$groupCategory = "Security"
	if ($val -ne $null) 
	{ 
		$groupCategory = $val
		$r = $dic.Remove("groupCategory")	
	}
	# [-Path <string>] 
	($rdn,$path) = $accessor.GetName().GetNameValue().Split(',',2)
	
	# [-OtherAttributes <hashtable>]
	# we put the remaining attributes here
	$otherAttrs = @{}
	foreach($h in $dic.GetEnumerator())
	{
		if ($h.Value -ne $null)
		{
			$vals = @()
			foreach($val in $h.Value.Value)
			{
				$vals += $val
			}
			$otherAttrs.Add($h.Key,$vals)
		}
	}
	
	$adgroup = New-ADGroup  -SamAccountName $accessor.FindString("sAMAccountName") `
							-Name $accessor.FindString("cn") `
							-GroupScope $groupScope `
							-GroupCategory $groupCategory `
							-Path $path `
							-OtherAttributes $otherAttrs `
							-PassThru
	$adgroup.ObjectGUID.ToString()
}

function Create-NewUser ($attributes)
{
	$accessor = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributesAccessor($attributes)
	$basic = [Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeUtil]::GetBasicAttributes($attributes)
	$dic = [Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeUtil]::ToMap($basic)
	
	# New-ADUser
	# See http://technet.microsoft.com/en-us/library/ee617253.aspx
	$aduser = New-Object Microsoft.ActiveDirectory.Management.ADUser

	# [-Name] <string>
	# Name is cn, cn is required. Could be name as well
	
	$r = $dic.Remove("cn")
	$r = $dic.Remove("name")
	$r = $dic.Remove("sAMAccountName")

	# [-AccountExpirationDate <System.Nullable[System.DateTime]>] 
	# This corresponds to the AccountExpires attribute
	# 0 means never expire
	# other values must be in the format of System.DateTime
	# like: "05/06/2014 17:02:23"
	
	$val = $accessor.FindString("accountExpirationDate")
	if ($val -ne $null) 
	{ 
		$aduser.AccountExpirationDate = [System.DateTime]::Parse($val)
		$r = $dic.Remove("accountExpirationDate")	
	}

	# [-AccountPassword <SecureString>] 
	$password = $accessor.GetPassword()
	If ($password -ne $null)
	{
		$dec = $secutil::Decrypt($password)
		$password = ConvertTo-SecureString -String $dec -AsPlainText -Force
	}

	# [-AllowReversiblePasswordEncryption <System.Nullable[bool]>] 
	$bool = $accessor.FindBoolean("allowReversiblePasswordEncryption")
	if ($bool -ne $null) 
	{
		$aduser.AllowReversiblePasswordEncryption = $bool
		$r = $dic.Remove("allowReversiblePasswordEncryption")
	}

	# [-CannotChangePassword <System.Nullable[bool]>] 
	$bool = $accessor.FindBoolean("cannotChangePassword")
	if ($bool -ne $null) 
	{
		$aduser.CannotChangePassword = $bool
		$r = $dic.Remove("cannotChangePassword")
	}

	# [-Certificates <X509Certificate[]>] 
	# This is the "userCertificate" attribute

	# [-ChangePasswordAtLogon <System.Nullable[bool]>] 
	$bool = $accessor.FindBoolean("changePasswordAtLogon")
	if ($bool -ne $null) 
	{
		$aduser.ChangePasswordAtLogon = $bool
		$r = $dic.Remove("changePasswordAtLogon")
	}

	# [-Enabled <System.Nullable[bool]>] 
	$aduser.Enabled = $accessor.GetEnabled($false)

	# [-PasswordNeverExpires <System.Nullable[bool]>] 
	$bool = $accessor.FindBoolean("passwordNeverExpires")
	if ($bool -ne $null) 
	{
		$aduser.PasswordNeverExpires = $bool
		$r = $dic.Remove("passwordNeverExpires")
	}

	# [-PasswordNotRequired <System.Nullable[bool]>] 
	$bool = $accessor.FindBoolean("passwordNotRequired")
	if ($bool -ne $null) 
	{
		$aduser.PasswordNotRequired = $bool
		$r = $dic.Remove("passwordNotRequired")
	}

	# [-Path <string>] 
	($rdn,$path) = $accessor.GetName().GetNameValue().Split(',',2)

	# [-SmartcardLogonRequired <System.Nullable[bool]>] 
	$bool = $accessor.FindBoolean("smartcardLogonRequired")
	if ($bool -ne $null) 
	{
		$aduser.SmartcardLogonRequired = $bool
		$r = $dic.Remove("smartcardLogonRequired")
	}
	# [-TrustedForDelegation <System.Nullable[bool]>] 
	$bool = $accessor.FindBoolean("trustedForDelegation")
	if ($bool -ne $null) 
	{
		$aduser.TrustedForDelegation = $bool
		$r = $dic.Remove("trustedForDelegation")
	}
	
	# [-OtherAttributes <hashtable>]
	# we put the remaining attributes here
	$otherAttrs = @{}
	foreach($h in $dic.GetEnumerator())
	{
		if ($h.Value -ne $null)
		{
			$vals = @()
			foreach($val in $h.Value.Value)
			{
				$vals += $val
			}
			$otherAttrs.Add($h.Key,$vals)
		}
	}
	$aduser = New-ADUser -SamAccountName $accessor.FindString("sAMAccountName") `
							-AccountPassword $password `
							-Name $accessor.FindString("cn") `
							-Instance $aduser `
							-Path $path `
							-OtherAttributes $otherAttrs `
							-PassThru
	$aduser.ObjectGUID.ToString()
}

try
{
if ($Connector.Operation -eq "CREATE")
{
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
