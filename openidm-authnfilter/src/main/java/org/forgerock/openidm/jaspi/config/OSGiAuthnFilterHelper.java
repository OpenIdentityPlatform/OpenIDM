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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openidm.jaspi.config;

import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.script.ScriptRegistry;

/**
 * An interface to help provide the auth module dependencies from OSGi.
 */
public interface OSGiAuthnFilterHelper {

    /**
     * Returns the Crypto Service instance.
     *
     * @return The Crypto Service instance.
     */
    CryptoService getCryptoService();

    /**
     * Returns the ScriptRegistry instance
     *
     * @return the ScriptRegistry instance
     */
    ScriptRegistry getScriptRegistry();

    /**
     * Returns the ConnectionFactory instance
     *
     * @return The ConnectionFactory instance
     */
    ConnectionFactory getConnectionFactory();

}
