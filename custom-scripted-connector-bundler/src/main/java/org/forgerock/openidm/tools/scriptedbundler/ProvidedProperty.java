/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.tools.scriptedbundler;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A single connector configuration property, contained in {@link org.forgerock.openidm.tools.scriptedbundler.CustomConfiguration}.
 *
 * The JSON configuration might contain something like the following:
 * <pre><blockquote>
 * "providedProperties" : [
 *      {
 *          "name" : "FirstProperty",
 *          "value" : "firstValue",
 *          "type" : "String"
 *      }, {
 *          "name" : "SecondProperty",
 *          "value" : 1.234,
 *          "type" : "Float"
 *      }
 *  ]
 * </blockquote></pre>
 *
 * This object represents a single property in the list above.  These properties populate values for the properties
 * defined by default in the connector's base configuration class.  For example, the ScriptedSQLConfiguration class
 * provides for a "name" property and a "password" property (among others).  To preset these values or even set a
 * default in the connector's provisioner config a developer would add providedProperties for name and password.
 */
public class ProvidedProperty extends CustomBaseObject {
    private static final List<String> CONFIG_STRING_TYPES = Arrays.asList(
            "string",
            "uri",
            "file",
            "guardedstring",
            "script"
    );
    private static final List<String> CONFIG_CHAR_TYPES = Arrays.asList("character");
    private static final List<String> CONFIG_RAW_TYPES = Arrays.asList(
            "long",
            "double",
            "float",
            "integer",
            "boolean",
            "guardedbytearray"
    );
    private static final List<String> CONFIG_SUPPORTED_TYPES = new ArrayList<String>();
    static {
        CONFIG_SUPPORTED_TYPES.addAll(CONFIG_STRING_TYPES);
        CONFIG_SUPPORTED_TYPES.addAll(CONFIG_CHAR_TYPES);
        CONFIG_SUPPORTED_TYPES.addAll(CONFIG_RAW_TYPES);
    }

    private String name;
    private Object value;
    private String type;

    /**
     * Return the name of this property.
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Template utility method to lower-case the first character of the name attribute.
     *
     * @return
     */
    @JsonIgnore
    public String getNameCamelCase() {
        return name.substring(0,1).toLowerCase() + name.substring(1);
    }

    /**
     * Set the name of this property.
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Return the default value of this property.
     *
     * @return
     */
    public Object getValue() {
        return value;
    }

    /**
     * Set the default value of this property.
     *
     * @param value
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * Helper method for handlebars template, not a configuration parameter.
     *
     * @return
     */
    @JsonIgnore
    public Object getFormattedValue() {
        if (CONFIG_STRING_TYPES.contains(type)) {
            return "\"" + value + "\"";
        } else if (CONFIG_CHAR_TYPES.contains(type)) {
            return "'" + value + "'";
        } else {
            return value;
        }
    }

    /**
     * Return the type of this property. Enhanced getter to enforce supported types
     *
     * @return
     */
    public String getType() {
        return type;
    }

    /**
     * Set the type of this property. Enhanced setter to enforce supported types
     *
     * @param type
     */
    public void setType(String type) {
        type = type.toLowerCase();
        if (CONFIG_SUPPORTED_TYPES.contains(type)) {
            this.type = type;
        } else {
            throw new UnsupportedOperationException("Type '" + type + "' is not supported");
        }
    }
}
