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
package org.forgerock.openidm.workflow.remote;

import java.util.logging.Level;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.delegate.VariableScope;
import org.activiti.engine.impl.bpmn.data.ItemInstance;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.activiti.engine.impl.el.ExpressionManager;
import org.activiti.engine.impl.el.VariableScopeElResolver;
import org.activiti.engine.impl.javax.el.*;
import org.activiti.engine.impl.persistence.entity.VariableScopeImpl;
import org.activiti.engine.impl.variable.CustomObjectType;
import org.forgerock.json.resource.restlet.JsonResourceRestlet;
import org.forgerock.openidm.objset.JsonResourceObjectSet;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.workflow.HttpRemoteJsonResource;
import org.forgerock.openidm.workflow.activiti.impl.JsonValueType;
import org.forgerock.openidm.workflow.activiti.impl.OpenIDMELResolver;
import org.forgerock.openidm.workflow.activiti.impl.ActivitiResource;
import org.h2.jdbcx.JdbcDataSource;
import org.restlet.Application;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.security.SecretVerifier;
import org.restlet.security.Verifier;
// SLF4J
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author orsolyamebold
 */
public class ActivitiIntegrationApplication extends Application {

    private final static Logger LOGGER = LoggerFactory.getLogger(ActivitiIntegrationApplication.class);
    public static final String PROCESS_ENGINE_NAME = "openidmengine";
    private ProcessEngine engine;
    private OpenIDMELResolver openIDMELResolver = new OpenIDMELResolver();
    private ChallengeAuthenticator authenticator;

    /**
     * Creates a root Restlet that will receive all incoming calls.
     */
    @Override
    public synchronized Restlet createInboundRoot() {
        try {
            if (ProcessEngines.getProcessEngines().isEmpty()) {
                //initialise the default h2 DataSource
                JdbcDataSource jdbcDataSource = new org.h2.jdbcx.JdbcDataSource();
                jdbcDataSource.setURL("jdbc:h2:tcp://localhost/activiti");
                jdbcDataSource.setUser("sa");
                ProcessEngineConfiguration configuration = new StandaloneProcessEngineConfiguration();
                configuration.setDatabaseType("h2");
                configuration.setProcessEngineName(PROCESS_ENGINE_NAME);
                openIDMELResolver.setRouter(new JsonResourceObjectSet(new HttpRemoteJsonResource()));
                ((ProcessEngineConfigurationImpl) configuration).setExpressionManager(new org.forgerock.openidm.workflow.remote.ActivitiIntegrationApplication.OpenIDMExpressionManager());

                configuration.setJobExecutorActivate(true);
                engine = configuration.buildProcessEngine();
                ((ProcessEngineConfigurationImpl) configuration).getVariableTypes().addType(new CustomObjectType("openidm", ObjectSet.class));
                ((ProcessEngineConfigurationImpl) configuration).getVariableTypes().addType(new JsonValueType());
            }
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(ActivitiIntegrationApplication.class.getName()).log(Level.SEVERE, null, ex);
        }
        Verifier verifier = new SecretVerifier() {

            @Override
            public boolean verify(String username, char[] password) {
                boolean verified = engine.getIdentityService().checkPassword(username, new String(password));
                return verified;
            }
        };

        authenticator = new ChallengeAuthenticator(null, true, ChallengeScheme.HTTP_BASIC,
                "Activiti Realm") {

            @Override
            protected boolean authenticate(Request request, Response response) {
                if (request.getChallengeResponse() == null) {
                    return false;
                } else {
                    boolean authenticated = super.authenticate(request, response);
                    if (authenticated) {
                        Parameter user = ((Form) request.getAttributes().get("org.restlet.http.headers")).getFirst("X-OpenIDM-Username", true);
                        if (user != null) {
                            engine.getIdentityService().setAuthenticatedUserId(user.getValue());
                        }
                    }
                    return authenticated;
                }
            }
        };
        authenticator.setVerifier(verifier);

        JsonResourceRestlet root = new JsonResourceRestlet(new ActivitiResource(engine));
        authenticator.setNext(root);
        return authenticator;
    }

    public class OpenIDMExpressionManager extends ExpressionManager {

        @Override
        protected ELResolver createElResolver(VariableScope variableScope) {
            CompositeELResolver compositeElResolver = new CompositeELResolver();
            compositeElResolver.add(new VariableScopeElResolver(variableScope));
            openIDMELResolver.setRouter(new JsonResourceObjectSet(new HttpRemoteJsonResource()));
            compositeElResolver.add(openIDMELResolver);
            compositeElResolver.add(new ArrayELResolver());
            compositeElResolver.add(new ListELResolver());
            compositeElResolver.add(new MapELResolver());
            compositeElResolver.add(new DynamicBeanPropertyELResolver(ItemInstance.class, "getFieldValue", "setFieldValue")); //TODO: needs verification
            compositeElResolver.add(new BeanELResolver());
            return compositeElResolver;
        }
        
        @Override
        public ELContext getElContext(VariableScope variableScope) {
            ELContext elContext = null;
            if (variableScope instanceof VariableScopeImpl) {
                if (variableScope.getVariable("openidm") == null) {
                    variableScope.removeVariable("openidm");
                }
                VariableScopeImpl variableScopeImpl = (VariableScopeImpl) variableScope;
                elContext = variableScopeImpl.getCachedElContext();
            }

            if (elContext == null) {
                elContext = createElContext(variableScope);
                if (variableScope instanceof VariableScopeImpl) {
                    ((VariableScopeImpl) variableScope).setCachedElContext(elContext);
                }
            }

            return elContext;
        }
    }
}
