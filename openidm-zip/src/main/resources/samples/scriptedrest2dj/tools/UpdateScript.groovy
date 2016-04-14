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
import org.identityconnectors.framework.common.objects.Uid

import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.PUT

def operation = operation as OperationType
def updateAttributes = new AttributesAccessor(attributes as Set<Attribute>)
def configuration = configuration as ScriptedRESTConfiguration
def httpClient = connection as HttpClient
def connection = customizedConnection as RESTClient
def name = id as String
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions
def uid = uid as Uid

log.info("Entering " + operation + " Script");

switch (objectClass) {
    case ObjectClass.ACCOUNT:
        def builder = new JsonBuilder()
        builder {
            contactInformation {
                telephoneNumber(updateAttributes.hasAttribute("telephoneNumber") ? updateAttributes.findString("telephoneNumber") : "")
                emailAddress(updateAttributes.hasAttribute("emailAddress") ? updateAttributes.findString("emailAddress") : "")
            }
            delegate.name({
                familyName(updateAttributes.hasAttribute("familyName") ? updateAttributes.findString("familyName") : "")
                givenName(updateAttributes.hasAttribute("givenName") ? updateAttributes.findString("givenName") : "")
            })
            displayName(updateAttributes.hasAttribute("displayName") ? updateAttributes.findString("displayName") : "")
        }

        return connection.request(PUT, JSON) { req ->
            uri.path = "/users/${uid.uidValue}"
            body = builder.toString()
            headers.'If-Match' =  "*"

            response.success = { resp, json ->
                new Uid(json._id, json._rev)
            }
        }
    case ObjectClass.GROUP:
        if (updateAttributes.hasAttribute("members")) {
            def builder = new JsonBuilder()
            builder {
                members(updateAttributes.findList("members"))
            }
            return connection.request(PUT, JSON) { req ->
                uri.path = "/groups/${uid.uidValue}"
                body = builder.toString()
                headers.'If-Match' =  "*"

                response.success = { resp, json ->
                    new Uid(json._id, json._rev)
                }
            }           
        }
}
return uid