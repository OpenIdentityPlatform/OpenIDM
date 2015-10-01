/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openidm.util;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;

/**
 * Utilities methods and fields for managed object relationships.
 */
public class RelationshipUtil {
    
    /**
     * The id of the resource that the relationship references
     */
    public static JsonPointer REFERENCE_ID = new JsonPointer("_ref");
    
    /**
     * The map of properties associated with the relationship
     */
    public static JsonPointer REFERENCE_PROPERTIES = new JsonPointer("_refProperties");

    /**
     * Returns true if the supplied {@link JsonValue} is an instance of a relationship, meaning that is is a {@link Map} 
     * and it contains and entry for "_ref".
     * 
     * @param value a {@link JsonValue} instance
     * @return true if the value is an relationship, false otherwise.
     */
    public static boolean isRelationship(JsonValue value) {
        return value.isMap() && value.keys().contains(REFERENCE_ID.leaf());
    }
}
