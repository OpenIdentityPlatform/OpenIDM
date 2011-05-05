/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.sync.impl;

import java.util.Map;
import java.util.HashMap;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;

/**
 * A bean like wrapper for {@code objectSynchronization} {@code mappings} json
 * configuration structure. Maps {@code sourceObjects} to their defined
 * {@link PropertyEntry}'s
 */
public class MappingConfiguration {

    private static final long serialVersionUID = 1L;
    final static Logger logger = LoggerFactory.getLogger(MappingConfiguration.class);

    private Map<String, Collection<MapEntry>> sourceObjectToMappings;

    private static final String SYNCHRONOUS = "synchronous";
    private static final String ASYNCHRONOUS = "asynchronous";

    /**
     * Construct a mapping configuration from json configuration data.
     *
     * @param mappingConfiguration
     * @throws JsonNodeException
     */
    public MappingConfiguration(Map<String, Object> mappingConfiguration) throws JsonNodeException {
        sourceObjectToMappings = new HashMap<String, Collection<MapEntry>>();
        buildMappings(mappingConfiguration);
    }

    /**
     * Build out the individual {@link MapEntry}'s that are defined in the mapping configuration and
     * map the entries by {@code sourceObject}.
     *
     * @param mappingConfiguration
     * @return sourceObjectToEntryMap
     * @throws JsonNodeException
     */
    @SuppressWarnings("unchecked")
    private void buildMappings(Map<String, Object> mappingConfiguration) throws
            JsonNodeException {
        Map<String, MapEntry> sourceObjectToEntryMap = new HashMap<String, MapEntry>();
        Map<String, Collection<MapEntry>> sourceObjectToMapEntries = new HashMap<String, Collection<MapEntry>>();
        JsonNode node = new JsonNode(mappingConfiguration);
        List mappings = node.get("mappings").required().asList();
        Iterator it = mappings.iterator();
        while (it.hasNext()) {
            Map map = (Map) it.next();
            MapEntry entry = new MapEntry(map);
            addSourceMapping(entry);
            sourceObjectToEntryMap.put(entry.getSourceObject(), entry);
        }
    }

    /**
     * For each {@link MapEntry} add it to the collection of mappings for a given
     * {@code sourceObject}.
     *
     * @param entry
     */
    private void addSourceMapping(MapEntry entry) {
        Collection<MapEntry> entryCollection = sourceObjectToMappings.get(entry.getSourceObject());
        if (entryCollection == null) {
            entryCollection = new ArrayList<MapEntry>();
            entryCollection.add(entry);
            sourceObjectToMappings.put(entry.getSourceObject(), entryCollection);
        }
    }

    /**
     * Get all known MapEntries.
     *
     * @return sourceObjectToMapping values
     */
    public Collection<MapEntry> getMapEntries() {
        Collection<MapEntry> allEntries = new ArrayList<MapEntry>();
        for (Collection<MapEntry> entryCollection : sourceObjectToMappings.values()) {
            allEntries.addAll(entryCollection);
        }
        return allEntries;
    }

    /**
     * For the given {@code sourceObject} get all {@link MapEntry}'s.
     *
     * @param sourceObject key
     * @return sourceObjectToMapping
     */
    public Collection<MapEntry> getMapEntriesFor(String sourceObject) {
        return sourceObjectToMappings.get(sourceObject);
    }

    /**
     * Filter out the mappings to return only synchronous mappings.
     *
     * @param sourceObject to return synchronous mappings for
     * @return synchronousEntries
     */
    public Collection<MapEntry> getSynchronousEntriesFor(String sourceObject) {
        Collection<MapEntry> synchronousEntries = new ArrayList<MapEntry>();
        Collection<MapEntry> allEntries = getMapEntriesFor(sourceObject);
        for (MapEntry entry : allEntries) {
            if (entry.getSynchrony() == SYNCHRONOUS) {
                synchronousEntries.add(entry);
            }
        }
        return synchronousEntries;
    }

    /**
     * Filter out the mappings to teturn only asynchronous mappings.
     *
     * @param sourceObject to return asynchronous mappings for
     * @return asynchronousEntries
     */
    public Collection<MapEntry> getAsynchronousEntriesFor(String sourceObject) {
        Collection<MapEntry> asynchronousEntries = new ArrayList<MapEntry>();
        Collection<MapEntry> allEntries = getMapEntriesFor(sourceObject);
        for (MapEntry entry : allEntries) {
            if (entry.getSynchrony() == ASYNCHRONOUS) {
                asynchronousEntries.add(entry);
            }
        }
        return asynchronousEntries;
    }

    /**
     * Debugging toString.
     *
     * @return nested toString values
     */
    @Override
    public String toString() {
        return "MappingConfiguration{" +
                "sourceObjectToMappings=" + sourceObjectToMappings +
                '}';
    }
}
