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
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.provisioner.Id;
import org.identityconnectors.framework.api.operations.APIOperation;
import org.identityconnectors.framework.api.operations.CreateApiOp;
import org.identityconnectors.framework.api.operations.UpdateApiOp;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.serializer.SerializerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;

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
     * Creates a new {@link ObjectClassInfoHelper}.
     * @param objectClass the {@link ObjectClass} to create the helper for.
     * @param nameAttribute the name attribute for the {@link ObjectClass}.
     * @param attributes the set of attributes for the {@link ObjectClass}.
     * @param attributesReturnedByDefault the  attributes to return by default for the {@link ObjectClass}.
     * @param properties the properties of the {@link ObjectClass}.
     */
    ObjectClassInfoHelper(ObjectClass objectClass, String nameAttribute, Set<AttributeInfoHelper> attributes,
        Set<String> attributesReturnedByDefault, JsonValue properties) {
        this.objectClass = objectClass;
        this.nameAttribute= nameAttribute;
        this.attributes = attributes;
        this.attributesReturnedByDefault = attributesReturnedByDefault;
        this.properties = properties;
    }

    /**
     * Get the {@link org.identityconnectors.framework.common.objects.ObjectClass} for this Helper.
     *
     * @return {@link org.identityconnectors.framework.common.objects.ObjectClass} of this Helper
     */
    public ObjectClass getObjectClass() {
        return objectClass;
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
            Set<String> attrsToGet = new HashSet();
            for (JsonPointer field : fieldFilters) {
                if (field.isEmpty() || returnResource || !Resource.FIELD_CONTENT_ID.equals(field.leaf()) || !Resource.FIELD_CONTENT_REVISION.equals(field.leaf())){
                    returnResource = true;
                }
                if (field.isEmpty()) {
                    attrsToGet.addAll(attributesReturnedByDefault);
                    continue;
                }
                
                for (AttributeInfoHelper attribute : attributes) {
                    if (attribute.getName().equals(field.leaf()) && attribute.getAttributeInfo().isReadable()) {
                        attrsToGet.add(attribute.getAttributeInfo().getName());
                        break;
                    }
                }
            }
            builder.setAttributesToGet(attrsToGet);
        }
        return returnResource;
    }


    public Attribute filterAttribute(JsonPointer field, Object valueAssertion) {
        if (field.size() != 1) {
            throw new IllegalArgumentException("Only one level JsonPointer supported");
        }
        String attributeName = field.leaf();

        // OPENIDM-2385 - map _id to the Uid attribute containing valueAssertion
        if (Resource.FIELD_CONTENT_ID.equals(attributeName)) {
            return new Uid(String.valueOf(valueAssertion));
        }

        for (AttributeInfoHelper ai: attributes){
            if (ai.getName().equals(attributeName)) {
                return ai.build(valueAssertion);
            }
        }

        throw new AttributeMissingException("Attribute " + attributeName + " does not exist as part of " + objectClass);
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

        Set<String> keySet = content.keys();
        Map<String,Attribute> result = new HashMap<String, Attribute>(keySet.size());
        if (!StringUtils.isBlank(nameValue)) {
            result.put(Name.NAME, new Name(nameValue));
        }
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

        checkForInvalidAttributes(request.getContent().keys());

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

        checkForInvalidAttributes(request.getContent().keys());

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

    // ensure all attributes specified in the data are present in the target schema
    private void checkForInvalidAttributes(Set<String> keys) throws BadRequestException {
        for (String requestKey : keys) {
            if (!requestKey.startsWith("_")) {
                boolean found = false;
                for (AttributeInfoHelper attributeInfo : attributes) {
                    if (attributeInfo.getName().equals(requestKey)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new BadRequestException("Target does not support attribute " + requestKey);
                }
            }
        }
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
