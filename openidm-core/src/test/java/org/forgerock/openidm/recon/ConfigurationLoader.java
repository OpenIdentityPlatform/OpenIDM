/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */
package org.forgerock.openidm.recon;

import java.util.Map;
import java.util.HashMap;

import java.io.IOException;

import org.codehaus.jackson.map.ObjectMapper;

/**
 * Helper utility to load configurations until the confugration
 * service is ready. This is also used in TestCases.
 */
public class ConfigurationLoader {

    /**
     * Given a file path to the configuration file, load and map it using
     * simple object mapping.
     *
     * @param sourceString to the configuration
     * @return mapped json configuration object
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> loadConfiguration(String sourceString) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> object = new HashMap<String, Object>();
        try {

            object = mapper.readValue(sourceString, Map.class);

        } catch (IOException io) {
            System.out.println(io.getLocalizedMessage());
        }

        return object;
    }
}
