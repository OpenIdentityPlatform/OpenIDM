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

package org.forgerock.openidm.config.crypto;

import java.util.Dictionary;

import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationPlugin;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public class CryptoConfigurationPlugin implements ConfigurationPlugin  {
    @Override
    public void modifyConfiguration(ServiceReference reference, Dictionary properties) {
        properties.put(JSONEnhancedConfig.class.getCanonicalName(), JSONEnhancedConfig
                .newInstance().getConfiguration((Dictionary<String, Object>) properties,
                        (String) reference.getProperty(Constants.SERVICE_PID), true));
    }
}
