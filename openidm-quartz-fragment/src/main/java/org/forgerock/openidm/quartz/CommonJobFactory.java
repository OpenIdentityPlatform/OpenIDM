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

package org.forgerock.openidm.quartz;

import org.forgerock.json.resource.PersistenceConfig;
import org.forgerock.script.ScriptRegistry;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.simpl.PropertySettingJobFactory;
import org.quartz.spi.TriggerFiredBundle;


/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public class CommonJobFactory extends PropertySettingJobFactory {


    public static final String PERSISTENCE_CONFIG = "persistenceConfig";
    public static final String SCRIPT_REGISTRY = "scriptRegistry";

    private final PersistenceConfig persistenceConfig;

    private final ScriptRegistry scriptRegistry;


    public CommonJobFactory(PersistenceConfig persistenceConfig, ScriptRegistry scriptRegistry) {
        this.persistenceConfig = persistenceConfig;
        this.scriptRegistry = scriptRegistry;
    }

    @Override
    public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {

        JobDetail jobDetail = bundle.getJobDetail();
        Class<? extends Job> jobClass = jobDetail.getJobClass();
        try {
            if(getLog().isDebugEnabled()) {
                getLog().debug(
                        "Producing instance of Job '" + jobDetail.getKey() +
                                "', class=" + jobClass.getName());
            }

            Job job = jobClass.newInstance();

            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.putAll(scheduler.getContext());
            jobDataMap.putAll(bundle.getJobDetail().getJobDataMap());
            jobDataMap.putAll(bundle.getTrigger().getJobDataMap());
            jobDataMap.put(PERSISTENCE_CONFIG, persistenceConfig);
            jobDataMap.put(SCRIPT_REGISTRY, scriptRegistry);

            setBeanProps(job, jobDataMap);

            return job;
        } catch (Exception e) {
            SchedulerException se = new SchedulerException(
                    "Problem instantiating class '"
                            + jobDetail.getJobClass().getName() + "'", e);
            throw se;
        }
    }
}
