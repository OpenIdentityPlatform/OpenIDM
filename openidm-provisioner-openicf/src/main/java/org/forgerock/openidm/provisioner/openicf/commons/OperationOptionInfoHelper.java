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
 *
 * $Id$
 */

package org.forgerock.openidm.provisioner.openicf.commons;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.schema.validator.Constants;
import org.forgerock.json.schema.validator.exceptions.SchemaException;
import org.identityconnectors.framework.common.objects.OperationOptionInfo;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;

import java.io.IOException;
import java.util.*;

import static org.forgerock.json.schema.validator.Constants.*;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class OperationOptionInfoHelper {


    public static final String OPERATION_OPTION_DENIED = "denied";
    public static final String OPERATION_OPTION_ON_DENY = "onDeny";
    public static final String OPERATION_OPTION_SUPPORTEDOBJECTTYPES = "supportedObjectTypes";
    public static final String OPERATION_OPTION_OPERATION_OPTION_INFO = "operationOptionInfo";


    public static enum OnDenyAction {
        THROW_EXCEPTION,
        DO_NOTHING
    }

    private boolean denied;
    private OnDenyAction onDeny;
    private Set<String> supportedObjectTypes = null;
    private final Set<AttributeInfoHelper> attributes;

    public OperationOptionInfoHelper() {
        denied = false;
        onDeny = OnDenyAction.DO_NOTHING;
        attributes = Collections.emptySet();
    }

    public OperationOptionInfoHelper(JsonValue configuration) throws JsonValueException, SchemaException {
        denied = configuration.get(OPERATION_OPTION_DENIED).defaultTo(false).asBoolean();
        JsonValue onDenyValue = configuration.get(OPERATION_OPTION_ON_DENY);
        onDeny = onDenyValue.isNull() ? OperationOptionInfoHelper.OnDenyAction.DO_NOTHING : onDenyValue.asEnum(OnDenyAction.class);
        JsonValue operationOptionInfo = configuration.get(OPERATION_OPTION_OPERATION_OPTION_INFO);
        if (operationOptionInfo.isMap()) {
            attributes = new HashSet<AttributeInfoHelper>();
            for (Map.Entry<String, Object> entry : operationOptionInfo.get(Constants.PROPERTIES).expect(Map.class).asMap().entrySet()) {
                attributes.add(new AttributeInfoHelper(entry.getKey(), true, (Map<String, Object>) entry.getValue()));
            }
        } else {
            attributes = Collections.emptySet();
        }
        JsonValue supportedObjectTypesValue = configuration.get(OPERATION_OPTION_SUPPORTEDOBJECTTYPES);
        if (supportedObjectTypesValue.isList()) {
            List<Object> source = supportedObjectTypesValue.asList();
            this.supportedObjectTypes = new HashSet<String>(source.size());
            for (Object o : source) {
                if (o instanceof String) {
                    this.supportedObjectTypes.add((String) o);
                }
            }
        }

    }

    public OperationOptionInfoHelper(JsonValue configuration, OperationOptionInfoHelper globalOption) throws JsonValueException, SchemaException {
        denied = configuration.get(OPERATION_OPTION_DENIED).defaultTo(globalOption.isDenied()).asBoolean();
        JsonValue onDenyValue = configuration.get(OPERATION_OPTION_ON_DENY);
        onDeny = onDenyValue.isNull() ? globalOption.getOnDeny() : onDenyValue.asEnum(OnDenyAction.class);
        attributes = new HashSet<AttributeInfoHelper>(globalOption.getAttributes());
        JsonValue operationOptionInfo = configuration.get(OPERATION_OPTION_OPERATION_OPTION_INFO);
        if (operationOptionInfo.isMap()) {
            for (Map.Entry<String, Object> entry : operationOptionInfo.get(Constants.PROPERTIES).expect(Map.class).asMap().entrySet()) {
                attributes.add(new AttributeInfoHelper(entry.getKey(), true, (Map<String, Object>) entry.getValue()));
            }
        }
        JsonValue supportedObjectTypesValue = configuration.get(OPERATION_OPTION_SUPPORTEDOBJECTTYPES);
        if (supportedObjectTypesValue.isList()) {
            List<Object> source = supportedObjectTypesValue.asList();
            this.supportedObjectTypes = new HashSet<String>(source.size());
            for (Object o : source) {
                if (o instanceof String) {
                    this.supportedObjectTypes.add((String) o);
                }
            }
        } else {
            this.supportedObjectTypes = globalOption.getSupportedObjectTypes();
        }
    }

    public boolean isDenied() {
        return denied;
    }

    public OnDenyAction getOnDeny() {
        return onDeny;
    }

    Set<AttributeInfoHelper> getAttributes() {
        return attributes;
    }

    public Set<String> getSupportedObjectTypes() {
        return null != supportedObjectTypes ? Collections.unmodifiableSet(supportedObjectTypes) : null;
    }

    public OperationOptionsBuilder build(JsonValue source, ObjectClassInfoHelper objectClassInfoHelper) throws IOException {
        OperationOptionsBuilder builder = new OperationOptionsBuilder();
        if (null != source) {
            //Get the explicit options object if defied.
            Object o = source.get("_options");
            Map<String, Object> options = null;
            if (o instanceof Map) {
                options = (Map<String, Object>) o;
            }


            for (AttributeInfoHelper helper : attributes) {
                helper.build(builder, source.get(helper.getName()));
            }
        } else {
            builder.setAttributesToGet(objectClassInfoHelper.getAttributesReturnedByDefault());
        }
        return builder;
    }

    public static Map<String, Object> build(Set<OperationOptionInfo> operationOptionInfoSet) {
        return build(operationOptionInfoSet, false, OnDenyAction.DO_NOTHING);
    }

    public static Map<String, Object> build(Set<OperationOptionInfo> operationOptionInfoSet, boolean denied, OnDenyAction onDeny) {
        Map<String, Object> operationOptionInfoHelper = new LinkedHashMap<String, Object>(12);
        operationOptionInfoHelper.put(OPERATION_OPTION_DENIED, denied);
        operationOptionInfoHelper.put(OPERATION_OPTION_ON_DENY, onDeny.name());
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
