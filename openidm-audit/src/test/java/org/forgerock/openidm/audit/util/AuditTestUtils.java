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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openidm.audit.util;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.audit.AuditException;
import org.forgerock.json.JsonValue;
import org.forgerock.util.promise.ResultHandler;

public class AuditTestUtils {

    private AuditTestUtils () {
        //prevent instantiation
    }

    //checkstyle:off
    private static final ObjectMapper mapper = new ObjectMapper();
    // checkstyle:on

    public static JsonValue getJson(InputStream input) throws AuditException {
        try {
            return new JsonValue(mapper.readValue(input, LinkedHashMap.class));
        } catch (IOException e) {
            throw new AuditException("Unable to retrieve json value from json input stream", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> ResultHandler<T> mockResultHandler(Class<T> type) {
        return mock(ResultHandler.class);

    }
}
