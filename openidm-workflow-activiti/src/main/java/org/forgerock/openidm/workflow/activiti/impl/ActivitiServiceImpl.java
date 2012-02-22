/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2012 ForgeRock AS. All rights reserved.
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
import java.io.IOException;
import java.net.URLDecoder;
import java.util.*;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.impl.cfg.JtaProcessEngineConfiguration;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.apache.felix.scr.annotations.*;
import org.apache.felix.scr.annotations.Properties;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.json.resource.SimpleJsonResource;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.h2.jdbcx.JdbcDataSource;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.JSONEnhancedConfig;

import javax.sql.DataSource;
import javax.transaction.TransactionManager;

// JSON Resource
import org.forgerock.json.resource.JsonResource;

// Deprecated
import org.forgerock.openidm.objset.JsonResourceObjectSet;

/**
 * Workflow service implementation
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
@Component(name = ActivitiServiceImpl.PID, immediate = true, policy = ConfigurationPolicy.OPTIONAL)
@Service
@References({
        @Reference(
                name = "JavaDelegateServiceReference",
                referenceInterface = JavaDelegate.class,
                bind = "bindService",
                unbind = "unbindService",
                cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
                policy = ReferencePolicy.DYNAMIC
        ),
        @Reference(
                name = "ref_ActivitiServiceImpl_JsonResourceRouterService",
                referenceInterface = JsonResource.class,
                bind = "bindRouter",
                unbind = "unbindRouter",
                cardinality = ReferenceCardinality.OPTIONAL_UNARY,
                policy = ReferencePolicy.DYNAMIC,
                target = "(service.pid=org.forgerock.openidm.router)")})
@Properties({
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "Activiti Service"),
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = ServerConstants.ROUTER_PREFIX, value = ActivitiServiceImpl.ROUTER_PREFIX)
})
public class ActivitiServiceImpl implements JsonResource {
    final static Logger logger = LoggerFactory.getLogger(ActivitiServiceImpl.class);
    public final static String PID = "org.forgerock.openidm.workflow.activiti";
    public final static String ROUTER_PREFIX = "workflow/activiti";

    //Just to make fancy the sample. Let's allow to others to register the service and we just use it
    private boolean selfMadeProcessEngine = true;

    private final OpenIDMELResolver openIDMELResolver = new OpenIDMELResolver();

    private final SharedIdentityService identityService = new SharedIdentityService();


    protected void bindRouter(JsonResource router) {
        this.openIDMELResolver.setRouter(new JsonResourceObjectSet(router));
        this.identityService.setRouter(router);
    }

    protected void unbindRouter(JsonResource router) {
        this.openIDMELResolver.setRouter(null);
        this.identityService.setRouter(null);
    }

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

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    private ConfigurationAdmin configurationAdmin = null;

    /**
     * Some need to register a TransactionManager or we need to create one.
     */
    @Reference
    private TransactionManager transactionManager;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, target = "(osgi.jndi.service.name=jdbc/openidm)")
    private DataSource dataSource;

    private OpenIDMProcessEngineFactory processEngineFactory = null;
    private Configuration barInstallerConfiguration = null;

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
    public JsonValue handle(JsonValue request) throws JsonResourceException {
        try {
            switch (request.get("method").required().asEnum(SimpleJsonResource.Method.class)) {
                case create:
                    return null;
                case read:
                    return read();
                case update:
                    return null;
                case delete:
                    return null;
                case patch:
                    return null;
                case query:
                    return null;
                case action:
                    return action(request);
                default:
                    throw new JsonResourceException(JsonResourceException.BAD_REQUEST);
            }
        } catch (JsonValueException jve) {
            throw new JsonResourceException(JsonResourceException.BAD_REQUEST, jve);
        }
    }


    public JsonValue read() throws JsonResourceException {
        JsonValue result = new JsonValue(new HashMap<String, Object>());
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

    public JsonValue action(JsonValue params) throws JsonResourceException {
        JsonValue result = null;
        String action = params.get("params").get("_action").required().asString();
        JsonValue workflowParams = params.get("value");

        //POST openidm/workflow/activiti?_action=TestWorkFlow will trigger the process
        ProcessInstance instance = null;
        Map<String, Object> variables;
        if (workflowParams.isNull()) {
            variables = new HashMap<String, Object>(1);
        } else {
            variables = new HashMap<String, Object>(workflowParams.asMap());
        }
        //TODO consider to put only the parent into the params. parent/security may contain confidential access token
        variables.put("openidm-context", params);
        instance = processEngine.getRuntimeService().startProcessInstanceByKey(action, variables);
        if (null != instance) {
            result = new JsonValue(new HashMap<String, Object>());
            result.put("status", instance.isEnded() ? "ended" : "suspended");
            result.put("processInstanceId", instance.getProcessInstanceId());
            result.put("businessKey", instance.getBusinessKey());
            result.put("processDefinitionId", instance.getProcessDefinitionId());
            result.put("id", instance.getId());
        }

        return result;
    }

    @Activate
    void activate(ComponentContext compContext) {
        logger.debug("Activating Service with configuration {}", compContext.getProperties());
        if (null == processEngine) {
            // If the processEngine was initialised outside of OpenIDM we just use that and we don't crate a new one
            try {
                EnhancedConfig enhancedConfig = new JSONEnhancedConfig();
                JsonValue config = enhancedConfig.getConfigurationAsJson(compContext);

                /*MysqlConnectionPoolDataSource dataSource = new MysqlConnectionPoolDataSource();
                dataSource.setUser("root");
                //dataSource.setPassword("password");
                dataSource.setServerName("localhost");
                dataSource.setPort(3306);
                dataSource.setDatabaseName("activiti");*/

                //we need a TransactionManager to use this
                JtaProcessEngineConfiguration configuration = new JtaProcessEngineConfiguration();

                if (null == dataSource) {
                    //initialise the DataSource
                    JdbcDataSource jdbcDataSource = new org.h2.jdbcx.JdbcDataSource(); //Implement it here. There are examples in the JDBCRepoService
                    File root = IdentityServer.getFileForPath("db/activiti/database");
                    jdbcDataSource.setURL("jdbc:h2:file:" + URLDecoder.decode(root.getPath(), "UTF-8") + ";DB_CLOSE_DELAY=1000");
                    jdbcDataSource.setUser("sa");
                    configuration.setDatabaseType("h2");
                    configuration.setDataSource(jdbcDataSource);
                } else {
                    configuration.setDatabaseType("mysql");
                    configuration.setDataSource(dataSource);
                    configuration.setIdentityService(identityService);
                }


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

                if (null != configurationAdmin) {
                    barInstallerConfiguration = configurationAdmin.createFactoryConfiguration("org.apache.felix.fileinstall", null);
                    Dictionary props = barInstallerConfiguration.getProperties();
                    if (props == null) {
                        props = new Hashtable();
                    }
                    props.put("felix.fileinstall.poll", "2000");
                    props.put("felix.fileinstall.noInitialDelay", "true");
                    //TODO java.net.URLDecoder.decode(IdentityServer.getFileForPath("workflow").getAbsolutePath(),"UTF-8")
                    props.put("felix.fileinstall.dir", IdentityServer.getFileForPath("workflow").getAbsolutePath());
                    props.put("felix.fileinstall.filter", ".*\\.bar");
                    props.put("felix.fileinstall.bundles.new.start", "true");
                    props.put("config.factory-pid", "activiti");
                    barInstallerConfiguration.update(props);
                }
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
        if (null != barInstallerConfiguration) {
            try {
                barInstallerConfiguration.delete();
            } catch (IOException e) {
                logger.error("Can not delete org.apache.felix.fileinstall-activiti configuration", e);
            }
            barInstallerConfiguration = null;
        }
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
