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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openidm.sync.impl.api;

import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;

/**
 * A class for resource linked to the main object.
 */
@Title("Linked Resource")
public class LinkedResource {
    private String resourceName;
    private String linkType;
    private String linkQualifier;
    private List<Map<String, Mapping>> mappings;
    private Map<String, Object> content;

    /**
     * Gets resource name.
     *
     * @return Resource name
     */
    @NotNull
    @Description("Resource name (e.g., managed/user/bjensen)")
    public String getResourceName() {
        return resourceName;
    }

    /**
     * Sets resource name.
     *
     * @param resourceName Resource name
     */
    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    /**
     * Gets the type of the link. Usually the name of the mapping which created the link.
     *
     * @return The type of the link.
     */
    @NotNull
    @Description("The type of the link. Usually the name of the mapping which created the link")
    public String getLinkType() {
        return linkType;
    }

    /**
     * Sets the type of the link. Usually the name of the mapping which created the link.
     *
     * @param linkType The type of the link.
     */
    public void setLinkType(String linkType) {
        this.linkType = linkType;
    }

    /**
     * Gets the link Qualifier (e.g., default).
     *
     * @return The link Qualifier (e.g., default).
     */
    @NotNull
    @Description("Link Qualifier (e.g., default)")
    public String getLinkQualifier() {
        return linkQualifier;
    }

    /**
     * Sets the link qualifier.
     *
     * @param linkQualifier The link qualifier.
     */
    public void setLinkQualifier(String linkQualifier) {
        this.linkQualifier = linkQualifier;
    }

    /**
     * Gets mappings.
     *
     * @return Mapping
     */
    @NotNull
    @Description("Mappings")
    public List<Map<String, Mapping>> getMappings() {
        return mappings;
    }

    /**
     * Sets mappings.
     *
     * @param mappings Mapping
     */
    public void setMappings(List<Map<String, Mapping>> mappings) {
        this.mappings = mappings;
    }

    /**
     * Content.
     *
     * @return Content
     */
    @NotNull
    @Description("Content")
    public Map<String, Object> getContent() {
        return content;
    }

    /**
     * Sets content.
     *
     * @param content Content
     */
    public void setContent(Map<String, Object> content) {
        this.content = content;
    }
}