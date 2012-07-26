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

import org.forgerock.openidm.objset.ObjectSet;

/**
 * Custom Session providing access to OpenIDM functions from Activiti
 * @author orsolyamebold
 */
public class OpenIDMSessionImpl implements OpenIDMSession {

    private ObjectSet router;

    public OpenIDMSessionImpl() {
    }

    public OpenIDMSessionImpl(ObjectSet router) {
        this.router = router;
    }

    @Override
    public ObjectSet getOpenIDM() {
        return router;
    }

    @Override
    public void setOpenIDM(ObjectSet router) {
        this.router = router;
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
}
