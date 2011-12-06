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

import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.impl.javax.el.ELContext;
import org.activiti.engine.impl.javax.el.ELResolver;
import org.forgerock.openidm.objset.ObjectSet;
import org.osgi.service.component.ComponentConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.FeatureDescriptor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class OpenIDMELResolver extends ELResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenIDMELResolver.class);
    private Map<String, JavaDelegate> delegateMap = new HashMap<String, JavaDelegate>();
    private ObjectSet router = null;

    public Object getValue(ELContext context, Object base, Object property) {
        if (base == null) {
            // according to javadoc, can only be a String
            String key = (String) property;
            if (null != router && "openidm".equals(key)) {
                context.setPropertyResolved(true);
                return new OpenIDMDelegate(router);
            } else if (null != router && "openidm.router".equals(key)) {
                context.setPropertyResolved(true);
                return router;
            } else {
                for (String name : delegateMap.keySet()) {
                    if (name.equalsIgnoreCase(key)) {
                        context.setPropertyResolved(true);
                        return delegateMap.get(name);
                    }
                }
            }
        }

        return null;
    }

    public void bindService(JavaDelegate delegate, Map props) {
        String name = (String) props.get(ComponentConstants.COMPONENT_ID);
        delegateMap.put(name, delegate);
        LOGGER.info("added Activiti service to delegate cache " + name);
    }

    public void unbindService(JavaDelegate delegate, Map props) {
        String name = (String) props.get(ComponentConstants.COMPONENT_ID);
        if (delegateMap.containsKey(name)) {
            delegateMap.remove(name);
        }
        LOGGER.info("removed Activiti service from delegate cache " + name);
    }

    public boolean isReadOnly(ELContext context, Object base, Object property) {
        return true;
    }

    public void setValue(ELContext context, Object base, Object property, Object value) {
    }

    public Class<?> getCommonPropertyType(ELContext context, Object arg) {
        return Object.class;
    }

    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object arg) {
        return null;
    }

    public Class<?> getType(ELContext context, Object arg1, Object arg2) {
        return Object.class;
    }

    public void setRouter(ObjectSet router) {
        this.router = router;
    }
}

