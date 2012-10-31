/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2012 ForgeRock AS. All Rights Reserved
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
*/
package org.forgerock.openidm.info.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.osgi.framework.Constants;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration for additional info service(s)
 *
 * @author aegloff
 */
@Component(name = InfoConfigImpl.PID, policy = ConfigurationPolicy.REQUIRE,
        description = "OpenIDM Config for Info Service", immediate = true,
        configurationFactory=true)
@Service()
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM Config for Info Service")})
public class InfoConfigImpl implements InfoConfig {

    public static final String PID = "org.forgerock.openidm.info";

    private static final Logger logger = LoggerFactory.getLogger(InfoConfigImpl.class);

    private JsonValue config;
    
    private ComponentContext context;
    private String name;
    
    /* (non-Javadoc)
     * @see org.forgerock.openidm.info.impl.InfoConfig#getConfig()
     */
    @Override
    public JsonValue getConfig() {
        return this.config;
    }
    
    /* (non-Javadoc)
     * @see org.forgerock.openidm.info.impl.InfoConfig#getName()
     */
    public String getName() {
        return name;
    }

    @Activate
    protected void activate(ComponentContext context) {
        this.context = context;
        this.name = "info/" + context.getProperties().get("config.factory-pid");
        this.config = JSONEnhancedConfig.newInstance().getConfigurationAsJson(context);
        logger.debug("OpenIDM Config for Info Service {} is activated.", config.get(Constants.SERVICE_PID));
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        this.context = null;
        logger.debug("OpenIDM Config for Info Service {} is deactivated.", config.get(Constants.SERVICE_PID));
    }
}

