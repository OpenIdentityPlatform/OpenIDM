/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014-2015 ForgeRock AS
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

import org.forgerock.json.resource.Connection
import org.forgerock.util.query.QueryFilter
import org.forgerock.json.resource.QueryRequest
import org.forgerock.json.resource.Requests
import org.forgerock.json.resource.ResourceResponse
import org.forgerock.services.context.RootContext
import org.forgerock.openicf.connectors.scriptedcrest.ScriptedCRESTConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.exceptions.UnknownUidException
import org.identityconnectors.framework.common.objects.Name
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.Uid

def operation = operation as OperationType
def configuration = configuration as ScriptedCRESTConfiguration
def connection = connection as Connection
def username = username as String
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions


def objectClassInfo = configuration.propertyBag[objectClass.objectClassValue];
if (objectClassInfo != null) {

    QueryRequest request = Requests.newQueryRequest(objectClassInfo.resourceContainer)

    def attributeDefinition = objectClassInfo.attributes[name]
    if (null != attributeDefinition) {
        request.queryFilter = QueryFilter.equalTo(String.valueOf(attributeDefinition.jsonName), username)
    } else {
        request.queryFilter = QueryFilter.equalTo(Name.NAME, username)
    }
    request.addField("_id", "_rev")


    def results = []
    def queryResult = connection.query(new RootContext(), request, results)

    if (results.empty) {
        throw new UnknownUidException()
    } else if (results.size() > 1) {
        throw new ConnectException("Multiple results 'userName' is not unique!")
    } else {
        ResourceResponse r = results.get(0) as ResourceResponse;
        return new Uid(r.id, r.revision)
    }

} else {
    throw new UnsupportedOperationException(operation.name() + " operation of type:" +
            objectClass.objectClassValue + " is not supported.")
}
