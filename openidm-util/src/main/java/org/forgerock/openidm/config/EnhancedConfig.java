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

import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;

import org.osgi.service.component.ComponentContext;

public interface EnhancedConfig {

    /**
     * Gets the extended Configuration which allows for nested Maps and Lists
     * 
     * @param compContext The component context with the configuration to retrieve
     * @return the enhanced configuration (with nested maps, list allowed), 
     * empty map if no configuration properties exist.
     * @throws InvalidException
     */
    public Map<String, Object> getConfiguration(ComponentContext compContext)
            throws InvalidException;
    
    /**
     * Gets the extended Configuration which allows for nested Maps and Lists
     * 
     * @param compContext The component context with the configuration to retrieve
     * @return the enhanced configuration with nested maps, list allowed
     * @throws InvalidException
     */
    public JsonNode getConfigurationAsJson(ComponentContext compContext)
            throws InvalidException;

    /**
     * Gets the extended Configuration which allows for nested Maps and Lists
     * 
     * @param dict The standard OSGi configuration properties dictionary
     * @return the enhanced configuration (with nested maps, list allowed), 
     * empty map if no configuration properties exist.
     * @throws InvalidException
     */
    public Map<String, Object> getConfiguration(Dictionary<String, Object> dict)
            throws InvalidException;

}