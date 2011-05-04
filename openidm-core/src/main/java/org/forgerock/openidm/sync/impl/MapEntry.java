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

import java.util.*;

import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;

/**
 * A bean like wrapper for {@code mappings} json configuration.
 * </p>
 * The following default values are applied if values are missing from configuration.
 * </p>
 * {@code synchrony = "synchronous"}
 * </p>
 * <p/>
 * If the {@code qualifier} is missing from configuration then the mapping is not qualified.
 * </br>
 * If the {@code namedQuery} is missing from configuration then all attributes for {@code sourceObject} and
 * {@code targetObject} will be pulled back without applying additional query parameters.
 */
public class MapEntry {

    private String name;
    private String sourceObject;
    private String targetObject;
    private String synchrony;
    private String qualifier;
    private String namedQuery;
    private List<PropertyEntry> propertyEntries;

    /**
     * Construct a mapping configuration from the given json configuration data.
     *
     * @param mapEntryConfig
     * @throws JsonNodeException if there is a parsing error or a required property is missing
     */
    public MapEntry(Map<String, Object> mapEntryConfig) throws JsonNodeException {
        mapProperties(mapEntryConfig);
    }

    private void mapProperties(Map<String, Object> mapEntryConfig) throws JsonNodeException {
        JsonNode node = new JsonNode(mapEntryConfig);
        setName(node.get("name").required().asString());
        setSourceObject(node.get("sourceObject").required().asString());
        setTargetObject(node.get("targetObject").required().asString());
        setSynchrony(node.get("synchrony").defaultTo("synchrnous").asString());
        setQualifier(node.get("qualifier").asString());
        setNamedQuery(node.get("namedQuery").asString());
        propertyEntries = buildEntriesList(node.get("propertyMappings").required().asList());
    }

    /**
     * Given a list of property mapping json configuration data, create individual
     * {@link PropertyEntry}'s.
     *
     * @param propertyMappings
     * @return propertyEntries
     * @throws JsonNodeException if there is a parsing error or a required property is missing
     */
    private List<PropertyEntry> buildEntriesList(List propertyMappings) throws JsonNodeException {
        propertyEntries = new ArrayList<PropertyEntry>();
        Iterator it = propertyMappings.iterator();
        while (it.hasNext()) {
            Map propertyEntry = (Map) it.next();
            JsonNode node = new JsonNode(propertyEntry);
            PropertyEntry entry = new PropertyEntry();
            //entry.setSourcePath(node.get("sourceObject").required().asString());
            entry.setSourcePath(node.get("sourcePath").required().asString());
            //entry.setTargetPath(node.get("targetObject").required().asString());
            entry.setTargetPath(node.get("targetPath").required().asString());
            entry.setScript(node.get("script").asMap());
            propertyEntries.add(entry);
        }
        return propertyEntries;
    }

    /**
     * Get the name for this map.
     *
     * @return name of the map
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name for this map
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the sourceObject identifier for this mapping.
     *
     * @return sourceObject
     */
    public String getSourceObject() {
        return sourceObject;
    }

    /**
     * Set the sourceObject identifier for this mapping.
     *
     * @param sourceObject
     */
    public void setSourceObject(String sourceObject) {
        this.sourceObject = sourceObject;
    }

    /**
     * Get the targetObject identifier for this mapping.
     *
     * @return targetObject
     */
    public String getTargetObject() {
        return targetObject;
    }

    /**
     * Set the targetObject for this mapping.
     *
     * @param targetObject
     */
    public void setTargetObject(String targetObject) {
        this.targetObject = targetObject;
    }

    /**
     * Get the synchrony of this mapping.
     *
     * @return synchrony either synchronous or asynchronous
     */
    public String getSynchrony() {
        return synchrony;
    }

    /**
     * Set the synchrony of this mapping, either "synchronous" or "asynchronous"
     *
     */
    public void setSynchrony(String synchrony) {
        this.synchrony = synchrony;
    }

    /**
     * Get the qualifier that will be applied for this mapping. The Qualifier must
     * pass for the mapping to be applied.
     *
     * @return qualifier
     */
    public String getQualifier() {
        return qualifier;
    }

    /**
     * Set the qualifier for this mapping, it must be a valid script.
     *
     * @param qualifier
     */
    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    /**
     * Get the namedQuery for this mapping, if it exists will be applied to sourceObject
     * and targetObject systems when listing or querying for objects.
     *
     * @return namedQuery
     */
    public String getNamedQuery() {
        return namedQuery;
    }

    /**
     * Set the namedQuery for this mapping.
     *
     * @param namedQuery
     */
    public void setNamedQuery(String namedQuery) {
        this.namedQuery = namedQuery;
    }

    /**
     * Get all the {@link PropertyEntry}'s that are defined for this mapping.
     *
     * @return propertyEntries
     */
    public Collection<PropertyEntry> getPropertyEntries() {
        return propertyEntries;
    }

    public void setPropertyEntries(List<PropertyEntry> propertyEntries) {
        this.propertyEntries = propertyEntries;
    }

    /**
     * Debugging toString.
     *
     * @return nested toString values
     */
    @Override
    public String toString() {
        return "MapEntry{" +
                "name='" + name + '\'' +
                ", sourceObject='" + sourceObject + '\'' +
                ", targetObject='" + targetObject + '\'' +
                ", synchrony='" + synchrony + '\'' +
                ", qualifier='" + qualifier + '\'' +
                ", namedQuery='" + namedQuery + '\'' +
                ", propertyEntries=" + propertyEntries +
                '}';
    }
}