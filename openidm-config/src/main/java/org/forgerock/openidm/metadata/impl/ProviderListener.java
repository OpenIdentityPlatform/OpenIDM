/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2015 ForgeRock AS. All Rights Reserved
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
 *
 */
package org.forgerock.openidm.metadata.impl;

import org.forgerock.openidm.config.crypto.ConfigCrypto;
import org.forgerock.openidm.metadata.MetaDataProvider;

/**
 * Listener to handle Provider registrations
 * 
 */
public interface ProviderListener {

    /**
     * Initialize the listener to make it ready to handle listener events
     * @param configCrypto The configuration cryptography facility holding the 
     * meta-data about encrypted properties
     */
    public void init(ConfigCrypto configCrypto);
    
    /**
     * Notified when a provider was added
     * @param originId an identifier indicating where the meta data provider is coming from
     * @param service the added provider
     */
    public void addedProvider(Object originId, MetaDataProvider service);
}
