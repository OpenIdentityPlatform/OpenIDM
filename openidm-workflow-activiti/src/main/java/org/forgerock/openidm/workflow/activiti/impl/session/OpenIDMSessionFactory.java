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
package org.forgerock.openidm.workflow.activiti.impl.session;

import org.activiti.engine.impl.interceptor.Session;
import org.activiti.engine.impl.interceptor.SessionFactory;
import org.forgerock.openidm.objset.JsonResourceObjectSet;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.workflow.HttpRemoteJsonResource;

/**
 * Custom SessionFactory for OpenIDM
 * Provides access to the OpenIDM router
 * 
 * @author orsolyamebold
 */
public class OpenIDMSessionFactory implements SessionFactory {

    private ObjectSet router;
    private String url;
    private String user;
    private String password;

    /**
     * Creates new OpenIDMSessionFactory
     */
    public OpenIDMSessionFactory() {
    }

    /**
     * Creates new OpenIDMSessionFactory
     * @param url base URL of the OpenIDM REST interface
     * @param user OpenIDM username
     * @param password OpenIDM password
     */
    public OpenIDMSessionFactory(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setUser(String user) {
        this.user = user;
    }
    
    /**
     * Creates new OpenIDMSessionFactory
     * @param router Router instance of the OpenIDM
     */
    public OpenIDMSessionFactory(ObjectSet router) {
        this.router = router;
    }

    public void setRouter(ObjectSet router) {
        this.router = router;
    }

    @Override
    public Class<?> getSessionType() {
        return OpenIDMSession.class;
    }

    @Override
    public Session openSession() {
        if (router == null) {
            router = new JsonResourceObjectSet(new HttpRemoteJsonResource(url, user, password));
        }
        return new OpenIDMSessionImpl(router);
    }
}
