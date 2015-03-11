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
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */
package org.forgerock.openidm.config.installer;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.openidm.config.crypto.ConfigCrypto;
import org.forgerock.openidm.metadata.impl.ProviderListener;
import org.forgerock.openidm.metadata.MetaDataProvider;
import org.forgerock.openidm.metadata.WaitForMetaData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles configuration that needs to wait for related meta-data to become available
 * (e.g. for encryption) before it can be provided to OSGi
 */
public class DelayedConfigHandler implements ProviderListener {
    final static Logger logger = LoggerFactory.getLogger(DelayedConfigHandler.class);
    
    ConfigCrypto configCrypto;
    List<DelayedConfig> delayedConfigs = new ArrayList<DelayedConfig>();
    
    /**
     * {@inheritDoc}
     */
    public void init(ConfigCrypto configCrypto) {
        this.configCrypto = configCrypto;
    }
    
    /**
     * Add a configuration to the delayed pool
     * 
     * @param config the configuration to handle when new meta-data becomes available
     */
    public void addConfig(DelayedConfig config) {
        delayedConfigs.add(config);
    }
    
    /**
     * Detects changed meta data providers
     */
    public void addedProvider(Object originId, MetaDataProvider provider) {
        for (DelayedConfig config : delayedConfigs) {
            if (configCrypto != null) {
                List<JsonPointer> props = null;
                try {
                    props = config.configCrypto.getPropertiesToEncrypt(config.pidOrFactory, config.factoryAlias, config.parsedConfig);
                    try {
                        // Meta data now found, handle it
                        config.configInstaller.setConfig(config.newConfig, new String[] {config.pidOrFactory, config.factoryAlias}, config.file);
                    } catch (Exception ex) {
                        logger.warn("Setting delayed configuration failed for {} {}", new Object[] {config.pidOrFactory, config.factoryAlias, ex});
                    }
                } catch (WaitForMetaData ex) {
                    // Still not available for this config, leave it in delayed config
                    logger.trace("Still no meta data provider for {}-{}", config.pidOrFactory, config.factoryAlias );
                }
            } else {
                logger.warn("Provider change received before DelayedConfigHandler properly initialized");
            }
        }
    }
}
