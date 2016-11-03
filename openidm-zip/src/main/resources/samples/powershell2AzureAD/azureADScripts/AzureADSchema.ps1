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
		@("DisplayName","UserPrincipalName") | foreach {
			$caib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeInfoBuilder($_)
			$caib.Required = $TRUE
			$caib.ValueType = [string]
			$ocib.AddAttributeInfo($caib.Build())
		}
	
		# Standard attributes - single valued
		#@("City","Country ","Department","Fax","FirstName","LastName","MobilePhone","TenantId",
		#"Office","PhoneNumber","PostalCode","PreferredLanguage","State","StreetAddress","Title",
		#"UsageLocation","LastPasswordChangeTimestamp","LiveId") | foreach
		#{
		#	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build($_,[string]))
		#}
	
		# Standard attributes - multi valued
		@("AlternateEmailAddresses", "AlternateMobilePhones", "Licenses") | foreach {
			$caib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeInfoBuilder($_)
			$caib.MultiValued = $TRUE
			$caib.ValueType = [string]
			$ocib.AddAttributeInfo($caib.Build())
		}
		
		# Technical attributes
		@("EnabledFilter") | foreach {
			$caib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeInfoBuilder($_)
			$caib.Creatable = $FALSE
			$caib.Updateable = $FALSE
			$caib.ValueType = [string]
			$ocib.AddAttributeInfo($caib.Build())
		}
	
		# A few custom attributes we want to add here
		$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("PasswordNeverExpires",[bool]))
		$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("ForceChangePassword",[bool]))
		$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("BlockCredential",[bool]))
		$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("StrongPasswordRequired",[bool]))

		# LicenseOptions attribute - single map
		$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("LicenseOptions",[System.Collections.Generic.Dictionary[String,Object]]))
	
 		# A few operational attributes as well
		$opAttrs = [Org.IdentityConnectors.Framework.Common.Objects.OperationalAttributeInfos]
		$ocib.AddAttributeInfo($opAttrs::PASSWORD)
	
		# Build the object class info
		$Connector.SchemaBuilder.DefineObjectClass($ocib.Build())
	
		###########################
 		# __GROUP__ object class
		###########################
		$ocib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ObjectClassInfoBuilder
		$ocib.ObjectType = "__GROUP__"
	
		# Required Attributes
		@("DisplayName") | foreach {
			$caib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeInfoBuilder($_)
			$caib.Required = $TRUE
			$caib.ValueType = [string]
			$ocib.AddAttributeInfo($caib.Build())
		}
	
		# Standard attributes - single valued
		@("CommonName","Description","EmailAddress","GroupType","ManagedBy") | foreach {
			$ocib.AddAttributeInfo($AttributeInfoBuilder::Build($_,[string]))
		}
	
		# Standard attributes - multi valued
		@("Licenses") | foreach {
			$caib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeInfoBuilder($_)
			$caib.MultiValued = $TRUE
			$caib.ValueType = [string]
			$ocib.AddAttributeInfo($caib.Build())
		}

		# Technical attributes
		@("objectId","LastDirSyncTime") | foreach {
			$caib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeInfoBuilder($_)
			$caib.Creatable = $FALSE
			$caib.Updateable = $FALSE
			$caib.ValueType = [string]
			$ocib.AddAttributeInfo($caib.Build())
		}
	
		# Helper attribute to show the members
		@("__MEMBERS__") | foreach {
			$caib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeInfoBuilder($_)
			$caib.Creatable = $FALSE
			$caib.MultiValued = $TRUE
			$caib.ValueType = [System.Collections.Generic.Dictionary[String,Object]]
			$ocib.AddAttributeInfo($caib.Build())
		}
	
		$Connector.SchemaBuilder.DefineObjectClass($ocib.Build())

		###########################
 		# License object class
		###########################
		$ocib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ObjectClassInfoBuilder
		$ocib.ObjectType = "License"

		# Standard attributes - single valued string
		@("AccountName","AccountObjectId","AccountSkuId","SkuPartNumber","TargetClass") | foreach {
			$caib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeInfoBuilder($_)
			$caib.Creatable = $FALSE
			$caib.Updateable = $FALSE
			$caib.ValueType = [string]
			$ocib.AddAttributeInfo($caib.Build())
		}

		# Standard attributes - single valued integer
		@("ActiveUnits","ConsumedUnits","SuspendedUnits","WarningUnits") | foreach {
			$caib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeInfoBuilder($_)
			$caib.Creatable = $FALSE
			$caib.Updateable = $FALSE
			$caib.ValueType = [int]
			$ocib.AddAttributeInfo($caib.Build())
		}

		# Service status attribute - single map
		$caib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeInfoBuilder("ServiceStatus")
		$caib.Creatable = $FALSE
		$caib.Updateable = $FALSE
		$caib.ValueType = [System.Collections.Generic.Dictionary[String,Object]]
		$ocib.AddAttributeInfo($caib.Build())

		$Connector.SchemaBuilder.DefineObjectClass($ocib.Build())

		###########################
 		# Subscription object class
		###########################
		$ocib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ObjectClassInfoBuilder
		$ocib.ObjectType = "Subscription"

		# Standard attributes - single valued string
		@("DateCreated","NextLifecycleDate","OcpSubscriptionId","SkuId","SkuPartNumber","Status") | foreach {
			$caib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeInfoBuilder($_)
			$caib.Creatable = $FALSE
			$caib.Updateable = $FALSE
			$caib.ValueType = [string]
			$ocib.AddAttributeInfo($caib.Build())
		}

		# Standard attributes - single valued integer
		@("TotalLicenses") | foreach {
			$caib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeInfoBuilder($_)
			$caib.Creatable = $FALSE
			$caib.Updateable = $FALSE
			$caib.ValueType = [int]
			$ocib.AddAttributeInfo($caib.Build())
		}

		# Service status attribute - single map
		$caib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeInfoBuilder("ServiceStatus")
		$caib.Creatable = $FALSE
		$caib.Updateable = $FALSE
		$caib.ValueType = [System.Collections.Generic.Dictionary[String,Object]]
		$ocib.AddAttributeInfo($caib.Build())

		$Connector.SchemaBuilder.DefineObjectClass($ocib.Build())

	}
 }
catch #Re-throw the original exception message within a connector exception
{
	throw New-Object Org.IdentityConnectors.Framework.Common.Exceptions.ConnectorException($_.Exception.Message)
}
