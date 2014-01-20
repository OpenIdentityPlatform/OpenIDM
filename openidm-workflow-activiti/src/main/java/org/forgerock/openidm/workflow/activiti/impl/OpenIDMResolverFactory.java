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
import java.util.Map;
import javax.script.Bindings;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.delegate.VariableScope;
import org.activiti.engine.impl.bpmn.behavior.ScriptTaskActivityBehavior;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
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
        OpenIDMSession session = Context.getCommandContext().getSession(OpenIDMSession.class);
        PersistenceConfig persistenceConfig = session.getOpenIDMPersistenceConfig();
        ScriptRegistry scriptRegistry = session.getOpenIDMScriptRegistry();
        JsonValue openidmContext = (JsonValue) variableScope.getVariable(ActivitiConstants.OPENIDM_CONTEXT);
        Bindings bindings = null;
        ScriptTaskActivityBehavior behaviour = (ScriptTaskActivityBehavior) ((ExecutionEntity) variableScope).getProcessInstance().getActivity().getActivityBehavior();
        try {
            Class cls;
            String language;
            Map<String, String> scriptJson = new HashMap<String, String>(3);
            cls = Class.forName("org.activiti.engine.impl.bpmn.behavior.ScriptTaskActivityBehavior");
            Field languageField = cls.getDeclaredField("language");
            languageField.setAccessible(true);
            language = (String) languageField.get(behaviour);
            ServerContext context = new ServerContext(openidmContext, persistenceConfig);
            ScriptEntry script = scriptRegistry.takeScript(new ScriptName("ActivitiScript", language));
            if (script == null) {
                scriptJson.put("source", "");
                scriptJson.put("type", language);
                scriptJson.put("name", "ActivitiScript");
                script = scriptRegistry.takeScript(new JsonValue(scriptJson));
            }
            bindings = script.getScriptBindings(context, null);
        } catch (Exception ex) {
            LoggerFactory.getLogger(OpenIDMResolverFactory.class).error(ex.getMessage(), ex);
            throw new ActivitiException(ex.getMessage(), ex);
        }
        return new OpenIDMResolver(bindings);
    }
}
