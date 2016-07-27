/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for
 * the specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file
 * and include the License file at legal/CDDLv1.0.txt. If applicable, add the following
 * below the CDDL Header, with the fields enclosed by brackets [] replaced by your
 * own identifying information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openidm.provisioner.openicf.impl;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.openidm.audit.util.ActivityLogger;
import org.forgerock.openidm.audit.util.Status;
import org.forgerock.services.context.Context;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.common.exceptions.ConnectionBrokenException;
import org.identityconnectors.framework.common.exceptions.ConnectionFailedException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.exceptions.ConnectorSecurityException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException;
import org.identityconnectors.framework.common.exceptions.InvalidPasswordException;
import org.identityconnectors.framework.common.exceptions.OperationTimeoutException;
import org.identityconnectors.framework.common.exceptions.PasswordExpiredException;
import org.identityconnectors.framework.common.exceptions.PermissionDeniedException;
import org.identityconnectors.framework.common.exceptions.PreconditionFailedException;
import org.identityconnectors.framework.common.exceptions.PreconditionRequiredException;
import org.identityconnectors.framework.common.exceptions.RetryableException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.impl.api.remote.RemoteWrappedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.HashMap;

/**
 * Collection of helper utilities to manage Exceptions thrown from ICF
 */
class ExceptionHelper {
    private static final int UNAUTHORIZED_ERROR_CODE = 401;
    private static final Logger logger = LoggerFactory.getLogger(OpenICFProvisionerService.class);

    /**
     * Handle ConnectorExceptions from ConnectorFacade invocations.  Maps each ConnectorException subtype to the
     * appropriate {@link ResourceException} for passing to {@code handleError}.  Optionally logs to activity log.
     *
     * @param context the Context from the original request
     * @param request the original request
     * @param exception the ConnectorException that was thrown by the facade
     * @param resourceId the resourceId being operated on
     * @param before the object value "before" the request
     * @param after the object value "after" the request
     * @param connectorExceptionActivityLogger the ActivityLogger to use to log the exception
     */
    static ResourceException adaptConnectorException(Context context, Request request, ConnectorException exception,
            String resourceContainer, String resourceId, JsonValue before, JsonValue after,
            ActivityLogger connectorExceptionActivityLogger) {

        // default message
        String message = MessageFormat.format("Operation {0} failed with {1} on system object: {2}",
                request.getRequestType(), exception.getClass().getSimpleName(), resourceId);

        try {
            throw exception;
        } catch (AlreadyExistsException e) {
            message = MessageFormat.format("System object {0} already exists", resourceId);
            return new org.forgerock.json.resource.PreconditionFailedException(message, exception);
        } catch (ConfigurationException e) {
            message = MessageFormat.format("Operation {0} failed with ConfigurationException on system object: {1}",
                    request.getRequestType().toString(), resourceId);
            return new InternalServerErrorException(message, exception);
        } catch (ConnectionBrokenException e) {
            message = MessageFormat.format("Operation {0} failed with ConnectionBrokenException on system object: {1}",
                    request.getRequestType().toString(), resourceId);
            return new ServiceUnavailableException(message, exception);
        } catch (ConnectionFailedException e) {
            message = MessageFormat.format("Connection failed during operation {0} on system object: {1}",
                    request.getRequestType().toString(), resourceId);
            return new ServiceUnavailableException(message, exception);
        } catch (ConnectorIOException e) {
            message = MessageFormat.format("Operation {0} failed with ConnectorIOException on system object: {1}",
                    request.getRequestType().toString(), resourceId);
            return new ServiceUnavailableException(message, exception);
        } catch (OperationTimeoutException e) {
            message = MessageFormat.format("Operation {0} Timeout on system object: {1}",
                    request.getRequestType().toString(), resourceId);
            return new ServiceUnavailableException(message, exception);
        } catch (PasswordExpiredException e) {
            message = MessageFormat.format("Operation {0} failed with PasswordExpiredException on system object: {1}",
                    request.getRequestType().toString(), resourceId);
            return new ForbiddenException(message, exception);
        } catch (InvalidPasswordException e) {
            message = MessageFormat.format("Invalid password has been provided to operation {0} for system object: {1}",
                    request.getRequestType().toString(), resourceId);
            return ResourceException.newResourceException(UNAUTHORIZED_ERROR_CODE, message, exception);
        } catch (UnknownUidException e) {
            message = MessageFormat.format("Operation {0} could not find resource {1} on system object: {2}",
                    request.getRequestType().toString(), resourceId, resourceContainer);
            return new NotFoundException(message, exception).setDetail(new JsonValue(new HashMap<String, Object>()));
        } catch (InvalidCredentialException e) {
            message = MessageFormat.format("Invalid credential has been provided to operation {0} for system object: {1}",
                    request.getRequestType().toString(), resourceId);
            return ResourceException.newResourceException(UNAUTHORIZED_ERROR_CODE, message, exception);
        } catch (PermissionDeniedException e) {
            message = MessageFormat.format("Permission was denied on {0} operation for system object: {1}",
                    request.getRequestType().toString(), resourceId);
            return new ForbiddenException(message, exception);
        } catch (ConnectorSecurityException e) {
            message = MessageFormat.format("Operation {0} failed with ConnectorSecurityException on system object: {1}",
                    request.getRequestType().toString(), resourceId);
            return new InternalServerErrorException(message, exception);
        } catch (InvalidAttributeValueException e) {
            message = MessageFormat.format("Attribute value conflicts with the attribute''s schema definition on " +
                            "operation {0} for system object: {1}",
                    request.getRequestType().toString(), resourceId);
            return new BadRequestException(message, exception);
        } catch (PreconditionFailedException e) {
            message = MessageFormat.format("The resource version for {0} does not match the version provided on " +
                            "operation {1} for system object: {2}",
                    resourceId, request.getRequestType().toString(), resourceContainer);
            return new org.forgerock.json.resource.PreconditionFailedException(message, exception);
        } catch (PreconditionRequiredException e) {
            message = MessageFormat.format("No resource version for resource {0} has been provided on operation {1} for system object: {2}",
                    resourceId , request.getRequestType().toString(), resourceContainer);
            return new org.forgerock.json.resource.PreconditionRequiredException(message, exception);
        } catch (RetryableException e) {
            message = MessageFormat.format("Request temporarily unavailable on operation {0} for system object: {1}",
                    request.getRequestType().toString(), resourceId);
            return new ServiceUnavailableException(message, exception);
        } catch (UnsupportedOperationException e) {
            message = MessageFormat.format("Operation {0} is no supported for system object: {1}",
                    request.getRequestType().toString(), resourceId);
            return new NotFoundException(message, exception);
        } catch (IllegalArgumentException e) {
            message = MessageFormat.format("Operation {0} failed with IllegalArgumentException on system object: {1}",
                    request.getRequestType().toString(), resourceId);
            return new InternalServerErrorException(message, e);
        } catch (RemoteWrappedException e) {
            return adaptRemoteWrappedException(context, request, exception, resourceContainer, resourceId,
                    before, after, connectorExceptionActivityLogger);
        } catch (ConnectorException e) {
            message = MessageFormat.format("Operation {0} failed with ConnectorException on system object: {1}",
                    request.getRequestType().toString(), resourceId);
            return new InternalServerErrorException(message, exception);
        } finally {
            // log the ConnectorException
            logger.debug(message, exception);
            try {
                connectorExceptionActivityLogger.log(context, request, message, resourceId,
                        before, after, Status.FAILURE);
            } catch (ResourceException e) {
                // this means the ActivityLogger couldn't log request; log to error log
                logger.warn("Failed to write activity log", e);
            }
        }
    }

    /**
     * .NET Exceptions that may be wrapped in a RemoteWrappedException
     */
    private enum DotNetExceptionHelper {

        ArgumentException("System.ArgumentException") {
            Exception getMappedException(Exception e) {
                return new IllegalArgumentException(e.getMessage(), e.getCause());
            }
        },
        InvalidOperationException("System.InvalidOperationException") {
            Exception getMappedException(Exception e) {
                return new IllegalStateException(e.getMessage(), e.getCause());
            }
        },
        NullReferenceException("System.NullReferenceException") {
            Exception getMappedException(Exception e) {
                return new NullPointerException(e.getMessage());
            }
        },
        NotSupportedException("System.NotSupportedException") {
            Exception getMappedException(Exception e) {
                return new UnsupportedOperationException(e.getMessage(), e.getCause());
            }
        },
        UnknownDotNetException("") {
            Exception getMappedException(Exception e) {
                return new InternalServerErrorException(e.getMessage(), e.getCause());
            }
        };

        private final String exceptionName;

        DotNetExceptionHelper(final String exceptionName) {
            this.exceptionName = exceptionName;
        }

        abstract Exception getMappedException(Exception e);

        ConnectorException getConnectorException(Exception e) {
            return new ConnectorException(e.getMessage(), getMappedException(e));
        }

        static DotNetExceptionHelper fromExceptionClass(String name) {
            for (DotNetExceptionHelper helper : values()) {
                if (helper.exceptionName.equals(name)) {
                    return helper;
                }

            }
            return UnknownDotNetException;
        }
    }

    /**
     * Checks the RemoteWrappedException to determine which Exception has been wrapped and returns
     * the appropriated corresponding exception.
     *
     * @param context the Context from the original request
     * @param request the original request
     * @param exception the ConnectorException that was thrown by the facade
     * @param resourceId the resourceId being operated on
     * @param before the object value "before" the request
     * @param after the object value "after" the request
     * @param connectorExceptionActivityLogger the ActivityLogger to use to log the exception
     */
    private static ResourceException adaptRemoteWrappedException(Context context, Request request,
            ConnectorException exception, String resourceContainer, String resourceId, JsonValue before,
            JsonValue after, ActivityLogger connectorExceptionActivityLogger) {

        RemoteWrappedException remoteWrappedException = (RemoteWrappedException) exception;
        final String message = exception.getMessage();
        final Throwable cause = exception.getCause();

        if (remoteWrappedException.is(AlreadyExistsException.class)) {
            return adaptConnectorException(context, request, new AlreadyExistsException(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(ConfigurationException.class)) {
            return adaptConnectorException(context, request, new ConfigurationException(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(ConnectionBrokenException.class)) {
            return adaptConnectorException(context, request, new ConnectionBrokenException(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(ConnectionFailedException.class)) {
            return adaptConnectorException(context, request, new ConnectionFailedException(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(ConnectorIOException.class)) {
            return adaptConnectorException(context, request, new ConnectorIOException(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(InvalidAttributeValueException.class)) {
            return adaptConnectorException(context, request, new InvalidAttributeValueException(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(InvalidCredentialException.class)) {
            return adaptConnectorException(context, request, new InvalidCredentialException(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(InvalidPasswordException.class)) {
            return adaptConnectorException(context, request, new InvalidPasswordException(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(OperationTimeoutException.class)) {
            return adaptConnectorException(context, request, new OperationTimeoutException(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(PasswordExpiredException.class)) {
            return adaptConnectorException(context, request, new PasswordExpiredException(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(PermissionDeniedException.class)) {
            return adaptConnectorException(context, request, new PermissionDeniedException(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(PreconditionFailedException.class)) {
            return adaptConnectorException(context, request, new PreconditionFailedException(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(PreconditionRequiredException.class)) {
            return adaptConnectorException(context, request, new PreconditionRequiredException(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(RetryableException.class)) {
            return adaptConnectorException(context, request, RetryableException.wrap(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(UnknownUidException.class)) {
            return adaptConnectorException(context, request, new UnknownUidException(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else if (remoteWrappedException.is(ConnectorException.class)) {
            return adaptConnectorException(context, request, new ConnectorException(message, cause),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        } else {
            // handle .NET exceptions
            return adaptConnectorException(context, request,
                    DotNetExceptionHelper.fromExceptionClass(remoteWrappedException.getExceptionClass())
                            .getConnectorException(remoteWrappedException),
                    resourceContainer, resourceId, before, after, connectorExceptionActivityLogger);
        }
    }
}
