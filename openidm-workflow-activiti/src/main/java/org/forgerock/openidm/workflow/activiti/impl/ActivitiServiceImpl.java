/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.workflow.activiti.impl;

import java.io.File;
import java.net.URLDecoder;
import java.util.*;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.impl.cfg.JtaProcessEngineConfiguration;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.apache.felix.scr.annotations.*;
import org.apache.felix.scr.annotations.Properties;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.objset.ObjectSet;
import org.h2.jdbcx.JdbcDataSource;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.objset.ForbiddenException;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.Patch;

import javax.transaction.TransactionManager;

/**
 * Workflow service implementation
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
@Component(name = ActivitiServiceImpl.PID, immediate = true, policy = ConfigurationPolicy.OPTIONAL)
@Service
@Reference(
        name = "JavaDelegateServiceReference",
        referenceInterface = JavaDelegate.class,
        bind = "bindService",
        unbind = "unbindService",
        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
        policy = ReferencePolicy.DYNAMIC
)
@Properties({
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "Activiti Service"),
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = ServerConstants.ROUTER_PREFIX, value = ActivitiServiceImpl.ROUTER_PREFIX)
})
public class ActivitiServiceImpl implements ObjectSet {
    final static Logger logger = LoggerFactory.getLogger(ActivitiServiceImpl.class);
    public final static String PID = "org.forgerock.openidm.workflow.activiti";
    public final static String ROUTER_PREFIX = "workflow/activiti";

    //Just to make fancy the sample. Let's allow to others to register the service and we just use it
    private boolean selfMadeProcessEngine = true;

    private final OpenIDMELResolver openIDMELResolver = new OpenIDMELResolver();

    @Reference(referenceInterface = ObjectSet.class,
            cardinality = ReferenceCardinality.MANDATORY_UNARY,
            policy = ReferencePolicy.STATIC,
            target = "(service.pid=org.forgerock.openidm.router)")
    private ObjectSet router;


    @Reference(
            name = "processEngine",
            referenceInterface = ProcessEngine.class,
            bind = "bind",
            unbind = "unbind",
            cardinality = ReferenceCardinality.OPTIONAL_UNARY,
            policy = ReferencePolicy.STATIC,
            target = "(!openidm.activiti.engine=true)"  //avoid register the self made service
    )
    private ProcessEngine processEngine = null;


    //This method called before activate if there is a ProcessEngine service in the Service Registry
    protected void bind(ProcessEngine processEngine) {
        logger.info("Some other process already created the ProcessEngine so we don't need to make our own");
        if (null == processEngine) {
            this.processEngine = processEngine;
            selfMadeProcessEngine = false;
        }
    }

    protected void unbind(ProcessEngine processEngine) {
        if (!selfMadeProcessEngine) {
            this.processEngine = null;
        }
        logger.info("Ops! The ProcessEngine was stopped. Shell I create my own one or just say. We did run out of the ProcessEngine :)");
    }

    public void bindService(JavaDelegate delegate, Map props) {
        openIDMELResolver.bindService(delegate, props);
    }

    public void unbindService(JavaDelegate delegate, Map props) {
        openIDMELResolver.unbindService(delegate, props);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> read(String fullId) throws ObjectSetException {
        Map<String, Object> result = new HashMap<String, Object>();
        List<ProcessDefinition> definitionList = processEngine.getRepositoryService().createProcessDefinitionQuery().list();
        if (definitionList != null && definitionList.size() > 0) {
            for (ProcessDefinition processDefinition : definitionList) {
                Map<String, Object> processMap = new HashMap<String, Object>();
                processMap.put("key", processDefinition.getKey());
                processMap.put("id", processDefinition.getId());
                result.put(processDefinition.getName(), processMap);
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void create(String fullId, Map<String, Object> obj) throws ObjectSetException {
        throw new ForbiddenException("Not allowed on external activiti service");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(String fullId, String rev, Map<String, Object> obj) throws ObjectSetException {
        throw new ForbiddenException("Not allowed on external activiti service");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(String fullId, String rev) throws ObjectSetException {
        throw new ForbiddenException("Not allowed on external activiti service");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void patch(String id, String rev, Patch patch) throws ObjectSetException {
        throw new ForbiddenException("Not allowed on external activiti service");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> query(String fullId, Map<String, Object> params) throws ObjectSetException {
        // TODO
        return new HashMap<String, Object>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> action(String fullId, Map<String, Object> params) throws ObjectSetException {
        Map<String, Object> result = null;
        JsonValue paramsValue = new JsonValue(params);
        String action = paramsValue.get("_action").required().asString();
        JsonValue workflowParams = paramsValue.get("_workflowParams");

        //POST openidm/workflow/activiti?_action=TestWorkFlow will trigger the process
        ProcessInstance instance = null;
        if (workflowParams.isNull()) {
            instance = processEngine.getRuntimeService().startProcessInstanceByKey(action);
        } else {
            instance = processEngine.getRuntimeService().startProcessInstanceByKey(action, workflowParams.asMap());
        }
        if (null != instance) {
            result = new HashMap<String, Object>();
            result.put("status", instance.isEnded() ? "ended" : "suspended");
            result.put("processInstanceId", instance.getProcessInstanceId());
            result.put("businessKey", instance.getBusinessKey());
            result.put("processDefinitionId", instance.getProcessDefinitionId());
            result.put("id", instance.getId());
        }

        return result;
    }

    /**
     * Some need to register a TransactionManager or we need to create one.
     */
    @Reference
    private TransactionManager transactionManager;

    private OpenIDMProcessEngineFactory processEngineFactory = null;

    @Activate
    void activate(ComponentContext compContext) {
        logger.debug("Activating Service with configuration {}", compContext.getProperties());
        if (null == processEngine) {
            // If the processEngine was initialised outside of OpenIDM we just use that and we don't crate a new one
            try {
                EnhancedConfig enhancedConfig = new JSONEnhancedConfig();
                JsonValue config = enhancedConfig.getConfigurationAsJson(compContext);

                //initialise the DataSource
                JdbcDataSource dataSource = new org.h2.jdbcx.JdbcDataSource(); //Implement it here. There are examples in the JDBCRepoService
                File root = IdentityServer.getFileForPath("db/activiti/database");
                dataSource.setURL("jdbc:h2:file:" + URLDecoder.decode(root.getPath(), "UTF-8") + ";DB_CLOSE_DELAY=1000");
                dataSource.setUser("sa");

                /*MysqlConnectionPoolDataSource dataSource = new MysqlConnectionPoolDataSource();
                dataSource.setUser("root");
                //dataSource.setPassword("password");
                dataSource.setServerName("localhost");
                dataSource.setPort(3306);
                dataSource.setDatabaseName("activiti");*/

                //we need a TransactionManager to use this
                JtaProcessEngineConfiguration configuration = new JtaProcessEngineConfiguration();

                configuration.setDatabaseType("h2");
                configuration.setDataSource(dataSource);
                configuration.setTransactionManager(transactionManager);
                configuration.setDatabaseSchemaUpdate("true");

                //StandaloneInMemProcessEngineConfiguration configuration = new StandaloneInMemProcessEngineConfiguration();

                //needed for async workflows
                configuration.setJobExecutorActivate(true);


                /*ConfigurationFactory configurationFactory = new ConfigurationFactory();
                configurationFactory.setDataSource(dataSource);
                configurationFactory.setDatabaseSchemaUpdate("true");
                StandaloneProcessEngineConfiguration configuration = configurationFactory.getConfiguration();
                configuration.setDatabaseType("mysql");*/

                //This allow activiti to call back to OpenIDM
                openIDMELResolver.setRouter(router);

                processEngineFactory = new OpenIDMProcessEngineFactory();
                processEngineFactory.setProcessEngineConfiguration(configuration);
                processEngineFactory.setBundle(compContext.getBundleContext().getBundle());
                processEngineFactory.setOpenIDMELResolver(openIDMELResolver);
                processEngineFactory.init();

                //We are done!!
                processEngine = processEngineFactory.getObject();

                //We need to register the service because the Activiti-OSGi need this to deploy new BAR or BPMN
                Hashtable<String, String> prop = new Hashtable<String, String>();
                prop.put(Constants.SERVICE_PID, "org.forgerock.openidm.workflow.activiti.engine");
                prop.put("openidm.activiti.engine", "true");
                compContext.getBundleContext().registerService(ProcessEngine.class.getName(), processEngine, prop);

                logger.debug("Activiti ProcessEngine is enabled");
            } catch (RuntimeException ex) {
                logger.warn("Configuration invalid, can not start Activiti ProcessEngine service.", ex);
                throw ex;
            } catch (Exception ex) {
                logger.warn("Configuration invalid, can not start  Activiti ProcessEngine service.", ex);
                throw new RuntimeException(ex);
            }
        }
        logger.info(" Activiti ProcessEngine started.");
    }

    @Deactivate
    void deactivate(ComponentContext compContext) {
        logger.debug("Deactivating Service {}", compContext.getProperties());
        if (null != processEngineFactory) {
            try {
                processEngineFactory.destroy();
            } catch (Exception e) {
                //Do Something?
            }
        }
        logger.info(" Activiti ProcessEngine stopped.");
    }
}
