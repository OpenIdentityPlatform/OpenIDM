/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.security.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.util.ClusterUtil;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.crypto.factory.CryptoServiceFactory;
import org.forgerock.openidm.repo.RepositoryService;
import org.forgerock.openidm.security.KeyStoreHandler;
import org.forgerock.openidm.security.KeyStoreManager;
import org.forgerock.openidm.util.DateUtil;
import org.forgerock.util.encode.Base64;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class containing common members and methods of a Security ResourceProvider implementation.
 */
public class SecurityResourceProvider {
    
    private final static Logger logger = LoggerFactory.getLogger(SecurityResourceProvider.class);

    public static final String BC = org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;
    
    public static final String ACTION_GENERATE_CERT = "generateCert";
    public static final String ACTION_GENERATE_CSR = "generateCSR";

    public static final String DEFAULT_SIGNATURE_ALGORITHM = "SHA512WithRSAEncryption";
    public static final String DEFAULT_ALGORITHM = "RSA";
    public static final String DEFAULT_CERTIFICATE_TYPE = "X509";
    public static final int DEFAULT_KEY_SIZE = 2048;
    
    public static final String KEYS_CONTAINER = "security/keys";

    /**
     * The Keystore handler which handles access to actual Keystore instance
     */
    protected KeyStoreHandler store = null;
    
    /**
     * The KeyStoreManager used for reloading the stores. 
     */
    protected KeyStoreManager manager = null;

    /**
     * The RepositoryService
     */
    protected RepositoryService repoService;

    /**
     * The resource name, "truststore" or "keystore".
     */
    protected String resourceName = null;
    
    /**
     * The instance type (standalone, clustered-first, clustered-additional)
     */
    private String instanceType;
    
    private String cryptoAlias;
    
    private String cryptoCipher;

    public SecurityResourceProvider(String resourceName, KeyStoreHandler store, KeyStoreManager manager,
            RepositoryService repoService) {
        this.store = store;
        this.resourceName = resourceName;
        this.manager = manager;
        this.repoService = repoService;
        this.cryptoAlias = IdentityServer.getInstance().getProperty("openidm.config.crypto.alias");
        this.cryptoCipher = ServerConstants.SECURITY_CRYPTOGRAPHY_DEFAULT_CIPHER;
        this.instanceType = IdentityServer.getInstance().getProperty(
                "openidm.instance.type", ClusterUtil.TYPE_STANDALONE);
    }
    /**
     * Returns a PEM String representation of a object.
     * 
     * @param object the object
     * @return the PEM String representation
     * @throws Exception
     */
    protected String toPem(Object object) throws Exception {
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
    protected <T> T fromPem(String pem) throws Exception {
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
    protected Certificate readCertificate(String certString, String type) throws Exception {
        StringReader sr = new StringReader(certString);
        PEMReader pw = new PEMReader(sr);
        Object object = pw.readObject();
        if (object instanceof X509Certificate) {
            return (X509Certificate)object;
        } else {
            throw ResourceException.getException(ResourceException.BAD_REQUEST, "Unsupported certificate format");
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
    protected Certificate[] readCertificateChain(List<String> certStringChain, String type) throws Exception {
        Certificate [] certChain = new Certificate[certStringChain.size()];
        for (int i=0; i<certChain.length; i++) {
            certChain[i] = readCertificate(certStringChain.get(i), type);
        }
        return certChain;
    }

    /**
     * Returns a JsonValue map representing a certificate
     * 
     * @param alias  the certificate alias
     * @param cert  The certificate
     * @return a JsonValue map representing the certificate
     * @throws Exception
     */
    protected JsonValue returnCertificate(String alias, Certificate cert) throws Exception {
        JsonValue content = new JsonValue(new LinkedHashMap<String, Object>());
        content.put(ResourceResponse.FIELD_CONTENT_ID, alias);
        content.put("type", cert.getType());
        content.put("cert", getCertString(cert));
        content.put("publicKey", getKeyMap(cert.getPublicKey()));
        if (cert instanceof X509Certificate) {
            Map<String, Object> issuer = new HashMap<>();
            X500Name name = X500Name.getInstance(PrincipalUtil.getIssuerX509Principal((X509Certificate)cert));
            addAttributeToIssuer(issuer, name, "C", BCStyle.C);
            addAttributeToIssuer(issuer, name, "ST", BCStyle.ST);
            addAttributeToIssuer(issuer, name, "L", BCStyle.L);
            addAttributeToIssuer(issuer, name, "OU", BCStyle.OU);
            addAttributeToIssuer(issuer, name, "O", BCStyle.O);
            addAttributeToIssuer(issuer, name, "CN", BCStyle.CN);
            content.put("issuer", issuer);
            content.put("notBefore", ((X509Certificate)cert).getNotBefore());
            content.put("notAfter", ((X509Certificate)cert).getNotAfter());
        }
        return content;
    }

    /**
     * Returns a JsonValue map representing a CSR
     * 
     * @param alias  the certificate alias
     * @param csr  The CSR
     * @return a JsonValue map representing the CSR
     * @throws Exception
     */
    protected JsonValue returnCertificateRequest(String alias, PKCS10CertificationRequest csr) throws Exception {
        JsonValue content = new JsonValue(new LinkedHashMap<String, Object>());
        content.put(ResourceResponse.FIELD_CONTENT_ID, alias);
        content.put("csr", getCertString(csr));
        content.put("publicKey", getKeyMap(csr.getPublicKey()));
        return content;
    }

    /**
     * Returns a JsonValue map representing a CSR
     * 
     * @param alias  the certificate alias
     * @param key The key
     * @return a JsonValue map representing the CSR
     * @throws Exception
     */
    protected JsonValue returnKey(String alias, Key key) throws Exception {
        JsonValue content = new JsonValue(new LinkedHashMap<String, Object>());
        content.put(ResourceResponse.FIELD_CONTENT_ID, alias);
        if (key instanceof PrivateKey) {
            content.put("privateKey", getKeyMap(key));
        } else if (key instanceof SecretKey) {
            content.put("secret", getSecretKeyMap(key));
        }
        return content;
    }

    /**
     * Returns a JsonValue map representing key
     * 
     * @param key  The key
     * @return a JsonValue map representing the key
     * @throws Exception
     */
    protected Map<String, Object> getKeyMap(Key key) throws Exception {
        Map<String, Object> keyMap = new HashMap<>();
        keyMap.put("algorithm", key.getAlgorithm());
        keyMap.put("format", key.getFormat());
        keyMap.put("encoded", toPem(key));
        return keyMap;
    }

    /**
     * Returns a JsonValue map representing key
     *
     * @param key  The key
     * @return a JsonValue map representing the key
     * @throws Exception
     */
    protected Map<String, Object> getSecretKeyMap(Key key) throws Exception {
        Map<String, Object> keyMap = new HashMap<>();
        keyMap.put("algorithm", key.getAlgorithm());
        keyMap.put("format", key.getFormat());
        keyMap.put("encoded", Base64.encode(key.getEncoded()));
        return keyMap;
    }

    /**
     * Returns a PEM formatted string representation of an object
     * 
     * @param object the object to write
     * @return a PEM formatted string representation of the object
     * @throws Exception
     */
    protected String getCertString(Object object) throws Exception {
        try (StringWriter sw = new StringWriter(); PEMWriter pemWriter = new PEMWriter(sw)) {
            pemWriter.writeObject(object);
            pemWriter.flush();
            return sw.getBuffer().toString();
        }
    }

    /**
     * Generates a self signed certificate using the given properties.
     *
     * @param commonName the common name to use for the new certificate
     * @param algorithm the algorithm to use
     * @param keySize the keysize to use
     * @param signatureAlgorithm the signature algorithm to use
     * @param validFrom when the certificate is valid from
     * @param validTo when the certificate is valid until
     * @return The generated certificate
     * @throws Exception
     */
    protected Pair<X509Certificate, PrivateKey> generateCertificate(String commonName, 
            String algorithm, int keySize, String signatureAlgorithm, String validFrom,
            String validTo) throws Exception {
        return generateCertificate(commonName, "None", "None", "None", "None", "None",
                algorithm, keySize, signatureAlgorithm, validFrom, validTo);
    }

        
    /**
     * Generates a self signed certificate using the given properties.
     *
     * @param commonName the subject's common name
     * @param organization the subject's organization name
     * @param organizationUnit the subject's organization unit name
     * @param stateOrProvince the subject's state or province
     * @param country the subject's country code
     * @param locality the subject's locality
     * @param algorithm the algorithm to use
     * @param keySize the keysize to use
     * @param signatureAlgorithm the signature algorithm to use
     * @param validFrom when the certificate is valid from
     * @param validTo when the certificate is valid until
     * @return The generated certificate
     * @throws Exception
     */
    protected Pair<X509Certificate, PrivateKey> generateCertificate(String commonName, 
            String organization, String organizationUnit, String stateOrProvince, 
            String country, String locality, String algorithm, int keySize,
            String signatureAlgorithm, String validFrom, String validTo) throws Exception {
        
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm); // "RSA","BC"
        keyPairGenerator.initialize(keySize);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // Generate self-signed certificate
        X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);
        builder.addRDN(BCStyle.C, country);
        builder.addRDN(BCStyle.ST, stateOrProvince);
        builder.addRDN(BCStyle.L, locality);
        builder.addRDN(BCStyle.OU, organizationUnit);
        builder.addRDN(BCStyle.O, organization);
        builder.addRDN(BCStyle.CN, commonName);

        Date notBefore = null;
        Date notAfter = null;
        if (validFrom == null) {
            notBefore = new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30);
        } else {
            DateTime notBeforeDateTime = DateUtil.getDateUtil().parseIfDate(validFrom);
            if (notBeforeDateTime == null) {
                throw new InternalServerErrorException("Invalid date format for 'validFrom' property");
            } else {
                notBefore = notBeforeDateTime.toDate();
            }
        }
        if (validTo == null) {
            Calendar date = Calendar.getInstance();
            date.setTime(new Date());
            date.add(Calendar.YEAR, 10);
            notAfter = date.getTime();
        } else {
            DateTime notAfterDateTime = DateUtil.getDateUtil().parseIfDate(validTo);
            if (notAfterDateTime == null) {
                throw new InternalServerErrorException("Invalid date format for 'validTo' property");
            } else {
                notAfter = notAfterDateTime.toDate();
            }
        }

        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());

        X509v3CertificateBuilder v3CertGen = new JcaX509v3CertificateBuilder(builder.build(), serial, 
                notBefore, notAfter, builder.build(), keyPair.getPublic());

        ContentSigner sigGen =
                new JcaContentSignerBuilder(signatureAlgorithm).setProvider(BC).build(keyPair.getPrivate());

        X509Certificate cert =
                new JcaX509CertificateConverter().setProvider(BC).getCertificate(v3CertGen.build(sigGen));
        cert.checkValidity(new Date());
        cert.verify(cert.getPublicKey());

        return Pair.of(cert, keyPair.getPrivate());
    }
    
    /**
     * Generates a CSR request.
     * 
     * @param alias
     * @param algorithm
     * @param signatureAlgorithm
     * @param keySize
     * @param params
     * @return
     * @throws Exception
     */
    protected Pair<PKCS10CertificationRequest, PrivateKey> generateCSR(String alias, String algorithm,
            String signatureAlgorithm, int keySize, JsonValue params) throws Exception {

        // Construct the distinguished name
        StringBuilder sb = new StringBuilder(); 
        sb.append("CN=").append(params.get("CN").required().asString().replaceAll(",", "\\\\,"));
        sb.append(", OU=").append(params.get("OU").defaultTo("None").asString().replaceAll(",", "\\\\,"));
        sb.append(", O=").append(params.get("O").defaultTo("None").asString().replaceAll(",", "\\\\,"));
        sb.append(", L=").append(params.get("L").defaultTo("None").asString().replaceAll(",", "\\\\,"));
        sb.append(", ST=").append(params.get("ST").defaultTo("None").asString().replaceAll(",", "\\\\,"));
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
        PKCS10CertificationRequest cr = new PKCS10CertificationRequest(signatureAlgorithm, subjectName, publicKey,
                null, privateKey);
        
        // Store the private key to use when the signed cert is return and updated
        logger.debug("Storing private key with alias {}", alias);
        storeKeyPair(alias, keyPair);
        
        return Pair.of(cr, privateKey);
    }   

    /**
     * Stores a KeyPair (associated with a CSR request on the specified alias) in the repository.
     * 
     * @param alias the alias from the CSR
     * @param keyPair the KeyPair object
     * @throws ResourceException
     */
    protected void storeKeyPair(String alias, KeyPair keyPair) throws ResourceException {
        try {
            JsonValue keyPairValue = new JsonValue(new HashMap<String, Object>());
            keyPairValue.put("value" , toPem(keyPair));
            JsonValue encrypted = getCryptoService().encrypt(keyPairValue, cryptoCipher, cryptoAlias);
            JsonValue keyMap = new JsonValue(new HashMap<String, Object>());
            keyMap.put("keyPair", encrypted.getObject());
            storeInRepo(KEYS_CONTAINER, alias, keyMap);
        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
        
    }
    
    /**
     * Reads an object from the repository
     * @param id the object's id
     * @return the object
     * @throws ResourceException
     */
    protected JsonValue readFromRepo(String id) throws ResourceException {
        JsonValue keyMap = new JsonValue(repoService.read(Requests.newReadRequest(id)).getContent());
        return keyMap;
    }
    
    /**
     * Stores an object in the repository
     * @param id the object's id
     * @param value the value of the object to store
     * @throws ResourceException
     */
    protected void storeInRepo(String container, String id, JsonValue value) throws ResourceException {
        ResourceResponse oldResource;
        try {
            oldResource = repoService.read(Requests.newReadRequest(container, id));
        } catch (NotFoundException e) {
            logger.debug("creating object " + id);
            repoService.create(Requests.newCreateRequest(container, id, value));
            return;
        }
        UpdateRequest updateRequest = Requests.newUpdateRequest(container, id, value);
        updateRequest.setRevision(oldResource.getRevision());
        repoService.update(updateRequest);
    }
    
    /**
     * Returns a stored KeyPair (associated with a CSR request on the specified alias) from the repository.
     * 
     * @param alias the alias from the CSR
     * @return the KeyPair
     * @throws ResourceException
     */
    protected KeyPair getKeyPair(String alias) throws ResourceException {
        String id = KEYS_CONTAINER + "/" + alias;
        ResourceResponse keyResource = repoService.read(Requests.newReadRequest(id));
        if (keyResource.getContent().isNull()) {
            throw new NotFoundException("Cannot find stored key for alias " + alias);
        }
        try {
            JsonValue encrypted = keyResource.getContent().get("keyPair");
            JsonValue keyPairValue = getCryptoService().decrypt(encrypted);
            return fromPem(keyPairValue.get("value").asString());
        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }
    
    /**
     * Verifies that the supplied private key and signed certificate match by signing/verifying some test data.
     * 
     * @param privateKey A private key
     * @param cert the certificate
     * @throws ResourceException if the verification fails, or an error is encountered.
     */
    protected void verify(PrivateKey privateKey, Certificate cert) throws ResourceException {
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
            throw new InternalServerErrorException("Error verifying private key and signed certificate", e);
        }
        if (!verified) {
            throw new BadRequestException("Private key does not match signed certificate");
        }
    }
    
    /**
     * Saves the local store only if in a clustered environment.
     * 
     * @throws ResourceException
     */
    protected void saveStore() throws ResourceException {
        if (!instanceType.equals(ClusterUtil.TYPE_STANDALONE)) {
            saveStoreToRepo();
        }
    }
    
    /**
     * Loads the store from the repository and stores it locally
     * 
     * @throws ResourceException
     */
    public void loadStoreFromRepo() throws ResourceException {
        JsonValue keystoreValue = readFromRepo("security/" + resourceName);
        String keystoreString = keystoreValue.get("storeString").asString();
        byte [] keystoreBytes = Base64.decode(keystoreString.getBytes());
        ByteArrayInputStream bais = new ByteArrayInputStream(keystoreBytes);
        try {
            KeyStore keystore = null;
            try {
                keystore = KeyStore.getInstance(store.getType());     
                keystore.load(bais, store.getPassword().toCharArray());
            } finally {
                bais.close();
            }
            store.setStore(keystore);
        } catch (Exception e) {
            // Note this may catch NPE from Base64.decode returning null if keyStoreString
            // is null or not a base64-encoded string
            throw new InternalServerErrorException("Error creating keystore from store bytes", e);
        }
    }
    
    /**
     * Saves the local store to the repository
     * 
     * @throws ResourceException
     */
    public void saveStoreToRepo() throws ResourceException {
        byte [] keystoreBytes = null;
        File file = new File(store.getLocation());

        try (FileInputStream fin = new FileInputStream(file)) {
            keystoreBytes = new byte[(int) file.length()];
            fin.read(keystoreBytes);
        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
        
        String keystoreString = Base64.encode(keystoreBytes);
        JsonValue value = new JsonValue(new HashMap<String, Object>());
        value.add("storeString", keystoreString);
        storeInRepo("security", resourceName, value);
    }
    
    /**
     * Returns and instance of the CryptoService.
     * 
     * @return CryptoService instance.
     */
    private CryptoService getCryptoService() {
        return CryptoServiceFactory.getInstance();
    }
    
    /**
     * Adds an attribute to an issuer map object if it exists in the supplied X500Name object.
     * 
     * @param issuer The issuer to add to
     * @param name The X500Name object
     * @param attribute the name of the attribute
     * @param oid the ASN1ObjectIdentifier corresponding to the attribute
     * @throws Exception
     */
    private void addAttributeToIssuer(Map<String, Object> issuer, X500Name name, String attribute,
            ASN1ObjectIdentifier oid) throws Exception {
        RDN [] rdns = name.getRDNs(oid);
        if (rdns != null && rdns.length > 0) {
            issuer.put(attribute, rdns[0].getFirst().getValue().toString());
        } 
    }
}
