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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A single connector configuration objectType, contained in {@link CustomConfiguration}.
 *
 * Given a JSON configuration such as:
 * <pre><blockquote>
 *      "objectTypes" : [
 *          {
 *              "name" : "group",
 *              "id" : "__GROUP__",
 *              "type" : "object",
 *              "nativeType" : "__GROUP__",
 *              "objectClass" : "ObjectClass.GROUP_NAME",
 *              "flags" : [
 *                  "NOT_READABLE",
 *                  "NOT_RETURNED_BY_DEFAULT"
 *              ],
 *              "properties" : [
 *                  {
 *                      "name" : "name",
 *                      "type" : "string",
 *                      "required" : true,
 *                      "nativeName" : "__NAME__",
 *                      "nativeType" : "string"
 *                      "items" : [
 *                          {
 *                              "type" : "object",
 *                              "properties" : [{
 *                                  "name" : "uid",
 *                                  "type" : "string"
 *                              }]
 *                          }
 *                      ]
 *                  },{
 *                      ...
 *                  }
 *              ]
 *          }
 *      ]
 * </blockquote></pre>
 * this object represents a single objectType array element.
 */
public class CustomObjectType extends CustomBaseObject {

    /**
     * Supported types for objectTypes
     */
    private static final List<String> OBJECT_TYPES = Arrays.asList("object");

    private String name;
    private String id;
    private String type;
    private String nativeType;
    private String objectClass;

    // A list of properties for this object type
    private List<CustomObjectTypeProperty> properties = new ArrayList<CustomObjectTypeProperty>();

    /**
     * Return the name of this object type.
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of this object type.
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Return the ID of this object type.
     *
     * @return
     */
    public String getId() { return id; }

    /**
     * Set the ID of this object type.
     *
     * @param id
     */
    public void setId(String id) { this.id = id; }

    /**
     * Return the type of this object type.
     *
     * @return
     */
    public String getType() { return type; }

    /**
     * Set the type of this object type. Enhanced setter to enforce supported object types.
     *
     * @param type
     */
    public void setType(String type) {
        type = type.toLowerCase();
        if (OBJECT_TYPES.contains(type)) {
            this.type = type;
        } else {
            throw new UnsupportedOperationException("objectType type '" + type + "' is not supported");
        }
    }

    /**
     * Return the native type of this object type.
     *
     * @return
     */
    public String getNativeType() {
        return nativeType;
    }

    /**
     * Set the native type of this object type.
     *
     * @param nativeType
     */
    public void setNativeType(String nativeType) {
        this.nativeType = nativeType;
    }

    /**
     * Return the object class of this object type.
     *
     * @return
     */
    public String getObjectClass() {
        return objectClass;
    }

    /**
     * Set the object class of this object type.
     *
     * @param nativeObjectClass
     */
    public void setObjectClass(String nativeObjectClass) {
        this.objectClass = nativeObjectClass;
    }

    /**
     * Return the properties of this object type.
     *
     * @return
     */
    public List<CustomObjectTypeProperty> getProperties() {
        return flagLast(properties);
    }

    /**
     * Set the properties of this object type.
     *
     * @param properties
     */
    public void setProperties(List<CustomObjectTypeProperty> properties) {
        this.properties.clear();
        this.properties.addAll(flagLast(properties));
    }
}
