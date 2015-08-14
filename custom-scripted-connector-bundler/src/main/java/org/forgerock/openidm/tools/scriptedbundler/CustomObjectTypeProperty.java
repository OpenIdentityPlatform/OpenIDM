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

import java.util.*;

/**
 * A single connector objectType property, contained in {@link CustomObjectType}.
 *
 * Given a JSON configuration such as:
 * <pre><blockquote>
 *      "properties" : [
 *          {
 *              "name" : "name",
 *              "type" : "string",
 *              "required" : true,
 *              "nativeName" : "__NAME__",
 *              "nativeType" : "string",
 *              "flags" : [
 *                  "NOT_READABLE",
 *                  "NOT_RETURNED_BY_DEFAULT"
 *              ],
 *              "items" : [
 *                  {
 *                      "type" : "object",
 *                      "properties" : [{
 *                          "name" : "uid",
 *                          "type" : "string"
 *                      }]
 *                  }
 *              ]
 *          },{
 *              ...
 *          }
 *      ]
 * </blockquote></pre>
 * this object represents a single properties array element.
 */
public class CustomObjectTypeProperty extends CustomBaseObject {
    /**
     * Supported types for configuration properties.
     */
    private static final Map<String,String> PROPERTY_TYPES = new HashMap<String,String>() {{
        put("string", "String.class");
        put("array", "Object.class");
        put("boolean", "Boolean.class");
        put("integer", "Integer.class");
        put("object", "Object.class");
    }};

    private String name;
    private String type;
    private Boolean required = false;
    private String nativeName;
    private String nativeType;
    private List<CustomObjectTypePropertyFlag> flags = new ArrayList<CustomObjectTypePropertyFlag>();
    private CustomObjectTypePropertyItems items;

    /**
     * Return the name of this property.
     *
     * @return
     */
    public String getName() {
        return name;
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
     * Return the type of this property.
     *
     * @return
     */
    public String getType() { return type; }

    /**
     * Set the type of this property. Enhanced setter to enforce supported property types.
     *
     * @param type
     */
    public void setType(String type) {
        type = type.toLowerCase();
        if (PROPERTY_TYPES.keySet().contains(type)) {
            this.type = type;
        } else {
            throw new UnsupportedOperationException("objectType property type '" + type + "' is not supported");
        }
    }

    /**
     * Template utility method to return a formatted string representing the flags on an object.
     *
     * @return
     */
    @JsonIgnore
    public String getObjectDescriptor() {
        if (type == null) {
            return "UNKNOWN_TYPE";
        }
        return PROPERTY_TYPES.get(type) + (required
                ? "," + (required ? " REQUIRED" : "")
                : "");
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
     * Return the native name of this property.
     *
     * @return
     */
    public String getNativeName() {
        return nativeName;
    }

    /**
     * Set the native name of this property.
     *
     * @param nativeName
     */
    public void setNativeName(String nativeName) {
        this.nativeName = nativeName;
    }

    /**
     * Return the native type of this property.
     *
     * @return
     */
    public String getNativeType() {
        return nativeType;
    }

    /**
     * Enhanced setter to enforce supported native types
     *
     * @param nativeType
     */
    public void setNativeType(String nativeType) {
        this.nativeType = nativeType;
    }

    /**
     * Return the flags for this property.
     *
     * @return
     */
    public List<CustomObjectTypePropertyFlag> getFlags() {
        return flagLast(flags);
    }

    /**
     * Set the flags for this property.
     *
     * @param flags
     */
    public void setFlags(List<CustomObjectTypePropertyFlag> flags) {
        this.flags.clear();
        this.flags.addAll(flagLast(flags));
    }

    /**
     * Return whether this property has flags.
     *
     * @return
     */
    @JsonIgnore
    public boolean getHasFlags() {
        return !flags.isEmpty();
    }

    /**
     * Return the items for this property.
     *
     * @return
     */
    public CustomObjectTypePropertyItems getItems() {
        return items;
    }

    /**
     * Set the items for this property.
     *
     * @param items
     */
    public void setItems(CustomObjectTypePropertyItems items) {
        this.items = items;
    }

    /**
     * Return whether this property has items.
     *
     * @return
     */
    @JsonIgnore
    public boolean getHasItems() {
        return items != null;
    }
}
