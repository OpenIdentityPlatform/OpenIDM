/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2015 ForgeRock AS. All Rights Reserved
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

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.http.util.Paths.*;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.crypto.JsonCryptoException;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestType;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.Responses;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.provisioner.Id;
import org.forgerock.openidm.util.ResourceUtil;
import org.identityconnectors.framework.api.operations.APIOperation;
import org.identityconnectors.framework.api.operations.CreateApiOp;
import org.identityconnectors.framework.api.operations.UpdateApiOp;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.serializer.SerializerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
            Set<String> attrsToGet = new HashSet<>();
            for (JsonPointer field : fieldFilters) {
                if (field.isEmpty()
                        || returnResource
                        || !ResourceResponse.FIELD_CONTENT_ID.equals(field.leaf())
                        || !ResourceResponse.FIELD_CONTENT_REVISION.equals(field.leaf())){
                    returnResource = true;
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

    /**
     * Extracts attribute-name from {@link JsonPointer} and performs any necessary normalization
     *
     * @param field Single-level {@link JsonPointer} which is the CREST attribute-name
     * @return ICF attribute-name
     */
    protected String extractAttributeName(JsonPointer field) {
        if (field.size() != 1) {
            throw new IllegalArgumentException("Only one level JsonPointer supported");
        }
        final String attributeName = field.leaf();

        // OPENIDM-2385 - map _id to the Uid attribute containing valueAssertion
        if (ResourceResponse.FIELD_CONTENT_ID.equals(attributeName)) {
            return Uid.NAME;
        }
        return attributeName;
    }

    /**
     * Finds a ICF attribute-name, given a CREST attribute-name
     *
     * @param field Single-level {@link JsonPointer} which is the CREST attribute-name
     * @return ICF attribute-name or {@code null} if not present
     */
    public String getAttributeName(JsonPointer field) {
        final String attributeName = extractAttributeName(field);

        for (final AttributeInfoHelper ai: attributes){
            if (ai.getName().equals(attributeName)) {
                return ai.getAttributeInfo().getName();
            }
        }
        return null;
    }

    public Attribute filterAttribute(JsonPointer field, Object valueAssertion) {
        if (field.size() != 1) {
            throw new IllegalArgumentException("Only one level JsonPointer supported");
        }
        String attributeName = field.leaf();

        // OPENIDM-2385 - map _id to the Uid attribute containing valueAssertion
        if (ResourceResponse.FIELD_CONTENT_ID.equals(attributeName)) {
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
     * Get the un-encoded resourceId from the specified Request
     * @param request Request
     * @return resourceId to be used for CRUDPAQ operations
     */
    public String getFullResourceId(final Request request) {
        ResourcePath fullId = request.getResourcePathObject();
        
        // For everything but Create requests, simply decode the resource path.
        // If this is a Create request, construct the fullId from a concatentation
        // of the resource path and the user spcified id if present.
        if (request.getRequestType().equals(RequestType.CREATE)) {
            CreateRequest cr = (CreateRequest)request;
            String newResourceId = cr.getNewResourceId();
            if (null == newResourceId) {
                JsonValue o = cr.getContent().get(nameAttribute);
                if (o.isNull()) {
                    o = cr.getContent().get(ResourceResponse.FIELD_CONTENT_ID);
                }
                if (o.isString()) {
                    newResourceId = o.asString();
                }
            }
            if (newResourceId != null) {
                fullId = fullId.concat(newResourceId);
            }
        }
        return urlDecode(fullId.toString());
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
        String nameValue = getFullResourceId(request);

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
                if (null != a && a.getValue() != null && a.getValue().size() > 0) {
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

    /**
     * Get the attributes are that are writable on an update.
     * 
     * @param request UpdateRequest
     * @param newName a new name
     * @param cryptoService encryption and decryption service
     * @return Set of attributes to that are writable on update
     * @throws ResourceException if and error is encountered
     */
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

    /**
     * Gets an attribute that is writable on a patch.
     * 
     * @param patchOperation the {@link PatchOperation}
     * @param before the before value
     * @param cryptoService encryption and decryption service
     * @return an Attributes that is writable on patch
     * @throws ResourceException if and error is encountered
     */
    public Attribute getPatchAttribute(final PatchOperation patchOperation, final JsonValue before, 
            final CryptoService cryptoService) throws ResourceException {
        Attribute result = null;
        
        // A Map to hold the attributes being patched
        JsonValue patchedContent = json(object());
        // The field to patch
    	JsonPointer field = patchOperation.getField();
    	// The patched value
    	Object value = null;

    	if (patchOperation.isAdd()) {
    		value = patchOperation.getValue().getObject();
    	} else if (patchOperation.isRemove()) {
            if (patchOperation.getValue().isNull()) {
                if (before.get(field) != null) {
                    value = before.get(field).getObject();
                }
            } else {
                value = patchOperation.getValue().getObject();
            }
    	} else {
        	// If the patch operation is replace or increment, update the value
        	JsonValue beforeValue = before.get(field);
        	if (beforeValue != null && !beforeValue.isNull()) {
        		patchedContent.put(field, before.get(field).getObject());
        	}
        	// Apply the patch operations to an object which contains only the attributes being patched
        	ResourceUtil.applyPatchOperation(patchOperation, patchedContent);
    		value = patchedContent.get(field).getObject();
        }

    	// The String representation of the field, with the leading slash trimmed
    	String fieldName = patchOperation.getField().get(0);

        // Build up the map of patched Attributes
        for (AttributeInfoHelper attributeInfo : attributes) {
            // Get the attribute's nativeName and check if it is on of the attributes to patch
            String attributeName = attributeInfo.getAttributeInfo().getName();
            if (fieldName.equals(attributeName)) {
            	result = attributeInfo.build(value, cryptoService);
            }
        }

        // Check if any of the attributes to patch are invalid/unsupported
        checkForInvalidAttribute(fieldName);

        if (logger.isTraceEnabled()) {
            ConnectorObjectBuilder builder = new ConnectorObjectBuilder().addAttribute(result);
            builder.setName("***");
            builder.setUid("***");
            logger.trace("Patch ConnectorObject: {}", SerializerUtil.serializeXmlObject(builder.build(), false));
        }
        
        return result;
    }

    // ensure all attributes specified in the data are present in the target schema
    private void checkForInvalidAttributes(Set<String> keys) throws BadRequestException {
        for (String requestKey : keys) {
        	checkForInvalidAttribute(requestKey);
        }
    }

    // ensure all attributes specified in the data are present in the target schema
    private void checkForInvalidAttribute(String key) throws BadRequestException {
    	if (!key.startsWith("_")) {
    		boolean found = false;
    		for (AttributeInfoHelper attributeInfo : attributes) {
    			if (attributeInfo.getName().equals(key)) {
    				found = true;
    				break;
    			}
    		}
    		if (!found) {
    			throw new BadRequestException("Target does not support attribute " + key);
    		}
    	}
    }

    /**
     * @param operation
     * @param name
     * @param source
     * @param cryptoService
     * @return
     * @throws ResourceException if ID value can not be determined from the {@code source}
     */
    public ConnectorObject build(Class<? extends APIOperation> operation, String name, JsonValue source, CryptoService cryptoService) throws Exception {
        String nameValue = name;

        if (null == nameValue) {
            JsonValue o = source.get(ResourceResponse.FIELD_CONTENT_ID);
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

    public ResourceResponse build(ConnectorObject source, CryptoService cryptoService) throws IOException, JsonCryptoException {
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
        result.put(ResourceResponse.FIELD_CONTENT_ID, /*Id.escapeUid(*/uid.getUidValue()/*)*/);
        if (null != uid.getRevision()) {
            //System supports Revision
            result.put(ResourceResponse.FIELD_CONTENT_REVISION, uid.getRevision());
        }
        return Responses.newResourceResponse(uid.getUidValue(), uid.getRevision(), result);
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
