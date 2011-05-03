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

package org.forgerock.openidm.config;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.openidm.config.installer.JSONConfigInstaller;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility to handle enhanced configuration, including nested lists and maps to 
 * represent JSON based structures. 
 *
 * @author aegloff
 */
public class JSONEnhancedConfig implements EnhancedConfig {

    final static Logger logger = LoggerFactory.getLogger(EnhancedConfig.class);
    
    private ObjectMapper mapper = new ObjectMapper();

    /* (non-Javadoc)
     * @see org.forgerock.openidm.config.impl.EnhancedConfig#getConfiguration(org.osgi.service.component.ComponentContext)
     */
    @Override
    public Map<String, Object> getConfiguration(ComponentContext compContext) throws InvalidException { 
        Dictionary dict = null;
        if (compContext != null) {
            dict = compContext.getProperties();
        }
        return getConfiguration(dict);
    }
    
    /* (non-Javadoc)
     * @see org.forgerock.openidm.config.impl.EnhancedConfig#getConfiguration(java.util.Dictionary)
     */
    @Override
    public  Map<String, Object> getConfiguration(Dictionary<String, Object> dict) throws InvalidException {
        Map<String, Object> parsedConfig = new HashMap<String, Object>();
        
        if (dict != null) {
            String jsonConfig = (String) dict.get(JSONConfigInstaller.JSON_CONFIG_PROPERTY);
            logger.debug("Get configuration from JSON config property ", jsonConfig);
    
            try {
                if (jsonConfig != null && jsonConfig.trim().length() > 0) {
                    parsedConfig = mapper.readValue(jsonConfig, Map.class);
                }
            } catch (Exception ex) {
                throw new InvalidException("Configuration could not be parsed: " + ex.getMessage(), ex);
            }
        }
        logger.debug("Parsed configuration ", parsedConfig);
        return parsedConfig;
    }
}
