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

package org.forgerock.openidm.config;

import java.util.Dictionary;
import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

public interface EnhancedConfig {

    /**
     * Gets the extended Configuration which allows for nested Maps and Lists
     * 
     * @param compContext The component context with the configuration to retrieve
     * @return the enhanced configuration (with nested maps, list allowed), 
     * empty map if no configuration properties exist.
     * @throws InvalidException if the configuration is invalid
     * @throws InternalServerErrorException if a failure occurred in retrieving the configuration
     */
    public Map<String, Object> getConfiguration(ComponentContext compContext)
            throws InvalidException, InternalErrorException;
    
    /**
     * Gets the extended Configuration which allows for nested Maps and Lists
     * 
     * @param compContext The component context with the configuration to retrieve
     * @return the enhanced configuration with nested maps, list allowed
     * @throws InvalidException if the configuration is invalid
     * @throws InternalServerErrorException if a failure occurred in retrieving the configuration
     */
    public JsonValue getConfigurationAsJson(ComponentContext compContext)
            throws InvalidException, InternalErrorException;

    /**
     * Gets the extended Configuration which allows for nested Maps and Lists
     * 
     * @param dict The standard OSGi configuration properties dictionary
     * @param context The OSGi bundle context, used to detect other services such as for decryption
     * @param the service pid this configuration is for
     * @return the enhanced configuration (with nested maps, list allowed), 
     * empty map if no configuration properties exist.
     * @throws InvalidException
     */
    public JsonValue getConfiguration(Dictionary<String, Object> dict, BundleContext context, String servicePid) 
            throws InvalidException, InternalErrorException;

}
