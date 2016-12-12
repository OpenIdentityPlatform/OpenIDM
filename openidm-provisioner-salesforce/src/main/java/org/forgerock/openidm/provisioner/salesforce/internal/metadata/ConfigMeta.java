/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 ForgeRock AS. All Rights Reserved
 */

package org.forgerock.openidm.provisioner.salesforce.internal.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.metadata.MetaDataProvider;
import org.forgerock.openidm.metadata.MetaDataProviderCallback;
import org.forgerock.openidm.metadata.NotConfiguration;
import org.forgerock.openidm.metadata.WaitForMetaData;
import org.forgerock.openidm.provisioner.salesforce.internal.SalesforceProvisionerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Meta data provider to describe configuration requirements of this bundle
 *
 * @author Laszlo Hordos
 */
public class ConfigMeta implements MetaDataProvider {

    /**
     * Setup logging for the {@link ConfigMeta}.
     */
    final static Logger logger = LoggerFactory.getLogger(ConfigMeta.class);

    final static List<JsonPointer> properties;

    static {
        List<JsonPointer> p = new ArrayList<JsonPointer>(4);
        p.add(new JsonPointer("configurationProperties/refreshToken"));
        p.add(new JsonPointer("configurationProperties/clientSecret"));
        properties = Collections.unmodifiableList(p);
    }

    /**
     * @inheritDoc
     */
    public List<JsonPointer> getPropertiesToEncrypt(String pidOrFactory, String instanceAlias,
            JsonValue config) throws WaitForMetaData, NotConfiguration {
        if (SalesforceProvisionerService.PID.equalsIgnoreCase(pidOrFactory)) {
            logger.trace("Configuration advised {}-{}", pidOrFactory, instanceAlias);
            return properties;
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setCallback(MetaDataProviderCallback callback) {
        // This newBuilder won't be updated
    }
}
