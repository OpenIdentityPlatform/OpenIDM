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

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.forgerock.api.annotations.Description;
import org.forgerock.json.JsonValue;

/**
 * Configuration for a single property mapping
 */
@Description("Mapping from one property name to another")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SingleMapping {

    private String source;
    private String target;
    private String condition;
    private String defaultValue;

    @JsonProperty
    private Transform transform;

    /**
     * Gets source property-name.
     *
     * @return Source property-name
     */
    @NotNull
    @Description("Source property-name")
    public String getSource() {
        return source;
    }

    /**
     * Sets source property-name.
     *
     * @param source Source property-name
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Gets target property-name.
     *
     * @return Target property-name
     */
    @NotNull
    @Description("Target property-name")
    public String getTarget() {
        return target;
    }

    /**
     * Sets target property-name.
     *
     * @param target Target property-name
     */
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * Gets query-filter that must evaluate to {@code true} for mapping to occur.
     *
     * @return Query-filter that must evaluate to {@code true} for mapping to occur
     */
    @Description("Query-filter that must evaluate to 'true' for mapping to occur")
    public String getCondition() {
        return condition;
    }

    /**
     * Sets query-filter that must evaluate to {@code true} for mapping to occur.
     *
     * @param condition Query-filter that must evaluate to {@code true} for mapping to occur
     */
    public void setCondition(String condition) {
        this.condition = condition;
    }

    /**
     * Sets default target value to use when there is no source value.
     *
     * @return Default target value to use when there is no source value
     */
    @JsonProperty("default")
    @Description("Default target value to use when there is no source value")
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Gets default target value to use when there is no source value.
     *
     * @param defaultValue Default target value to use when there is no source value
     */
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * Sets script that transforms source value before mapping to target.
     *
     * @return Script that transforms source value before mapping to target
     */
    @Description("Script that transforms source value before mapping to target")
    public Transform getTransform() {
        return transform;
    }

    /**
     * Gets script that transforms source value before mapping to target.
     *
     * @param transform Script that transforms source value before mapping to target
     */
    public void setTransform(Transform transform) {
        this.transform = transform;
    }

    /**
     * Serializes this object as a {@link JsonValue}.
     *
     * @return {@link JsonValue} representation
     */
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
        if (getDefaultValue() != null) {
            retval.add("default", getDefaultValue());
        }
        return retval;
    }
}
