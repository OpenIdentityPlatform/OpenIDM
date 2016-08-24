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
package org.forgerock.openidm.idp.config;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.forgerock.json.JsonValue;

/**
 * Configuration for a single property mapping
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SingleMapping {
    @JsonProperty
    private String source;

    @JsonProperty
    private String target;

    @JsonProperty
    private String condition;

    @JsonProperty("default")
    private String dflt;

    @JsonProperty
    private Transform transform;

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    public String getCondition() {
        return condition;
    }

    public String getDefault() {
        return dflt;
    }

    public Transform getTransform() {
        return transform;
    }

    public JsonValue asJsonValue() {
        JsonValue retval = json(object(
                field("target", getTarget()),
                field("source", getSource())
        ));
        if (getCondition() != null) {
            retval.add("condition", getCondition());
        }
        if (getTransform() != null) {
            retval.add("transform", getTransform().asJsonValue());
        }
        if (getDefault() != null) {
            retval.add("default", getDefault());
        }
        return retval;
    }
}
