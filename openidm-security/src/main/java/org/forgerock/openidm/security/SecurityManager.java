/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013-2015 ForgeRock AS.
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

package org.forgerock.openidm.security;

import static org.forgerock.json.resource.Router.uriTemplate;

import java.security.Security;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.util.ClusterUtil;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.factory.CryptoUpdateService;
import org.forgerock.openidm.jetty.Config;
import org.forgerock.openidm.jetty.Param;
import org.forgerock.openidm.repo.RepositoryService;
import org.forgerock.openidm.security.impl.CertificateResourceProvider;
import org.forgerock.openidm.security.impl.EntryResourceProvider;
import org.forgerock.openidm.security.impl.MappedAliasKeyManager;
import org.forgerock.openidm.security.impl.JcaKeyStoreHandler;
import org.forgerock.openidm.security.impl.KeystoreResourceProvider;
import org.forgerock.openidm.security.impl.PrivateKeyResourceProvider;
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
public class SecurityManager implements RequestHandler, KeyStoreManager {

    public static final String PID = "org.forgerock.openidm.security";

    /**
     * Setup logging for the {@link SecurityManager}.
     */
    private final static Logger logger = LoggerFactory.getLogger(SecurityManager.class);
    
    @Reference
    protected RepositoryService repoService;

    @Reference
    private CryptoUpdateService cryptoUpdateService;

    private final Router router = new Router();
    
    private KeyStoreHandler trustStoreHandler = null;
    private KeyStoreHandler keyStoreHandler = null;
    
    private final String keyStoreType;
    private final String keyStoreLocation;
    private final String keyStorePassword; 

    private final String trustStoreType;
    private final String trustStoreLocation;
    private final String trustStorePassword;
    
    private final String keyStoreHostAliases;
    
    private static final String HOST_ALIAS_MAPPING_REGEX = ", *(?![^\\[\\]]*\\])";
        
    public SecurityManager() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        
        this.keyStoreHostAliases = Param.getProperty("openidm.ssl.host.aliases");
        this.trustStorePassword = Param.getTruststorePassword(false);
        this.trustStoreLocation = Param.getTruststoreLocation();
        this.trustStoreType = Param.getTruststoreType();
        this.keyStorePassword = Param.getKeystorePassword(false);
        this.keyStoreLocation = Param.getKeystoreLocation();
        this.keyStoreType = Param.getKeystoreType();

        // Set System properties
        if (System.getProperty("javax.net.ssl.keyStore") == null) {
            System.setProperty("javax.net.ssl.keyStore", keyStoreLocation);
            System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword);
            System.setProperty("javax.net.ssl.keyStoreType", keyStoreType);
        }
        if (System.getProperty("javax.net.ssl.trustStore") == null) {
            System.setProperty("javax.net.ssl.trustStore", trustStoreLocation);
            System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
            System.setProperty("javax.net.ssl.trustStoreType", trustStoreType);
        }
        
        this.trustStoreHandler = new JcaKeyStoreHandler(trustStoreType, trustStoreLocation, trustStorePassword);
        this.keyStoreHandler = new JcaKeyStoreHandler(keyStoreType, keyStoreLocation, keyStorePassword);
    }

    @Activate
    void activate(ComponentContext compContext) throws Exception {
        logger.debug("Activating Security Management Service {}", compContext);               
        
        KeystoreResourceProvider keystoreProvider =
                new KeystoreResourceProvider("keystore", keyStoreHandler, this, repoService);
        EntryResourceProvider keystoreCertProvider =
                new CertificateResourceProvider("keystore", keyStoreHandler, this, repoService);
        EntryResourceProvider privateKeyProvider =
                new PrivateKeyResourceProvider("keystore", keyStoreHandler, this, repoService);

        router.addRoute(uriTemplate("/keystore"), keystoreProvider);
        router.addRoute(uriTemplate("/keystore/cert"), keystoreCertProvider);
        router.addRoute(uriTemplate("/keystore/privatekey"), privateKeyProvider);

        KeystoreResourceProvider truststoreProvider =
                new KeystoreResourceProvider("truststore", trustStoreHandler, this, repoService);
        EntryResourceProvider truststoreCertProvider =
                new CertificateResourceProvider("truststore", trustStoreHandler, this, repoService);

        router.addRoute(uriTemplate("/truststore"), truststoreProvider);
        router.addRoute(uriTemplate("/truststore/cert"), truststoreCertProvider);
        
        String instanceType =
                IdentityServer.getInstance().getProperty("openidm.instance.type", ClusterUtil.TYPE_STANDALONE);
        
        String propValue = Param.getProperty("openidm.https.keystore.cert.alias");
        String privateKeyAlias = (propValue == null) ? "openidm-localhost" : propValue;

        try {
            if (instanceType.equals(ClusterUtil.TYPE_CLUSTERED_ADDITIONAL)) {
                // Load keystore and truststore from the repository
                keystoreProvider.loadStoreFromRepo();
                truststoreProvider.loadStoreFromRepo();

                // Reload the SSL context
                reload();
                // Update CryptoService
                cryptoUpdateService.updateKeySelector(keyStoreHandler.getStore(), keyStorePassword);
            } else {
                // Check if the default alias exists in keystore and truststore
                final boolean defaultPrivateKeyEntryExists = privateKeyProvider.hasEntry(privateKeyAlias);
                final boolean defaultTruststoreEntryExists = truststoreCertProvider.hasEntry(privateKeyAlias);
                if (!defaultPrivateKeyEntryExists && !defaultTruststoreEntryExists) {
                    // dafault keystore/truststore entries do not exist
                    // Create the default private key
                    createDefaultKeystoreAndTruststoreEntries(privateKeyAlias, privateKeyProvider, keystoreCertProvider,
                            truststoreCertProvider);

                    // Reload the SSL context
                    reload();

                    Config.updateConfig(null);
                } else if (!defaultPrivateKeyEntryExists) {
                    // no default keystore entry, but truststore has default entry
                    // this should only happen if the enduser is manually editing the keystore/truststore
                    logger.error("Keystore and truststore out of sync. The keystore doesn't contain the default "
                            + "entry, but the truststore does.");
                    throw new InternalServerErrorException("Keystore and truststore out of sync. The keystore "
                            + "doesn't contain the default entry, but the truststore does.");

                } else if (!defaultTruststoreEntryExists) {
                    // default keystore entry exists, but truststore default entry does not exist
                    // this should only happen if the enduser is manually editing the keystore/truststore
                    logger.error("Keystore and truststore out of sync. The keystore contains the default entry, but "
                            + "the truststore doesn't");
                    throw new InternalServerErrorException("Keystore and truststore out of sync. The keystore "
                            + "contains the default entry, but the truststore doesn't");
                } else {
                    // the default entry exists in both the truststore and keystore
                    // do nothing
                }

                // If this is the first/primary node in a cluster, then save the keystore and truststore to the repository
                if (instanceType.equals(ClusterUtil.TYPE_CLUSTERED_FIRST)) {
                    keystoreProvider.saveStoreToRepo();
                    truststoreProvider.saveStoreToRepo();
                }
            }
        } catch (Exception e) {
            logger.warn("Error initializing keys", e);
        }
    }

    @Deactivate
    void deactivate(ComponentContext compContext) {
        logger.debug("Deactivating Security Management Service {}", compContext);
        router.removeAllRoutes();
    }
    
    // ----- Implementation of KeyStoreManager interface
    
    @Override
    public void reload() throws Exception {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStoreHandler.getStore());
        TrustManager [] trustManagers = tmf.getTrustManagers();

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStoreHandler.getStore(), keyStoreHandler.getPassword().toCharArray());
        KeyManager [] keyManagers = kmf.getKeyManagers();
        
        // Override the default X509KeyManager with our own MappedAliasKeyManager.
        // This allows for mapping hosts to specific key aliases within the keystore.
        for (int i = 0; i < keyManagers.length; i++) {
            if (keyManagers[i] instanceof X509KeyManager) {
                keyManagers[i] = new MappedAliasKeyManager( (X509KeyManager)keyManagers[i], 
                        getHostAliasMappings(keyStoreHostAliases)
                );
            }
        }
        
        SSLContext context = SSLContext.getInstance("SSL");
        context.init(keyManagers, trustManagers, null);
        SSLContext.setDefault(context);
    }

    /**
     * Parses a string of comma separated key-value pairs into a map.
     * For example: "localhost=my-key-alias, service.forgerock.com=fr-client"
     * 
     * @param mappings The comma-separated string of key-value pairs to parse
     * @return A map containing the key value pairs
     */
    private Map<String, String> getHostAliasMappings(String mappings) {
        Map<String, String> map = new HashMap<String, String>();
        if (mappings != null) {
            for (String pair : mappings.split(HOST_ALIAS_MAPPING_REGEX)) {
                String[] parts = pair.split("=");
                if (parts.length == 2) {
                    map.put(parts[0].toUpperCase(), parts[1]);
                } else if (parts.length == 1) {
                    map.put(parts[0].toUpperCase(), null);
                }
            }
        }
        return map;
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

    private void createDefaultKeystoreAndTruststoreEntries(final String alias,
            final EntryResourceProvider privateKeyProvider,
            final EntryResourceProvider keystoreCertProvider,
            final EntryResourceProvider truststoreCertProvider)
    throws Exception {

        //create the keystore default entry
        privateKeyProvider.createDefaultEntry(alias);

        //get the keystore default entry cert
        final ReadRequest readRequest = Requests.newReadRequest("/keystore/cert");
        Promise<ResourceResponse, ResourceException> result =
                keystoreCertProvider.readInstance(new RootContext(), alias, readRequest);

        //add the keystore default entry cert to the truststore
        final CreateRequest createRequest = Requests.newCreateRequest("/truststore/cert", alias,
                result.getOrThrow().getContent());
        result = truststoreCertProvider.createInstance(new RootContext(), createRequest);
        result.getOrThrow();
    }
}
