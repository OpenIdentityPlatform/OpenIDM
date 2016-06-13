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

import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.schema.validator.Constants;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionInfo;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;

import java.io.IOException;
import java.util.*;

import static org.forgerock.json.schema.validator.Constants.*;

/**
 *
 */
public class OperationOptionInfoHelper {


    public static final String OPERATION_OPTION_DENIED = "denied";
    public static final String OPERATION_OPTION_SUPPORTEDOBJECTTYPES = "supportedObjectTypes";
    public static final String OPERATION_OPTION_OPERATION_OPTION_INFO = "operationOptionInfo";


    public enum OnActionPolicy {
        THROW_EXCEPTION,
        ALLOW;
    }

    private OnActionPolicy onActionPolicy;
    private final Set<String> supportedObjectTypes;
    private final Set<AttributeInfoHelper> attributes;

    public OperationOptionInfoHelper() {
        onActionPolicy = OnActionPolicy.ALLOW;
        attributes = Collections.emptySet();
        supportedObjectTypes = null;
    }

    public OperationOptionInfoHelper(JsonValue configuration) throws JsonValueException {
        onActionPolicy =  configuration.get(OPERATION_OPTION_DENIED).defaultTo(false).asBoolean()
            ? OnActionPolicy.THROW_EXCEPTION
            : OnActionPolicy.ALLOW;

        JsonValue operationOptionInfo = configuration.get(OPERATION_OPTION_OPERATION_OPTION_INFO);
        if (operationOptionInfo.isMap()) {
            JsonValue properties = operationOptionInfo.get(Constants.PROPERTIES).expect(Map.class);
            attributes = new HashSet<AttributeInfoHelper>(properties.size());
            for (String entry : properties.keys()) {
                attributes.add(new AttributeInfoHelper(entry, true, properties.get(entry)));
            }
        } else {
            attributes = Collections.emptySet();
        }
        JsonValue supportedObjectTypesValue = configuration.get(OPERATION_OPTION_SUPPORTEDOBJECTTYPES);
        if (supportedObjectTypesValue.isList()) {
            List<Object> source = supportedObjectTypesValue.asList();
            Set<String> objectTypes = new HashSet<String>(source.size());
            for (Object o : source) {
                if (o instanceof String) {
                    objectTypes.add((String) o);
                }
            }
            this.supportedObjectTypes = Collections.unmodifiableSet(objectTypes);
        } else {
            //Support all object types
            this.supportedObjectTypes = null;
        }
    }

    public OperationOptionInfoHelper(JsonValue configuration, OperationOptionInfoHelper globalOption) throws JsonValueException {
        onActionPolicy =  configuration.get(OPERATION_OPTION_DENIED).defaultTo(globalOption.getOnActionPolicy().equals(OnActionPolicy.THROW_EXCEPTION)).asBoolean()
            ? OnActionPolicy.THROW_EXCEPTION
            : OnActionPolicy.ALLOW;
        attributes = new HashSet<AttributeInfoHelper>(globalOption.getAttributes());
        JsonValue operationOptionInfo = configuration.get(OPERATION_OPTION_OPERATION_OPTION_INFO);
        if (operationOptionInfo.isMap()) {
            JsonValue properties = operationOptionInfo.get(Constants.PROPERTIES).expect(Map.class);
            for (String entry : properties.keys()) {
                attributes.add(new AttributeInfoHelper(entry, true, properties.get(entry)));
            }
        }
        JsonValue supportedObjectTypesValue = configuration.get(OPERATION_OPTION_SUPPORTEDOBJECTTYPES);
        if (supportedObjectTypesValue.isList()) {
            List<Object> source = supportedObjectTypesValue.asList();
            Set<String> objectTypes = new HashSet<String>(source.size());
            for (Object o : source) {
                if (o instanceof String) {
                    objectTypes.add((String) o);
                }
            }
            this.supportedObjectTypes = Collections.unmodifiableSet(objectTypes);
        } else {
            this.supportedObjectTypes = globalOption.getSupportedObjectTypes();
        }
    }

    public boolean isDenied() {
        return !OnActionPolicy.ALLOW.equals(getOnActionPolicy());
    }

    public OnActionPolicy getOnActionPolicy() {
        return onActionPolicy;
    }

    Set<AttributeInfoHelper> getAttributes() {
        return attributes;
    }

    public Set<String> getSupportedObjectTypes() {
        return null != supportedObjectTypes ? Collections.unmodifiableSet(supportedObjectTypes) : null;
    }

    public OperationOptionsBuilder build(JsonValue source, ObjectClassInfoHelper objectClassInfoHelper) throws IOException {
        OperationOptionsBuilder builder = new OperationOptionsBuilder();
        if (null != source && !source.isNull()) {
            for (AttributeInfoHelper helper : attributes) {
                helper.build(builder, source.get(helper.getName()));
            }
        }
        
        // Ensure that the default attributes are set if not already explicitly
        // specified within the options object.
        if (!builder.getOptions().containsKey(OperationOptions.OP_ATTRIBUTES_TO_GET)) {
            builder.setAttributesToGet(objectClassInfoHelper.getAttributesReturnedByDefault());
        }
        return builder;
    }

    public static Map<String, Object> build(Set<OperationOptionInfo> operationOptionInfoSet) {
        return build(operationOptionInfoSet, null);
    }

    public static Map<String, Object> build(Set<OperationOptionInfo> operationOptionInfoSet, OnActionPolicy onActionPolicy) {
        Map<String, Object> operationOptionInfoHelper = new LinkedHashMap<String, Object>(12);
        if (null != onActionPolicy && OnActionPolicy.THROW_EXCEPTION.equals(onActionPolicy)){
        operationOptionInfoHelper.put(OPERATION_OPTION_DENIED, true);
        }
        if (null != operationOptionInfoSet) {
            Map<String, Object> schema = new LinkedHashMap<String, Object>();
            operationOptionInfoHelper.put(OPERATION_OPTION_OPERATION_OPTION_INFO, schema);
            schema.put(SCHEMA, JSON_SCHEMA_DRAFT03);
            schema.put(ID, "FIX_ME");
            schema.put(TYPE, TYPE_OBJECT);

            Map<String, Object> properties = new LinkedHashMap<String, Object>(operationOptionInfoSet.size());
            schema.put(PROPERTIES, properties);
            for (OperationOptionInfo ooi : operationOptionInfoSet) {
                properties.put(ooi.getName(), ConnectorUtil.getOperationOptionInfoMap(ooi));
            }
        }
        return operationOptionInfoHelper;
    }
}
