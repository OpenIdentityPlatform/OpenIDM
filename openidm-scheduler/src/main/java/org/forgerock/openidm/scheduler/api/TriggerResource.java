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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.ReadOnly;
import org.forgerock.api.annotations.Title;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openidm.scheduler.SchedulerService;

/**
 * Resource for {@link SchedulerService} Trigger Instance responses.
 */
@Title("State of a Quartz Trigger")
public class TriggerResource {
    private String id;
    private String rev;
    private String name;
    private String serialized;
    private int previousState;
    private boolean acquired;
    private String nodeId;

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

    /**
     * Name given to the Trigger by Quartz.
     *
     * @return Name given to the Trigger by Quartz.
     */
    @Description("Name given to the Trigger by Quartz")
    @ReadOnly
    public String getName() {
        return name;
    }

    /**
     * Quartz serialized state of the trigger.
     *
     * @return Quartz serialized state of the trigger.
     */
    @Description("Quartz serialized state of the trigger")
    @ReadOnly
    public String getSerialized() {
        return serialized;
    }

    /**
     * Previous state of the trigger.
     *
     * @return Previous state of the trigger.
     */
    @Description("Previous state of the trigger. See Quartz Trigger documentation for value descriptions. "
            + "STATE_NORMAL = 0, STATE_PAUSED = 1, STATE_COMPLETE = 2, STATE_ERROR = 3, STATE_BLOCKED = 4, STATE_NONE"
            + " = -1")
    @ReadOnly
    public int getPreviousState() {
        return previousState;
    }

    /**
     * True if acquired by the OpenIDM instance.
     *
     * @return True if acquired by the OpenIDM instance.
     */
    @Description("True if acquired by the OpenIDM instance")
    @ReadOnly
    public boolean isAcquired() {
        return acquired;
    }

    /**
     * Node ID of the OpenIDM instance which has acquired this trigger.
     *
     * @return Node ID of the OpenIDM instance which has acquired this trigger.
     */
    @Description("Node ID of the OpenIDM instance which has acquired this trigger")
    @ReadOnly
    public String getNodeId() {
        return nodeId;
    }
}
