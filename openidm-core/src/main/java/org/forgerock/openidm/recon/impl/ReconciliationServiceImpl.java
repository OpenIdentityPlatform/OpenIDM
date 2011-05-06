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

package org.forgerock.openidm.recon.impl;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;

import org.forgerock.json.fluent.JsonNodeException;

import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.config.InvalidException;
import org.forgerock.openidm.scheduler.ScheduledService;
import org.forgerock.openidm.scheduler.ExecutionException;
import org.forgerock.openidm.recon.ReconciliationService;

import org.osgi.service.component.ComponentContext;

/**
 * An default implementation for the {@link org.forgerock.openidm.recon.ReconciliationService} interface implemented
 * as on OSGi service.
 */

@Component(name = "org.forgerock.openidm.reconciliation", immediate = false)
@Service
@Properties({
        @Property(name = "service.description", value = "Default Reconciliation Engine"),
        @Property(name = "service.vendor", value = "ForgeRock AS")
})
public class ReconciliationServiceImpl implements ReconciliationService, ScheduledService {

    final static Logger logger = LoggerFactory.getLogger(ReconciliationServiceImpl.class);

    private ReconciliationConfiguration reconciliationConfiguration;

    public ReconciliationServiceImpl() {
    }


    /**
     * Begin reconciliation specified by the given reconciliation configuration name. If
     * the specified reconciliation in already running, then subsequent calls will be ignored. TODO ensure this
     * <p/>
     * If the specified reconciliation configuration name is not found a {@link ReconciliationException} will
     * be thrown.
     * <p/>
     * If the specified reconciliation configuration is disabled then a {@link ReconciliationException} will
     * be thrown.
     *
     * @param reconciliationConfigurationName
     *         of the configured reconciliation process to start
     * @throws ReconciliationException if there is an error in starting reconciliation
     */
    @Override
    public void startReconciliation(String reconciliationConfigurationName) throws ReconciliationException {
        ReconciliationConfigurationEntry reconEntry = null;
        if (reconciliationConfiguration != null) {
            reconciliationConfiguration.getReconciliationConfigurationEntry(reconciliationConfigurationName);
        }
        if (reconEntry == null) {
            logger.warn("Named reconciliation configuration was not found: {}", reconciliationConfigurationName);
            throw new ReconciliationException("Named recnociliation configuraiton was not found: " + reconciliationConfigurationName);
        }
        if (!reconEntry.isEnabled()) {
            logger.warn("Reconciliation was called for a disabled configuration: {} ", reconEntry.getName());
            throw new ReconciliationException("ReconciliationConfigurationEntry is disabled for " + reconEntry.getName());
        }
        ReconciliationPolicyEntry policyEntry =
                reconciliationConfiguration.getReconciliationPolicyEntry(reconEntry.getPolicyName());
        ReconciliationEngine engine = new ReconciliationEngine(reconEntry);
        engine.reconcile();
    }

    /**
     * Get the most recent reconciliation status for the specified configuration. If the reconciliation
     * process has finished this will be the last run status, however if the given configuration is currently
     * executing it will return the latest status update.
     * <p/>
     * Status is in the form of a simple mapped json object.
     *
     * @param reconciliationConfigurationName
     *         of the reconciliation process to get status for
     * @return statusObject that is a simple mapped json object
     * @throws ReconciliationException if there was an error in getting the status
     */
    @Override
    public Map<String, Object> reconciliationStatus(String reconciliationConfigurationName) throws ReconciliationException {
        return null;
    }

    /**
     * Manually cancel a running reconciliation. If the reconciliation configuration is not
     * running than this request is ignored.
     *
     * @param reconciliationConfigurationName
     *         of the running reconciliation process to stop
     * @throws ReconciliationException if there was an exception raised in trying to stop reconciliation
     */
    @Override
    public void cancelReconciliation(String reconciliationConfigurationName) throws ReconciliationException {

    }
    
    /**
     * Invoked by the scheduler service when the configured schedule triggers.
     * 
     * @param context Context information passed by the scheduler service
     * @throws ExecutionException if execution of the scheduled work failed. 
     *         Implementations can also throw RuntimeExceptions which will get logged.
     */
    public void execute(Map<String, Object> context) throws ExecutionException {
        String reconConfigName = (String) context.get(CONFIGURED_INVOKE_CONTEXT);
        if (reconConfigName == null || reconConfigName.trim().length() == 0) {
            throw new ExecutionException("The scheduled reconciliation configuration " + context
                    + " needs to contain the reconciliation configuration name in the invoke context property,"
                    + " but invoke context is empty.");
        }
        logger.info("Scheduled reconciliation {} starting ", reconConfigName);
        try {
            startReconciliation(reconConfigName);
        } catch (Exception ex) {
            logger.warn("Reconciliation for " + reconConfigName + " and scheduled for " 
                    + context.get(SCHEDULED_FIRE_TIME) + " failed. ", ex);
        }
    }

    /**
     * Life cycle method to activate and load the configuration for Reconciliation.
     *
     * @param configuration json structure
     * @throws ReconciliationException upon an underlying error
     */
    @Activate
    private void activate(ComponentContext compContext) throws ReconciliationException {
        logger.debug("Activated with: {}", compContext.getProperties());
        try {
            EnhancedConfig enhancedConfig = new JSONEnhancedConfig();
            Map<String, Object> configuration = enhancedConfig.getConfiguration(compContext);
            if (configuration != null && configuration.size() > 0) {
                reconciliationConfiguration = new ReconciliationConfiguration(configuration);
            }
        } catch (JsonNodeException e) {
            logger.error("Failed to load reconciliation configuration");
            throw new ReconciliationException(e);
        }
    }

    /**
     * Life cycle methods to deactivate Reconciliation
     *
     * @param configuration
     * @throws ReconciliationException upon an underlying error
     */
    @Deactivate
    private void deactivate(Map<String, Object> configuration) throws ReconciliationException {
        logger.debug("{} was deactivated with: {}", new
                Object[]{ReconciliationServiceImpl.class.getName(), configuration});
    }

}