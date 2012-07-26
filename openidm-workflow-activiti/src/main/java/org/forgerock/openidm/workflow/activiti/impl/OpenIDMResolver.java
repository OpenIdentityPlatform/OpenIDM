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

import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.scripting.Resolver;
import org.forgerock.openidm.workflow.activiti.impl.session.OpenIDMSession;

/**
 * Custom resolver for OpenIDM (used in scripts)
 * @author orsolyamebold
 */
public class OpenIDMResolver implements Resolver {

    @Override
    public boolean containsKey(Object key) {
        return "openidm".equals(key);
    }

    @Override
    public Object get(Object key) {
        OpenIDMSession session = Context.getCommandContext().getSession(OpenIDMSession.class);
        return session.getOpenIDM();
    }
}
