/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.workflow.activiti.impl;

import org.activiti.engine.impl.variable.ValueFields;
import org.activiti.engine.impl.variable.VariableType;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * @version $Revision$ $Date$
 */
public class JsonValueType implements VariableType {

    private static final ObjectMapper mapper = new ObjectMapper();

    public String getTypeName() {
        return "jsonvalue";
    }

    public boolean isCachable() {
        return false;
    }

    public Object getValue(ValueFields valueFields) {
        String value = valueFields.getTextValue();
        if (null != value) {
            try {
                Object jsonValue = mapper.readValue(value, Object.class);
                if (jsonValue instanceof Map) {
                    return new JsonValue(((Map) jsonValue).get("value"), new JsonPointer((String) ((Map) jsonValue).get("pointer")));
                } else if (null == jsonValue) {
                    //IOException when setValue
                    return new JsonValue(null);
                }
            } catch (IOException e) {
                // Should not happen
            }
        }
        return null;
    }

    public void setValue(Object value, ValueFields valueFields) {
        if (null == value) {
            valueFields.setTextValue(null);
        } else {
            try {
                Map<String, Object> jsonValue = new HashMap<String, Object>(2);
                jsonValue.put("pointer", ((JsonValue) value).getPointer().toString());
                jsonValue.put("value", ((JsonValue) value).getObject());
                StringWriter writer = new StringWriter();
                mapper.writeValue(writer, jsonValue);
                valueFields.setTextValue(writer.toString());
            } catch (IOException e) {
                valueFields.setTextValue("null");
            }
        }
    }

    public boolean isAbleToStore(Object value) {
        if (value == null) {
            return true;
        }
        return JsonValue.class.isAssignableFrom(value.getClass());
    }
}
