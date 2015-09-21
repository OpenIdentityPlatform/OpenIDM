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
 * Portions copyright 2011-2015 ForgeRock AS.
 */
package org.forgerock.openidm.sync.impl;

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.script.ScriptException;

import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.services.context.SecurityContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.audit.util.Status;
import org.forgerock.openidm.config.enhanced.InternalErrorException;
import org.forgerock.openidm.patch.JsonPatch;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Name;
import org.forgerock.openidm.smartevent.Publisher;
import org.forgerock.openidm.sync.ReconAction;
import org.forgerock.openidm.sync.TriggerContext;
import org.forgerock.openidm.sync.impl.Scripts.Script;
import org.forgerock.openidm.util.RequestUtil;
import org.forgerock.script.exception.ScriptThrownException;
import org.forgerock.script.source.SourceUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An ObjectMapping defines policies between source and target objects and their attributes
 * during synchronization and reconciliation.  Mappings can also define triggers for validation,
 * customization, filtering, and transformation of source and target objects.
 */
class ObjectMapping {

    /**
     * Event names for monitoring ObjectMapping behavior
     */
    public static final Name EVENT_CREATE_OBJ = Name.get("openidm/internal/discovery-engine/sync/create-object");
    public static final Name EVENT_SOURCE_ASSESS_SITUATION = Name.get("openidm/internal/discovery-engine/sync/source/assess-situation");
    public static final Name EVENT_SOURCE_DETERMINE_ACTION = Name.get("openidm/internal/discovery-engine/sync/source/determine-action");
    public static final Name EVENT_SOURCE_PERFORM_ACTION = Name.get("openidm/internal/discovery-engine/sync/source/perform-action");
    public static final Name EVENT_CORRELATE_TARGET = Name.get("openidm/internal/discovery-engine/sync/source/correlate-target");
    public static final Name EVENT_UPDATE_TARGET = Name.get("openidm/internal/discovery-engine/sync/update-target");
    public static final Name EVENT_DELETE_TARGET = Name.get("openidm/internal/discovery-engine/sync/delete-target");
    public static final Name EVENT_TARGET_ASSESS_SITUATION = Name.get("openidm/internal/discovery-engine/sync/target/assess-situation");
    public static final Name EVENT_TARGET_DETERMINE_ACTION = Name.get("openidm/internal/discovery-engine/sync/target/determine-action");
    public static final Name EVENT_TARGET_PERFORM_ACTION = Name.get("openidm/internal/discovery-engine/sync/target/perform-action");
    public static final String EVENT_OBJECT_MAPPING_PREFIX = "openidm/internal/discovery-engine/sync/objectmapping/";

    /**
     * Event names for monitoring Reconciliation behavior
     */
    public static final Name EVENT_RECON = Name.get("openidm/internal/discovery-engine/reconciliation");
    public static final Name EVENT_RECON_ID_QUERIES = Name.get("openidm/internal/discovery-engine/reconciliation/id-queries-phase");
    public static final Name EVENT_RECON_SOURCE = Name.get("openidm/internal/discovery-engine/reconciliation/source-phase");
    public static final Name EVENT_RECON_TARGET = Name.get(
            "openidm/internal/discovery-engine/reconciliation/target-phase");

    /** Default number of executor threads to process ReconTasks */
    private static final int DEFAULT_TASK_THREADS = 10;

    /** Logger */
    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectMapping.class);

    /** The mapping name */
    private final String name;

    /** The raw mapping configuration */
    private final JsonValue config;

    /** The name of the links set to use. Defaults to mapping name. */
    private final String linkTypeName;

    /** The link type to use */
    LinkType linkType;

    /**
     * Whether to link source IDs in a case sensitive fashion.
     * Only effective if this mapping defines links, is ignored if this
     * mapping re-uses another mapping's links
     * Default to {@code TRUE}
     */
    private final Boolean sourceIdsCaseSensitive;

    /**
     * Whether to link target IDs in a case sensitive fashion.
     * Only effective if this mapping defines links, is ignored if this
     * mapping re-uses another mapping's links
     * Default to {@code TRUE}
     */
    private final Boolean targetIdsCaseSensitive;

    /** the source system */
    private final String sourceObjectSet;

    /** the target system */
    private String targetObjectSet;

    /** a script that determines if a source object is valid to be mapped */
    private Script validSource;

    /** a script that determines if a target object is valid to be mapped */
    private Script validTarget;
    
    /**
     * A boolean indicating if paging should be used for recon source queries.
     */
    private boolean reconSourceQueryPaging;
    
    /**
     * A page size for recon source queries, if paging is used.
     */
    private int reconSourceQueryPageSize;
    
    /**
     * A container for the correlation queries or script.
     */
    private Correlation correlation;

    /**
     * A {@link List} containing the configured link qualifiers. 
     */
    private Set<String> linkQualifiersList = new HashSet<String>();
    
    /**
     * A {@link Script} used for determining the link qualifiers. 
     */
    private Script linkQualifiersScript = null;
    
    /** a script that applies the effective assignments as part of the mapping */
    private Script defaultMapping;

    /** an array of property-mapping objects */
    private List<PropertyMapping> properties = new ArrayList<PropertyMapping>();

    /**
     * a map of {@link Policy} objects. 
     */
    private Map<String, List<Policy>> policies = new HashMap<String, List<Policy>>();

    /** a script to execute when a target object is to be created */
    private Script onCreateScript;

    /** a script to execute when a target object is to be updated */
    private Script onUpdateScript;

    /** a script to execute when a target object is to be deleted */
    private Script onDeleteScript;

    /** a script to execute when a source object is to be linked to a target object */
    private Script onLinkScript;

    /** a script to execute when a source object and a target object are to be unlinked */
    private Script onUnlinkScript;

    /** a script to execute on each mapping event regardless of the operation */
    private Script resultScript;

    /** an additional set of key-value conditions to be met for a source object to be valid to be mapped */
    private Condition sourceCondition;

    /**
     * Whether existing links should be fetched in one go along with the source and target id lists.
     * false indicates links should be retrieved individually as they are needed.
     */
    private final Boolean prefetchLinks;

    /**
     * Whether when at the outset of correlation the target set is empty (query all ids returns empty),
     * it should try to correlate source entries to target when necessary.
     * Default to {@code FALSE}
     */
    private final Boolean correlateEmptyTargetSet;

    /**
     * Whether to maintain links for sync-d targets
     * Default to {@code TRUE}
     */
    private final Boolean linkingEnabled;

    /** The number of processing threads to use in reconciliation */
    private int taskThreads;

    /** The number of initial tasks the ReconFeeder should submit to executors */
    private int feedSize;

    /** a reference to the {@link SynchronizationService} */
    private final SynchronizationService service;

    /** Whether synchronization (automatic propagation of changes as they are detected) is enabled on that mapping */
    private final Boolean syncEnabled;

    /**
     * Create an instance of a mapping between source and target
     *
     * @param service The associated synchronization service
     * @param config The configuration for this mapping
     * @throws JsonValueException if there is an issue initializing based on the configuration.
     */
    public ObjectMapping(SynchronizationService service, JsonValue config) throws JsonValueException {
        this.service = service;
        this.config = config;
        name = config.get("name").required().asString();
        linkTypeName = config.get("links").defaultTo(name).asString();
        sourceObjectSet = config.get("source").required().asString();
        targetObjectSet = config.get("target").required().asString();
        sourceIdsCaseSensitive = config.get("sourceIdsCaseSensitive").defaultTo(Boolean.TRUE).asBoolean();
        targetIdsCaseSensitive = config.get("targetIdsCaseSensitive").defaultTo(Boolean.TRUE).asBoolean();
        validSource = Scripts.newInstance(config.get("validSource"));
        validTarget = Scripts.newInstance(config.get("validTarget"));
        sourceCondition = new Condition(config.get("sourceCondition"));
        correlation = new Correlation(config);
        JsonValue linkQualifiersValue = config.get("linkQualifiers");
        if (linkQualifiersValue.isNull()) {
            // No link qualifiers configured, so add only the default
            linkQualifiersList.add(Link.DEFAULT_LINK_QUALIFIER);
        } else if (linkQualifiersValue.isList() || linkQualifiersValue.isSet()) {
            linkQualifiersList.addAll(config.get("linkQualifiers").asSet(String.class));
        } else if (linkQualifiersValue.isMap()) {
            linkQualifiersScript = Scripts.newInstance(linkQualifiersValue);
        } else {
            linkQualifiersValue.expect(List.class);
        }
        for (JsonValue jv : config.get("properties").expect(List.class)) {
            properties.add(new PropertyMapping(jv));
        }
        for (JsonValue jv : config.get("policies").expect(List.class)) {
            String situation = jv.get("situation").asString();
            if (policies.containsKey(situation)) {
                List<Policy> policy = policies.get(situation);
                policy.add(new Policy(jv));
                continue;
            }
            List<Policy> policyArrayList = new ArrayList<Policy>();
            policies.put(situation, policyArrayList);
            policyArrayList.add(new Policy(jv));
        }
        defaultMapping = Scripts.newInstance(config.get("defaultMapping").defaultTo(
                json(object(field(SourceUnit.ATTR_TYPE, "text/javascript"),
                    field(SourceUnit.ATTR_NAME, "roles/defaultMapping.js")))));
        onCreateScript = Scripts.newInstance(config.get("onCreate"));
        onUpdateScript = Scripts.newInstance(config.get("onUpdate"));
        onDeleteScript = Scripts.newInstance(config.get("onDelete"));
        onLinkScript = Scripts.newInstance(config.get("onLink"));
        onUnlinkScript = Scripts.newInstance(config.get("onUnlink"));
        resultScript = Scripts.newInstance(config.get("result"));
        prefetchLinks = config.get("prefetchLinks").defaultTo(Boolean.TRUE).asBoolean();
        taskThreads = config.get("taskThreads").defaultTo(DEFAULT_TASK_THREADS).asInteger();
        feedSize = config.get("feedSize").defaultTo(ReconFeeder.DEFAULT_FEED_SIZE).asInteger();
        correlateEmptyTargetSet = config.get("correlateEmptyTargetSet").defaultTo(Boolean.FALSE).asBoolean();
        syncEnabled = config.get("enableSync").defaultTo(Boolean.TRUE).asBoolean();
        linkingEnabled = config.get("enableLinking").defaultTo(Boolean.TRUE).asBoolean();
        reconSourceQueryPaging = config.get("reconSourceQueryPaging").defaultTo(false).asBoolean();
        reconSourceQueryPageSize = config.get("reconSourceQueryPageSize")
                .defaultTo(reconSourceQueryPaging ? ReconFeeder.DEFAULT_FEED_SIZE : 0).asInteger();

        LOGGER.debug("Instantiated {}", name);
    }

    /**
     * Return whether synchronization is enabled for this mapping.
     *
     * @return whether synchronization is enabled for this mapping
     */
    public boolean isSyncEnabled() {
        return syncEnabled.booleanValue();
    }

    /**
     * Return whether linking is enabled for this mapping.
     *
     * @return whether linking is enabled for this mapping
     */
    public boolean isLinkingEnabled() {
        return linkingEnabled.booleanValue();
    }

    /**
     * Mappings can share the same link tables.
     * Establish the relationship between the mappings and determine the proper
     * link type to use
     * @param syncSvc the associated synchronization service
     * @param allMappings The list of all existing mappings
     */
    public void initRelationships(SynchronizationService syncSvc, List<ObjectMapping> allMappings) {
        linkType = LinkType.getLinkType(this, allMappings);
    }

    /**
     * @return the associated synchronization service
     */
    SynchronizationService getService() {
        return service;
    }

    /**
     * @return The name of the object mapping
     */
    public String getName() {
        return name;
    }

    /**
     * @return The raw config associated with the object mapping
     */
    public JsonValue getConfig() {
        return config;
    }
    
    /**
     * Returns the complete set of link Qualifiers.
     * 
     * @return a {@link Set} object representing the complete set of link qualifiers
     * @throws SynchronizationException
     */
    private Set<String> getAllLinkQualifiers() throws SynchronizationException {
        return getLinkQualifiers(null, null, true);
    }
    
    /**
     * Returns a set of link qualifiers for a given source. If the returnAll boolean is used to indicate that all linkQualifiers
     * should be returned regardless of the source object.
     * 
     * @param object the object's value
     * @param oldValue the source object's old value
     * @param returnAll true if all link qualifiers should be returned, false otherwise
     * @return a {@link Set} object representing the complete set of link qualifiers
     * @throws SynchronizationException
     */
    private Set<String> getLinkQualifiers(JsonValue object, JsonValue oldValue, boolean returnAll) throws SynchronizationException {
        if (linkQualifiersScript != null) {
            // Execute script to find the list of link qualifiers
            Map<String, Object> scope = new HashMap<String, Object>();
            scope.put("mapping", name);
            scope.put("object", object == null || object.isNull() ? null : object.asMap());
            scope.put("oldValue", oldValue == null || oldValue.isNull() ? null : oldValue.asMap());
            scope.put("returnAll", returnAll);
            try {
                return json(linkQualifiersScript.exec(scope)).asSet(String.class);
            } catch (ScriptException se) {
                LOGGER.debug("{} {} script encountered exception", name, "linkQualifiers", se);
                throw new SynchronizationException(se);
            }
        } else {
            return linkQualifiersList;
        }
    }
    
    /**
     * Returns an object's required ID or null if the object is null.
     * 
     * @param object a source or target object
     * @return a String representing the object's ID
     */
    private String getObjectId(JsonValue object) {
        return object != null && !object.isNull() 
                ? object.get("_id").required().asString() 
                : null;
    }

    /**
     * @return The configured name of the link set to use for this object mapping
     */
    public String getLinkTypeName() {
        return linkTypeName;
    }

    /**
     * @return The resolved name of the link set to use for this object mapping
     */
    public LinkType getLinkType() {
        return linkType;
    }

    /**
     * @return The mapping source object set
     */
    public String getSourceObjectSet() {
        return sourceObjectSet;
    }

    /**
     * @return The mapping target object set
     */
    public String getTargetObjectSet() {
        return targetObjectSet;
    }

    /**
     * @return The setting for whether to link
     * source IDs in a case sensitive fashion.
     * Only effective if the mapping defines the links,
     * not if the mapping re-uses another mapping's links
     */
    public boolean getSourceIdsCaseSensitive() {
        return sourceIdsCaseSensitive.booleanValue();
    }

    /**
     * @return The setting for whether to link
     * target IDs in a case sensitive fashion.
     * Only effective if the mapping defines the links,
     * not if the mapping re-uses another mapping's links
     */
    public boolean getTargetIdsCaseSensitive() {
        return targetIdsCaseSensitive.booleanValue();
    }

    /**
     * Convenience function with deleted defaulted to false and oldValue defaulted to null
     * @see ObjectMapping#doSourceSync(Context, String, JsonValue, boolean, JsonValue)
     */
    private JsonValue doSourceSync(Context context, String resourceId, JsonValue value) throws SynchronizationException {
        return doSourceSync(context, resourceId, value, false, null);
    }

    /**
     * Source synchronization
     *
     * @param context the Context to use for this request
     * @param resourceId object identifier.
     * @param value null to have it query the source state if applicable,
     *        or JsonValue to tell it the value of the existing source to sync
     * @param sourceDeleted Whether the source object has been deleted
     * @return sync results of the {@link SyncOperation}
     * @throws SynchronizationException if sync-ing fails.
     */
    private JsonValue doSourceSync(Context context, String resourceId, JsonValue value, boolean sourceDeleted, JsonValue oldValue)
            throws SynchronizationException {
        JsonValue results = json(array());
        LOGGER.trace("Start source synchronization of {} {}", resourceId, (value == null) ? "without a value" : "with a value");

        LazyObjectAccessor sourceObjectAccessor = null;
        if (sourceDeleted) {
            sourceObjectAccessor = new LazyObjectAccessor(service, sourceObjectSet, resourceId, null);
        } else if (value != null) {
            value.put("_id", resourceId); // unqualified
            sourceObjectAccessor = new LazyObjectAccessor(service, sourceObjectSet, resourceId, value);
        } else {
            sourceObjectAccessor = new LazyObjectAccessor(service, sourceObjectSet, resourceId);
        }
                
        // Loop over correlation queries, performing a sync for each linkQualifier
        for (String linkQualifier : getLinkQualifiers(sourceObjectAccessor.getObject(), oldValue, false)) {
            // TODO: one day bifurcate this for synchronous and asynchronous source operation
            SourceSyncOperation op = new SourceSyncOperation();
            op.oldValue = oldValue;
            op.setLinkQualifier(linkQualifier);
            op.sourceObjectAccessor = sourceObjectAccessor;

            SyncAuditEventLogger syncAuditEvent = new SyncAuditEventLogger(op, name, context);
            syncAuditEvent.setSourceObjectId(LazyObjectAccessor.qualifiedId(sourceObjectSet, resourceId));

            Status status = Status.SUCCESS;
            try {
                results.add(op.sync());
            } catch (SynchronizationException e) {
                if (op.action != ReconAction.EXCEPTION) {
                    // exception was not intentional, set status to failure
                    status = Status.FAILURE;
                    LOGGER.warn("Unexpected failure during source synchronization", e);
                }
                setLogEntryMessage(syncAuditEvent, e);
                throw e;
            } finally {
                syncAuditEvent.setTargetObjectId(op.getTargetObjectId());
                syncAuditEvent.setLinkQualifier(op.getLinkQualifier());
                syncAuditEvent.setStatus(status);
                logEntry(syncAuditEvent);
            }
        }
        return results;
    }

    /**
     * TODO: Description.
     *
     * @param queryParameters TODO.
     * @return TODO.
     * @throws SynchronizationException TODO.
     */
    private Map<String, Object> queryTargetObjectSet(Map<String, Object> queryParameters)
            throws SynchronizationException {
        try {
            Map<String, Object> result = new HashMap<String, Object>(1);
            final Collection<Object> list = new ArrayList<Object>();
            result.put(QueryResponse.FIELD_RESULT, list);

            QueryRequest request = RequestUtil.buildQueryRequestFromParameterMap(targetObjectSet, queryParameters);
            service.getConnectionFactory().getConnection().query(service.getContext(), request,
                    new QueryResourceHandler() {
                        @Override
                        public boolean handleResource(ResourceResponse resource) {
                            list.add(resource.getContent().asMap());
                            return true;
                        }
                    });
            return result;
        } catch (ResourceException ose) {
            throw new SynchronizationException(ose);
        }
    }

    /**
     * Issues a request to create an object on the target.
     *
     * @param context the Context to use for the request
     * @param target the target object to create.
     * @throws SynchronizationException
     */
    private LazyObjectAccessor createTargetObject(Context context, JsonValue target) throws SynchronizationException {
        EventEntry measure = Publisher.start(EVENT_CREATE_OBJ, target, null);
        LazyObjectAccessor targetObject = null;
        LOGGER.trace("Create target object {}/{}", targetObjectSet, target.get("_id").asString());
        try {
            CreateRequest createRequest =
                    Requests.newCreateRequest(targetObjectSet, target.get("_id").asString(), target);
            ResourceResponse resource =  service.getConnectionFactory().getConnection().create(context, createRequest);
            targetObject = new LazyObjectAccessor(service, targetObjectSet, resource.getId(), resource.getContent());
            measure.setResult(target);
        } catch (JsonValueException jve) {
            throw new SynchronizationException(jve);
        } catch (ResourceException ose) {
            LOGGER.warn("Failed to create target object", ose);
            throw new SynchronizationException(ose);
        } finally {
            measure.end();
        }
        return targetObject;
    }

    /**
     * Issues a request to update an object on the target.
     *
     * @param context the Context to use for the request
     * @param target the target object to create.
     * @throws SynchronizationException
     */
    private void updateTargetObject(Context context, JsonValue target, String targetId) throws SynchronizationException {
        EventEntry measure = Publisher.start(EVENT_UPDATE_TARGET, target, null);
        try {
            final String id = target.get("_id").required().asString();
            final String fullId = LazyObjectAccessor.qualifiedId(targetObjectSet, id);
            if (!targetId.equals(id)) {
                throw new SynchronizationException("target '_id' has changed");
            }
            LOGGER.trace("Update target object {}", fullId);
            UpdateRequest ur = Requests.newUpdateRequest(fullId, target);
            ur.setRevision(target.get("_rev").asString());
            service.getConnectionFactory().getConnection().update(context, ur);
            measure.setResult(target);
        } catch (SynchronizationException se) {
            throw se;
        } catch (JsonValueException jve) {
            throw new SynchronizationException(jve);
        } catch (ResourceException ose) {
            LOGGER.warn("Failed to update target object", ose);
            throw new SynchronizationException(ose);
        } finally {
            measure.end();
        }
    }

    /**
     * Issues a request to delete an object on the target.
     *
     * @param context the Context to use for the request
     * @param target the target object to create.
     * @throws SynchronizationException
     */
    private void deleteTargetObject(Context context, JsonValue target) throws SynchronizationException {
        if (target != null && target.get("_id").isString()) { // forgiving delete
            EventEntry measure = Publisher.start(EVENT_DELETE_TARGET, target, null);
            try {
                DeleteRequest ur = Requests.newDeleteRequest(targetObjectSet, target.get("_id").required().asString());
                ur.setRevision(target.get("_rev").asString());
                LOGGER.trace("Delete target object {}", ur.getResourcePath());
                service.getConnectionFactory().getConnection().delete(context, ur);
            } catch (JsonValueException jve) {
                throw new SynchronizationException(jve);
            } catch (NotFoundException nfe) {
                // forgiving delete
            } catch (ResourceException ose) {
                LOGGER.warn("Failed to delete target object", ose);
                throw new SynchronizationException(ose);
            } finally {
                measure.end();
            }
        }
    }

    /**
     * Apply the configured sync mappings
     *
     * @param source The current source object 
     * @param oldSource an optional previous source object before the change(s) that triggered the sync, 
     * null if not provided
     * @param target the current target object to modify
     * @param existingTarget the full existing target object
     * @throws SynchronizationException if applying the mappings fails.
     */
    private void applyMappings(JsonValue source, JsonValue oldSource, JsonValue target, JsonValue existingTarget, String linkQualifier) throws SynchronizationException {
        EventEntry measure = Publisher.start(getObjectMappingEventName(), source, null);
        try {
            for (PropertyMapping property : properties) {
                property.apply(source, oldSource, target, linkQualifier);
            }
            // Apply default mapping, if configured
            applyDefaultMappings(source, oldSource, target, existingTarget, linkQualifier);
            
            measure.setResult(target);
        } finally {
            measure.end();
        }
    }

    private JsonValue applyDefaultMappings(JsonValue source, JsonValue oldSource, JsonValue target, JsonValue existingTarget, String linkQualifier) throws SynchronizationException {
        JsonValue result = null;
        if (defaultMapping != null) {
            Map<String, Object> queryScope = new HashMap<String, Object>();
            queryScope.put("source", source.asMap());
            if (oldSource != null) {
            	queryScope.put("oldSource", oldSource.asMap());
            }
            queryScope.put("target", target.asMap());
            queryScope.put("config", config.asMap());
            queryScope.put("existingTarget", existingTarget.copy().asMap());
            queryScope.put("linkQualifier", linkQualifier);
            try {
                result = json(defaultMapping.exec(queryScope));
            } catch (ScriptThrownException ste) {
                throw toSynchronizationException(ste, name, "defaultMapping");
            } catch (ScriptException se) {
                LOGGER.debug("{} defaultMapping script encountered exception", name, se);
                throw new SynchronizationException(se);
            }
        }
        return result;
    }

    /**
     * @return an event name for monitoring this object mapping
     */
    private Name getObjectMappingEventName() {
        return Name.get(EVENT_OBJECT_MAPPING_PREFIX + name);
    }

    /**
     * Returns {@code true} if the specified object identifer is in this mapping's source
     * object set.
     */
    public boolean isSourceObject(String resourceContainer, String resourceId) {
        return sourceObjectSet.equals(resourceContainer) && !resourceId.isEmpty();
    }

    /**
     * Notify the target system that an object has been created in a source system.
     *
     * @param resourceContainer source system
     * @param resourceId source object id
     * @param value object to synchronize
     * @return the SynchronizationOperation result details
     * @throws SynchronizationException on failure to synchronize
     */
    public JsonValue notifyCreate(Context context, String resourceContainer, String resourceId, JsonValue value)
            throws SynchronizationException {
        if (isSourceObject(resourceContainer, resourceId)) {
            if (value == null || value.getObject() == null) {
                // notification without the actual value
                value = LazyObjectAccessor.rawReadObject(
                        service.getContext(), service.getConnectionFactory(), resourceContainer, resourceId);
            }
            return doSourceSync(context, resourceId, value); // synchronous for now
        }
        return json(null);
    }

    /**
     * Notify the target system that an object has been updated in a source system.
     *
     * @param resourceContainer source system
     * @param resourceId source object id
     * @param oldValue previous object value
     * @param newValue updated object to synchronize
     * @return
     * @throws SynchronizationException on failure to synchronize
     */
    public JsonValue notifyUpdate(Context context, String resourceContainer, String resourceId, JsonValue oldValue, JsonValue newValue)
            throws SynchronizationException {
        if (isSourceObject(resourceContainer, resourceId)) {
        	if (newValue == null || newValue.getObject() == null) { // notification without the actual value
                newValue = LazyObjectAccessor.rawReadObject(
                        service.getContext(), service.getConnectionFactory(), resourceContainer, resourceId);
            }

            if (oldValue == null || oldValue.getObject() == null || JsonPatch.diff(oldValue, newValue).size() > 0) {
                return doSourceSync(context, resourceId, newValue, false, oldValue); // synchronous for now
            } else {
                LOGGER.trace("There is nothing to update on {}", resourceContainer + "/" + resourceId);
            }
        }
        return json(null);
    }

    /**
     * Notify the target system that an object has been deleted in a source system.
     *
     * @param resourceContainer source system
     * @param resourceId source object id
     * @param oldValue deleted object to synchronize
     * @return
     * @throws SynchronizationException on failure to synchronize
     */
    public JsonValue notifyDelete(Context context, String resourceContainer, String resourceId, JsonValue oldValue)
            throws SynchronizationException {
        if (isSourceObject(resourceContainer, resourceId)) {
            return doSourceSync(context, resourceId, null, true, oldValue); // synchronous for now
        }
        return json(null);
    }

    /**
     * Perform the reconciliation action on a pre-assessed job.
     * <p/>
     * For the input parameters see {@link org.forgerock.openidm.sync.impl.ObjectMapping.SourceSyncOperation#toJsonValue()} or
     * {@link org.forgerock.openidm.sync.impl.ObjectMapping.TargetSyncOperation#toJsonValue()}.
     * <p/>
     * Script example:
     * <pre>
     *     try {
     *          openidm.action('sync',recon.actionParam)
     *     } catch(e) {
     *
     *     };
     * </pre>
     * @param params the input parameters to proceed with the pre-assessed job
     * includes state from previous pre-assessment (source or target sync operation),
     * plus instructions of what to execute. Specifically beyond the pre-asessed state
     * it expects changes to params
     * - action: the desired action to execute
     * - situation (optional): the situation to expect before executing the action.
     * To enforce that the action is only executed if the situation didn't change,
     * supply the situation from the pre-assessment.
     * To attempt execution of the action without enforcing the situation check,
     * supply no situation param
     * @throws SynchronizationException
     */
    public void performAction(JsonValue params) throws SynchronizationException {
        // If reconId is set this action is part of a reconciliation run
        String reconId = params.get("reconId").asString();
        Context context = ObjectSetContext.get();
        if (reconId != null) {
            context = new TriggerContext(context, "recon");
            ObjectSetContext.push(context);
        }

        try {
            ReconAction action = params.get("action").required().asEnum(ReconAction.class);
            SyncOperation op = null;
            ReconAuditEventLogger event = null;
            Status status = Status.SUCCESS;
            try {
                if (params.get("target").isNull()) {
                    SourceSyncOperation sop = new SourceSyncOperation();
                    op = sop;
                    sop.fromJsonValue(params);

                    event = new ReconAuditEventLogger(sop, name, context);
                    event.setLinkQualifier(sop.getLinkQualifier());
                    String sourceObjectId = LazyObjectAccessor.qualifiedId(sourceObjectSet, sop.getSourceObjectId());
                    event.setSourceObjectId(sourceObjectId);
                    if (null == sop.getSourceObject()) {
                        throw new SynchronizationException("Source object " + sourceObjectId + " does not exist");
                    }
                    //TODO blank check
                    String targetId = params.get("targetId").asString();
                    if (null != targetId){
                        op.targetObjectAccessor = new LazyObjectAccessor(service, targetObjectSet, targetId);
                        if (null == sop.getTargetObject()) {
                            throw new SynchronizationException("Target object " + targetId + " does not exist");
                        }
                    }
                    sop.assessSituation();
                } else {
                    TargetSyncOperation top = new TargetSyncOperation();
                    op = top;
                    top.fromJsonValue(params);
                    String targetId = params.get("targetId").required().asString();

                    event = new ReconAuditEventLogger(top, name, context);
                    event.setLinkQualifier(top.getLinkQualifier());
                    String targetObjectId = LazyObjectAccessor.qualifiedId(targetObjectSet, targetId);
                    event.setTargetObjectId(targetObjectId);

                    top.targetObjectAccessor = new LazyObjectAccessor(service, targetObjectSet, targetId);
                    if (null == top.getTargetObject()) {
                        throw new SynchronizationException("Target object " + targetObjectId + " does not exist");
                    }
                    top.assessSituation();
                }
                // IF an expected situation is supplied, compare and reject if current situation changed
                if (params.isDefined("situation")) {
                    Situation situation = params.get("situation").required().asEnum(Situation.class);
                    if (!situation.equals(op.situation)) {
                        throw new SynchronizationException("Expected situation does not match. Expected: " 
                                + situation.name()
                                + ", Found: " + op.situation.name());
                    }
                }
                op.action = action;
                op.performAction();
            } catch (SynchronizationException se) {
                if (op.action != ReconAction.EXCEPTION) {
                    // exception was not intentional
                    status = Status.FAILURE;
                    if (reconId != null) {
                        LOGGER.warn("Unexpected failure during source reconciliation {}", reconId, se);
                    } else {
                        LOGGER.warn("Unexpected failure in performing action {}", params, se);
                    }
                }
                setLogEntryMessage(event, se);
            }
            if (reconId != null && !ReconAction.NOREPORT.equals(
                    action) && (status == Status.FAILURE || op.action != null)) {
                if (op instanceof SourceSyncOperation) {
                    event.setReconciling("source");
                    if (op.getTargetObject() != null) {
                        event.setTargetObjectId(LazyObjectAccessor.qualifiedId(targetObjectSet,
                                op.getTargetObject().get("_id").asString()));
                    }
                    event.setAmbiguousTargetIds(((SourceSyncOperation) op).getAmbiguousTargetIds());
                } else {
                    event.setReconciling("target");
                    if (op.getSourceObject() != null) {
                        event.setSourceObjectId(LazyObjectAccessor.qualifiedId(sourceObjectSet,
                                op.getSourceObject().get("_id").asString()));
                    }
                }
                event.setStatus(status);
                logEntry(event);
            }
        } finally {
            if (reconId != null) {
                ObjectSetContext.pop(); // pop the TriggerContext
            }
        }
    }

    private void doResults(ReconciliationContext reconContext) throws SynchronizationException {
        if (resultScript != null) {
            Map<String, Object> scope = new HashMap<String, Object>();
            scope.put("source", reconContext.getStatistics().getSourceStat().asMap());
            scope.put("target", reconContext.getStatistics().getTargetStat().asMap());
            scope.put("global", reconContext.getStatistics().asMap());
            try {
                resultScript.exec(scope);
            } catch (ScriptThrownException ste) {
                throw toSynchronizationException(ste, name, "result");
            } catch (ScriptException se) {
                LOGGER.debug("{} result script encountered exception", name, se);
                throw new SynchronizationException(se);
            }
        }
    }

    /**
     * Execute a full reconciliation
     *
     * @param reconContext the context specific to the reconciliation run
     * @throws SynchronizationException if any unforseen failure occurs during the reconciliation
     */
    public void recon(ReconciliationContext reconContext) throws SynchronizationException {
        EventEntry measure = Publisher.start(EVENT_RECON, reconContext.getReconId(), null);
        doRecon(reconContext);
        measure.end();
    }

    /**
     * TEMPORARY. Future version will have this break-down into discrete units of work.
     * @param reconContext
     * @throws SynchronizationException
     */
    private void doRecon(ReconciliationContext reconContext) throws SynchronizationException {
        reconContext.getStatistics().reconStart();
        String reconId = reconContext.getReconId();
        EventEntry measureIdQueries = Publisher.start(EVENT_RECON_ID_QUERIES, reconId, null);
        reconContext.setStage(ReconStage.ACTIVE_QUERY_ENTRIES);
        Context context = ObjectSetContext.get();
        try {
            context = new TriggerContext(context, "recon");
            ObjectSetContext.push(context);
            logReconStart(reconContext, context);

            // Get the relevant source (and optionally target) identifiers before we assess the situations
            reconContext.getStatistics().sourceQueryStart();
            
            ReconQueryResult sourceQueryResult = reconContext.querySourceIter(reconSourceQueryPageSize, null);
            Iterator<ResultEntry> sourceIter = sourceQueryResult.getIterator();
            reconContext.getStatistics().sourceQueryEnd();
            if (!sourceIter.hasNext()) {
                if (!reconContext.getReconHandler().allowEmptySourceSet()) {
                    LOGGER.warn("Cannot reconcile from an empty data source, unless allowEmptySourceSet is true.");
                    reconContext.setStage(ReconStage.COMPLETED_FAILED);
                    reconContext.getStatistics().reconEnd();
                    logReconEndFailure(reconContext, context);
                    return;
                }
            }

            // If we will handle a target phase, pre-load all relevant target identifiers
            Collection<String> remainingTargetIds = null;
            ResultIterable targetIterable = null;
            if (reconContext.getReconHandler().isRunTargetPhase()) {
                reconContext.getStatistics().targetQueryStart();
                targetIterable = reconContext.queryTarget();
                remainingTargetIds = targetIterable.getAllIds();
                reconContext.getStatistics().targetQueryEnd();
            } else {
                remainingTargetIds = new ArrayList<String>();
            }

            // Optionally get all links up front as well
            Map<String, Map<String, Link>> allLinks = null;
            if (prefetchLinks) {
                allLinks = new HashMap<String, Map<String, Link>>();
                Integer totalLinkEntries = new Integer(0);
                reconContext.getStatistics().linkQueryStart();
                for (String linkQualifier : getAllLinkQualifiers()) {
                    Map<String, Link> linksByQualifier = Link.getLinksForMapping(ObjectMapping.this, linkQualifier);
                    allLinks.put(linkQualifier, linksByQualifier);
                    totalLinkEntries += linksByQualifier.size();
                }
                reconContext.setTotalLinkEntries(totalLinkEntries);
                reconContext.getStatistics().linkQueryEnd();
            }

            measureIdQueries.end();

            EventEntry measureSource = Publisher.start(EVENT_RECON_SOURCE, reconId, null);
            reconContext.setStage(ReconStage.ACTIVE_RECONCILING_SOURCE);

            reconContext.getStatistics().sourcePhaseStart();
            
            boolean queryNextPage = false;

            LOGGER.info("Performing source sync for recon {} on mapping {}", new Object[] {reconId, name});
            do {
                // Query next page of results if paging
                if (queryNextPage) {
                    LOGGER.debug("Querying next page of source ids");
                    sourceQueryResult = reconContext.querySourceIter(reconSourceQueryPageSize, sourceQueryResult.getPagingCookie());
                    sourceIter = sourceQueryResult.getIterator();
                }
                // Perform source recon phase on current set of source ids
                ReconPhase sourcePhase = new ReconPhase(sourceIter, reconContext, context, allLinks, remainingTargetIds, sourceRecon);
                sourcePhase.setFeedSize(feedSize);
                sourcePhase.execute();
                queryNextPage = true;
            } while (reconSourceQueryPaging && sourceQueryResult.getPagingCookie() != null); // If paging, loop through next pages
            
            reconContext.getStatistics().sourcePhaseEnd();
            measureSource.end();

            LOGGER.debug("Remaining targets after source phase : {}", remainingTargetIds);

            if (reconContext.getReconHandler().isRunTargetPhase()) {
                EventEntry measureTarget = Publisher.start(EVENT_RECON_TARGET, reconId, null);
                reconContext.setStage(ReconStage.ACTIVE_RECONCILING_TARGET);       
                targetIterable.removeNotMatchingEntries(remainingTargetIds);
                reconContext.getStatistics().targetPhaseStart();
                ReconPhase targetPhase = new ReconPhase(targetIterable.iterator(), reconContext, context,
                        allLinks, null, targetRecon);
                targetPhase.setFeedSize(feedSize);
                targetPhase.execute();
                reconContext.getStatistics().targetPhaseEnd();
                measureTarget.end();
            }

            reconContext.getStatistics().reconEnd();
            reconContext.setStage(ReconStage.ACTIVE_PROCESSING_RESULTS);
            doResults(reconContext);
            reconContext.setStage(ReconStage.COMPLETED_SUCCESS);
            logReconEndSuccess(reconContext, context);
        } catch (InterruptedException ex) {
            SynchronizationException syncException;
            if (reconContext.isCanceled()) {
                reconContext.setStage(ReconStage.COMPLETED_CANCELED);
                syncException = new SynchronizationException("Reconciliation canceled: " + reconContext.getReconId());
            }
            else {
                reconContext.setStage(ReconStage.COMPLETED_FAILED);
                syncException = new SynchronizationException("Interrupted execution of reconciliation", ex);
            }
            doResults(reconContext);
            throw syncException;
        } catch (SynchronizationException e) {
            // Make sure that the error did not occur within doResults or last logging for completed success case
            reconContext.setStage(ReconStage.COMPLETED_FAILED);
            if ( reconContext.getStage() != ReconStage.ACTIVE_PROCESSING_RESULTS
                    && reconContext.getStage() != ReconStage.COMPLETED_SUCCESS ) {
                doResults(reconContext);
            }
            reconContext.getStatistics().reconEnd();
            logReconEndFailure(reconContext, context);
            throw new SynchronizationException("Synchronization failed", e);
        } catch (Exception e) {
            reconContext.setStage(ReconStage.COMPLETED_FAILED);
            doResults(reconContext);
            reconContext.getStatistics().reconEnd();
            logReconEndFailure(reconContext, context);
            throw new SynchronizationException("Synchronization failed", e);
        } finally {
            ObjectSetContext.pop(); // pop the TriggerContext
            if (!reconContext.getStatistics().hasEnded()) {
                reconContext.getStatistics().reconEnd();
            }
        }

// TODO: cleanup orphan link objects (no matching source or target) here
    }

    /**
     * Sets the LogEntry message and messageDetail with the appropriate information
     * from the given Exception.
     * 
     * @param entry the LogEntry
     * @param syncException the Exception
     */
    public void setLogEntryMessage(AbstractSyncAuditEventLogger entry, Exception syncException) {
        JsonValue messageDetail = null;  // top level ResourceException
        Throwable cause = syncException; // Root cause
        entry.setException(syncException);
        
        // Loop to find original cause and top level ResourceException (if any)
        while (cause.getCause() != null) {
            cause = cause.getCause();
            if (messageDetail == null && cause instanceof ResourceException) {
                messageDetail = ((ResourceException) cause).toJsonValue();
                // Check if there is a detail field to use
                if (!messageDetail.get("detail").isNull()) {
                	messageDetail = messageDetail.get("detail");
                }
            }
        }
        
        // Set message and messageDetail
        entry.setMessageDetail(messageDetail);
        entry.setMessage(syncException != cause
                ? syncException.getMessage() + ". Root cause: " + cause.getMessage()
                : cause.getMessage());
    }

    /**
     * Reconciliation interface.
     * Implementation is passed to ReconPhase and executed by the ReconTask
     *
     */
    private interface Recon {
        /**
         * Reconcile a given object ID
         * @param id the object id to reconcile
         * @param entry an optional value if the given entry was pre-loaded, or null if not
         * @param reconContext reconciliation context
         * @param rootContext json resource root ctx
         * @param allLinks all links if pre-queried, or null for on-demand link querying
         * @param remainingIds The set to update/remove any targets that were matched
         * @throws SynchronizationException if there is a failure reported in reconciling this id
         */
        void recon(String id, JsonValue entry, ReconciliationContext reconContext, Context rootContext, 
                Map<String, Map<String, Link>> allLinks, Collection<String> remainingIds)  throws SynchronizationException;
    }

    /**
     * Reconcile a given source ID
     */
    private final Recon sourceRecon = new Recon() {
        /**
         * {@inheritDoc}
         */
        @Override
        public void recon(String id, JsonValue objectEntry, ReconciliationContext reconContext, Context context,
                Map<String, Map<String, Link>> allLinks, Collection<String> remainingIds)
                throws SynchronizationException {
            reconContext.checkCanceled();
            LazyObjectAccessor sourceObjectAccessor = objectEntry == null 
                    ? new LazyObjectAccessor(service, sourceObjectSet, id) // Load source detail on demand
                    : new LazyObjectAccessor(service, sourceObjectSet, id, objectEntry); // Pre-queried source detail
            Status status = Status.SUCCESS;

            for (String linkQualifier : getLinkQualifiers(sourceObjectAccessor.getObject(), null, false)) {
                SourceSyncOperation op = new SourceSyncOperation();
                op.reconContext = reconContext;
                op.setLinkQualifier(linkQualifier);

                ReconAuditEventLogger auditEvent = new ReconAuditEventLogger(op, name, context);
                auditEvent.setLinkQualifier(op.getLinkQualifier());
                op.sourceObjectAccessor = sourceObjectAccessor;
                if (allLinks != null) {
                    String normalizedSourceId = linkType.normalizeSourceId(id);
                    op.initializeLink(allLinks.get(linkQualifier).get(normalizedSourceId));
                }
                auditEvent.setSourceObjectId(LazyObjectAccessor.qualifiedId(sourceObjectSet, id));
                op.reconId = reconContext.getReconId();
                try {
                    op.sync();
                } catch (SynchronizationException se) {
                    if (op.action != ReconAction.EXCEPTION) {
                        status = Status.FAILURE; // exception was not intentional
                        LOGGER.warn("Unexpected failure during source reconciliation {}", op.reconId, se);
                    }
                    setLogEntryMessage(auditEvent, se);
                }

                // update statistics with status
                reconContext.getStatistics().processStatus(status);

                String[] targetIds = op.getTargetIds();
                for (String handledId : targetIds) {
                    // If target system has case insensitive IDs, remove without regard to case
                    String normalizedHandledId = linkType.normalizeTargetId(handledId);
                    remainingIds.remove(normalizedHandledId);
                    LOGGER.trace("Removed target from remaining targets: {}", normalizedHandledId);
                }
                if (!ReconAction.NOREPORT.equals(op.action) && (status == Status.FAILURE || op.action != null)) {
                    auditEvent.setReconciling("source");
                    try {
                        if (op.hasTargetObject()) {
                            auditEvent.setTargetObjectId(LazyObjectAccessor.qualifiedId(targetObjectSet,
                                    op.getTargetObjectId()));
                        }
                    } catch (SynchronizationException ex) {
                        auditEvent.setMessage("Failure in preparing recon entry " + ex.getMessage() + " for target: "
                                + op.getTargetObjectId() + " original status: " + status + " " +
                                "message: " + auditEvent.getMessage());
                        status = Status.FAILURE;
                    }
                    auditEvent.setStatus(status);
                    auditEvent.setAmbiguousTargetIds(op.getAmbiguousTargetIds());
                    auditEvent.setReconId(reconContext.getReconId());
                    logEntry(auditEvent);
                }
            }
        }
    };

    /**
     * Reconcile a given target ID
     */
    private final Recon targetRecon = new Recon() {
        /**
         * {@inheritDoc}
         */
        @Override
        public void recon(String id, JsonValue objectEntry, ReconciliationContext reconContext, Context context, Map<String,
                Map<String, Link>> allLinks, Collection<String> remainingIds)  throws SynchronizationException {
            reconContext.checkCanceled();
            for (String linkQualifier : getAllLinkQualifiers()) {
                TargetSyncOperation op = new TargetSyncOperation();
                op.reconContext = reconContext;
                op.setLinkQualifier(linkQualifier);

                ReconAuditEventLogger event = new ReconAuditEventLogger(op, name, context);
                event.setLinkQualifier(op.getLinkQualifier());
                
                if (objectEntry == null) {
                    // Load target detail on demand
                    op.targetObjectAccessor = new LazyObjectAccessor(service, targetObjectSet, id);
                } else {
                    // Pre-queried target detail
                    op.targetObjectAccessor = new LazyObjectAccessor(service, targetObjectSet, id, objectEntry);
                }
                event.setTargetObjectId(LazyObjectAccessor.qualifiedId(targetObjectSet, id));
                op.reconId = reconContext.getReconId();
                Status status = Status.SUCCESS;
                try {
                    op.sync();
                } catch (SynchronizationException se) {
                    if (op.action != ReconAction.EXCEPTION) {
                        status = Status.FAILURE; // exception was not intentional
                        LOGGER.warn("Unexpected failure during target reconciliation {}", reconContext.getReconId(),
                                se);
                    }
                    setLogEntryMessage(event, se);
                }
                // update statistics with status
                reconContext.getStatistics().processStatus(status);

                if (!ReconAction.NOREPORT.equals(op.action) && (status == Status.FAILURE || op.action != null)) {
                    event.setReconciling("target");
                    if (op.getSourceObjectId() != null) {
                        event.setSourceObjectId(
                                LazyObjectAccessor.qualifiedId(sourceObjectSet, op.getSourceObjectId()));
                    }
                    event.setStatus(status);
                    event.setReconId(reconContext.getReconId());
                    logEntry(event);
                }
            }
        }
    };
      
    /**
     * Wrapper to submit source/target recon for a given id for concurrent processing
     */
    class ReconTask implements Callable<Void> {
        String id;
        JsonValue objectEntry;
        ReconciliationContext reconContext;
        Context parentContext;
        Map<String, Map<String, Link>> allLinks;
        Collection<String> remainingIds;
        Recon reconById;

        public ReconTask(ResultEntry resultEntry, ReconciliationContext reconContext, Context parentContext,
                Map<String, Map<String, Link>> allLinks, Collection<String> remainingIds, Recon reconById) {
            this.id = resultEntry.getId();
            // This value is null if it wasn't pre-queried
            this.objectEntry = resultEntry.getValue();
            LOGGER.debug("Recon task on {} {}", id, objectEntry);
            
            this.reconContext = reconContext;
            this.parentContext = parentContext;
            this.allLinks = allLinks;
            this.remainingIds = remainingIds;
            this.reconById = reconById;
        }

        public Void call() throws SynchronizationException {
            //TODO I miss the Request Context
            ObjectSetContext.push(parentContext);
            try {
                reconById.recon(id, objectEntry, reconContext, parentContext, allLinks, remainingIds);
            } finally {
                ObjectSetContext.pop();
            }
            return null;
        }
    }

    /**
     * Reconcile the source/target phase, multi threaded or single threaded.
     */
    class ReconPhase extends ReconFeeder {
        Context parentContext;
        Map<String, Map<String, Link>> allLinks;
        Collection<String> remainingIds;
        Recon reconById;

        public ReconPhase(Iterator<ResultEntry> resultIter, ReconciliationContext reconContext, Context parentContext,
                Map<String, Map<String, Link>> allLinks, Collection<String> remainingIds, Recon reconById) {
            super(resultIter, reconContext);
            this.parentContext = parentContext;
            this.allLinks = allLinks;
            this.remainingIds = remainingIds;
            this.reconById = reconById;
        }
        @Override
        Callable createTask(ResultEntry objectEntry) throws SynchronizationException {
            return new ReconTask(objectEntry, reconContext, parentContext,
                    allLinks, remainingIds, reconById);
        }
    }

    /**
     * @return the configured number of threads to use for processing tasks.
     * 0 to process in a single thread.
     */
    int getTaskThreads() {
        return taskThreads;
    }

    /**
     * Creates an entry in the audit log.
     *
     * @param entry the entry to create
     * @throws SynchronizationException
     */
    private void logEntry(AbstractSyncAuditEventLogger entry) throws SynchronizationException {
        try {
            entry.log(service.getConnectionFactory());
        } catch (ResourceException ose) {
            throw new SynchronizationException(ose);
        }
    }

    /**
     * Record the start of a new reconciliation.
     *
     * @param reconContext
     * @param context
     * @throws SynchronizationException
     */
    private void logReconStart(ReconciliationContext reconContext, Context context)
            throws SynchronizationException {
        ReconAuditEventLogger reconStartEntry = new ReconAuditEventLogger(null, name, context);
        reconStartEntry.setEntryType(ReconAuditEventLogger.RECON_LOG_ENTRY_TYPE_RECON_START);
        reconStartEntry.setReconciliationServiceReconAction(reconContext.getReconAction());
        reconStartEntry.setReconId(reconContext.getReconId());
        reconStartEntry.setMessage("Reconciliation initiated by "
                + context.asContext(SecurityContext.class).getAuthenticationId());
        logEntry(reconStartEntry);

    }

    /**
     * Record the successful completion of a reconciliation.
     *
     * @param reconContext
     * @param context
     * @throws SynchronizationException
     */
    private void logReconEndSuccess(ReconciliationContext reconContext, Context context)
            throws SynchronizationException {
        logReconEnd(reconContext, context, Status.SUCCESS, "Reconciliation completed.");
    }

    /**
     * Record the premature failure of a reconciliation.
     *
     * @param reconContext
     * @param context
     * @throws SynchronizationException
     */
    private void logReconEndFailure(ReconciliationContext reconContext, Context context)
            throws SynchronizationException {
        logReconEnd(reconContext, context, Status.FAILURE, "Reconciliation failed.");
    }

    /**
     * Record a final entry for a reconciliation.
     *
     * @param reconContext
     * @param rootContext
     * @param status
     * @param loggerMessage
     * @throws SynchronizationException
     */
    private void logReconEnd(ReconciliationContext reconContext, Context rootContext, Status status,
            String loggerMessage) throws SynchronizationException {

        ReconAuditEventLogger reconAuditEvent = new ReconAuditEventLogger(null, name, rootContext);
        reconAuditEvent.setEntryType(ReconAuditEventLogger.RECON_LOG_ENTRY_TYPE_RECON_END);
        reconAuditEvent.setReconciliationServiceReconAction(reconContext.getReconAction());
        reconAuditEvent.setStatus(status);
        reconAuditEvent.setReconId(reconContext.getReconId());
        String simpleSummary = reconContext.getStatistics().simpleSummary();
        reconAuditEvent.setMessage(simpleSummary);
        reconAuditEvent.setMessageDetail(json(reconContext.getSummary()));
        logEntry(reconAuditEvent);
        LOGGER.info(loggerMessage + " " + simpleSummary);
    }

    /**
     * Execute a sync engine action explicitly, without going through situation assessment.
     * @param sourceObject the source object if applicable to the action
     * @param targetObject the target object if applicable to the action
     * @param situation an optional situation that was originally assessed. Null if not the result of an earlier situation assessment.
     * @param action the explicit action to invoke
     * @param reconId an optional identifier for the recon context if this is done in the context of reconciliation
     */
    public void explicitOp(JsonValue sourceObject, JsonValue targetObject, Situation situation, ReconAction action, String reconId)
            throws SynchronizationException {
        for (String linkQualifier : getLinkQualifiers(sourceObject, null, false)) {
            ExplicitSyncOperation linkOp = new ExplicitSyncOperation();
            linkOp.setLinkQualifier(linkQualifier);
            linkOp.init(sourceObject, targetObject, situation, action, reconId);
            linkOp.sync();
        }
    }
    
    /**
     * Returns a {@link SynchronizationException} that represents the supplied {@link ScriptThrownException}.

     * @param ste a ScriptThrownException
     * @param type
     * @return
     */
    private SynchronizationException toSynchronizationException(ScriptThrownException ste, String name, String type) {
        String errorMessage = name + " " + type + " script encountered exception";
        LOGGER.debug(errorMessage, ste);
        return new SynchronizationException(ste.toResourceException(ResourceException.INTERNAL_ERROR, errorMessage));
    }

    /**
     * TODO: Description.
     */
    abstract class SyncOperation {

        /** 
         * A reconciliation ID 
         */
        public String reconId;
        
        /** 
         * An optional reconciliation context 
         */
        public ReconciliationContext reconContext;
        
        /** 
         * Access to the source object 
         */
        public LazyObjectAccessor sourceObjectAccessor;
        
        /** 
         * Access to the target object 
         */
        public LazyObjectAccessor targetObjectAccessor;
        
        /** 
         * Optional value of the source object before the change that triggered this sync, or null if not supplied 
         */
        public JsonValue oldValue;

        /**
         * Holds the link representation
         * An initialized link can be interpreted as representing state retrieved from the repository,
         * i.e. a linkObject with id of null represents a link that does not exist (yet)
         */
        public Link linkObject = new Link(ObjectMapping.this);
        
        /** 
         * Indicates whether the link was created during this operation.
         * (linkObject above may not be set for newly created links)
         */
        boolean linkCreated;

        /** 
         * The current sync operation's situation 
         */
        public Situation situation;
        
        /** 
         * The current sync operation's action
         */
        public ReconAction action;
        
        /**
         * The current sync operation's active policy
         */
        public Policy activePolicy = null;
        
        /**
         * An action ID
         */
        public String actionId;
        
        /**
         * A boolean indicating whether to ignore any configured post action.
         */
        public boolean ignorePostAction = false;

        /**
         * Performs the sync operation.
         *
         * @return sync results of the {@link SyncOperation}
         * @throws SynchronizationException TODO.
         */
        public abstract JsonValue sync() throws SynchronizationException;

        protected abstract boolean isSourceToTarget();
        
        /**
         * Sets the link qualifier for the current sync operation.
         * 
         * @param linkQualifier a link qualifier.
         */
        protected void setLinkQualifier(String linkQualifier) {
            this.linkObject.setLinkQualifier(linkQualifier);
        }

        /**
         * @return the source object, potentially loaded on demand and/or cached, or null if does not exist
         * @throws SynchronizationException if on-demand load of the object failed
         */
        protected JsonValue getSourceObject() throws SynchronizationException {
            if (sourceObjectAccessor == null || sourceObjectAccessor.getLocalId() == null) {
                return null;
            } else {
                return sourceObjectAccessor.getObject();
            }
        }

        /**
         * @return the target object, potentially loaded on demand and/or cached, or null if does not exist
         * @throws SynchronizationException if on-demand load of the object failed
         */
        protected JsonValue getTargetObject() throws SynchronizationException {
            if (targetObjectAccessor == null || (!targetObjectAccessor.isLoaded() && targetObjectAccessor.getLocalId() == null)) {
                return null;
            } else {
                return targetObjectAccessor.getObject();
            }
        }

        /**
         * The set unqualified (local) source object ID
         * That a source identifier is set does not automatically imply that the source object exists.
         * @return local identifier of the source object, or null if none
         */
        protected String getSourceObjectId() {
            return sourceObjectAccessor == null ? null : sourceObjectAccessor.getLocalId();
        }

        /**
         * The set unqualified (local) targt object ID
         * That a target identifier is set does not automatically imply that the target object exists.
         * @return local identifier of the target object, or null if none
         */
        protected String getTargetObjectId() {
            return targetObjectAccessor == null ? null : targetObjectAccessor.getLocalId();
        }

        /**
          * @return Whether the target representation is loaded, i.e. the getObject represents what it found.
          * IF a target was not found, the state is loaded with a payload / object of null.
         */
        protected boolean isTargetLoaded() {
            return targetObjectAccessor == null ? false : targetObjectAccessor.isLoaded();
        }
        
        /**
         * Returns the linkQualifier associated with this SyncOperation
         * @return a String representing the linkQualifier
         */
        protected String getLinkQualifier() {
            return linkObject.linkQualifier;
        }

        /**
         * @return Whether the source object exists. May cause the loading of the (lazy) source object,
         * or in the context of reconciliation may check against the bulk existing source/target IDs if
         * the object existed at that point.
         * @throws SynchronizationException if on-demand load of the object failed
         */
        protected boolean hasSourceObject() throws SynchronizationException {
            boolean defined = false;
            if (sourceObjectAccessor == null || sourceObjectAccessor.getLocalId() == null) {
                defined = false;
            } else {
                if (sourceObjectAccessor.isLoaded() && sourceObjectAccessor.getObject() != null) {
                    // Check against already laoded/defined object first, without causing new load
                    defined = true;
                } else if (reconContext != null && reconContext.getSourceIds() != null) {
                    // If available, check against all queried existing IDs
                    defined = reconContext.getSourceIds().contains(sourceObjectAccessor.getLocalId());
                } else {
                    // If no lists of existing ids is available, do a load of the object to check
                    defined = (sourceObjectAccessor.getObject() != null);
                }
            }
            return defined;
        }

        /**
         * @return Whether the target object exists. May cause the loading of the (lazy) source object,
         * or in the context of reconciliation may check against the bulk existing source/target IDs if
         * the object existed at that point.
         * @throws SynchronizationException if on-demand load of the object failed
         */
        protected boolean hasTargetObject() throws SynchronizationException {
            boolean defined = false;

            if (isTargetLoaded()) {
                // Check against already laoded/defined object first, without causing new load
                defined = (targetObjectAccessor.getObject() != null);
            } else if (targetObjectAccessor == null || targetObjectAccessor.getLocalId() == null) {
                // If it's not loaded, but no id to load is available it has no target
                defined = false;
            } else {
                // Either check against a list of all targets, or load to check for existence
                if (reconContext != null && reconContext.getTargets() != null) {
                    // If available, check against all queried existing IDs
                    // If target system has case insensitive IDs, compare without regard to case
                    String normalizedTargetId = linkType.normalizeTargetId(targetObjectAccessor.getLocalId());
                    defined = reconContext.getTargets().containsKey(normalizedTargetId);
                } else {
                    // If no lists of existing ids is available, do a load of the object to check
                    defined = (targetObjectAccessor.getObject() != null);
                }
            }

            return defined;
        }

        /**
         * @return true if it knows there were no objects in the target set during a bulk query
         * at the outset of reconciliation.
         * false if there were objects, or it does not know.
         * Does not take into account objects getting added during reconciliation, or data getting added
         * by another process concurrently
         */
        protected boolean hadEmptyTargetObjectSet() {
            if (reconContext != null && reconContext.getTargets() != null) {
                // If available, check against all queried existing IDs
                return (reconContext.getTargets().size() == 0);
            } else {
                return false;
            }
        }

        /**
         * Returns a String representing the link ID.
         * 
         * @return the found unqualified (local) link ID, null if none
         */
        protected String getLinkId() {
            if (linkObject != null && linkObject.initialized) {
                return linkObject._id;
            } else {
                return null;
            }
        }

        /**
         * Initializes the link representation.
         * 
         * @param link the link object for links that were found/exist in the repository, null to represent no existing link
         */
        protected void initializeLink(Link link) {
            if (link != null) {
                this.linkObject = link;
            } else {
                // Keep track of the fact that we did not find a link
                this.linkObject.clear();
                this.linkObject.initialized = true;
            }
        }

        /**
         * Returns all possible policies for a given situation.
         *  
         * @param situation to get policies for
         * @return List of policies for given situation
         */
        protected List<Policy> getPolicies(Situation situation) {
           return (policies.get(situation.toString()) == null) ? new ArrayList<Policy>() : (policies.get(situation.toString()));
        }
        
        /**
         * Sets the action and active policy based on the current situation.
         *
         * @throws SynchronizationException when cannot determine action from script
         */
        protected void determineAction() throws SynchronizationException {
            if (situation != null) {
                // start with a reasonable default
                action = situation.getDefaultAction();
                List<Policy> situationPolicies = getPolicies(situation);
                for (Policy policy : situationPolicies) {
                    if (policy.getCondition().evaluate(
                            json(object(
                                    field("object", getSourceObject()),
                                    field("linkQualifier", getLinkQualifier()))))) {
                        activePolicy = policy;
                        action = activePolicy.getAction(sourceObjectAccessor, 
                                                targetObjectAccessor, this, getLinkQualifier());
                        break;
                    }
                }
            }
            LOGGER.debug("Determined action to be {}", action);
        }

        /**
         * Performs the current action for this SyncOperation
         *
         * @throws SynchronizationException TODO.
         */
        @SuppressWarnings("fallthrough")
        protected void performAction() throws SynchronizationException {
            switch (action) {
                case CREATE:
                case UPDATE:
                case LINK:
                case DELETE:
                case UNLINK:
                case EXCEPTION:
                    try {
                        Context context = ObjectSetContext.get();
                        actionId = ObjectSetContext.get().getId();
                        switch (action) {
                            case CREATE:
                                if (getSourceObject() == null) {
                                    throw new SynchronizationException("no source object to create target from");
                                }
                                if (getTargetObject() != null) {
                                    throw new SynchronizationException("target object already exists");
                                }
                                JsonValue createTargetObject = json(object());
                                applyMappings(getSourceObject(), oldValue, createTargetObject, json(null), linkObject.linkQualifier); // apply property mappings to target
                                targetObjectAccessor = new LazyObjectAccessor(service, targetObjectSet, createTargetObject.get("_id").asString(), createTargetObject);
                                execScript("onCreate", onCreateScript);

                                // Allow the early link creation as soon as the target identifier is known
                                String sourceId = getSourceObjectId();
                                if (isLinkingEnabled()) {
                                    // Create and populate the PendingActionContext for the LINK action
                                    context = PendingAction.createPendingActionContext(context, ReconAction.LINK, ObjectMapping.this.name, getSourceObject(), reconId, situation);
                                }

                                targetObjectAccessor = createTargetObject(context, createTargetObject);

                                if (!isLinkingEnabled()) {
                                    LOGGER.debug("Linking disabled for {} during {}, skipping additional link processing", sourceId, reconId);
                                    break;
                                }

                                boolean wasLinked = PendingAction.wasPerformed(context, ReconAction.LINK);
                                if (wasLinked) {
                                    linkCreated = true;
                                    LOGGER.debug("Pending link for {} during {} has already been created, skipping additional link processing", sourceId, reconId);
                                    break;
                                } else {
                                    LOGGER.debug("Pending link for {} during {} not yet resolved, proceed to link processing", sourceId, reconId);
                                    PendingAction.clear(context); // We'll now handle link creation ourselves
                                }
                                // falls through to link the newly created target
                            case UPDATE:
                            case LINK:
                                String targetId = getTargetObjectId();
                                if (getTargetObjectId() == null) {
                                    throw new SynchronizationException("no target object to link");
                                }

                                // get a copy of the target before the onLink trigger,
                                // the onUpdate trigger or the mappings are applied
                                JsonValue oldTarget = getTargetObject().copy();

                                if (isLinkingEnabled() && linkObject._id == null) {
                                    try {
                                        createLink(getSourceObjectId(), targetId, reconId);
                                        linkCreated = true;
                                    } catch (SynchronizationException ex) {
                                        // Allow for link to have been created in the meantime, e.g. programmatically
                                        // create would fail with a failed precondition for link already existing
                                        // Try to read again to see if that is the issue
                                        linkObject.getLinkForSource(getSourceObjectId());
                                        if (linkObject._id == null) {
                                            LOGGER.warn("Failed to create link between {} and {}",
                                                    LazyObjectAccessor.qualifiedId(sourceObjectSet, getSourceObjectId()),
                                                    LazyObjectAccessor.qualifiedId(targetObjectSet, targetId),
                                                    ex);
                                            throw ex; // it was a different issue
                                        }
                                    }
                                }
                                if (isLinkingEnabled() && linkObject._id != null && !linkObject.targetEquals(targetId)) {
                                    linkObject.targetId = targetId;
                                    linkObject.update();
                                }
                                // TODO: Detect change of source id, and update link accordingly.
                                if (action == ReconAction.CREATE || action == ReconAction.LINK) {
                                    break; // do not update target
                                }
                                if (getSourceObject() != null && getTargetObject() != null) {
                                    applyMappings(getSourceObject(), oldValue, getTargetObject(), oldTarget, linkObject.linkQualifier);
                                    execScript("onUpdate", onUpdateScript, oldTarget);
                                    if (JsonPatch.diff(oldTarget, getTargetObject()).size() > 0) { // only update if target changes
                                        updateTargetObject(context, getTargetObject(), targetId);
                                    }
                                }
                                break; // terminate UPDATE
                            case DELETE:
                                if (isLinkingEnabled()) {
                                    // Create and populate the PendingActionContext for the UNLINK action
                                    context = PendingAction.createPendingActionContext(context, ReconAction.UNLINK, ObjectMapping.this.name, getSourceObject(), reconId, situation);
                                }
                                if (getTargetObjectId() != null && getTargetObject() != null) { // forgiving; does nothing if no target
                                    execScript("onDelete", onDeleteScript);
                                    deleteTargetObject(context, getTargetObject());
                                    // Represent as not existing anymore so it gets removed from processed targets
                                    targetObjectAccessor = new LazyObjectAccessor(service,
                                            targetObjectSet, getTargetObjectId(), null);
                                }
                                boolean wasUnlinked = PendingAction.wasPerformed(context, ReconAction.UNLINK);
                                if (wasUnlinked) {
                                    LOGGER.debug("Pending unlink for {} during {} has already been performed, skipping additional unlink processing", getTargetObjectId(), reconId);
                                    break;
                                } else {
                                    LOGGER.debug("Pending unlink for {} during {} not yet performed, proceed to unlink processing", getTargetObjectId(), reconId);
                                    PendingAction.clear(context); // We'll now handle unlinking
                                }
                                // falls through to unlink the deleted target
                            case UNLINK:
                                if (linkObject._id != null) { // forgiving; does nothing if no link exists
                                    execScript("onUnlink", onUnlinkScript);
                                    linkObject.delete();
                                }
                                break; // terminate DELETE and UNLINK
                            case EXCEPTION:
                                throw new SynchronizationException("Situation " + situation + " marked as EXCEPTION"); // aborts change; recon reports
                        }
                    } catch (JsonValueException jve) {
                        throw new SynchronizationException(jve);
                    }
                case REPORT:
                case NOREPORT:
                    if (!ignorePostAction) {
                        if (null == activePolicy) {
                            List<Policy> situationPolicies = getPolicies(situation);
                            for (Policy policy : situationPolicies) {
                                // assigns the first policy found, as active policy
                                activePolicy = policy;
                                break;
                            }
                        }
                        postAction(isSourceToTarget());
                    }
                    break;
                case ASYNC:
                case IGNORE:
            }
        }

        /**
         * Evaluates a post action
         * @param sourceAction sourceAction true if the {@link ReconAction} is determined for the {@link SourceSyncOperation}
         * and false if the action is determined for the {@link TargetSyncOperation}.
         * @throws SynchronizationException TODO.
         */
        protected void postAction(boolean sourceAction) throws SynchronizationException {
            if (null != activePolicy) {
                activePolicy.evaluatePostAction(
                                sourceObjectAccessor, targetObjectAccessor, action, sourceAction, getLinkQualifier(), reconId);
            }
        }

        protected void createLink(String sourceId, String targetId, String reconId) throws SynchronizationException {
            Link linkObject = new Link(ObjectMapping.this);
            linkObject.setLinkQualifier(this.linkObject.linkQualifier);
            execScript("onLink", onLinkScript);
            linkObject.sourceId = sourceId;
            linkObject.targetId = targetId;
            linkObject.create();
            initializeLink(linkObject);
            LOGGER.debug("Established link sourceId: {} targetId: {} in reconId: {}", new Object[] {sourceId, targetId, reconId});
        }
        
        /**
         * Evaluates the source condition on the source object
         * 
         * @param linkQualifier the link qualifier for the current sync operation.
         * @return true if the conditions pass, false otherwise
         * @throws SynchronizationException
         */
        protected boolean checkSourceConditions(String linkQualifier) throws SynchronizationException {
        	JsonValue params = json(object(
        			field("source", sourceObjectAccessor.getObject()), 
        			field("linkQualifier", linkQualifier)));
        	return sourceCondition.evaluate(params);
        }

        /**
         * Evaluated source valid on source object
         * @see SyncOperation#isSourceValid(JsonValue)
         */
        protected boolean isSourceValid() throws SynchronizationException {
            return isSourceValid(null);
        }

        /**
         * Evaluates source valid for the supplied sourceObjectOverride, or the source object
         * associated with the sync operation if null
         *
         * @return whether valid for this mapping or not.
         * @throws SynchronizationException if evaluation failed.
         */
        protected boolean isSourceValid(JsonValue sourceObjectOverride) throws SynchronizationException {
            boolean result = false;
            if (hasSourceObject() || sourceObjectOverride != null) { // must have a source object to be valid
                if (validSource != null) {
                    final JsonValue sourceObject = (sourceObjectOverride != null)
                            ? sourceObjectOverride
                            : getSourceObject();
                    if (sourceObject == null) {
                        throw new SynchronizationException("Source object " + getSourceObjectId() + " no longer exists");
                    }
                    
                    Map<String, Object> scope = new HashMap<String, Object>();
                    scope.put("source", sourceObject.asMap());
                    scope.put("linkQualifier", getLinkQualifier());
                    try {
                        Object o = validSource.exec(scope);
                        if (o == null || !(o instanceof Boolean)) {
                            throw new SynchronizationException("Expecting boolean value from validSource");
                        }
                        result = (Boolean) o;
                    } catch (ScriptThrownException ste) {
                        throw toSynchronizationException(ste, name, "validSource");
                    } catch (ScriptException se) {
                        LOGGER.debug("{} validSource script encountered exception", name, se);
                        throw new SynchronizationException(se);
                    }
                } else { // no script means true
                    result = true;
                }
            }
            if (sourceObjectOverride == null) {
                LOGGER.trace("isSourceValid of {} evaluated: {}", getSourceObjectId(), result);
            }
            return result;
        }

        /**
         * TODO: Description.
         *
         * @return TODO.
         * @throws SynchronizationException TODO.
         */
        protected boolean isTargetValid() throws SynchronizationException {
            boolean result = false;
            if (hasTargetObject()) { // must have a target object to qualify
                if (validTarget != null && getTargetObject() != null) { // forces pulling object into memory
                    Map<String, Object> scope = new HashMap<String, Object>();
                    scope.put("target", getTargetObject().asMap());
                    scope.put("linkQualifier", getLinkQualifier());
                    try {
                        Object o = validTarget.exec(scope);
                        if (o == null || !(o instanceof Boolean)) {
                            throw new SynchronizationException("Expecting boolean value from validTarget");
                        }
                        result = (Boolean) o;
                    } catch (ScriptThrownException ste) {
                        throw toSynchronizationException(ste, name, "validTarget");
                    } catch (ScriptException se) {
                        LOGGER.debug("{} validTarget script encountered exception", name, se);
                        throw new SynchronizationException(se);
                    }
                } else { // no script means true
//TODO: is this the right result if getTargetObject was null?
                    result = true;
                }
            }
            LOGGER.trace("isTargetValid of {} evaluated: {}", getTargetObjectId(), result);
            return result;
        }

        /**
         * @see #execScript with oldTarget null
         */
        private void execScript(String type, Script script) throws SynchronizationException {
            execScript(type, script, null);
        }

        /**
         * Executes the given script with the appropriate context information
         *
         * @param type The script hook name
         * @param script The script to execute
         * @param oldTarget optional old target object before any mappings were applied,
         * such as before an update
         * null if not applicable to this script hook
         * @throws SynchronizationException TODO.
         */
        private void execScript(String type, Script script, JsonValue oldTarget) throws SynchronizationException {
            if (script != null) {
                Map<String, Object> scope = new HashMap<String, Object>();
                scope.put("linkQualifier", getLinkQualifier());
                // TODO: Once script engine can do on-demand get replace these forced loads
                if (getSourceObjectId() != null) {
                    JsonValue source = getSourceObject();
                    scope.put("source", null != source ? source.getObject() : null);
                }
                // Target may not have ID yet, e.g. an onCreate with the target object defined,
                // but not stored/id assigned.
                if (isTargetLoaded() || getTargetObjectId() != null) {
                    if (getTargetObject() != null) {
                        scope.put("target", getTargetObject().asMap());
                        if (oldTarget != null) {
                            scope.put("oldTarget", oldTarget.asMap());
                        }
                    }
                }
                if (situation != null) {
                    scope.put("situation", situation.toString());
                }
                try {
                    script.exec(scope);
                } catch (ScriptThrownException se) {
                    throw toSynchronizationException(se, name, type);
                } catch (ScriptException se) {
                    LOGGER.debug("{} script encountered exception", name + " " + type, se);
                    throw new SynchronizationException(new InternalErrorException(name + " " + type + " script encountered exception", se));
                }
            }
        }

        public JsonValue toJsonValue() {
            return json(object(
                    field("reconId", reconId),
                    field("mapping", ObjectMapping.this.getName()),
                    field("situation", situation != null ? situation.name() : null),
                    field("action", situation != null ? situation.getDefaultAction().name() : null),
                    field("sourceId", getSourceObjectId()),
                    field("linkQualifier", linkObject.linkQualifier),
                    (targetObjectAccessor != null && targetObjectAccessor.getLocalId() != null) 
                        ? field("targetId", targetObjectAccessor.getLocalId()) 
                        : null)); 
        }
    }

    /**
     * Explicit execution of a sync operation where the appropriate
     * action is known without having to assess the situation and apply
     * policy to decide the action
     */
    private class ExplicitSyncOperation extends SyncOperation {

        protected boolean isSourceToTarget() {
            //TODO: detect by the source id match
            return true;
        }

        public void init(JsonValue sourceObject, JsonValue targetObject, Situation situation, ReconAction action, String reconId) {
            String sourceObjectId = getObjectId(sourceObject);
            String targetObjectId = getObjectId(targetObject);
            this.sourceObjectAccessor = new LazyObjectAccessor(service, sourceObjectSet, sourceObjectId, sourceObject);
            this.targetObjectAccessor = new LazyObjectAccessor(service, targetObjectSet, targetObjectId, targetObject);
            this.reconId = reconId;
            this.situation = situation;
            this.action = action;
            this.ignorePostAction = true;

            switch (action) {
            case UNLINK:
                try {
                    if (sourceObjectId != null) {
                        linkObject.getLinkForSource(sourceObjectId);
                    } else if (targetObjectId != null) {
                        linkObject.getLinkForTarget(targetObjectId);
                    }
                } catch (SynchronizationException e) {
                    LOGGER.debug("Unable to find link for explicit sync operation UNLINK");
                }
            default:
                break;
            }
        }

        @Override
        public JsonValue sync() throws SynchronizationException {
            LOGGER.debug("Initiate explicit operation call for situation: {}, action: {}", situation, action);
            performAction();
            LOGGER.debug("Complected explicit operation call for situation: {}, action: {}", situation, action);
            return toJsonValue();
        }
    }

    /**
     * TODO: Description.
     */
    class SourceSyncOperation extends SyncOperation {

        // If it can not uniquely identify a target, the list of ambiguous target ids
        public List<String> ambiguousTargetIds;

        @Override
        @SuppressWarnings("fallthrough")
        public JsonValue sync() throws SynchronizationException {
            JsonValue oldTargetValue = json(null);
            try {
                EventEntry measureSituation = Publisher.start(EVENT_SOURCE_ASSESS_SITUATION, getSourceObjectId(), null);
                try {
                    assessSituation();
                } finally {
                    measureSituation.end();
                    if (targetObjectAccessor != null) {
                        oldTargetValue = targetObjectAccessor.getObject();
                    }
                }
                EventEntry measureDetermine = Publisher.start(EVENT_SOURCE_DETERMINE_ACTION, getSourceObjectId(), null);
                boolean linkExisted = (getLinkId() != null);

                try {
                    determineAction();
                } finally {
                    measureDetermine.end();
                }
                EventEntry measurePerform = Publisher.start(EVENT_SOURCE_PERFORM_ACTION, getSourceObjectId(), null);
                try {
                    performAction();
                } finally {
                    measurePerform.end();
                    if (reconContext != null){
                        // The link ID presence after the action can not be interpreted as an indication if the link has been created
                        reconContext.getStatistics().getSourceStat().processed(getSourceObjectId(), getTargetObjectId(),
                                linkExisted, getLinkId(), linkCreated, situation, action);
                    }
                }

                JsonValue syncResult = toJsonValue();
                syncResult.put("oldTargetValue", oldTargetValue != null ? oldTargetValue.getObject() : null);
                return syncResult;
            } catch (SynchronizationException e) {
                JsonValue syncResult = toJsonValue();
                syncResult.put("oldTargetValue", oldTargetValue != null ? oldTargetValue.getObject() : null);
                e.setDetail(syncResult);
                throw e;
            }
        }

        protected boolean isSourceToTarget() {
            return true;
        }

        /**
         * @return all found matching target identifier(s), or a 0 length array if none.
         * More than one target identifier is possible for ambiguous matches
         */
        public String[] getTargetIds() {
            String[] targetIds = null;
            if (ambiguousTargetIds != null) {
                targetIds = ambiguousTargetIds.toArray(new String[ambiguousTargetIds.size()]);
            } else if (getTargetObjectId() != null) {
                targetIds = new String[] { getTargetObjectId() };
            } else {
                targetIds = new String[0];
            }
            return targetIds;
        }

        /**
         * @return the ambiguous target identifier(s), or an empty list if no ambiguous entries are present
         */
        public List getAmbiguousTargetIds() {
            return ambiguousTargetIds;
        }

        private void setAmbiguousTargetIds(JsonValue results) {
            ambiguousTargetIds = new ArrayList<String>(results == null ? 0 : results.size());
            for (JsonValue resultValue : results) {
                String anId = resultValue.get("_id").required().asString();
                ambiguousTargetIds.add(anId);
            }
        }

        public void fromJsonValue(JsonValue params) {
            reconId = params.get("reconId").asString();
            sourceObjectAccessor = new LazyObjectAccessor(service, sourceObjectSet, params.get("sourceId").required().asString());
            ignorePostAction = params.get("ignorePostAction").defaultTo(false).asBoolean();
            linkObject.setLinkQualifier(params.get("linkQualifier").defaultTo(Link.DEFAULT_LINK_QUALIFIER).asString());
        }

        /**
         * TODO: Description.
         *
         * @throws SynchronizationException TODO.
         */
        private void assessSituation() throws SynchronizationException {
            situation = null;
            
            // If linking is disabled, set link to null
            if (!isLinkingEnabled()) {
                initializeLink(null);
            }

            // In case the link was not pre-read get it here
            if (getSourceObjectId() != null && linkObject.initialized == false) {
                linkObject.getLinkForSource(getSourceObjectId());
            }
            
            // If an existing link was found, set the targetObjectAccessor
            if (linkObject._id != null) {
                JsonValue preloaded = null;
                if (reconContext != null) {
                    // If there is a pre-loaded target value, use it
                    if (reconContext.hasTargetsValues()) {
                        preloaded = reconContext.getTargets().get(linkObject.targetId);
                    }
                }
                if (preloaded != null) {
                    targetObjectAccessor = new LazyObjectAccessor(service, targetObjectSet, linkObject.targetId, preloaded);
                } else {
                    targetObjectAccessor = new LazyObjectAccessor(service, targetObjectSet, linkObject.targetId);
                }
            }

            if (!hasSourceObject()) {
                /*
                For sync of delete. For recon these are assessed instead in target phase

                no source, link, target & valid target     -  source missing
                no source, link, target & not valid target - target ignored
                no source, link, no target                 - link only
                no source, no link - can't correlate (no previous object available) - all gone
                no source, no link - (correlate)
                                     no target                       - all gone
                                     1 target & valid (source)       - unassigned
                                     1 target & not valid (source)   - target ignored
                                     > 1 target & valid (source)     - ambiguous
                                     > 1 target & not valid (source) - unqualified
                 */

                if (linkObject._id != null) {
                    if (hasTargetObject()) {
                        if (isTargetValid()) {
                            situation = Situation.SOURCE_MISSING;
                        } else {
                            // target is not valid for this mapping; ignore it
                            situation = Situation.TARGET_IGNORED;
                        }
                    } else {
                        situation = Situation.LINK_ONLY;
                    }
                } else {
                    if (oldValue == null) {
                        // If there is no previous value known we can not correlate
                        situation = Situation.ALL_GONE;
                    } else {
                        // Correlate the old value to potential target(s)
                        JsonValue results = correlateTarget(oldValue);
                        boolean valid = isSourceValid(oldValue);
                        if (results == null || results.size() == 0) {
                            // Results null means no correlation query defined, size 0 we know there is no target
                            situation = Situation.ALL_GONE;
                        } else if (results.size() == 1) {
                            JsonValue resultValue = results.get((Integer) 0).required();
                            targetObjectAccessor = getCorrelatedTarget(resultValue);
                            if (valid) {
                                situation = Situation.UNASSIGNED;
                            } else {
                                // target is not valid for this mapping; ignore it
                                situation = Situation.TARGET_IGNORED;
                            }
                        } else if (results.size() > 1) {
                            if (valid) {
                                // Note this situation is used both when there is a source and a deleted source
                                // with multiple matching targets
                                situation = Situation.AMBIGUOUS;
                            } else {
                                situation = Situation.UNQUALIFIED;
                            }
                        }
                    }
                }
            } else if (isSourceValid() && checkSourceConditions(getLinkQualifier())) { // source is valid for mapping
                if (linkObject._id != null) { // source object linked to target
                    if (hasTargetObject()) {
                        situation = Situation.CONFIRMED;
                    } else {
                        situation = Situation.MISSING;
                    }
                } else { // source object not linked to target
                    JsonValue results = correlateTarget();
                    if (results == null) { // no correlationQuery defined
                        situation = Situation.ABSENT;
                    } else if (results.size() == 1) {
                        JsonValue resultValue = results.get((Integer) 0).required();
                        targetObjectAccessor = getCorrelatedTarget(resultValue);

                        Link checkExistingLink = new Link(ObjectMapping.this);
                        checkExistingLink.getLinkForTarget(targetObjectAccessor.getLocalId());
                        if (checkExistingLink._id == null || checkExistingLink.sourceId == null) {
                            situation = Situation.FOUND;
                        } else {
                            situation = Situation.FOUND_ALREADY_LINKED;
                            // TODO: consider enhancements:
                            // For reporting, should it log existing link and source
                            // What actions should be available for a found, already linked
                        }
                    } else if (results.size() == 0) {
                        situation = Situation.ABSENT;
                    } else {
                        situation = Situation.AMBIGUOUS;
                        setAmbiguousTargetIds(results);
                    }
                }
            } else { // mapping does not qualify for target
                if (linkObject._id != null) {
                    situation = Situation.UNQUALIFIED;
                } else {
                    JsonValue results = correlateTarget();
                    if (results == null || results.size() == 0) {
                        situation = Situation.SOURCE_IGNORED; // source not valid for mapping, and no link or target exist
                    } else if (results.size() == 1) {
                        // TODO: Consider if we can optimize out the read for unqualified conditions
                        JsonValue resultValue = results.get((Integer) 0).required();
                        targetObjectAccessor = getCorrelatedTarget(resultValue);
                        situation = Situation.UNQUALIFIED;
                    } else if (results.size() > 1) {
                        situation = Situation.UNQUALIFIED;
                        setAmbiguousTargetIds(results);
                    }
                }
                if (reconContext != null) {
                    reconContext.getStatistics().getSourceStat().addNotValid(getSourceObjectId());
                }
            }
            LOGGER.debug("Mapping '{}' assessed situation of {} to be {}", new Object[]{name, getSourceObjectId(), situation});
        }

        /**
         * Correlates (finds an associated) target for the source object
         * @see #correlateTarget(JsonValue)
         *
         * @return JsonValue if found, null if none
         * @throws SynchronizationException if the correlation failed.
         */
        private JsonValue correlateTarget() throws SynchronizationException {
            return correlateTarget(null);
        }
        
        /**
         * Correlates (finds an associated) target for the given source
         * @param sourceObjectOverride optional explicitly supplied source object to correlate,
         * or null to use the source object associated with the sync operation
         *
         * @return JsonValue if found, null if none
         * @throws SynchronizationException if the correlation failed.
         */
        @SuppressWarnings("unchecked")
        private JsonValue correlateTarget(JsonValue sourceObjectOverride) throws SynchronizationException {
            JsonValue result = null;
            // TODO: consider if there are cases where this would better be lazy and not get the full target
            if (hasTargetObject()) {
                result = json(array(getTargetObject()));
            } else if (correlation.hasCorrelation(getLinkQualifier()) && (correlateEmptyTargetSet || !hadEmptyTargetObjectSet())) {
                EventEntry measure = Publisher.start(EVENT_CORRELATE_TARGET, getSourceObject(), null);

                Map<String, Object> scope = new HashMap<String, Object>();
                if (sourceObjectOverride != null) {
                    scope.put("source", sourceObjectOverride.asMap());
                } else {
                    scope.put("source", getSourceObject().asMap());
                }
                try {
                    result = correlation.correlate(scope, getLinkQualifier());
                } finally {
                    measure.end();
                }
            }
            return result;
        }

        /**
         * Given a result entry from a correlation query get the full correlated target object
         * @param resultValue an entry from the correlation query result list.
         * May already be the full target object, or just contain the id.
         * @return the target object
         * @throws SynchronizationException
         */
        private LazyObjectAccessor getCorrelatedTarget(JsonValue resultValue) throws SynchronizationException {
            // TODO: Optimize to get the entire object with one query if it's sufficient
            LazyObjectAccessor fullObj = null;
            if (hasNonSpecialAttribute(resultValue.keys())) { //Assume this is a full object
                fullObj = new LazyObjectAccessor(service, targetObjectSet, resultValue.get("_id").required().asString(), resultValue);
            } else {
                fullObj = new LazyObjectAccessor(service, targetObjectSet, resultValue.get("_id").required().asString());
                //fullObj.getObject();
            }
            return fullObj;
        }

        /**
         * Primitive implementation to decide if the object is a "full" or a partial.
         *
         * @param keys attribute names of object
         * @return true if the {@code keys} has value not starting with "_" char
         */
        private boolean hasNonSpecialAttribute(Collection<String> keys) {
            for (String attr : keys) {
                if (!attr.startsWith("_")) {
                    return true;
                }
            }
            return false;
        }
    }
    
    enum CorrelationType {
        correlationQuery,
        correlationScript,
        none
    }
    
    /**
     * A class used to store and execute correlation queries and scripts.
     */
    class Correlation {
        
        /**
         * A Map of correlation queries where the keys are {@link String} instances representing link qualifiers and the 
         * values are the correlation query {@link Script} instances.
         */
        private Map<String, Script> correlationQueries = null;
        
        /**
         * A correlation script which will return a Map object where the keys are {@link String} instances representing link qualifiers and the 
         * values are the correlation query results.
         */
        private Script correlationScript = null;
        
        /**
         * The type of the correlation
         */
        private CorrelationType type;
        
        /**
         * Constructor.
         * 
         * @param config the mapping configuration
         * @throws JsonValueException
         */
        public Correlation(JsonValue config) throws JsonValueException {
            JsonValue correlationQueryValue = config.get("correlationQuery");
            JsonValue correlationScriptValue = config.get("correlationScript");
            if (!correlationQueryValue.isNull() && !correlationScriptValue.isNull()) {
                throw new JsonValueException(config, "Cannot configure both correlationQuery and correlationScript in a single mapping");
            } else if (!correlationQueryValue.isNull()) {
                correlationQueries = new HashMap<String, Script>();
                type = CorrelationType.correlationQuery;
                if (correlationQueryValue.isList()) {
                    for (JsonValue correlationQuery : correlationQueryValue) {
                        correlationQueries.put(correlationQuery.get("linkQualifier").defaultTo(Link.DEFAULT_LINK_QUALIFIER).asString(), 
                                Scripts.newInstance(correlationQuery));
                    }
                } else if (correlationQueryValue.isMap()) {
                    correlationQueries.put(correlationQueryValue.get("linkQualifier").defaultTo(Link.DEFAULT_LINK_QUALIFIER).asString(), 
                            Scripts.newInstance(correlationQueryValue));
                }
            } else if (!correlationScriptValue.isNull()) {
                type = CorrelationType.correlationScript;
                correlationScript = Scripts.newInstance(correlationScriptValue);
            } else {
                type = CorrelationType.none;
            }
        }
        
        /**
         * Returns true if there is a correlation query or script configured for the given link qualifier, false otherwise.
         * 
         * @param linkQualifier the link qualifier for the current sync operation.
         * @return true if correlation is configured, fals otherwise.
         */
        public boolean hasCorrelation(String linkQualifier) {
            switch (type) {
            case correlationQuery:
                return correlationQueries.get(linkQualifier) != null;
            case correlationScript:
                return true;
            default:
                return false;
            }
        }

        /**
         * Performs the correlation.
         * 
         * @param scope the scope to use for the correlation script
         * @param linkQualifier the link qualifier
         * @return a list of results if no correlation is configured
         * @throws SynchronizationException if there was an error during correlation
         */
        public JsonValue correlate(Map<String, Object> scope, String linkQualifier) throws SynchronizationException {
            // Set the link qualifier in the script's scope
            scope.put("linkQualifier", linkQualifier);
            try {
                switch (type) {
                case correlationQuery:
                    // Execute the correlationQuery and return the results
                    return json(queryTargetObjectSet(execScript(type.toString(), correlationQueries.get(linkQualifier), scope).asMap()))
                            .get(QueryResponse.FIELD_RESULT).required();
                case correlationScript:
                    // Execute the correlationScript and return the results corresponding to the given linkQualifier
                    return execScript(type.toString(), correlationScript, scope);
                default:
                    return null;
                }
            } catch (ScriptThrownException ste) {
                throw toSynchronizationException(ste, name, type.toString());
            } catch (ScriptException se) {
                LOGGER.debug("{} {} script encountered exception", name, type.toString(), se);
                throw new SynchronizationException(se);
            }
        }

        /**
         * Executes a script of a given type with the given scope.
         * 
         * @param type the type of script (correlationQuery or correlationScript)
         * @param script the {@link Script} object representing the script
         * @param scope the script's scope
         * @return A {@link Map} representing the results
         * @throws ScriptException if there was an error during execution
         */
        private JsonValue execScript(String type, Script script, Map<String, Object> scope) throws ScriptException {
            Object results = script.exec(scope);
            return json(results);
        }
        
    }

    /**
     * TODO: Description.
     */
    class TargetSyncOperation extends SyncOperation {

        @Override
        public JsonValue sync() throws SynchronizationException {
            try {
                EventEntry measureSituation = Publisher.start(EVENT_TARGET_ASSESS_SITUATION, targetObjectAccessor, null);
                try {
                    assessSituation();
                } finally {
                    measureSituation.end();
                }
                boolean linkExisted = (getLinkId() != null);

                EventEntry measureDetermine = Publisher.start(EVENT_TARGET_DETERMINE_ACTION, targetObjectAccessor, null);
                try {
                    determineAction();
                } finally {
                    measureDetermine.end();
                }
                EventEntry measurePerform = Publisher.start(EVENT_TARGET_PERFORM_ACTION, targetObjectAccessor, null);
                try {
                    // TODO: Option here to just report what action would be performed?
                    performAction();
                } finally {
                    measurePerform.end();
                    if (reconContext != null) {
                        reconContext.getStatistics().getTargetStat().processed(getSourceObjectId(), getTargetObjectId(),
                                linkExisted, getLinkId(), linkCreated, situation, action);
                    }
                }
                return toJsonValue();
            } catch (SynchronizationException e) {
                e.setDetail(toJsonValue());
                throw e;
            }
        }

        protected boolean isSourceToTarget() {
            return false;
        }

        public void fromJsonValue(JsonValue params) {
            reconId = params.get("reconId").asString();
            ignorePostAction = params.get("ignorePostAction").defaultTo(false).asBoolean();
            linkObject.setLinkQualifier(params.get("linkQualifier").defaultTo(Link.DEFAULT_LINK_QUALIFIER).asString());
        }

        public JsonValue toJsonValue() {
            return json(object(
                    field("reconId", reconId),
                    field("mapping", ObjectMapping.this.getName()),
                    field("situation", situation.name()),
                    field("action", situation.getDefaultAction().name()),
                    field("target", "true"),
                    field("linkQualifier", linkObject.linkQualifier),
                    (targetObjectAccessor != null && targetObjectAccessor.getLocalId() != null) 
                        ? field("targetId", targetObjectAccessor.getLocalId()) 
                        : null)); 
        }

        /**
         * TODO: Description.
         *
         * @throws SynchronizationException TODO.
         */
        private void assessSituation() throws SynchronizationException {
            situation = null;
            String targetId = getTargetObjectId();

            // May want to consider an optimization to not query
            // if we don't need the link for the TARGET_IGNORED action
            if (targetId != null) {
                linkObject.getLinkForTarget(targetId);
            }

            if (!isTargetValid()) { // target is not valid for this mapping; ignore it
                situation = Situation.TARGET_IGNORED;
                if (reconContext != null && targetId != null) {
                    reconContext.getStatistics().getTargetStat().addNotValid(targetId);
                }
                return;
            }
            if (linkObject._id == null || linkObject.sourceId == null) {
                situation = Situation.UNASSIGNED;
            } else {
                sourceObjectAccessor = new LazyObjectAccessor(service, sourceObjectSet, linkObject.sourceId);
                if (getSourceObject() == null) { // force load to double check
                    situation = Situation.SOURCE_MISSING;
                } else if (!isSourceValid()) {
                    situation = Situation.UNQUALIFIED; // Should happen rarely done in source phase
                    LOGGER.info("Situation in target reconciliation that indicates source may have changed {} {} {} {}",
                            new Object[] {situation, getSourceObject(), targetId, linkObject});
                } else { // proper link
                    situation = Situation.CONFIRMED; // Should happen rarely as done in source phase
                    LOGGER.info("Situation in target reconciliation that indicates source may have changed {} {} {} {}",
                            new Object[] {situation, getSourceObject(), targetId, linkObject});
                }
            }
        }
    }
}
