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
 * Portions Copyrighted 2024 3A Systems LLC.
 */
package org.forgerock.openidm.felix.webconsole;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.Dictionary;

import org.apache.felix.webconsole.WebConsoleSecurityProvider;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.component.propertytypes.ServiceVendor;

/**
 * Creates a WebConsoleSecurityProvider service that the felix web console will use to delegate authentication attempts.
 */
@Component(
        name = WebConsoleSecurityProviderService.PID,
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
@ServiceVendor(ServerConstants.SERVER_VENDOR_NAME)
@ServiceDescription("OpenIDM Felix Web Console Security Provider")
public class WebConsoleSecurityProviderService implements WebConsoleSecurityProvider {

    public static final String PID = "org.forgerock.openidm.felix.webconsole";
    private static final String USER_NAME = "username";
    private static final String PASSWORD = "password";
    private static final String AUTHENTICATED = "authenticated";

    /** Enhanced configuration service. */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile EnhancedConfig enhancedConfig;

    @Reference
    private CryptoService cryptoService;

    private String userId;
    private JsonValue password;

    @Activate
    public void activate(ComponentContext context) {
        final Dictionary<String, Object> dict = context.getProperties();
        final String servicePid = dict==null?null:(String) dict.get(Constants.SERVICE_PID);

        final JsonValue config = enhancedConfig.getConfiguration(dict, servicePid, false);
        userId = config.get(USER_NAME).asString();
        password = config.get(PASSWORD);
    }

    // TODO Enhance this to use CAF?
    @Override
    public Object authenticate(final String username, final String password) {
        if (username == null || password == null || this.userId == null || this.password == null) {
            return null;
        } else if (username.equals(userId)
                && password.equals(cryptoService.decryptIfNecessary(this.password).asString())) {
            return json(object(field(AUTHENTICATED, true))).asMap();
        } else {
            return null;
        }
    }

    @Override
    public boolean authorize(final Object user, final String role) {
        // accept all roles
        return true;
    }

	public void bindCryptoService(CryptoService cryptoService2) {
		cryptoService=cryptoService2;
		
	}

	public void bindEnhancedConfig(JSONEnhancedConfig jsonEnhancedConfig) {
		enhancedConfig=jsonEnhancedConfig;
		
	}
}
