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

import org.apache.olingo.client.api.v3.EdmEnabledODataClient
import org.apache.olingo.commons.api.domain.v3.ODataEntity
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.forgerock.openicf.misc.scriptedcommon.ScriptedConfiguration
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.SyncToken

def operation = operation as OperationType
def configuration = configuration as ScriptedConfiguration
def client = configuration.propertyBag.get("ODataClient") as EdmEnabledODataClient
def log = log as Log
def objectClass = objectClass as ObjectClass

//http://msdn.microsoft.com/en-us/library/jj836245.aspx
def entityType = ODataUtils.getEdmEntityType(client.cachedEdm, objectClass)

if (null != entityType) {

    switch (operation) {
        case OperationType.SYNC:
            def options = options as OperationOptions
            def token = token as Object
            //TODO GET https://graph.windows.net/contoso.com/groups?deltaLink={token}

            def request =
                    client.getRetrieveRequestFactory().getEntitySetRequest(URI.create("https://graph.windows.net/goldengate.onmicrosoft.com/groups?deltaLink="));
            def response = AzureADOAuth2HttpClientFactory.execute(request)

            for (ODataEntity entity : response.getBody().getEntities()) {
                //entityHandler(entity)
                println(entity.properties)
            }

            break;
        case OperationType.GET_LATEST_SYNC_TOKEN:
            //TODO GET https://graph.windows.net/contoso.com/groups?deltaLink=

            def request =
                    client.getRetrieveRequestFactory().getEntityRequest(URI.create("https://graph.windows.net/goldengate.onmicrosoft.com/groups?deltaLink="));
            def response = AzureADOAuth2HttpClientFactory.execute(request)

            def body = response.body

            return new SyncToken(lastToken)

            break;
        default:
            throw new ConnectorException("SyncScript can not handle operation:" + operation.name())
    }

} else {
    throw new UnsupportedOperationException(operation.name() + " operation of type:" +
            objectClass.objectClassValue + " is not supported.")
}



