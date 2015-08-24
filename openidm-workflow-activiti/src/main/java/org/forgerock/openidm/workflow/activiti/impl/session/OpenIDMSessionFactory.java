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
 * Copyright 2012-2015 ForgeRock AS.
 */
package org.forgerock.openidm.workflow.activiti.impl.session;

import org.activiti.engine.impl.interceptor.Session;
import org.activiti.engine.impl.interceptor.SessionFactory;
import org.forgerock.script.ScriptRegistry;

/**
 * Custom SessionFactory for OpenIDM
 * Provides access to the OpenIDM functions
 * 
 */
public class OpenIDMSessionFactory implements SessionFactory {

    private ClassLoader classLoader;
    private ScriptRegistry scriptRegistry;
//    private String url;
//    private String user;
//    private String password;
//
    /**
     * Creates new OpenIDMSessionFactory
     */
    public OpenIDMSessionFactory() {
    }

//
//    /**
//     * Creates new OpenIDMSessionFactory
//     * @param url base URL of the OpenIDM REST interface
//     * @param user OpenIDM username
//     * @param password OpenIDM password
//     */
//    public OpenIDMSessionFactory(String url, String user, String password) {
//        this.url = url;
//        this.user = user;
//        this.password = password;
//    }
//
//    public void setPassword(String password) {
//        this.password = password;
//    }
//
//    public void setUrl(String url) {
//        this.url = url;
//    }
//
//    public void setUser(String user) {
//        this.user = user;
//    }
//    
    /**
     * Creates new OpenIDMSessionFactory
     * @param router Router newBuilder of the OpenIDM
     */
    public OpenIDMSessionFactory(ClassLoader classLoader, ScriptRegistry scriptRegistry) {
        this.classLoader = classLoader;
        this.scriptRegistry = scriptRegistry;
    }

    /**
     * Sets ClassLoader for OpenIDMSession.
     *
     * @param classLoader org.forgerock.openidm.router ClassLoader
     */
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public void setScriptRegistry(ScriptRegistry scriptRegistry) {
        this.scriptRegistry = scriptRegistry;
    }
    
    @Override
    public Class<?> getSessionType() {
        return OpenIDMSession.class;
    }

    @Override
    public Session openSession() {
        return new OpenIDMSessionImpl(classLoader, scriptRegistry);
    }
}
