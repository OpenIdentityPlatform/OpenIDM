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

import javax.validation.constraints.NotNull;

import java.util.Map;

import org.forgerock.api.annotations.Description;

/**
 * Reconciliation-progress summary for various recon-phases.
 */
public class ReconProgress {

    private Map<String, Object> source;

    private Map<String, Object> target;

    private Map<String, Object> links;

    /**
     * Gets stats for source objects.
     *
     * @return Stats for source objects
     */
    @NotNull
    @Description("Stats for source objects")
    public Map<String, Object> getSource() {
        return source;
    }

    /**
     * Sets stats for source objects.
     *
     * @param source Stats for source objects
     */
    public void setSource(Map<String, Object> source) {
        this.source = source;
    }

    /**
     * Gets stats for target objects.
     *
     * @return Stats for target objects
     */
    @NotNull
    @Description("Stats for target objects")
    public Map<String, Object> getTarget() {
        return target;
    }

    /**
     * Sets stats for target objects.
     *
     * @param target Stats for target objects
     */
    public void setTarget(Map<String, Object> target) {
        this.target = target;
    }

    /**
     * Gets stats for links.
     *
     * @return Stats for links
     */
    @NotNull
    @Description("Stats for links")
    public Map<String, Object> getLinks() {
        return links;
    }

    /**
     * Sets stats for links.
     *
     * @param links Stats for links
     */
    public void setLinks(Map<String, Object> links) {
        this.links = links;
    }

}
