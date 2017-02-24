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
 * Copyright 2017 ForgeRock AS.
 */
package org.forgerock.openidm.repo.opendj.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.metadata.MetaDataProvider;
import org.forgerock.openidm.metadata.MetaDataProviderCallback;
import org.forgerock.openidm.repo.opendj.impl.OpenDJRepoService;

/**
 * Meta data provider to describe configuration requirements of this bundle
 */
public class ConfigMeta implements MetaDataProvider {

    private final List<JsonPointer> propertiesToEncrypt;

    public ConfigMeta() {
        List<JsonPointer> props = new ArrayList<JsonPointer>();
        propertiesToEncrypt = Collections.unmodifiableList(props);
    }

    @Override
    public List<JsonPointer> getPropertiesToEncrypt(String pidOrFactory, String instanceAlias,
            JsonValue config) {
        if (OpenDJRepoService.PID.equals(pidOrFactory)) {
            return propertiesToEncrypt;
        }
        return null;
    }

    @Override
    public void setCallback(MetaDataProviderCallback callback) {
    }
}

