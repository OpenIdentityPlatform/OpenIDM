/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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

package org.forgerock.openidm.external.email.impl;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.JSONEnhancedConfig;

// Deprecated
import org.forgerock.openidm.objset.ForbiddenException;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.ObjectSetJsonResource;
import org.forgerock.openidm.objset.Patch;

/**
 * Email service implementation
 * @author gael
 */
@Component(name = "org.forgerock.openidm.external.email", immediate = true, policy = ConfigurationPolicy.REQUIRE)
@Service
@Properties({
    @Property(name = "service.description", value = "Outbound Email Service"),
    @Property(name = "service.vendor", value = "ForgeRock AS"),
    @Property(name = "openidm.router.prefix", value = "external/email")
})
public class EmailServiceImpl extends ObjectSetJsonResource {
    final static Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);
    public static final String PID = "org.forgerock.openidm.external.email";
    
    EnhancedConfig enhancedConfig = new JSONEnhancedConfig();
    EmailClient emailClient;

    @Override
    public Map<String, Object> action(String fullId, Map<String, Object> params) throws ObjectSetException {
        Map<String, Object> result = new HashMap<String, Object>();
        logger.debug("External Email service action called for {} with {}", fullId, params);
        emailClient.send(params);
        result.put("status", "OK");
        return result;
    }

    @Activate
    void activate(ComponentContext compContext) {
        logger.debug("Activating Service with configuration {}", compContext.getProperties());
        JsonValue config = null;
        try {
            config = enhancedConfig.getConfigurationAsJson(compContext);
            emailClient = new EmailClient(config);
            logger.debug("external email client enabled");
        } catch (RuntimeException ex) {
            logger.warn("Configuration invalid, can not start external email client service.", ex);
            throw ex;
        }
        logger.info(" external email service started.");
    }

    @Deactivate
    void deactivate(ComponentContext compContext) {
        logger.debug("Deactivating Service {}", compContext.getProperties());
        logger.info("Notification service stopped.");
    }
}
