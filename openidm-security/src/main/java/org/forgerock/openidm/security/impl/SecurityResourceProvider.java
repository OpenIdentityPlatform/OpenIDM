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

package org.forgerock.openidm.security.impl;

import static org.forgerock.util.Reject.checkNotNull;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.jce.X509Principal;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.crypto.KeyRepresentation;
import org.forgerock.openidm.keystore.KeyStoreService;
import org.forgerock.openidm.repo.RepositoryService;
import org.forgerock.openidm.util.CertUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class containing common members and methods of a Security ResourceProvider implementation.
 */
public class SecurityResourceProvider {
    
    private final static Logger logger = LoggerFactory.getLogger(SecurityResourceProvider.class);
    
    public static final String ACTION_GENERATE_CERT = "generateCert";
    public static final String ACTION_GENERATE_CSR = "generateCSR";

    public static final String DEFAULT_SIGNATURE_ALGORITHM = "SHA512WithRSAEncryption";
    public static final String DEFAULT_ALGORITHM = "RSA";
    public static final String DEFAULT_CERTIFICATE_TYPE = "X509";
    public static final int DEFAULT_KEY_SIZE = 2048;
    
    public static final String KEYS_CONTAINER = "security/keys";

    /**
     * The Keystore service which handles access to actual Keystore instance.
     */
    protected KeyStoreService keyStoreService;

    /**
     * The keystore to act on.
     */
    protected KeyStore keyStore;

    /**
     * The RepositoryService
     */
    protected RepositoryService repoService;

    protected CryptoService cryptoService;

    /**
     * The resource name, "truststore" or "keystore".
     */
    protected String resourceName = null;
    
    private String cryptoAlias;
    
    private String cryptoCipher;

    public SecurityResourceProvider(String resourceName, KeyStore keyStore, KeyStoreService keyStoreService,
            RepositoryService repoService, CryptoService cryptoService) {
        this.keyStoreService = checkNotNull(keyStoreService);
        this.keyStore = checkNotNull(keyStore);
        this.resourceName = checkNotNull(resourceName);
        this.repoService = checkNotNull(repoService);
        this.cryptoAlias = IdentityServer.getInstance().getProperty("openidm.config.crypto.alias");
        this.cryptoCipher = ServerConstants.SECURITY_CRYPTOGRAPHY_DEFAULT_CIPHER;
        this.cryptoService = checkNotNull(cryptoService);
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
        content.put("cert", CertUtil.getCertString(cert));
        content.put("publicKey", KeyRepresentation.getKeyMap(cert.getPublicKey()).getObject());
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
        content.put("csr", CertUtil.getCertString(csr));
        content.put("publicKey", KeyRepresentation.getKeyMap(csr.getPublicKey()).getObject());
        return content;
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
            keyPairValue.put("value" , KeyRepresentation.toPem(keyPair));
            JsonValue encrypted = cryptoService.encrypt(keyPairValue, cryptoCipher, cryptoAlias);
            JsonValue keyMap = new JsonValue(new HashMap<String, Object>());
            keyMap.put("keyPair", encrypted.getObject());
            storeInRepo(KEYS_CONTAINER, alias, keyMap);
        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
        
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
            JsonValue keyPairValue = cryptoService.decrypt(encrypted);
            return CertUtil.fromPem(keyPairValue.get("value").asString());
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
