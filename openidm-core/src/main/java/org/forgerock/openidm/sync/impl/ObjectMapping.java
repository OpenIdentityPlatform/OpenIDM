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
 * Portions copyright 2011-2017 ForgeRock AS.
 */
package org.forgerock.openidm.sync.impl;

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.JsonValueFunctions.enumConstant;
import static org.forgerock.json.JsonValueFunctions.setOf;
import static org.forgerock.openidm.sync.impl.ReconciliationStatistic.DurationMetric;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.ScriptException;

import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.openidm.sync.SynchronizationException;
import org.forgerock.openidm.sync.impl.cluster.ClusteredReconJobDispatch;
import org.forgerock.openidm.sync.impl.cluster.SchedulerClusteredReconJobDispatch;
import org.forgerock.openidm.sync.impl.cluster.ClusteredSourcePhaseTargetIdRegistry;
import org.forgerock.openidm.sync.impl.cluster.ClusteredSourcePhaseTargetIdRegistryImpl;
import org.forgerock.openidm.sync.impl.cluster.NoOpClusteredSourcePhaseTargetIdRegistry;
import org.forgerock.openidm.sync.impl.cluster.ReconciliationStatisticsPersistence;
import org.forgerock.openidm.sync.impl.cluster.ReconciliationStatisticsPersistenceImpl;
import org.forgerock.openidm.util.DateUtil;
import org.forgerock.openidm.util.DurationStatistics;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.services.context.SecurityContext;
import org.forgerock.openidm.audit.util.Status;
import org.forgerock.openidm.config.enhanced.InternalErrorException;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Name;
import org.forgerock.openidm.smartevent.Publisher;
import org.forgerock.openidm.sync.ReconAction;
import org.forgerock.openidm.sync.TriggerContext;
import org.forgerock.openidm.util.Script;
import org.forgerock.openidm.util.Scripts;
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
    static final Name EVENT_CREATE_OBJ = Name.get("openidm/internal/discovery-engine/sync/create-object");
    static final Name EVENT_SOURCE_ASSESS_SITUATION = Name.get("openidm/internal/discovery-engine/sync/source/assess-situation");
    static final Name EVENT_SOURCE_DETERMINE_ACTION = Name.get("openidm/internal/discovery-engine/sync/source/determine-action");
    static final Name EVENT_SOURCE_PERFORM_ACTION = Name.get("openidm/internal/discovery-engine/sync/source/perform-action");
    static final Name EVENT_CORRELATE_TARGET = Name.get("openidm/internal/discovery-engine/sync/source/correlate-target");
    static final Name EVENT_UPDATE_TARGET = Name.get("openidm/internal/discovery-engine/sync/update-target");
    static final Name EVENT_DELETE_TARGET = Name.get("openidm/internal/discovery-engine/sync/delete-target");
    static final Name EVENT_TARGET_ASSESS_SITUATION = Name.get("openidm/internal/discovery-engine/sync/target/assess-situation");
    static final Name EVENT_TARGET_DETERMINE_ACTION = Name.get("openidm/internal/discovery-engine/sync/target/determine-action");
    static final Name EVENT_TARGET_PERFORM_ACTION = Name.get("openidm/internal/discovery-engine/sync/target/perform-action");
    static final String EVENT_OBJECT_MAPPING_PREFIX = "openidm/internal/discovery-engine/sync/objectmapping/";

    /**
     * Event names for monitoring Reconciliation behavior
     */
    static final Name EVENT_RECON = Name.get("openidm/internal/discovery-engine/reconciliation");
    static final Name EVENT_RECON_ID_QUERIES = Name.get("openidm/internal/discovery-engine/reconciliation/id-queries-phase");
    static final Name EVENT_RECON_SOURCE = Name.get("openidm/internal/discovery-engine/reconciliation/source-phase");
    static final Name EVENT_RECON_SOURCE_PAGE = Name.get("openidm/internal/discovery-engine/reconciliation/source-phase");
    static final Name EVENT_RECON_TARGET = Name.get("openidm/internal/discovery-engine/reconciliation/target-phase");

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
    protected LinkType linkType;

    /**
     * Whether to link source IDs in a case sensitive fashion.
     * Only effective if this mapping defines links, is ignored if this
     * mapping re-uses another mapping's links
     * Default to {@code TRUE}
     */
    private final boolean sourceIdsCaseSensitive;

    /**
     * Whether to link target IDs in a case sensitive fashion.
     * Only effective if this mapping defines links, is ignored if this
     * mapping re-uses another mapping's links
     * Default to {@code TRUE}
     */
    private final boolean targetIdsCaseSensitive;

    /** the source system */
    private final String sourceObjectSet;

    /** the target system */
    private String targetObjectSet;
    
    /**
     * A boolean indicating if paging should be used for recon source queries.
     */
    private boolean reconSourceQueryPaging;
    
    /**
     * A page size for recon source queries, if paging is used.
     */
    private int reconSourceQueryPageSize;

    /**
     * A {@link List} containing the configured link qualifiers. 
     */
    private Set<String> linkQualifiersList = new HashSet<>();
    
    /**
     * A {@link Script} used for determining the link qualifiers. 
     */
    private Script linkQualifiersScript;

    /** a script to execute when a reconciliation process has started */
    private Script onReconScript;

    /** a script to execute on each mapping event regardless of the operation */
    private Script resultScript;

    /**
     * Whether existing links should be fetched in one go along with the source and target id lists.
     * false indicates links should be retrieved individually as they are needed.
     */
    private final boolean prefetchLinks;

    /**
     * Whether to maintain links for sync-d targets
     * Default to {@code TRUE}
     */
    private final boolean linkingEnabled;

    /** The number of processing threads to use in reconciliation */
    private int taskThreads;

    /** The number of initial tasks the ReconFeeder should submit to executors */
    private int feedSize;

    /** a reference to the {@link ConnectionFactory} */
    private final ConnectionFactory connectionFactory;

    /** Whether synchronization (automatic propagation of changes as they are detected) is enabled on that mapping */
    private final boolean syncEnabled;

    /**
     * Reconcile a given source ID
     */
    private final Recon sourceRecon = new SourceRecon(this);

    /**
     * Reconcile a given target ID
     */
    private final Recon targetRecon = new TargetRecon(this);

    /** Whether source recon pages should be distributed across cluster nodes. */
    private final boolean clusteredSourceReconEnabled;

    /**
     * Create an instance of a mapping between source and target
     *
     * @param connectionFactory The ConnectionFactory
     * @param config The configuration for this mapping
     * @throws JsonValueException if there is an issue initializing based on the configuration.
     */
    public ObjectMapping(ConnectionFactory connectionFactory, JsonValue config) throws JsonValueException {
        this.connectionFactory = connectionFactory;
        this.config = config;
        name = config.get("name").required().asString();
        linkTypeName = config.get("links").defaultTo(name).asString();
        sourceObjectSet = config.get("source").required().asString();
        targetObjectSet = config.get("target").required().asString();
        sourceIdsCaseSensitive = config.get("sourceIdsCaseSensitive").defaultTo(true).asBoolean();
        targetIdsCaseSensitive = config.get("targetIdsCaseSensitive").defaultTo(true).asBoolean();
        clusteredSourceReconEnabled = config.get("clusteredSourceReconEnabled").defaultTo(false).asBoolean();
        JsonValue linkQualifiersValue = config.get("linkQualifiers");
        if (linkQualifiersValue.isNull()) {
            // No link qualifiers configured, so add only the default
            linkQualifiersList.add(Link.DEFAULT_LINK_QUALIFIER);
        } else if (linkQualifiersValue.isList()) {
            linkQualifiersList.addAll(config.get("linkQualifiers").as(setOf(String.class)));
        } else if (linkQualifiersValue.isMap()) {
            linkQualifiersScript = Scripts.newScript(linkQualifiersValue);
        } else {
            linkQualifiersValue.expect(List.class);
        }
        onReconScript = Scripts.newScript(config.get("onRecon").defaultTo(
                json(object(field(SourceUnit.ATTR_TYPE, "groovy"),
                    field(SourceUnit.ATTR_NAME, "roles/onRecon.groovy")))));
        resultScript = Scripts.newScript(config.get("result"));
        prefetchLinks = config.get("prefetchLinks").defaultTo(true).asBoolean();
        taskThreads = config.get("taskThreads").defaultTo(DEFAULT_TASK_THREADS).asInteger();
        feedSize = config.get("feedSize").defaultTo(ReconFeeder.DEFAULT_FEED_SIZE).asInteger();
        syncEnabled = config.get("enableSync").defaultTo(true).asBoolean();
        linkingEnabled = config.get("enableLinking").defaultTo(true).asBoolean();
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
        return syncEnabled;
    }

    /**
     * Return whether linking is enabled for this mapping.
     *
     * @return whether linking is enabled for this mapping
     */
    public boolean isLinkingEnabled() {
        return linkingEnabled;
    }

    /**
     * Mappings can share the same link tables.
     * Establish the relationship between the mappings and determine the proper
     * link type to use
     *
     * @param allMappings The list of all existing mappings
     */
    public void initRelationships(List<ObjectMapping> allMappings) {
        linkType = LinkType.getLinkType(this, allMappings);
    }

    /**
     * @return the associated synchronization service
     */
    ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    /**
     * @return The name of the object mapping
     */
    public String getName() {
        return name;
    }

    int getFeedSize() {
        return feedSize;
    }

    Recon getSourceRecon() {
        return sourceRecon;
    }

    Recon getTargetRecon() {
        return targetRecon;
    }

    int getReconSourceQueryPageSize() {
        return reconSourceQueryPageSize;
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
     * @param context {@link Context} associated with the current sync.
     * @param reconContext Recon context or {@code null}
     * @return a {@link Set} object representing the complete set of link qualifiers
     * @throws SynchronizationException
     */
    Set<String> getAllLinkQualifiers(Context context, ReconciliationContext reconContext)
            throws SynchronizationException {
        return getLinkQualifiers(null, null, true, context, reconContext);
    }
    
    /**
     * Returns a set of link qualifiers for a given source. If the returnAll boolean is used to indicate that all linkQualifiers
     * should be returned regardless of the source object.
     *
     * @param object the object's value
     * @param oldValue the source object's old value
     * @param returnAll true if all link qualifiers should be returned, false otherwise
     * @param context {@link Context} associated with the current sync.
     * @param reconContext Recon context or {@code null}
     * @return a {@link Set} object representing the complete set of link qualifiers
     * @throws SynchronizationException
     */
    Set<String> getLinkQualifiers(JsonValue object, JsonValue oldValue, boolean returnAll, Context context,
            ReconciliationContext reconContext) throws SynchronizationException {
        if (linkQualifiersScript != null) {
            // Execute script to find the list of link qualifiers
            Map<String, Object> scope = new HashMap<>();
            scope.put("mapping", name);
            scope.put("object", object == null || object.isNull() ? null : object.asMap());
            scope.put("oldValue", oldValue == null || oldValue.isNull() ? null : oldValue.asMap());
            scope.put("returnAll", returnAll);

            final long startNanoTime = startNanoTime(reconContext);
            try {
                return json(linkQualifiersScript.exec(scope, context)).as(setOf(String.class));
            } catch (ScriptException se) {
                LOGGER.debug("{} {} script encountered exception", name, "linkQualifiers", se);
                throw new SynchronizationException(se);
            } finally {
                addDuration(reconContext, DurationMetric.linkQualifiersScript, startNanoTime);
            }
        } else {
            return linkQualifiersList;
        }
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
        return sourceIdsCaseSensitive;
    }

    /**
     * @return The setting for whether to link target IDs in a case sensitive fashion.
     * Only effective if the mapping defines the links, not if the mapping re-uses another mapping's links
     */
    public boolean getTargetIdsCaseSensitive() {
        return targetIdsCaseSensitive;
    }

    /**
     * @return The setting for whether to clustered
     * source recon is enabled.
     */
    boolean getClusteredSourceReconEnabled() {
        return clusteredSourceReconEnabled;
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
    private JsonValue doSourceSync(Context context, String resourceId, JsonValue value, boolean sourceDeleted, 
            JsonValue oldValue) throws SynchronizationException {
        JsonValue results = json(array());
        LOGGER.trace("Start source synchronization of {} {}", resourceId, (value == null) ? "without a value" : "with a value");

        LazyObjectAccessor sourceObjectAccessor = null;
        if (sourceDeleted) {
            sourceObjectAccessor = new LazyObjectAccessor(connectionFactory, sourceObjectSet, resourceId, null);
        } else if (value != null) {
            value.put("_id", resourceId); // unqualified
            sourceObjectAccessor = new LazyObjectAccessor(connectionFactory, sourceObjectSet, resourceId, value);
        } else {
            sourceObjectAccessor = new LazyObjectAccessor(connectionFactory, sourceObjectSet, resourceId);
        }
                
        // Loop over correlation queries, performing a sync for each linkQualifier
        for (String linkQualifier : getLinkQualifiers(sourceObjectAccessor.getObject(), oldValue, false, context, null)) {
            // TODO: one day bifurcate this for synchronous and asynchronous source operation
            SourceSyncOperation op = new SourceSyncOperation(this, context);
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
                logEntry(syncAuditEvent, null);
            }
        }
        return results;
    }

    /**
     * @return an event name for monitoring this object mapping
     */
    Name getObjectMappingEventName() {
        return Name.get(EVENT_OBJECT_MAPPING_PREFIX + name);
    }

    /**
     * Returns {@code true} if the specified object identifier is in this mapping's source
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
                value = LazyObjectAccessor.rawReadObject(connectionFactory, context, resourceContainer, resourceId);
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
                newValue = LazyObjectAccessor.rawReadObject(connectionFactory, context, resourceContainer, resourceId);
            }

            if (oldValue == null || oldValue.getObject() == null || !oldValue.isEqualTo(newValue)) {
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
     * For the input parameters see {@link SourceSyncOperation#toJsonValue()} or
     * {@link TargetSyncOperation#toJsonValue()}.
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
            ReconAction action = params.get("action").required().as(enumConstant(ReconAction.class));
            SyncOperation op = null;
            ReconAuditEventLogger event = null;
            Status status = Status.SUCCESS;
            SynchronizationException caughtSynchronizationException = null;
            try {
                if (params.get("target").isNull()) {
                    SourceSyncOperation sop = new SourceSyncOperation(this, context);
                    op = sop;
                    sop.fromJsonValue(params);

                    event = new ReconAuditEventLogger(sop, name, context);
                    event.setLinkQualifier(sop.getLinkQualifier());
                    String sourceObjectId = LazyObjectAccessor.qualifiedId(sourceObjectSet, sop.getSourceObjectId());
                    event.setSourceObjectId(sourceObjectId);
                    if (null == sop.getSourceObject()) {
                        throw new SynchronizationException("Source object " + sourceObjectId + " does not exist");
                    }
                    String targetId = params.get("targetId").asString();
                    if (null != targetId){
                        op.targetObjectAccessor = new LazyObjectAccessor(connectionFactory, targetObjectSet, targetId);
                        if (null == sop.getTargetObject()) {
                            throw new SynchronizationException("Target object " + targetId + " does not exist");
                        }
                    }
                    sop.assessSituation();
                } else {
                    TargetSyncOperation top = new TargetSyncOperation(this, context);
                    op = top;
                    top.fromJsonValue(params);
                    String targetId = params.get("targetId").required().asString();

                    event = new ReconAuditEventLogger(top, name, context);
                    event.setLinkQualifier(top.getLinkQualifier());
                    String targetObjectId = LazyObjectAccessor.qualifiedId(targetObjectSet, targetId);
                    event.setTargetObjectId(targetObjectId);

                    top.targetObjectAccessor = new LazyObjectAccessor(connectionFactory, targetObjectSet, targetId);
                    if (null == top.getTargetObject()) {
                        throw new SynchronizationException("Target object " + targetObjectId + " does not exist");
                    }
                    top.assessSituation();
                }
                // IF an expected situation is supplied, compare and reject if current situation changed
                if (params.isDefined("situation")) {
                    Situation situation = params.get("situation").required().as(enumConstant(Situation.class));
                    if (!situation.equals(op.situation)) {
                        throw new SynchronizationException("Expected situation does not match. Expected: " 
                                + situation.name()
                                + ", Found: " + op.situation.name());
                    }
                }
                op.action = action;
                op.performAction();
            } catch (SynchronizationException se) {
                if (op != null && op.action != ReconAction.EXCEPTION) {
                    // exception was not intentional
                    caughtSynchronizationException = se;
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
                logEntry(event, null);
            }
            if (caughtSynchronizationException != null) {
                throw caughtSynchronizationException;
            }
        } finally {
            if (reconId != null) {
                ObjectSetContext.pop(); // pop the TriggerContext
            }
        }
    }

    void doResults(ReconciliationContext reconContext, Context context)
            throws SynchronizationException {
        if (resultScript != null) {
            Map<String, Object> scope = new HashMap<>();
            scope.put("source", reconContext.getStatistics().getSourceStat().asMap());
            scope.put("target", reconContext.getStatistics().getTargetStat().asMap());
            scope.put("global", reconContext.getStatistics().asMap());

            final long startNanoTime = startNanoTime(reconContext);
            try {
                resultScript.exec(scope, context);
            } catch (ScriptThrownException ste) {
                throw toSynchronizationException(ste, name, "result");
            } catch (ScriptException se) {
                LOGGER.debug("{} result script encountered exception", name, se);
                throw new SynchronizationException(se);
            } finally {
                addDuration(reconContext, DurationMetric.resultScript, startNanoTime);
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
        if (clusteredSourceReconEnabled) {
            new ClusteredRecon(this, reconContext, newClusteredSourcePhaseTargetIdRegistry(reconContext),
                    newClusteredSourceReconSchedulerDispatch(), newReconciliationStatisticsPersistence()).dispatchClusteredRecon();
        } else {
            doRecon(reconContext);
        }
        measure.end();
    }

    protected ClusteredSourcePhaseTargetIdRegistry newClusteredSourcePhaseTargetIdRegistry(ReconciliationContext reconContext) {
        if (reconContext.getReconHandler().isRunTargetPhase()) {
            return new ClusteredSourcePhaseTargetIdRegistryImpl(
                    connectionFactory, LoggerFactory.getLogger(ClusteredSourcePhaseTargetIdRegistryImpl.class));
        } else {
            return new NoOpClusteredSourcePhaseTargetIdRegistry();
        }
    }

    protected ClusteredReconJobDispatch newClusteredSourceReconSchedulerDispatch() {
        return new SchedulerClusteredReconJobDispatch(connectionFactory, DateUtil.getDateUtil(),
                LoggerFactory.getLogger(SchedulerClusteredReconJobDispatch.class));
    }

    protected ReconciliationStatisticsPersistence newReconciliationStatisticsPersistence() {
        return new ReconciliationStatisticsPersistenceImpl();
    }

    /**
     * TEMPORARY. Future version will have this break-down into discrete units of work.
     * @param reconContext
     * @throws SynchronizationException
     */
    private void doRecon(ReconciliationContext reconContext) throws SynchronizationException {
        final ReconciliationStatistic stats = reconContext.getStatistics();
        stats.reconStart();
        String reconId = reconContext.getReconId();
        EventEntry measureIdQueries = Publisher.start(EVENT_RECON_ID_QUERIES, reconId, null);
        reconContext.setStage(ReconStage.ACTIVE_QUERY_ENTRIES);
        Context context = ObjectSetContext.get();
        try {
            // Execute onRecon script.
            executeOnRecon(context, reconContext);
            
            context = new TriggerContext(context, "recon");
            ObjectSetContext.push(context);
            logReconStart(reconContext, context);

            // Get the relevant source (and optionally target) identifiers before we assess the situations
            stats.sourceQueryStart();
            final long firstSourceQueryStart = startNanoTime(reconContext);

            ReconQueryResult sourceQueryResult = reconContext.querySourceIter(reconSourceQueryPageSize, null);
            Iterator<ResultEntry> sourceIter = sourceQueryResult.getIterator();

            stats.addDuration(DurationMetric.sourceQuery, firstSourceQueryStart);
            stats.sourceQueryEnd();
            if (!sourceIter.hasNext()) {
                if (!reconContext.getReconHandler().allowEmptySourceSet()) {
                    LOGGER.warn("Cannot reconcile from an empty data source, unless allowEmptySourceSet is true.");
                    reconContext.setStage(ReconStage.COMPLETED_FAILED);
                    stats.reconEnd();
                    logReconEndFailure(reconContext, context);
                    return;
                }
            }
            ResultIterable targetIterable =
                    new ResultIterable(Collections.<String>emptyList(), Collections.<JsonValue>emptyList());
            LocalSourcePhaseTargetIdRegistry targetIdRegistry;
            if (reconContext.getReconHandler().isRunTargetPhase()) {
                stats.targetQueryStart();
                // If we will handle a target phase, pre-load all relevant target identifiers
                Set<String> remainingTargetIds = new LinkedHashSet<>();
                final long targetQueryStart = startNanoTime(reconContext);

                targetIterable = reconContext.queryTarget();
                remainingTargetIds.addAll(targetIterable.getAllIds());

                stats.addDuration(DurationMetric.targetQuery, targetQueryStart);
                stats.targetQueryEnd();
                targetIdRegistry = new LocalSourcePhaseTargetIdRegistryImpl(remainingTargetIds);
            } else {
                targetIdRegistry = new NoOpLocalSourcePhaseTargetIdRegistry();
            }

            // Optionally get all links up front as well
            Map<String, Map<String, Link>> allLinks = null;
            if (prefetchLinks) {
                allLinks = new HashMap<>();
                int totalLinkEntries = 0;
                stats.linkQueryStart();
                for (String linkQualifier : getAllLinkQualifiers(context, reconContext)) {
                    final long linkQueryStart = startNanoTime(reconContext);
                    Map<String, Link> linksByQualifier = Link.getLinksForMapping(ObjectMapping.this, linkQualifier);
                    stats.addDuration(DurationMetric.linkQuery, linkQueryStart);

                    allLinks.put(linkQualifier, linksByQualifier);
                    totalLinkEntries += linksByQualifier.size();
                }
                reconContext.setTotalLinkEntries(totalLinkEntries);
                stats.linkQueryEnd();
            }

            measureIdQueries.end();

            EventEntry measureSource = Publisher.start(EVENT_RECON_SOURCE, reconId, null);
            reconContext.setStage(ReconStage.ACTIVE_RECONCILING_SOURCE);

            stats.sourcePhaseStart();
            final long sourcePhaseStart = startNanoTime(reconContext);
            
            boolean queryNextPage = false;

            LOGGER.info("Performing source sync for recon {} on mapping {}", reconId, name);
            do {
                // Query next page of results if paging
                if (queryNextPage) {
                    LOGGER.debug("Querying next page of source ids");
                    final long pagedSourceQueryStart = startNanoTime(reconContext);
                    sourceQueryResult = reconContext.querySourceIter(reconSourceQueryPageSize, 
                            sourceQueryResult.getPagingCookie());
                    sourceIter = sourceQueryResult.getIterator();
                    stats.addDuration(DurationMetric.sourceQuery, pagedSourceQueryStart);
                }
                // Perform source recon phase on current set of source ids
                ReconPhase sourcePhase = 
                        new ReconPhase(sourceIter, reconContext, context, allLinks, targetIdRegistry, sourceRecon);
                sourcePhase.setFeedSize(feedSize);
                sourcePhase.execute();
                queryNextPage = true;
            } while (reconSourceQueryPaging && sourceQueryResult.getPagingCookie() != null); // If paging, loop through next pages

            stats.addDuration(DurationMetric.sourcePhase, sourcePhaseStart);
            stats.sourcePhaseEnd();
            measureSource.end();

            LOGGER.debug("Remaining targets after source phase : {}", targetIdRegistry.getTargetPhaseIds());

            if (reconContext.getReconHandler().isRunTargetPhase()) {
                EventEntry measureTarget = Publisher.start(EVENT_RECON_TARGET, reconId, null);
                final long targetPhaseStart = startNanoTime(reconContext);
                reconContext.setStage(ReconStage.ACTIVE_RECONCILING_TARGET);
                targetIterable = targetIterable.removeNotMatchingEntries(targetIdRegistry.getTargetPhaseIds());
                stats.targetPhaseStart();
                ReconPhase targetPhase = new ReconPhase(targetIterable.iterator(), reconContext, context,
                        allLinks, null, targetRecon);
                targetPhase.setFeedSize(feedSize);
                targetPhase.execute();
                stats.addDuration(DurationMetric.targetPhase, targetPhaseStart);
                stats.targetPhaseEnd();
                measureTarget.end();
            }

            stats.reconEnd();
            reconContext.setStage(ReconStage.ACTIVE_PROCESSING_RESULTS);
            doResults(reconContext, context);
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
            doResults(reconContext, context);
            throw syncException;
        } catch (SynchronizationException e) {
            // Make sure that the error did not occur within doResults or last logging for completed success case
            reconContext.setStage(ReconStage.COMPLETED_FAILED);
            if ( reconContext.getStage() != ReconStage.ACTIVE_PROCESSING_RESULTS
                    && reconContext.getStage() != ReconStage.COMPLETED_SUCCESS ) {
                doResults(reconContext, context);
            }
            stats.reconEnd();
            logReconEndFailure(reconContext, context);
            throw new SynchronizationException("Synchronization failed", e);
        } catch (Exception e) {
            reconContext.setStage(ReconStage.COMPLETED_FAILED);
            doResults(reconContext, context);
            stats.reconEnd();
            logReconEndFailure(reconContext, context);
            throw new SynchronizationException("Synchronization failed", e);
        } finally {
            ObjectSetContext.pop(); // pop the TriggerContext
            if (!stats.hasEnded()) {
                stats.reconEnd();
            }
        }

// TODO: cleanup orphan link objects (no matching source or target) here
    }
    
    void executeOnRecon(Context context, final ReconciliationContext reconContext) throws SynchronizationException {
        if (onReconScript != null) {
            Map<String, Object> scope = new HashMap<>();
            scope.put("context", context);
            scope.put("mappingConfig", config);
            final long startNanoTime = startNanoTime(reconContext);
            try {
                onReconScript.exec(scope, context);
            } catch (ScriptThrownException se) {
                throw toSynchronizationException(se, name, "onRecon");
            } catch (ScriptException se) {
                LOGGER.debug("{} script encountered exception", name + " onRecon", se);
                throw new SynchronizationException(
                        new InternalErrorException(name + " onRecon script encountered exception", se));
            } finally {
                addDuration(reconContext, DurationMetric.onReconScript, startNanoTime);
            }
        }
    }

    /**
     * Sets the LogEntry message and messageDetail with the appropriate information
     * from the given Exception.
     * 
     * @param entry the LogEntry
     * @param syncException the Exception
     */
    <T extends AbstractSyncAuditEventBuilder<T>> void setLogEntryMessage(AbstractSyncAuditEventLogger<T> entry, Exception syncException) {
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
     * @param reconContext Recon context or {@code null}
     * @throws SynchronizationException
     */
    <T extends AbstractSyncAuditEventBuilder<T>> void logEntry(AbstractSyncAuditEventLogger<T> entry,
            ReconciliationContext reconContext)
            throws SynchronizationException {
        final long startNanoTime = startNanoTime(reconContext);
        try {
            entry.log(connectionFactory);
        } catch (ResourceException e) {
            throw new SynchronizationException(e);
        } finally {
            addDuration(reconContext, DurationMetric.auditLog, startNanoTime);
        }
    }

    /**
     * Record the start of a new reconciliation.
     *
     * @param reconContext
     * @param context
     * @throws SynchronizationException
     */
    void logReconStart(ReconciliationContext reconContext, Context context)
            throws SynchronizationException {
        ReconAuditEventLogger reconStartEntry = new ReconAuditEventLogger(null, name, context);
        reconStartEntry.setEntryType(ReconAuditEventLogger.RECON_LOG_ENTRY_TYPE_RECON_START);
        reconStartEntry.setReconciliationServiceReconAction(reconContext.getReconAction());
        reconStartEntry.setReconId(reconContext.getReconId());
        reconStartEntry.setMessage("Reconciliation initiated by "
                + context.asContext(SecurityContext.class).getAuthenticationId());
        logEntry(reconStartEntry, reconContext);

    }

    /**
     * Record the successful completion of a reconciliation.
     *
     * @param reconContext
     * @param context
     * @throws SynchronizationException
     */
    void logReconEndSuccess(ReconciliationContext reconContext, Context context)
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
    void logReconEndFailure(ReconciliationContext reconContext, Context context)
            throws SynchronizationException {
        logReconEnd(reconContext, context, Status.FAILURE, "Reconciliation failed.");
    }

    /**
     * Record a final entry for a reconciliation.
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
        logEntry(reconAuditEvent, reconContext);
        LOGGER.info(loggerMessage + " " + simpleSummary);
    }

    /**
     * Execute a sync engine action explicitly, without going through situation assessment.
     * 
     * @param context {@link Context} associated with the current sync.
     * @param sourceObject the source object if applicable to the action
     * @param targetObject the target object if applicable to the action
     * @param situation an optional situation that was originally assessed. Null if not the result of an earlier situation assessment.
     * @param action the explicit action to invoke
     * @param reconId an optional identifier for the recon context if this is done in the context of reconciliation
     */
    public void explicitOp(Context context, JsonValue sourceObject, JsonValue targetObject, Situation situation, 
            ReconAction action, String reconId) throws SynchronizationException {
        for (String linkQualifier : getLinkQualifiers(sourceObject, null, false, context, null)) {
            ExplicitSyncOperation linkOp = new ExplicitSyncOperation(this, context);
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
     * Gets a start-time, in nanoseconds, for the current thread. Calling thread must be the same thread used to call
     * {@link #addDuration(ReconciliationContext, DurationMetric, long)}.
     *
     * @param reconContext Recon context or {@code null}
     * @return Start-time or {@code -1} if {@code reconContext} was {@code null}
     */
    static long startNanoTime(final ReconciliationContext reconContext) {
        return reconContext == null ? -1 : DurationStatistics.startNanoTime();
    }

    /**
     * Gets a start-time, in nanoseconds, for the current thread. Calling thread must be the same thread used to call
     * {@link #addDuration(ReconciliationContext, ReconciliationStatistic.DurationMetric, long)}.
     *
     * @param reconContext Recon context or {@code null}
     * @param expression Expression that must be {@code true}, for start-time to be generated
     * @return Start-time or {@code -1} if {@code reconContext} was {@code null} or {@code expression} was {@code false}
     */
    static long startNanoTime(final ReconciliationContext reconContext, final boolean expression) {
        return reconContext == null || !expression ? -1 : DurationStatistics.startNanoTime();
    }

    /**
     * Delegates a call to {@link ReconciliationStatistic#addDuration(DurationMetric, long)}, in situations where
     * {@code reconContext} may be {@code null} and/or {@code startNanoTime} may be {@code -1} to signal that
     * the duration statistic should <em>not</em> be recorded.
     *
     * @param reconContext Recon context or {@code null} to cause this to be a no-op
     * @param metric Metric for statistic
     * @param startNanoTime Start-time, in nanoseconds or {@code -1} to cause this to be a no-op
     */
    static void addDuration(final ReconciliationContext reconContext,
            final ReconciliationStatistic.DurationMetric metric, final long startNanoTime) {
        if (reconContext != null && startNanoTime != -1) {
            reconContext.getStatistics().addDuration(metric, startNanoTime);
        }
    }

}
