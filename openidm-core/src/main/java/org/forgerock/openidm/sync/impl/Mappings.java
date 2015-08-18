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
 * Portions copyright 2012-2015 ForgeRock AS.
 */
package org.forgerock.openidm.sync.impl;

import org.forgerock.json.JsonValue;

/**
 * Interface to manage and access mappings
 */
public interface Mappings {
    /**
     * Get a mapping by name
     * @param name the mapping name
     * @return the found mapping
     * @throws SynchronizationException if retrieving the mapping failed
     */
    ObjectMapping getMapping(String name) throws SynchronizationException;

    /**
     * Factory method to instantiate and register a new mapping,
     * given the supplied config
     * @param mappingConfig the configuration details of the mapping
     * @return the mapping instance
     */
    ObjectMapping createMapping(JsonValue mappingConfig);
}
