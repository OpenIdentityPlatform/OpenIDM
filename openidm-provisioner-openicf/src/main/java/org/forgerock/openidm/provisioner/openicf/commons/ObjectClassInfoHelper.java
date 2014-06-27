/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.provisioner.openicf.commons;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.crypto.JsonCryptoException;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.schema.validator.Constants;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.provisioner.Id;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.operations.APIOperation;
import org.identityconnectors.framework.api.operations.CreateApiOp;
import org.identityconnectors.framework.api.operations.UpdateApiOp;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.serializer.SerializerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 *
 * @author Laszlo Hordos
 */
public class ObjectClassInfoHelper {

    /**
     * Setup logging for the {@link ObjectClassInfoHelper}.
     */
    private final static Logger logger = LoggerFactory.getLogger(ObjectClassInfoHelper.class);

    private final ObjectClass objectClass;
    private final String nameAttribute;
    private final Set<AttributeInfoHelper> attributes;
    private final Set<String> attributesReturnedByDefault;
    private final JsonValue properties;

    /**
     * Create a custom object class.
     *
     * @param schema string representation for the name of the object class.
     * @throws IllegalArgumentException when objectClass is null
     * @throws org.forgerock.json.fluent.JsonValueException
     *
     */
    public ObjectClassInfoHelper(JsonValue schema) {
        //Expect ObjectClass
        objectClass = new ObjectClass(schema.get(ConnectorUtil.OPENICF_OBJECT_CLASS).required().asString());

        //Expect Properties Map
        properties = schema.get(Constants.PROPERTIES).required().expect(Map.class);
        Set<String> propertyNames = properties.keys();

        Set<AttributeInfoHelper> attributes0 = new HashSet<AttributeInfoHelper>(propertyNames.size());
        Set<String> defaultAttributes = new HashSet<String>(propertyNames.size());
        String __NAME__ = null;

        for (String propertyName : propertyNames) {
            AttributeInfoHelper helper = new AttributeInfoHelper(propertyName, false, properties.get(propertyName));
            if (helper.getAttributeInfo().getName().equals(Name.NAME)) {
                __NAME__ = propertyName;
            }
            if (helper.getAttributeInfo().isReturnedByDefault()) {
                defaultAttributes.add(helper.getAttributeInfo().getName());
            }
            attributes0.add(helper);
        }
        //TODO Should we throw exceptions or ??
        if (null == __NAME__) {
            logger.warn("Required __NAME__ attribute definition is not configured. The CREATE operation will be disabled");
            //throw new IllegalArgumentException("Required __NAME__ attribute is not configured");
        }
        nameAttribute = __NAME__;
        attributes = Collections.unmodifiableSet(attributes0);
        attributesReturnedByDefault = CollectionUtil.newReadOnlySet(defaultAttributes);
    }

    /**
     * Get a new newBuilder of the {@link org.identityconnectors.framework.common.objects.ObjectClass} for this schema.
     *
     * @return new newBuilder of {@link org.identityconnectors.framework.common.objects.ObjectClass}
     */
    public ObjectClass getObjectClass() {
        return objectClass;
    }


    public boolean isCreateable() {
        return null != nameAttribute;
    }

    /**
     * Get a read only set of attributes should return by default.
     * <p/>
     * If the {@link OperationOptions#OP_ATTRIBUTES_TO_GET} attribute value is null this is the default always.
     *
     * @return set of attribute names to get for the object.
     */
    public Set<String> getAttributesReturnedByDefault() {
        return attributesReturnedByDefault;
    }

    public boolean setAttributesToGet(final OperationOptionsBuilder builder,
            final List<JsonPointer> fieldFilters) {
        boolean returnResource = false;
        if (null != fieldFilters) {
            for (JsonPointer field : fieldFilters) {
                if (field.isEmpty() || returnResource || !Resource.FIELD_CONTENT_ID.equals(field.leaf()) || !Resource.FIELD_CONTENT_REVISION.equals(field.leaf())){
                    returnResource = true;
                }
                if (field.isEmpty()) {
                    builder.setAttributesToGet(attributesReturnedByDefault);
                    continue;
                }
                for (AttributeInfoHelper attribute : attributes) {
                    if (attribute.getName().equals(field.leaf()) && attribute.getAttributeInfo().isReadable()) {
                        builder.setAttributesToGet(attribute.getAttributeInfo().getName());
                        break;
                    }
                }
            }
        }
        return returnResource;
    }


    //TODO throw-catch exceptions if type is not match
    public Attribute filterAttribute(JsonPointer field, Object valueAssertion) {
      if (field.size() != 1){
          throw new IllegalArgumentException("Only one level JsonPointer supported");
      }
        String attributeName = field.leaf();

       for (AttributeInfoHelper ai: attributes){
          if (ai.getName().equals(attributeName)) {
              return ai.build(valueAssertion);
          }
       }
       return null;
    }

    /**
     * Get the resourceId from a CreateRequest
     * @param request CreateRequest
     * @return resourceId to be used for creating the object
     */
    public String getCreateResourceId(final CreateRequest request) {
        String nameValue = request.getNewResourceId();

        if (null == nameValue) {
            JsonValue o = request.getContent().get(nameAttribute);
            if (o.isNull()) {
                o = request.getContent().get(Resource.FIELD_CONTENT_ID);
            }
            if (o.isString()) {
                nameValue = o.asString();
            }
        }
        return nameValue;
    }

    /**
     * Get the attributes are that are writable on a create
     * @param request CreateRequest
     * @param cryptoService encryption and decryption service
     * @return Set of attributes to that are writable on create
     * @throws BadRequestException when attribute is missing or has a null value
     */
    public Set<Attribute> getCreateAttributes(final CreateRequest request,
            final CryptoService cryptoService) throws ResourceException {
        JsonValue content = request.getContent().required().expect(Map.class);
        String nameValue = getCreateResourceId(request);

        if (StringUtils.isBlank(nameValue)) {
            throw new BadRequestException("Required '_id' or '" + nameAttribute
                    + "' attribute is missing");
        }

        Set<String> keySet = content.keys();
        Map<String,Attribute> result = new HashMap<String, Attribute>(keySet.size());
        result.put(Name.NAME, new Name(nameValue));
        for (AttributeInfoHelper attributeInfo : attributes) {
            if (Name.NAME.equals(attributeInfo.getAttributeInfo().getName())
                    || Uid.NAME.equals(attributeInfo.getAttributeInfo().getName())
                    || (!keySet.contains(attributeInfo.getName()) && !attributeInfo.getAttributeInfo().isRequired())) {
                continue;
            }
            if (attributeInfo.getAttributeInfo().isCreateable()) {
                JsonValue v = content.get(attributeInfo.getName());
                if (v.isNull() && attributeInfo.getAttributeInfo().isRequired()) {
                    throw new BadRequestException("Required attribute '" + attributeInfo.getName()
                            + "' value is null");
                }
                Attribute a = attributeInfo.build(v.getObject(), cryptoService);
                if (null != a) {
                    result.put(attributeInfo.getAttributeInfo().getName(), a);
                }
            }
        }
        if (logger.isTraceEnabled()) {
            ConnectorObjectBuilder builder = new ConnectorObjectBuilder().addAttributes(result.values());
            builder.setName(nameValue);
            builder.setUid(nameValue);
            logger.trace("Update ConnectorObject: {}", SerializerUtil.serializeXmlObject(builder
                    .build(), false));
        }
        return new HashSet<Attribute>(result.values());
    }

    public Set<Attribute> getUpdateAttributes(final UpdateRequest request, final Name newName,
            final CryptoService cryptoService) throws ResourceException {
        JsonValue content = request.getContent().required().expect(Map.class);
        Set<String> keySet = content.keys();

        Map<String,Attribute> result = new HashMap<String, Attribute>(keySet.size());
        if (null != newName) {
            result.put(Name.NAME, newName);
        }
        for (AttributeInfoHelper attributeInfo : attributes) {
            if ( Uid.NAME.equals(attributeInfo.getAttributeInfo().getName())
                    || !keySet.contains(attributeInfo.getName())) {
                continue;
            }
            if (attributeInfo.getAttributeInfo().isUpdateable()) {
                Object v = content.get(attributeInfo.getName()).getObject();
                Attribute a = attributeInfo.build(v, cryptoService);
                if (null != a) {
                    result.put(attributeInfo.getAttributeInfo().getName(), a);
                }
            }
        }
        if (logger.isTraceEnabled()) {
            ConnectorObjectBuilder builder = new ConnectorObjectBuilder().addAttributes(result.values());
            if (null != newName) {
                builder.setUid(newName.getName());
            } else {
                builder.setName("***");
                builder.setUid("***");
            }
            logger.trace("Update ConnectorObject: {}", SerializerUtil.serializeXmlObject(builder
                    .build(), false));
        }
        return new HashSet<Attribute>(result.values());
    }

    /**
     * @param operation
     * @param name
     * @param source
     * @param cryptoService
     * @return
     * @throws JsonResourceException if ID value can not be determined from the {@code source}
     */
    public ConnectorObject build(Class<? extends APIOperation> operation, String name, JsonValue source, CryptoService cryptoService) throws Exception {
        String nameValue = name;

        if (null == nameValue) {
            JsonValue o = source.get(Resource.FIELD_CONTENT_ID);
            if (o.isNull()) {
                o = source.get(nameAttribute);
            }
            if (o.isString()) {
                nameValue = o.asString();
            }
        }

        if (null == nameValue) {
            throw new BadRequestException("Required NAME attribute is missing");
        } else {
            nameValue = Id.unescapeUid(nameValue);
            logger.trace("Build ConnectorObject {} for {}", nameValue, operation.getSimpleName());
        }

        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
        builder.setObjectClass(objectClass);
        builder.setUid(nameValue);
        builder.setName(nameValue);
        Set<String> keySet = source.required().asMap().keySet();
        if (CreateApiOp.class.isAssignableFrom(operation)) {
            for (AttributeInfoHelper attributeInfo : attributes) {
                if (Name.NAME.equals(attributeInfo.getName()) || Uid.NAME.equals(attributeInfo.getName()) ||
                        !keySet.contains(attributeInfo.getName())) {
                    continue;
                }
                if (attributeInfo.getAttributeInfo().isCreateable()) {
                    Object v = source.get(attributeInfo.getName()).getObject();
                    if (null == v && attributeInfo.getAttributeInfo().isRequired()) {
                        throw new IllegalArgumentException("Required attribute {" + attributeInfo.getName() + "} value is null");
                    }
                    builder.addAttribute(attributeInfo.build(v, cryptoService));
                }
            }
        } else if (UpdateApiOp.class.isAssignableFrom(operation)) {
            for (AttributeInfoHelper attributeInfo : attributes) {
                if (Name.NAME.equals(attributeInfo.getName()) || Uid.NAME.equals(attributeInfo.getName()) ||
                        !keySet.contains(attributeInfo.getName())) {
                    continue;
                }
                if (attributeInfo.getAttributeInfo().isUpdateable()) {
                    Object v = source.get(attributeInfo.getName()).getObject();
                    builder.addAttribute(attributeInfo.build(v, cryptoService));
                }
            }
        } else {
            for (AttributeInfoHelper attributeInfo : attributes) {
                if (Name.NAME.equals(attributeInfo.getName()) || Uid.NAME.equals(attributeInfo.getName()) ||
                        !keySet.contains(attributeInfo.getName())) {
                    continue;
                }
                Object v = source.get(attributeInfo.getName()).getObject();
                builder.addAttribute(attributeInfo.build(v, cryptoService));
            }
        }
        ConnectorObject result = builder.build();
        if (logger.isTraceEnabled()) {
            logger.trace("ConnectorObject build return: {}", SerializerUtil.serializeXmlObject(result, false));
        }
        return result;
    }

    public Resource build(ConnectorObject source, CryptoService cryptoService) throws IOException, JsonCryptoException {
        if (null == source) {
            return null;
        }
        if (logger.isTraceEnabled()) {
            logger.trace("ConnectorObject source: {}", SerializerUtil.serializeXmlObject(source, false));
        }
        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>(source.getAttributes().size()));
        for (AttributeInfoHelper attributeInfo : attributes) {
            Attribute attribute = source.getAttributeByName(attributeInfo.getAttributeInfo().getName());
            if (null != attribute && attributeInfo.getAttributeInfo().isReadable()) {
                result.put(attributeInfo.getName(), attributeInfo.build(attribute, cryptoService));
            }
        }
        Uid uid = source.getUid();
        // TODO are we going to escape ids?
        result.put(Resource.FIELD_CONTENT_ID, /*Id.escapeUid(*/uid.getUidValue()/*)*/);
        if (null != uid.getRevision()) {
            //System supports Revision
            result.put(Resource.FIELD_CONTENT_REVISION, uid.getRevision());
        }
        return new Resource(uid.getUidValue(), uid.getRevision(), result );
    }

    public Attribute build(String attributeName, Object source, CryptoService cryptoService) throws Exception {
        for (AttributeInfoHelper attributeInfoHelper : attributes) {
            if (attributeInfoHelper.getName().equals(attributeName)) {
                return attributeInfoHelper.build(source, cryptoService);
            }
        }
        if (source instanceof Collection) {
            return AttributeBuilder.build(attributeName, (Collection) source);
        } else {
            return AttributeBuilder.build(attributeName, source);
        }
    }

    /**
     * Get the object class properties
     * @return The object class properties as a jsonValue map.
     */
    public JsonValue getProperties() {
        return properties;
    }
}
