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

package org.forgerock.openidm.provisioner.impl.api;

import org.forgerock.api.annotations.Default;
import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;

/**
 * Configuration for how OpenICF returns results.
 */
@Title("Results Handler Config")
public class ResultsHandlerConfig {

    private boolean enableNormalizingResultsHandler;
    private boolean enableFilteredResultsHandler;
    private boolean enableCaseInsensitiveFilter;
    private boolean enableAttributesToGetSearchResultsHandler;

    /**
     * Gets attribute normalizer flag.
     *
     * @return Enables attribute normalizer, if supported, when true
     */
    @Description("Enables attribute normalizer, if supported, when true")
    public boolean isEnableNormalizingResultsHandler() {
        return enableNormalizingResultsHandler;
    }

    /**
     * Sets attribute normalizer flag.
     *
     * @param enableNormalizingResultsHandler Enables attribute normalizer, if supported, when true
     */
    public void setEnableNormalizingResultsHandler(boolean enableNormalizingResultsHandler) {
        this.enableNormalizingResultsHandler = enableNormalizingResultsHandler;
    }

    /**
     * Gets filtered results flag.
     *
     * @return When a remote connected system should handle all filtering/search, set to false
     */
    @Description("When a remote connected system should handle all filtering/search, set to false")
    @Default("true")
    public boolean isEnableFilteredResultsHandler() {
        return enableFilteredResultsHandler;
    }

    /**
     * Sets filtered results flag.
     *
     * @param enableFilteredResultsHandler When a remote connected system should handle all filtering/search, set to
     * false
     */
    public void setEnableFilteredResultsHandler(boolean enableFilteredResultsHandler) {
        this.enableFilteredResultsHandler = enableFilteredResultsHandler;
    }

    /**
     * Gets case-insensitive flag.
     *
     * @return Disable case-sensitive filtering/search, when true and 'enableFilteredResultsHandler' enabled
     */
    @Description("Disable case-sensitive filtering/search, when true and 'enableFilteredResultsHandler' enabled")
    public boolean isEnableCaseInsensitiveFilter() {
        return enableCaseInsensitiveFilter;
    }

    /**
     * Sets case-insensitive flag.
     *
     * @param enableCaseInsensitiveFilter Disable case-sensitive filtering/search, when true and
     * 'enableFilteredResultsHandler' enabled
     */
    public void setEnableCaseInsensitiveFilter(boolean enableCaseInsensitiveFilter) {
        this.enableCaseInsensitiveFilter = enableCaseInsensitiveFilter;
    }

    /**
     * Gets remove-attributes flag.
     *
     * @return Removes all unrequested attributes from READ/QUERY response when true
     */
    @Description("Removes all unrequested attributes from READ/QUERY response when true. Recommend false for local "
            + "connectors and true for remote connectors")
    public boolean isEnableAttributesToGetSearchResultsHandler() {
        return enableAttributesToGetSearchResultsHandler;
    }

    /**
     * Sets remove-attributes flag.
     *
     * @param enableAttributesToGetSearchResultsHandler Removes all unrequested attributes from READ/QUERY response
     * when true
     */
    public void setEnableAttributesToGetSearchResultsHandler(boolean enableAttributesToGetSearchResultsHandler) {
        this.enableAttributesToGetSearchResultsHandler = enableAttributesToGetSearchResultsHandler;
    }

}
