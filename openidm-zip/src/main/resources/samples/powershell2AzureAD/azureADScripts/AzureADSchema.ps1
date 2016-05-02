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
    This is a sample Schema script for Azure AD
	
.DESCRIPTION
	The script builds the OpenICF schema for users and groups
	
.INPUT VARIABLES
	The connector sends us the following:
	- <prefix>.Configuration : handler to the connector's configuration object
	- <prefix>.Operation: String correponding to the action ("SCHEMA" here)
	- <prefix>.SchemaBuilder: an instance of org.identityconnectors.framework.common.objects.SchemaBuilder 
	that must be used to define the schema.
	
.RETURNS 
	Nothing. Connector will finalize the schema build.
	
.NOTES  
    File Name      : AzureADSchema.ps1  
    Author         : Gael Allioux (gael.allioux@forgerock.com)
    Prerequisite   : PowerShell V2 and later
    Copyright      : 2015-2016 - ForgeRock AS    

.LINK  
    Script posted over:  
    http://openicf.forgerock.org
		
	Azure Active Directory Module for Windows PowerShell
	https://msdn.microsoft.com/en-us/library/azure/jj151815.aspx

#>

# See https://technet.microsoft.com/en-us/library/hh847796.aspx
$ErrorActionPreference = "Stop"
$VerbosePreference = "Continue"

try
{
$AttributeInfoBuilder = [Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeInfoBuilder]

 if ($Connector.Operation -eq "SCHEMA")
 {

 	###########################
 	# __ACCOUNT__ object class
	###########################

	$ocib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ObjectClassInfoBuilder
	$ocib.ObjectType = "__ACCOUNT__"
	
	# Required Attributes
	$Required = @("DisplayName","UserPrincipalName")
	
	foreach ($attr in $Required)
	{
		$caib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeInfoBuilder($attr);
		$caib.Required = $TRUE
		$caib.ValueType = [string];
		$ocib.AddAttributeInfo($caib.Build())
	}
	
	# Standard attributes - single valued
	$StandardSingle = @("City","Country ","Department","Fax","FirstName","LastName","MobilePhone","TenantId"
	"Office","PhoneNumber","PostalCode","PreferredLanguage","State","StreetAddress","Title","UsageLocation","LastPasswordChangeTimestamp","LiveId")
	
	foreach ($attr in $StandardSingle)
	{
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build($attr,[string]))
	}
	
	# Standard attributes - multi valued
	$StandardMulti = @("AlternateEmailAddresses", "AlternateMobilePhones", "Licenses")
	
	foreach ($attr in $StandardMulti)
	{
		$caib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeInfoBuilder($attr);
		$caib.MultiValued = $TRUE
		$caib.ValueType = [string];
		$ocib.AddAttributeInfo($caib.Build())
	}
		
	# Technical attributes
	$Technical = @("EnabledFilter")
	
	foreach ($attr in $Technical)
	{
		$caib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeInfoBuilder($attr);
		$caib.Creatable = $FALSE
		$caib.Updateable = $FALSE
		$caib.ValueType = [string];
		$ocib.AddAttributeInfo($caib.Build())
	}
	
	# A few custom attributes we want to add here
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("PasswordNeverExpires",[bool]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("ForceChangePassword",[bool]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("BlockCredential",[bool]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("StrongPasswordRequired",[bool]))
	
 	# A few operational attributes as well
	$opAttrs = [Org.IdentityConnectors.Framework.Common.Objects.OperationalAttributeInfos]
	#$ocib.AddAttributeInfo($opAttrs::ENABLE)
	$ocib.AddAttributeInfo($opAttrs::PASSWORD)
	#$ocib.AddAttributeInfo($opAttrs::LOCK_OUT)
	#$ocib.AddAttributeInfo($opAttrs::PASSWORD_EXPIRED)
	#$ocib.AddAttributeInfo($opAttrs::DISABLE_DATE)
	#$ocib.AddAttributeInfo($opAttrs::ENABLE_DATE)
	
	$Connector.SchemaBuilder.DefineObjectClass($ocib.Build())
	
	
	###########################
 	# __GROUP__ object class
	###########################
	$ocib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ObjectClassInfoBuilder
	$ocib.ObjectType = "__GROUP__"
	
	# Required Attributes
	$Required = @("DisplayName")
	
	foreach ($attr in $Required)
	{
		$caib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeInfoBuilder($attr);
		$caib.Required = $TRUE
		$caib.ValueType = [string];
		$ocib.AddAttributeInfo($caib.Build())
	}
	
	# Standard attributes - single valued
	$StandardSingle = @("CommonName","Description","EmailAddress","GroupType","ManagedBy")
	
	foreach ($attr in $StandardSingle)
	{
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build($attr,[string]))
	}
	
	# Standard attributes - multi valued
	$StandardMulti = @("Licenses")
	
	foreach ($attr in $StandardMulti)
	{
		$caib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeInfoBuilder($attr);
		$caib.MultiValued = $TRUE
		$caib.ValueType = [string];
		$ocib.AddAttributeInfo($caib.Build())
	}

	# Technical attributes
	$Technical = @("objectId","LastDirSyncTime")
	
	foreach ($attr in $Technical)
	{
		$caib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeInfoBuilder($attr);
		$caib.Creatable = $FALSE
		$caib.Updateable = $FALSE
		$caib.ValueType = [string];
		$ocib.AddAttributeInfo($caib.Build())
	}
	
	# Helper attribute to show the members
	$TechnicalMultiRO = @("__MEMBERS__")
	foreach ($attr in $TechnicalMultiRO)
	{
		$caib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeInfoBuilder($attr);
		$caib.Creatable = $FALSE
		$caib.MultiValued = $TRUE
		$caib.ValueType = [object];
		$ocib.AddAttributeInfo($caib.Build())
	}
	
	#$TechnicalMulti = @("__ADD_MEMBERS__", "__REMOVE_MEMBERS__")
	#foreach ($attr in $TechnicalMulti)
	#{
	#	$caib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeInfoBuilder($attr);
	#	$caib.Creatable = $FALSE
	#	$caib.Updateable = $TRUE
	#	$caib.MultiValued = $TRUE
	#	$caib.ReturnedByDefault = $FALSE
	#	$caib.Readable = $FALSE
	#	$caib.ValueType = [string];
	#	$ocib.AddAttributeInfo($caib.Build())
	#}
	
	$Connector.SchemaBuilder.DefineObjectClass($ocib.Build())
 }
 }
catch #Re-throw the original exception message within a connector exception
{
	throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.ConnectorException($_.Exception.Message)
}