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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */
package org.forgerock.openidm.jetty.jaas;

import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.plus.jaas.JAASLoginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Login service to give us additional control over how JAAS is handled in 
 * the context of an OSGi environment.
 * 
 * @author aegloff
 */
public class OsgiJAASLoginService extends JAASLoginService {
    
    final static Logger logger = LoggerFactory.getLogger(OsgiJAASLoginService.class);

    /** 
     * @InheritDoc
     */
    public UserIdentity login(final String username, final Object credentials) {
        logger.trace("Invoking login for {} with context cl {}", username,  Thread.currentThread().getContextClassLoader());
        return super.login(username, credentials);
    }
}

