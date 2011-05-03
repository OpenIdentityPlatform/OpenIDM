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

import org.forgerock.openidm.recon.ReconciliationService;

/**
 * An default implementation for the {@link org.forgerock.openidm.recon.ReconciliationService} interface implemented
 * as on OSGi service.
 */

@Component(name = "reconciliation-engine", immediate = true)
@Service(value = ReconciliationServiceImpl.class)
@Properties({
        @Property(name = "service.description", value = "Default Reconciliation Engine"),
        @Property(name = "service.vendor", value = "ForgeRock AS")
})
public class ReconciliationServiceImpl implements ReconciliationService {

    final static Logger logger = LoggerFactory.getLogger(ReconciliationServiceImpl.class);

    private RelationshipIndexImpl relationshipIndex;


    /**
     * Begin reconciliation specified by the given reconciliation configuration name. If
     * the specified reconciliation in already running, then subsequent calls will be ignored.
     *
     * @param reconciliationConfigurationName
     *         of the configured reconciliation process to start
     * @throws ReconciliationException if there is an error in starting reconciliation
     */
    @Override
    public void startReconciliation(String reconciliationConfigurationName) throws ReconciliationException {

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
     * Life cycle method to activate and load the configuration for Reconciliation.
     *
     * @param configuration json structure
     * @throws ReconciliationException upon an underlying error
     */
    @Activate
    private void activate(Map<String, Object> configuration) throws ReconciliationException {
        logger.debug("{} was activated with: {}", new
                Object[]{ReconciliationServiceImpl.class.getName(), configuration});
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