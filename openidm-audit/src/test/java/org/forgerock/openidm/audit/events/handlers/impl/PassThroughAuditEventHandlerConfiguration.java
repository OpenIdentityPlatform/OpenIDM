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
package org.forgerock.openidm.audit.events.handlers.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.forgerock.audit.events.handlers.EventHandlerConfiguration;

/**
 * A configuration for pass-through audit event handler.
 */
public class PassThroughAuditEventHandlerConfiguration extends EventHandlerConfiguration {

    @JsonProperty("message")
    private String message;

    /**
     * Returns the message.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message.
     *
     * @param message the message
     */
    public void setMessage(String message) {
        this.message = message;
    }
}
