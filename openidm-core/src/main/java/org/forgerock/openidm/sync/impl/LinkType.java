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

// Java Standard Edition
import java.util.List;

// SLF4J
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// OpenIDM
import org.forgerock.openidm.config.enhanced.InvalidException;

/**
 * Represents the sharable Link Types,
 * and helpers for mappings to find and use matching link types
 * as mappings can share link sets, bi-directional.
 */
class LinkType {

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
     * @param forMapping the mapping to find the link type for
     * @param allMappings all configured mappings
     * @return the link type matching the requested
     */
    public static LinkType getLinkType(ObjectMapping forMapping, List<ObjectMapping> allMappings) {
        ObjectMapping linkDefiner = null;
        String linkTypeName = forMapping.getLinkTypeName();
        for (ObjectMapping otherMapping : allMappings) {
            if (linkTypeName.equals(otherMapping.getLinkTypeName())) {
                // Link order is influenced by link type name being same as the (defining) mapping name.
                // Enhancement to consider: May want to allow explicit LinkType definition option
                if (linkTypeName.equals(otherMapping.getName())) {
                    linkDefiner = otherMapping;
                }
            }
        }
        if (linkDefiner == null) {
            String warning = "When using links from another mapping the links must be named according to the mapping."
            + " Could not find a mapping for " + linkTypeName + " used in " + forMapping.getName();
            throw new InvalidException(warning);
        }

        LinkType.Match match = Match.getLinkTypeMatch(forMapping, linkDefiner);

        return new LinkType(linkDefiner.getName(), linkDefiner.getSourceObjectSet(), linkDefiner.getTargetObjectSet(),
                linkDefiner.getSourceIdsCaseSensitive(), linkDefiner.getTargetIdsCaseSensitive(), match);
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
     * Normalizes the source ID if required, e.g. make lower case for
     * case insensitive id comparison purposes
     * @param aSourceId the original id
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
     * Normalizes the target ID if required, e.g. make lower case for
     * case insensitive id comparison purposes
     * @param aSourceId the original id
     * @return normalized id
     */
    public String normalizeTargetId(String aTargetId) {
        if (!isTargetCaseSensitive()) {
            return (aTargetId == null ? null : aTargetId.toLowerCase());
        } else {
            return aTargetId;
        }
    }

    /**
     * Unconditionally normalizes the given ID, currently to lower case
     * @param anId the original id
     * @return normalized id
     */
    public String normalizeId(String anId) {
        return (anId == null ? null : anId.toLowerCase());
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
     * Determines whether the link types are compatible and in which order (direction)
     */
    public enum Match {
        MATCH_EXACT,
        MATCH_REVERSE;

        private final static Logger logger = LoggerFactory.getLogger(Match.class);

        static Match getLinkTypeMatch(ObjectMapping forMapping, ObjectMapping linkDefiner) {
            if (forMapping.getSourceObjectSet().equals(linkDefiner.getSourceObjectSet())
                    && forMapping.getTargetObjectSet().equals(linkDefiner.getTargetObjectSet())) {
                if (forMapping.equals(linkDefiner)) {
                    logger.info("Mapping {} defines links named {}.", linkDefiner.getName(), linkDefiner.getLinkTypeName());
                } else {
                    logger.info("Mapping {} shares the links of {} named {}.", new Object[] {forMapping.getName(), linkDefiner.getName(), linkDefiner.getLinkTypeName()});
                }
                return MATCH_EXACT;
            } else if (forMapping.getSourceObjectSet().equals(linkDefiner.getTargetObjectSet())
                    && forMapping.getTargetObjectSet().equals(linkDefiner.getSourceObjectSet())) {
                logger.info("Mapping {} shares the links of {} in the opposite direction, named {}.", new Object[] {forMapping.getName(), linkDefiner.getName(), linkDefiner.getLinkTypeName()});
                return MATCH_REVERSE;
            } else {
                throw new InvalidException("Mappings " + forMapping.getName() + " and " + linkDefiner.getName() + " {} are configured to share the same links of "
                        + linkDefiner.getLinkTypeName() + ", but use incompatible source or targets: (" + forMapping.getSourceObjectSet() + "->" + forMapping.getTargetObjectSet()
                        + ") vs (" + linkDefiner.getSourceObjectSet() + "->" + linkDefiner.getTargetObjectSet() + ")");
            }
        }
    }
}


