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

import org.apache.commons.lang3.StringUtils;
import org.forgerock.guava.common.base.Predicate;
import org.forgerock.guava.common.collect.FluentIterable;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.crypto.JsonCryptoException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryFilters;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openidm.audit.util.NullActivityLogger;
import org.forgerock.openidm.audit.util.Status;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.provisioner.ProvisionerService;
import org.forgerock.openidm.provisioner.openicf.commons.ConnectorUtil;
import org.forgerock.openidm.provisioner.openicf.commons.ObjectClassInfoHelper;
import org.forgerock.openidm.provisioner.openicf.commons.OperationOptionInfoHelper;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Publisher;
import org.forgerock.openidm.util.ContextUtil;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilterVisitor;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.operations.APIOperation;
import org.identityconnectors.framework.api.operations.AuthenticationApiOp;
import org.identityconnectors.framework.api.operations.CreateApiOp;
import org.identityconnectors.framework.api.operations.DeleteApiOp;
import org.identityconnectors.framework.api.operations.GetApiOp;
import org.identityconnectors.framework.api.operations.SearchApiOp;
import org.identityconnectors.framework.api.operations.UpdateApiOp;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.SortKey;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.util.promise.Promises.newResultPromise;

/**
 * Handle request on /system/[systemName]/[objectClass]/{id}
 *
 * Thread-Safe
 */
class ObjectClassResourceProvider implements RequestHandler {

    //Private Constants
    private static final String EVENT_PREFIX = "openidm/internal/system/";
    private static final String RUN_AS_USER = "runAsUser";
    private static final String REAUTH_HEADER = "X-OpenIDM-Reauth-Password";
    private static final String ACCOUNT_USERNAME_ATTRIBUTES = "accountUserNameAttributes";

    private static final Logger logger = LoggerFactory.getLogger(OpenICFProvisionerService.class);

    private static final QueryFilterVisitor<Filter, ObjectClassInfoHelper, JsonPointer> RESOURCE_FILTER =
            new OpenICFFilterAdapter();

    private final ObjectClassInfoHelper objectClassInfoHelper;
    private final Map<Class<? extends APIOperation>, OperationOptionInfoHelper> operations;
    private final String objectClass;
    private final OpenICFProvisionerService provisionerService;
    private final JsonValue jsonConfiguration;

    ObjectClassResourceProvider(String objectClass, ObjectClassInfoHelper objectClassInfoHelper,
            Map<Class<? extends APIOperation>, OperationOptionInfoHelper> operations,
            OpenICFProvisionerService provisionerService,
            JsonValue jsonConfiguration) {
        this.objectClassInfoHelper = objectClassInfoHelper;
        this.operations = operations;
        this.objectClass = objectClass;
        this.provisionerService = provisionerService;
        this.jsonConfiguration = jsonConfiguration;
    }

    /**
     * Checks the {@code operation} permission before execution.
     *
     * @param operation operation for which to return a facade
     * @return if {@code denied} is true and the {@code onDeny} equals
     *         {@link org.forgerock.openidm.provisioner.openicf.commons.OperationOptionInfoHelper.OnActionPolicy#ALLOW}
     *         returns false else true
     * @throws ResourceException
     *             if {@code denied} is true and the {@code onDeny} equals
     *             {@link org.forgerock.openidm.provisioner.openicf.commons.OperationOptionInfoHelper.OnActionPolicy#THROW_EXCEPTION}
     */
    private ConnectorFacade getConnectorFacade0(Class<? extends APIOperation> operation) throws ResourceException {
        final ConnectorFacade facade = provisionerService.getConnectorFacade();
        if (null == facade) {
            throw new ServiceUnavailableException();
        }
        OperationOptionInfoHelper operationOptionInfoHelper = operations.get(operation);

        if (null == facade.getOperation(operation)) {
            throw new NotSupportedException(
                    "Operation " + operation.getCanonicalName() + " is not supported by the Connector");
        } else if (null != operationOptionInfoHelper
                && (null != operationOptionInfoHelper.getSupportedObjectTypes())) {
            if (!operationOptionInfoHelper.getSupportedObjectTypes().contains(
                    objectClassInfoHelper.getObjectClass().getObjectClassValue())) {
                throw new NotSupportedException(
                        "Actions are not supported for resource instances");
            } else if (OperationOptionInfoHelper.OnActionPolicy.THROW_EXCEPTION.equals(
                    operationOptionInfoHelper.getOnActionPolicy())) {
                throw new ForbiddenException(
                        "Operation " + operation.getCanonicalName() + " is configured to be denied");
            }
        }
        return facade;
    }

    private Promise<ActionResponse, ResourceException> handleAuthenticate(Context context, ActionRequest request)
            throws IOException {
        try {
            final ConnectorFacade facade = getConnectorFacade0(AuthenticationApiOp.class);
            final JsonValue params = new JsonValue(request.getAdditionalParameters());
            final String username = params.get("username").required().asString();
            final String password = params.get("password").required().asString();

            OperationOptions operationOptions = operations.get(AuthenticationApiOp.class)
                    .build(jsonConfiguration, objectClassInfoHelper)
                    .build();

            // Throw ConnectorException
            Uid uid = facade.authenticate(objectClassInfoHelper.getObjectClass(), username,
                    new GuardedString(password.toCharArray()), operationOptions);

            JsonValue result = new JsonValue(new HashMap<String, Object>());
            result.put(ResourceResponse.FIELD_CONTENT_ID, uid.getUidValue());
            if (null != uid.getRevision()) {
                result.put(ResourceResponse.FIELD_CONTENT_REVISION, uid.getRevision());
            }
            return newActionResponse(result).asPromise();
        } catch (ConnectorException e) {
            // handle ConnectorException from facade.authenticate:
            // log to activity log only if this is an external request
            // (let internal requests do their own logging upon the handleError...)
            //noinspection ThrowableResultOfMethodCallIgnored
            throw ExceptionHelper.adaptConnectorException(context, request, e, null, null, null, null,
                    ContextUtil.isExternal(context)
                            ? provisionerService.getActivityLogger()
                            : NullActivityLogger.INSTANCE);
        }
    }

    private Promise<ActionResponse, ResourceException> handleLiveSync(
            Context context, ActionRequest request) throws ResourceException {
        final ActionRequest forwardRequest =
                Requests.newActionRequest(ProvisionerService.ROUTER_PREFIX, request.getAction())
                        .setAdditionalParameter("source", provisionerService.getSource(objectClass));

        // forward request to be handled in SystemObjectSetService#actionInstance
        return provisionerService.connectionFactory.getConnection().action(context, forwardRequest).asPromise();

    }

    /**
     * ActionRequest actions we support on /system/[systemName]/[objectClass/{id}
     */
    private enum ObjectClassAction {
        authenticate, resolveUsername, liveSync
    }

    public Promise<ActionResponse, ResourceException> handleAction(
            Context context, ActionRequest request) {
        try {
            switch (request.getActionAsEnum(ObjectClassAction.class)) {
                case authenticate:
                    return handleAuthenticate(context, request);
                case liveSync:
                    return handleLiveSync(context, request);
                default:
                    throw new BadRequestException("Unsupported action: " + request.getAction());
            }
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (JsonValueException e) {
            return new BadRequestException(e.getMessage(), e).asPromise();
        } catch (IllegalArgumentException e) { // from request.getActionAsEnum
            return new BadRequestException(e.getMessage(), e).asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
        try {
            final ConnectorFacade facade = getConnectorFacade0(CreateApiOp.class);
            final Set<Attribute> createAttributes =
                    objectClassInfoHelper.getCreateAttributes(request, provisionerService.getCryptoService());

            OperationOptions operationOptions = operations.get(CreateApiOp.class)
                    .build(jsonConfiguration, objectClassInfoHelper)
                    .build();

            Uid uid = facade.create(objectClassInfoHelper.getObjectClass(),
                    AttributeUtil.filterUid(createAttributes), operationOptions);

            ResourceResponse resource = getCurrentResource(facade, uid, null);
            provisionerService.getActivityLogger().log(context, request, "message",
                    provisionerService.getSource(objectClass, uid.getUidValue()),
                    null, resource.getContent(), Status.SUCCESS);
            return resource.asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (ConnectorException e) {
            return ExceptionHelper.adaptConnectorException(context, request, e,
                    provisionerService.getSource(objectClass),
                    objectClassInfoHelper.getFullResourceId(request), request.getContent(), null,
                    provisionerService.getActivityLogger())
                    .asPromise();
        } catch (JsonValueException e) {
            return new BadRequestException(e.getMessage(), e).asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(
            Context context, DeleteRequest request) {
        String resourceId = objectClassInfoHelper.getFullResourceId(request);
        try {
            if (resourceId.isEmpty()) {
                throw new BadRequestException(
                        "The resource collection " + request.getResourcePath() + " cannot be deleted");
            }
            final ConnectorFacade facade = getConnectorFacade0(DeleteApiOp.class);
            final Uid uid = request.getRevision() != null
                    ? new Uid(resourceId, request.getRevision())
                    : new Uid(resourceId);

            // do a read first (largely for logging)
            ResourceResponse before = getCurrentResource(facade, uid, null);

            OperationOptions operationOptions = operations.get(DeleteApiOp.class)
                    .build(jsonConfiguration, objectClassInfoHelper)
                    .build();

            facade.delete(objectClassInfoHelper.getObjectClass(), uid, operationOptions);

            JsonValue result = before.getContent().copy();
            result.put(ResourceResponse.FIELD_CONTENT_ID, uid.getUidValue());
            if (null != uid.getRevision()) {
                result.put(ResourceResponse.FIELD_CONTENT_REVISION, uid.getRevision());
            }
            provisionerService.getActivityLogger().log(context, request, "message",
                    provisionerService.getSource(objectClass,
                    uid.getUidValue()), before.getContent(), null, Status.SUCCESS);
            return newResourceResponse(uid.getUidValue(), uid.getRevision(), result).asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (ConnectorException e) {
            return ExceptionHelper.adaptConnectorException(context, request, e,
                    provisionerService.getSource(objectClass), resourceId, null, null,
                    provisionerService.getActivityLogger())
                    .asPromise();
        } catch (JsonValueException e) {
            return new BadRequestException(e.getMessage(), e).asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(
            Context context, PatchRequest request) {
        JsonValue beforeValue = null;
        String resourceId = objectClassInfoHelper.getFullResourceId(request);
        try {
            if (resourceId.isEmpty()) {
                throw new BadRequestException(
                        "The resource collection " + request.getResourcePath() + " cannot be patched");
            }

            final ConnectorFacade facade = getConnectorFacade0(UpdateApiOp.class);
            final Uid _uid = request.getRevision() != null
                    ? new Uid(resourceId, request.getRevision())
                    : new Uid(resourceId);

            // read resource before update for logging
            ResourceResponse before = getCurrentResource(facade, _uid, null);
            beforeValue = before.getContent();

            final Set<String> attributeNames = new HashSet<>();
            final Set<Attribute> addedAttributes = new HashSet<>();
            final Set<Attribute> removedAttributes = new HashSet<>();
            final Set<Attribute> updatedAttributes = new HashSet<>();
            for (PatchOperation operation : request.getPatchOperations()) {
                Attribute attribute = objectClassInfoHelper.getPatchAttribute(operation, beforeValue,
                        provisionerService.getCryptoService());
                if (attribute != null) {
                    if (operation.isAdd()) {
                        addedAttributes.add(attribute);
                    } else if (operation.isRemove()) {
                        removedAttributes.add(attribute);
                    } else {
                        updatedAttributes.add(attribute);
                    }
                    attributeNames.add(attribute.getName());
                }
            }

            OperationOptions operationOptions;
            OperationOptionsBuilder operationOptionsBuilder = operations.get(UpdateApiOp.class)
                    .build(jsonConfiguration, objectClassInfoHelper);

            final String reauthPassword = getReauthPassword(context);

            // if reauth and updating attribute requiring user credentials
            if (runAsUser(attributeNames, reauthPassword)) {
                // get username attribute
                final List<String> usernameAttrs =
                        jsonConfiguration.get(ConnectorUtil.OPENICF_CONFIGURATION_PROPERTIES)
                                .get(ACCOUNT_USERNAME_ATTRIBUTES)
                                .asList(String.class);
                final String username = beforeValue.get(usernameAttrs.get(0)).asString();

                if (StringUtils.isNotBlank(username) && reauthPassword != null) {
                    operationOptionsBuilder.setRunAsUser(username)
                            .setRunWithPassword(new GuardedString(reauthPassword.toCharArray()));
                }
            }

            operationOptions = operationOptionsBuilder.build();

            Uid uid = _uid;
            if (addedAttributes.size() > 0) {
                // Perform any add operations
                uid = facade.addAttributeValues(objectClassInfoHelper.getObjectClass(), uid,
                        AttributeUtil.filterUid(addedAttributes), operationOptions);
            }
            if (removedAttributes.size() > 0) {
                // Perform any remove operations
                try {
                    uid = facade.removeAttributeValues(objectClassInfoHelper.getObjectClass(), uid,
                            AttributeUtil.filterUid(removedAttributes), operationOptions);
                } catch (ConnectorException e) {
                    logger.debug("Error removing attribute values for object {}", uid, e);
                }
            }
            if (updatedAttributes.size() > 0) {
                // Perform any increment or replace operations
                uid = facade.update(objectClassInfoHelper.getObjectClass(), uid,
                        AttributeUtil.filterUid(updatedAttributes), operationOptions);
            }

            ResourceResponse resource = getCurrentResource(facade, uid, null);
            provisionerService.getActivityLogger().log(context, request, "message",
                    provisionerService.getSource(objectClass, uid.getUidValue()),
                    beforeValue, resource.getContent(), Status.SUCCESS);
            return resource.asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (ConnectorException e) {
            return ExceptionHelper.adaptConnectorException(context, request, e,
                    provisionerService.getSource(objectClass),
                    resourceId, beforeValue, null, provisionerService.getActivityLogger())
                    .asPromise();
        } catch (JsonValueException e) {
            return new BadRequestException(e.getMessage(), e).asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(
            final Context context, final QueryRequest request, final QueryResourceHandler handler) {
        EventEntry measure = Publisher.start(getQueryEventName( objectClass, request), request, null);
        String resourceId = objectClassInfoHelper.getFullResourceId(request);
        try {
            if (!resourceId.isEmpty()) {
                throw new BadRequestException(
                        "The resource instance " + request.getResourcePath() + " cannot be queried");
            }

            final ConnectorFacade facade = getConnectorFacade0(SearchApiOp.class);
            OperationOptionsBuilder operationOptionsBuilder = operations.get(SearchApiOp.class)
                    .build(jsonConfiguration, objectClassInfoHelper);

            Filter filter = null;

            if (request.getQueryId() != null) {
                if (ServerConstants.QUERY_ALL_IDS.equals(request.getQueryId())) {
                    operationOptionsBuilder.setAttributesToGet(Uid.NAME);
                } else {
                    throw new BadRequestException("Unsupported _queryId: " + request.getQueryId());
                }
            } else if (request.getQueryExpression() != null) {
                filter = QueryFilters.parse(request.getQueryExpression()).accept(
                        RESOURCE_FILTER, objectClassInfoHelper);
            } else if (request.getQueryFilter() != null) {
                // No filtering or query by filter.
                filter = request.getQueryFilter().accept(RESOURCE_FILTER, objectClassInfoHelper);
            } else {
                throw new BadRequestException("One of _queryId, _queryExpression, or _queryFilter is required.");
            }

            // If paged results are requested then decode the cookie in
            // order to determine
            // the index of the first result to be returned.
            final int pageSize = request.getPageSize();
            final String pagedResultsCookie = request.getPagedResultsCookie();
            if (pageSize > 0) {
                operationOptionsBuilder.setPageSize(pageSize);
            }
            if (null != pagedResultsCookie) {
                operationOptionsBuilder.setPagedResultsCookie(pagedResultsCookie);
            }
            operationOptionsBuilder.setPagedResultsOffset(request.getPagedResultsOffset());
            if (null != request.getSortKeys()) {
                List<SortKey> sortKeys = new ArrayList<>(request.getSortKeys().size());
                for (org.forgerock.json.resource.SortKey s: request.getSortKeys()){
                    sortKeys.add(new SortKey(s.getField().leaf(), s.isAscendingOrder()));
                }
                operationOptionsBuilder.setSortKeys(sortKeys);
            }

            // Override ATTRS_TO_GET if fields are specified within the Request
            if (!request.getFields().isEmpty()) {
                objectClassInfoHelper.setAttributesToGet(operationOptionsBuilder, request.getFields());
            }

            final JsonValue logValue = json(array());
            final Exception[] ex = new Exception[] { null };
            SearchResult searchResult = facade.search(objectClassInfoHelper.getObjectClass(), filter,
                    new ResultsHandler() {
                        @Override
                        public boolean handle(ConnectorObject obj) {
                            try {
                                ResourceResponse resource = objectClassInfoHelper.build(obj,
                                        provisionerService.getCryptoService());
                                logValue.add(resource.getContent().getObject());
                                return handler.handleResource(resource);
                            } catch (Exception e) {
                                ex[0] = e;
                                // TODO ICF needs a way to handle exceptions through the facade
                                return false;
                            }
                        }
                    }, operationOptionsBuilder.build());
            if (ex[0] != null) {
                throw new InternalServerErrorException(ex[0].getMessage(), ex[0]);
            }
            provisionerService.getActivityLogger().log(context, request,
                    "query: " + request.getQueryId()
                            + ", queryExpression: " + request.getQueryExpression()
                            + ", queryFilter: " + (request.getQueryFilter() != null ? request.getQueryFilter().toString() : null)
                            + ", parameters: " + request.getAdditionalParameters(),
                    request.getQueryId(), null, logValue, Status.SUCCESS);

            // TODO Support count policy and totalPagedResults
            return newResultPromise(
                    newQueryResponse(searchResult != null ? searchResult.getPagedResultsCookie() : null));
        } catch (EmptyResultSetException e) {
            // cause an empty-result to be returned
            return newResultPromise(newQueryResponse());
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (ConnectorException e) {
            return ExceptionHelper.adaptConnectorException(context, request, e, null, null, null, null,
                    provisionerService.getActivityLogger()).asPromise();
        } catch (IllegalArgumentException | JsonValueException e) {
            return new BadRequestException(e.getMessage(), e).asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        } finally {
            measure.end();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(
            Context context, ReadRequest request) {
        String resourceId = objectClassInfoHelper.getFullResourceId(request);
        try {
            if (resourceId.isEmpty()) {
                throw new BadRequestException(
                        "The resource collection " + request.getResourcePath() + " cannot be read");
            }

            final ConnectorFacade facade = getConnectorFacade0(GetApiOp.class);
            Uid uid = new Uid(resourceId);
            ConnectorObject connectorObject = getConnectorObject(facade, uid, request.getFields());

            if (null != connectorObject) {
                ResourceResponse resource = objectClassInfoHelper.build(connectorObject,
                        provisionerService.getCryptoService());
                provisionerService.getActivityLogger().log(context, request, "message",
                        provisionerService.getSource(objectClass, uid.getUidValue()),
                        resource.getContent(), resource.getContent(), Status.SUCCESS);
                return resource.asPromise();
            } else {
                final String matchedUri = context.containsContext(UriRouterContext.class)
                        ? context.asContext(UriRouterContext.class).getMatchedUri()
                        : "unknown path";
                throw new NotFoundException("Object " + resourceId + " not found on " + matchedUri);

            }
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (ConnectorException e) {
            return ExceptionHelper.adaptConnectorException(context, request, e,
                    provisionerService.getSource(objectClass), resourceId, null, null,
                    provisionerService.getActivityLogger())
                    .asPromise();
        } catch (JsonValueException e) {
            return new BadRequestException(e.getMessage(), e).asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(
            Context context, UpdateRequest request) {
        JsonValue content = request.getContent();
        String resourceId = objectClassInfoHelper.getFullResourceId(request);
        try {
            if (resourceId.isEmpty()) {
                throw new BadRequestException(
                        "The resource collection " + request.getResourcePath() + " cannot be updated");
            }

            final ConnectorFacade facade = getConnectorFacade0(UpdateApiOp.class);
            final Uid _uid = request.getRevision() != null
                    ? new Uid(resourceId, request.getRevision())
                    : new Uid(resourceId);

            // read resource before update for logging
            ResourceResponse before = getCurrentResource(facade, _uid, null);

            // TODO Fix for http://bugster.forgerock.org/jira/browse/CREST-29
            final Name newName = null;
            final Set<Attribute> replaceAttributes =
                    objectClassInfoHelper.getUpdateAttributes(request, newName, provisionerService.getCryptoService());

            OperationOptions operationOptions;
            OperationOptionsBuilder operationOptionsBuilder = operations.get(UpdateApiOp.class)
                    .build(jsonConfiguration, objectClassInfoHelper);

            final String reauthPassword = getReauthPassword(context);

            // if reauth and updating attributes requiring user credentials
            if (runAsUser(content.asMap().keySet(), reauthPassword)) {
                // get username attribute
                final List<String> usernameAttrs =
                        jsonConfiguration.get(ConnectorUtil.OPENICF_CONFIGURATION_PROPERTIES)
                                .get(ACCOUNT_USERNAME_ATTRIBUTES)
                                .asList(String.class);
                final String username = content.get(usernameAttrs.get(0)).asString();

                if (StringUtils.isNotBlank(username) && reauthPassword != null) {
                    operationOptionsBuilder.setRunAsUser(username)
                            .setRunWithPassword(new GuardedString(reauthPassword.toCharArray()));
                }
            }

            operationOptions = operationOptionsBuilder.build();

            Uid uid = facade.update(objectClassInfoHelper.getObjectClass(), _uid,
                    AttributeUtil.filterUid(replaceAttributes), operationOptions);

            ResourceResponse resource = getCurrentResource(facade, uid, null);
            provisionerService.getActivityLogger().log(context, request, "message",
                    provisionerService.getSource(objectClass, uid.getUidValue()),
                    before.getContent(), resource.getContent(), Status.SUCCESS);
            return resource.asPromise();
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (ConnectorException e) {
            return ExceptionHelper.adaptConnectorException(context, request, e,
                        provisionerService.getSource(objectClass),
                    resourceId, content, null, provisionerService.getActivityLogger())
                    .asPromise();
        } catch (JsonValueException e) {
            return new BadRequestException(e.getMessage(), e).asPromise();
        } catch (Exception e) {
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

    // see if there is a reauth password provided
    private String getReauthPassword(Context context) {
        try {
            // get reauth password
            return context.asContext(HttpContext.class).getHeaderAsString(REAUTH_HEADER);
        } catch (Exception e) {
            // there will not always be a HttpContext and this is acceptable so catch exception to
            // prevent the exception from  stopping the remaining update
            return null;
        }
    }

    /**
     * Checks if any of the supplied attributes require re-authentication to update.
     *
     * @param attributes the attributes being updated
     * @param reauthPassword the re-authentication password
     * @return true if a password is re-authentication is required, false otherwise.
     */
    private boolean runAsUser(Set<String> attributes, String reauthPassword) {
        final JsonValue properties = objectClassInfoHelper.getProperties();
        final Predicate<String> attributesToRunAsUser = new Predicate<String>() {
            @Override
            public boolean apply(String attribute) {
                return !properties.get(attribute).isNull()
                        && properties.get(attribute).get(RUN_AS_USER).defaultTo(false).asBoolean();
            }
        };

        return StringUtils.isNotEmpty(reauthPassword)
                && FluentIterable.from(attributes).filter(attributesToRunAsUser).iterator().hasNext();
    }

    private ResourceResponse getCurrentResource(final ConnectorFacade facade,
            final Uid uid, final List<JsonPointer> fields) throws IOException, JsonCryptoException {

        final ConnectorObject co = getConnectorObject(facade, uid, fields);
        if (null != co) {
            return objectClassInfoHelper.build(co, provisionerService.getCryptoService());
        } else {
            JsonValue result = new JsonValue(new HashMap<String, Object>());
            result.put(ResourceResponse.FIELD_CONTENT_ID, uid.getUidValue());
            if (null != uid.getRevision()) {
                result.put(ResourceResponse.FIELD_CONTENT_REVISION, uid.getRevision());
            }
            return newResourceResponse(uid.getUidValue(), uid.getRevision(), result);
        }
    }

    private ConnectorObject getConnectorObject(final ConnectorFacade facade,
            final Uid uid, final List<JsonPointer> fields) throws IOException, JsonCryptoException {

        final OperationOptions operationOptions;
        if (fields == null || fields.isEmpty()) {
            operationOptions = operations.get(GetApiOp.class)
                    .build(jsonConfiguration, objectClassInfoHelper)
                    .build();
        } else {
            OperationOptionsBuilder operationOptionsBuilder = new OperationOptionsBuilder();
            objectClassInfoHelper.setAttributesToGet(operationOptionsBuilder, fields);
            operationOptions = operationOptionsBuilder.build();
        }

        return facade.getObject(objectClassInfoHelper.getObjectClass(), uid, operationOptions);
    }

    /**
     * @return the smartevent Name for a given query
     */
    org.forgerock.openidm.smartevent.Name getQueryEventName(String objectClass, QueryRequest request) {
        String prefix = EVENT_PREFIX + provisionerService.getSystemIdentifierName() + "/" + objectClass + "/query/";

        if (request.getQueryId() != null) {
            return org.forgerock.openidm.smartevent.Name.get(prefix + request.getQueryId());
        } else if (request.getQueryExpression() != null) {
            return org.forgerock.openidm.smartevent.Name.get(prefix + "_query_expression");
        } else if (request.getQueryFilter() != null) {
            return org.forgerock.openidm.smartevent.Name.get(prefix + "_queryFilter");
        } else {
            // This should never happen...
            return org.forgerock.openidm.smartevent.Name.get(prefix + "_UNKNOWN");
        }
    }
}
