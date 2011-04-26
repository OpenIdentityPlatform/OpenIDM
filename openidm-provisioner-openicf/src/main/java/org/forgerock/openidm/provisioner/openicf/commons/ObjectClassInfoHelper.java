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
import org.identityconnectors.framework.api.operations.APIOperation;
import org.identityconnectors.framework.api.operations.CreateApiOp;
import org.identityconnectors.framework.api.operations.UpdateApiOp;
import org.identityconnectors.framework.common.objects.*;

import java.io.IOException;
import java.util.*;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class ObjectClassInfoHelper {
    private final Set<AttributeInfoHelper> attributes;
    private final ObjectClass objectClass;

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
            attributes.add(new AttributeInfoHelper(e.getKey(), (Map<String, Object>) e.getValue()));
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


    public ConnectorObject build(Class<? extends APIOperation> operation, Map<String, Object> source) throws IOException {
        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
        builder.setObjectClass(objectClass);

        if (CreateApiOp.class.isAssignableFrom(operation)) {
            for (AttributeInfoHelper attributeInfo : attributes) {
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
                if (attributeInfo.getAttributeInfo().isUpdateable()) {
                    Object v = source.get(attributeInfo.getName());
                    builder.addAttribute(attributeInfo.build(v));
                }
            }
        } else {
            for (AttributeInfoHelper attributeInfo : attributes) {
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
        return result;


    }

    public Map<String, Object> build(SyncDelta source) throws IOException {
        Map<String, Object> result = new LinkedHashMap<String, Object>(5);
        //@TODO Token serialization problem.
        result.put("token", source.getToken().getValue());
        result.put("deltaType", source.getDeltaType().name());
        result.put("_id", source.getUid().getUidValue());
        if (null != source.getPreviousUid()) {
            result.put("_previousid", source.getPreviousUid().getUidValue());
        }
        result.put("object", build(source.getObject()));
        return result;
    }

    public void resetUid(Uid uid,  Map<String, Object> target) {

    }
}
