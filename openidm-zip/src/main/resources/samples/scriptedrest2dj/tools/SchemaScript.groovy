/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

import groovyx.net.http.RESTClient
import org.apache.http.client.HttpClient
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder
import org.identityconnectors.framework.common.objects.ObjectClass

import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.NOT_CREATABLE
import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.NOT_READABLE
import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.NOT_RETURNED_BY_DEFAULT
import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.NOT_UPDATEABLE

def operation = operation as OperationType
def configuration = configuration as ScriptedRESTConfiguration
def httpClient = connection as HttpClient
def connection = customizedConnection as RESTClient
def log = log as Log

log.info("Entering " + operation + " Script");

// Declare the __ACCOUNT__ attributes
// _id
def idAIB = new AttributeInfoBuilder("__NAME__", String.class);
idAIB.setRequired(true);
idAIB.setCreateable(true);
idAIB.setMultiValued(false);
idAIB.setUpdateable(false);

// userName
def userNameAIB = new AttributeInfoBuilder("userName", String.class);
userNameAIB.setCreateable(false);
userNameAIB.setMultiValued(false);
userNameAIB.setUpdateable(false);

// displayName
def displayNameAIB = new AttributeInfoBuilder("displayName", String.class);
displayNameAIB.setRequired(true);
displayNameAIB.setMultiValued(false);

// group displayName
def grpDisplayNameAIB = new AttributeInfoBuilder("displayName", String.class);
grpDisplayNameAIB.setMultiValued(false);
grpDisplayNameAIB.setCreateable(false);
grpDisplayNameAIB.setUpdateable(false);

// familyName
def familyNameAIB = new AttributeInfoBuilder("familyName", String.class);
familyNameAIB.setRequired(true);
familyNameAIB.setMultiValued(false);

// givenName
def givenNameAIB = new AttributeInfoBuilder("givenName", String.class);
givenNameAIB.setMultiValued(false);

// telephoneNumber
def telephoneNumberAIB = new AttributeInfoBuilder("telephoneNumber", String.class);
telephoneNumberAIB.setMultiValued(false);

// emailAddress
def emailAddressAIB = new AttributeInfoBuilder("emailAddress", String.class);
emailAddressAIB.setMultiValued(false);

// members
def membersAIB = new AttributeInfoBuilder("members", String.class);
membersAIB.setMultiValued(true);

// groups
def groupsAIB = new AttributeInfoBuilder("groups", String.class);
groupsAIB.setMultiValued(true);

//created
def createdAIB = new AttributeInfoBuilder("created", String.class);
createdAIB.setCreateable(false);
createdAIB.setMultiValued(false);
createdAIB.setUpdateable(false);

//lastModified
def lastModifiedAIB = new AttributeInfoBuilder("lastModified", String.class);
lastModifiedAIB.setCreateable(false);
lastModifiedAIB.setMultiValued(false);
lastModifiedAIB.setUpdateable(false);


return builder.schema({
    objectClass {
        type ObjectClass.ACCOUNT_NAME
        attribute idAIB.build()
        attribute userNameAIB.build()
        attribute displayNameAIB.build()
        attribute familyNameAIB.build()
        attribute givenNameAIB.build()
        attribute telephoneNumberAIB.build()
        attribute emailAddressAIB.build()
        attribute groupsAIB.build()
        attribute createdAIB.build()
        attribute lastModifiedAIB.build()
        attribute "password", String.class, EnumSet.of(NOT_RETURNED_BY_DEFAULT, NOT_UPDATEABLE, NOT_READABLE)
    }
    objectClass {
        type ObjectClass.GROUP_NAME
        attribute idAIB.build()
        attribute grpDisplayNameAIB.build()
        attribute createdAIB.build()
        attribute lastModifiedAIB.build()
        attribute membersAIB.build()
    }
})

log.info("Schema script done");