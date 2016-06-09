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
 * Portions copyright 2012-2015 ForgeRock AS.
 */

package org.forgerock.openidm.scheduler;

import java.text.ParseException;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.config.enhanced.InvalidException;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Schedule Config Service
 *
 */

@Component(name = "org.forgerock.openidm.schedule", immediate=true, policy=ConfigurationPolicy.REQUIRE, configurationFactory=true)
public class ScheduleConfigService {

    final static Logger logger = LoggerFactory.getLogger(ScheduleConfigService.class);

    // Optional user defined name for this instance, derived from the file install name
    String configFactoryPID;

    // Scheduling
    private Boolean schedulePersisted = false;
    private String jobName = null;
    private ScheduleConfig scheduleConfig;

    /** Enhanced configuration service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile EnhancedConfig enhancedConfig;

    @Reference
    SchedulerService schedulerService;
    protected void bindSchedulerService(final SchedulerService service) {
        schedulerService = service;
    }

    protected void unbindSchedulerService(final SchedulerService service) {
        schedulerService = null;
    }

    @Activate
    void activate(ComponentContext compContext) throws SchedulerException, ParseException, ResourceException {
        logger.debug("Activating Service with configuration {}", compContext.getProperties());

        scheduleConfig = initConfig(compContext);
        if (scheduleConfig == null) {
            logger.debug("No preconfigured schedule");
            return;
        }

        if (configFactoryPID != null) {
            jobName = configFactoryPID;
        } else {
            jobName = (String) compContext.getProperties().get(Constants.SERVICE_PID);
        }

        schedulerService.registerConfigService(this);

    }

    @Deactivate
    void deactivate(ComponentContext compContext) {
        logger.debug("Deactivating Service {}", compContext);
        schedulerService.unregisterConfigService(this, isFrameworkStopping(compContext));
    }

    /*
    A bit of a hack, but bundle 0 is the Framework bundle, whose state will be STOPPING if the osgi framework is
    shutting-down.
     */
    private boolean isFrameworkStopping(ComponentContext componentContext) {
        return componentContext.getBundleContext().getBundle(0).getState() == Bundle.STOPPING;
    }

    /**
     * Initialize the service configuration
     * @param compContext
     * @throws InvalidException if the configuration is invalid.
     */
    private ScheduleConfig initConfig(ComponentContext compContext) throws ResourceException {

        // Optional property SERVICE_FACTORY_PID set by JSONConfigInstaller
        configFactoryPID = (String) compContext.getProperties().get("config.factory-pid");
        Map<String, Object> config = enhancedConfig.getConfiguration(compContext);
        logger.debug("Scheduler service activating with configuration {}", config);
        if (config == null) {
            return null;
        }
        return new ScheduleConfig(new JsonValue(config));
    }

    public Boolean getSchedulePersisted() {
        return schedulePersisted;
    }

    public void setSchedulePersisted(Boolean localSchedulePersisted) {
        this.schedulePersisted = localSchedulePersisted;
    }

    public String getJobName() {
        return jobName;
    }

    public ScheduleConfig getScheduleConfig() {
        return scheduleConfig;
    }
}
