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
 * Copyright 2011-2017 ForgeRock AS.
 */

package org.forgerock.openidm.external.email.impl;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newActionResponse;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.api.annotations.Actions;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Parameter;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.annotations.SingletonProvider;
import org.forgerock.api.enums.ParameterSource;
import org.forgerock.json.JsonValue;
import org.forgerock.services.context.Context;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.util.promise.Promise;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Email service implementation
 */
@SingletonProvider(@Handler(
        id = "emailService:0",
        title = "Email",
        description = "Service that sends email via an external SMTP server.",
        mvccSupported = false))
@Component(name = EmailServiceImpl.PID, immediate = true, policy = ConfigurationPolicy.REQUIRE)
@Service
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Outbound Email Service"),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "external/email") })
public class EmailServiceImpl implements SingletonResourceProvider {
    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);
    public static final String PID = "org.forgerock.openidm.external.email";

    private static final String CONFIG_MAIL_THREAD_POOL_SIZE = "threadPoolSize";
    private static final int DEFAULT_THREAD_POOL_SIZE = 20;

    /** Response send when email has been submitted, but we're not waiting for completion */
    static final ActionResponse DID_NOT_WAIT = newActionResponse(json(object(
            field("status", "OK"),
            field("message", "Email submitted"))));

    /** EmailService parameter to indicate sending asynchronously */
    static final String PARAM_WAIT_FOR_COMPLETION = "waitForCompletion";

    /** Enhanced configuration service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile EnhancedConfig enhancedConfig;

    private ExecutorService executorService;

    EmailClient emailClient;

    enum Action { send, sendEmail }

    @Actions({
            @org.forgerock.api.annotations.Action(
                    operationDescription = @Operation(
                            description = "Send email",
                            errorRefs = "frapi:common#/errors/badRequest",
                            parameters = {
                                    @Parameter(
                                            name = "waitForCompletion",
                                            description = "Whether or not request will block until email has been "
                                                    + "accepted by the SMTP server.",
                                            type = "string",
                                            required = false,
                                            defaultValue = "true",
                                            source = ParameterSource.ADDITIONAL)
                            }),
                    name = "send",
                    request = @Schema(schemaResource = "sendActionRequest.json"),
                    response = @Schema(schemaResource = "sendActionResponse.json"))
    })
    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, ActionRequest request) {
        switch (request.getActionAsEnum(Action.class)) {
            case sendEmail:
                logger.warn("\"sendEmail\" is deprecated, please use the \"send\" action instead");
            case send:
                // Support (not) waiting for completion of email to send
                final boolean waitForCompletion = !"false".equalsIgnoreCase(
                        request.getAdditionalParameter(PARAM_WAIT_FOR_COMPLETION));

                logger.debug("External Email service action {} ({}) called for {} with {}",
                        request.getAction(), waitForCompletion ? "wait" : "don't wait",
                        request.getResourcePath(), request.getContent());

                final Promise<ActionResponse, ResourceException> promise = emailClient.sendAsync(request.getContent());
                return waitForCompletion
                        ? promise
                        : DID_NOT_WAIT.asPromise();
            default:
                return new BadRequestException("Not a supported Action").asPromise();
        }
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
            final JsonValue config = enhancedConfig.getConfigurationAsJson(compContext);
            executorService = Executors.newFixedThreadPool(
                    config.get(CONFIG_MAIL_THREAD_POOL_SIZE).defaultTo(DEFAULT_THREAD_POOL_SIZE).asInteger());

            emailClient = new EmailClient(config, executorService);
            logger.debug("external email client enabled");
        } catch (RuntimeException ex) {
            logger.warn("Configuration invalid, can not start external email client service.", ex);
            throw ex;
        }
        logger.info("External email service started.");
    }

    @Deactivate
    void deactivate(ComponentContext compContext) {
        logger.debug("Deactivating Service {}", compContext.getProperties());
        executorService.shutdown();
        logger.info("External email service stopped.");
    }
}
