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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.json.resource.SimpleJsonResource;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.jetty.Param;
import org.forgerock.openidm.util.DateUtil;
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

    @Activate
    void activate(ComponentContext compContext) throws ParseException {
        logger.debug("Activating Security Management Service {}", compContext);
        // Add the Bouncy Castle provider
        Security.addProvider(new BouncyCastleProvider());
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
                        String certString = value.get("cert").required().asString();
                        String type = value.get("type").defaultTo("X509").asString();
                        Certificate cert = generateCertificate(certString, type);
                        store.addCert(alias, cert);
                        store.store();
                        store.reload();
                        
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
                        String certString = value.get("cert").required().asString();
                        String type = value.get("type").defaultTo("X509").asString();
                        Certificate cert = generateCertificate(certString, type);
                        store.addCert(alias, cert);
                        store.store();
                        store.reload();
                        
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
                    "Failed to update certificate: " + e.getMessage(), e);
        }
        return new JsonValue(resultMap);
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
                    store.reload();
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
            if (!id.isNull() && ("truststore".equals(id.asString()) || "keystore".equals(id.asString()))) {
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
                        throw new JsonResourceException(JsonResourceException.NOT_FOUND);
                    }
                    Certificate cert = store.getStore().getCertificate(alias);
                    resultMap.put("_id", alias);
                    resultMap.put("type", cert.getType());
                    resultMap.put("publicKey", getPublicKeyResult(cert));
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
            if (!action.isNull() && "generateCert".equalsIgnoreCase(action.asString())) {
                JsonValue id = request.get("id");
                if (!id.isNull()) {
                    JsonValue value = request.get("value");
                    StoreWrapper store = getStoreFromResourceId(id.asString());
                    String alias = getAlias(id.asString());
                    if (alias == null) {
                        throw new JsonResourceException(JsonResourceException.BAD_REQUEST, 
                                "A valid resource ID must be specified in the request");
                    }
                    String domainName = value.get("domainName").required().asString();
                    String algorithm = value.get("algorithm").defaultTo("RSA").asString();
                    String signatureAlgorithm = value.get("signatureAlgorithm")
                            .defaultTo("MD5WithRSAEncryption").asString();
                    int keySize = value.get("keySize").defaultTo(1024).asInteger();
                    String validFrom = value.get("validFrom").asString();
                    String validTo = value.get("validTo").asString();
                    
                    // Generate the cert
                    Certificate cert = generateCertificate(domainName, algorithm, keySize, 
                            signatureAlgorithm, validFrom, validTo);
                    
                    // Add it to the store and reload
                    store.getStore().setCertificateEntry(alias, cert);
                    store.store();
                    store.reload();
                    
                    // Populate response
                    resultMap.put("_id", alias);
                    resultMap.put("type", cert.getType());
                    resultMap.put("publicKey", getPublicKeyResult(cert));
                } else {
                    throw new JsonResourceException(JsonResourceException.BAD_REQUEST, 
                            "A valid resource ID must be specified in the request");
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
        if (id.equals("truststore") || id.equals("keystore")) {
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
    
    private Map<String, Object> getPublicKeyResult(Certificate cert) throws Exception {
        Map<String, Object> publicKey = new HashMap<String, Object>();
        PEMWriter pemWriter = null;
        try {
            StringWriter sw = new StringWriter();
            pemWriter = new PEMWriter(sw);
            pemWriter.writeObject(cert);
            pemWriter.flush();
            publicKey.put("algorithm", cert.getPublicKey().getAlgorithm());
            publicKey.put("format", cert.getPublicKey().getFormat());
            publicKey.put("encoded", cert.getPublicKey().getEncoded());
            publicKey.put("cert", sw.getBuffer().toString());
        } finally {
            pemWriter.close();
        }
        return publicKey;
    }
    
    /**
     * Generates a certificate from a supplied string representation of one, and a supplied type.
     * 
     * @param certString A String representation of a certificate
     * @param type The type of certificate ("X509").
     * @return The generated certificate
     * @throws Exception
     */
    private Certificate generateCertificate(String certString, String type) throws Exception {
        StringReader sr = new StringReader(certString);
        PEMReader pw = new PEMReader(sr);
        Object object = pw.readObject();
        if (object instanceof X509Certificate) {
            return (X509Certificate)object;
        } else {
            throw new JsonResourceException(JsonResourceException.BAD_REQUEST, 
                    "Unsupported certificate format");
        }
    }
    
    /**
     * Generates a self signed certificate from a given domain name.
     * 
     * @param domainName the domain name to use for the new certificate
     * @return  The generated certificate
     * @throws Exception
     */
    private Certificate generateCertificate(String domainName, String algorithm, int keySize, 
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
        v3CertGen.setIssuerDN(new X509Principal("CN=" + domainName + ", OU=None, O=None L=None, C=None"));  
        v3CertGen.setNotBefore(notBefore);  
        v3CertGen.setNotAfter(notAfter);  
        v3CertGen.setSubjectDN(new X509Principal("CN=" + domainName + ", OU=None, O=None L=None, C=None")); 
        
        v3CertGen.setPublicKey(keyPair.getPublic());  
        v3CertGen.setSignatureAlgorithm(signatureAlgorithm); 
        
        X509Certificate cert = v3CertGen.generateX509Certificate(keyPair.getPrivate());
        
        return cert;
    }
    
    /**
     * Creates a truststore or keystore object and returns an instance of StoreWrapper.
     * 
     * @param name the name of the store to create ("truststore" or "keystore"
     * @return and instance of StoreWrapper representing the created store. 
     * @throws Exception
     */
    public StoreWrapper createStore(String name) throws Exception {
        String type = null;
        String location = null;
        String password = null;
        if (name.equals("truststore")) {
            type = Param.getTruststoreType();
            location = Param.getTruststoreLocation();
            password = Param.getTruststorePassword(false);
        } else if (name.equals("keystore")) {
            type = Param.getKeystoreType();
            location = Param.getKeystoreLocation();
            password = Param.getKeystorePassword(false);
        } else {
            return null;
        }
        
        KeyStore store = null;
        InputStream in = new FileInputStream(location);
        try {
            store = KeyStore.getInstance(type);     
            store.load(in, password.toCharArray());
        } finally {
            in.close();
        }
        
        return new StoreWrapper(name, location, password, type, store);
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
   
        public void store() throws Exception {
            OutputStream out = new FileOutputStream(location);
            try {
                store.store(out, password.toCharArray());
            } finally {
                out.close();
            }
        }
        
        public void reload() throws Exception {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(store);
            TrustManager [] managers = tmf.getTrustManagers();
            SSLContext context = SSLContext.getInstance("SSL");
            context.init(null, managers, null);
            SSLContext.setDefault(context);
        }
    }
}
