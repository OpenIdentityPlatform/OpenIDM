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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */
package org.forgerock.openidm.config.installer;

import java.io.File;
import java.util.Dictionary;

import org.forgerock.json.fluent.JsonValue;

import org.forgerock.openidm.config.crypto.ConfigCrypto;


/**
 * Info for Configuration whose installation is delayed
 * until for example required meta data is available
 */
public class DelayedConfig {
    public String pidOrFactory;
    public String factoryAlias;
    public File file;
    public Dictionary oldConfig;
    public Dictionary newConfig;
    public JsonValue parsedConfig;
    public JSONConfigInstaller configInstaller;
    public ConfigCrypto configCrypto;
}
