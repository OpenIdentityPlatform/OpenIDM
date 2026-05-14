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
    This is a sample Schema script for Active Directory 
	
.DESCRIPTION
	The script fetches user and group schema definition from Active Directory Schema
	and then build the proper OpenICF Schema
	
.INPUT VARIABLES
	The connector sends us the following:
	- <prefix>.Configuration : handler to the connector's configuration object
	- <prefix>.Operation: String correponding to the action ("SCHEMA" here)
	- <prefix>.SchemaBuilder: an instance of org.identityconnectors.framework.common.objects.SchemaBuilder 
	that must be used to define the schema.
	
.RETURNS 
	Nothing. Connector will finalize the schema build.
	
.NOTES  
    File Name      : ADSchema.ps1  
    Author         : Gael Allioux (gael.allioux@forgerock.com)
    Prerequisite   : PowerShell V2 - AD module loaded by the connector
    Copyright 2014 - ForgeRock AS    

.LINK  
    Script posted over:  
    http://openicf.forgerock.org
		
	Active Directory Administration with Windows PowerShell
	http://technet.microsoft.com/en-us/library/dd378937(v=ws.10).aspx
#>

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
	$Required = @("sAMAccountName")
	
	foreach ($attr in $Required)
	{
		$caib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeInfoBuilder($attr);
		$caib.Required = $TRUE
		$caib.ValueType = [string];
		$ocib.AddAttributeInfo($caib.Build())
	}
	
	# Standard attributes - single valued
	$StandardSingle = @("division","primaryInternationalISDNNumber","c","l","department","givenName","telephoneNumber","employeeNumber","displayName",
	"personalTitle","homeDirectory","postalCode","manager","st","initials","employeeType","streetAddress","co","title","middleName","wWWHomePage","company",
	"comment","scriptPath","mail","displayNamePrintable","ipPhone","homePostalAddress","facsimileTelephoneNumber","homePhone","street","homeDrive",
	"info","assistant","mobile","employeeID","logonWorkstation","userWorkstations","userSharedFolder","description", "name","sn","userPrincipalName")
	
	foreach ($attr in $StandardSingle)
	{
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build($attr,[string]))
	}
	
	# Standard attributes - multi valued
	$StandardMulti = @("otherMailbox","otherLoginWorkstations","o","postOfficeBox","otherTelephone",
	"otherMobile","seeAlso","url","ou","postalAddress","otherHomePhone","internationalISDNNumber")
	
	foreach ($attr in $StandardMulti)
	{
		$caib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeInfoBuilder($attr);
		$caib.MultiValued = $TRUE
		$caib.ValueType = [string];
		$ocib.AddAttributeInfo($caib.Build())
	}
	
	# Standard attributes - multi valued - readonly back link
	$StandardMultiRO = @("memberOf","directReports")
	
	foreach ($attr in $StandardMultiRO)
	{
		$caib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeInfoBuilder($attr);
		$caib.MultiValued = $TRUE
		$caib.Creatable = $FALSE
		$caib.Updateable = $FALSE
		$caib.ValueType = [string];
		$ocib.AddAttributeInfo($caib.Build())
	}
	
	# Technical attributes
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("userAccountControl",[int]))
	# $ocib.AddAttributeInfo($AttributeInfoBuilder::Build("accountExpires",[int]))
	
	$Technical = @("objectGUID","cn","uSNCreated","uSNChanged","whenCreated","whenChanged","createTimeStamp","modifyTimeStamp","lastLogonTimestamp",
	"badPwdCount","badPasswordTime","lastLogon","pwdLastSet","logonCount","lockoutTime")
	
	foreach ($attr in $Technical)
	{
		$caib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeInfoBuilder($attr);
		$caib.Creatable = $FALSE
		$caib.Updateable = $FALSE
		$caib.ValueType = [string];
		$ocib.AddAttributeInfo($caib.Build())
	}
	
	# A few custom attributes we want to add here
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("accountExpirationDate",[string]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("allowReversiblePasswordEncryption",[bool]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("cannotChangePassword",[bool]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("changePasswordAtLogon",[bool]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("passwordNeverExpires",[bool]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("passwordNotRequired",[bool]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("smartcardLogonRequired",[bool]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("trustedForDelegation",[bool]))
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build("enabled",[bool]))
	
 	# A few operational attributes as well
	$opAttrs = [Org.IdentityConnectors.Framework.Common.Objects.OperationalAttributeInfos]
	$ocib.AddAttributeInfo($opAttrs::PASSWORD)
	$ocib.AddAttributeInfo($opAttrs::LOCK_OUT)
	$ocib.AddAttributeInfo($opAttrs::PASSWORD_EXPIRED)
	
	$Connector.SchemaBuilder.DefineObjectClass($ocib.Build())
	
	
	###########################
 	# __GROUP__ object class
	###########################
	$ocib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ObjectClassInfoBuilder
	$ocib.ObjectType = "__GROUP__"
		
	# Standard attributes - single valued
	$StandardSingle = @("wWWHomePage","telephoneNumber","mail","displayNamePrintable","displayName",
						"managedBy","info","description","name","sAMAccountName","groupCategory","groupScope")
	
	foreach ($attr in $StandardSingle)
	{
	$ocib.AddAttributeInfo($AttributeInfoBuilder::Build($attr,[string]))
	}
	
	# Standard attributes - multi valued
	$StandardMulti = @("secretary","member")
	
	foreach ($attr in $StandardMulti)
	{
		$caib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeInfoBuilder($attr);
		$caib.MultiValued = $TRUE
		$caib.ValueType = [string];
		$ocib.AddAttributeInfo($caib.Build())
	}

	# Standard attributes - multi valued - readonly back link
	$StandardMultiRO = @("memberOf","directReports")
	
	foreach ($attr in $StandardMultiRO)
	{
		$caib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeInfoBuilder($attr);
		$caib.MultiValued = $TRUE
		$caib.Creatable = $FALSE
		$caib.Updateable = $FALSE
		$caib.ValueType = [string];
		$ocib.AddAttributeInfo($caib.Build())
	}
	
	# Technical attributes
	$Technical = @("objectGUID","cn","uSNCreated","uSNChanged","whenCreated","whenChanged","createTimeStamp","modifyTimeStamp")
	
	foreach ($attr in $Technical)
	{
		$caib = New-Object Org.IdentityConnectors.Framework.Common.Objects.ConnectorAttributeInfoBuilder($attr);
		$caib.Creatable = $FALSE
		$caib.Updateable = $FALSE
		$caib.ValueType = [string];
		$ocib.AddAttributeInfo($caib.Build())
	}
	
	$Connector.SchemaBuilder.DefineObjectClass($ocib.Build())
 }
 }
 catch #Rethrow the original exception
 {
 	throw
 }
