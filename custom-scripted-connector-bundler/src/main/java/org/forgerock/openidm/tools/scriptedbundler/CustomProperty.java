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
 * A single connector configuration property, contained in {@link CustomConfiguration}.
 *
 * The JSON configuration might contain something like the following:
 * <pre><blockquote>
 * "properties" : [
 *      {
 *          "order" : 0,
 *          "type" : "String",
 *          "name" : "FirstProperty",
 *          "value" : "firstValue",
 *          "required" : true,
 *          "confidential" : false,
 *          "displayMessage" : "This is my first property",
 *          "helpMessage" : "This should be a String value",
 *          "group" : "default"
 *      }, {
 *          "order" : 1,
 *          "type" : "Double",
 *          "name" : "SecondProperty",
 *          "value" : 1.234,
 *          "required" : false,
 *          "confidential" : false,
 *          "displayMessage" : "This is my second property",
 *          "helpMessage" : "This should be a Double value",
 *          "group" : "default"
 *      }
 *  ]
 * </blockquote></pre>
 *
 * This object represents a single property in the list above.  These properties define the connector's
 * configuration properties and are the data source for generating the code to support connector metadata
 * used by the ICF consumer.
 */
public class CustomProperty extends CustomBaseObject {
    /**
     * Supported types for configuration properties.
     */
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

    private Integer order;
    private String type;
    private String name;
    private Object value;
    private Boolean required;
    private Boolean confidential;
    private String displayMessage;
    private String helpMessage;
    private String group;

    /**
     * Return the order of this property.
     *
     * @return
     */
    public Integer getOrder() {
        return order;
    }

    /**
     * Set the order of this property.
     *
     * @param order
     */
    public void setOrder(Integer order) {
        this.order = order;
    }

    /**
     * Return the type of this property. Enhanced getter to enforce supported types
     *
     * @return
     */
    public String getType() {
        return isConfidential() && type.equals("String")
                ? "GuardedString"
                : type.substring(0,1).toUpperCase() + type.substring(1);
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
     * Return whether this property is required.
     *
     * @return
     */
    public Boolean isRequired() {
        return required;
    }

    /**
     * Set whether this property is required.
     *
     * @param required
     */
    public void setRequired(Boolean required) {
        this.required = required;
    }

    /**
     * Return whether this property is confidential.
     *
     * @return
     */
    public Boolean isConfidential() {
        return confidential;
    }

    /**
     * Set whether this property is confidential.
     *
     * @param confidential
     */
    public void setConfidential(Boolean confidential) {
        this.confidential = confidential;
    }

    /**
     * Return the display message for this property.
     *
     * @return
     */
    public String getDisplayMessage() {
        return displayMessage;
    }

    /**
     * Set the display message for this property.
     *
     * @param displayMessage
     */
    public void setDisplayMessage(String displayMessage) {
        this.displayMessage = displayMessage;
    }

    /**
     * Return the help message for this property.
     *
     * @return
     */
    public String getHelpMessage() {
        return helpMessage;
    }

    /**
     * Set the help message for this property.
     *
     * @param helpMessage
     */
    public void setHelpMessage(String helpMessage) {
        this.helpMessage = helpMessage;
    }

    /**
     * Return the display group for this property.
     *
     * @return
     */
    public String getGroup() {
        return group;
    }

    /**
     * Set the display group for this property.
     *
     * @param group
     */
    public void setGroup(String group) {
        this.group = group;
    }
}
