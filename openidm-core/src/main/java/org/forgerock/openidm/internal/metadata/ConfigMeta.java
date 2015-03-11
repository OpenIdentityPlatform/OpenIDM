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

package org.forgerock.openidm.internal.metadata;

import java.util.Collections;
import java.util.List;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.managed.ManagedObjectService;
import org.forgerock.openidm.metadata.MetaDataProvider;
import org.forgerock.openidm.metadata.MetaDataProviderCallback;
import org.forgerock.openidm.metadata.NotConfiguration;
import org.forgerock.openidm.metadata.WaitForMetaData;
import org.forgerock.openidm.router.impl.JsonResourceRouterService;
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
        } else if (JsonResourceRouterService.PID.equalsIgnoreCase(pidOrFactory)) {
            logger.trace("Configuration advised {}-{}", pidOrFactory, instanceAlias);
            return Collections.emptyList();
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setCallback(MetaDataProviderCallback callback) {
        // This instance won't be updated
    }
}
