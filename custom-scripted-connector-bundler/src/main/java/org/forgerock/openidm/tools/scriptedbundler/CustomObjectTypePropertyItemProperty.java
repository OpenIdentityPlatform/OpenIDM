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

import java.util.Arrays;
import java.util.List;

/**
 * A single property of an objectType item, contained in {@link CustomObjectTypePropertyItems}.
 *
 * Represented in JSON as:
 * <pre><blockquote>
 *      {
 *          "name" : "uid",
 *          "type" : "string"
 *      }
 * </blockquote></pre>
 */
public class CustomObjectTypePropertyItemProperty extends CustomBaseObject {
    /**
     * Supported types for configuration properties.
     */
    private static final List<String> PROPERTY_TYPES = Arrays.asList("string");

    private String name;
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
    public String getType() {
        return type;
    }

    /**
     * Set the type of this property.  Enhanced setter to enforce supported property types.
     *
     * @param type
     */
    public void setType(String type) {
        type = type.toLowerCase();
        if (PROPERTY_TYPES.contains(type)) {
            this.type = type;
        } else {
            throw new UnsupportedOperationException("objectType property item property type '" + type + "' is not supported");
        }
    }
}
