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
package org.forgerock.openidm.scheduler.api;

import javax.validation.constraints.NotNull;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.forgerock.api.annotations.AdditionalProperties;
import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Example;
import org.forgerock.api.annotations.ReadOnly;
import org.forgerock.api.annotations.Title;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openidm.scheduler.SchedulerService;

/**
 * Resource for {@link SchedulerService} Trigger 'acquiredTriggers' or 'waitingTriggers' responses.
 */
@Title("For each node in the cluster, an array of triggers based on the requested trigger type "
        + "('acquiredTriggers' or 'waitingTriggers').")
@Example("\n{\n"
        + "  \"_id\": \"acquiredTriggers\",\n"
        + "  \"_rev\": \"4642\",\n"
        + "  \"node1\": [\n"
        + "    \"scheduler-service-group_$x$x$_trigger-jobName\"\n"
        + "  ]\n"
        + "}")
@AdditionalProperties(String[].class)
public class TriggerListResource {
    private String id;
    private String rev;

    /**
     * Either 'acquiredTriggers' or 'waitingTriggers'.
     *
     * @return Either 'acquiredTriggers' or 'waitingTriggers'.
     */
    @JsonProperty(ResourceResponse.FIELD_CONTENT_ID)
    @Description("Either 'acquiredTriggers' or 'waitingTriggers'")
    @NotNull
    public String getId() {
        return id;
    }

    /**
     * Revision of the trigger repo state.
     *
     * @return Revision of the trigger repo state.
     */
    @JsonProperty(ResourceResponse.FIELD_CONTENT_REVISION)
    @Description("Revision of the trigger repo state")
    @NotNull
    public String getRev() {
        return rev;
    }

}
