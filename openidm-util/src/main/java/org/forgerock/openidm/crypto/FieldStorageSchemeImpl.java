/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at legal-notices/CDDLv1_0.txt
 * or http://forgerock.org/license/CDDLv1.0.html.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at legal-notices/CDDLv1_0.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2006-2008 Sun Microsystems, Inc.
 *      Portions Copyright 2010-2015 ForgeRock AS.
 */
package org.forgerock.openidm.crypto;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import org.forgerock.util.encode.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class defines a field storage scheme based on the provided algorithm. This is a one-way digest 
 * algorithm so there is no way to retrieve the original clear-text version of the field from the hashed 
 * value.  The values that it generates are also salted, which protects against dictionary attacks. It 
 * does this by generating a random salt which is appended to the  clear-text value.  A hash is then 
 * generated based on this, the salt is appended to the hash, and  then the entire value is base64-encoded.
 */
public class FieldStorageSchemeImpl implements FieldStorageScheme {

    /**
     * Setup logging for the {@link SaltedSHA256PasswordStorageScheme}.
     */
    private final static Logger logger = LoggerFactory.getLogger(FieldStorageSchemeImpl.class);
    
    /**
     * The number of bytes of random data to use as the salt when generating the hashes.
     */
    private static final int NUM_SALT_BYTES = 16;

    /**
     * The message digest that will actually be used to generate the hashes.
     */
    private MessageDigest messageDigest;

    /** 
     * The lock used to provide thread safe access to the message digest. 
     */
    private Object digestLock;

    /** 
     * The secure random number generator to use to generate the salt values. 
     */
    private SecureRandom random;

    /** 
     * Size of the digest in bytes.
     */
    private int digestSize;

    /**
     * Creates a new instance of this field storage scheme.
     * 
     * @param digestSize the size of the digest in bytes.
     * @param algorithm the algorithm to use.
     * @throws Exception
     */
    public FieldStorageSchemeImpl(int digestSize, String algorithm) throws Exception {
        this.messageDigest = MessageDigest.getInstance(algorithm);
        this.digestLock = new Object();
        this.random     = new SecureRandom();
        this.digestSize = digestSize;
    }

    @Override
    public String hashField(String plaintext) {
        int plainBytesLength = plaintext.length();
        byte[] saltBytes     = new byte[NUM_SALT_BYTES];
        byte[] plainPlusSalt = new byte[plainBytesLength + NUM_SALT_BYTES];

        System.arraycopy(plaintext.getBytes(),0, plainPlusSalt, 0, plainBytesLength);
        byte[] digestBytes;

        synchronized (digestLock) {
            try {
                // Generate the salt and put in the plain+salt array.
                random.nextBytes(saltBytes);
                System.arraycopy(saltBytes,0, plainPlusSalt, plainBytesLength, NUM_SALT_BYTES);

                // Create the hash from the concatenated value.
                digestBytes = messageDigest.digest(plainPlusSalt);
            } catch (Exception e) {
                logger.error("Cannot encode field: " + e.getMessage(), e);
                throw e;
            } finally {
                Arrays.fill(plainPlusSalt, (byte) 0);
            }
        }

        // Append the salt to the hashed value and base64-the whole thing.
        byte[] hashPlusSalt = new byte[digestBytes.length + NUM_SALT_BYTES];

        System.arraycopy(digestBytes, 0, hashPlusSalt, 0, digestBytes.length);
        System.arraycopy(saltBytes, 0, hashPlusSalt, digestBytes.length, NUM_SALT_BYTES);

        return Base64.encode(hashPlusSalt);
    }

    @Override
    public boolean fieldMatches(String plaintextfield, String storedField) {
        // Base64-decode the stored value and take the first 256 bits (SHA256_LENGTH) as the digest.
        byte[] saltBytes;
        byte[] digestBytes = new byte[digestSize];
        int saltLength = 0;

        try {
            byte[] decodedBytes = Base64.decode(storedField);

            saltLength = decodedBytes.length - digestSize;
            if (saltLength <= 0) {
                logger.error("Invalid decoded stored field", storedField);
                return false;
            }
            saltBytes = new byte[saltLength];
            System.arraycopy(decodedBytes, 0, digestBytes, 0, digestSize);
            System.arraycopy(decodedBytes, digestSize, saltBytes, 0, saltLength);
        } catch (Exception e) {
            // May catch NPE if Base64.decode returns null on bad (non-base64) input
            logger.error("Cannot decode stored field", storedField, e);
            return false;
        }

        // Use the salt to generate a digest based on the provided plain-text value.
        int plainBytesLength = plaintextfield.length();
        byte[] plainPlusSalt = new byte[plainBytesLength + saltLength];

        System.arraycopy(plaintextfield.getBytes(),0, plainPlusSalt, 0, plainBytesLength);
        System.arraycopy(saltBytes, 0,plainPlusSalt, plainBytesLength, saltLength);

        byte[] userDigestBytes;

        synchronized (digestLock) {
            try {
                userDigestBytes = messageDigest.digest(plainPlusSalt);
            } catch (Exception e) {
                logger.error("Cannot encode field", storedField, e);
                return false;
            } finally {
                Arrays.fill(plainPlusSalt, (byte) 0);
            }
        }

        return Arrays.equals(digestBytes, userDigestBytes);
    }
    
}
