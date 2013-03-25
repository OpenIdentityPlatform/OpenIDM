/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.quartz;

import java.io.Serializable;
import java.util.HashMap;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.SerializationUtils;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.quartz.JobPersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public class QuartzUtil {
    /**
     * Setup logging for the {@link QuartzUtil}.
     */
    final static Logger logger = LoggerFactory.getLogger(QuartzUtil.class);

    /**
     * Converts a serializable object into a String.
     * 
     * @param object
     *            the object to serialize.
     * @return a string representation of the serialized object.
     * @throws Exception
     */
    public static String serialize(Serializable object) throws JobPersistenceException {
        try {
            return Base64.encodeBase64String(SerializationUtils.serialize(object));
        } catch (Exception e) {
            logger.error("Failed to serialize object", e);
            throw new JobPersistenceException(e.getMessage());
        }
    }

    /**
     * Converts a String representation of a serialized object back into an
     * object.
     * 
     * @param str
     *            the representation of the serialized object
     * @return the deserialized object
     * @throws Exception
     */
    public static Object deserialize(String str) throws JobPersistenceException {
        try {
            return SerializationUtils.deserialize(Base64.decodeBase64(str));
        } catch (Exception e) {
            logger.error("Failed to deserialize string", e);
            throw new JobPersistenceException(e.getMessage());
        }
    }

    public static JsonValue fromSerializable(Serializable object) throws JobPersistenceException {
        JsonValue content = new JsonValue(new HashMap<String, Object>(1));
        content.put("serialized", serialize(object));
        return content;
    }

    public <T extends Serializable> T fromJsonValue(JsonValue value, Class<T> type)
            throws JobPersistenceException {
        Object object = deserialize(value.get("serialized").required().asString());
        if (!type.isInstance(object)) {
            throw new JsonValueException(value, "Expecting a " + type.getName());
        }
        return (T) object;
    }
}
