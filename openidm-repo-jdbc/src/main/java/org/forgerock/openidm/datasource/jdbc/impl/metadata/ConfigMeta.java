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
 * Copyright 2011-2016 ForgeRock AS.
 */
package org.forgerock.openidm.datasource.jdbc.impl.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.core.PropertyUtil;
import org.forgerock.openidm.datasource.jdbc.impl.JDBCDataSourceService;
import org.forgerock.openidm.metadata.MetaDataProvider;
import org.forgerock.openidm.metadata.MetaDataProviderCallback;

/**
 * Meta data provider to describe configuration requirements of this bundle
 */
public class ConfigMeta implements MetaDataProvider {

    private static final JsonPointer PASSWORD_PTR = new JsonPointer("/password");
    private final Map<String, List<JsonPointer>> propertiesToEncrypt;

    public ConfigMeta() {
        propertiesToEncrypt = new HashMap<>();
        List<JsonPointer> props = new ArrayList<>();
        props.add(PASSWORD_PTR);
        propertiesToEncrypt.put(JDBCDataSourceService.PID, props);
    }

    @Override
    public List<JsonPointer> getPropertiesToEncrypt(String pidOrFactory, String instanceAlias, JsonValue config) {
        if (propertiesToEncrypt.containsKey(pidOrFactory)) {
            return FluentIterable.from(propertiesToEncrypt.get(pidOrFactory))
                    .filter(new HasNoPropertyFilter(config)).toList();
        }
        return null;
    }

    @Override
    public void setCallback(MetaDataProviderCallback callback) {
    }

    /**
     * Filters out config property keys that have a config value that does NOT contain a property.
     */
    private static class HasNoPropertyFilter implements Predicate<JsonPointer> {
        private final JsonValue config;

        public HasNoPropertyFilter(JsonValue config) {
            this.config = config;
        }

        @Override
        public boolean apply(JsonPointer configKey) {
            final JsonValue value = config.get(configKey);
            return value != null && !(value.isString() && PropertyUtil.containsProperty(value.asString()));
        }
    }
}
