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
 * Portions copyright 2011-2016 ForgeRock AS.
 * Portions Copyrighted 2024 3A Systems LLC.
 */
package org.forgerock.openidm.sync.impl;

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Requests.newQueryRequest;
import static org.forgerock.json.resource.Requests.newReadRequest;
import static org.forgerock.json.resource.ResourcePath.*;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openidm.util.ResourceUtil.notSupported;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.audit.events.AuditEvent;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.router.IDMConnectionFactory;
import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.quartz.impl.ExecutionException;
import org.forgerock.openidm.quartz.impl.ScheduledService;
import org.forgerock.openidm.sync.ReconAction;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.forgerock.util.query.QueryFilter;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.component.propertytypes.ServiceVendor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The resource provider for requests on /sync.  Dispatches actions to synchronize between source
 * and targets listed in the synchronization mappings described by the injected Mappings.
 * Also supports invocation as a {@link ScheduledService}.
 */
@Component(
        name = SynchronizationService.PID,
        configurationPolicy = ConfigurationPolicy.IGNORE,
        immediate = true,
        property = {
                Constants.SERVICE_PID + "=" + SynchronizationService.PID,
                ServerConstants.ROUTER_PREFIX + "=/sync/*",
                ServerConstants.SCHEDULED_SERVICE_INVOKE_SERVICE + "=sync"
        })
@ServiceVendor(ServerConstants.SERVER_VENDOR_NAME)
@ServiceDescription("OpenIDM object synchronization service")
public class SynchronizationService implements SingletonResourceProvider, ScheduledService {
    public static final String PID = "org.forgerock.openidm.synchronization";

    /** Actions supported by this service. */
    public enum SyncServiceAction {
        notifyCreate, notifyUpdate, notifyDelete, recon, performAction, getLinkedResources
    }

    /** Logger */
    private final static Logger logger = LoggerFactory.getLogger(SynchronizationService.class);

    /** The resource container action parameter. */
    public static final String ACTION_PARAM_RESOURCE_CONTAINER = "resourceContainer";
    /** The resource id action parameter. */
    public static final String ACTION_PARAM_RESOURCE_ID = "resourceId";
    /** The resource name action parameter. */
    public static final String ACTION_PARAM_RESOURCE_NAME = "resourceName";

    /** The Connection Factory */
    @Reference(policy = ReferencePolicy.STATIC)
    protected IDMConnectionFactory connectionFactory;

    /** Binds the Connection Factory */
    protected void bindConnectionFactory(IDMConnectionFactory connectionFactory) {
    	this.connectionFactory = connectionFactory;
    }
    
    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    @Reference
    Reconcile reconService;

    @Reference
    Mappings mappings;

    /** Enhanced configuration service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile EnhancedConfig enhancedConfig;

    @Activate
    protected void activate(ComponentContext context) {
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
    }

    @Modified
    protected void modified(ComponentContext context) {
    }

    /**
     * Results we can expect from synchronizing a specific source object to a given mapping.
     */
    private enum MappingSyncResult {
        SUCCESSFUL, SKIPPED, FAILED
    }

    /**
     * Local interface to encapsulate the notifyCreate/notifyUpdate/notifyDelete ObjectMapping synchronization
     * across all mappings.
     *
     * @see #syncAllMappings(Context, SyncAction, String, String)
     */
    private interface SyncAction {
        JsonValue sync(Context context, ObjectMapping mapping) throws SynchronizationException;
    }

    /**
     * Synchronize all mappings; keeping track of success/failure conditions.
     *
     * @param action the {@code SyncAction} to perform
     * @param resourceContainer the source object set
     * @param resourceId the source object id
     * @returns a JsonValue list of ObjectMappings' sync results
     * @throws SynchronizationException on failure to sync one of the mappings
     */
    private JsonValue syncAllMappings(Context context, SyncAction action, final String resourceContainer, final String resourceId)
            throws SynchronizationException {
        final JsonValue syncDetails = new JsonValue(new ArrayList<Object>());
        SynchronizationException exceptionPending = null;

        // cannot sync anything with empty resourceId
        if (resourceId.isEmpty()) {
            return syncDetails;
        }

        // mappings that should be synced are those which are enabled and whose
        // source object set matches the resource container
        final Predicate<ObjectMapping> thatMatchSource = new Predicate<ObjectMapping>() {
            public boolean apply(ObjectMapping objectMapping) {
                return objectMapping.isSyncEnabled()
                        && objectMapping.isSourceObject(resourceContainer, resourceId);
            }
        };

        for (final ObjectMapping mapping : FluentIterable.from(mappings).filter(thatMatchSource)) {
            JsonValue mappingResults = json(array());
            MappingSyncResult result = MappingSyncResult.SUCCESSFUL;
            try {
                if (exceptionPending == null) {
                    // No failures yet, perform sync
                    // This operation returns a list which will contain more than one result if 
                    // there are multiple targets to sync the source to
                    mappingResults = action.sync(context, mapping);
                } else {
                    // we've already failed, skip the sync attempt
                    result = MappingSyncResult.SKIPPED;
                }
            } catch (SynchronizationException e) {
                // failed to sync; store the exception and mark as failed
                exceptionPending = new SynchronizationException(e.getMessage(), e.getCause());
                // the exception detail contains the mapping result
                JsonValue failedResult = e.getDetail();
                failedResult.put("cause", exceptionPending.toJsonValue().getObject());
                mappingResults.add(failedResult);
                result = MappingSyncResult.FAILED;
            } finally {
                // Loop over each result, setting result fields and adding to syncDetails list
                for (JsonValue mappingResult : mappingResults) {
                    mappingResult.put("result", result.name());
                    mappingResult.put("mapping", mapping.getName());
                    mappingResult.put("targetObjectSet", mapping.getTargetObjectSet());
                    syncDetails.add(mappingResult);
                }
            }
        }

        // If there was a failure, send the synchronization results along in the exception...
        if (exceptionPending != null) {
            exceptionPending.setDetail(syncDetails);
            throw exceptionPending;
        }

        // ...otherwise, return them
        return syncDetails;
    }

    private JsonValue notifyCreate(Context context, final String resourceContainer, final String resourceId, final JsonValue object)
            throws SynchronizationException {
        // Handle pending link action if present
        PendingAction.handlePendingActions(context, ReconAction.LINK, mappings, resourceContainer, resourceId, object);
        return syncAllMappings(context, new SyncAction() {
            @Override
            public JsonValue sync(Context context, ObjectMapping mapping) throws SynchronizationException {
                return mapping.notifyCreate(context, resourceContainer, resourceId, object);
            }
        }, resourceContainer, resourceId);
    }

    private JsonValue notifyUpdate(Context context, final String resourceContainer, final String resourceId, final JsonValue oldValue, final JsonValue newValue)
            throws SynchronizationException {
        return syncAllMappings(context, new SyncAction() {
            @Override
            public JsonValue sync(Context context, ObjectMapping mapping) throws SynchronizationException {
                return mapping.notifyUpdate(context, resourceContainer, resourceId, oldValue, newValue);
            }
        }, resourceContainer, resourceId);
    }

    private JsonValue notifyDelete(Context context, final String resourceContainer, final String resourceId, final JsonValue oldValue)
            throws SynchronizationException {
        // Handle pending unlink action if present
        PendingAction.handlePendingActions(context, ReconAction.UNLINK, mappings, resourceContainer, resourceId, oldValue);
        return syncAllMappings(context, new SyncAction() {
            @Override
            public JsonValue sync(Context context, ObjectMapping mapping) throws SynchronizationException {
                return mapping.notifyDelete(context, resourceContainer, resourceId, oldValue);
            }
        }, resourceContainer, resourceId);
    }

    /**
     * ScheduledService interface for supporting scheduled recon.
     */
    @Override
    public void execute(Context context, Map<String, Object> scheduledContext) throws ExecutionException {
        try {
            JsonValue params = new JsonValue(scheduledContext).get(CONFIGURED_INVOKE_CONTEXT);
            String action = params.get("action").asString();

            // "reconcile" in schedule config is the legacy equivalent of the action "recon"
            if ("reconcile".equals(action)
                    || ReconciliationService.ReconAction.isReconAction(action)) {
                JsonValue mapping = params.get("mapping");
                ObjectSetContext.push(context);

                // Legacy support for spelling recon action as reconcile
                if ("reconcile".equals(action)) {
                    params.put("_action", ReconciliationService.ReconAction.recon.toString());
                } else {
                    params.put("_action", action);
                }

                try {
                    reconService.reconcile(ReconciliationService.ReconAction.recon, mapping, Boolean.TRUE, params, null);
                } finally {
                    ObjectSetContext.pop();
                }
            } else {
                throw new ExecutionException("Action '" + action +
                        "' configured in schedule not supported.");
            }
        } catch (JsonValueException jve) {
            throw new ExecutionException(jve);
        } catch (ResourceException re) {
            throw new ExecutionException(re);
        }
    }

    @Override
    public void auditScheduledService(final Context context, final AuditEvent auditEvent)
            throws ExecutionException {
        try {
            connectionFactory.getConnection().create(
                    context, Requests.newCreateRequest("audit/access", auditEvent.getValue()));
        } catch (ResourceException e) {
            logger.error("Unable to audit scheduled service {}", auditEvent.toString());
            throw new ExecutionException("Unable to audit scheduled service", e);
        }
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, ActionRequest request) {
        try {
            ObjectSetContext.push(context);
            JsonValue _params = new JsonValue(request.getAdditionalParameters(), new JsonPointer("params"));
            String resourceContainer;
            String resourceId;
            switch (request.getActionAsEnum(SyncServiceAction.class)) {
                case notifyCreate:
                    resourceContainer = _params.get(ACTION_PARAM_RESOURCE_CONTAINER).required().asString();
                    resourceId = _params.get(ACTION_PARAM_RESOURCE_ID).required().asString();
                    logger.debug("Synchronization action=notifyCreate, resourceContainer={}, resourceId={} ", resourceContainer, resourceId);
                    return newActionResponse(notifyCreate(context, resourceContainer, resourceId, request.getContent().get("newValue"))).asPromise();
                case notifyUpdate:
                    resourceContainer = _params.get(ACTION_PARAM_RESOURCE_CONTAINER).required().asString();
                    resourceId = _params.get(ACTION_PARAM_RESOURCE_ID).required().asString();
                    logger.debug("Synchronization action=notifyUpdate, resourceContainer={}, resourceId={}", resourceContainer, resourceId);
                    return newActionResponse(notifyUpdate(context, resourceContainer, resourceId, request.getContent().get("oldValue"), request.getContent().get("newValue"))).asPromise();
                case notifyDelete:
                    resourceContainer = _params.get(ACTION_PARAM_RESOURCE_CONTAINER).required().asString();
                    resourceId = _params.get(ACTION_PARAM_RESOURCE_ID).required().asString();
                    logger.debug("Synchronization action=notifyDelete, resourceContainer={}, resourceId={}", resourceContainer, resourceId);
                    return newActionResponse(notifyDelete(context, resourceContainer, resourceId, request.getContent().get("oldValue"))).asPromise();
                case recon:
                    JsonValue result = new JsonValue(new HashMap<String, Object>());
                    JsonValue mapping = _params.get("mapping").required();
                    logger.debug("Synchronization action=recon, mapping={}", mapping);
                    String reconId = reconService.reconcile(ReconciliationService.ReconAction.recon, mapping, Boolean.TRUE, _params, request.getContent());
                    result.put("reconId", reconId);
                    result.put("_id", reconId);
                    result.put("comment1", "Deprecated API on sync service. Call recon action on recon service instead.");
                    result.put("comment2", "Deprecated return property reconId, use _id instead.");
                    return newActionResponse(result).asPromise();
                case performAction:
                    logger.debug("Synchronization action=performAction, params={}", _params);
                    ObjectMapping objectMapping = mappings.getMapping(_params.get("mapping").required().asString());
                    objectMapping.performAction(_params);
                    return newActionResponse(json(object(field("status", "OK")))).asPromise();
                case getLinkedResources:
                    return getLinkedResources(context, resourcePath(request.getAdditionalParameter(ACTION_PARAM_RESOURCE_NAME)));
                default:
                    throw new BadRequestException("Action" + request.getAction() + " is not supported.");
            }
        } catch (ResourceException e) {
        	return e.asPromise();
        } catch (IllegalArgumentException e) { 
        	// from getActionAsEnum
        	return new BadRequestException(e.getMessage(), e).asPromise();
        } finally {
            ObjectSetContext.pop();
        }
    }

    /**
     * Provide a list of linked resources for the given resourceName.
     * See openidm-zip/src/main/resources/bin/defaults/script/linkedView.js for the format.
     *
     * @param context the request context
     * @param resourceName the full resource name
     * @return an ActionResponse including, as JSON content, the list of linked resources and associated mapping details
     * @throws ResourceException
     */
    private Promise<ActionResponse, ResourceException> getLinkedResources(
            final Context context, ResourcePath resourceName) throws ResourceException {

        // IMPORTANT - Use external connection as this is called externally and we want to read the linked
        // resources with the same permissions / business-logic as if they were done externally as well.
        final Connection connection = connectionFactory.getExternalConnection();
        final String resourceContainer = resourceName.parent().toString();
        final String resourceId = resourceName.leaf();

        final Map<String, ObjectMapping> relatedMappings = new HashMap<>();
        for (ObjectMapping mapping : mappings) {
            // we only care about those mappings which aren't using another mapping's links entry
            // we also only care about those which involve the given component in some way
            if ((mapping.getLinkTypeName() == null
                    || mapping.getLinkTypeName().equals(mapping.getName()))
                    && (mapping.getTargetObjectSet().equals(resourceContainer)
                    || mapping.getSourceObjectSet().equals(resourceContainer))) {
                relatedMappings.put(mapping.getName(), mapping);
            }
        }

        // all links found referring to this resourceId
        final JsonValue allLinks = json(array());
        connection.query(context,
                newQueryRequest("repo/link").setQueryFilter(
                        QueryFilter.or(
                                QueryFilter.equalTo(new JsonPointer("/firstId"), resourceId),
                                QueryFilter.equalTo(new JsonPointer("/secondId"), resourceId))),
                new QueryResourceHandler() {
                    @Override
                    public boolean handleResource(ResourceResponse resourceResponse) {
                        allLinks.add(resourceResponse.getContent().getObject());
                        return true;
                    }
                });

        final List<ResourceResponse> linkedResources = Promises.when(
                // create a batch of Promises from the list of links
                FluentIterable.from(allLinks)
                        .filter(new Predicate<JsonValue>() {
                            // Need to verify that these links we are processing are one of those we know relates to
                            // the given resourceName. It's possible that the link queries above found results for id
                            // values which happen to match the one provided, but are in fact unrelated to the given
                            // resource. This filter guards against that possibility.
                            @Override
                            public boolean apply(JsonValue link) {
                                return relatedMappings.containsKey(link.get("linkType").asString());
                            }
                        })
                        .transform(new Function<JsonValue, Promise<ResourceResponse, ResourceException>>() {
                            // For each of the found links, determine the full linked resourceName and
                            // return some useful information about it by reading it on the router.
                            @Override
                            public Promise<ResourceResponse, ResourceException> apply(final JsonValue link) {
                                final String linkType = link.get("linkType").asString();
                                final String source = relatedMappings.get(linkType).getSourceObjectSet();
                                final String target = relatedMappings.get(linkType).getTargetObjectSet();

                                final ResourcePath linkedResourcePath = (source.equals(resourceContainer))
                                        ? resourcePath(target).child(link.get("secondId").asString())
                                        : resourcePath(source).child(link.get("firstId").asString());

                                final List<Object> relatedMappings = FluentIterable.from(mappings)
                                        .filter(new Predicate<ObjectMapping>() {
                                            @Override
                                            public boolean apply(ObjectMapping mapping) {
                                                return mapping.getName().equals(linkType)
                                                        || mapping.getLinkTypeName().equals(linkType);
                                            }
                                        })
                                        .transform(new Function<ObjectMapping, Object>() {
                                            @Override
                                            public Object apply(ObjectMapping mapping) {
                                                return object(
                                                        field("name", mapping.getName()),
                                                        // the type is how the linkedResourceName relates to the main
                                                        // resourceName in the context of a particular mapping.
                                                        field("type", mapping.isSourceObject(resourceContainer, resourceId)
                                                                ? "target"
                                                                : "source"));
                                            }
                                        })
                                        .toList();

                                // Read the linked resource asynchronously so Promises can be executed in parallel.
                                return connection.readAsync(context, newReadRequest(linkedResourcePath))
                                        // transform the ResourceResponse into the format this endpoint desires
                                        .thenAsync(new AsyncFunction<ResourceResponse, ResourceResponse, ResourceException>() {
                                            @Override
                                            public Promise<ResourceResponse, ResourceException> apply(ResourceResponse resourceResponse)
                                                    throws ResourceException {
                                                JsonValue linkedResource = resourceResponse.getContent();

                                                return newResourceResponse(
                                                        resourceResponse.getId(),
                                                        resourceResponse.getRevision(),
                                                        json(object(
                                                                field("resourceName", linkedResourcePath.toString()),
                                                                field("content", linkedResource.getObject()),
                                                                field("linkQualifier", link.get("linkQualifier").asString()),
                                                                field("linkType", linkType),
                                                                field("mappings", relatedMappings))))
                                                       .asPromise();
                                            }
                                        }, new AsyncFunction<ResourceException, ResourceResponse, ResourceException>() {
                                            @Override
                                            public Promise<ResourceResponse, ResourceException> apply(ResourceException error) throws ResourceException {
                                                return newResourceResponse(
                                                        "0",
                                                        null,
                                                        json(object(
                                                                field("resourceName", linkedResourcePath.toString()),
                                                                field("content", null),
                                                                field("error", error.getMessage()),
                                                                field("linkQualifier", link.get("linkQualifier").asString()),
                                                                field("linkType", linkType),
                                                                field("mappings", relatedMappings))))
                                                        .asPromise();
                                            }
                                        });
                            }
                        })
                        .toList())
                .getOrThrowUninterruptibly(); // wait for promises

        JsonValue response = json(array());
        for (ResourceResponse linked : linkedResources) {
            response.add(linked.getContent().getObject());
        }
        return newActionResponse(response).asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, PatchRequest request) {
        return notSupported(request).asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, ReadRequest request) {
        return notSupported(request).asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, UpdateRequest request) {
        return notSupported(request).asPromise();
    }

	public void bindMappings(SyncMappings mappings2) {
		mappings=mappings2;
	}
}
