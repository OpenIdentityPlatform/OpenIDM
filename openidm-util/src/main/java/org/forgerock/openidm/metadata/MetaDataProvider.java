/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 ForgeRock AS. All Rights Reserved
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
package org.forgerock.openidm.metadata;

import java.util.List;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;

/**
 * Meta data provider interface to describe configuration requirements of a
 * bundle. Use a meta-data.json file to declare a meta data provider for a
 * bundle*
 *
 */
public interface MetaDataProvider {

    /**
     * Meta-data describing which configuration properties need to be encrypted
     * for a given configuration.
     *
     * @param pidOrFactory
     *            the PID of either the managed service; or for factory
     *            configuration the PID of the Managed Service Factory
     * @param instanceAlias
     *            null for plain managed service, or the subname (alias) for the
     *            managed factory configuration newBuilder
     * @param config
     *            the new configuration that is being set. May or may not
     *            already have encrypted values.
     *
     * @return a list of configuration properties (identified by JSON pointers)
     *         that need to be encrypted if this MetaDataProvider is responsible
     *         for this configuration. Empty list if none should be encrypted.
     *         Null if this provider is not responsible for this configuration.
     * @throws WaitForMetaData
     *             thrown if this provider knows that the given configuration
     *             has associated meta-data, but the meta-data is not yet
     *             available.
     * @throws NotConfiguration
     *             throws when the config parameter is not represent a real
     *             configuration.
     */
    List<JsonPointer> getPropertiesToEncrypt(String pidOrFactory, String instanceAlias,
            JsonValue config) throws WaitForMetaData, NotConfiguration;

    /**
     * Sets a callback to be used to refresh/update the configuration
     * requirements/properties.
     *
     * @param callback
     *            a MetaDataProviderCallback implementation
     */
    public void setCallback(MetaDataProviderCallback callback);
}
