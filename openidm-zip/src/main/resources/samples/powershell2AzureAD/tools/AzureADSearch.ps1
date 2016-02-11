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
	This script leverages both Get-MsolUser and Get-MsolGroup to search for users and groups.
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
    Prerequisite   : PowerShell V2 and later
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


# We define a filter to process results through a pipe and feed the result handler
filter Process-Results {
	$result = @{"__UID__" = $_.ObjectId.ToString(); "__NAME__"= $_.UserPrincipalName}
	
	if ($Connector.ObjectClass.Type -eq "__GROUP__")
	{
			$result.Remove("__NAME__")
			$result.Add("__NAME__", $_.DisplayName)
	}
	
	foreach($attrName in $Connector.Options.AttributesToGet)
	{
		if ($Connector.ObjectClass.Type -eq "__GROUP__" -and $attrName -eq "__MEMBERS__")
		{
			 $members = Get-MsolGroupMember -All -GroupObjectId $result["__UID__"]
			 $list = @()
			 foreach($member in $members)
			 {
				$list += $member.EmailAddress
			 }
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

# Always put code in try/catch statement and make sure exceptions are re-thrown to connector
try
{
    $scriptPath = split-path -parent $Connector.Configuration.SearchScriptFileName
    . $scriptPath\AzureADCommon.ps1

	authenticateSession($Connector)

	switch ($Connector.ObjectClass.Type)
	{
		"__ACCOUNT__"
		{
			if ($Connector.Query -eq $null) {
				Get-MsolUser -All | Process-Results
			}
			elseif ($Connector.Query.Operation -eq "EQUALS")
			{
				switch ($Connector.Query.Left)
				{
					"__UID__"
					{
						Get-MsolUser -ObjectId $Connector.Query.Right | Process-Results
					}
					"__NAME__"
					{
						Get-MsolUser -UserPrincipalName $Connector.Query.Right | Process-Results
					}
					"UserPrincipalName"
					{
						Get-MsolUser -UserPrincipalName $Connector.Query.Right | Process-Results
					}
					"ObjectId"
					{
						Get-MsolUser -ObjectId $Connector.Query.Right | Process-Results
					}
					"LiveId"
					{
						Get-MsolUser -LiveId $Connector.Query.Right | Process-Results
					}
					"City"
					{
						Get-MsolUser -All -City $Connector.Query.Right | Process-Results
					}
					"Country"
					{
						Get-MsolUser -All -Country $Connector.Query.Right | Process-Results
					}
					"Department"
					{
						Get-MsolUser -All -Department $Connector.Query.Right | Process-Results
					}
					"DomainName"
					{
						Get-MsolUser -All -DomainName $Connector.Query.Right | Process-Results
					}
					"EnabledFilter"
					{
						Get-MsolUser -All -EnabledFilter $Connector.Query.Right | Process-Results
					}
					
					"State"
					{
						Get-MsolUser -All -State $Connector.Query.Right | Process-Results
					}
					"TenantId"
					{
						Get-MsolUser -All -TenantId $Connector.Query.Right | Process-Results
					}
					"UsageLocation"
					{
						Get-MsolUser -All -UsageLocation $Connector.Query.Right | Process-Results
					}
				}
			}
			elseif ($Connector.Query.Operation -eq "STARTSWITH")
			{
				Get-MsolUser -All -SearchString $Connector.Query.Right | Process-Results
			}
		}
		"__GROUP__"
		{
			if ($Connector.Query -eq $null) {
				Get-MsolGroup -All | Process-Results
			}
			elseif ($Connector.Query.Operation -eq "EQUALS")
			{
				switch ($Connector.Query.Left)
				{
					"__UID__"
					{
						Get-MsolGroup -ObjectId $Connector.Query.Right | Process-Results
					}
					"ObjectId"
					{
						Get-MsolGroup -ObjectId $Connector.Query.Right | Process-Results
					}
					"__NAME__"
					{
						Get-MsolGroup -DisplayName $Connector.Query.Right | Process-Results
					}
					"DisplayName"
					{
						Get-MsolGroup -DisplayName $Connector.Query.Right | Process-Results
					}
					"GroupType"
					{
						Get-MsolGroup -All -GroupType $Connector.Query.Right | Process-Results
					}
					"TenantId"
					{
						Get-MsolGroup -All -TenantId $Connector.Query.Right | Process-Results
					}
				}
			}
			elseif ($Connector.Query.Operation -eq "STARTSWITH")
			{
				Get-MsolGroup -All -SearchString $Connector.Query.Right | Process-Results
			}
		}
		default
		{
			throw "Unsupported type: $($Connector.ObjectClass.Type)"	
		}
	}	
}
catch #Re-throw the original exception
{
	throw
}