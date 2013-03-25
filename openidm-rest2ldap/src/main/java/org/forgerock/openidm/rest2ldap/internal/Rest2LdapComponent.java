/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.rest2ldap.internal;

import java.util.Dictionary;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Router;
import org.forgerock.opendj.rest2ldap.AuthorizationPolicy;
import org.forgerock.opendj.rest2ldap.Rest2LDAP;
import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Rest2LdapComponent does ...
 * 
 * @author Laszlo Hordos
 */
@Component(name = Rest2LdapComponent.PID, immediate = true, policy = ConfigurationPolicy.REQUIRE)
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Salesforce sample") })
public class Rest2LdapComponent {

    public static final String PID = "org.forgerock.openidm.rest2ldap";

    /**
     * Setup logging for the {@link Rest2LdapComponent}.
     */
    final static Logger logger = LoggerFactory.getLogger(Rest2LdapComponent.class);

    private ServiceRegistration<RequestHandler> serviceRegistration = null;

    @Activate
    protected void activate(ComponentContext context) {

        EnhancedConfig config = JSONEnhancedConfig.newInstance();

        String factoryPid = config.getConfigurationFactoryPid(context);
        if (StringUtils.isBlank(factoryPid)) {
            throw new IllegalArgumentException("Configuration must have property: "
                    + ServerConstants.CONFIG_FACTORY_PID);
        }

        JsonValue configuration = config.getConfigurationAsJson(context);
        configuration.put(ServerConstants.CONFIG_FACTORY_PID, factoryPid);

        // ---


        // Parse the authorization configuration.
        final AuthorizationPolicy authzPolicy =
                configuration.get("servlet").get("authorizationPolicy").required().asEnum(
                        AuthorizationPolicy.class);
        final String proxyAuthzTemplate =
                configuration.get("servlet").get("proxyAuthzIdTemplate").asString();

        // Parse the connection factory if present.
        final String ldapFactoryName =
                configuration.get("servlet").get("ldapConnectionFactory").asString();
        final org.forgerock.opendj.ldap.ConnectionFactory ldapFactory;
        if (ldapFactoryName != null) {
            ldapFactory =
                    Rest2LDAP.configureConnectionFactory(configuration.get(
                            "ldapConnectionFactories").required(), ldapFactoryName);
        } else {
            ldapFactory = null;
        }

        // Create the router.
        final Router router = new Router();
        final JsonValue mappings = configuration.get("servlet").get("mappings").required();
        for (final String mappingUrl : mappings.keys()) {
            final JsonValue mapping = mappings.get(mappingUrl);
            final CollectionResourceProvider provider =
                    Rest2LDAP.builder().ldapConnectionFactory(ldapFactory).authorizationPolicy(
                            authzPolicy).proxyAuthzIdTemplate(proxyAuthzTemplate)
                            .configureMapping(mapping).build();
            router.addRoute(mappingUrl, provider);
        }


        // ----

        Dictionary properties = context.getProperties();
        properties.put(ServerConstants.ROUTER_PREFIX, "/rest2ldap/" + factoryPid + "*");

        serviceRegistration =
                context.getBundleContext()
                        .registerService(RequestHandler.class, router, properties);

        logger.info("OpenIDM Rest2LDAP Service component is activated.");
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        if (null != serviceRegistration) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
        logger.info("OpenIDM Rest2LDAP Service component is deactivated.");
    }
}
