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
import org.apache.olingo.commons.api.edm.EdmPrimitiveType
import org.apache.olingo.commons.api.edm.EdmProperty
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.forgerock.openicf.misc.scriptedcommon.ScriptedConfiguration
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.FrameworkUtil
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder
import org.identityconnectors.framework.common.objects.Name

def operation = operation as OperationType
def configuration = configuration as ScriptedConfiguration
def client = configuration.propertyBag.get("ODataClient") as EdmEnabledODataClient
def log = log as Log

//http://msdn.microsoft.com/en-us/library/azure/hh974483.aspx

builder.schema({
    client.cachedEdm.schemas.each { schema ->
        schema.entityTypes.each { entityType ->
            objectClass {
                type entityType.annotationsTargetFQN.name

                attribute Name.NAME, String.class, EnumSet.of(Flags.NOT_CREATABLE, Flags.NOT_UPDATEABLE)
                entityType.properties.each { key, value ->

                    def edmProperty = value as EdmProperty

                    def builder = AttributeInfoBuilder.define(key as String)
                            .setMultiValued(edmProperty.isCollection())
                    //.setRequired(!edmProperty.isNullable())

                    if (edmProperty.isPrimitive()) {
                        def type = edmProperty.getType() as EdmPrimitiveType
                        if (FrameworkUtil.isSupportedAttributeType(type.defaultType)) {
                            builder.setType(type.defaultType)
                        }
                    } else {
                        builder.setType(Map.class)
                    }

                    attribute builder.build()

                }

                entityType.navigationPropertyNames.each {
                    def edmProperty = entityType.getNavigationProperty(it)

                    def builder = AttributeInfoBuilder.define(it)
                            .setMultiValued(edmProperty.isCollection())
                    //.setRequired(!edmProperty.isNullable())

                    attribute builder.build()
                }
            }
        }
    }
})



