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

package org.forgerock.openidm.security.internal;

import java.security.Security;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.jetty.Param;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Security Manager Service which handles operations on the java security
 * keystore and truststore files.
 *
 * @author ckienle
 */
@Component(name = SecurityManager.PID, policy = ConfigurationPolicy.IGNORE,
        description = "OpenIDM Security Management Service", immediate = true)
@Service
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Security Management Service"),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "/security/*") })
public class SecurityManager implements RequestHandler {

    public static final String PID = "org.forgerock.openidm.security";

    /**
     * Setup logging for the {@link SecurityManager}.
     */
    private final static Logger logger = LoggerFactory.getLogger(SecurityManager.class);

    private final Router router = new Router();

    @Activate
    void activate(ComponentContext compContext) throws Exception {
        logger.debug("Activating Security Management Service {}", compContext);
        // Add the Bouncy Castle provider
        Security.addProvider(new BouncyCastleProvider());


        // Set System properties
        if (System.getProperty("javax.net.ssl.keyStore") == null) {
            System.setProperty("javax.net.ssl.keyStore", Param.getKeystoreLocation());
            System.setProperty("javax.net.ssl.keyStorePassword", Param.getKeystorePassword(false));
            System.setProperty("javax.net.ssl.keyStoreType", Param.getKeystoreType());
        }
        if (System.getProperty("javax.net.ssl.trustStore") == null) {
            System.setProperty("javax.net.ssl.trustStore", Param.getTruststoreLocation());
            System.setProperty("javax.net.ssl.trustStorePassword", Param.getTruststorePassword(false));
            System.setProperty("javax.net.ssl.trustStoreType", Param.getTruststoreType());
        }

        KeystoreResourceProvider provider =
                new KeystoreResourceProvider("keystore", new JcaKeyStoreHandler(Param.getKeystoreType(),
                        Param.getKeystoreLocation(), Param.getKeystorePassword(false)));

        router.addRoute("/keystore", provider);
        router.addRoute("/keystore/cert", provider.CERT);
        //router.addRoute("/keystore/key", provider.KEY);

        provider =
                new KeystoreResourceProvider("truststore", new JcaKeyStoreHandler(
                        Param.getTruststoreType(), Param.getTruststoreLocation(), Param
                                .getTruststorePassword(false)));

        router.addRoute("/truststore", provider);
        router.addRoute("/truststore/cert", provider.CERT);
    }

    @Deactivate
    void deactivate(ComponentContext compContext) {
        logger.debug("Deactivating Security Management Service {}", compContext);
        router.removeAllRoutes();
        //TODO: Release the KeyStore
    }

    // ----- Implementation of RequestHandler interface

    @Override
    public void handleAction(final ServerContext context, final ActionRequest request,
            final ResultHandler<JsonValue> handler) {
        router.handleAction(context, request, handler);
    }

    @Override
    public void handleCreate(final ServerContext context, final CreateRequest request,
            final ResultHandler<Resource> handler) {
        router.handleCreate(context, request, handler);
    }

    @Override
    public void handleDelete(final ServerContext context, final DeleteRequest request,
            final ResultHandler<Resource> handler) {
        router.handleDelete(context, request, handler);
    }

    @Override
    public void handlePatch(final ServerContext context, final PatchRequest request,
            final ResultHandler<Resource> handler) {
        router.handlePatch(context, request, handler);
    }

    @Override
    public void handleQuery(final ServerContext context, final QueryRequest request,
            final QueryResultHandler handler) {
        router.handleQuery(context, request, handler);
    }

    @Override
    public void handleRead(final ServerContext context, final ReadRequest request,
            final ResultHandler<Resource> handler) {
        router.handleRead(context, request, handler);
    }

    @Override
    public void handleUpdate(final ServerContext context, final UpdateRequest request,
            final ResultHandler<Resource> handler) {
        router.handleUpdate(context, request, handler);
    }
}
