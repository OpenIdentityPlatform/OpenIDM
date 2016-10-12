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

import static org.forgerock.openidm.core.IdentityServer.TRUSTSTORE_LOCATION;
import static org.forgerock.openidm.core.IdentityServer.TRUSTSTORE_PASSWORD;
import static org.forgerock.openidm.core.IdentityServer.TRUSTSTORE_PROVIDER;
import static org.forgerock.openidm.core.IdentityServer.TRUSTSTORE_TYPE;

import java.security.GeneralSecurityException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.keystore.KeyStoreService;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        name = TrustStoreServiceImpl.PID,
        immediate = true,
        policy = ConfigurationPolicy.IGNORE
)
@Properties({
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM TrustStore Service"),
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME)
})
@Service
/**
 * Implements
 */
public class TrustStoreServiceImpl extends AbstractKeyStoreService {

    private static final Logger logger = LoggerFactory.getLogger(TrustStoreServiceImpl.class);
    static final String PID = "org.forgerock.openidm.impl.truststore";

    @Reference(target="(service.pid=" + KeyStoreServiceImpl.PID + ")")
    private KeyStoreService keyStore;

    public TrustStoreServiceImpl() throws GeneralSecurityException {
        super(TRUSTSTORE_PASSWORD, TRUSTSTORE_TYPE, TRUSTSTORE_PROVIDER, TRUSTSTORE_LOCATION);
        System.setProperty("javax.net.ssl.trustStore", keyStoreDetails.getFilename());
        System.setProperty("javax.net.ssl.trustStorePassword", keyStoreDetails.getPassword());
        System.setProperty("javax.net.ssl.trustStoreType", keyStoreDetails.getType().name());
    }

    @Activate
    public void activate(ComponentContext context) throws GeneralSecurityException {
        logger.debug("Activating trust store service");
        this.store = keyStoreInitializer.initializeTrustStore(keyStore.getKeyStore(), keyStoreDetails);
        store();
        keyStore.store();
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        logger.debug("Deactivating trust store service");
        this.store = null;
    }
}
