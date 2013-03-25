/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.internal.sync;

// Java Standard Edition

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.RetryableException;
import org.forgerock.openidm.config.InvalidException;
import org.forgerock.openidm.internal.recon.ReconUtil;
import org.forgerock.script.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.forgerock.openidm.internal.recon.ReconUtil.executeQuery;
import static org.forgerock.openidm.internal.recon.ReconUtil.resourceToMap;

// SLF4J
// JSON Fluent library
// OpenIDM

/**
 * Represents the sharable Link Types, and helpers for mappings to find and use
 * matching link types as mappings can share link sets, bi-directional.
 * 
 * @author aegloff
 */
class LinkType {

    /**
     * Setup logging for the {@link LinkType}.
     */
    private final static Logger logger = LoggerFactory.getLogger(LinkType.class);

    // The link type name
    String name;

    // The linked object sets, bi-directional
    String firstObjectSet;
    String secondObjectSet;

    // Whether linking should be case sensitive on the first or second IDs
    boolean firstCaseSensitive;
    boolean secondCaseSensitive;

    // Represents how this LinkType matches a given ObjectMapping
    Match match;

    /**
     * LinkType factory method
     * 
     * @param forMapping
     *            the mapping to find the link type for
     * @param allMappings
     *            all configured mappings
     * @return the link type matching the requested
     */
    public static LinkType getLinkType(ObjectMapping forMapping, List<ObjectMapping> allMappings) {
        ObjectMapping linkDefiner = null;
        String linkTypeName = forMapping.getLinkTypeName();
        for (ObjectMapping otherMapping : allMappings) {
            if (linkTypeName.equals(otherMapping.getLinkTypeName())) {
                // Link order is influenced by link type name being same as the
                // (defining) mapping name.
                // Enhancement to consider: May want to allow explicit LinkType
                // definition option
                if (linkTypeName.equals(otherMapping.getName())) {
                    linkDefiner = otherMapping;
                }
            }
        }
        if (linkDefiner == null) {
            String warning =
                    "When using links from another mapping the links must be named according to the mapping."
                            + " Could not find a mapping for " + linkTypeName + " used in "
                            + forMapping.getName();
            throw new InvalidException(warning);
        }

        LinkType.Match match = Match.getLinkTypeMatch(forMapping, linkDefiner);

        return new LinkType(linkDefiner.getName(), linkDefiner.getSourceObjectSet(), linkDefiner
                .getTargetObjectSet(), linkDefiner.getSourceIdsCaseSensitive(), linkDefiner
                .getTargetIdsCaseSensitive(), match);
    }

    public String getName() {
        return name;
    }

    public boolean useReverse() {
        return Match.MATCH_REVERSE.equals(match);
    }

    public boolean getFirstCaseSensitive() {
        return firstCaseSensitive;
    }

    public boolean getSecondCaseSensitive() {
        return secondCaseSensitive;
    }



    LinkType(String name, String firstObjectSet, String secondObjectSet,
            boolean firstCaseSensitive, boolean secondCaseSensitive, Match match) {
        this.name = name;
        this.firstObjectSet = firstObjectSet;
        this.secondObjectSet = secondObjectSet;
        this.firstCaseSensitive = firstCaseSensitive;
        this.secondCaseSensitive = secondCaseSensitive;
        this.match = match;
    }

    /**
     * Normalizes the source ID if required, e.g. make lower case for case
     * insensitive id comparison purposes
     * 
     * @param aSourceId
     *            the original id
     * @return normalized id
     */
    public String normalizeSourceId(String aSourceId) {
        if (!isSourceCaseSensitive()) {
            return (aSourceId == null ? null : aSourceId.toLowerCase());
        } else {
            return aSourceId;
        }
    }

    /**
     * Normalizes the target ID if required, e.g. make lower case for case
     * insensitive id comparison purposes
     * 
     * @param aSourceId
     *            the original id
     * @return normalized id
     */
    public String normalizeTargetId(String aTargetId) {
        if (!isTargetCaseSensitive()) {
            return (aTargetId == null ? null : normalizeId(aTargetId));
        } else {
            return aTargetId;
        }
    }

    /**
     * Unconditionally normalizes the given ID, currently to lower case
     * 
     * @param anId
     *            the original id
     * @return normalized id
     */
    public String normalizeId(String anId) {
        return (anId == null ? null : anId.toLowerCase());
    }

    public String sourceResourceContainer(){
        if (useReverse()){
            return secondObjectSet;
        } else {
            return firstObjectSet;
        }
    }

    public String targetResourceContainer(){
        if (useReverse()){
            return firstObjectSet;
        } else {
            return secondObjectSet;
        }
    }

    /**
     * @return whether the source id is case sensitive
     */
    public boolean isSourceCaseSensitive() {
        if (useReverse()) {
            return getSecondCaseSensitive();
        } else {
            return getFirstCaseSensitive();
        }
    }

    /**
     * @return whether the target id is case sensitive
     */
    public boolean isTargetCaseSensitive() {
        if (useReverse()) {
            return getFirstCaseSensitive();
        } else {
            return getSecondCaseSensitive();
        }
    }

    /**
     * Determines whether the link types are compatible and in which order
     * (direction)
     */
    public enum Match {
        MATCH_EXACT, MATCH_REVERSE;

        private final static Logger logger = LoggerFactory.getLogger(Match.class);

        static Match getLinkTypeMatch(ObjectMapping forMapping, ObjectMapping linkDefiner) {
            if (forMapping.getSourceObjectSet().equals(linkDefiner.getSourceObjectSet())
                    && forMapping.getTargetObjectSet().equals(linkDefiner.getTargetObjectSet())) {
                if (forMapping.equals(linkDefiner)) {
                    logger.info("Mapping {} defines links named {}.", linkDefiner.getName(),
                            linkDefiner.getLinkTypeName());
                } else {
                    logger.info("Mapping {} shares the links of {} named {}.",
                            new Object[] { forMapping.getName(), linkDefiner.getName(),
                                linkDefiner.getLinkTypeName() });
                }
                return MATCH_EXACT;
            } else if (forMapping.getSourceObjectSet().equals(linkDefiner.getTargetObjectSet())
                    && forMapping.getTargetObjectSet().equals(linkDefiner.getSourceObjectSet())) {
                logger.info(
                        "Mapping {} shares the links of {} in the opposite direction, named {}.",
                        new Object[] { forMapping.getName(), linkDefiner.getName(),
                            linkDefiner.getLinkTypeName() });
                return MATCH_REVERSE;
            } else {
                throw new InvalidException("Mappings " + forMapping.getName() + " and "
                        + linkDefiner.getName() + " {} are configured to share the same links of "
                        + linkDefiner.getLinkTypeName()
                        + ", but use incompatible source or targets: ("
                        + forMapping.getSourceObjectSet() + "->" + forMapping.getTargetObjectSet()
                        + ") vs (" + linkDefiner.getSourceObjectSet() + "->"
                        + linkDefiner.getTargetObjectSet() + ")");
            }
        }
    }

    /**
     * Correlates (finds an associated) target for the given source
     * 
     * @param triplet
     * 
     * @return
     * @throws ResourceException
     *             if the correlation failed.
     */
    @SuppressWarnings("unchecked")
    private Set<Resource> correlateTarget(ReconUtil.Triplet triplet) throws ResourceException {

        /*
                     * Execute the Correlation query only if there is no link or
                     * links with linkType and not targetId?
                     */

//        if (null != correlationQuery) {
//            try {
//                Set<Resource> targets =
//                        executeQuery(context, correlationQuery, triplet, linkBuilder
//                                .getReconProperties());
//                if (targets.isEmpty()) {
//                    // TODO how to mark if nothing found
//                    triplet.target().setResource(null,
//                            linkBuilder.isTargetCaseSensitive());
//                } else if (targets.size() == 1) {
//                    triplet.target().setResource(targets.iterator().next(),
//                            linkBuilder.isTargetCaseSensitive());
//                } else {
//                    for (Resource r : targets) {
//                        triplet.match(resourceToMap(r, linkBuilder
//                                .isSourceCaseSensitive()));
//                    }
//                }
//            } catch (ResourceException e) {
//                triplet.error(e);
//                logger.error("Failed to query", e);
//            } catch (Exception e) {
//                triplet.error(ResourceException.getException(
//                        ResourceException.INTERNAL_ERROR,
//                        "Failed to execute correlation query", e));
//                // TODO Mark the event as failed.
//                logger.error("Failed to query", e);
//            }
//        } else if (null != correlationScript) {
//
//            /////
//
////                    } else if (correlationQuery != null
////                            && (correlateEmptyTargetSet || !hadEmptyTargetObjectSet())) {
////                        EventEntry measure =
////                                Publisher.start(EVENT_CORRELATE_TARGET, getSourceObject(), null);
////
////                        Map<String, Object> queryScope = service.newScope();
////                        if (sourceObjectOverride != null) {
////                            queryScope.put("source", sourceObjectOverride.asMap());
////                        } else {
////                            queryScope.put("source", getSourceObject().asMap());
////                        }
////                        try {
////                            Object query = correlationQuery.exec(queryScope);
////                            if (query == null || !(query instanceof Map)) {
////                                throw new ResourceException(
////                                        "Expected correlationQuery script to yield a Map");
////                            }
////                            result =
////                                    new JsonValue(queryTargetObjectSet((Map) query)).get(
////                                            QueryConstants.QUERY_RESULT).required();
////                        } catch (ScriptException se) {
////                            logger.debug("{} correlationQuery script encountered exception", name, se);
////                            throw new ResourceException(se);
////                        } finally {
////                            measure.end();
////                        }
////                    }
//
//            /////
//
//
//
//            try {
//                Script script = correlationScript.getScript(context);
//                script.put("correlationQuery", Requests
//                        .copyOfQueryRequest(correlationQuery));
//
//                Object result = script.eval();
//                if (request instanceof QueryRequest) {
//                    Set<Resource> correlationResult = new HashSet<Resource>();
//                    // TODO handle paged results
//                    context.getConnection().query(context, (QueryRequest) result,
//                            correlationResult);
//                } else if (result instanceof Collection) {
//
//                } else {
//
//                }
//            } catch (NotFoundException e) {
//
//            } catch (RetryableException e) {
//            } catch (ResourceException e) {
//
//            } catch (Exception e) {
//
//            }
//
//        }

        return Collections.emptySet();
    }
}
