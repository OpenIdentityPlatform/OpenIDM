/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock Inc. All rights reserved.
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
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.impl.cfg.JtaProcessEngineConfiguration;
import org.activiti.engine.impl.interceptor.SessionFactory;
import org.activiti.engine.impl.scripting.ResolverFactory;
import org.activiti.osgi.blueprint.ProcessEngineFactory;
import org.apache.felix.scr.annotations.*;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResource;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.config.InvalidException;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.objset.JsonResourceObjectSet;
import org.forgerock.openidm.objset.ServiceUnavailableException;
import org.forgerock.openidm.workflow.HttpRemoteJsonResource;
import org.forgerock.openidm.workflow.activiti.impl.session.OpenIDMSessionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Workflow service implementation
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
@Component(name = ActivitiServiceImpl.PID, immediate = true, policy = ConfigurationPolicy.REQUIRE)
@Service
@Properties({
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Workflow Service"),
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = ActivitiServiceImpl.ROUTER_PREFIX)})
@References({
    @Reference(name = "JavaDelegateServiceReference",
    referenceInterface = JavaDelegate.class,
    bind = "bindService",
    unbind = "unbindService",
    cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
    policy = ReferencePolicy.DYNAMIC),
    @Reference(name = "ref_ActivitiServiceImpl_JsonResourceRouterService",
    referenceInterface = JsonResource.class,
    bind = "bindRouter",
    unbind = "unbindRouter",
    cardinality = ReferenceCardinality.OPTIONAL_UNARY,
    policy = ReferencePolicy.DYNAMIC,
    target = "(service.pid=org.forgerock.openidm.router)")})
public class ActivitiServiceImpl implements JsonResource {

    final static Logger logger = LoggerFactory.getLogger(ActivitiServiceImpl.class);
    public final static String PID = "org.forgerock.openidm.workflow";
    public final static String ROUTER_PREFIX = "workflow";
    // Keys in the JSON configuration
    public static final String CONFIG_ENABLED = "enabled";
    public static final String CONFIG_LOCATION = "location";
    public static final String CONFIG_ENGINE = "engine";
    public static final String CONFIG_ENGINE_URL = "engine/url";
    public static final String CONFIG_ENGINE_USERNAME = "engine/username";
    public static final String CONFIG_ENGINE_PASSWORD = "engine/password";
    public static final String CONFIG_MAIL = "mail";
    public static final String CONFIG_MAIL_HOST = "host";
    public static final String CONFIG_MAIL_PORT = "port";
    public static final String CONFIG_MAIL_USERNAME = "username";
    public static final String CONFIG_MAIL_PASSWORD = "password";
    public static final String CONFIG_MAIL_STARTTLS = "starttls";
    public static final String CONFIG_CONNECTION = "connection";
    public static final String CONFIG_JNDI_NAME = "jndiName";
    public static final String CONFIG_HISTORY = "history";
    private String jndiName;
    private boolean selfMadeProcessEngine = true;
    @Reference(name = "processEngine",
    referenceInterface = ProcessEngine.class,
    bind = "bindProcessEngine",
    unbind = "unbindProcessEngine",
    cardinality = ReferenceCardinality.OPTIONAL_UNARY,
    policy = ReferencePolicy.STATIC,
    target = "(!openidm.activiti.engine=true)" //avoid register the self made service
    )
    private ProcessEngine processEngine;
    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    private ConfigurationAdmin configurationAdmin = null;
    /**
     * Some need to register a TransactionManager or we need to create one.
     */
    @Reference
    private TransactionManager transactionManager;
    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, target = "(osgi.jndi.service.name=jdbc/openidm)")
    private DataSource dataSource;
    private final OpenIDMELResolver openIDMELResolver = new OpenIDMELResolver();
    private final SharedIdentityService identityService = new SharedIdentityService();
    private final OpenIDMSessionFactory idmSessionFactory = new OpenIDMSessionFactory();
    private ProcessEngineFactory processEngineFactory;
    private Configuration barInstallerConfiguration;
    private JsonResource activitiResource;
    //Configuration variables
    private boolean enabled;
    private EngineLocation location = EngineLocation.embedded;
    private String url;
    private String username;
    private String password;
    private String mailhost = "localhost";
    private int mailport = 25;
    private String mailusername;
    private String mailpassword;
    private boolean starttls;
    private String historyLevel;

    public enum EngineLocation {

        embedded, local, remote
    }

    @Activate
    void activate(ComponentContext compContext) {
        logger.debug("Activating Service with configuration {}", compContext.getProperties());
        try {
            readConfiguration(compContext);
            if (enabled) {
                switch (location) {
                    case embedded: //start our embedded ProcessEngine
                        try {
                            // Data Source configuration
                            if (jndiName != null && jndiName.trim().length() > 0) {
                                // Get DB connection via JNDI
                                logger.info("Using DB connection configured via Driver Manager");
                                InitialContext ctx = null;
                                try {
                                    ctx = new InitialContext();
                                } catch (NamingException ex) {
                                    logger.warn("Getting JNDI initial context failed: " + ex.getMessage(), ex);
                                }
                                if (ctx == null) {
                                    throw new InvalidException("Current platform context does not support lookup of repository DB via JNDI. "
                                            + " Use embedded OpenIDM repository instead.");
                                }
                                dataSource = (DataSource) ctx.lookup(jndiName);
                            }
                        } catch (RuntimeException ex) {
                            logger.warn("Configuration invalid, can not start JDBC repository.", ex);
                            throw new InvalidException("Configuration invalid, can not start JDBC repository.", ex);
                        } catch (NamingException ex) {
                            throw new InvalidException("Could not find configured jndiName " + jndiName + " to start repository ", ex);
                        }

                        //we need a TransactionManager to use this
                        JtaProcessEngineConfiguration configuration = new JtaProcessEngineConfiguration();

                        if (null == dataSource) {
                            //initialise the default h2 DataSource
                            JdbcDataSource jdbcDataSource = new org.h2.jdbcx.JdbcDataSource(); //Implement it here. There are examples in the JDBCRepoService
                            File root = IdentityServer.getFileForPath("db/activiti/database");
                            jdbcDataSource.setURL("jdbc:h2:file:" + URLDecoder.decode(root.getPath(), "UTF-8") + ";DB_CLOSE_DELAY=1000");
                            jdbcDataSource.setUser("sa");
                            configuration.setDatabaseType("h2");
                            configuration.setDataSource(jdbcDataSource);
                        } else {
                            configuration.setDataSource(dataSource);
                            configuration.setIdentityService(identityService);
                        }

                        configuration.setTransactionManager(transactionManager);
                        configuration.setDatabaseSchemaUpdate("true");

                        List<SessionFactory> customSessionFactories = configuration.getCustomSessionFactories();
                        if (customSessionFactories == null) {
                            customSessionFactories = new ArrayList<SessionFactory>();
                        }
                        customSessionFactories.add(idmSessionFactory);
                        configuration.setCustomSessionFactories(customSessionFactories);
                        configuration.setExpressionManager(new OpenIDMExpressionManager());

                        configuration.setMailServerHost(mailhost);
                        configuration.setMailServerPort(mailport);
                        configuration.setMailServerUseTLS(starttls);
                        if (mailusername != null) {
                            configuration.setMailServerUsername(mailusername);
                        }
                        if (mailpassword != null) {
                            configuration.setMailServerPassword(mailpassword);
                        }

                        if (historyLevel != null) {
                            configuration.setHistory(historyLevel);
                        }

                        //needed for async workflows
                        configuration.setJobExecutorActivate(true);

                        processEngineFactory = new ProcessEngineFactory();
                        processEngineFactory.setProcessEngineConfiguration(configuration);
                        processEngineFactory.setBundle(compContext.getBundleContext().getBundle());
                        processEngineFactory.init();

                        //ScriptResolverFactory
                        List<ResolverFactory> resolverFactories = configuration.getResolverFactories();
                        resolverFactories.add(new OpenIDMResolverFactory());
                        configuration.setResolverFactories(resolverFactories);

                        //We are done!!
                        processEngine = processEngineFactory.getObject();
                        //We need to register the service because the Activiti-OSGi need this to deploy new BAR or BPMN
                        Hashtable<String, String> prop = new Hashtable<String, String>();
                        prop.put(Constants.SERVICE_PID, "org.forgerock.openidm.workflow.activiti.engine");
                        prop.put("openidm.activiti.engine", "true");
                        compContext.getBundleContext().registerService(ProcessEngine.class.getName(), processEngine, prop);

                        if (null != configurationAdmin) {
                            try {
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
                            } catch (IOException ex) {
                                java.util.logging.Logger.getLogger(ActivitiServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                        activitiResource = new ActivitiResource(processEngine);
                        logger.debug("Activiti ProcessEngine is enabled");
                        break;
                    case local: //ProcessEngine is connected by @Reference
                        activitiResource = new ActivitiResource(processEngine);
                        break;
                    case remote: //fetch remote connection parameters
                        activitiResource = new HttpRemoteJsonResource(url, username, password);
                        break;
                    default:
                        throw new InvalidException(CONFIG_LOCATION + " invalid, can not start workflow service.");
                }
            }
        } catch (RuntimeException ex) {
            logger.warn("Configuration invalid, can not start Activiti ProcessEngine service.", ex);
            throw ex;
        } catch (Exception ex) {
            logger.warn("Configuration invalid, can not start  Activiti ProcessEngine service.", ex);
            throw new RuntimeException(ex);
        }
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

    /**
     * Read and process Workflow configuration file
     *
     * @param compContext
     */
    private void readConfiguration(ComponentContext compContext) {
        EnhancedConfig enhancedConfig = new JSONEnhancedConfig();
        JsonValue config = enhancedConfig.getConfigurationAsJson(compContext);
        if (!config.isNull()) {
            enabled = config.get(CONFIG_ENABLED).defaultTo(true).asBoolean();
            location = config.get(CONFIG_LOCATION).defaultTo(EngineLocation.embedded.name()).asEnum(EngineLocation.class);
            JsonValue connectionConfig = config.get(CONFIG_CONNECTION);
            jndiName = connectionConfig.get(CONFIG_JNDI_NAME).asString();
            JsonValue mailconfig = config.get(CONFIG_MAIL);
            if (!mailconfig.isNull()) {
                mailhost = mailconfig.get(new JsonPointer(CONFIG_MAIL_HOST)).asString();
                mailport = mailconfig.get(new JsonPointer(CONFIG_MAIL_PORT)).asInteger();
                mailusername = mailconfig.get(new JsonPointer(CONFIG_MAIL_USERNAME)).asString();
                mailpassword = mailconfig.get(new JsonPointer(CONFIG_MAIL_PASSWORD)).asString();
                starttls = mailconfig.get(new JsonPointer(CONFIG_MAIL_STARTTLS)).asBoolean();
            }
            JsonValue engineConfig = config.get(CONFIG_ENGINE);
            if (!engineConfig.isNull()) {
                url = config.get(new JsonPointer(CONFIG_ENGINE_URL)).asString();
                username = config.get(new JsonPointer(CONFIG_ENGINE_USERNAME)).asString();
                password = config.get(new JsonPointer(CONFIG_ENGINE_PASSWORD)).asString();
            }
            historyLevel = config.get(CONFIG_HISTORY).asString();
        }
    }

    //This method called before activate if there is a ProcessEngine service in the Service Registry
    protected void bindProcessEngine(ProcessEngine processEngine) {
        logger.info("Some other process already created the ProcessEngine so we don't need to make our own");
        if (null == processEngine) {
            this.processEngine = processEngine;
            selfMadeProcessEngine = false;
        }
    }

    protected void unbindProcessEngine(ProcessEngine processEngine) {
        if (!selfMadeProcessEngine) {
            this.processEngine = null;
            this.activitiResource = null;
        }
        logger.info("ProcessEngine stopped.");
    }

    protected void bindRouter(JsonResource router) {
        this.idmSessionFactory.setRouter(new JsonResourceObjectSet(router));
        this.identityService.setRouter(router);
    }

    protected void unbindRouter(JsonResource router) {
        this.idmSessionFactory.setRouter(null);
        this.identityService.setRouter(null);
    }

    public void bindService(JavaDelegate delegate, Map props) {
        openIDMELResolver.bindService(delegate, props);
    }

    public void unbindService(JavaDelegate delegate, Map props) {
        openIDMELResolver.unbindService(delegate, props);
    }

    @Override
    public JsonValue handle(JsonValue request) throws JsonResourceException {
        if (activitiResource != null) {
            return activitiResource.handle(request);
        } else {
            throw new ServiceUnavailableException("No workflow resource is available");
        }
    }
}
