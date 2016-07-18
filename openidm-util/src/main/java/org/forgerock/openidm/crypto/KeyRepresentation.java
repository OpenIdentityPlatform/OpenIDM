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
 * Copyright 2016 ForgeRock AS
 */
package org.forgerock.openidm.crypto;

import static org.forgerock.json.JsonValue.*;

import java.io.StringWriter;
import java.security.Key;
import java.security.PrivateKey;

import javax.crypto.SecretKey;

import org.bouncycastle.openssl.PEMWriter;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.util.encode.Base64;

/**
 * Utilities to represent keys in JsonValue form.
 */
public class KeyRepresentation {

    /**
     * Returns a JsonValue map representing a key
     *
     * @param alias  the certificate alias
     * @param key The key
     * @return a JsonValue map representing the key
     * @throws Exception
     */
    public static JsonValue toJsonValue(String alias, Key key) throws Exception {
        JsonValue content = json(object());
        content.put(ResourceResponse.FIELD_CONTENT_ID, alias);
        if (key instanceof PrivateKey) {
            content.put("privateKey", getKeyMap(key).asMap());
        } else if (key instanceof SecretKey) {
            content.put("secret", getSecretKeyMap(key).asMap());
        }
        return content;
    }

    /**
     * Returns a JsonValue map representing key
     *
     * @param key  The key
     * @return a JsonValue map representing the key
     * @throws Exception
     */
    public static JsonValue getKeyMap(Key key) throws Exception {
        return json(object(
                field("algorithm", key.getAlgorithm()),
                field("format", key.getFormat()),
                field("encoded", toPem(key))
        ));
    }

    /**
     * Returns a JsonValue map representing key
     *
     * @param key  The key
     * @return a JsonValue map representing the key
     * @throws Exception
     */
    public static JsonValue getSecretKeyMap(Key key) throws Exception {
        return json(object(
                field("algorithm", key.getAlgorithm()),
                field("format", key.getFormat()),
                field("encoded", Base64.encode(key.getEncoded()))
        ));
    }

    /**
     * Returns a PEM String representation of a object.
     *
     * @param object the object
     * @return the PEM String representation
     * @throws Exception
     */
    public static String toPem(Object object) throws Exception {
        StringWriter sw = new StringWriter();
        PEMWriter pw = new PEMWriter(sw);
        pw.writeObject(object);
        pw.flush();
        return sw.toString();
    }

    private KeyRepresentation() {
        // prevent instantiation
    }
}
