/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright © 2012-2014 ForgeRock Inc. All rights reserved.
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

import org.forgerock.openidm.workflow.activiti.ActivitiConstants;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.delegate.VariableScope;
import org.activiti.engine.impl.bpmn.behavior.ScriptTaskActivityBehavior;
import org.activiti.engine.impl.bpmn.helper.ClassDelegate;
import org.activiti.engine.impl.bpmn.parser.FieldDeclaration;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.el.Expression;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.impl.pvm.delegate.ActivityBehavior;
import org.activiti.engine.impl.scripting.Resolver;
import org.activiti.engine.impl.scripting.ResolverFactory;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.PersistenceConfig;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openidm.workflow.activiti.impl.session.OpenIDMSession;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptName;
import org.forgerock.script.ScriptRegistry;
import org.slf4j.LoggerFactory;

/**
 * Factory for resolving openidm variable in scripts
 *
 * @author orsolyamebold
 */
public class OpenIDMResolverFactory implements ResolverFactory {

    @Override
    public Resolver createResolver(VariableScope variableScope) {
        Bindings bindings;
        ScriptTaskActivityBehavior behaviour;
        String language = "groovy";
        Map<String, String> scriptJson = new HashMap<String, String>(3);
        ServerContext context;
        ScriptEntry script;

        OpenIDMSession session = Context.getCommandContext().getSession(OpenIDMSession.class);
        PersistenceConfig persistenceConfig = session.getOpenIDMPersistenceConfig();
        ScriptRegistry scriptRegistry = session.getOpenIDMScriptRegistry();
        JsonValue openidmContext = (JsonValue) variableScope.getVariable(ActivitiConstants.OPENIDM_CONTEXT);

        try {
            if (variableScope instanceof ExecutionEntity) {
                ActivityBehavior activityBehavior = ((ExecutionEntity) variableScope).getActivity().getActivityBehavior();
                //Called from ScriptTask
                if (activityBehavior instanceof ScriptTaskActivityBehavior) {
                    behaviour = (ScriptTaskActivityBehavior) activityBehavior;
                    Class cls = Class.forName("org.activiti.engine.impl.bpmn.behavior.ScriptTaskActivityBehavior");
                    Field languageField = cls.getDeclaredField("language");
                    languageField.setAccessible(true);
                    language = (String) languageField.get(behaviour);
                } else {
                    //Called from ExecutionListener
                    String eventName = ((ExecutionEntity) variableScope).getEventName();
                    List<ExecutionListener> executionListeners = ((ExecutionEntity) variableScope).getActivity().getExecutionListeners(eventName);
                    for (ExecutionListener executionListener : executionListeners) {
                        if (executionListener instanceof ClassDelegate) {
                            String className = ((ClassDelegate) executionListener).getClassName();
                            if ("org.activiti.engine.impl.bpmn.listener.ScriptExecutionListener".equals(className)) {
                                language = processClassDelegate(variableScope, (ClassDelegate) executionListener);
                            }
                        }
                    }
                }
            } else if (variableScope instanceof TaskEntity) {
                //Called from TaskListener
                String eventName = ((TaskEntity) variableScope).getEventName();
                Map<String, List<TaskListener>> taskListeners = ((TaskEntity) variableScope).getTaskDefinition().getTaskListeners();
                List<TaskListener> eventListeners = taskListeners.get(eventName);
                for (TaskListener taskListener : eventListeners) {
                    if (taskListener instanceof ClassDelegate) {
                        String className = ((ClassDelegate) taskListener).getClassName();
                        if ("org.activiti.engine.impl.bpmn.listener.ScriptTaskListener".equals(className)) {
                            language = processClassDelegate(variableScope, (ClassDelegate) taskListener);
                        }
                    }
                }
            } else {
                LoggerFactory.getLogger(OpenIDMResolverFactory.class).info("Script language could not be determined, using default groovy instead");
            }
        } catch (Exception ex) {
            LoggerFactory.getLogger(OpenIDMResolverFactory.class).error(ex.getMessage(), ex);
        }

        try {
            context = new ServerContext(openidmContext, persistenceConfig);
            script = scriptRegistry.takeScript(new ScriptName("ActivitiScript", language));
            if (script == null) {
                scriptJson.put("source", "");
                scriptJson.put("type", language);
                scriptJson.put("name", "ActivitiScript");
                script = scriptRegistry.takeScript(new JsonValue(scriptJson));
            }
        } catch (Exception ex) {
            LoggerFactory.getLogger(OpenIDMResolverFactory.class).error(ex.getMessage(), ex);
            throw new ActivitiException(ex.getMessage(), ex);
        }
        bindings = script.getScriptBindings(context, null);
        return new OpenIDMResolver(bindings);
    }
    
    /**
     * Processes a ClassDelegate, fetching the language field of it.
     * @param scope variable scope the function was called from
     * @param delegate object containing the language field
     * @return value of language field, if not found groovy is the default language
     */
    private String processClassDelegate(VariableScope scope, ClassDelegate delegate) {
        String language = "groovy";
        try {
            Class cls = Class.forName("org.activiti.engine.impl.bpmn.helper.ClassDelegate");
            Field languageFields = cls.getDeclaredField("fieldDeclarations");
            languageFields.setAccessible(true);
            List<FieldDeclaration> fieldDeclarations = (List<FieldDeclaration>) languageFields.get(delegate);
            for (FieldDeclaration field : fieldDeclarations) {
                if ("language".equals(field.getName())) {
                    language = (String) ((Expression) field.getValue()).getValue(scope);
                }
            }
        } catch (Exception ex) {
            LoggerFactory.getLogger(OpenIDMResolverFactory.class).error(ex.getMessage(), ex);
        }
        return language;
    }
}
