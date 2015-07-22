/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2015 ForgeRock AS. All rights reserved.
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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Email service implementation
 * 
 */
@Component(name = EmailServiceImpl.PID, immediate = true, policy = ConfigurationPolicy.REQUIRE)
@Service
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Outbound Email Service"),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "external/email") })
public class EmailServiceImpl implements SingletonResourceProvider {
    final static Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);
    public static final String PID = "org.forgerock.openidm.external.email";

    /** Enhanced configuration service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private EnhancedConfig enhancedConfig;

    EmailClient emailClient;

    @Override
    public void actionInstance(ServerContext context, ActionRequest request,
            ResultHandler<JsonValue> handler) {
        Map<String, Object> result = new HashMap<String, Object>();
        logger.debug("External Email service action called for {} with {}", request
                .getResourceName(), request.getContent());
        try {
            emailClient.send(request.getContent());
        } catch (ResourceException e) {
           handler.handleError(e);
        }
        result.put("status", "OK");
        handler.handleResult(new JsonValue(result));
    }

    @Override
    public void patchInstance(ServerContext context, PatchRequest request,
            ResultHandler<Resource> handler) {
        handler.handleError(new ForbiddenException("Operation is not implemented"));
    }

    @Override
    public void readInstance(ServerContext context, ReadRequest request,
            ResultHandler<Resource> handler) {
        handler.handleError(new ForbiddenException("Operation is not implemented"));
    }

    @Override
    public void updateInstance(ServerContext context, UpdateRequest request,
            ResultHandler<Resource> handler) {
        handler.handleError(new ForbiddenException("Operation is not implemented"));
    }

    @Activate
    void activate(ComponentContext compContext) {
        logger.debug("Activating Service with configuration {}", compContext.getProperties());
        try {
            emailClient = new EmailClient(enhancedConfig.getConfigurationAsJson(compContext));
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
