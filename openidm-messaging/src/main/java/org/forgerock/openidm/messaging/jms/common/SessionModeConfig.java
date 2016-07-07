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
package org.forgerock.openidm.messaging.jms.common;

import javax.jms.Session;

/**
 * Configuration wrapper for JMS {@link Session#getAcknowledgeMode()} SessionMode setting.
 */
public enum SessionModeConfig {

    /**
     * Auto-acknowledge session mode.
     */
    AUTO(Session.AUTO_ACKNOWLEDGE),

    /**
     * Client-acknowledge session mode.
     */
    CLIENT(Session.CLIENT_ACKNOWLEDGE),

    /**
     * Dups-OK-acknowledge session mode.
     */
    DUPS_OK(Session.DUPS_OK_ACKNOWLEDGE);

    private int mode;

    /**
     * Creates the config instance with the passed in 'mode' setting.
     *
     * @param mode the session mode setting for this instance.
     * @see Session#getAcknowledgeMode()
     */
    SessionModeConfig(int mode) {
        this.mode = mode;
    }

    /**
     * Returns the session mode setting for this configuration.
     *
     * @return the session mode setting for this configuration.
     * @see Session#getAcknowledgeMode()
     */
    public int getMode() {
        return mode;
    }
}