/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.internal.recon;

import static org.forgerock.openidm.internal.recon.ReconUtil.*;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.script.Bindings;
import javax.script.ScriptException;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.RetryableException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.internal.recon.ConfigurationProvider.Mode;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Name;
import org.forgerock.openidm.smartevent.Publisher;
import org.forgerock.openidm.util.DateUtil;
import org.forgerock.openidm.util.JsonUtil;
import org.forgerock.openidm.util.ResourceUtil;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.engine.Utils;
import org.forgerock.script.scope.Function;
import org.forgerock.script.scope.FunctionFactory;
import org.forgerock.script.scope.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.SequenceReportingEventHandler;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;
import com.lmax.disruptor.dsl.ProducerType;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public class ReconExecutor {

    /**
     * Event names for monitoring Reconciliation behavior
     */
    public static final Name EVENT_RECON = Name
            .get("openidm/internal/discovery-engine/reconciliation");
    public static final Name EVENT_RECON_ID_QUERIES = Name
            .get("openidm/internal/discovery-engine/reconciliation/id-queries-phase");
    public static final Name EVENT_RECON_SOURCE = Name
            .get("openidm/internal/discovery-engine/reconciliation/source-phase");
    public static final Name EVENT_RECON_TARGET = Name
            .get("openidm/internal/discovery-engine/reconciliation/target-phase");

    /**
     * Setup logging for the {@link ReconExecutor}.
     */
    final static Logger logger = LoggerFactory.getLogger(ReconExecutor.class);

    private final Disruptor<ReconEvent> disruptor;

    private final ServerContext context;

    private final ExecutorPreferences preferences;

    private final DefaultThreadFactory threadFactory;

    /**
     * Cache for Target Resources without links. In MANY-TO-ONE relation the
     * situation is UNASSIGNED only if no single CONFIRMED, SOURCE_MISSING or
     * FOUND situation was assessed. The Boolean is false to mark it was not
     * assessed yet;
     */
    private final NavigableMap<String, Utils.Pair<AtomicInteger, Map<String, Object>>> targetCollection;

    /**
     * Cache for Links if the Target Resources are preloaded. The Target
     * Resource are in the {@link #sourceLink} and this is just a secondary
     * index with the target key.
     */
    private final ConcurrentSkipListMap<String, SoftReference<Map<String, Object>>> targetLinks;

    private final ConcurrentSkipListMap<String, ConcurrentSkipListMap<String, SoftReference<Map<String, Object>>>> targetTypedLinks;

    /**
     * Cache for Target Resources with links and the
     */
    private final ConcurrentSkipListMap<String, Map<String, Object>> sourceLink;

    private final ConcurrentSkipListMap<String, ConcurrentSkipListMap<String, Map<String, Object>>> sourceTypedLink;

    /**
     * Helper to build the link object and hide the complexity of the link
     * directions and the case sensitiveness.
     */
    private final LinkBuilder linkBuilder;

    /**
     * Many function relates to the relation between the {@code source} and
     * {@code target} collections.
     */
    private final Mode reconMode;

    /**
     * The ReconciliationStatistic
     */
    private final StatisticsEventHandler statistics;

    private final TargetResourceEventHandler targetHandler;

    private final Resource resource;

    /**
     * EventTranslator used to publish the target resource collection
     */
    // private final BatchEventProcessor<ReconEvent> targetEventProcessor;

    public ReconExecutor(final ServerContext context, final ExecutorPreferences preferences) {
        threadFactory = new DefaultThreadFactory(preferences.getConfiguration().name);
        disruptor =
                new Disruptor<ReconEvent>(new EventFactory<ReconEvent>() {
                    @Override
                    public ReconEvent newInstance() {
                        return new ReconEvent();
                    }
                }, 1024, new ReconJobExecutor(threadFactory), ProducerType.MULTI,
                        new SleepingWaitStrategy());
        this.context = context;
        this.preferences = preferences;

        linkBuilder = new LinkBuilder(preferences.getConfiguration());
        reconMode = preferences.getConfiguration().getRelation();

        /*
         * Links are not preloaded
         */
        if (preferences.getConfiguration().isPreLoadLinkingData()) {
            if (Mode.MANY_TO_ONE.equals(reconMode)) {
                targetLinks = null;
                targetTypedLinks =
                        new ConcurrentSkipListMap<String, ConcurrentSkipListMap<String, SoftReference<Map<String, Object>>>>();

                targetTypedLinks.put(DEFAULT_LINK_TYPE,
                        new ConcurrentSkipListMap<String, SoftReference<Map<String, Object>>>());
            } else {
                targetLinks =
                        new ConcurrentSkipListMap<String, SoftReference<Map<String, Object>>>();
                targetTypedLinks = null;
            }
        } else {
            targetLinks = null;
            targetTypedLinks = null;
        }

        if (preferences.getConfiguration().isPreLoadTargetCollection()) {
            // if (Mode.MANY_TO_ONE.equals(reconMode)) {
            // targetCollection = null;
            targetCollection =
                    new ConcurrentSkipListMap<String, Utils.Pair<AtomicInteger, Map<String, Object>>>();
            // } else {
            // targetCollection = new ConcurrentSkipListMap<String, Map<String,
            // Object>>();
            // targetCollectionPair = null;
            // }
            // final SequenceBarrier barrier =
            // disruptor.getRingBuffer().newBarrier(new Sequence[0]);

            targetHandler =
                    new TargetResourceEventHandler(
                            preferences.getConfiguration().linkQueryWithTarget,
                            preferences.targetLinkScript);

            // BatchEventProcessor<ReconEvent> targetEventProcessor =
            // new BatchEventProcessor<ReconEvent>(disruptor.getRingBuffer(),
            // barrier,
            // eventHandler);
            disruptor.handleEventsWith(targetHandler);
        } else {
            targetCollection = null;
            targetHandler = null;
            // targetCollectionPair = null;
            // targetEventProcessor = null;
        }

        if (Mode.ONE_TO_MANY.equals(reconMode)) {
            sourceLink = null;
            sourceTypedLink =
                    new ConcurrentSkipListMap<String, ConcurrentSkipListMap<String, Map<String, Object>>>();
            sourceTypedLink.put(DEFAULT_LINK_TYPE,
                    new ConcurrentSkipListMap<String, Map<String, Object>>());
        } else {
            sourceLink = new ConcurrentSkipListMap<String, Map<String, Object>>();
            sourceTypedLink = null;
        }

        statistics = new StatisticsEventHandler(preferences);

        /*
         * Handle the source recon events
         */
        EventHandlerGroup<ReconEvent> eventEventHandlerGroup =
                disruptor.handleEventsWith(new SourceResourceEventHandler(preferences
                        .getConfiguration().linkQueryWithTarget,
                        preferences.getConfiguration().linkQueryWithSource,
                        preferences.sourceLinkScript));

        // ----- Setup the EventHandlers to load the target resource

        List<EventHandler<ReconEvent>> handlers = new ArrayList<EventHandler<ReconEvent>>(1);

        if (preferences.getConfiguration().getTarget() instanceof ReadRequest) {
            handlers.add(new TargetLoaderEventHandler(context, preferences.getConfiguration()
                    .getTarget(), linkBuilder));
        }

        if (null != preferences.getConfiguration().correlationQuery
                || null != preferences.correlationScript) {
            handlers.add(new CorrelateEventHandler(context,
                    preferences.getConfiguration().correlationQuery, preferences.correlationScript,
                    linkBuilder));

            eventEventHandlerGroup =
                    eventEventHandlerGroup
                            .then(handlers.toArray(new EventHandler[handlers.size()]));

            if (null != preferences.confirmationScript) {
                eventEventHandlerGroup =
                        eventEventHandlerGroup.then(new ConfirmationEventHandler(context,
                                preferences.confirmationScript));
            }
        } else {
            eventEventHandlerGroup =
                    eventEventHandlerGroup
                            .then(handlers.toArray(new EventHandler[handlers.size()]));
        }

        // ----- Setup the Last EventHandlers in the chain

        eventEventHandlerGroup.then(new ReconciliationEventHandler(true),
                new ReconciliationEventHandler(false)).then(statistics);

        // ----- Setup Global ExceptionHandler

        disruptor.handleExceptionsWith(new ExceptionHandler() {
            @Override
            public void handleEventException(Throwable ex, long sequence, Object event) {
                logger.error("Event exception #:{}", sequence, ex);
            }

            @Override
            public void handleOnStartException(Throwable ex) {
                logger.error("Start Exception", ex);
            }

            @Override
            public void handleOnShutdownException(Throwable ex) {
                logger.error("Shutdown Exception", ex);
            }
        });
        resource =
                new Resource(preferences.getReconId(), null, new JsonValue(
                        new LinkedHashMap<String, Object>()));
        resource.getContent().put(Resource.FIELD_CONTENT_ID, resource.getId());
    }

    StatisticsEventHandler getStatistics() {
        return statistics;
    }

    public Resource executeAsync(final ResultHandler<JsonValue> handler) {
        Thread thread = threadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Resource result = execute();
                    handler.handleResult(result.getContent());
                } catch (ResourceException e) {
                    handler.handleError(e);
                }
            }
        });
        thread.setName(thread.getName() + " ASYNC");
        thread.start();

        // TODO get stat
        return resource;
    }

    public Resource execute() throws ResourceException {
        statistics.reconStart();

        /*
         * Load Links in advance with query if defined.
         */
        prefetchLinks(preferences.getConfiguration().linkQuery, preferences.getConfiguration()
                .isPreLoadTargetCollection());

        if (preferences.isCanceled()) {
            logger.info("Reconciliation is cancelled.");
            return resource;
        }

        /*
         * Start some real work
         */
        disruptor.start();

        try {
            if (preferences.getConfiguration().isPreLoadTargetCollection()) {
                statistics.targetQueryStart();
                logger.info("Start reconciliation/{} -> loading target object with query",
                        preferences.getConfiguration().name);
                publishQueryResult((QueryRequest) preferences.getConfiguration().getTarget(),
                        new EventTranslatorOneArg<ReconEvent, Resource>() {
                            @Override
                            public void translateTo(ReconEvent event, long sequence,
                                    Resource resource) {
                                event.clear();
                                event.setTarget(resource);
                                logger.trace("Publish target: {}:{}", sequence, resource.getId());
                                statistics.targetIncrementTotal();
                            }
                        });

                final long targetCursor = disruptor.getRingBuffer().getCursor();

                /*
                 * Wait until all event processed and then halt the event
                 * processor, we don't need this any more.
                 */
                while (targetHandler.hasBacklog(targetCursor)) {
                    logger.info(
                            "Wait for backlog in source event processing cursor:{} sequence:{} after {} sec",
                            targetCursor, statistics.getPosition(), statistics
                                    .duration(TimeUnit.SECONDS));
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        preferences.cancel();
                    }
                }
                statistics.targetQueryEnd();

                if (null != targetTypedLinks) {
                    targetTypedLinks.clear();
                }
                if (null != targetLinks) {
                    targetLinks.clear();
                }
            }

            if (preferences.isCanceled()) {
                logger.info("Reconciliation is cancelled.");
                return resource;
            }

            /*
             * Publish the source Resources
             */
            logger.info("Start reconciliation/{} -> loading source object with query", preferences
                    .getConfiguration().name);
            statistics.sourceQueryStart();
            statistics.startStage(ReconStage.ACTIVE_RECONCILING_SOURCE);

            publishQueryResult(preferences.getConfiguration().getSource(),
                    new EventTranslatorOneArg<ReconEvent, Resource>() {
                        @Override
                        public void translateTo(ReconEvent event, long sequence, Resource resource) {
                            event.clear();
                            event.setSource(resource);
                            logger.trace("Publish source: {}:{}", sequence, resource.getId());
                            statistics.sourceIncrementTotal();
                        }
                    });

            final long sourceCursor = disruptor.getRingBuffer().getCursor();

            /*
             * Wait until all event processed and then continue to the next
             * phase.
             */
            while (statistics.hasBacklog(sourceCursor)) {
                logger.info(
                        "Wait for backlog in source event processing cursor:{} sequence:{} after {} sec",
                        sourceCursor, statistics.getPosition(), statistics
                                .duration(TimeUnit.SECONDS));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    preferences.cancel();
                }
            }
            statistics.endStage(ReconStage.ACTIVE_RECONCILING_SOURCE);
            statistics.sourceQueryEnd();

            /*
             * Publish the remaining preloaded link resources
             */
            statistics.startStage(ReconStage.ACTIVE_LINK_CLEANUP);
            logger.info("Start reconciliation/{} -> publish remaining target and links resources",
                    preferences.getConfiguration().name);
            if (Mode.ONE_TO_MANY.equals(reconMode)) {
                for (ConcurrentSkipListMap<String, Map<String, Object>> tripletsCache : sourceTypedLink
                        .values()) {

                    Map<String, Map<String, Object>> triplets =
                            new HashMap<String, Map<String, Object>>(tripletsCache.size());

                    for (Map.Entry<String, Map<String, Object>> entry : tripletsCache.entrySet()) {
                        Map<String, Object> triplet = entry.getValue();
                        Object t = triplet.get(TARGET_FIELD);
                        if (t instanceof SoftReference) {
                            triplet.put(TARGET_FIELD, ((SoftReference) t).get());
                        }
                        triplets.put(entry.getKey(), triplet);
                    }

                    if (publishRemainingTriplets(triplets))
                        return resource;
                    tripletsCache.clear();
                }
                sourceTypedLink.clear();
            } else {
                for (Map<String, Object> triplet : sourceLink.values()) {
                    Map<String, Map<String, Object>> triplets =
                            new HashMap<String, Map<String, Object>>(1);
                    triplets.put(DEFAULT_LINK_TYPE, triplet);
                    if (publishRemainingTriplets(triplets))
                        return resource;
                }
                sourceLink.clear();
            }
            statistics.endStage(ReconStage.ACTIVE_LINK_CLEANUP);

            /*
             * Publish the target object without link and not correlated with
             * any source object.
             */
            statistics.startStage(ReconStage.ACTIVE_RECONCILING_TARGET);
            if (null != targetCollection) {
                for (Utils.Pair<AtomicInteger, Map<String, Object>> pair : targetCollection
                        .values()) {
                    if (pair.fst.get() > 0) {
                        continue;
                    }

                    if (preferences.isCanceled()) {
                        logger.info("Reconciliation is cancelled.");
                        return resource;
                    }
                    final long sequence = disruptor.getRingBuffer().next();
                    try {
                        ReconEvent event = disruptor.getRingBuffer().getPreallocated(sequence);
                        event.clear();
                        event.setTargetMap(pair.snd);
                    } finally {
                        disruptor.getRingBuffer().publish(sequence);
                    }
                }
                targetCollection.clear();
            }
            statistics.endStage(ReconStage.ACTIVE_RECONCILING_TARGET);

        } finally {
            logger.info("Initiate shutdown reconciliation/{} -> after {} sec", preferences
                    .getConfiguration().name, statistics.duration(TimeUnit.SECONDS));
            disruptor.shutdown();
            statistics.reconEnd();
            threadFactory.joinAllThreads();
            logger.info("Finish reconciliation/{} -> execution completed after {} sec", preferences
                    .getConfiguration().name, statistics.duration(TimeUnit.SECONDS));
            resource.getContent().asMap().putAll(statistics.asMap());
        }
        return resource;
    }

    /**
     * Publish a collection of triplets.
     * 
     * @param triplets
     *            collection of triplets to publish.
     * @return true if the reconciliation process is cancelled.
     */
    private boolean publishRemainingTriplets(final Map<String, Map<String, Object>> triplets) {
        if (preferences.isCanceled()) {
            logger.info("Reconciliation is cancelled.");
            return true;
        }
        final long sequence = disruptor.getRingBuffer().next();
        try {
            ReconEvent event = disruptor.getRingBuffer().getPreallocated(sequence);
            event.clear();
            event.setTriplets(triplets);
        } finally {
            disruptor.getRingBuffer().publish(sequence);
        }
        return false;
    }

    // private void cacheTargetCollection(final Resource targetResource) {
    // Map<String, Object> target = linkBuilder.targetResource(targetResource);
    // targetCollection.put(linkBuilder.normalizeTargetId(targetResource.getId()),
    // target);
    // }
    //
    // private Map<String, Object> cacheSourceCollection(final Resource
    // targetResource, final Resource link) {
    // Map<String, Object> sourceCacheItem = new HashMap<String, Object>(2);
    // final Map<String, Object> linkMap = linkBuilder.linkResource(link);
    // sourceCacheItem.put(LINK_FIELD, linkMap);
    //
    // if (null != targetResource) {
    // // TODO FIX me?
    // final Map<String, Object> targetMap =
    // linkBuilder.targetResource(targetResource);
    // sourceCacheItem.put(TARGET_FIELD, targetMap);
    //
    // }
    // return cacheSourceCollection(sourceCacheItem, linkMap);
    // }

    /**
     * Cache the triplet for source recon optimized format and does an absents
     * check.
     * 
     * @param triplet
     * @return The {@code triplet} given in the parameter or the one stored
     *         before.
     */
    private Triplet cacheSourceCollection(final Triplet triplet) {

        String sourceId = triplet.link().sourceId();
        Map<String, Object> previous = null;
        if (Mode.ONE_TO_MANY.equals(reconMode)) {
            /*
             * One record in sourceTypedLink for each source object
             */
            ConcurrentSkipListMap<String, Map<String, Object>> sourceRecordMap =
                    sourceTypedLink.get(sourceId);
            if (null == sourceRecordMap) {
                sourceRecordMap = new ConcurrentSkipListMap<String, Map<String, Object>>();
                ConcurrentSkipListMap<String, Map<String, Object>> c =
                        sourceTypedLink.putIfAbsent(sourceId, sourceRecordMap);
                if (c != null) {
                    sourceRecordMap = c;
                }
            }

            String linkType = triplet.link().linkType();
            if (null == linkType) {
                previous = sourceRecordMap.putIfAbsent(DEFAULT_LINK_TYPE, triplet.map());
            } else {
                previous = sourceRecordMap.putIfAbsent(linkType, triplet.map());
            }
        } else {
            previous = sourceLink.putIfAbsent(sourceId, triplet.map());
        }

        if (null != previous && previous != triplet.map()) {
            Triplet previousTriplet = Triplet.fromTriplet(previous);
            previousTriplet.ambiguous(triplet.map());
            logger.info(
                    "Multiple links with the same type points to the same source object. sourceId:{}",
                    sourceId);
            return previousTriplet;
        }
        return triplet;
    }

    // private void processOneToMany(final LinkWrapper link, final Resource
    // resource) {
    // String type = link.getLinkType();
    // if (null == type) {
    // type = "#"; // Default type key
    // }
    // NavigableMap<String, Triplet> linkMap = sourceTypedLink.get(type);
    // if (null == linkMap) {
    // linkMap = new ConcurrentSkipListMap<String, Triplet>();
    // NavigableMap<String, Triplet> m = sourceTypedLink.putIfAbsent(type,
    // linkMap);
    // if (m != null) {
    // linkMap = m;
    // }
    // }
    //
    // Triplet next = new Triplet(link);
    // next.setTarget(resource);
    // Triplet previous = linkMap.put(link.getSourceId(), next);
    // if (null != previous) {
    // logger.info(
    // "Multiple links with the same type points to the same source object. sourceId:{}, type:{}, targetId:{}",
    // link.getSourceId(), type, link.getTargetId());
    // next.addAmbiguousTriplet(previous);
    // }
    // }
    //
    // private void processOneOrManyToOne(final Resource resource, final
    // LinkWrapper link) {
    // if (null != link) {
    // Triplet next = new Triplet(link);
    // next.setTarget(resource);
    // Triplet previous = sourceLink.put(link.getSourceId(), next);
    // if (null != previous) {
    // logger.info(
    // "Multiple links points to the same source object. sourceId:{} , type:{}, targetId:{}",
    // link.getSourceId(), link.getLinkType(), link.getTargetId());
    // next.addAmbiguousTriplet(previous);
    // }
    // }
    // }

    /**
     * Gets all the links from the repository to combine with the source/target
     * object.
     * <p/>
     * The link size is smaller then the Resource object this is why loading
     * them is cheaper then loading the Source/Target and find the links for
     * each. We also need to mark those links which has no source or target
     * objects.
     * 
     * @param linkQueryRequest
     * @throws ResourceException
     */
    private void prefetchLinks(final QueryRequest linkQueryRequest, final boolean optimiseForTarget)
            throws ResourceException {
        if (null != linkQueryRequest) {
            statistics.linkQueryStart();
            QueryRequest request = linkQueryRequest;
            final AtomicReference<QueryResult> queryResult = new AtomicReference<QueryResult>();
            do {
                // TODO Use ReconContext
                QueryResult queryResult1 =
                        context.getConnection().query(context, request, new QueryResultHandler() {
                            @Override
                            public void handleError(final ResourceException error) {
                                // TODO Fix a better fatal event
                                preferences.handleFatalEventException(error, 0l, "");
                                queryResult.lazySet(new QueryResult());
                            }

                            @Override
                            public boolean handleResource(final Resource resource) {
                                Triplet triplet =
                                        Triplet.fromLink(linkBuilder.linkResource(resource))
                                                .triplet();
                                /*
                                 * Cache the triplet and
                                 */
                                triplet = cacheSourceCollection(triplet);
                                statistics.linkIncrementTotal();

                                if (optimiseForTarget) {

                                    String targetId = triplet.link().targetId();

                                    SoftReference<Map<String, Object>> previousReference = null;

                                    if (Mode.MANY_TO_ONE.equals(reconMode)) {
                                        /*
                                         * One record in targetTypedLink for
                                         * each target object
                                         */
                                        ConcurrentSkipListMap<String, SoftReference<Map<String, Object>>> sourceRecordMap =
                                                targetTypedLinks.get(targetId);
                                        if (null == sourceRecordMap) {
                                            sourceRecordMap =
                                                    new ConcurrentSkipListMap<String, SoftReference<Map<String, Object>>>();
                                            ConcurrentSkipListMap<String, SoftReference<Map<String, Object>>> c =
                                                    targetTypedLinks.putIfAbsent(targetId,
                                                            sourceRecordMap);
                                            if (c != null) {
                                                sourceRecordMap = c;
                                            }
                                        }

                                        String linkType = triplet.link().linkType();
                                        if (null == linkType) {
                                            previousReference =
                                                    sourceRecordMap.putIfAbsent(DEFAULT_LINK_TYPE,
                                                            new SoftReference<Map<String, Object>>(
                                                                    triplet.map()));
                                        } else {
                                            previousReference =
                                                    sourceRecordMap.putIfAbsent(linkType,
                                                            new SoftReference<Map<String, Object>>(
                                                                    triplet.map()));
                                        }
                                    } else {
                                        previousReference =
                                                targetLinks.putIfAbsent(targetId,
                                                        new SoftReference<Map<String, Object>>(
                                                                triplet.map()));
                                    }

                                    if (null != previousReference) {
                                        final Map<String, Object> previous =
                                                previousReference.get();
                                        if (null == previous) {
                                            logger.error(
                                                    "Something is wrong! The GC removed the SoftReferenced object!? key:{}",
                                                    targetId);
                                            throw new IllegalStateException(
                                                    "Something is wrong! The GC removed the SoftReferenced object!?");

                                        } else if (previous != triplet.map()) {
                                            Triplet previousTriplet = Triplet.fromTriplet(previous);
                                            // TODO Fix this (lighter object)
                                            previousTriplet.ambiguous(triplet.map());
                                            logger.info(
                                                    "Multiple links with the same type points to the same target object. targetId:{}",
                                                    targetId);
                                        }
                                    }
                                }
                                return !preferences.isCanceled();
                            }

                            @Override
                            public void handleResult(final QueryResult result) {
                                queryResult.lazySet(result);
                            }
                        });

                QueryResult result = queryResult.get();
                if (result == null) {
                    /*
                     * Likely the query failed.
                     */
                    break;
                } else if (result.getRemainingPagedResults() < 1) {
                    // We did fetch all links
                    break;
                } else {
                    request = Requests.copyOfQueryRequest(linkQueryRequest);
                    request.setPagedResultsCookie(result.getPagedResultsCookie());
                }
            } while (!preferences.isCanceled());
        }
        statistics.linkQueryEnd();
    }

    private long publishQueryResult(final QueryRequest targetQueryRequest,
            final EventTranslatorOneArg<ReconEvent, Resource> eventTranslator)
            throws ResourceException {

        QueryRequest request = targetQueryRequest;
        final AtomicReference<QueryResult> queryResult = new AtomicReference<QueryResult>();

        do {
            context.getConnection().query(context, request, new QueryResultHandler() {
                @Override
                public void handleError(final ResourceException error) {
                    // TODO Fix a better fatal event
                    preferences.handleFatalEventException(error, 0l, "");
                    queryResult.lazySet(new QueryResult());
                }

                @Override
                public boolean handleResource(final Resource resource) {
                    // TODO fix logging
                    long sequence = -1l;
                    try {
                        disruptor.getRingBuffer().publishEvent(eventTranslator, resource);
                    } catch (Exception e) {
                        logger.error("Failed to publish target event to disruptor, sequence# {}",
                                sequence, e);
                        // TODO Fix a better fatal event
                        preferences.handleFatalEventException(e, 0l, "");
                        return false;
                    }
                    return !preferences.isCanceled();
                }

                @Override
                public void handleResult(QueryResult result) {
                    queryResult.lazySet(result);
                }
            });

            QueryResult result = queryResult.get();
            if (result == null) {
                /*
                 * Likely the query failed. See {@code
                 * disruptor.publishEvent(eventTranslator)}
                 */
                break;
            } else if (result.getRemainingPagedResults() < 1) {
                // We did fetch all links
                break;
            } else {
                request = Requests.copyOfQueryRequest(request);
                request.setPagedResultsCookie(result.getPagedResultsCookie());
            }
        } while (!preferences.isCanceled());
        return disruptor.getCursor();
    }

    class TargetResourceEventHandler implements SequenceReportingEventHandler<ReconEvent> {

        final boolean preLoadLinkingData;

        /**
         * Query used to find the link for with target object.
         */
        final QueryRequest linkQueryWithTarget;

        final ScriptEntry targetLinkScript;

        private Sequence sequence = null;

        TargetResourceEventHandler(final QueryRequest linkQueryWithTarget,
                final ScriptEntry targetLinkScript) {
            this.linkQueryWithTarget = linkQueryWithTarget;
            this.targetLinkScript = targetLinkScript;
            this.preLoadLinkingData = null != preferences.getConfiguration().linkQuery;
        }

        /**
         * Confirms if all messages have been consumed by this event processors
         */
        private boolean hasBacklog(final long cursor) {

            if (cursor > sequence.get()) {
                return true;
            }
            return false;
        }

        Sequence getSequence() {
            return sequence;
        }

        @Override
        public void setSequenceCallback(Sequence sequenceCallback) {
            sequence = sequenceCallback;
        }

        public void onEvent(ReconEvent event, long sequence, boolean endOfBatch) throws Exception {
            /*
             * This EventHandler must only handle events from target loader
             * event handler.
             */
            final Resource resource = event.getTarget();
            if (null != resource) {
                Triplet triplet =
                        Triplet.fromTarget(resource, linkBuilder.isTargetCaseSensitive()).triplet();
                String targetId = triplet.target().getId();

                if (preLoadLinkingData) {
                    /*
                     * Links are preloaded into the targetLinks or
                     * targetTypedLinks and we need to combine the target
                     * resources with them.
                     */
                    if (Mode.MANY_TO_ONE.equals(reconMode)) {
                        /*
                         * TODO FIX THE SCRIPT TO RETURN LINKS
                         * 
                         * Use of the script here may be too expensive. The same
                         * script will be implemented in the ONE_TO_MANY to
                         * qualify the expected link type. Copy that code later
                         * here.
                         * 
                         * Now just use the existing links/linkTypes
                         */

                        ConcurrentSkipListMap<String, SoftReference<Map<String, Object>>> e =
                                targetTypedLinks.get(targetId);
                        if (e != null) {
                            for (Map.Entry<String, SoftReference<Map<String, Object>>> entry : e
                                    .entrySet()) {
                                entry.getValue().get().put(
                                        TARGET_FIELD,
                                        new SoftReference<Map<String, Object>>(triplet.target()
                                                .map()));
                            }
                        }

                        /**
                         * This is a marker for the source recon phase about the
                         * target object if no correlation query finds this
                         * resource.
                         */
                        cacheTargetCollection(triplet.target(), targetId);
                    } else {
                        SoftReference<Map<String, Object>> e = targetLinks.get(targetId);
                        if (null != e) {
                            Triplet.fromTriplet(e.get()).target().setResource(triplet.target());
                        } else {
                            /*
                             * All target exists and no-link resource.
                             * 
                             * The source recon phase determine the expected
                             * linkType and execute the correlation query. The
                             * query may find this {@code target} resource but
                             * at that point the type is pre determined.
                             * 
                             * This is a marker for the source recon phase about
                             * the target object if no correlation query finds
                             * this resource.
                             */
                            cacheTargetCollection(triplet.target(), targetId);
                        }
                    }
                } else if (null != targetLinkScript) {
                    /*
                     * The links are generated with script if {@code
                     * targetLinkScript} is set.
                     */

                    try {
                        Script script = targetLinkScript.getScript(context);
                        script.getBindings().putAll(triplet.map());

                        Set<Map<String, Object>> links = getLinksWithScript(script);
                        if (links.isEmpty()) {
                            cacheTargetCollection(triplet.target(), targetId);
                        } else {
                            if (Mode.MANY_TO_ONE.equals(reconMode)) {
                                // allow multiple links
                                for (Map<String, Object> link : getLinksWithScript(script)) {
                                    cacheSourceCollection(Triplet.fromLink(link).triplet().target()
                                            .setResource(triplet.target()).triplet());
                                }
                            } else {
                                // at most one link allowed
                                // TODO Optimize the ambiguous lookup
                            }
                        }

                    } catch (ResourceException e) {
                        // logger.debug("ObjectMapping/{} script {} encountered exception at {}",
                        // name,
                        // // scriptPair.snd.getName(), scriptPair.fst, e);

                    } catch (Exception e) {
                        // TODO Improve the logging with the Recon name
                        logger.error("Failed to get the Links for {}", resource.getId());
                        /**
                         * Mark Failed to get the links!!
                         * 
                         * This is a marker for the source recon phase about the
                         * target object if no correlation query finds this
                         * resource.
                         */
                        cacheTargetCollection(triplet.target(), targetId);
                        // TODO handle fatal exception
                    }
                } else {
                    cacheTargetCollection(triplet.target(), targetId);
                }
            }

        }

        private void cacheTargetCollection(final Triplet.Vertex target, String targetId) {
            // if (Mode.ONE_TO_MANY.equals(reconMode)) {
            targetCollection.put(targetId, new Utils.Pair<AtomicInteger, Map<String, Object>>(
                    new AtomicInteger(0), target.map()));
            // } else {
            // targetCollection.put(targetId, target);
            // }
        }
    }

    /**
     * The SourceReconEventHandler finds the link and target and adds the
     * triplet to the event.
     */
    class SourceResourceEventHandler implements EventHandler<ReconEvent> {

        final boolean cacheFirst;

        /**
         * Query used to find the link for with source object.
         */
        final QueryRequest linkQueryWithSource;

        /**
         * Query used to find the link for with target object.
         */
        final QueryRequest linkQueryWithTarget;

        final ScriptEntry sourceLinkScript;

        SourceResourceEventHandler(final QueryRequest linkQueryWithTarget,
                final QueryRequest linkQueryWithSource, final ScriptEntry linkScript) {
            this.linkQueryWithTarget = linkQueryWithTarget;
            this.linkQueryWithSource = linkQueryWithSource;
            this.sourceLinkScript = linkScript;
            this.cacheFirst =
                    preferences.getConfiguration().isPreLoadLinkingData()
                            || (preferences.getConfiguration().isPreLoadTargetCollection() && null != preferences.targetLinkScript);
        }

        public void onEvent(ReconEvent event, long sequence, boolean endOfBatch) throws Exception {
            // Handle only even events, one way to add more threads
            // if (event instanceof ReconEvent && sequence % 2 == 0) {}

            /*
             * This EventHandler must only handle events from source.
             */
            final Resource resource = event.getSource();
            if (null != resource) {

                // final Map<String, Object> source =
                // linkBuilder.sourceResource(resource);

                final Map<String, Map<String, Object>> triplets =
                        new HashMap<String, Map<String, Object>>(1);

                String sourceId = linkBuilder.normalizeSourceId(resource.getId());

                /*
                 * The key format of targetLinks Map is #resourceName
                 */
                if (cacheFirst) {
                    /*
                     * If the links are preloaded or the target is preloaded
                     * then find the link/target in the the sourceLinks or
                     * sourceTypedLinks then we need to combine source resources
                     * with them.
                     */
                    if (Mode.ONE_TO_MANY.equals(reconMode)) {

                        // Get from the cache
                        // Expecting a set of triplet for each source!!
                        final ConcurrentSkipListMap<String, Map<String, Object>> cacheEntry =
                                sourceTypedLink.remove(sourceId);
                        if (null != cacheEntry) {
                            for (Map.Entry<String, Map<String, Object>> entry : cacheEntry
                                    .entrySet()) {

                                Triplet triplet = Triplet.fromTriplet(entry.getValue());
                                triplet.source().setResource(resource,
                                        linkBuilder.isSourceCaseSensitive());

                                // TODO Recheck this reference
                                triplet.target().map();
                                triplets.put(entry.getKey(), triplet.map());
                            }
                        } else {
                            triplets.put(DEFAULT_LINK_TYPE, Triplet.fromSource(resource,
                                    linkBuilder.isSourceCaseSensitive()).triplet().map());
                        }
                    } else {
                        Map<String, Object> tmap = sourceLink.remove(sourceId);
                        if (null == tmap) {
                            triplets.put(DEFAULT_LINK_TYPE, Triplet.fromSource(resource,
                                    linkBuilder.isSourceCaseSensitive()).triplet().map());
                        } else {
                            triplets.put(DEFAULT_LINK_TYPE, Triplet.fromTriplet(tmap).source()
                                    .setResource(resource, linkBuilder.isSourceCaseSensitive())
                                    .triplet().map());
                        }
                    }
                } else if (null != linkQueryWithSource) {
                    /*
                     * Execute query or generate the the links with same Id. If
                     * there is a script then it will be called in every case.
                     */
                    try {
                        Triplet triplet =
                                Triplet.fromSource(resource, linkBuilder.isSourceCaseSensitive())
                                        .triplet();

                        Set<Resource> result =
                                executeQuery(context, linkQueryWithSource, triplet, linkBuilder
                                        .getReconProperties());

                        if (result.isEmpty()) {
                            triplets.put(DEFAULT_LINK_TYPE, triplet.map());
                        } else if (result.size() == 1) {

                            triplet.link().setLink(
                                    linkBuilder.linkResource(result.iterator().next()));

                            // // ----------
                            if (preferences.getConfiguration().isPreLoadTargetCollection()) {
                                String targetId = triplet.link().targetId();
                                Utils.Pair<AtomicInteger, Map<String, Object>> t =
                                        targetCollection.get(targetId);
                                if (null != t) {
                                    if (!Mode.MANY_TO_ONE.equals(reconMode) && t.fst.get() > 0) {
                                        // TODO Collision
                                    } else {
                                        triplet.map().put(TARGET_FIELD, t.snd);
                                        t.fst.incrementAndGet();
                                    }
                                }
                            }
                            // // ----------
                            String linkType = triplet.link().linkType();
                            if (null == linkType) {
                                triplets.put(DEFAULT_LINK_TYPE, triplet.map());
                            } else {
                                triplets.put(linkType, triplet.map());
                            }
                        } else {
                            for (Resource linkResource : result) {

                                if (Mode.ONE_TO_MANY.equals(reconMode)) {

                                } else {
                                    /*
                                     * Add ambiguous links
                                     */
                                }
                            }
                        }
                    } catch (ResourceException e) {
                        // TODO Mark this event as failed to query the link
                    }
                } else if (preferences.isLinkGeneratedById(true)) {
                    /*
                     * Execute query or generate the the links with same Id. If
                     * there is a script then it will be called in every case.
                     */
                    Triplet triplet =
                            Triplet.fromSource(resource, linkBuilder.isSourceCaseSensitive())
                                    .triplet();
                    triplet.link().setLink(linkBuilder.generateLink(sourceId, null));
                    triplets.put(DEFAULT_LINK_TYPE, triplet.map());
                }

                // Execute script to qualify the links and add new
                if (null != sourceLinkScript) {

                }
                event.setTriplets(triplets);
            } else if (null != event.getTargetMap()) {
                /*
                 *
                 */
                final Map<String, Map<String, Object>> triplets =
                        new HashMap<String, Map<String, Object>>(1);
                Triplet triplet = Triplet.buildWithTarget(event.getTargetMap());
                if (null != linkQueryWithTarget) {
                    /*
                     * Execute query or generate the the links with same Id. If
                     * there is a script then it will be called in every case.
                     */
                    try {
                        Set<Resource> result =
                                executeQuery(context, linkQueryWithTarget, triplet, linkBuilder
                                        .getReconProperties());

                        if (result.isEmpty()) {
                            triplets.put(DEFAULT_LINK_TYPE, triplet.map());
                        } else if (result.size() == 1) {
                            triplet.link().setLink(
                                    linkBuilder.linkResource(result.iterator().next()));
                            String linkType = triplet.link().linkType();
                            if (null == linkType) {
                                triplets.put(DEFAULT_LINK_TYPE, triplet.map());
                            } else {
                                triplets.put(linkType, triplet.map());
                            }

                        } else {
                            for (Resource linkResource : result) {

                                if (Mode.ONE_TO_MANY.equals(reconMode)) {

                                } else {
                                    /*
                                     * Add ambiguous links
                                     */
                                }
                            }
                        }
                    } catch (ResourceException e) {
                        // TODO Mark this event as failed to query the
                        // link
                    }
                } else if (preferences.isLinkGeneratedById(true)) {
                    /*
                     * Execute query or generate the the links with same Id. If
                     * there is a script then it will be called in every case.
                     */
                    triplet.link()
                            .setLink(linkBuilder.generateLink(triplet.target().getId(), null));
                    triplets.put(DEFAULT_LINK_TYPE, triplet.map());
                } else {

                }
                event.setTriplets(triplets);
            }
        }
    }

    class ReconciliationEventHandler implements EventHandler<ReconEvent> {

        private boolean evenHandler;

        ReconciliationEventHandler(boolean evenHandler) {
            this.evenHandler = evenHandler;
        }

        /**
         * 
         * DO_NOTHING Performs no automated action
         * 
         * ON_CREATE_SOURCE: Creates new source
         * 
         * LINK Assigns the source to target
         * 
         * ON_CREATE_TARGET Creates new target
         * 
         * ON_DELETE_TARGET Removes the target
         * 
         * ON_UPDATE_TARGET Update/Disables the target
         * 
         * <pre>
         *  t       s
         *  a       o
         *  r   l   u
         *  g   i   r
         *  e   n   c
         *  t   k   e
         *  ---------
         *  1   1   1   =   7   CONFIRMED
         *  1   1   0   =   6   SOURCE_MISSING
         *  1   0   1   =   5   FOUND
         *  1   0   0   =   4   UNASSIGNED
         *  0   1   1   =   3   MISSING
         *  0   1   0   =   2   LINK_ONLY
         *  0   0   1   =   1   ABSENT
         *  0   0   0   =   0   ALL_GONE
         * </pre>
         */
        public void onEvent(ReconEvent event, long sequence, boolean endOfBatch) throws Exception {
            // Handle only events
            if ((sequence % 2 == 0) == evenHandler) {
                final Map<String, Map<String, Object>> triplets = event.getTriplets();
                if (null != triplets) {
                    for (Map<String, Object> tmap : triplets.values()) {
                        // Todo check ambiguous links

                        Triplet triplet = Triplet.fromTriplet(tmap);
                        if (triplet.target().exits()) {
                            // If the target was found by correlation or other
                            // ways.
                            if (null != targetCollection) {
                                targetCollection.remove(triplet.target().getId());
                            }
                        }

                        ReconSituation situation = triplet.assess();

                        logger.info("{} triplet:{}", situation.name(), triplet);

                        List<Object> policyActionList = preferences.policyMap.get(situation);

                        if (null != policyActionList) {
                            JsonValue defaultProperties = linkBuilder.getReconProperties();
                            defaultProperties.asMap().putAll(triplet.map());
                            defaultProperties.put("situation", situation.name());
                            // TODO new Recon Context
                            Context actionContext = context;

                            for (Object policyAction : policyActionList) {
                                try {
                                    if (policyAction instanceof Request) {

                                        JsonValue jsonRequest =
                                                ResourceUtil
                                                        .requestToJsonValue((Request) policyAction);
                                        jsonRequest.getTransformers().add(
                                                JsonUtil.getPropertyJsonTransformer(
                                                        defaultProperties, true));
                                        Request request =
                                                ResourceUtil.requestFromJsonValue(jsonRequest
                                                        .copy());

                                        switch (request.getRequestType()) {
                                        case CREATE: {
                                            context.getConnection().create(actionContext,
                                                    (CreateRequest) request);
                                            break;
                                        }
                                        case READ: {
                                            context.getConnection().read(actionContext,
                                                    (ReadRequest) request);
                                            break;
                                        }
                                        case UPDATE: {
                                            context.getConnection().update(actionContext,
                                                    (UpdateRequest) request);
                                            break;
                                        }
                                        case PATCH: {
                                            context.getConnection().patch(actionContext,
                                                    (PatchRequest) request);
                                            break;
                                        }
                                        case QUERY: {
                                            context.getConnection().query(actionContext,
                                                    (QueryRequest) request,
                                                    (QueryResultHandler) null);
                                            break;
                                        }
                                        case DELETE: {
                                            context.getConnection().delete(actionContext,
                                                    (DeleteRequest) request);
                                            break;
                                        }
                                        case ACTION: {
                                            context.getConnection().action(actionContext,
                                                    (ActionRequest) request);
                                            break;
                                        }
                                        }

                                    } else if (policyAction instanceof ScriptEntry) {
                                        Script script =
                                                ((ScriptEntry) policyAction).getScript(actionContext);
                                        Bindings bindings = script.createBindings();
                                        bindings.putAll(defaultProperties.asMap());
                                        script.setBindings(bindings);
                                        script.eval();
                                    }
                                } catch (Throwable t) {
                                    logger.error("Failed to invoke recon action {}",triplet,t);
                                    triplet.error(Utils.adapt(t));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    static class StatisticsEventHandler implements SequenceReportingEventHandler<ReconEvent> {

        private static final String START_TIME = "startTime";
        private static final String END_TIME = "endTime";

        static DateUtil dateUtil = DateUtil.getDateUtil("UTC");

        private long startTime = -1L;
        private long endTime = -1L;

        private long sourceQueryStartTime = -1L;
        private long sourceQueryEndTime = -1L;
        private long targetQueryStartTime = -1L;
        private long targetQueryEndTime = -1L;
        private long linkQueryStartTime = -1L;
        private long linkQueryEndTime = -1L;

        private AtomicInteger linkProcessed = new AtomicInteger(-1);
        private AtomicInteger sourceProcessed = new AtomicInteger(-1);
        private AtomicInteger targetProcessed = new AtomicInteger(-1);

        private AtomicInteger totalSourceEntries = new AtomicInteger(-1);
        private AtomicInteger totalTargetEntries = new AtomicInteger(-1);
        private AtomicInteger totalLinkEntries = new AtomicInteger(-1);

        private AtomicInteger failedEntries = new AtomicInteger(-1);

        private Map<ReconSituation, StringBuilder> ids =
                new EnumMap<ReconSituation, StringBuilder>(ReconSituation.class);
        private AtomicLong processedEntries = new AtomicLong();

        private ConcurrentMap<ReconStage, Map<String, Object>> stageStat =
                new ConcurrentHashMap<ReconStage, Map<String, Object>>(ReconStage.values().length);

        private Sequence sequence = null;

        private final ExecutorPreferences preferences;

        StatisticsEventHandler(final ExecutorPreferences preferences) {
            this.preferences = preferences;
            for (ReconSituation situation : ReconSituation.values()) {
                ids.put(situation, new StringBuilder("["));
            }
            // Initialise the class only
            dateUtil.now();
        }

        public void onEvent(ReconEvent event, long sequence, boolean endOfBatch) throws Exception {
            final Map<String, Map<String, Object>> triplets = event.getTriplets();
            if (null != triplets) {
                for (Map<String, Object> tmap : triplets.values()) {
                    Triplet triplet = Triplet.fromTriplet(tmap);
                    processedEntries.incrementAndGet();

                    if (triplet.hasError()) {
                        failedEntries.incrementAndGet();
                    } else {

                    }

                    if (triplet.target().exits()) {
                        targetProcessed.incrementAndGet();
                    }
                    if (triplet.link().exits()) {
                        linkProcessed.incrementAndGet();
                    }
                    if (triplet.source().exits()) {
                        sourceProcessed.incrementAndGet();
                    }

                    ids.get(triplet.assess()).append(tripletToJSON(triplet));
                }
            }
        }

        /**
         * Confirms if all messages have been consumed by this event processors
         */
        private boolean hasBacklog(final long cursor) {
            if (cursor > sequence.get()) {
                return true;
            }
            return false;
        }

        private long getPosition() {
            return sequence.get();
        }

        @Override
        public void setSequenceCallback(Sequence sequenceCallback) {
            sequence = sequenceCallback;
        }

        // ----- Statistics Methods

        public void reconStart() {
            startTime = System.currentTimeMillis();
        }

        public void reconEnd() {
            endTime = System.currentTimeMillis();
        }

        public void startStage(ReconStage stage) {
            Map<String, Object> stageEntry = new ConcurrentHashMap<String, Object>(1);
            stageEntry.put(START_TIME, Long.valueOf(System.currentTimeMillis()));
            stageStat.putIfAbsent(stage, stageEntry);
        }

        public void endStage(ReconStage stage) {
            Map<String, Object> stageEntry = stageStat.get(stage);
            if (stageEntry != null) {
                stageEntry.put(END_TIME, Long.valueOf(System.currentTimeMillis()));
            }
        }

        void linkIncrementTotal() {
            totalLinkEntries.incrementAndGet();
        }

        void sourceIncrementTotal() {
            totalSourceEntries.incrementAndGet();
        }

        void targetIncrementTotal() {
            totalTargetEntries.incrementAndGet();
        }

        public void sourceQueryStart() {
            if (sourceQueryStartTime < 0)
                sourceQueryStartTime = System.currentTimeMillis();
        }

        public void sourceQueryEnd() {
            if (sourceQueryEndTime < 0)
                sourceQueryEndTime = System.currentTimeMillis();
        }

        public void targetQueryStart() {
            if (targetQueryStartTime < 0)
                targetQueryStartTime = System.currentTimeMillis();
        }

        public void targetQueryEnd() {
            if (targetQueryEndTime < 0)
                targetQueryEndTime = System.currentTimeMillis();
        }

        public void linkQueryStart() {
            if (linkQueryStartTime < 0)
                linkQueryStartTime = System.currentTimeMillis();
        }

        public void linkQueryEnd() {
            if (linkQueryEndTime < 0)
                linkQueryEndTime = System.currentTimeMillis();
        }

        /**
         * @return The number of existing source objects processed
         */
        public int getSourceProcessed() {
            return sourceProcessed.get();
        }

        /**
         * @return The number of existing target objects processed
         */
        public int getTargetProcessed() {
            return targetProcessed.get();
        }

        /**
         * @return The number of existing links processed
         */
        public int getLinkProcessed() {
            return linkProcessed.get();
        }

        /**
         * @return The reconciliation start time, formatted
         */
        public String getStarted() {
            String startFormatted = "";
            if (startTime > 0) {
                startFormatted = dateUtil.formatDateTime(new Date(startTime));
            }
            return startFormatted;
        }

        /**
         * @return The reconciliation end time, formatted, or empty string if
         *         not ended
         */
        public String getEnded() {
            String endFormatted = "";
            if (endTime > 0) {
                endFormatted = dateUtil.formatDateTime(new Date(endTime));
            }
            return endFormatted;
        }

        public long duration(TimeUnit timeUnit) {
            TimeUnit target = null != timeUnit ? timeUnit : TimeUnit.SECONDS;

            if (startTime > 0) {
                if (endTime > startTime) {
                    return target.convert(endTime - startTime, TimeUnit.MILLISECONDS);
                } else {
                    return target.convert(System.currentTimeMillis() - startTime,
                            TimeUnit.MILLISECONDS);
                }
            } else {
                return -1L;
            }
        }

        public Map<String, Object> asMap() {

            Map<String, Object> results = new HashMap();
            results.put(START_TIME, getStarted());
            results.put(END_TIME, getEnded());
            results.put("duration", Long.toString(duration(TimeUnit.SECONDS)) + " sec");
            results.put("state", preferences.getStage().getState());
            results.put("stage", preferences.getStage().name());
            results.put("stageDescription", preferences.getStage().getDescription());
            results.put("progress", getProgress());
            return results;
        }

        /**
         * @return the populated run progress structure
         */
        public Map<String, Object> getProgress() {
            // Unknown total entries are currently represented via question mark
            // string.
            String totalSourceEntriesStr =
                    (totalSourceEntries.get() < 0 ? "?" : Integer
                            .toString(totalSourceEntries.get()));
            String totalTargetEntriesStr =
                    (totalTargetEntries.get() < 0 ? "?" : Integer
                            .toString(totalTargetEntries.get()));

            String totalLinkEntriesStr = "?";
            if (totalLinkEntries.get() < 0) {
                if (preferences.getStage() == ReconStage.COMPLETED_SUCCESS) {
                    totalLinkEntriesStr = Integer.toString(getLinkProcessed());
                }
            } else {
                totalLinkEntriesStr = Integer.toString(totalLinkEntries.get());
            }

            Map<String, Object> progressDetail = new LinkedHashMap<String, Object>();
            Map<String, Object> sourceDetail = new LinkedHashMap<String, Object>();
            Map<String, Object> sourceExisting = new LinkedHashMap<String, Object>();
            Map<String, Object> targetDetail = new LinkedHashMap<String, Object>();
            Map<String, Object> targetExisting = new LinkedHashMap<String, Object>();
            Map<String, Object> linkDetail = new LinkedHashMap<String, Object>();
            Map<String, Object> linkExisting = new LinkedHashMap<String, Object>();

            sourceExisting.put("processed", getSourceProcessed());
            sourceExisting.put("total", totalSourceEntriesStr);
            sourceDetail.put("existing", sourceExisting);
            progressDetail.put("source", sourceDetail);

            targetExisting.put("processed", getTargetProcessed());
            targetExisting.put("total", totalTargetEntriesStr);
            targetDetail.put("existing", targetExisting);
            progressDetail.put("target", targetDetail);

            linkExisting.put("processed", getLinkProcessed());
            linkExisting.put("total", totalLinkEntriesStr);
            linkDetail.put("existing", linkExisting);
            progressDetail.put("links", linkDetail);

            return progressDetail;
        }

    }

    static class CorrelateEventHandler implements EventHandler<ReconEvent> {

        private final ServerContext context;
        private final QueryRequest correlationQuery;
        private final ScriptEntry correlationScript;
        private final LinkBuilder linkBuilder;

        CorrelateEventHandler(final ServerContext context, final QueryRequest correlationQuery,
                final ScriptEntry correlationScript, final LinkBuilder linkBuilder) {
            this.context = context;
            this.correlationQuery = correlationQuery;
            this.correlationScript = correlationScript;
            this.linkBuilder = linkBuilder;
        }

        /**
         * List of filed to get when a target object is read.
         */
        private final JsonPointer[] fields = null;

        public void onEvent(ReconEvent event, long sequence, boolean endOfBatch) throws Exception {
            final Map<String, Map<String, Object>> triplets = event.getTriplets();
            if (null != triplets) {
                for (Map<String, Object> tmap : triplets.values()) {
                    Triplet triplet = Triplet.fromTriplet(tmap);
                    if (triplet.hasError() || triplet.link().exits() || triplet.target().notFound()
                            || triplet.target().exits()) {
                        continue;
                    }

                    /*
                     * Execute the Correlation query only if there is no link or
                     * links with linkType and not targetId?
                     */

                    if (null != correlationQuery) {
                        try {
                            Set<Resource> targets =
                                    executeQuery(context, correlationQuery, triplet, linkBuilder
                                            .getReconProperties());
                            if (targets.isEmpty()) {
                                // TODO how to mark if nothing found
                                triplet.target().setResource(null,
                                        linkBuilder.isTargetCaseSensitive());
                            } else if (targets.size() == 1) {
                                triplet.target().setResource(targets.iterator().next(),
                                        linkBuilder.isTargetCaseSensitive());
                            } else {
                                for (Resource r : targets) {
                                    triplet.match(resourceToMap(r, linkBuilder
                                            .isSourceCaseSensitive()));
                                }
                            }
                        } catch (ResourceException e) {
                            triplet.error(e);
                            logger.error("Failed to query", e);
                        } catch (Exception e) {
                            triplet.error(ResourceException.getException(
                                    ResourceException.INTERNAL_ERROR,
                                    "Failed to execute correlation query", e));
                            // TODO Mark the event as failed.
                            logger.error("Failed to query", e);
                        }
                    } else if (null != correlationScript) {

                        /////

//                    } else if (correlationQuery != null
//                            && (correlateEmptyTargetSet || !hadEmptyTargetObjectSet())) {
//                        EventEntry measure =
//                                Publisher.start(EVENT_CORRELATE_TARGET, getSourceObject(), null);
//
//                        Map<String, Object> queryScope = service.newScope();
//                        if (sourceObjectOverride != null) {
//                            queryScope.put("source", sourceObjectOverride.asMap());
//                        } else {
//                            queryScope.put("source", getSourceObject().asMap());
//                        }
//                        try {
//                            Object query = correlationQuery.exec(queryScope);
//                            if (query == null || !(query instanceof Map)) {
//                                throw new ResourceException(
//                                        "Expected correlationQuery script to yield a Map");
//                            }
//                            result =
//                                    new JsonValue(queryTargetObjectSet((Map) query)).get(
//                                            QueryConstants.QUERY_RESULT).required();
//                        } catch (ScriptException se) {
//                            logger.debug("{} correlationQuery script encountered exception", name, se);
//                            throw new ResourceException(se);
//                        } finally {
//                            measure.end();
//                        }
//                    }

                        /////



                        try {
                            Script script = correlationScript.getScript(context);
                            //TODO This is null!!
                            script.put("correlationQuery", Requests
                                    .copyOfQueryRequest(correlationQuery));
                            script.getBindings().putAll(triplet.map());
                            Object result = script.eval();
                            if (result instanceof QueryRequest) {
                                Set<Resource> correlationResult = new HashSet<Resource>();
                                // TODO handle paged results
                                context.getConnection().query(context, (QueryRequest) result,
                                        correlationResult);
                            } else if (result instanceof Collection) {

                            } else {

                            }
                        } catch (NotFoundException e) {

                        } catch (RetryableException e) {
                        } catch (ResourceException e) {

                        } catch (Exception e) {

                        }

                    }

                }
            }
        }
    }

    static class ConfirmationEventHandler implements EventHandler<ReconEvent> {

        private final ServerContext context;

        private final ScriptEntry scriptEntry;

        ConfirmationEventHandler(final ServerContext context, final ScriptEntry scriptEntry) {
            this.context = context;
            this.scriptEntry = scriptEntry;
        }

        public void onEvent(ReconEvent event, long sequence, boolean endOfBatch) throws Exception {
            // If the correlation query has multiple result then pick one
            final Map<String, Map<String, Object>> triplets = event.getTriplets();
            if (null != triplets) {
                for (Map<String, Object> triplet : triplets.values()) {
                    if (triplet.containsKey(SOMETHING_TO_NAME)) {

                        Script script = scriptEntry.getScript(context);
                        // TODO This is not correct, the AMBIGUOUS_FIELD filed
                        // is used for links
                        script.put("correlationResult", triplet.get(SOMETHING_TO_NAME));

                        final AtomicReference<Map<String, Object>> selected =
                                new AtomicReference<Map<String, Object>>();

                        script.putSafe("confirm", new Function<Boolean>() {

                            private static final long serialVersionUID = 1L;

                            @Override
                            public Boolean call(Parameter scope, Function<?> callback,
                                    Object... arguments) throws ResourceException,
                                    NoSuchMethodException {
                                if (arguments.length == 1 && arguments[0] instanceof Resource) {
                                    return selected.compareAndSet(null,
                                            (Map<String, Object>) arguments[0]);
                                } else {
                                    throw new NoSuchMethodException(FunctionFactory
                                            .getNoSuchMethodMessage("confirm", arguments));
                                }
                            }
                        });

                        try {
                            script.eval();
                        } catch (Exception e) {
                            // TODO Mark the event as failed.
                        }
                        if (null != selected.get()) {
                            triplet.put(TARGET_FIELD, selected.get());
                        } else {
                            // TODO Mark the event as failed or it's really no
                            // target found/confirmed
                        }
                    }
                }
            }
        }
    }

    static class TargetLoaderEventHandler implements EventHandler<ReconEvent> {

        private final ServerContext context;

        private final LinkBuilder linkBuilder;

        /**
         * List of filed to get when a tergat object is read.
         */
        private final JsonPointer[] fields;

        TargetLoaderEventHandler(final ServerContext context, final Request readRequest,
                final LinkBuilder linkBuilder) {
            this.context = context;
            this.linkBuilder = linkBuilder;
            if (!readRequest.getFields().isEmpty()) {
                fields =
                        readRequest.getFields().toArray(
                                new JsonPointer[readRequest.getFields().size()]);
            } else {
                fields = null;
            }
        }

        public void onEvent(ReconEvent event, long sequence, boolean endOfBatch) throws Exception {
            if (!event.getTriplets().isEmpty()) {
                final Map<String, Map<String, Object>> triplets = event.getTriplets();
                if (null != triplets) {
                    for (Map<String, Object> tmap : triplets.values()) {
                        Triplet triplet = Triplet.fromTriplet(tmap);

                        if (triplet.hasError() || !triplet.link().exits()
                                || triplet.target().notFound() || triplet.target().exits()) {
                            continue;
                        }

                        String targetId = triplet.link().targetId();
                        if (targetId != null) {
                            ReadRequest request =
                                    Requests.newReadRequest(linkBuilder
                                            .targetResourceName(targetId));
                            if (null != fields) {
                                request.addField(fields);
                            }
                            try {
                                Resource resource = context.getConnection().read(context, request);

                                triplet.target().setResource(resource,
                                        linkBuilder.isTargetCaseSensitive());
                            } catch (NotFoundException e) {
                                triplet.target().setResource(null,
                                        linkBuilder.isTargetCaseSensitive());
                            } catch (ResourceException e) {
                                triplet.error(e);
                                logger.error("Failed to read the target: {}", request, e);
                            } catch (Exception e) {
                                triplet.error(new InternalServerErrorException(e.getMessage(), e));
                                logger.error("Failed to read the target: {}", request, e);
                            }
                        } else {
                            logger.error("There is a link but the link has targetId??",
                                    new JsonValue(triplet));
                        }
                    }
                }
            }
        }
    }

    public final class ReconEvent {

        private Resource source = null;
        private Resource target = null;
        private Map<String, Object> targetMap = null;

        private final AtomicReference<Map<String, Map<String, Object>>> tripletsReference =
                new AtomicReference<Map<String, Map<String, Object>>>();

        /**
         * Get the target resource for
         * {@link org.forgerock.openidm.internal.recon.ReconExecutor.TargetResourceEventHandler}
         * . The event handler process events where it's not null. The Handler
         * set it null after processed this event.
         * 
         * @return target resource or null if this event is not meant for
         *         {@link org.forgerock.openidm.internal.recon.ReconExecutor.TargetResourceEventHandler}
         *         .
         */
        public Resource getTarget() {
            return target;
        }

        /**
         * Set the target resource handled during the target query.
         * 
         * @param target
         *            the target resource.
         */
        public void setTarget(final Resource target) {
            this.target = target;
        }

        public Map<String, Object> getTargetMap() {
            return targetMap;
        }

        public void setTargetMap(final Map<String, Object> targetMap) {
            this.targetMap = targetMap;
        }

        /**
         * Get the source resource for
         * {@link org.forgerock.openidm.internal.recon.ReconExecutor.SourceResourceEventHandler}
         * . The event handler process events where it's not null. The Handler
         * set it null after processed this event.
         * 
         * @return source resource or null if this event is not meant for
         *         {@link org.forgerock.openidm.internal.recon.ReconExecutor.SourceResourceEventHandler}
         *         .
         */
        public Resource getSource() {
            return source;
        }

        /**
         * Set the source resource handled during the target query.
         * 
         * @param source
         *            the target resource.
         */
        public void setSource(Resource source) {
            this.source = source;
        }

        /**
         * Get the triplets to process and handle the recon situation.
         * 
         * @return the non-null list of all Triplets assigned with this event.
         */
        public Map<String, Map<String, Object>> getTriplets() {
            return tripletsReference.get();
        }

        /**
         * Get the triplets to process and handle the recon situation.
         * 
         * @return the non-null list of all Triplets assigned with this event.
         */
        public void setTriplets(final Map<String, Map<String, Object>> triplets) {
            tripletsReference.set(triplets);
        }

        /**
         * Null all internal variable. No EventHandler will process this
         * ReconEvent
         */
        public void clear() {
            source = null;
            target = null;
            targetMap = null;
            tripletsReference.set(null);
        }

    }

    //TODO Use the Shared LinkType class instance
    static class LinkBuilder {

        final boolean reverseLink;

        /**
         * Whether to link source IDs in a case sensitive fashion. Default to
         * {@code TRUE}
         */

        final boolean sourceIdsCaseSensitive;

        /**
         * Whether to link target IDs in a case sensitive fashion. Default to
         * {@code TRUE}
         */
        final boolean targetIdsCaseSensitive;

        final String sourceResourceCollection;

        final String targetResourceCollection;

        LinkBuilder(ConfigurationProvider provider) {
            this.reverseLink = provider.reverseLink;
            this.sourceIdsCaseSensitive = provider.sourceIdCaseSensitive;
            this.targetIdsCaseSensitive = provider.targetIdCaseSensitive;
            this.sourceResourceCollection = provider.getSource().getResourceName();
            this.targetResourceCollection = provider.getTarget().getResourceName();
        }

        public JsonValue getReconProperties() {
            JsonValue properties = new JsonValue(new HashMap<String, Object>(2));
            properties.put("sourceResourceCollection", sourceResourceCollection);
            properties.put("targetResourceCollection", targetResourceCollection);
            return properties;
        }

        public Map<String, Object> generateLink(String id, String linkType) {
            Map<String, Object> link = new HashMap<String, Object>(2);
            // Map<String, Object> resource = new HashMap<String, Object>(2);
            // link.put("resource", resource);
            // resource.put("firstId",id);
            // resource.put("secondId",id);

            if (null != linkType && !DEFAULT_LINK_TYPE.equals(linkType)) {
                link.put("linkType", linkType);
                // resource.put("type", linkType);
            }

            link.put("sourceId", id);
            link.put("targetId", id);

            return link;
        }

        public Map<String, Object> linkResource(final Resource linkResource) {
            JsonValue result = linkResource.getContent();
            Map<String, Object> link = new LinkedHashMap<String, Object>(3);
            if (useReverse()) {
                link.put("sourceId", result.get("secondId").required().asString());
                link.put("targetId", result.get("firstId").required().asString());
            } else {
                link.put("sourceId", result.get("firstId").required().asString());
                link.put("targetId", result.get("secondId").required().asString());
            }
            String linkType = result.get("type").asString();
            if (null != linkType) {
                link.put("linkType", linkType);
            }
            // This may be too heavy but may contains required data!!
            link.put("resource", result.asMap());
            link.put(Resource.FIELD_CONTENT_ID, linkResource.getId());
            if (null != linkResource.getRevision()) {
                link.put(Resource.FIELD_CONTENT_REVISION, linkResource.getRevision());
            }
            return link;
        }

        /**
         * 
         * @param resource
         * @return
         * @throws org.forgerock.json.fluent.JsonValueException
         *             if {@code firstId, secondId} parameters are not Strings.
         */

        /**
         * Normalizes the source ID if required, e.g. make lower case for case
         * insensitive id comparison purposes
         * 
         * @param sourceId
         *            the original id
         * @return normalized id
         */
        public String normalizeSourceId(String sourceId) {
            if (isSourceCaseSensitive()) {
                return sourceId;
            } else {
                return (sourceId == null ? null : sourceId.toLowerCase());
            }
        }

        /**
         * Normalizes the target ID if required, e.g. make lower case for case
         * insensitive id comparison purposes
         * 
         * @param targetId
         *            the original id
         * @return normalized id
         */
        public String normalizeTargetId(String targetId) {
            if (isTargetCaseSensitive()) {
                return targetId;
            } else {
                return (targetId == null ? null : targetId.toLowerCase());
            }
        }

        /**
         * Build full resourceName prefixed with the targetResourceCollection
         * name.
         * 
         * @param targetId
         * @return null if the {@code target} parametr is null else the full
         *         resourceName.
         */
        public String targetResourceName(String targetId) {
            if (null != targetId) {
                return targetResourceCollection + "/" + normalizeTargetId(targetId);
            }
            return null;
        }

        public boolean useReverse() {
            return reverseLink;
        }

        /**
         * @return whether the source id is case sensitive
         */
        public boolean isSourceCaseSensitive() {
            return sourceIdsCaseSensitive;
        }

        /**
         * @return whether the target id is case sensitive
         */
        public boolean isTargetCaseSensitive() {
            return targetIdsCaseSensitive;
        }
    }

    /**
     * Executor to run the Disruptor EventHandler.
     */
    static class ReconJobExecutor implements Executor {

        private ThreadFactory threadFactory;

        ReconJobExecutor(final ThreadFactory threadFactory) {
            this.threadFactory = threadFactory;
        }

        @Override
        public void execute(final Runnable command) {
            Thread t = threadFactory.newThread(command);
            t.start();
        }
    }

    /**
     * The default daemon thread factory
     */
    static class DefaultThreadFactory implements ThreadFactory {
        final Collection<Thread> threads = new CopyOnWriteArrayList<Thread>();
        final ThreadGroup group;
        final AtomicInteger threadNumber = new AtomicInteger(1);
        final String namePrefix;

        DefaultThreadFactory(String name) {
            group = new ThreadGroup("OpenIDM Recon");
            namePrefix = "OpenIDM Recon '" + name + "' Thread #: ";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            t.setDaemon(true);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            logger.info("New Thread: {}", t.getName());
            threads.add(t);
            return t;
        }

        public void joinAllThreads() {
            for (Thread thread : threads) {
                if (thread.isAlive()) {
                    try {
                        thread.join(5000);
                    } catch (InterruptedException e) {
                        logger.error("Recon thread is interrupted: {}", thread.getName());
                    }
                }
                if (thread.isAlive()) {
                    logger.error("Recon thread is still alive: {}", thread.getName());
                }
            }
            threads.clear();
        }
    }
}
