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
package org.forgerock.openidm.security.impl.api;

import java.security.Provider;
import java.util.List;
import java.util.Map;

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.ReadOnly;
import org.forgerock.api.annotations.Title;
import org.forgerock.security.keystore.KeyStoreType;

/**
 * Resource for {@link org.forgerock.openidm.security.SecurityManager} Keystore responses.
 */
@Title("KeyStore Resource")
public class KeyStoreResource {

    @ReadOnly
    @Description("The type of the keystore")
    private String type;

    @ReadOnly
    @Description("The current provider's configuration of the keystore")
    private Map<String, String> provider;

    @ReadOnly
    @Description("The list of aliases in the keystore")
    private List<String> aliases;

    /**
     * Gets the {@link KeyStoreType} as a string.
     *
     * @return the {@link KeyStoreType} as a string.
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the string identifier of the {@link Provider}.
     *
     * @return the string identifier of the {@link Provider}.
     */
    public Map<String, String> getProvider() {
        return provider;
    }

    /**
     * Gets the aliases in the keystore.
     *
     * @return the aliases in the keystore.
     */
    public List<String> getAliases() {
        return aliases;
    }
}
