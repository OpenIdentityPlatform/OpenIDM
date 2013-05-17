package org.forgerock.jaspi.config;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.forgerock.jaspi.container.callback.CallbackHandlerImpl;
import org.forgerock.jaspi.container.config.AuthConfigFactoryImpl;
import org.forgerock.jaspi.container.config.AuthConfigProviderImpl;
import org.forgerock.jaspi.container.config.ServerAuthConfigImpl;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.config.AuthConfigFactory;
import java.util.Map;

/**
 * Configures the authentication chains based on configuration obtained through OSGi.
 *
 * Example:
 *
 * <pre>
 *     <code>
 * {
 *     "server-auth-config" : {
 *         "JWT-IWA" : {
 *             "session-module" : {
 *                 "classname" : "org.forgerock.jaspi.container.modules.JWTModule"
 *                 "some-setting" : "some-value"
 *             }
 *             "auth-module" : {
 *                 "classname" : "org.forgerock.jaspi.container.modules.IWAADModule"
 *                 "some-setting" : "some-value"
 *             }
 *         }
 *     }
 * }
 *     </code>
 * </pre>
 *
 */
@Component(name = OSGiAuthnFilterBuilder.PID, immediate = true, policy = ConfigurationPolicy.REQUIRE)
public class OSGiAuthnFilterBuilder {

    public static final String PID = "org.forgerock.openidm.authnfilter";

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Activate
    protected void activate(ComponentContext context) {

        EnhancedConfig config = JSONEnhancedConfig.newInstance();
        JsonValue jsonConfig = config.getConfigurationAsJson(context);

        // Expecting a JSON structured like this
        /*
        {
            "server-auth-config" : {
                "JWT-IWA" : {
                    "session-module" : {
                        "classname" : "org.forgerock.jaspi.container.modules.JWTModule"
                        "some-setting" : "some-value"
                    }
                    "auth-module" : {
                        "classname" : "org.forgerock.jaspi.container.modules.IWAADModule"
                        "some-setting" : "some-value"
                    }
                }
            }
        }
         */

        if (jsonConfig == null ) {
            // Something strange going on, no configurations found
            logger.warn("Could not find any configurations for the AuthnFilter, filter will not function");
            return;
        }

        CallbackHandler handler = new CallbackHandlerImpl();
        ServerAuthConfigImpl sac = new ServerAuthConfigImpl(null, null, handler);

        // For each ServerAuthConfig
        for (String s : jsonConfig.get("server-auth-config").required().keys()) {

            Map<String, Object> contextProperties = jsonConfig.get("server-auth-config").required().get(s).asMap();
            sac.registerAuthContext(s, contextProperties);
        }

        try {
            // Now assemble the factory-provider-config-context-module structure
            AuthConfigFactory factory = new AuthConfigFactoryImpl();
            AuthConfigProviderImpl provider = new AuthConfigProviderImpl(null, null);
            provider.setServerAuthConfig(sac);
            factory.registerConfigProvider(provider, null, null, null);

            // Set this chain to be default
            AuthConfigFactory.setFactory(factory);

        } catch (AuthException e) {
            // TODO log and fail
        }
    }
}