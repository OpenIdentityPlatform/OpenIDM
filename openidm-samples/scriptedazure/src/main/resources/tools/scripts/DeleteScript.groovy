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

import AzureADOAuth2HttpClientFactory
import org.apache.olingo.client.api.communication.ODataClientErrorException
import org.apache.olingo.client.api.v3.EdmEnabledODataClient
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.forgerock.openicf.misc.scriptedcommon.ScriptedConfiguration
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.Uid

def operation = operation as OperationType
def configuration = configuration as ScriptedConfiguration
def client = configuration.propertyBag.get("ODataClient") as EdmEnabledODataClient
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions
def uid = uid as Uid

def edmEntityType = ODataUtils.getEdmEntityType(client.cachedEdm, objectClass)

if (null != edmEntityType) {
    try {
        def request = client.CUDRequestFactory.getDeleteRequest(
                ODataUtils.buildEntityURI(client.newURIBuilder(), edmEntityType, uid.uidValue))
        def response = AzureADOAuth2HttpClientFactory.execute(request)
        response.close()
    } catch (ODataClientErrorException e) {
        throw ODataUtils.adapt(e)
    }
} else {
    throw new UnsupportedOperationException(operation.name() + " operation of type:" +
            objectClass.objectClassValue + " is not supported.")
}
