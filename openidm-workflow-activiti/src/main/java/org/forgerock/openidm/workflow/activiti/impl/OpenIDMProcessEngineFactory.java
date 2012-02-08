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

import org.activiti.engine.delegate.VariableScope;
import org.activiti.engine.impl.bpmn.data.ItemInstance;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.el.ExpressionManager;
import org.activiti.engine.impl.el.VariableScopeElResolver;
import org.activiti.engine.impl.javax.el.*;
import org.activiti.engine.impl.variable.CustomObjectType;
import org.activiti.osgi.blueprint.ProcessEngineFactory;
import org.forgerock.openidm.objset.ObjectSet;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class OpenIDMProcessEngineFactory extends ProcessEngineFactory {

    private OpenIDMELResolver openIDMELResolver;

    @Override
    public void init() throws Exception {
        ((ProcessEngineConfigurationImpl) getProcessEngineConfiguration()).setExpressionManager(new OpenIDMExpressionManager());
        super.init();
        ((ProcessEngineConfigurationImpl) getProcessEngineConfiguration()).getVariableTypes().addType(new CustomObjectType("openidm", ObjectSet.class));
        ((ProcessEngineConfigurationImpl) getProcessEngineConfiguration()).getVariableTypes().addType(new JsonValueType());
    }

    public class OpenIDMExpressionManager extends ExpressionManager {
        @Override
        protected ELResolver createElResolver(VariableScope variableScope) {
            CompositeELResolver compositeElResolver = new CompositeELResolver();
            compositeElResolver.add(new VariableScopeElResolver(variableScope));
            compositeElResolver.add(openIDMELResolver);
            compositeElResolver.add(new ArrayELResolver());
            compositeElResolver.add(new ListELResolver());
            compositeElResolver.add(new MapELResolver());
            compositeElResolver.add(new DynamicBeanPropertyELResolver(ItemInstance.class, "getFieldValue", "setFieldValue")); //TODO: needs verification
            compositeElResolver.add(new BeanELResolver());
            return compositeElResolver;

        }
    }

    public void setOpenIDMELResolver(OpenIDMELResolver openIDMELResolver) {
        this.openIDMELResolver = openIDMELResolver;
    }
}