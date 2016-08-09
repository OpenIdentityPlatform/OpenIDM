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
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.openidm.security;

import java.security.KeyStore;

import org.forgerock.security.keystore.KeyStoreType;

/**
 * Handles Access to a Java KeyStore.
 */
public interface KeyStoreHandler {

    /**
     * Gets the {@link KeyStore}.
     * @return The {@link KeyStore} object.
     */
    KeyStore getStore();

    /**
     * Sets the {@link KeyStore} implementations should reject a null keystore.
     * @param keystore The {@link KeyStore} to set.
     * @throws Exception If unable to set the {@link KeyStore}.
     */
    void setStore(KeyStore keystore) throws Exception;

    /**
     * Gets the {@link KeyStore} password.
     * @return The {@link KeyStore} password.
     */
    String getPassword();

    /**
     * Gets the {@link KeyStore} location.
     * @return The {@link KeyStore} location.
     */
    String getLocation();

    /**
     * Gets the {@link KeyStoreType}.
     * @return The {@link KeyStore} type.
     */
    KeyStoreType getType();

    /**
     * Stores/Saves the {@link KeyStore}.
     * @throws Exception If unable to store the {@link KeyStore}.
     */
    void store() throws Exception;
}
