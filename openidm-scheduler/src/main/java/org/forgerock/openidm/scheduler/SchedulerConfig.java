/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2012 ForgeRock AS. All Rights Reserved
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
*
*/

package org.forgerock.openidm.scheduler;

import java.util.Properties;
import java.util.Set;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.quartz.impl.RepoJobStore;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds a Scheduler's configuration
 * 
 * @author ckienle
 */
public class SchedulerConfig {
    final static Logger logger = LoggerFactory.getLogger(SchedulerConfig.class);
    
    // Top level objects
    private final static String ADVANCED_CONFIG = "advancedProperties";
    private final static String THREADPOOL = "threadPool";
    private final static String SCHEDULER = "scheduler";
    
    // Threadpool objects
    private final static String THREAD_COUNT = "threadCount";
    
    // Scheduler objects
    private final static String INSTANCE_ID = "instanceId";
    private final static String INSTANCE_TIMEOUT = "instanceTimeout";
    private final static String INSTANCE_RECOVERY_TIMEOUT = "instanceRecoveryTimeout";
    private final static String INSTANCE_CHECK_IN_INTERVAL = "instanceCheckInInterval";
    private final static String INSTANCE_CHECK_IN_OFFSET = "instanceCheckInOffset";
    private final static String EXECUTE_PERSISTENT_SCHEDULES = "executePersistentSchedules";
    
    private String threadCount = null;
    private String instanceId = null;
    private boolean executePersistentSchedules = true;
    
    private Properties props;
    
    public SchedulerConfig(JsonValue config) {
        if (config != null && !config.isNull()) {
            setThreadpoolConfig(config.get(THREADPOOL));
            setSchedulerConfig(config.get(SCHEDULER));
            props = getAdvancedProps(config);
        } else {
            logger.debug("Config is null, defaults will be used");
            props = new Properties();
        }
    }

    private void setThreadpoolConfig(JsonValue config) {
        if (!config.isNull()) {
            JsonValue value = config.get(THREAD_COUNT);
            if (!value.isNull()) {
                threadCount = value.asString();
            }
        }
    }

    private void setSchedulerConfig(JsonValue config) {
        if (!config.isNull()) {
            JsonValue value = config.get(INSTANCE_ID);
            if (!value.isNull()) {
                instanceId = value.asString();
            }
            value = config.get(EXECUTE_PERSISTENT_SCHEDULES);
            if (!value.isNull()) {
                if (value.isString()) {
                    executePersistentSchedules = Boolean.parseBoolean(value.asString());
                } else {
                    executePersistentSchedules = value.asBoolean();
                }
            }
            value = config.get(INSTANCE_TIMEOUT);
            if (!value.isNull()) {
                RepoJobStore.setInstanceTimeout(Long.parseLong(value.asString()));
            }
            value = config.get(INSTANCE_RECOVERY_TIMEOUT);
            if (!value.isNull()) {
                RepoJobStore.setInstanceRecoveryTimeout(Long.parseLong(value.asString()));
            }
            value = config.get(INSTANCE_CHECK_IN_INTERVAL);
            if (!value.isNull()) {
                RepoJobStore.setInstanceCheckInInterval(Long.parseLong(value.asString()));
            }
            value = config.get(INSTANCE_CHECK_IN_OFFSET);
            if (!value.isNull()) {
                RepoJobStore.setInstanceCheckInOffset(Long.parseLong(value.asString()));
            }
        }
    }

    private Properties getAdvancedProps(JsonValue config) {
        Properties props = new Properties();
        JsonValue advancedConfig = config.get(ADVANCED_CONFIG);
        if (!advancedConfig.isNull()) {
            Set<String> keys = advancedConfig.keys();
            for (String key : keys) {
                props.put(key, advancedConfig.get(key).asString());
            }
        }
        return props;
    }

    public boolean executePersistentSchedulesEnabled() {
        return executePersistentSchedules;
    }
    
    public String getInstanceName() {
        return props.getProperty(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, "DefaultQuartzScheduler");
    }
    
    public void setInstanceName(String instanceName) {
        props.setProperty(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, instanceName);
    }
    
    public String getInstanceId() {
        if (instanceId != null) {
            return instanceId;
        }
        return props.getProperty(StdSchedulerFactory.PROP_SCHED_INSTANCE_ID, "AUTO");
    }
    
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
    
    public String getRmiExport() {
        return props.getProperty(StdSchedulerFactory.PROP_SCHED_RMI_EXPORT, "false");
    }
    
    public void setRmiExport(String rmiExport) {
        props.setProperty(StdSchedulerFactory.PROP_SCHED_RMI_EXPORT, rmiExport);
    }
    
    public String getRmiProxy() {
        return props.getProperty(StdSchedulerFactory.PROP_SCHED_RMI_PROXY, "false");
    }
    
    public void setRmiProxy(String rmiProxy) {
        props.setProperty(StdSchedulerFactory.PROP_SCHED_RMI_PROXY, rmiProxy);
    }
    
    public String getWrapJobExecutionInUserTransaction() {
        return props.getProperty(StdSchedulerFactory.PROP_SCHED_WRAP_JOB_IN_USER_TX, "false");
    }
    
    public void setWrapJobExecutionInUserTransaction(
            String wrapJobExecutionInUserTransaction) {
        props.setProperty(StdSchedulerFactory.PROP_SCHED_WRAP_JOB_IN_USER_TX, wrapJobExecutionInUserTransaction);
    }
    
    public String getThreadPoolClass() {
        return props.getProperty(StdSchedulerFactory.PROP_THREAD_POOL_CLASS, "org.quartz.simpl.SimpleThreadPool");
    }
    
    public void setThreadPoolClass(String threadPoolClass) {
        StringBuilder prop = new StringBuilder(StdSchedulerFactory.PROP_THREAD_POOL_PREFIX).append(".threadCount");
        props.setProperty(prop.toString(), threadPoolClass);
    }
    
    public String getThreadPoolThreadCount() {
        if (threadCount != null) {
            return threadCount;
        }
        StringBuilder prop = new StringBuilder(StdSchedulerFactory.PROP_THREAD_POOL_PREFIX).append(".threadCount");
        return props.getProperty(prop.toString(), "10");
    }
    
    public void setThreadPoolThreadCount(String threadPoolThreadCount) {
        threadCount = threadPoolThreadCount;
    }
    
    public String getThreadPoolThreadPriority() {
        StringBuilder prop = new StringBuilder(StdSchedulerFactory.PROP_THREAD_POOL_PREFIX).append(".threadPriority");
        return props.getProperty(prop.toString(), "5");
    }
    
    public void setThreadPoolThreadPriority(String threadPoolThreadPriority) {
        StringBuilder prop = new StringBuilder(StdSchedulerFactory.PROP_THREAD_POOL_PREFIX).append(".threadPriority");
        props.setProperty(prop.toString(), threadPoolThreadPriority);
    }
    
    public String getThreadPoolThreadsInheritContextClassLoaderOfInitializingThread() {
        return props.getProperty(
                StdSchedulerFactory.PROP_SCHED_SCHEDULER_THREADS_INHERIT_CONTEXT_CLASS_LOADER_OF_INITIALIZING_THREAD, 
                "true");
    }
    
    public void setThreadPoolThreadsInheritCtxtCLOfInitThread(
            String threadPoolThreadsInheritCtxtCLOfInitThread) {
        props.setProperty(
                StdSchedulerFactory.PROP_SCHED_SCHEDULER_THREADS_INHERIT_CONTEXT_CLASS_LOADER_OF_INITIALIZING_THREAD, 
                threadPoolThreadsInheritCtxtCLOfInitThread);
    }
    
    public String getJobStoreClass() {
        return props.getProperty(StdSchedulerFactory.PROP_JOB_STORE_CLASS, "org.forgerock.openidm.quartz.impl.RepoJobStore");
    }
    
    public void setJobStoreClass(String jobStoreClass) {
        props.setProperty(StdSchedulerFactory.PROP_JOB_STORE_CLASS, jobStoreClass);
    }

    public Properties toProps() {
        StringBuilder propThreadPoolPriority = new StringBuilder(StdSchedulerFactory.PROP_THREAD_POOL_PREFIX).append(".threadPriority");
        StringBuilder propThreadPoolCount = new StringBuilder(StdSchedulerFactory.PROP_THREAD_POOL_PREFIX).append(".threadCount");
        Properties props = new Properties(); 
        props.put(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, getInstanceName());
        props.put(StdSchedulerFactory.PROP_SCHED_INSTANCE_ID, getInstanceId());
        props.put(StdSchedulerFactory.PROP_SCHED_RMI_EXPORT, getRmiExport());
        props.put(StdSchedulerFactory.PROP_SCHED_RMI_PROXY, getRmiProxy());
        props.put(StdSchedulerFactory.PROP_SCHED_WRAP_JOB_IN_USER_TX, getWrapJobExecutionInUserTransaction());
        props.put(StdSchedulerFactory.PROP_THREAD_POOL_CLASS, getThreadPoolClass());
        props.put(propThreadPoolCount.toString(), getThreadPoolThreadCount());
        props.put(propThreadPoolPriority.toString(), getThreadPoolThreadPriority());
        props.put(StdSchedulerFactory.PROP_SCHED_SCHEDULER_THREADS_INHERIT_CONTEXT_CLASS_LOADER_OF_INITIALIZING_THREAD, 
                getThreadPoolThreadsInheritContextClassLoaderOfInitializingThread());
        props.put(StdSchedulerFactory.PROP_JOB_STORE_CLASS, getJobStoreClass());
        return props;
    }
}
