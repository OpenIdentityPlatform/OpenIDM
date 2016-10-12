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
package org.forgerock.openidm.keystore.impl;

import static org.forgerock.openidm.core.IdentityServer.KEYSTORE_LOCATION;
import static org.forgerock.openidm.core.IdentityServer.KEYSTORE_PASSWORD;
import static org.forgerock.openidm.core.IdentityServer.KEYSTORE_PROVIDER;
import static org.forgerock.openidm.core.IdentityServer.KEYSTORE_TYPE;

import java.security.GeneralSecurityException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.openidm.core.ServerConstants;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        name = KeyStoreServiceImpl.PID,
        immediate = true,
        policy = ConfigurationPolicy.IGNORE
)
@Properties({
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM KeyStore Service"),
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME)
})
@Service
/**
 * Implements a service which contains access to the openidm keystore.
 */
public class KeyStoreServiceImpl extends AbstractKeyStoreService {

    private static final Logger logger = LoggerFactory.getLogger(KeyStoreServiceImpl.class);
    static final String PID = "org.forgerock.openidm.impl.keystore";

    /**
     * Constructs a {@link KeyStoreServiceImpl} and sets the keystore java system properties.
     * @throws GeneralSecurityException if unable to construct a {@link KeyStoreServiceImpl}.
     */
    public KeyStoreServiceImpl() throws GeneralSecurityException {
        super(KEYSTORE_PASSWORD, KEYSTORE_TYPE, KEYSTORE_PROVIDER, KEYSTORE_LOCATION);
        // Set System properties
        if (System.getProperty("javax.net.ssl.keyStore") == null) {
            System.setProperty("javax.net.ssl.keyStore", keyStoreDetails.getFilename());
            System.setProperty("javax.net.ssl.keyStorePassword", keyStoreDetails.getPassword());
            System.setProperty("javax.net.ssl.keyStoreType", keyStoreDetails.getType().name());
        }
    }

    /**
     * Initializes and loads the openidm keystore.
     * @param context the {@link ComponentContext}.
     * @throws GeneralSecurityException if unable to initialize and load the openidm keystore.
     */
    @Activate
    public void activate(@SuppressWarnings("unused") ComponentContext context) throws GeneralSecurityException {
        logger.debug("Activating key store service");
        this.store = keyStoreInitializer.initializeKeyStore(getKeyStoreDetails());
        store();
    }

    /**
     * Nullifies the openidm keystore and keystore details.
     * @param context the {@link ComponentContext}.
     */
    @Deactivate
    public void deactivate(@SuppressWarnings("unused") ComponentContext context) {
        logger.debug("Deactivating key store service");
        store = null;
    }
}
