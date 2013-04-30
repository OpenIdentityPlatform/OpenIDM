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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
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

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

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
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
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

    private static final String BC =
            org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

    private static final String FIELD_CERT = "cert";

    private final KeyStoreHandler store;

    private final String resourceName;

    public KeystoreResourceProvider( String resourceName, KeyStoreHandler store) {
        this.store = store;
        this.resourceName = resourceName;
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
            handler.handleError(ResourceUtil.adapt(t));
        }
    }

    @Override
    public void updateInstance(ServerContext context, UpdateRequest request,
            ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Update operations are not supported");
        handler.handleError(e);
    }

    // ----- Implementation of CollectionResourceProvider interface

    final CollectionResourceProvider CERT = new CollectionResourceProvider() {
        @Override
        public void actionCollection(final ServerContext context, final ActionRequest request,
                final ResultHandler<JsonValue> handler) {
            final ResourceException e =
                    new NotSupportedException("Action operations are not supported");
            handler.handleError(e);
        }

        @Override
        public void actionInstance(final ServerContext context, final String resourceId,
                final ActionRequest request, final ResultHandler<JsonValue> handler) {
            final ResourceException e =
                    new NotSupportedException("Action operations are not supported");
            handler.handleError(e);

            try {

                if ("generateCert".equalsIgnoreCase(request.getAction())) {
                    if (store.getStore().containsAlias(resourceId)) {
                        handler.handleError(new ConflictException("The resource with ID '"
                                + resourceId + "' could not be created because "
                                + "there is already another resource with the same ID"));
                    } else {

                        String domainName =
                                request.getContent().get("domainName").required().asString();
                        String algorithm =
                                request.getContent().get("algorithm").defaultTo("RSA").asString();
                        String signatureAlgorithm =
                                request.getContent().get("signatureAlgorithm").defaultTo(
                                        "MD5WithRSAEncryption").asString();  //SHA256WithRSAEncryption
                        int keySize =
                                request.getContent().get("keySize").defaultTo(1024).asInteger();
                        String validFrom = request.getContent().get("validFrom").asString();
                        String validTo = request.getContent().get("validTo").asString();

                        // Generate the cert
                        Certificate cert =
                                generateCertificate(domainName, algorithm, keySize,
                                        signatureAlgorithm, validFrom, validTo);

                        // Add it to the store and reload
                        store.getStore().setCertificateEntry(resourceId, cert);
                        store.store();
                        reload();

                        handler.handleResult(returnCertificate(resourceId, cert));
                    }
                } else {
                    handler.handleError(new BadRequestException("Unsupported action "
                            + request.getAction()));
                }
            } catch (Throwable t) {
                handler.handleError(ResourceUtil.adapt(t));
            }
        }

        @Override
        public void createInstance(final ServerContext context, final CreateRequest request,
                final ResultHandler<Resource> handler) {
            try {

                Map<String, Object> resultMap = new HashMap<String, Object>();

                if (null != request.getNewResourceId()) {

                    if (store.getStore().containsAlias(request.getNewResourceId())) {
                        handler.handleError(new ConflictException("The resource with ID '"
                                + request.getNewResourceId() + "' could not be created because "
                                + "there is already another resource with the same ID"));
                    } else {

                        Certificate cert = generateCertificate(request.getContent());
                        store.getStore().setCertificateEntry(request.getNewResourceId(), cert);
                        store.store();
                        reload();

                        resultMap.put(Resource.FIELD_CONTENT_ID, request.getNewResourceId());

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

                    handler.handleResult(new Resource(resourceId, null, returnCertificate(resourceId, cert)));
                }
            } catch (Throwable t) {
                handler.handleError(ResourceUtil.adapt(t));
            }
        }

        @Override
        public void updateInstance(final ServerContext context, final String resourceId,
                final UpdateRequest request, final ResultHandler<Resource> handler) {
            try {
                if (!store.getStore().containsAlias(resourceId)) {
                    handler.handleError(new NotFoundException());
                } else {
                    Certificate cert = generateCertificate(request.getNewContent());
                    store.getStore().setCertificateEntry(resourceId, cert);
                    store.store();
                    reload();

                    handler.handleResult(new Resource(resourceId, null, returnCertificate(
                            resourceId, cert)));
                }
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
                    reload();
                }

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

        private void reload() throws Exception {
            TrustManagerFactory tmf =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(store.getStore());
            TrustManager[] managers = tmf.getTrustManagers();
            SSLContext context = SSLContext.getInstance("SSL");
            context.init(null, managers, null);
            SSLContext.setDefault(context);
        }


        private JsonValue returnCertificate(String alias, Certificate cert) throws Exception {
            JsonValue content = new JsonValue(new LinkedHashMap<String, Object>(3));
            content.put(Resource.FIELD_CONTENT_ID, alias);
            content.put("type", cert.getType());
            content.put("publicKey", getPublicKeyResult(cert));
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

        //TODO move this out from here
        private void reload() throws Exception {
            KeyManagerFactory kmf =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(store.getStore(), "changeit".toCharArray());
            KeyManager[] managers = kmf.getKeyManagers();
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(managers, null, null);
            SSLContext.setDefault(context);
        }
    };

    private Map<String, Object> getPublicKeyResult(Certificate cert) throws Exception {
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
        String type = content.get("type").defaultTo("X509").asString();

        StringReader sr = new StringReader(certString);
        PEMReader pw = new PEMReader(sr);
        Object object = pw.readObject();
        if (object instanceof X509Certificate) {
            return (X509Certificate) object;
        } else {
            throw new BadRequestException("Unsupported certificate format");
        }
    }

    /**
     * Generates a self signed certificate from a given domain name.
     * 
     * @param domainName
     *            the domain name to use for the new certificate
     * @return The generated certificate
     * @throws Exception
     */
    private X509Certificate generateCertificate(String domainName, String algorithm, int keySize,
            String signatureAlgorithm, String validFrom, String validTo) throws Exception {

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm); // "RSA",
                                                                                     // "BC"
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
                throw new InternalServerErrorException(
                        "Invalid date format for 'validFrom' property");
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

        // BigInteger.valueOf(Math.abs(new SecureRandom().nextLong()))
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());

        X509v3CertificateBuilder v3CertGen =
                new JcaX509v3CertificateBuilder(builder.build(), serial, notBefore, notAfter,
                        builder.build(), keyPair.getPublic());

        ContentSigner sigGen =
                new JcaContentSignerBuilder(null != signatureAlgorithm ? signatureAlgorithm
                        : "SHA256WithRSAEncryption") // MD5WithRSAEncryption
                        .setProvider(BC).build(keyPair.getPrivate());

        X509Certificate cert =
                new JcaX509CertificateConverter().setProvider(BC).getCertificate(
                        v3CertGen.build(sigGen));
        cert.checkValidity(new Date());
        cert.verify(cert.getPublicKey());

        return cert;
    }

    public static X509Certificate sign(PKCS10CertificationRequest inputCSR, PrivateKey caPrivate,
            KeyPair pair) throws InvalidKeyException, NoSuchAlgorithmException,
            NoSuchProviderException, SignatureException, IOException, OperatorCreationException,
            CertificateException {

        AlgorithmIdentifier sigAlgId =
                new DefaultSignatureAlgorithmIdentifierFinder().find("SHA1withRSA");
        AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);

        AsymmetricKeyParameter caKeyParam = PrivateKeyFactory.createKey(caPrivate.getEncoded());
        SubjectPublicKeyInfo keyInfo =
                SubjectPublicKeyInfo.getInstance(pair.getPublic().getEncoded());

        PKCS10CertificationRequestHolder pk10Holder =
                new PKCS10CertificationRequestHolder(inputCSR);

        X509v3CertificateBuilder v3CertGen =
                new X509v3CertificateBuilder(new X500Name("CN=issuer"), new BigInteger("1"),
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
        // return null;
    }

}
