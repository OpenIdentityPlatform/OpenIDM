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
 * Copyright 2011-2016 ForgeRock AS.
 */

package org.forgerock.openidm.config.enhanced;

import java.util.Dictionary;

import org.forgerock.json.JsonValue;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

public interface EnhancedConfig {

    /**
     * Gets the extended Configuration which allows for nested Maps and Lists
     * 
     * @param compContext
     *            The component context with the configuration to retrieve
     * @return the enhanced configuration with nested maps, list allowed
     * @throws InvalidException
     *             if the configuration is invalid
     * @throws InternalErrorException
     *             if a failure occurred in retrieving the configuration
     */
    JsonValue getConfigurationAsJson(ComponentContext compContext) throws InvalidException, InternalErrorException;

    /**
     * Gets the extended Configuration which allows for nested Maps and Lists
     * 
     * @param dict
     *            The standard OSGi configuration properties dictionary
     * @param context
     *            The OSGi bundle context, used to detect other services such as
     *            for decryption
     * @param servicePid
     *            the service pid this configuration is for
     * @return the enhanced configuration (with nested maps, list allowed),
     *         empty json if no configuration properties exist.
     * @throws InvalidException
     */
    JsonValue getConfiguration(Dictionary<String, Object> dict, BundleContext context, String servicePid)
            throws InvalidException, InternalErrorException;

    /**
     * Gets the extended Configuration which allows for nested Maps and Lists
     *
     * @param dict
     *            The standard OSGi configuration properties dictionary
     * @param servicePid
     *            the service pid this configuration is for
     * @param decrypt
     *            true if any encrypted values should be decrypted in the result
     * @return the enhanced configuration (with nested maps, list allowed),
     *         empty json if no configuration properties exist.
     * @throws InvalidException
     */
    JsonValue getConfiguration(Dictionary<String, Object> dict, String servicePid, boolean decrypt)
            throws InvalidException, InternalErrorException;

    /**
     * Gets the extended Configuration which allows for nested Maps and Lists.
     * The returned configuration will *not* be decrypted, nor have properties evaluated.
     *
     * @param dict The standard OSGi configuration properties dictionary
     * @param servicePid the service pid this configuration is for
     * @return the enhanced configuration (with nested maps, list allowed),
     * empty json if no configuration properties exist.
     * @throws InvalidException
     */
    JsonValue getRawConfiguration(Dictionary<String, Object> dict, String servicePid) throws InvalidException;

    /**
     * Gets the service factory pid from the given OSGi component context.
     *
     * @param compContext
     *            The component context with the configuration to retrieve
     * @return the service factory pid
     */
    String getConfigurationFactoryPid(ComponentContext compContext);

}
