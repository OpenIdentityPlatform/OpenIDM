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

package org.forgerock.openidm.security;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.SecretKeyEntry;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResource;
import org.forgerock.json.resource.JsonResourceAccessor;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.json.resource.SimpleJsonResource;
import org.forgerock.openidm.cluster.ClusterUtils;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.factory.CryptoUpdateService;
import org.forgerock.openidm.jetty.Config;
import org.forgerock.openidm.jetty.Param;
import org.forgerock.openidm.objset.JsonResourceObjectSet;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.repo.RepositoryService;
import org.forgerock.openidm.util.DateUtil;
import org.forgerock.util.encode.Base64;
import org.joda.time.DateTime;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Security Manager Service which handles operations on the java security keystore and 
 * truststore files.
 * 
 * @author ckienle
 */
@Component(name = SecurityManager.PID, policy = ConfigurationPolicy.OPTIONAL,
description = "OpenIDM Security Management Service", immediate = true)
@Service
@Properties({
@Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
@Property(name = Constants.SERVICE_DESCRIPTION, value = "Security Management Service"),
@Property(name = ServerConstants.ROUTER_PREFIX, value = "security")
})
public class SecurityManager extends SimpleJsonResource {

    private final static Logger logger = LoggerFactory.getLogger(SecurityManager.class);
    
    public static final String PID = "org.forgerock.openidm.security";
    
    public static final String ACTION_GENERATE_CERT = "generateCert";
    public static final String ACTION_GENERATE_CSR = "generateCSR";

    public static final String TRUSTSTORE = "truststore";
    public static final String KEYSTORE = "keystore";

    public static final String DEFAULT_SIGNATURE_ALGORITHM = "SHA512WithRSAEncryption";
    public static final String DEFAULT_ALGORITHM = "RSA";
    public static final String DEFAULT_CERTIFICATE_TYPE = "X509";
    public static final int DEFAULT_KEY_SIZE = 2048;

    protected ObjectSet router;

    @Reference(
            name = "ref_SecurityManager_RepositoryService",
            bind = "bindRepo",
            unbind = "unbindRepo"
            )
    protected RepositoryService repo;
    protected void bindRepo(RepositoryService repo) {
        logger.debug("binding RepositoryService");
        this.router = new JsonResourceObjectSet(repo);
    }
    protected void unbindRepo(RepositoryService repo) {
        logger.debug("unbinding RepositoryService");
        this.router = null;
    }

    
    @Reference
    private CryptoUpdateService cryptoUpdateService;
    
    @Activate
    void activate(ComponentContext compContext) throws ParseException {
        logger.debug("Activating Security Management Service {}", compContext);
        // Add the Bouncy Castle provider
        Security.addProvider(new BouncyCastleProvider());
        
        // Set System properties
        String keystoreLocation = System.getProperty("javax.net.ssl.keyStore");
        String truststoreLocation = System.getProperty("javax.net.ssl.trustStore");
        String keystorePassword = Param.getKeystorePassword(false);
        if (keystoreLocation == null) {
            System.setProperty("javax.net.ssl.keyStore", Param.getKeystoreLocation());
            System.setProperty("javax.net.ssl.keyStorePassword", keystorePassword);
            System.setProperty("javax.net.ssl.keyStoreType", Param.getKeystoreType());
        }
        if (truststoreLocation == null) {
            System.setProperty("javax.net.ssl.trustStore", Param.getTruststoreLocation());
            System.setProperty("javax.net.ssl.trustStorePassword", Param.getTruststorePassword(false));
            System.setProperty("javax.net.ssl.trustStoreType", Param.getTruststoreType());
        }
        
        String instanceType = IdentityServer.getInstance().getProperty("openidm.instance.type", ClusterUtils.TYPE_STANDALONE);
        
        String privateKeyAlias = Param.getProperty("openidm.https.keystore.cert.alias");
        try {
            StoreWrapper storeWrapper = null;
            if (instanceType.equals(ClusterUtils.TYPE_CLUSTERED_ADDITIONAL)) {
                // Read keystore
                storeWrapper = readKeystore();
                // Update keystore
                storeWrapper.store();
                reloadStore(storeWrapper);
                // Update CryptoService
                cryptoUpdateService.updateKeySelector(storeWrapper.getStore(), keystorePassword);
            } else {
                storeWrapper = createStore("keystore");
                Key privateKey = storeWrapper.getStore().getKey(privateKeyAlias, keystorePassword.toCharArray());
                CertificateWrapper certWrapper = null;
                if (privateKey == null) {
                    String dn = "CN=local.openidm.forgerock.org, OU=None, O=OpenIDM Self-Signed Certificate L=None, C=None";
                    certWrapper = generateCertificate(dn, DEFAULT_ALGORITHM, DEFAULT_KEY_SIZE, 
                            DEFAULT_SIGNATURE_ALGORITHM, null, null);
                    storeWrapper.addPrivateKey(privateKeyAlias, certWrapper.getPrivateKey(), new Certificate[]{certWrapper.getCertificate()});
                    storeWrapper.store();
                    reloadStore(storeWrapper);
                    privateKey = certWrapper.getPrivateKey();
                }
                if (instanceType.equals(ClusterUtils.TYPE_CLUSTERED_FIRST)) {
                    storeKeystore(storeWrapper.getLocation());
                }
            }
        } catch (Exception e) {
            logger.warn("Error initializing keys", e);
        }
        
    }
    
    @Deactivate
    void deactivate(ComponentContext compContext) {
        logger.debug("Deactivating Security Management Service {}", compContext);
    }

    @Override
    public JsonValue create(JsonValue request) throws JsonResourceException {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            JsonValue id = request.get("id");
            if (!id.isNull()) {
                StoreWrapper store = getStoreFromResourceId(id.asString());
                String alias = getAlias(id.asString());
                if (alias != null) {
                    JsonValue value = request.get("value");
                    try {
                        storeCert(value, store, alias);
                        resultMap.put("_id", alias);
                    } catch (Exception e) {
                        throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, 
                                "Failed to store certificate: " + e.getMessage(), e);
                    }
                } else {
                    throw new JsonResourceException(JsonResourceException.BAD_REQUEST, 
                            "A valid resource ID must be specified in the request");
                }
            } else {
                throw new JsonResourceException(JsonResourceException.BAD_REQUEST, 
                        "A valid resource ID must be specified in the request");
            }
        } catch (JsonResourceException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR,  
                    "Failed to create certificate: " + e.getMessage(), e);
        }
        return new JsonValue(resultMap);
    }
    
    @Override
    public JsonValue update(JsonValue request) throws JsonResourceException {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            JsonValue id = request.get("id");
            if (!id.isNull()) {
                StoreWrapper store = getStoreFromResourceId(id.asString());
                String alias = getAlias(id.asString());
                if (!store.getStore().containsAlias(alias)) {
                    throw new JsonResourceException(JsonResourceException.NOT_FOUND);
                }
                if (alias != null) {
                    JsonValue value = request.get("value");
                    try {
                        storeCert(value, store, alias);
                        resultMap.put("_id", alias);
                    } catch (JsonResourceException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, 
                                "Failed to store certificate: " + e.getMessage(), e);
                    }
                } else {
                    throw new JsonResourceException(JsonResourceException.BAD_REQUEST, 
                            "A valid resource ID must be specified in the request");
                }
            } else {
                throw new JsonResourceException(JsonResourceException.BAD_REQUEST, 
                        "A valid resource ID must be specified in the request");
            }
        } catch (JsonResourceException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR,  
                    "Failed to update certificate: " + e.getMessage(), e);
        }
        return new JsonValue(resultMap);
    }
    
    private void storeCert(JsonValue value, StoreWrapper store, String alias) throws Exception{
        boolean fromCsr = value.get("fromCSR").defaultTo(false).asBoolean();
        String type = value.get("type").defaultTo(DEFAULT_CERTIFICATE_TYPE).asString();
        if (fromCsr) {
            PrivateKey privateKey = null;
            String privateKeyPem = value.get("privateKey").asString();
            if (privateKeyPem == null) {
                privateKey = getCsrKeyPair(alias).getPrivate();
            } else {
                privateKey = ((KeyPair)fromPem(privateKeyPem)).getPrivate();
            }
            if (privateKey == null) {
                throw new JsonResourceException(JsonResourceException.NOT_FOUND,
                        "No private key exists for the supplied signed certificate");
            }
            List<String> certStringChain = value.get("certs").required().asList(String.class);
            Certificate [] certChain = readCertificateChain(certStringChain, type);
            verify(privateKey, certChain[0]);
            store.addPrivateKey(alias, privateKey, certChain);
        } else {
            String certString = value.get("cert").required().asString();
            Certificate cert = readCertificate(certString, type);
            store.addCert(alias, cert);
        }
        store.store();
        reloadStore(store);
        Config.updateConfig(null);
    }
    
    @Override
    public JsonValue delete(JsonValue request) throws JsonResourceException {
        try {
            JsonValue id = request.get("id");
            if (!id.isNull()) {
                StoreWrapper store = getStoreFromResourceId(id.asString());
                String alias = getAlias(id.asString());
                if (alias != null) {
                    if (!store.getStore().containsAlias(alias)) {
                        throw new JsonResourceException(JsonResourceException.NOT_FOUND);
                    }
                    store.getStore().deleteEntry(alias);
                    store.store();
                    reloadStore(store);
                } else {
                    throw new JsonResourceException(JsonResourceException.BAD_REQUEST, 
                            "A valid alias must be specified");
                }
            } else {
                throw new JsonResourceException(JsonResourceException.BAD_REQUEST, 
                        "A valid resource ID must be specified in the request");
            }
        } catch (JsonResourceException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR,   
                    "Failed to delete certificate: " + e.getMessage(), e);
        }
        return new JsonValue(null);
    }
    
    @Override
    public JsonValue read(JsonValue request) throws JsonResourceException {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            JsonValue id = request.get("id");
            if (!id.isNull() && (TRUSTSTORE.equals(id.asString()) || KEYSTORE.equals(id.asString()))) {
                StoreWrapper store = createStore(id.asString());
                resultMap.put("name", id.asString());
                resultMap.put("type", store.getStore().getType());
                resultMap.put("provider", store.getStore().getProvider());
                resultMap.put("location", store.getLocation());
                Enumeration<String> aliases = store.getStore().aliases();
                List<String> aliasList = new ArrayList<String>();
                while (aliases.hasMoreElements()) {
                    aliasList.add(aliases.nextElement());
                }
                resultMap.put("aliases", aliasList);
            } else if (!id.isNull()) {
                StoreWrapper store = getStoreFromResourceId(id.asString());
                String alias = getAlias(id.asString());
                if (alias != null) {
                    if (!store.getStore().containsAlias(alias)) {
                        throw new JsonResourceException(JsonResourceException.NOT_FOUND,
                                "No alias " + alias + " exists in " + store.getType());
                    }
                    Certificate cert = store.getStore().getCertificate(alias);
                    if (cert == null) {
                        throw new JsonResourceException(JsonResourceException.NOT_FOUND, 
                                "No certificate exists for alias " + alias + " in " + store.getType());
                    }
                    resultMap.put("_id", alias);
                    resultMap.put("type", cert.getType());
                    resultMap.put("cert", getCertString(cert));
                    resultMap.put("publicKey", getKeyMap(cert.getPublicKey()));
                } else {
                    throw new JsonResourceException(JsonResourceException.BAD_REQUEST, 
                            "A valid alias must be specified");
                }
            } else {
                throw new JsonResourceException(JsonResourceException.BAD_REQUEST, 
                        "A valid resource ID must be specified in the request");
            }
        } catch (JsonResourceException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR,  
                    "Failed to read certificate: " + e.getMessage(), e);
        }
        return new JsonValue(resultMap);
    }
    
    @Override
    public JsonValue action(JsonValue request) throws JsonResourceException {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            JsonValue action = request.get("params").get("_action");
            JsonValue id = request.get("id");
            JsonValue value = request.get("value");
            if (id.isNull()) {
                throw new JsonResourceException(JsonResourceException.BAD_REQUEST, 
                        "A valid resource ID must be specified in the request");
            }
            if (action.isNull()) {
                throw new JsonResourceException(JsonResourceException.BAD_REQUEST, 
                        "A valid action must be specified in the request");
            }
            if (ACTION_GENERATE_CERT.equalsIgnoreCase(action.asString()) || 
                    ACTION_GENERATE_CSR.equalsIgnoreCase(action.asString())) {
                StoreWrapper store = getStoreFromResourceId(id.asString());
                String alias = getAlias(id.asString());
                if (alias == null) {
                    throw new JsonResourceException(JsonResourceException.BAD_REQUEST, 
                            "A valid resource ID must be specified in the request");
                }
                String algorithm = value.get("algorithm").defaultTo(DEFAULT_ALGORITHM).asString();
                String signatureAlgorithm = value.get("signatureAlgorithm").defaultTo(DEFAULT_SIGNATURE_ALGORITHM).asString();
                int keySize = value.get("keySize").defaultTo(DEFAULT_KEY_SIZE).asInteger();
                if (ACTION_GENERATE_CERT.equalsIgnoreCase(action.asString())) {
                    // Set parameters
                    String domainName = value.get("domainName").required().asString();
                    String dn = "CN=" + domainName + ", OU=None, O=None L=None, C=None";
                    String validFrom = value.get("validFrom").asString();
                    String validTo = value.get("validTo").asString();
                    // Generate the certificate
                    CertificateWrapper certWrapper = generateCertificate(dn, algorithm, keySize, signatureAlgorithm, validFrom, validTo);

                    // Add it to the store and reload
                    store.addCert(alias, certWrapper.getCertificate());
                    store.addPrivateKey(alias, certWrapper.getPrivateKey(), new Certificate[]{certWrapper.getCertificate()});
                    store.store();
                    reloadStore(store);

                    // Populate response
                    resultMap.put("_id", alias);
                    resultMap.put("type", certWrapper.getCertificate().getType());
                    resultMap.put("cert", getCertString(certWrapper.getCertificate()));
                    resultMap.put("publicKey", getKeyMap(certWrapper.getCertificate().getPublicKey()));
                } else if (ACTION_GENERATE_CSR.equalsIgnoreCase(action.asString())) {
                    CertificationRequestWrapper certRequest = generateCSR(alias, algorithm, signatureAlgorithm, keySize, value, store);
                    PublicKey publicKey = certRequest.getCertificationRequest().getPublicKey();
                    resultMap.put("_id", alias);
                    resultMap.put("csr", getCertString(certRequest.getCertificationRequest()));
                    resultMap.put("publicKey", getKeyMap(publicKey));
                    if (value.get("returnPrivateKey").defaultTo(false).asBoolean()) {
                        resultMap.put("privateKey", getKeyMap(certRequest.getPrivateKey()));
                    }
                } else {
                    throw new JsonResourceException(JsonResourceException.BAD_REQUEST, 
                            "Unsupported action " + action.asString());
                }
            } else {
                throw new JsonResourceException(JsonResourceException.BAD_REQUEST, 
                        "Unsupported action " + action.asString());
            }
        } catch (JsonResourceException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR,  
                    "Failed to execute action: " + e.getMessage(), e);
        }
        return new JsonValue(resultMap);
    }

    private StoreWrapper getStoreFromResourceId(String id) throws Exception, JsonResourceException {
        String name = getStoreName(id);
        if (name != null) {
            return createStore(name);
        } else {
            throw new JsonResourceException(JsonResourceException.BAD_REQUEST, 
                    "A valid resource ID must be specified in the request");
        }
    }
    
    private String getStoreName(String id) {
        if (id.equals(TRUSTSTORE) || id.equals(KEYSTORE)) {
            return id;
        } else if (id.startsWith("truststore/") || id.startsWith("keystore/")) {
            return id.substring(0, id.indexOf("/"));
        }
        return null;
    }
   
    private String getAlias(String id) {
        try {
            int i = id.indexOf("/");
            return id.substring(i + 1);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> getKeyMap(Key key) throws Exception {
        Map<String, Object> keyMap = new HashMap<String, Object>();
        keyMap.put("algorithm", key.getAlgorithm());
        keyMap.put("format", key.getFormat());
        keyMap.put("encoded", toPem(key));
        return keyMap;
    }

    private String getCertString(Object object) throws Exception {
        PEMWriter pemWriter = null;
        StringWriter sw = null;
        try {
            sw = new StringWriter();
            pemWriter = new PEMWriter(sw);
            pemWriter.writeObject(object);
            pemWriter.flush();
        } finally {
            pemWriter.close();
        }
        return sw.getBuffer().toString();
    }

    /**
     * Stores a KeyPair (associated with a CSR request on the specified alias) in the repository.
     * 
     * @param alias the alias from the CSR
     * @param keyPair the KeyPair object
     * @throws JsonResourceException
     */
    private void storeCsrKeyPair(String alias, KeyPair keyPair) throws JsonResourceException {
        String id = "security/keys/" + alias;
        String encoded;
        try {
            encoded = toPem(keyPair);          
        } catch (Exception e) {
            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e);
        }
        JsonValue keyMap = new JsonValue(new HashMap<String, Object>());
        keyMap.put("encoded", encoded);
        storeInRepo(id, keyMap); 
    }
    
    /**
     * Reads the keystore from the repository
     * 
     * @return A StoreWrapper containing the keystore
     * @throws JsonResourceException
     */
    private StoreWrapper readKeystore() throws JsonResourceException {
        StoreWrapper storeWrapper = null;
        JsonValue keystoreValue = readFromRepo("security/keystore");
        String keystoreString = keystoreValue.get("keystoreString").asString();
        byte [] keystoreBytes = Base64.decode(keystoreString.getBytes());
        ByteArrayInputStream bais = new ByteArrayInputStream(keystoreBytes);
        try {
            storeWrapper = createKeyStore(bais);
        } catch (Exception e) {
            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, "Error creating keystore from store bytes");
        }
        return storeWrapper;
    }
    
    /**
     * Stores the keystore in the repository
     * 
     * @param keystoreLocation the local location of the keystore to store
     * @throws IOException
     * @throws JsonResourceException
     */
    private void storeKeystore(String keystoreLocation) throws IOException, JsonResourceException {
        byte [] keystoreBytes = null;
        FileInputStream fin = null;
        File file = new File(keystoreLocation);

        try {
            fin = new FileInputStream(file);
            keystoreBytes = new byte[(int) file.length()];
            fin.read(keystoreBytes);
        } finally {
            fin.close();
        }
        
        String keystoreString = new String(Base64.encode(keystoreBytes));
        String id = "security/keystore";
        JsonValue value = new JsonValue(new HashMap<String, Object>());
        value.add("keystoreString", keystoreString);
        storeInRepo(id, value);
    }
    
    /**
     * Reads an object from the repository
     * @param id the object's id
     * @return the object
     * @throws JsonResourceException
     */
    private JsonValue readFromRepo(String id) throws JsonResourceException {
        if (router == null) {
            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, "Repo router is null");
        }
        JsonValue keyMap = new JsonValue(router.read(id));
        return keyMap;
    }
    
    /**
     * Stores an object in the repository
     * @param id the object's id
     * @param value the value of the object to store
     * @throws JsonResourceException
     */
    private void storeInRepo(String id, JsonValue value) throws JsonResourceException {
        if (router == null) {
            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, "Repo router is null");
        }
        JsonValue oldValue = null;
        try {
            oldValue = new JsonValue(router.read(id));
        } catch (NotFoundException e) {
            logger.debug("creating object " + id);
            router.create(id, value.asMap());
            return;
        }
        router.update(id, oldValue.get("_rev").asString(), value.asMap());
    }
    
    /**
     * Returns a stored KeyPair (associated with a CSR request on the specified alias) from the repository.
     * 
     * @param alias the alias from the CSR
     * @return the KeyPair
     * @throws JsonResourceException
     */
    private KeyPair getCsrKeyPair(String alias) throws JsonResourceException {
        if (router == null) {
            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, "Repo router is null");
        }
        String id = "security/keys/" + alias;
        JsonValue keyMap = new JsonValue(router.read(id));
        if (keyMap.isNull()) {
            throw new JsonResourceException(JsonResourceException.NOT_FOUND, "Cannot find stored key for alias " + alias);
        }
        try {
            JsonValue key = keyMap.get("encoded");
            String pemString = key.asString();
            return fromPem(pemString);
        } catch (Exception e) {
            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, e);
        }
    }
    
    /**
     * Returns a PEM String representation of a object.
     * 
     * @param object the object
     * @return the PEM String representation
     * @throws Exception
     */
    private String toPem(Object object) throws Exception {
        StringWriter sw = new StringWriter(); 
        PEMWriter pw = new PEMWriter(sw); 
        pw.writeObject(object); 
        pw.flush(); 
        return sw.toString();
    }
    
    /**
     * Returns an object from a PEM String representation
     * 
     * @param pem the PEM String representation
     * @return the object
     * @throws Exception
     */
    private <T> T fromPem(String pem) throws Exception {
        StringReader sr = new StringReader(pem);
        PEMReader pw = new PEMReader(sr);
        Object object = pw.readObject();
        return (T)object;
    }
    
    /**
     * Reads a certificate from a supplied string representation, and a supplied type.
     * 
     * @param certString A String representation of a certificate
     * @param type The type of certificate ("X509").
     * @return The certificate
     * @throws Exception
     */
    private Certificate readCertificate(String certString, String type) throws Exception {
        StringReader sr = new StringReader(certString);
        PEMReader pw = new PEMReader(sr);
        Object object = pw.readObject();
        if (object instanceof X509Certificate) {
            return (X509Certificate)object;
        } else {
            throw new JsonResourceException(JsonResourceException.BAD_REQUEST, "Unsupported certificate format");
        }
    }
    
    /**
     * Reads a certificate chain from a supplied string array representation, and a supplied type.
     * 
     * @param certStringChain an array of strings representing a certificate chain
     * @param type the type of certificates ("X509")
     * @return the certificate chain
     * @throws Exception
     */
    private Certificate[] readCertificateChain(List<String> certStringChain, String type) throws Exception {
        Certificate [] certChain = new Certificate[certStringChain.size()];
        for (int i=0; i<certChain.length; i++) {
            certChain[i] = readCertificate(certStringChain.get(i), type);
        }
        return certChain;
    }
    
    private CertificationRequestWrapper generateCSR(String alias, String algorithm, String signatureAlgorithm, int keySize, 
            JsonValue params, StoreWrapper store) throws Exception {

        // Construct the distinguished name
        StringBuilder sb = new StringBuilder(); 
        sb.append("CN=").append(params.get("CN").required().asString().replaceAll(",", "\\\\,"));
        sb.append(", OU=").append(params.get("OU").defaultTo("None").asString().replaceAll(",", "\\\\,"));
        sb.append(", O=").append(params.get("O").defaultTo("None").asString().replaceAll(",", "\\\\,"));
        sb.append(", L=").append(params.get("L").defaultTo("None").asString().replaceAll(",", "\\\\,"));
        sb.append(", C=").append(params.get("C").defaultTo("None").asString().replaceAll(",", "\\\\,"));

        // Create the principle subject name
        X509Principal subjectName = new X509Principal(sb.toString());
        
        //store.getStore().
        
        // Generate the key pair
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm);  
        keyPairGenerator.initialize(keySize); 
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();
        
        // Generate the certificate request
        PKCS10CertificationRequest cr = new PKCS10CertificationRequest(signatureAlgorithm, subjectName, publicKey, null, privateKey);
        
        // Store the private key to use when the signed cert is return and updated
        logger.debug("Storing private key with alias {}", alias);
        storeCsrKeyPair(alias, keyPair);
        
        return new CertificationRequestWrapper(cr, privateKey);
    }
    
    /**
     * Generates a self signed certificate from a given domain name.
     * 
     * @param domainName the domain name to use for the new certificate
     * @return  The generated certificate
     * @throws Exception
     */
    private CertificateWrapper generateCertificate(String dn, String algorithm, int keySize, 
            String signatureAlgorithm, String validFrom, String validTo) throws Exception {
        
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm);  
        keyPairGenerator.initialize(keySize);  
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        
        X509V3CertificateGenerator v3CertGen = new X509V3CertificateGenerator(); 
        
        Date notBefore = null;
        Date notAfter = null;
        if (validFrom == null) {
            notBefore = new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30);
        } else {
            DateTime notBeforeDateTime = DateUtil.getDateUtil().parseIfDate(validFrom);
            if (notBeforeDateTime == null) {
                throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR,  
                        "Invalid date format for 'validFrom' property");
            } else {
                notBefore = notBeforeDateTime.toDate();
            }
        }
        if (validTo == null) {
            notAfter = new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365*10));   
        } else {
            DateTime notAfterDateTime = DateUtil.getDateUtil().parseIfDate(validTo);
            if (notAfterDateTime == null) {
                throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR,  
                        "Invalid date format for 'validTo' property");
            } else {
                notAfter = notAfterDateTime.toDate();
            }
        }
        
        v3CertGen.setSerialNumber(BigInteger.valueOf(Math.abs(new SecureRandom().nextLong())));  
        v3CertGen.setIssuerDN(new X509Principal(dn));  
        v3CertGen.setNotBefore(notBefore);  
        v3CertGen.setNotAfter(notAfter);  
        v3CertGen.setSubjectDN(new X509Principal(dn)); 
        
        v3CertGen.setPublicKey(keyPair.getPublic());  
        v3CertGen.setSignatureAlgorithm(signatureAlgorithm); 
        
        PrivateKey privateKey = keyPair.getPrivate();
        X509Certificate cert = v3CertGen.generateX509Certificate(privateKey);
        
        return new CertificateWrapper(cert, privateKey);
    }
    
    /**
     * Creates a truststore or keystore object and returns an instance of StoreWrapper.
     * 
     * @param name the name of the store to create ("truststore" or "keystore"
     * @return an instance of StoreWrapper representing the created store. 
     * @throws Exception
     */
    public StoreWrapper createStore(String name) throws Exception {
        StoreWrapper sw = createDefaultStore(name);
        InputStream in = new FileInputStream(sw.getLocation());
        KeyStore store = createKeyStore(sw.getType(), sw.getPassword(), in);
        sw.setStore(store);
        return sw;
    }
    
    /**
     * Creates a keystore object from a given InputStream and returns an instance of StoreWrapper.
     * 
     * @param in an InputStream for the store
     * @return an instance of StoreWrapper representing the created store. 
     * @throws Exception
     */
    public StoreWrapper createKeyStore(InputStream in) throws Exception {
        StoreWrapper sw = createDefaultStore("keystore");
        KeyStore store = createKeyStore(sw.getType(), sw.getPassword(), in);
        sw.setStore(store);
        return sw;
    }
    
    private KeyStore createKeyStore(String type, String password, InputStream in) throws Exception{
        KeyStore store = null;
        try {
            store = KeyStore.getInstance(type);     
            store.load(in, password.toCharArray());
        } finally {
            in.close();
        }
        return store;
    }
    
    private StoreWrapper createDefaultStore(String name) {
        String type = null;
        String location = null;
        String password = null;
        if (name.equals(TRUSTSTORE)) {
            type = Param.getTruststoreType();
            location = Param.getTruststoreLocation();
            password = Param.getTruststorePassword(false);
        } else if (name.equals(KEYSTORE)) {
            type = Param.getKeystoreType();
            location = Param.getKeystoreLocation();
            password = Param.getKeystorePassword(false);
        } else {
            return null;
        }
        return new StoreWrapper(name, location, password, type);
    }
    
    private void reloadStore(StoreWrapper storeWrapper) throws Exception {
        if (storeWrapper != null && storeWrapper.getName().equals(TRUSTSTORE)) {
            reloadStores(storeWrapper, null);
        } else if (storeWrapper != null && storeWrapper.getName().equals(KEYSTORE)){
            reloadStores(null, storeWrapper);
        } else {
            reloadStores(null, null);
        }
    }
    
    private void reloadStores(StoreWrapper trustStoreWrapper, StoreWrapper keyStoreWrapper) throws Exception {
        TrustManager [] trustManagers = null;
        KeyManager [] keyManagers = null;
        
        if (trustStoreWrapper != null) {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStoreWrapper.getStore());
            trustManagers = tmf.getTrustManagers();
        }

        if (keyStoreWrapper != null) {
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStoreWrapper.getStore(), keyStoreWrapper.getPassword().toCharArray());
            keyManagers = kmf.getKeyManagers();
        }
        
        SSLContext context = SSLContext.getInstance("SSL");
        context.init(keyManagers, trustManagers, null);
        SSLContext.setDefault(context);
    }
    
    /**
     * Verifies that the supplied private key and signed certificate match by signing/verifying some test data.
     * 
     * @param privateKey A private key
     * @param publicKey A public key
     * @throws JsonResourceException if the verification fails, or an error is encountered.
     */
    private void verify(PrivateKey privateKey, Certificate cert) throws JsonResourceException {
        PublicKey publicKey = cert.getPublicKey();
        byte[] data = { 65, 66, 67, 68, 69, 70, 71, 72, 73, 74 };
        boolean verified;
        try {
            Signature signer = Signature.getInstance(privateKey.getAlgorithm());
            signer.initSign(privateKey);
            signer.update(data);
            byte[] signed = signer.sign();
            Signature verifier = Signature.getInstance(publicKey.getAlgorithm());
            verifier.initVerify(publicKey);
            verifier.update(data);
            verified = verifier.verify(signed);
        } catch (Exception e) {
            throw new JsonResourceException(JsonResourceException.INTERNAL_ERROR, 
                    "Error verifying private key and signed certificate", e);
        }
        if (!verified) {
            throw new JsonResourceException(JsonResourceException.BAD_REQUEST, 
                    "Private key does not match signed certificate");
        }
    }

    /**
     * A wrapper class for a truststore or keystore.  Contains methods for storing,
     * reloading, and adding certificates to the store.
     */
    private class StoreWrapper {
        
        private String name;
        private String location;
        private String password;
        private String type;
        private KeyStore store;
        
        public StoreWrapper(String name,
                String location,
                String password,
                String type) {
            this.name = name;
            this.location = location;
            this.password = password;
            this.type = type;
            this.store = null;
        }
        
        public StoreWrapper(String name,
                String location,
                String password,
                String type,
                KeyStore store) {
            this.name = name;
            this.location = location;
            this.password = password;
            this.type = type;
            this.store = store;
        }
        
        public String getName() {
            return name;
        }
        
        public KeyStore getStore() {
            return store;
        }
        
        public void setStore(KeyStore store) {
            this.store = store;
        }

        public String getLocation() {
            return location;
        }

        public String getPassword() {
            return password;
        }

        public String getType() {
            return type;
        }
        
        public void addCert(String alias, Certificate cert) throws Exception {
            store.setCertificateEntry(alias, cert);
        }
        
        public void addPrivateKey(String alias, PrivateKey privateKey, Certificate [] chain) throws Exception {
            store.setEntry(alias, new PrivateKeyEntry(privateKey, chain), new KeyStore.PasswordProtection(password.toCharArray()));
        }
        
        public void addSecretKey(String alias, SecretKey secretKey) throws Exception {
            store.setEntry(alias, new SecretKeyEntry(secretKey), new KeyStore.PasswordProtection(password.toCharArray()));
        }
   
        public void store() throws Exception {
            OutputStream out = new FileOutputStream(location);
            try {
                store.store(out, password.toCharArray());
            } finally {
                out.close();
            }
        }
    }
    
    private class CertificateWrapper {
        
        private Certificate certificate;
        private PrivateKey privateKey;
        
        public CertificateWrapper(Certificate certificate, PrivateKey privateKey) {
            this.certificate = certificate;
            this.privateKey = privateKey;
        }

        public Certificate getCertificate() {
            return certificate;
        }

        public PrivateKey getPrivateKey() {
            return privateKey;
        }
    }
    
    private class CertificationRequestWrapper {
        
        private PKCS10CertificationRequest certificationRequest;
        private PrivateKey privateKey;
        
        public CertificationRequestWrapper(PKCS10CertificationRequest certificationRequest, PrivateKey privateKey) {
            this.certificationRequest = certificationRequest;
            this.privateKey = privateKey;
        }

        public PKCS10CertificationRequest getCertificationRequest() {
            return certificationRequest;
        }

        public PrivateKey getPrivateKey() {
            return privateKey;
        }
    }
    
}
