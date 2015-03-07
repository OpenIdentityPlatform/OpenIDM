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
import org.apache.olingo.commons.api.domain.v3.ODataEntity
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.forgerock.openicf.misc.scriptedcommon.ScriptedConfiguration
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.FrameworkUtil
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.objects.AttributeUtil
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.Uid
import org.identityconnectors.framework.common.objects.filter.EqualsFilter
import org.identityconnectors.framework.common.objects.filter.Filter

def operation = operation as OperationType
def configuration = configuration as ScriptedConfiguration
def client = configuration.propertyBag.get("ODataClient") as EdmEnabledODataClient
def filter = filter as Filter
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions

def edmEntityType = ODataUtils.getEdmEntityType(client.cachedEdm, objectClass)

def entityHandler = { ODataEntity entity ->
    def co = ICF.co {
        def objectId = ODataUtils.getUid(edmEntityType, entity)
        uid objectId
        id objectId.uidValue
        delegate.objectClass(objectClass)

        for (def property : entity.properties) {
            if (property.hasNullValue()) {
                attribute property.name
            } else if (property.hasPrimitiveValue()) {
                def value = property.getPrimitiveValue()
                if (FrameworkUtil.isSupportedAttributeType(value.type.defaultType)) {
                    attribute property.name, value.toValue()
                } else {
                    attribute property.name, value.toString()
                }
            } else if (property.hasCollectionValue()) {
                def value = property.getCollectionValue()
                attribute property.name, value.asJavaCollection()
            } else if (property.hasComplexValue()) {
                def value = property.getComplexValue()
                attribute property.name, value.asJavaMap()

            } else {
                throw new ConnectorException("Unknown property value")
            }
        }
    }
    handler(co)
}

if (null != edmEntityType) {
    if (filter instanceof EqualsFilter && ((EqualsFilter) filter).attribute.is(Uid.NAME)) {
        //This is a Read Request
        try {
            def uid = AttributeUtil.getStringValue(((EqualsFilter) filter).attribute)
            def request = client.getRetrieveRequestFactory().getEntityRequest(
                    ODataUtils.buildEntityURI(client.newURIBuilder(), edmEntityType, uid))
            def response = AzureADOAuth2HttpClientFactory.execute(request)
            entityHandler(response.body)

        } catch (ODataClientErrorException e) {
            throw ODataUtils.adapt(e)
        }
    } else {
        def builder = client.newURIBuilder()

        if (null != filter) {
            builder.filter(filter.accept(ODataFilterVisitor.VISITOR, client.getFilterFactory()))
        }

        def request =
                client.getRetrieveRequestFactory().getEntitySetRequest(ODataUtils.buildEntityURI(builder, edmEntityType, null));
        def response = AzureADOAuth2HttpClientFactory.execute(request)

        for (ODataEntity entity : response.getBody().getEntities()) {
            entityHandler(entity)
        }
    }
} else {
    throw new UnsupportedOperationException(operation.name() + " operation of type:" +
            objectClass.objectClassValue + " is not supported.")
}
