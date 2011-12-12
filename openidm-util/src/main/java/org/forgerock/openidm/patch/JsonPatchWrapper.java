/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.patch;

// Java SE
import java.util.Map;

// OpenIDM
import org.forgerock.openidm.objset.ConflictException;
import org.forgerock.openidm.objset.Patch;

// JSON Fluent
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;

// JSON Patch
import org.forgerock.json.patch.JsonPatch;

/**
 * TODO: Description.
 *
 * @author Paul C. Bryan
 * @deprecated Implement {@code JsonResource} (or extend {@code SimpleJsonResource}) instead.
 */
@Deprecated
public class JsonPatchWrapper implements Patch {

    /** TODO: Description. */
    private JsonValue diff;

    /**
     * TODO: Description.
     *
     * @param diff TODO.
     * @throws NullPointerException if {@code patch} is {@code null}.
     */
    public JsonPatchWrapper(JsonValue diff) {
        if (diff == null) {
            throw new NullPointerException();
        }
        this.diff = diff;
    }

    public JsonValue getDiff() {
        return diff;
    }

    @Override
    public void apply(Map<String, Object> object) throws ConflictException {
        try {
            JsonValue jv = new JsonValue(object);
            JsonPatch.patch(jv, diff);
            if (jv.getObject() != object) {
                throw new ConflictException("replacing the root value is not supported");
            }
        } catch (JsonValueException jve) {
            throw new ConflictException(jve);
        }
    }
}
