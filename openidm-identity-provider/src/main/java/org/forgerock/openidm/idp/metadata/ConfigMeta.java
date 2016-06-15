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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openidm.idp.metadata;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.idp.config.ProviderConfig;
import org.forgerock.openidm.idp.impl.IdentityProviderConfig;
import org.forgerock.openidm.metadata.MetaDataProvider;
import org.forgerock.openidm.metadata.MetaDataProviderCallback;
import org.forgerock.openidm.metadata.WaitForMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Meta data provider to describe configuration
 * requirements of this bundle.
 */
public class ConfigMeta implements MetaDataProvider {
    final static Logger logger = LoggerFactory.getLogger(ConfigMeta.class);

    Map<String, List<JsonPointer>> propertiesToEncrypt;
    private MetaDataProviderCallback callback = null;

    public ConfigMeta() {
        propertiesToEncrypt = new HashMap<>();
        List<JsonPointer> props = new ArrayList<>();
        props.add(new JsonPointer(ProviderConfig.CLIENT_SECRET));
        propertiesToEncrypt.put(IdentityProviderConfig.PID, props);
    }

    @Override
    public List<JsonPointer> getPropertiesToEncrypt(String pidOrFactory, String instanceAlias, JsonValue config)
            throws WaitForMetaData {
        return propertiesToEncrypt.get(pidOrFactory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCallback(MetaDataProviderCallback callback) {
        this.callback = callback;
    }

}
