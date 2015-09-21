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
 * Copyright 2011-2015 ForgeRock AS.
 */
package org.forgerock.openidm.workflow.activiti.impl;

import org.forgerock.openidm.workflow.activiti.ActivitiConstants;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.impl.javax.el.ELContext;
import org.activiti.engine.impl.javax.el.ELResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.FeatureDescriptor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.script.Bindings;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.impl.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.script.ScriptRegistry;
import org.forgerock.openidm.workflow.activiti.impl.session.OpenIDMSession;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptName;
import org.forgerock.script.groovy.FunctionClosure;
import org.forgerock.util.LazyMap;

/**
 * Custom ExpressionResolver for OpenIDM
 *
 * @version $Revision$ $Date$
 */
public class OpenIDMELResolver extends ELResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenIDMELResolver.class);
    private Map<String, JavaDelegate> delegateMap = new HashMap<String, JavaDelegate>();
    private ClassLoader classLoader;
    private ScriptRegistry scriptRegistry;

    public OpenIDMELResolver(Map<String, JavaDelegate> delegateMap) {
        this.delegateMap = delegateMap;
    }
    
    @Override
    public Object getValue(ELContext elContext, Object base, Object property) {
        OpenIDMSession session = Context.getCommandContext().getSession(OpenIDMSession.class);
        classLoader = session.getClassLoader();
        scriptRegistry = session.getOpenIDMScriptRegistry();
        Map<String, String> scriptJson = new HashMap<String, String>(3);
        Bindings bindings = null;
        String key = (String) property;
        try {
            JsonValue openidmContext = (JsonValue) elContext.getELResolver().getValue(elContext, null, ActivitiConstants.OPENIDM_CONTEXT);
            org.forgerock.services.context.Context context = new ActivitiContext(openidmContext, classLoader);
            ScriptEntry script = scriptRegistry.takeScript(new ScriptName("ActivitiScript", "groovy"));
            if (script == null) {
                scriptJson.put("source", "");
                scriptJson.put("type", "groovy");
                scriptJson.put("name", "ActivitiScript");
                script = scriptRegistry.takeScript(new JsonValue(scriptJson));
            }
            bindings = script.getScriptBindings(context, null);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            throw new ActivitiException(ex.getMessage(), ex);
        }
        if (base == null) {
            // according to javadoc, can only be a String
            if (bindings.containsKey(key)) {
                elContext.setPropertyResolved(true);
                return bindings.get(key);
            } else {
                for (String name : delegateMap.keySet()) {
                    if (name.equalsIgnoreCase(key)) {
                        elContext.setPropertyResolved(true);
                        return delegateMap.get(name);
                    }
                }
            }
        }
        //fetching the openidmcontext sets it to true, we need to set it 
        //to false again if the property was not found
        elContext.setPropertyResolved(false);
        return null;
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) {
        return true;
    }

    @Override
    public void setValue(ELContext context, Object base, Object property, Object value) {
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object arg) {
        return Object.class;
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object arg) {
        return null;
    }

    @Override
    public Class<?> getType(ELContext context, Object arg1, Object arg2) {
        return Object.class;
    }

    /**
     * Called when openidm.xxx() is called from an Expression
     * @return result of the call
    */
    @Override
    public Object invoke(ELContext context, Object base, Object method, Class<?>[] paramTypes, Object[] params) {
        if (base instanceof LazyMap && ((LazyMap)base).containsKey(method)) {
            context.setPropertyResolved(true);
            FunctionClosure function = (FunctionClosure)((LazyMap)base).get(method);
            return function.doCall(params);
        }
        return null;
    }
}
