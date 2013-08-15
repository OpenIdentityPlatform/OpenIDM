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

package org.forgerock.openidm.security.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequestHolder;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.jetty.Param;
import org.forgerock.openidm.security.KeyStoreHandler;
import org.forgerock.openidm.security.KeyStoreManager;
import org.forgerock.openidm.util.DateUtil;
import org.forgerock.openidm.util.ResourceUtil;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public class KeystoreResourceProvider implements SingletonResourceProvider {

    /**
     * Setup logging for the {@link SecurityManager}.
     */
    private final static Logger logger = LoggerFactory.getLogger(SecurityManager.class);

    private static final String BC = org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

    private static final String FIELD_CERT = "cert";
    
    public static final String ACTION_GENERATE_CERT = "generateCert";
    public static final String ACTION_GENERATE_CSR = "generateCSR";

    public static final String DEFAULT_SIGNATURE_ALGORITHM = "SHA512WithRSAEncryption";
    public static final String DEFAULT_ALGORITHM = "RSA";
    public static final String DEFAULT_CERTIFICATE_TYPE = "X509";
    public static final int DEFAULT_KEY_SIZE = 2048;

    private final KeyStoreHandler store;
    
    private final KeyStoreManager manager;    
    
    /**
     * The Repository Service Accessor
     */
    private ServerContext accessor;

    private final String resourceName;

    public KeystoreResourceProvider(String resourceName, KeyStoreHandler store, KeyStoreManager manager, ServerContext accessor) {
        this.store = store;
        this.resourceName = resourceName;
        this.manager = manager;
        this.accessor = accessor;
    }

    @Override
    public void actionInstance(ServerContext context, ActionRequest request,
            ResultHandler<JsonValue> handler) {
        final ResourceException e =
                new NotSupportedException("Action operations are not supported");
        handler.handleError(e);
    }

    @Override
    public void patchInstance(ServerContext context, PatchRequest request,
            ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Patch operations are not supported");
        handler.handleError(e);
    }

    @Override
    public void readInstance(final ServerContext context, final ReadRequest request,
            final ResultHandler<Resource> handler) {
        try {
            JsonValue content = new JsonValue(new LinkedHashMap<String, Object>(5));
            content.put("type", store.getStore().getType());
            content.put("provider", store.getStore().getProvider());
            Enumeration<String> aliases = store.getStore().aliases();
            List<String> aliasList = new ArrayList<String>();
            while (aliases.hasMoreElements()) {
                aliasList.add(aliases.nextElement());
            }
            content.put("aliases", aliasList);
            handler.handleResult(new Resource(resourceName, null, content));
        } catch (Throwable t) {
        	t.printStackTrace();
            handler.handleError(ResourceUtil.adapt(t));
        }
    }

    @Override
    public void updateInstance(ServerContext context, UpdateRequest request,
            ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Update operations are not supported");
        handler.handleError(e);
    }

    // ----- Implementation of CollectionResourceProvider interface

    public final CollectionResourceProvider CERT = new CollectionResourceProvider() {
        @Override
        public void actionCollection(final ServerContext context, final ActionRequest request,
                final ResultHandler<JsonValue> handler) {
            final ResourceException e = new NotSupportedException("Action operations are not supported");
            handler.handleError(e);
        }

        @Override
        public void actionInstance(final ServerContext context, final String resourceId,
                final ActionRequest request, final ResultHandler<JsonValue> handler) {
            try {
            	if (ACTION_GENERATE_CERT.equalsIgnoreCase(request.getAction()) || 
                        ACTION_GENERATE_CSR.equalsIgnoreCase(request.getAction())) {
                    if (resourceId == null) {
                        throw ResourceException.getException(ResourceException.BAD_REQUEST, 
                                "A valid resource ID must be specified in the request");
                    }
                    String algorithm = request.getContent().get("algorithm").defaultTo(DEFAULT_ALGORITHM).asString();
                    String signatureAlgorithm = request.getContent().get("signatureAlgorithm")
                    		.defaultTo(DEFAULT_SIGNATURE_ALGORITHM).asString();
                    int keySize = request.getContent().get("keySize").defaultTo(DEFAULT_KEY_SIZE).asInteger();
                    JsonValue result = null;
            		if (ACTION_GENERATE_CERT.equalsIgnoreCase(request.getAction())) {
            			// Generate self-signed certificate
            			if (store.getStore().containsAlias(resourceId)) {
            				handler.handleError(new ConflictException("The resource with ID '" + resourceId 
            						+ "' could not be created because there is already another resource with the same ID"));
            			} else {
            				String domainName = request.getContent().get("domainName").required().asString();
            				String validFrom = request.getContent().get("validFrom").asString();
            				String validTo = request.getContent().get("validTo").asString();

            				// Generate the cert
            				Pair<X509Certificate, PrivateKey> cert = generateCertificate(domainName, algorithm, 
            						keySize, signatureAlgorithm, validFrom, validTo);

            				String password = request.getContent().get("password").defaultTo(
            						Param.getKeystoreKeyPassword()).asString();

            				// Add it to the store and reload
            				store.getStore().setCertificateEntry(resourceId, cert.getKey());
            				store.getStore().setEntry( resourceId, new KeyStore.PrivateKeyEntry(cert.getValue(),
            						new Certificate[]{cert.getKey()}), new KeyStore.PasswordProtection(password.toCharArray()));
            				store.store();

            				manager.reload();

            				result = returnCertificate(resourceId, cert.getKey());
            			}
            		} else {
            			// Generate CSR
            			Pair<PKCS10CertificationRequest, PrivateKey> csr = generateCSR(resourceId, algorithm, 
            					signatureAlgorithm, keySize, request.getContent());
            			result = returnCertificateRequest(resourceId, csr.getKey());
            			if (request.getContent().get("returnPrivateKey").defaultTo(false).asBoolean()) {
            				result.put("privateKey", getKeyMap(csr.getRight()));
                        }
            		}
                    handler.handleResult(result);
            	} else {
            		handler.handleError(new BadRequestException("Unsupported action " + request.getAction()));
            	}
            	
            } catch (Throwable t) {
                handler.handleError(ResourceUtil.adapt(t));
            }
        }

        @Override
        public void createInstance(final ServerContext context, final CreateRequest request,
                final ResultHandler<Resource> handler) {
            try {

                if (null != request.getNewResourceId()) {
                    if (store.getStore().containsAlias(request.getNewResourceId())) {
                        handler.handleError(new ConflictException("The resource with ID '"
                                + request.getNewResourceId() + "' could not be created because "
                                + "there is already another resource with the same ID"));
                    } else {
                    	String resourceId = request.getNewResourceId();
                    	storeCert(request.getContent(), resourceId);
                    	manager.reload();
                    	handler.handleResult(new Resource(resourceId, null, request.getContent()));
                    }
                } else {
                    handler.handleError(new BadRequestException(
                            "A valid resource ID must be specified in the request"));
                }
            } catch (Throwable t) {
                handler.handleError(ResourceUtil.adapt(t));
            }
        }

        @Override
        public void readInstance(final ServerContext context, final String resourceId,
                final ReadRequest request, final ResultHandler<Resource> handler) {
            try {
                if (!store.getStore().containsAlias(resourceId)) {
                    handler.handleError(new NotFoundException());
                } else {
                    Certificate cert = store.getStore().getCertificate(resourceId);
                    handler.handleResult(new Resource(resourceId, null, returnCertificate( resourceId, cert)));
                }
            } catch (Throwable t) {
                handler.handleError(ResourceUtil.adapt(t));
            }
        }

        @Override
        public void updateInstance(final ServerContext context, final String resourceId,
                final UpdateRequest request, final ResultHandler<Resource> handler) {
            try {
            	storeCert(request.getNewContent(), resourceId);
            	manager.reload();
            	Certificate cert = store.getStore().getCertificate(resourceId);
            	handler.handleResult(new Resource(resourceId, null, returnCertificate( resourceId, cert)));
            } catch (Throwable t) {
                handler.handleError(ResourceUtil.adapt(t));
            }
        }

        @Override
        public void deleteInstance(final ServerContext context, final String resourceId,
                final DeleteRequest request, final ResultHandler<Resource> handler) {
            try {
                if (!store.getStore().containsAlias(resourceId)) {
                    handler.handleError(new NotFoundException());
                } else {
                    store.getStore().deleteEntry(resourceId);
                    store.store();
                    manager.reload();
                }
                handler.handleResult(new Resource(resourceId, null, new JsonValue(null)));
            } catch (Throwable t) {
                handler.handleError(ResourceUtil.adapt(t));
            }
        }

        @Override
        public void patchInstance(final ServerContext context, final String resourceId,
                final PatchRequest request, final ResultHandler<Resource> handler) {
            final ResourceException e =
                    new NotSupportedException("Patch operations are not supported");
            handler.handleError(e);
        }

        @Override
        public void queryCollection(final ServerContext context, final QueryRequest request,
                final QueryResultHandler handler) {
            final ResourceException e =
                    new NotSupportedException("Patch operations are not supported");
            handler.handleError(e);
        }

        private JsonValue returnCertificate(String alias, Certificate cert) throws Exception {
            JsonValue content = new JsonValue(new LinkedHashMap<String, Object>(3));
            content.put(Resource.FIELD_CONTENT_ID, alias);
            content.put("type", cert.getType());
            content.put("cert", getCertString(cert));
            content.put("publicKey", getKeyMap(cert.getPublicKey()));
            return content;
        }

        private JsonValue returnCertificateRequest(String alias, PKCS10CertificationRequest csr) throws Exception {
            JsonValue content = new JsonValue(new LinkedHashMap<String, Object>(3));
            content.put(Resource.FIELD_CONTENT_ID, alias);
            content.put("csr", getCertString(csr));
            content.put("publicKey", getKeyMap(csr.getPublicKey()));
            return content;
        }
    };

    // ----- Implementation of CollectionResourceProvider interface

    final CollectionResourceProvider KEY = new CollectionResourceProvider() {
        @Override
        public void actionCollection(final ServerContext context, final ActionRequest request,
                final ResultHandler<JsonValue> handler) {
            try {

            } catch (Throwable t) {
                handler.handleError(ResourceUtil.adapt(t));
            }
        }

        @Override
        public void actionInstance(final ServerContext context, final String resourceId,
                final ActionRequest request, final ResultHandler<JsonValue> handler) {
            try {

            } catch (Throwable t) {
                handler.handleError(ResourceUtil.adapt(t));
            }
        }

        @Override
        public void createInstance(final ServerContext context, final CreateRequest request,
                final ResultHandler<Resource> handler) {
            try {

            } catch (Throwable t) {
                handler.handleError(ResourceUtil.adapt(t));
            }
        }

        @Override
        public void readInstance(final ServerContext context, final String resourceId,
                final ReadRequest request, final ResultHandler<Resource> handler) {
            try {

            } catch (Throwable t) {
                handler.handleError(ResourceUtil.adapt(t));
            }
        }

        @Override
        public void updateInstance(final ServerContext context, final String resourceId,
                final UpdateRequest request, final ResultHandler<Resource> handler) {
            try {

            } catch (Throwable t) {
                handler.handleError(ResourceUtil.adapt(t));
            }
        }

        @Override
        public void deleteInstance(final ServerContext context, final String resourceId,
                final DeleteRequest request, final ResultHandler<Resource> handler) {
            try {

            } catch (Throwable t) {
                handler.handleError(ResourceUtil.adapt(t));
            }
        }

        @Override
        public void patchInstance(final ServerContext context, final String resourceId,
                final PatchRequest request, final ResultHandler<Resource> handler) {
            final ResourceException e =
                    new NotSupportedException("Patch operations are not supported");
            handler.handleError(e);
        }

        @Override
        public void queryCollection(final ServerContext context, final QueryRequest request,
                final QueryResultHandler handler) {
            final ResourceException e =
                    new NotSupportedException("Patch operations are not supported");
            handler.handleError(e);
        }
    };

    /*private Map<String, Object> getPublicKeyResult(Certificate cert) throws Exception {
        Map<String, Object> publicKey = new HashMap<String, Object>(4);
        PEMWriter pemWriter = null;
        try {
            StringWriter sw = new StringWriter();
            pemWriter = new PEMWriter(sw);
            pemWriter.writeObject(cert);
            pemWriter.flush();
            publicKey.put("algorithm", cert.getPublicKey().getAlgorithm());
            publicKey.put("format", cert.getPublicKey().getFormat());
            publicKey.put("encoded", cert.getPublicKey().getEncoded());
            publicKey.put(FIELD_CERT, sw.getBuffer().toString());
        } finally {
            pemWriter.close();
        }
        return publicKey;
    }*/

    private void storeCert(JsonValue value, String alias) throws Exception{
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
                throw new NotFoundException("No private key exists for the supplied signed certificate");
            }
            List<String> certStringChain = value.get("certs").required().asList(String.class);
            Certificate [] certChain = readCertificateChain(certStringChain, type);
            store.getStore().setEntry(alias, new PrivateKeyEntry(privateKey, certChain), 
            		new KeyStore.PasswordProtection(store.getPassword().toCharArray()));
        } else {
            String certString = value.get("cert").required().asString();
            Certificate cert = readCertificate(certString, type);
            store.getStore().setCertificateEntry(alias, cert);
        }
        store.store();
    }
    
    /**
     * Generates a certificate from a supplied string representation of one, and
     * a supplied type.
     *
     * @param content
     *            A JsonValue representation of a certificate
     * @return The generated certificate
     * @throws Exception
     */
    private Certificate generateCertificate(JsonValue content) throws Exception {
        String certString = content.get(FIELD_CERT).required().asString();
        //String type = content.get("type").defaultTo("X509").asString();

        StringReader sr = new StringReader(certString);
        PEMReader pw = new PEMReader(sr);
        Object object = pw.readObject();
        if (object instanceof X509Certificate) {
            return (X509Certificate) object;
        } else {
            throw new BadRequestException("Unsupported certificate format");
        }
    }
    
    private Pair<PKCS10CertificationRequest, PrivateKey> generateCSR(String alias, String algorithm, String signatureAlgorithm, int keySize, 
            JsonValue params) throws Exception {

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
        
        return Pair.of(cr, privateKey);
    }

    /**
     * Generates a self signed certificate from a given domain name.
     *
     * @param domainName
     *            the domain name to use for the new certificate
     * @return The generated certificate
     * @throws Exception
     */
    private Pair<X509Certificate, PrivateKey> generateCertificate(String domainName,
            String algorithm, int keySize, String signatureAlgorithm, String validFrom,
            String validTo) throws Exception {

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm); // "RSA","BC"
        keyPairGenerator.initialize(keySize);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // Generate self-signed certificate
        X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);
        builder.addRDN(BCStyle.C, "None");
        builder.addRDN(BCStyle.ST, "None");
        builder.addRDN(BCStyle.L, "None");
        builder.addRDN(BCStyle.OU, "None");
        builder.addRDN(BCStyle.O, "None");
        builder.addRDN(BCStyle.CN, domainName);

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

        ContentSigner sigGen = new JcaContentSignerBuilder(signatureAlgorithm).setProvider(BC).build(keyPair.getPrivate());

        X509Certificate cert = new JcaX509CertificateConverter().setProvider(BC).getCertificate(v3CertGen.build(sigGen));
        cert.checkValidity(new Date());
        cert.verify(cert.getPublicKey());

        return Pair.of(cert, keyPair.getPrivate());
    }

    public static X509Certificate sign(PKCS10CertificationRequest inputCSR, PrivateKey caPrivate,
            KeyPair pair) throws InvalidKeyException, NoSuchAlgorithmException,
            NoSuchProviderException, SignatureException, IOException, OperatorCreationException,
            CertificateException {

        AlgorithmIdentifier sigAlgId =
                new DefaultSignatureAlgorithmIdentifierFinder().find("SHA1withRSA");
        AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);

        AsymmetricKeyParameter caKeyParam = PrivateKeyFactory.createKey(caPrivate.getEncoded());
        SubjectPublicKeyInfo keyInfo = SubjectPublicKeyInfo.getInstance(pair.getPublic().getEncoded());

        PKCS10CertificationRequestHolder pk10Holder = new PKCS10CertificationRequestHolder(inputCSR);

        X509v3CertificateBuilder v3CertGen = new X509v3CertificateBuilder(new X500Name("CN=issuer"), new BigInteger("1"),
                        new Date(System.currentTimeMillis()), new Date(System.currentTimeMillis()
                                + 30 * 365 * 24 * 60 * 60 * 1000), pk10Holder.getSubject(), keyInfo);

        ContentSigner sigGen = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(caKeyParam);

        X509CertificateHolder holder = v3CertGen.build(sigGen);
        X509CertificateStructure eeX509CertificateStructure = holder.toASN1Structure();

        CertificateFactory cf = CertificateFactory.getInstance("X.509", "BC");

        // Read Certificate
        InputStream is1 = new ByteArrayInputStream(eeX509CertificateStructure.getEncoded());
        X509Certificate theCert = (X509Certificate) cf.generateCertificate(is1);
        is1.close();
        return theCert;
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
    private void storeCsrKeyPair(String alias, KeyPair keyPair) throws ResourceException {
        if (accessor == null) {
            throw ResourceException.getException(ResourceException.INTERNAL_ERROR, "Repo router is null");
        }
        try {
        	String container = "/repo/security/keys";
            String id = container + "/" + alias;
            JsonValue oldKeyMap = null;
            JsonValue keyMap = new JsonValue(new HashMap<String, Object>());
            String keyString = toPem(keyPair);
            keyMap.put("encoded", keyString);
            try {
            	oldKeyMap = accessor.getConnection().read(accessor, Requests.newReadRequest(id)).getContent();
            } catch (NotFoundException e) {
                logger.debug("creating object " + id);
                accessor.getConnection().create(accessor, Requests.newCreateRequest(container, alias, keyMap));
                return;
            }
            keyMap.put("_rev", oldKeyMap.get("_rev").getObject());
            accessor.getConnection().update(accessor, Requests.newUpdateRequest(container, alias, keyMap));
        } catch (Exception e) {
        	e.printStackTrace();
            throw ResourceException.getException(ResourceException.INTERNAL_ERROR, e.getMessage(), e);
        }
        
    }
    
    /**
     * Returns a stored KeyPair (associated with a CSR request on the specified alias) from the repository.
     * 
     * @param alias the alias from the CSR
     * @return the KeyPair
     * @throws JsonResourceException
     */
    private KeyPair getCsrKeyPair(String alias) throws ResourceException {
        if (accessor == null) {
            throw ResourceException.getException(ResourceException.INTERNAL_ERROR, "Repo router is null");
        }
        String container = "/repo/security/keys";
        String id = container + "/" + alias;
        Resource keyResource = accessor.getConnection().read(accessor, Requests.newReadRequest(id));
        if (keyResource.getContent().isNull()) {
            throw ResourceException.getException(ResourceException.NOT_FOUND, 
            		"Cannot find stored key for alias " + alias);
        }
        try {
            JsonValue key = keyResource.getContent().get("encoded");
            String pemString = key.asString();
            return fromPem(pemString);
        } catch (Exception e) {
        	e.printStackTrace();
            throw ResourceException.getException(ResourceException.INTERNAL_ERROR, e.getMessage(), e);
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
    private Certificate[] readCertificateChain(List<String> certStringChain, String type) throws Exception {
        Certificate [] certChain = new Certificate[certStringChain.size()];
        for (int i=0; i<certChain.length; i++) {
            certChain[i] = readCertificate(certStringChain.get(i), type);
        }
        return certChain;
    }
}
