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
    This is a sample Sync script for Active Directory users and groups
	
.DESCRIPTION
	This script leverages the uSNChanged attribute to find entries that have been
	modified since last invoke
	
.INPUT VARIABLES
	The connector injects the following variables to the script:
	- <prefix>.Configuration : handler to the connector's configuration object
	- <prefix>.Options: a handler to the Operation Options
	- <prefix>.Operation: String correponding to the operation ("SYNC" or "GET_LATEST_SYNC_TOKEN" here)
	- <prefix>.ObjectClass: the Object class object (__ACCOUNT__ / __GROUP__ / other)
	- <prefix>.Token: The sync token value

.RETURNS
	if Operation is "GET_LATEST_SYNC_TOKEN":
	Must return an object representing the last known sync token for the corresponding ObjectClass
	
	if Operation is "SYNC":
    Call Connector.Results.Process(Hashtable) describing one update:
    Map should look like the following:
   [
   "SyncToken": <Object> token of the object that changed(could be Integer, Date, String) , [!! could be null]
   "DeltaType": <String> ("CREATE|UPDATE|CREATE_OR_UPDATE"|"DELETE"), the type of change that occurred
   "Uid": <String>; the Uid (OpenICF __UID__) of the entry
   "PreviousUid": <String>, the Uid of the object before the change (This is for rename ops)
   "Object": <Hashtable> The Connector object that has changed
   "ObjectClass": <String|ObjectClass> The ObjectClass of the entry. Needs to be set if op=DELETE and Object=null
   ]
  
.NOTES  
    File Name      : ADSync.ps1  
    Author         : Gael Allioux (gael.allioux@forgerock.com)
    Prerequisite   : PowerShell V2 - AD module loaded by the connector
    Copyright 2014 - ForgeRock AS    

.LINK  
    Script posted over:  
    http://openicf.forgerock.org
		
	Active Directory Administration with Windows PowerShell
	http://technet.microsoft.com/en-us/library/dd378937(v=ws.10).aspx
#>

# We need this global Boolean to handle the case where the sync handler returns false 
# and we don't want to break the pipe because of https://bugster.forgerock.org/jira/browse/OPENIDM-2650
$proceed = $TRUE

# We define a filter to process results through a pipe and feed the sync result handler
filter Process-Sync {
	if ($proceed)
	{
		$object = @{"__NAME__" = $_.DistinguishedName; "__UID__" = $_.ObjectGUID.ToString();}
		foreach($attr in $_.GetEnumerator())
		{
			if ($attr.Value.GetType().Name -eq "ADPropertyValueCollection")
			{
				$values = @();
				foreach($val in $attr.Value) 
				{
					$values += $val
				}
				$object.Add($attr.Key, $values)
			}
			else
			{
				$object.Add($attr.Key, $attr.Value)
			}
		}
		
		$result = @{"SyncToken" = $_.uSNChanged; "DeltaType" = "CREATE_OR_UPDATE"; "Uid" = $_.ObjectGUID.ToString(); "Object" = $object}
		
		$proceed = $Connector.Result.Process($result)
	}
}

try
{
if ($Connector.Operation -eq "GET_LATEST_SYNC_TOKEN")
{
	# we should specify a server since USN is specific a DC instance
	# For that, Get-ADRootDSE has a -Server option
	$dse = Get-ADRootDSE
	$token = New-Object Org.IdentityConnectors.Framework.Common.Objects.SyncToken($dse.highestCommittedUSN);
	$Connector.Result.SyncToken = $token;
}
elseif ($Connector.Operation -eq "SYNC")
{
	$searchBase = 'CN=Users,DC=example,DC=com'
	$attrsToGet = "*"
	$filter = "uSNChanged -gt {0}" -f $Connector.Token
	
	switch ($Connector.ObjectClass.Type)
	{
		"__ACCOUNT__" 	
		{
			Get-ADUser -Filter $filter -SearchBase $searchBase -Properties $attrsToGet | Process-Sync
		}
		"__GROUP__"
		{
			Get-ADGroup -Filter $filter -SearchBase $searchBase -Properties $attrsToGet | Process-Sync
		}
		default {throw "Unsupported type: $($Connector.ObjectClass.Type)"}
	}
}
else
{
	throw new Org.IdentityConnectors.Framework.Common.Exceptions.ConnectorException("SyncScript can not handle operation: $($Connector.Operation)")
}
}
catch #Rethrow the original exception
{
	throw
}
