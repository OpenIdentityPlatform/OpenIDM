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
import org.forgerock.api.annotations.Action;
import org.forgerock.api.annotations.ApiError;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.annotations.SingletonProvider;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Email service implementation
 */
@SingletonProvider(@Handler(
        id = "emailService:0",
        title = "Email Service",
        description = "Service that sends email via an external SMTP server.",
        mvccSupported = false))
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
    private volatile EnhancedConfig enhancedConfig;

    EmailClient emailClient;

    @Action(operationDescription =
    @Operation(
            description = "Send email",
            errors = {
                    @ApiError(
                            id = "badRequest",
                            code = 400,
                            description = "Indicates that the request could not be understood by "
                                    + "the resource due to malformed syntax.")
            }),
            name = "send",
            request = @Schema(schemaResource = "sendActionRequest.json"),
            response = @Schema(schemaResource = "sendActionResponse.json"))
    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, ActionRequest request) {
        Map<String, Object> result = new HashMap<>();
        logger.debug("External Email service action called for {} with {}",
                request.getResourcePath(), request.getContent());
        try {
            emailClient.send(request.getContent());
        } catch (ResourceException e) {
            return e.asPromise();
        }
        result.put("status", "OK");
        return Promises.newResultPromise(Responses.newActionResponse(new JsonValue(result)));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, PatchRequest request) {
        return new ForbiddenException("Operation is not implemented").asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, ReadRequest request) {
        return new ForbiddenException("Operation is not implemented").asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, UpdateRequest request) {
        return new ForbiddenException("Operation is not implemented").asPromise();
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
