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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openidm.patch;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.JsonValue.field;
import static org.assertj.core.api.Assertions.assertThat;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.ResourceException;
import org.testng.annotations.Test;


/**
 * Test NullTransformerTest methods.
 */
public class NullTransformerTest {
    @Test
    public void testGetTransformedValue() throws ResourceException {
        JsonValue diff = json(object(
                field("operation", "transform"),
                field("field", "/key"),
                field("value", object(
                        field("script", "var source = content.key; var target = source + 'xformed'; target;")
                        )
                )));
        PatchOperation operation = PatchOperation.valueOf(diff);
        JsonValue subject = json(object(field("key", "value")));
        JsonValue value = NullTransformer.NULL_TRANSFORMER.getTransformedValue(operation, subject);

        assertThat(value.get("key").isString()).isTrue();
        assertThat(value.get("key").asString()).isEqualTo("value");
    }
}