/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.workflow.activiti.metadata;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.workflow.activiti.impl.ActivitiServiceImpl;
import org.forgerock.openidm.metadata.MetaDataProvider;
import org.forgerock.openidm.metadata.MetaDataProviderCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Meta data provider to describe configuration
 * requirements of this bundle
 *
 * @author aegloff
 * @author ckienle
 */
public class ConfigMeta implements MetaDataProvider {
    final static Logger logger = LoggerFactory.getLogger(ConfigMeta.class);

    Map<String, List<JsonPointer>> propertiesToEncrypt;
    private MetaDataProviderCallback callback = null;

    public ConfigMeta() {
        propertiesToEncrypt = new HashMap<String, List<JsonPointer>>();
        List<JsonPointer> props = new ArrayList<JsonPointer>();
        props.add(new JsonPointer("engine/password"));
        props.add(new JsonPointer("mail/password"));
        propertiesToEncrypt.put(ActivitiServiceImpl.PID, props);
    }

    /**
     * @inheritDoc
     */
    public List<JsonPointer> getPropertiesToEncrypt(String pidOrFactory, String instanceAlias, JsonValue config) {
        if (propertiesToEncrypt.containsKey(pidOrFactory)) {
            return propertiesToEncrypt.get(pidOrFactory);
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setCallback(MetaDataProviderCallback callback) {
        this.callback = callback;
    }
}
