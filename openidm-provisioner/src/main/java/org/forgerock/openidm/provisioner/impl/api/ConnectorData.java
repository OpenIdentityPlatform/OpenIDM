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

package org.forgerock.openidm.provisioner.impl.api;

import javax.validation.constraints.NotNull;

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;

/**
 * Connector data returned as part of the {@code liveSync}-action response.
 */
@Title("Live-Sync Metadata")
public class ConnectorData {

    private String nativeType;
    private Object syncToken;

    /**
     * Gets JSON- or Java-type of the Synchronization-Token.
     *
     * @return JSON- or Java-type of the Synchronization-Token
     */
    @Description("JSON- or Java-type of the Synchronization-Token")
    @NotNull
    public String getNativeType() {
        return nativeType;
    }

    /**
     * Sets JSON- or Java-type of the Synchronization-Token.
     *
     * @param nativeType JSON- or Java-type of the Synchronization-Token
     */
    public void setNativeType(String nativeType) {
        this.nativeType = nativeType;
    }

    /**
     * Gets synchronization-Token with connector-specific meaning.
     *
     * @return Synchronization-Token with connector-specific meaning
     */
    @Description("Synchronization-Token with connector-specific meaning")
    @NotNull
    public Object getSyncToken() {
        return syncToken;
    }

    /**
     * Sets synchronization-Token with connector-specific meaning.
     *
     * @param syncToken Synchronization-Token with connector-specific meaning
     */
    public void setSyncToken(Object syncToken) {
        this.syncToken = syncToken;
    }

}
