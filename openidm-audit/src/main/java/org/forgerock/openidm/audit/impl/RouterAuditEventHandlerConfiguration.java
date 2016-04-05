/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015-2016 ForgeRock AS.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.forgerock.openidm.audit.impl;

import org.forgerock.audit.events.handlers.EventHandlerConfiguration;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A configuration for router audit event handler.
 * <p>
 * This configuration object can be created from JSON. Example of valid JSON configuration:
 * <pre>
 *  {
 *    "resourcePath" : "system/auditdb"
 *  }
 * </pre>
 */
public class RouterAuditEventHandlerConfiguration extends EventHandlerConfiguration {

    @JsonProperty(required=true)
    private String resourcePath;

    /**
     * Returns the resourcePath where the audit events will be sent.
     *
     * @return the resourcePath target for the audit events
     */
    public String getResourcePath() {
        return resourcePath;
    }

    /**
     * Sets the resourcePath where audit events will be dispatched.
     *
     * @param resourcePath the resource path.
     */
    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    @Override
    public boolean isUsableForQueries() {
        return true;
    }
}