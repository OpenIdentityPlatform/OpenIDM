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
 *
 * $Id$
 */

package org.forgerock.openidm.web.osgi;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Sample Class Doc
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @see <a href="http://njbartlett.name/2011/03/07/embedding-osgi.html"></a>
 * @see <a href="http://heapdump.wordpress.com/2010/03/25/osgi-enabled-war"></a>
 */
public class StartupListener implements ServletContextListener {

    private FrameworkService service;

    public void contextInitialized(ServletContextEvent event) {
        this.service = new FrameworkService(event.getServletContext());
        this.service.start();
    }

    public void contextDestroyed(ServletContextEvent event) {
        this.service.stop();
    }
}
