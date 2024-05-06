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
 * Portions Copyrighted 2024 3A Systems LLC.
 */
package org.forgerock.openidm.keystore.impl;

import static org.forgerock.openidm.core.IdentityServer.KEYSTORE_LOCATION;
import static org.forgerock.openidm.core.IdentityServer.KEYSTORE_PASSWORD;
import static org.forgerock.openidm.core.IdentityServer.KEYSTORE_PROVIDER;
import static org.forgerock.openidm.core.IdentityServer.KEYSTORE_TYPE;

import java.security.GeneralSecurityException;
import java.security.KeyStore;

import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.keystore.KeyStoreDetails;
import org.forgerock.openidm.keystore.KeyStoreService;
import org.forgerock.openidm.util.CryptoUtil;
import org.forgerock.security.keystore.KeyStoreType;
import org.forgerock.util.Utils;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.component.propertytypes.ServiceVendor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        name = KeyStoreServiceImpl.PID,
        configurationPolicy = ConfigurationPolicy.IGNORE,
        immediate = true,
        property = Constants.SERVICE_PID + "=" + KeyStoreServiceImpl.PID,
        service = KeyStoreService.class)
@ServiceVendor(ServerConstants.SERVER_VENDOR_NAME)
@ServiceDescription("OpenIDM KeyStore Service")
/**
 * Implements a service which contains access to the openidm keystore.
 */
public class KeyStoreServiceImpl extends AbstractKeyStoreService {
    static final String PID = "org.forgerock.openidm.keystore";

    private static final Logger logger = LoggerFactory.getLogger(KeyStoreServiceImpl.class);

    /**
     * Constructs a {@link KeyStoreServiceImpl} and sets the keystore java system properties.
     * @throws GeneralSecurityException if unable to construct a {@link KeyStoreServiceImpl}.
     */
    public KeyStoreServiceImpl() throws GeneralSecurityException {
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

    @Override
    KeyStoreDetails createKeyStoreDetails() throws GeneralSecurityException {
        final String password = IdentityServer.getInstance().getProperty(KEYSTORE_PASSWORD);
        final KeyStoreType type =
                Utils.asEnum(
                        IdentityServer.getInstance().getProperty(KEYSTORE_TYPE, KeyStore.getDefaultType()),
                        KeyStoreType.class);
        final String provider = IdentityServer.getInstance().getProperty(KEYSTORE_PROVIDER);
        String filename = IdentityServer.getInstance().getProperty(KEYSTORE_LOCATION);
        if (isUsingFile(filename)) {
            // get filename absolute location
            filename = IdentityServer.getFileForInstallPath(filename).getAbsolutePath();
        }

        final char[] clearPassword = CryptoUtil.unfold(password);
        return new KeyStoreDetails(type, provider, filename, new String(clearPassword));
    }
}
