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

import java.util.HashMap;
import java.util.Map;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.delegate.VariableScope;
import org.activiti.engine.impl.bpmn.data.ItemInstance;
import org.activiti.engine.impl.el.ExpressionManager;
import org.activiti.engine.impl.el.ReadOnlyMapELResolver;
import org.activiti.engine.impl.el.VariableScopeElResolver;
import org.activiti.engine.impl.javax.el.*;
import org.osgi.service.component.ComponentConstants;

/**
 * Custom ExpressionManager for resolving 'openidm' variable in expressions
 *
 * @author orsolyamebold
 */
public class OpenIDMExpressionManager extends ExpressionManager {
    private Map<String, JavaDelegate> delegateMap = new HashMap<String, JavaDelegate>();
    
    public void bindService(JavaDelegate delegate, Map props) {
        String name = (String) props.get(ComponentConstants.COMPONENT_NAME);
        if (name == null) { //handle blueprint services as well
            name = (String) props.get("osgi.service.blueprint.compname");
        }
        if (name != null) {
            delegateMap.put(name, delegate);
        }
    }

    public void unbindService(JavaDelegate delegate, Map props) {
        String name = (String) props.get(ComponentConstants.COMPONENT_NAME);
        if (delegateMap.containsKey(name)) {
            delegateMap.remove(name);
        }
    }

    @Override
    protected ELResolver createElResolver(VariableScope variableScope) {
        CompositeELResolver elResolver = new CompositeELResolver();
        elResolver.add(new VariableScopeElResolver(variableScope));
        elResolver.add(new OpenIDMELResolver(delegateMap));

        if(beans != null) {
        // ACT-1102: Also expose all beans in configuration when using standalone activiti, not
        // in spring-context
        elResolver.add(new ReadOnlyMapELResolver(beans));
        }

        elResolver.add(new ArrayELResolver());
        elResolver.add(new ListELResolver());
        elResolver.add(new MapELResolver());
        elResolver.add(new DynamicBeanPropertyELResolver(ItemInstance.class, "getFieldValue", "setFieldValue")); //TODO: needs verification
        elResolver.add(new BeanELResolver());
        return elResolver;
    }
}
