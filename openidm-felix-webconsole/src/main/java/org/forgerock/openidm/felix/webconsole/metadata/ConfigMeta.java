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
package org.forgerock.openidm.felix.webconsole.metadata;

import java.util.LinkedList;
import java.util.List;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.felix.webconsole.WebConsoleSecurityProviderService;
import org.forgerock.openidm.metadata.MetaDataProvider;
import org.forgerock.openidm.metadata.MetaDataProviderCallback;
import org.forgerock.openidm.metadata.NotConfiguration;
import org.forgerock.openidm.metadata.WaitForMetaData;

/**
 * Implements a {@link MetaDataProvider} to encrypt sensitive fields in the {@link WebConsoleSecurityProviderService}
 * json configuration.
 */
public class ConfigMeta implements MetaDataProvider {
    private static final JsonPointer PASSWORD_PTR = new JsonPointer("password");
    private final List<JsonPointer> encryptedFields = new LinkedList<>();

    /**
     * Constructs a {@link ConfigMeta} and adds the sensitive field {@link JsonPointer JsonPointers} to the list of
     * encrypted fields.
     */
    public ConfigMeta() {
        encryptedFields.add(PASSWORD_PTR);
    }

    @Override
    public List<JsonPointer> getPropertiesToEncrypt(String pidOrFactory, String instanceAlias, JsonValue config)
            throws WaitForMetaData, NotConfiguration {
        if (!WebConsoleSecurityProviderService.PID.equalsIgnoreCase(pidOrFactory)) {
            return null;
        }
        return encryptedFields;
    }

    @Override
    public void setCallback(MetaDataProviderCallback callback) {
        // callback not used
    }
}
