/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.repo.orientdb.internal.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.openidm.metadata.MetaDataProvider;
import org.forgerock.openidm.metadata.MetaDataProviderCallback;
import org.forgerock.openidm.repo.orientdb.internal.OrientDBRepoService;


/**
 * Meta data provider to describe configuration
 * requirements of this bundle
 *
 * @author aegloff
 * @author ckienle
 */
public class ConfigMeta implements MetaDataProvider {

    private final List<JsonPointer> propertiesToEncrypt;

    public ConfigMeta() {
        List<JsonPointer> props = new ArrayList<JsonPointer>();
        props.add(new JsonPointer(OrientDBRepoService.CONFIG_PASSWORD));
        props.add(new JsonPointer("/embeddedServer/users/*/password"));
        propertiesToEncrypt = Collections.unmodifiableList(props);
    }

    /**
     * @inheritDoc
     */
    public List<JsonPointer> getPropertiesToEncrypt(String pidOrFactory, String instanceAlias,
            JsonValue config) {
        if (OrientDBRepoService.PID.equals(pidOrFactory)) {
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
