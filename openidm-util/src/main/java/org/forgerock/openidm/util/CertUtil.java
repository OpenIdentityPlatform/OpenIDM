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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openidm.util;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.joda.time.DateTime;

/**
 * Utilities for generating, serializing, and deserializing {@link Certificate Certificates}.
 */
public class CertUtil {

    private static final String BC = org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

    private CertUtil() {
        // prevent instantiation
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
    public static Pair<X509Certificate, PrivateKey> generateCertificate(String commonName,
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
    public static Pair<X509Certificate, PrivateKey> generateCertificate(String commonName,
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

        Date notBefore;
        Date notAfter;
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
     * Returns a PEM formatted string representation of an object
     *
     * @param object the object to write
     * @return a PEM formatted string representation of the object
     * @throws Exception
     */
    public static String getCertString(Object object) throws Exception {
        if (object instanceof PKCS10CertificationRequest) {
            PKCS10CertificationRequest pkcs10=(PKCS10CertificationRequest)object;
            String type = "CERTIFICATE REQUEST";
            byte[] encoding = pkcs10.getEncoded();

            PemObject pemObject = new PemObject(type, encoding);

            StringWriter str = new StringWriter();
            JcaPEMWriter pemWriter = new JcaPEMWriter(str);
            pemWriter.writeObject(pemObject);
            pemWriter.close();
            str.close();
            return str.getBuffer().toString();
        }
        try (StringWriter sw = new StringWriter(); JcaPEMWriter pemWriter = new JcaPEMWriter(sw)) {
            pemWriter.writeObject(object);
            pemWriter.flush();
            return sw.getBuffer().toString();
        }
    }

    /**
     * Returns an object from a PEM String representation
     *
     * @param pem the PEM String representation
     * @return the object
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public static <T> T fromPem(String pem) throws Exception {
        StringReader sr = new StringReader(pem);
        PEMParser pw = new PEMParser(sr);
        Object object = pw.readObject();
        return (T) object;
    }

    /**
     * Reads a certificate from a supplied string representation, and a supplied type.
     *
     * @param certString A String representation of a certificate
     * @return The certificate
     * @throws Exception
     */
    public static Certificate readCertificate(String certString) throws Exception {
        StringReader sr = new StringReader(certString);
        PEMParser pw = new PEMParser(sr);
        Object object = pw.readObject();
        if (object instanceof X509Certificate) {
            return (X509Certificate)object;
        } else {
            throw ResourceException.newResourceException(
                    ResourceException.BAD_REQUEST, "Unsupported certificate format");
        }
    }

    /**
     * Reads a certificate chain from a supplied string array representation, and a supplied type.
     *
     * @param certStringChain an array of strings representing a certificate chain
     * @return the certificate chain
     * @throws Exception
     */
    public static Certificate[] readCertificateChain(List<String> certStringChain) throws Exception {
        Certificate [] certChain = new Certificate[certStringChain.size()];
        for (int i=0; i<certChain.length; i++) {
            certChain[i] = readCertificate(certStringChain.get(i));
        }
        return certChain;
    }
}
