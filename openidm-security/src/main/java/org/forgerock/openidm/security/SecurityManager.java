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
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.openidm.security;

import static org.forgerock.json.resource.Router.uriTemplate;

import java.security.Security;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.keystore.KeyStoreManagementService;
import org.forgerock.openidm.keystore.KeyStoreService;
import org.forgerock.openidm.repo.RepositoryService;
import org.forgerock.openidm.security.impl.CertificateResourceProvider;
import org.forgerock.openidm.security.impl.EntryResourceProvider;
import org.forgerock.openidm.security.impl.KeystoreResourceProvider;
import org.forgerock.openidm.security.impl.PrivateKeyResourceProvider;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Security Manager Service which handles operations on the java security
 * keystore and truststore files.
 */
@Component(name = SecurityManager.PID, policy = ConfigurationPolicy.IGNORE, metatype = true, 
        description = "OpenIDM Security Management Service", immediate = true)
@Service
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Security Management Service"),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "/security/*") })
public class SecurityManager implements RequestHandler {

    static final String PID = "org.forgerock.openidm.security";

    /**
     * Setup logging for the {@link SecurityManager}.
     */
    private final static Logger logger = LoggerFactory.getLogger(SecurityManager.class);
    
    @Reference
    protected RepositoryService repoService;

    @Reference
    private CryptoService cryptoService;

    @Reference(target="(service.pid=org.forgerock.openidm.keystore)")
    private KeyStoreService keyStore;

    @Reference(target="(service.pid=org.forgerock.openidm.truststore)")
    private KeyStoreService trustStore;

    @Reference
    private KeyStoreManagementService keyStoreManager;

    private final Router router = new Router();
        
    public SecurityManager() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Activate
    void activate(ComponentContext compContext) throws Exception {
        logger.debug("Activating Security Management Service {}", compContext);
        final KeystoreResourceProvider keystoreProvider =
                new KeystoreResourceProvider("keystore", keyStore, repoService, cryptoService, keyStoreManager);
        final EntryResourceProvider keystoreCertProvider =
                new CertificateResourceProvider("keystore", keyStore, repoService, cryptoService, keyStoreManager);
        final EntryResourceProvider privateKeyProvider =
                new PrivateKeyResourceProvider("keystore", keyStore, repoService, cryptoService, keyStoreManager);

        router.addRoute(uriTemplate("/keystore"), keystoreProvider);
        router.addRoute(uriTemplate("/keystore/cert"), keystoreCertProvider);
        router.addRoute(uriTemplate("/keystore/privatekey"), privateKeyProvider);

        final KeystoreResourceProvider truststoreProvider =
                new KeystoreResourceProvider("truststore", trustStore, repoService, cryptoService, keyStoreManager);
        final EntryResourceProvider truststoreCertProvider =
                new CertificateResourceProvider("truststore", trustStore, repoService, cryptoService,
                        keyStoreManager);

        router.addRoute(uriTemplate("/truststore"), truststoreProvider);
        router.addRoute(uriTemplate("/truststore/cert"), truststoreCertProvider);
    }

    @Deactivate
    void deactivate(ComponentContext compContext) {
        logger.debug("Deactivating Security Management Service {}", compContext);
        router.removeAllRoutes();
    }

    // ----- Implementation of RequestHandler interface

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(final Context context, final ActionRequest request) {
        return router.handleAction(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(final Context context,
            final CreateRequest request) {
        return router.handleCreate(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(final Context context,
            final DeleteRequest request) {
        return router.handleDelete(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(final Context context, final PatchRequest request) {
        return router.handlePatch(context, request);
    }

    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(final Context context, final QueryRequest request,
            QueryResourceHandler queryResourceHandler) {
        return router.handleQuery(context, request, queryResourceHandler);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(final Context context, final ReadRequest request) {
        return router.handleRead(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(final Context context,
            final UpdateRequest request) {
        return router.handleUpdate(context, request);
    }
}
