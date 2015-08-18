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
 * Portions copyright 2011-2015 ForgeRock AS.
 */
package org.forgerock.openidm.internal.metadata;

import java.util.Collections;
import java.util.List;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.managed.ManagedObjectService;
import org.forgerock.openidm.metadata.MetaDataProvider;
import org.forgerock.openidm.metadata.MetaDataProviderCallback;
import org.forgerock.openidm.metadata.NotConfiguration;
import org.forgerock.openidm.metadata.WaitForMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A NAME does ...
 *
 */
public class ConfigMeta implements MetaDataProvider {

    /**
     * Setup logging for the {@link ConfigMeta}.
     */
    final static Logger logger = LoggerFactory.getLogger(ConfigMeta.class);

    /*
     * final static List<JsonPointer> properties;
     *
     * static { List<JsonPointer> p = new ArrayList<JsonPointer>(4); p.add(new
     * JsonPointer("/")); properties = Collections.unmodifiableList(p); }
     */

    /**
     * @inheritDoc
     */
    public List<JsonPointer> getPropertiesToEncrypt(String pidOrFactory, String instanceAlias,
            JsonValue config) throws WaitForMetaData, NotConfiguration {
        if (ManagedObjectService.PID.equalsIgnoreCase(pidOrFactory)) {
            logger.trace("Configuration advised {}-{}", pidOrFactory, instanceAlias);
            return Collections.emptyList();
        }
        return null;
    }

    /**
     * @inheritDoc
     */
//    @Override
    public void setCallback(MetaDataProviderCallback callback) {
        // This instance won't be updated
    }
}
