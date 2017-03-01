/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014-2016 ForgeRock AS.
 */

import groovy.json.JsonBuilder
import groovyx.net.http.RESTClient
import org.apache.http.client.HttpClient
import org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.Attribute
import org.identityconnectors.framework.common.objects.AttributesAccessor
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions

import static groovyx.net.http.ContentType.JSON

def operation = operation as OperationType
def createAttributes = new AttributesAccessor(attributes as Set<Attribute>)
def configuration = configuration as ScriptedRESTConfiguration
def httpClient = connection as HttpClient
def connection = customizedConnection as RESTClient
def name = id as String
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions

log.info("Entering " + operation + " Script");

switch (objectClass) {
    case ObjectClass.ACCOUNT:
        def builder = new JsonBuilder()
        builder {
            _id name
            contactInformation {
                telephoneNumber(createAttributes.hasAttribute("telephoneNumber") ? createAttributes.findString("telephoneNumber") : "")
                emailAddress(createAttributes.hasAttribute("emailAddress") ? createAttributes.findString("emailAddress") : "")
            }
            delegate.name({
                familyName(createAttributes.hasAttribute("familyName") ? createAttributes.findString("familyName") : "")
                givenName(createAttributes.hasAttribute("givenName") ? createAttributes.findString("givenName") : "")
            })
            displayName(createAttributes.hasAttribute("displayName") ? createAttributes.findString("displayName") : "")
        }

        if (createAttributes.hasAttribute("password")) {
            builder.content["password"] = createAttributes.findString("password")
        }
        
        connection.put(
                path: '/api/users/' + name,
                headers: ['If-None-Match': '*'],
                contentType: JSON,
                requestContentType: JSON,
                body: builder.toString());
        break

    case ObjectClass.GROUP:
        def builder = new JsonBuilder()
        builder {
            _id name
            members(createAttributes.hasAttribute("members") ? createAttributes.findList("members") : [])
        }
        connection.put(
                path: '/api/groups/' + name,
                headers: ['If-None-Match': '*'],
                body: builder.toString());

}
return name
