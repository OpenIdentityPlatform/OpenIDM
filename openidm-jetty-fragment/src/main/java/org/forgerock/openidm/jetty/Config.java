/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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
package org.forgerock.openidm.jetty;

import java.util.Dictionary;
import java.util.Enumeration;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Provides methods for management of the Jetty configuration
 * 
 */
public class Config {

    /**
     * Updates the Jetty configuration with new values for the supplied Dictionary of properties. Any existing
     * properties not contained in the supplied Dictionary will remain the same.
     * 
     * @param propsToUpdate the properties to update
     * @throws Exception
     */
    public static void updateConfig(Dictionary<String, Object> propsToUpdate) throws Exception {
        BundleContext context = FrameworkUtil.getBundle(Param.class).getBundleContext();
        if (context != null) {
            ServiceReference<?> configAdminRef = context.getServiceReference(ConfigurationAdmin.class.getName());
            if (configAdminRef != null) {
                ConfigurationAdmin confAdmin = (ConfigurationAdmin) context.getService(configAdminRef);
                Configuration configuration = confAdmin.getConfiguration("org.ops4j.pax.web");

                if (propsToUpdate != null) {
                    Dictionary<String, Object> props = configuration.getProperties();
                    Enumeration<String> keys = propsToUpdate.keys();
                    while (keys.hasMoreElements()) {
                        String key = keys.nextElement();
                        props.put(key, propsToUpdate.get(key));
                    }
                    configuration.update(props);
                } else {
                    configuration.update();
                }
            }  
        }
    }
}
