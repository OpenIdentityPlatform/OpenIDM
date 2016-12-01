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

package org.forgerock.openidm.config.manage.api;

import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.forgerock.api.annotations.Description;

/**
 * Resource for {@link org.forgerock.openidm.config.manage.ConfigObjectService}, but only includes fields
 * that all configurations will have.
 */
public class ConfigResource {

    @JsonProperty(FIELD_CONTENT_ID)
    private String id;

    /**
     * Gets configuration identifier (instanceId or factoryId/instanceId).
     *
     * @return Configuration identifier (instanceId or factoryId/instanceId)
     */
    @NotNull
    @Description("Configuration identifier (instanceId or factoryId/instanceId)")
    public String getId() {
        return id;
    }

    /**
     * Sets configuration identifier (instanceId or factoryId/instanceId).
     *
     * @param id Configuration identifier (instanceId or factoryId/instanceId)
     */
    public void setId(String id) {
        this.id = id;
    }

}
