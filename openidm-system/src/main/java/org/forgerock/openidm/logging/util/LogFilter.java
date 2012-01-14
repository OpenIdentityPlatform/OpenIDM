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
package org.forgerock.openidm.logging.util;

import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LogFilter implements Filter {

    @Override
    public boolean isLoggable(LogRecord rec) {
        // Filter out well known noise on the console
        
        // OrientDB RC6 noise
        if (rec.getLoggerName().startsWith("com.orientechnologies.orient.core.storage.impl.local.")) {
            if (rec.getMessage().endsWith("was not closed correctly last time. Checking segments...")
                    || rec.getMessage().endsWith("] OK")) {
                return false;
            }
        } else if (rec.getLoggerName().startsWith("org.forgerock.openidm.Framework")) {
            
            // Felix 4.0.2 shutdown has some noise in trying to unbind references when services are already gone
            if (rec.getMessage().endsWith("[org.forgerock.openidm.managed] Cannot create component instance due to failure to bind reference ref_ManagedObjectService_JsonResourceRouterService")
                    || rec.getMessage().endsWith("[org.forgerock.openidm.sync] Cannot create component instance due to failure to bind reference ref_SynchronizationService_JsonResourceRouterService")
                    || rec.getMessage().endsWith("[org.forgerock.openidm.sync] Component instance could not be created, activation failed")
                    || rec.getMessage().endsWith("[org.forgerock.openidm.authentication] Cannot create component instance due to failure to bind reference httpService")
                    || rec.getMessage().endsWith("[org.forgerock.openidm.authentication] Component instance could not be created, activation failed")
                    || rec.getMessage().endsWith("[org.forgerock.openidm.managed] Component instance could not be created, activation failed")
                    || (rec.getMessage().contains("org.ops4j.pax.web.pax-web-jetty-bundle [") && rec.getMessage().endsWith("] FrameworkEvent ERROR"))) {
                return false;
            }
        }
        return true;
    }
}
