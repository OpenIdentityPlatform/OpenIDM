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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.script.ScriptException;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openidm.internal.recon.ConfigurationProvider.Mode;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptRegistry;

/**
 * Represents the information and functionality for a reconciliation run
 * 
 * @author aegloff
 */
public final class ExecutorPreferences {

    /**
     * A builder for incremental construction of a {@code ExecutorPreferences}.
     * 
     * @see ExecutorPreferences#builder()
     */
    public static final class Builder {
        private ScriptRegistry scriptRegistry;
        private ConfigurationProvider provider;

        private Builder() {
            // No implementation required.
        }

        /**
         * Returns a new ExecutorPreferences having the properties of this
         * builder.
         * 
         * @return A new ExecutorPreferences having the properties of this
         *         builder.
         * @throws IllegalStateException
         *             If the ScriptRegistry or ConfigurationProvider has not
         *             been specified.
         */
        public ExecutorPreferences build(final String reconId, final ServerContext context)
                throws ScriptException {
            if (reconId == null) {
                throw new IllegalStateException("No ReconId specified");
            }
            if (context == null) {
                throw new IllegalStateException("No ServerContext specified");
            }
            if (scriptRegistry == null) {
                throw new IllegalStateException("No ScriptRegistry specified");
            }
            if (provider == null) {
                throw new IllegalStateException("No ConfigurationProvider specified");
            }

            ExecutorPreferences preferences = new ExecutorPreferences(reconId, context, provider);

            if (null != provider.sourceLinkScript) {
                ScriptEntry script = scriptRegistry.takeScript(provider.sourceLinkScript);
                if (null == script || !script.isActive()) {
                    throw new IllegalStateException("Script is not available");
                } else {
                    preferences.sourceLinkScript = script;
                }
            }

            if (null != provider.correlationScript) {
                ScriptEntry script = scriptRegistry.takeScript(provider.correlationScript);
                if (null == script || !script.isActive()) {
                    throw new IllegalStateException("Script is not available");
                } else {
                    preferences.correlationScript = script;
                }
            }

            if (null != provider.confirmationScript) {
                ScriptEntry script = scriptRegistry.takeScript(provider.confirmationScript);
                if (null == script || !script.isActive()) {
                    throw new IllegalStateException("Script is not available");
                } else {
                    preferences.confirmationScript = script;
                }
            }

            if (null != provider.targetLinkScript) {
                ScriptEntry script = scriptRegistry.takeScript(provider.targetLinkScript);
                if (null == script || !script.isActive()) {
                    throw new IllegalStateException("Script is not available");
                } else {
                    preferences.targetLinkScript = script;
                }
            }

            if (null != provider.getPolicies()) {
                for (Map.Entry<ReconSituation, List<Object>> entry : provider.getPolicies()
                        .entrySet()) {

                    if (null == entry.getValue()) {
                        preferences.policyMap.put(entry.getKey(), null);
                    } else {
                        List<Object> policyList = new ArrayList<Object>(entry.getValue().size());
                        for (Object o : entry.getValue()) {
                            if (o instanceof Request) {
                                policyList.add(o);
                            } else if (o instanceof JsonValue) {
                                ScriptEntry scriptEntry = scriptRegistry.takeScript((JsonValue) o);
                                if (null == scriptEntry) {
                                    throw new JsonValueException((JsonValue) o,
                                            "Failed to find script");
                                } else {
                                    policyList.add(scriptEntry);
                                }
                            }
                        }
                        preferences.policyMap.put(entry.getKey(), policyList);
                    }
                }
            }
            return preferences;
        }

        /**
         * Sets the ScriptRegistry which should be used when loading script from
         * their JSON representation.
         * <p/>
         * The ScriptRegistry mandatory and failure to specify one will result
         * in an error when the ExecutorPreferences is
         * {@link #build(String, ServerContext) built}.
         * 
         * @param registry
         *            The ScriptRegistry which should be used when taking script
         *            from their JSON representation.
         * @return This builder.
         */
        public Builder scriptRegistry(final ScriptRegistry registry) {
            this.scriptRegistry = registry;
            return this;
        }

        /**
         * Sets the ConfigurationProvider which should be used when building the
         * ExecutorPreferences.
         * <p/>
         * The ConfigurationProvider mandatory and failure to specify one will
         * result in an error when the ExecutorPreferences is
         * {@link #build(String, ServerContext) built}.
         * 
         * @param provider
         *            The ConfigurationProvider which should be used when
         *            building the ExecutorPreferences.
         * @return This builder.
         */
        public Builder configurationProvider(final ConfigurationProvider provider) {
            this.provider = provider;
            return this;
        }

    }

    /**
     * Returns a new builder which can be used to incrementally configure and
     * construct a new ExecutorPreferences.
     * 
     * @return A new builder which can be used to incrementally configure and
     *         construct a new ExecutorPreferences.
     */
    public static Builder builder() {
        return new Builder();
    }

    protected ScriptEntry sourceLinkScript = null;
    protected ScriptEntry correlationScript = null;
    protected ScriptEntry confirmationScript = null;
    protected ScriptEntry targetLinkScript = null;
    protected EnumMap<ReconSituation, List<Object>> policyMap =
            new EnumMap<ReconSituation, List<Object>>(ReconSituation.class);

    private ReconStage stage = ReconStage.ACTIVE_INITIALIZED;

    private final AtomicBoolean canceled = new AtomicBoolean(false);
    private final ConfigurationProvider configuration;
    private final String reconId;

    // private final ReconciliationStatistic reconStat;

    /**
     * 
     * @param context
     *            the resource call context
     * @param configuration
     */
    public ExecutorPreferences(final String reconId, final ServerContext context,
            final ConfigurationProvider configuration) {
        this.reconId = reconId != null ? reconId : UUID.randomUUID().toString();
        this.configuration = configuration;
        // this.reconStat = new ReconciliationStatistic(null);
    }

    Context getReconContext() {
        return new RootContext(reconId);
    }

    public ConfigurationProvider getConfiguration() {
        return configuration;
    }

    /**
     * @return A unique identifier for the reconciliation run
     */
    public String getReconId() {
        return reconId;
    }

    /**
     * Cancel the reconciliation run. May not take immediate effect in stopping
     * the reconciliation logic.
     */
    public boolean cancel() {
        stage = ReconStage.ACTIVE_CANCELING;
        return canceled.compareAndSet(false, true);
    }

    /**
     * @return Whether the reconciliation run has been canceled.
     */
    public boolean isCanceled() {
        return canceled.get();
    }

    /**
     * @return Statistics about this reconciliation run
     */
    /*
     * public ReconciliationStatistic getStatistics() { return reconStat; }
     */

    public String getState() {
        return stage.getState();
    }

    public ReconStage getStage() {
        return stage;
    }

    public void handleFatalEventException(Throwable ex, long sequence, Object event) {
        cancel();
    }

    /**
     * Indicates that the links are automatically generated because the
     * {@code source} and {@code target} collections are sharing the same ID.
     * 
     * This is {@literal true} only if the relation is {@link Mode#ONE_TO_ONE},
     * links are not persisted and there is no script defined to generate the
     * links.
     * 
     * @param forSource
     *            {@literal true} gets it for the {@code source} collection and
     *            {@literal false} gets it for the {@code target} collection.
     * 
     * @return
     */
    public boolean isLinkGeneratedById(boolean forSource) {
        return Mode.ONE_TO_ONE.equals(configuration.getRelation())
                && !configuration.hasPersistedLink()
                && (forSource ? configuration.sourceLinkScript == null
                        : configuration.targetLinkScript == null);
    }

    /**
     * @return the populated run progress structure
     */
    // public Map<String, Object> getProgress() {
    // // Unknown total entries are currently represented via question mark
    // // string.
    // String totalSourceEntriesStr =
    // (totalSourceEntries == null ? "?" :
    // Integer.toString(totalSourceEntries));
    // String totalTargetEntriesStr =
    // (totalTargetEntries == null ? "?" :
    // Integer.toString(totalTargetEntries));
    //
    // String totalLinkEntriesStr = "?";
    // if (totalLinkEntries == null) {
    // if (getStage() == ReconStage.COMPLETED_SUCCESS) {
    // totalLinkEntriesStr =
    // Integer.toString(getStatistics().getLinkProcessed());
    // }
    // } else {
    // totalLinkEntriesStr = Integer.toString(totalLinkEntries);
    // }
    //
    // Map<String, Object> progress = new LinkedHashMap<String, Object>();
    // Map<String, Object> progressDetail = new LinkedHashMap<String, Object>();
    // Map<String, Object> sourceDetail = new LinkedHashMap<String, Object>();
    // Map<String, Object> sourceExisting = new LinkedHashMap<String, Object>();
    // Map<String, Object> targetDetail = new LinkedHashMap<String, Object>();
    // Map<String, Object> targetExisting = new LinkedHashMap<String, Object>();
    // Map<String, Object> linkDetail = new LinkedHashMap<String, Object>();
    // Map<String, Object> linkExisting = new LinkedHashMap<String, Object>();
    //
    // sourceExisting.put("processed", getStatistics().getSourceProcessed());
    // sourceExisting.put("total", totalSourceEntriesStr);
    // sourceDetail.put("existing", sourceExisting);
    // progressDetail.put("source", sourceDetail);
    //
    // targetExisting.put("processed", getStatistics().getTargetProcessed());
    // targetExisting.put("total", totalTargetEntriesStr);
    // targetDetail.put("existing", targetExisting);
    // targetDetail.put("created", getStatistics().getTargetCreated());
    // progressDetail.put("target", targetDetail);
    //
    // linkExisting.put("processed", getStatistics().getLinkProcessed());
    // linkExisting.put("total", totalLinkEntriesStr);
    // linkDetail.put("existing", linkExisting);
    // linkDetail.put("created", getStatistics().getLinkCreated());
    // progressDetail.put("links", linkDetail);
    //
    // progress.put("progress", progressDetail);
    //
    // return progress;
    // }

    /**
     * @param newStage
     *            Sets the current state and stage in the reconciliation process
     */
    /*
     * public void setStage(ReconStage newStage) { // If there is already a
     * stage in progress, end it first if (this.stage !=
     * ReconStage.ACTIVE_INITIALIZED) { reconStat.endStage(this.stage); } if
     * (canceled.get()) { if (newStage.isComplete()) { this.stage =
     * ReconStage.COMPLETED_CANCELED; } else { this.stage =
     * ReconStage.ACTIVE_CANCELING; } } else { this.stage = newStage; } if
     * (newStage.isComplete()) { cleanupState(); } else {
     * reconStat.startStage(newStage); } }
     */

}
