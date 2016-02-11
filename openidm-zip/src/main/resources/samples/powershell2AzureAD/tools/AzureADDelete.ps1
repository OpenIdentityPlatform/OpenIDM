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
    This is a sample Delete script for Azure Active Directory
	
.DESCRIPTION
	The script uses the Remove-Msolser and Remove-MsolGroup cmdlets to delete objects
	
.INPUT VARIABLES
	The connector sends us the following:
	- <prefix>.Configuration : handler to the connector's configuration object
	- <prefix>.Options: a handler to the Operation Options
	- <prefix>.Operation: String correponding to the operation ("AUTHENTICATE" here)
	- <prefix>.ObjectClass: the Object class object (__ACCOUNT__ / __GROUP__ / other)
	- <prefix>.Uid: the Uid (__UID__) object that specifies the object to delete
	
.RETURNS 
	Nothing.

.NOTES
    Prerequisite   : PowerShell V2 and later
#>
try
{
    $scriptPath = split-path -parent $Connector.Configuration.DeleteScriptFileName
    . $scriptPath\AzureADCommon.ps1

    if ($Connector.Operation -eq "DELETE")
    {
	    authenticateSession($Connector)

	    switch ($Connector.ObjectClass.Type)
	    {
		    "__ACCOUNT__"
            {
                Remove-MsolUser -ObjectId $Connector.Uid.GetUidValue() -Force
            }
		    "__GROUP__"
            {
                Remove-MsolGroup -ObjectId $Connector.Uid.GetUidValue() -Force
            }
		    default {throw "Unsupported type: $($Connector.ObjectClass.Type)"}
	    }
    }
    else
    {
	    throw new Org.IdentityConnectors.Framework.Common.Exceptions.ConnectorException(
            "DeleteScript can not handle operation: $($Connector.Operation)")
    }
}
    catch #Re-throw the original exception
{
	throw
}