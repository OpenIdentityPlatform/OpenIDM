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
package org.forgerock.openidm.idp.impl;

/**
 * Should be implemented by any service that needs notifications when provider configurations
 * are changed.
 */
public interface IdentityProviderListener {

    /**
     * Returns the name of the IdentityProviderListener.
     * 
     * @return name of the listener as a String
     */
    String getListenerName();

    /**
     * When there is a configuration change on any ProviderConfig,
     * services that have a references to the IdentityProviderService need
     * to be notified that there have been changes.
     */
    void identityProviderConfigChanged();
}

