/*
 * Copyright 2013 ForgeRock, AS.
 *
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
 */
package org.forgerock.openidm.provisioner.openicf.syncfailure;

import org.forgerock.json.fluent.JsonValue;

/**
 * A factory interface to create a {@link SyncFailureHandler}.
 *
 * @author brmiller
 */
public interface SyncFailureHandlerFactory {
    /**
     * Create a <em>SyncFailureHandler</em> from the config.  The config should describe a strategy and
     * any necessary parameters needed by that handler.
     *
     * @param config the config for the SyncFailureHandler
     * @return the SyncFailureHandler
     */
    public SyncFailureHandler create(JsonValue config);
}

