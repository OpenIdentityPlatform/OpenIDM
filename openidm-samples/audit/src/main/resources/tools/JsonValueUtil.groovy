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

import static org.forgerock.json.JsonValue.*;

import org.forgerock.json.JsonValue
import org.forgerock.http.util.Json;

public class JsonValueUtil {
    public static JsonValue fromEntries(Map.Entry<String,Object>[] entries) {
        JsonValue json = json(object());

        for (Map.Entry<String,Object> entry : entries) {
            if (entry.getKey() != null && entry.getValue() != null) {
                json.add(entry.getKey(), entry.getValue());
            }
        }

        return json;
    }

    public static JsonValue fromJsonString(String string) {
        return string != null && string.length() > 0 ? json(Json.readJson(string)) : null;
    }

    public static Boolean booleanFromString(String string) {
        try {
            return Boolean.valueOf(string);
        } catch (Exception e) {
            return Boolean.FALSE;
        }
    }
}