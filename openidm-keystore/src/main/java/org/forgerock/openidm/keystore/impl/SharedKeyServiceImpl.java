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
 * Copyright 2016 ForgeRock AS
 */
package org.forgerock.openidm.keystore.impl;

import java.security.Key;
import java.security.KeyPair;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.crypto.JsonCryptoException;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.keystore.SharedKeyService;
import org.forgerock.openidm.keystore.KeyStoreService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared-key Service.
 */
@Component(name = SharedKeyServiceImpl.PID, immediate = true, policy = ConfigurationPolicy.IGNORE)
@Service
@Properties({
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM shared-key service"),
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME)
})
public class SharedKeyServiceImpl implements SharedKeyService {
    static final String PID = "org.forgerock.openidm.keystore.sharedkey";

    private static final Logger logger = LoggerFactory.getLogger(SharedKeyServiceImpl.class);

    private KeyPairSelector keySelector;

    @Reference(target="(service.pid=org.forgerock.openidm.keystore)")
    private KeyStoreService keyStoreService;

    public void activate(@SuppressWarnings("unused") BundleContext context) {
        logger.debug("Activating shared-key service");
        keySelector =
                new KeyPairSelector(
                        keyStoreService.getKeyStore(),
                        keyStoreService.getKeyStoreDetails().getPassword());
    }

    public void deactivate(@SuppressWarnings("unused") BundleContext context) {
        keySelector = null;
        logger.info("Shared-key service stopped.");
    }

    @Override
    public Key getSharedKey(String alias) throws Exception {
        return keySelector.select(alias);
    }

    @Override
    public KeyPair getKeyPair(String alias) throws JsonCryptoException {
        return keySelector.getKeyPair(alias);
    }
}
