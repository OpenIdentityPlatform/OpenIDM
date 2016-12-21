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

import org.apache.olingo.client.api.communication.ODataClientErrorException
import org.apache.olingo.client.api.uri.v3.URIBuilder
import org.apache.olingo.commons.api.Constants
import org.apache.olingo.commons.api.domain.CommonODataProperty
import org.apache.olingo.commons.api.domain.ODataComplexValue
import org.apache.olingo.commons.api.domain.ODataPrimitiveValue
import org.apache.olingo.commons.api.domain.v3.ODataEntity
import org.apache.olingo.commons.api.domain.v3.ODataObjectFactory
import org.apache.olingo.commons.api.edm.Edm
import org.apache.olingo.commons.api.edm.EdmComplexType
import org.apache.olingo.commons.api.edm.EdmEntityType
import org.apache.olingo.commons.api.edm.EdmPrimitiveType
import org.apache.olingo.commons.api.edm.EdmProperty
import org.identityconnectors.common.StringUtil
import org.identityconnectors.framework.common.FrameworkUtil
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException
import org.identityconnectors.framework.common.exceptions.UnknownUidException
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.Uid

/**
 *
 * @author Laszlo Hordos
 */
public class ODataUtils {
    private ODataUtils() {
        //This is a util class
    }


    static ODataPrimitiveValue primitiveBuild(ODataObjectFactory factory, EdmPrimitiveType type, Object value) {
        def valueBuilder = factory.newPrimitiveValueBuilder().setType(type);
        if (FrameworkUtil.isSupportedAttributeType(type.defaultType)) {
            if (type.defaultType.isInstance(value)) {
                valueBuilder.setValue(value)
            } else {
                throw new InvalidAttributeValueException("Expecting value ${type.defaultType} but found ${value.class}");
            }
        } else if (null != value) {
            valueBuilder.setValue(type.valueOfString(value as String, true, null,
                    Constants.DEFAULT_PRECISION, Constants.DEFAULT_SCALE, true, type.getDefaultType()))
        }
        return valueBuilder.build()
    }


    static ODataComplexValue<? extends CommonODataProperty> complexBuild(ODataObjectFactory factory, EdmComplexType
            complexType, Map<String, Object> map) {

        def complex = factory.newComplexValue(complexType.annotationsTargetFQN.fullQualifiedNameAsString)
        complexType.properties.each { key, value ->
            def attrValue = map.get(key)
            if (null != attrValue) {
                def edmProperty = value as EdmProperty
                complex.add(valueBuild(factory, edmProperty, attrValue))
            }
        }
        return complex
    }


    static CommonODataProperty valueBuild(ODataObjectFactory factory, EdmProperty
            edmProperty, Object value) {

        if (edmProperty.isPrimitive()) {
            def type = edmProperty.getType() as EdmPrimitiveType
            if (edmProperty.isCollection()) {
                if (null != value && !(value instanceof Collection)) {
                    value = [value]
                }
                def collectionValue = factory.newCollectionValue(type.fullQualifiedName as String)
                if (value != null) {
                    value.each {
                        if (it != null) {
                            collectionValue.add(primitiveBuild(factory, type, it))
                        }
                    }
                }
                return factory.newCollectionProperty(edmProperty.name, collectionValue)
            } else {
                if (value instanceof Collection) {
                    value = ((Collection) value).first()
                }
                def primitive
                if (value != null) {
                    primitive = primitiveBuild(factory, type, value)
                }
                return factory.newPrimitiveProperty(edmProperty.name, primitive)
            }
        } else if (edmProperty.getType() instanceof EdmComplexType) {
            if (edmProperty.isCollection()) {
                if (null != value && !(value instanceof Collection)) {
                    value = [value]
                }
                def collectionValue = factory.newCollectionValue(edmProperty.annotationsTargetFQN.fullQualifiedNameAsString)
                if (value != null) {
                    value.each {
                        def complex
                        if (it instanceof Map) {
                            complex = complexBuild(factory, edmProperty.type, it)
                            collectionValue.add(complex)
                        } else if (null != it) {
                            throw new InvalidAttributeValueException("Expecting Map to create ComplexValue for '${edmProperty.name}'");
                        }
                        //TODO What if Null?
                    }
                }
                return factory.newCollectionProperty(edmProperty.name, collectionValue)
            } else {
                if (value instanceof Collection) {
                    value = ((Collection) value).first()
                }
                def complex
                if (value instanceof Map) {
                    complex = complexBuild(factory, edmProperty.type, value)
                } else if (null != value) {
                    throw new InvalidAttributeValueException("Expecting Map to create ComplexValue for '${edmProperty.name}'");
                }
                return factory.newComplexProperty(edmProperty.name, complex)
            }
        }
    }

    /**
     * Get the String representing plural format of a entity set.
     *
     * @return the String representing plural format of a entity set
     */
    static String toPluralString(EdmEntityType edmEntityType) {
        return edmEntityType.annotationsTargetFQN.name.toLowerCase() + "s";
    }

    /**
     * Build a URI pointing to an Entity 
     *
     * @param builder
     * @param edmEntityType
     * @param id
     * @return
     */
    static URI buildEntityURI(URIBuilder builder, EdmEntityType edmEntityType, String id) {
        def uri = builder.appendEntitySetSegment(toPluralString(edmEntityType))
        if (StringUtil.isNotBlank(id)) {
            uri.appendNavigationSegment(id)
        }
        org.apache.http.client.utils.URIBuilder b = new org.apache.http.client.utils.URIBuilder(uri.build());
        //b.addParameter("api-version", "1.21-preview");
        b.addParameter("api-version", "1.5");
        /*
          Beginning with version 1.5, the Graph API namespace is changed from Microsoft.WindowsAzure.ActiveDirectory 
          to Microsoft.DirectoryServices. Earlier versions of the Graph API continue to use the previous namespace;
          for example, $filter=isof(‘Microsoft.WindowsAzure.ActiveDirectory.Contact’).
        */
        return b.build();
    }

    static RuntimeException adapt(ODataClientErrorException exception) {
        if ("Request_ResourceNotFound".equals(exception.ODataError.code)) {
            return new UnknownUidException(exception.getMessage(), exception)
        } else if ("Request_BadRequest".equals(exception.ODataError.code) &&
                "A conflicting object with one or more of the specified property values is present in the directory.".equals(exception.ODataError.message)) {
            return new AlreadyExistsException(exception.message, exception)
        } else {
            return ConnectorException.wrap(exception)
        }
    }

    static EdmEntityType getEdmEntityType(Edm edm, ObjectClass objectClass) {
        def type = edm.schemas.findResult { schema ->
            return schema.entityTypes.findResult { entityType ->
                if (objectClass.is(entityType.annotationsTargetFQN.name as String)) {
                    return entityType
                }
                return null
            }
        }
        return type
    }

    static Uid getUid(EdmEntityType edmEntityType, ODataEntity entity) {
        def keyProperty = edmEntityType.getKeyPropertyRefs().findResult {
            return entity.getProperty(it.getKeyPropertyName())
        }

        if (null != keyProperty) {
            if (keyProperty.hasPrimitiveValue()) {
                def objectId = keyProperty.value.asPrimitive().toValue() as String
                def eTag = entity.ETag
                if (StringUtil.isBlank(eTag)) {
                    return new Uid(objectId);
                } else {
                    return new Uid(objectId, eTag);
                }
            }
        }
    }
}