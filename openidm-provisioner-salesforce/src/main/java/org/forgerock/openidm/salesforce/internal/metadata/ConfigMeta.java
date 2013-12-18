/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
 */

package org.forgerock.openidm.salesforce.internal.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.metadata.MetaDataProvider;
import org.forgerock.openidm.metadata.MetaDataProviderCallback;
import org.forgerock.openidm.salesforce.internal.SalesforceRequestHandler;

/**
 * Meta data provider to describe configuration requirements of this bundle
 *
 */
public class ConfigMeta implements MetaDataProvider {

    final List<JsonPointer> propertiesToEncrypt;

    public ConfigMeta() {
        List<JsonPointer> pointers = new ArrayList<JsonPointer>();
        pointers.add(new JsonPointer("configurationProperties/refreshToken"));
        pointers.add(new JsonPointer("configurationProperties/clientSecret"));
        propertiesToEncrypt = Collections.unmodifiableList(pointers);
    }

    /**
     * @inheritDoc
     */
    public List<JsonPointer> getPropertiesToEncrypt(String pidOrFactory, String instanceAlias,
            JsonValue config) {
        if (SalesforceRequestHandler.PID.equals(pidOrFactory)) {
            return propertiesToEncrypt;
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setCallback(MetaDataProviderCallback callback) {
    }
}
