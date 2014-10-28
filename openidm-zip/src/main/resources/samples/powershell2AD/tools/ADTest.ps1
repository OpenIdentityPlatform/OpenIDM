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
    This is a sample Test script for Active Directory
	
.DESCRIPTION
	The script tries to fetch the list of Domains of the Forest the service
	is logged to

.INPUT
	The connector sends us the following:
	- <prefix>.Configuration : handler to the connector's configuration object
	- <prefix>.Operation: String correponding to the action ("TEST" here)
	
.RETURNS
	Nothing. Should throw an exception if test failed
	
.NOTES  
    File Name      : ADTest.ps1  
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
 if ($Connector.Operation -eq "TEST")
 {
 	(Get-ADForest).domains
 }
 else
 {
 	throw new Org.IdentityConnectors.Framework.Common.Exceptions.ConnectorException("TestScript can not handle operation: $($Connector.Operation)")
 }
}
catch #Rethrow the original exception
{
	throw
}
