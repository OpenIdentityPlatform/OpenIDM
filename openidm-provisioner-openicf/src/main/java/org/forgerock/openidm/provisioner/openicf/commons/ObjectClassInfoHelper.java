/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
 * $Id$
 */
package org.forgerock.openidm.provisioner.openicf.commons;

import org.forgerock.json.schema.validator.Constants;
import org.forgerock.json.schema.validator.exceptions.SchemaException;
import org.forgerock.openidm.objset.PreconditionFailedException;
import org.forgerock.openidm.provisioner.Id;
import org.identityconnectors.framework.api.operations.APIOperation;
import org.identityconnectors.framework.api.operations.CreateApiOp;
import org.identityconnectors.framework.api.operations.UpdateApiOp;
import org.identityconnectors.framework.common.objects.*;

import java.io.IOException;
import java.util.*;
import java.util.jar.Attributes;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class ObjectClassInfoHelper {
    private final Set<AttributeInfoHelper> attributes;
    private final ObjectClass objectClass;
    private String nameAttribute = null;

    /**
     * Create a custom object class.
     *
     * @param schema string representation for the name of the object class.
     * @throws IllegalArgumentException when objectClass is null
     */
    public ObjectClassInfoHelper(Map<String, Object> schema) throws SchemaException {
        objectClass = new ObjectClass((String) schema.get(ConnectorUtil.OPENICF_OBJECT_CLASS));
        Map<String, Object> properties = (Map<String, Object>) schema.get(Constants.PROPERTIES);
        attributes = new HashSet<AttributeInfoHelper>(properties.size());
        for (Map.Entry<String, Object> e : properties.entrySet()) {
            AttributeInfoHelper helper = new AttributeInfoHelper(e.getKey(), false, (Map<String, Object>) e.getValue());
            if (helper.getAttributeInfo().getName().equals(Name.NAME)) {
                nameAttribute = e.getKey();
            }
            attributes.add(helper);
        }
    }

    /**
     * Get a new instance of the {@link org.identityconnectors.framework.common.objects.ObjectClass} for this schema.
     *
     * @return new instance of {@link org.identityconnectors.framework.common.objects.ObjectClass}
     */
    public ObjectClass getObjectClass() {
        return objectClass;
    }


//    public Set<AttributeInfoHelper> getAttributes(Class<? extends APIOperation> operation) {
//        return Collections.unmodifiableSet(attributes);
//    }

    /**
     * @param operation
     * @param name
     * @param source
     * @return
     * @throws PreconditionFailedException if ID value can not be determined from the {@code source}
     */
    public ConnectorObject build(Class<? extends APIOperation> operation, String name, Map<String, Object> source) throws Exception {
        String nameValue = null;

        if (null != name) {
            nameValue = name;
        } else if (null != source.get("_id")) {
            Id id = new Id((String) source.get("_id"));
            nameValue = id.getLocalId();
        } else if (null != source.get(nameAttribute)) {
            nameValue = source.get(nameAttribute).toString();
        }

        if (null == nameValue) {
            throw new PreconditionFailedException("Required localId attribute is missing");
        }

        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
        builder.setObjectClass(objectClass);
        builder.setUid(nameValue);
        builder.setName(nameValue);
        Set<String> keySet = source.keySet();
        if (CreateApiOp.class.isAssignableFrom(operation)) {
            for (AttributeInfoHelper attributeInfo : attributes) {
                if (Name.NAME.equals(attributeInfo.getName()) || Uid.NAME.equals(attributeInfo.getName()) ||
                        !keySet.contains(attributeInfo.getName())) {
                    continue;
                }
                if (attributeInfo.getAttributeInfo().isCreateable()) {
                    Object v = source.get(attributeInfo.getName());
                    if (null == v && attributeInfo.getAttributeInfo().isRequired()) {
                        throw new IllegalArgumentException("Required value is null");
                    }
                    builder.addAttribute(attributeInfo.build(v));
                }
            }
        } else if (UpdateApiOp.class.isAssignableFrom(operation)) {
            for (AttributeInfoHelper attributeInfo : attributes) {
                if (Name.NAME.equals(attributeInfo.getName()) || Uid.NAME.equals(attributeInfo.getName()) ||
                        !keySet.contains(attributeInfo.getName())) {
                    continue;
                }
                if (attributeInfo.getAttributeInfo().isUpdateable()) {
                    Object v = source.get(attributeInfo.getName());
                    builder.addAttribute(attributeInfo.build(v));
                }
            }
        } else {
            for (AttributeInfoHelper attributeInfo : attributes) {
                 if (Name.NAME.equals(attributeInfo.getName()) || Uid.NAME.equals(attributeInfo.getName()) ||
                        !keySet.contains(attributeInfo.getName())) {
                    continue;
                }
                Object v = source.get(attributeInfo.getName());
                builder.addAttribute(attributeInfo.build(v));
            }
        }
        return builder.build();
    }

    public Map<String, Object> build(ConnectorObject source) throws IOException {
        if (null == source) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>(source.getAttributes().size());
        for (AttributeInfoHelper attributeInfo : attributes) {
            Attribute attribute = source.getAttributeByName(attributeInfo.getAttributeInfo().getName());
            if (null != attribute) {
                result.put(attributeInfo.getName(), attributeInfo.build(attribute));
            }
        }
        result.put("_id", Id.escapeUid(source.getUid().getUidValue()));
        return result;
    }

    public Attribute build(String attributeName, Object source) throws Exception {
        for (AttributeInfoHelper attributeInfoHelper : attributes) {
            if (attributeInfoHelper.getName().equals(attributeName)) {
                return attributeInfoHelper.build(source);
            }
        }
        if (source instanceof Collection) {
            return AttributeBuilder.build(attributeName, (Collection) source);
        } else {
            return AttributeBuilder.build(attributeName, source);
        }
    }
}
