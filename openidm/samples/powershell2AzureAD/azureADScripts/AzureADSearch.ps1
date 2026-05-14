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
    This is a sample Search script with Azure AD as a target
	
.DESCRIPTION
	This script leverages both Get-MsolUser and Get-MsolGroup to search for users and groups,
	as well as Get-MsolAccountSku to retrieve the list of available licenses.
	It also demo how to use PowerShell pipes to handles the results back to the connector framework.
	
.INPUT VARIABLES
	The connector injects the following variables to the script:
	- <prefix>.Configuration : handler to the connector's configuration object
	- <prefix>.Options: a handler to the Operation Options
	- <prefix>.Operation: String correponding to the operation ("AUTHENTICATE" here)
	- <prefix>.ObjectClass: the Object class object (__ACCOUNT__ / __GROUP__ / other)	
	- <prefix>.Query: a handler to the Query.
	
.RETURNS
	The entries found
	
.NOTES  
    File Name      : AzureADSearch.ps1  
    Author         : Gael Allioux (gael.allioux@forgerock.com)
    Prerequisite   : PowerShell V2 and later
    Copyright      : 2015-2016 - ForgeRock AS    
.LINK  
    Script posted over:  
    http://openicf.forgerock.org
		
	Azure Active Directory Module for Windows PowerShell
	https://msdn.microsoft.com/en-us/library/azure/jj151815.aspx
#>

# The "Map" filter visitor put the query in a HashMap with the following keys:
# 'Not': boolean to tell if the query uses the NOT (!)
# 'Left': the left side of the query
# 'Right: the right side of the query
# 'Operation': the query operation. Possible values are:
#  CONTAINS, EQUALS, GREATERTHAN, GREATERTHANOREQUAL, LESSTHAN, LESSTHANOREQUAL, STARTSWITH, ENDSWITH
#
# Example: 
# the query filter "UserPrincipalName eq JSmith" will come as the following map:
# @{'Not' = false; 'Left' = 'UserPrincipalName'; 'Operation' = 'EQUALS'; 'Right' = 'JSmith'}

# See https://technet.microsoft.com/en-us/library/hh847796.aspx
$ErrorActionPreference = "Stop"
$VerbosePreference = "Continue"

# We define a filter to process results through a pipe and feed the result handler
filter Process-Groups {
	$result = @{"__UID__" = $_.ObjectId.ToString(); "__NAME__"= $_.DisplayName}

	foreach($attrName in $Connector.Options.AttributesToGet)
	{
		if ($attrName -eq "__MEMBERS__")
		{
			$list = @()
			Get-MsolGroupMember -All -GroupObjectId $result["__UID__"] | foreach {
				$hash = @{}
				$hash.Add("ObjectId", $_.ObjectId)
				$hash.Add("GroupMemberType", $_.GroupMemberType)
				$hash.Add("DisplayName", $_.DisplayName)
				$hash.Add("EmailAddress", $_.EmailAddress)
				$list += $hash
			}
			Write-verbose "Group contains $($list.Count) members"
			$result.Add("__MEMBERS__",$list)
		}
		else
		{
			if ($_.$attrName -ne $null)
			{
				$value = $_.$attrName
				if ($value.GetType().Name.Contains("List"))
				{
					$multi = @()
					foreach($e in $value)
					{
						$multi += $e
					}
					$result.Add($attrName, $multi)
				}
				else
				{
					$result.Add($attrName, $_.$attrName.ToString())
				}
			}
		}
	}
	$Connector.Result.Process($result)	
}

filter Process-Users {
	$result = @{"__UID__" = $_.ObjectId.ToString(); "__NAME__"= $_.UserPrincipalName}
	
	foreach($attrName in $Connector.Options.AttributesToGet)
	{
			if ($_.$attrName -ne $null)
			{
				$value = $_.$attrName
				if ($value.GetType().Name.Contains("List"))
				{
					$multi = @()
					foreach($e in $value)
					{
						$multi += $e
					}
					$result.Add($attrName, $multi)
				}
				else
				{
					$result.Add($attrName, $_.$attrName.ToString())
				}
			}
	}
	# Get the licenses
	if ($_.IsLicensed)
	{
		$licenses = @()
		$licenseOptions = @{}
		foreach ($license in $_.Licenses)
		{
			$options = New-Object 'System.Collections.Generic.Dictionary[String,String]'
			foreach ($serviceStatus in $license.ServiceStatus)
			{
				$options.Add($serviceStatus.ServicePlan.ServiceName, $serviceStatus.ProvisioningStatus)	
			}
			$licenseOptions.Add($license.AccountSkuId, $options)
			$licenses += $license.AccountSkuId
		}
		$result.Set_Item("Licenses", $licenses)
		$result.Add("LicenseOptions", $licenseOptions)
	}
	$Connector.Result.Process($result)	
}

filter Process-Licenses {
	$result = @{"__UID__" = $_.SkuId.ToString(); "__NAME__"= $_.AccountSkuId}
	$result.Add("ActiveUnits", $_.ActiveUnits)
	$result.Add("ConsumedUnits", $_.ConsumedUnits)
	$result.Add("SuspendedUnits", $_.SuspendedUnits)
	$result.Add("WarningUnits", $_.WarningUnits)
	$result.Add("TargetClass", $_.TargetClass)
	$result.Add("SkuPartNumber", $_.SkuPartNumber)
	$result.Add("AccountName", $_.AccountName)

	$services = @{}
	foreach ($service in $_.ServiceStatus)
	{
		$services.Add($service.ServicePlan.ServiceName, $service.ProvisioningStatus.ToString())
	}
	$result.Add("ServiceStatus", $services)

	$Connector.Result.Process($result)	
}

filter Process-Subscriptions {
	$result = @{"__UID__" = $_.ObjectId.ToString(); "__NAME__" = $_.SkuPartNumber}
	$result.Add("DateCreated", $_.DateCreated.ToString())
	$result.Add("NextLifecycleDate", $_.NextLifecycleDate.ToString())
	$result.Add("OcpSubscriptionId", $_.OcpSubscriptionId.ToString())
	$result.Add("SkuId", $_.SkuId.ToString())
	$result.Add("TotalLicenses", $_.TotalLicenses)
	$result.Add("Status", $_.Status)

	$services = @{}
	foreach ($service in $_.ServiceStatus)
	{
		$services.Add($service.ServicePlan.ServiceName, $service.ProvisioningStatus.ToString())
	}
	$result.Add("ServiceStatus", $services)

	$Connector.Result.Process($result)	
}


# Always put code in try/catch statement and make sure exceptions are re-thrown to connector
try
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
			if ($Connector.Query -eq $null) {
				Get-MsolUser -All | Process-Users
			}
			elseif ($Connector.Query.Operation -eq "EQUALS")
			{
				switch ($Connector.Query.Left)
				{
					"__UID__"
					{
						Get-MsolUser -ObjectId $Connector.Query.Right | Process-Users
					}
					"__NAME__"
					{
						Get-MsolUser -UserPrincipalName $Connector.Query.Right | Process-Users
					}
					"UserPrincipalName"
					{
						Get-MsolUser -UserPrincipalName $Connector.Query.Right | Process-Users
					}
					"ObjectId"
					{
						Get-MsolUser -ObjectId $Connector.Query.Right | Process-Users
					}
					"LiveId"
					{
						Get-MsolUser -LiveId $Connector.Query.Right | Process-Users
					}
					"City"
					{
						Get-MsolUser -All -City $Connector.Query.Right | Process-Users
					}
					"Country"
					{
						Get-MsolUser -All -Country $Connector.Query.Right | Process-Users
					}
					"Department"
					{
						Get-MsolUser -All -Department $Connector.Query.Right | Process-Users
					}
					"DomainName"
					{
						Get-MsolUser -All -DomainName $Connector.Query.Right | Process-Users
					}
					"EnabledFilter"
					{
						Get-MsolUser -All -EnabledFilter $Connector.Query.Right | Process-Users
					}
					
					"State"
					{
						Get-MsolUser -All -State $Connector.Query.Right | Process-Users
					}
					"TenantId"
					{
						Get-MsolUser -All -TenantId $Connector.Query.Right | Process-Users
					}
					"UsageLocation"
					{
						Get-MsolUser -All -UsageLocation $Connector.Query.Right | Process-Users
					}
					"IsLicensed"
					{
						if ($Connector.Query.Right)
						{
							Get-MsolUser -All | Where-Object {$_.IsLicensed -eq $true} | Process-Users
						}
						else
						{
							Get-MsolUser -All -UnlicensedUsersOnly | Process-Users
						}
						
					}
				}
			}
			elseif ($Connector.Query.Operation -eq "STARTSWITH")
			{
				Get-MsolUser -All -SearchString $Connector.Query.Right | Process-Users
			}
		}
		"__GROUP__"
		{
			if ($Connector.Query -eq $null) {
				Get-MsolGroup -All | Process-Groups
			}
			elseif ($Connector.Query.Operation -eq "EQUALS")
			{
				switch ($Connector.Query.Left)
				{
					"__UID__"
					{
						Get-MsolGroup -ObjectId $Connector.Query.Right | Process-Groups
					}
					"ObjectId"
					{
						Get-MsolGroup -ObjectId $Connector.Query.Right | Process-Groups
					}
					"__NAME__"
					{
						Get-MsolGroup -DisplayName $Connector.Query.Right | Process-Groups
					}
					"DisplayName"
					{
						Get-MsolGroup -DisplayName $Connector.Query.Right | Process-Groups
					}
					"GroupType"
					{
						Get-MsolGroup -All -GroupType $Connector.Query.Right | Process-Groups
					}
					"TenantId"
					{
						Get-MsolGroup -All -TenantId $Connector.Query.Right | Process-Groups
					}
				}
			}
			elseif ($Connector.Query.Operation -eq "STARTSWITH")
			{
				Get-MsolGroup -All -SearchString $Connector.Query.Right | Process-Groups
			}
		}
		# https://msdn.microsoft.com/en-us/library/azure/dn194118
		"License"
		{
			if ($Connector.Query -eq $null) {
				Get-MsolAccountSku | Process-Licenses
			}
			elseif ($Connector.Query.Operation -eq "EQUALS")
			{
				switch ($Connector.Query.Left)
				{
					"__UID__"
					{
						Get-MsolAccountSku | Where-Object {$_.SkuId -eq $Connector.Query.Right} | Process-Licenses
					}
					"__NAME__"
					{
						Get-MsolAccountSku | Where-Object {$_.AccountSkuId -eq $Connector.Query.Right} | Process-Licenses
					}
				}
			}
		}
		# https://msdn.microsoft.com/en-us/library/azure/dn194084
		"Subscription"
		{
			if ($Connector.Query -eq $null) {
				Get-MsolSubscription | Process-Subscriptions
			}
			elseif ($Connector.Query.Operation -eq "EQUALS")
			{
				switch ($Connector.Query.Left)
				{
					"__UID__"
					{
						Get-MsolSubscription -SubscriptionId $Connector.Query.Right | Process-Subscriptions
					}
					"__NAME__"
					{
						Get-MsolSubscription | Where-Object {$_.SkuPartNumber -eq $Connector.Query.Right} | Process-Subscriptions
					}
				}
			}
		}
		default
		{
			throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.ConnectorException("Unsupported type: $($Connector.ObjectClass.Type)")	
		}
	}	
}
catch #Re-throw the original exception message within a connector exception
{
	($cause,$op) = $_.FullyQualifiedErrorId -split ","
	if ($cause.EndsWith("NotFoundException"))
	{
		throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.UnknownUidException($_.Exception.Message)
	}

	# It is safe to remove the session flag
	if ($Env:OpenICF_AAD) 
	{
		Remove-Item Env:\OpenICF_AAD
		Write-Verbose "Removed session flag"
	}

	throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.ConnectorException($_.Exception.Message)
}
