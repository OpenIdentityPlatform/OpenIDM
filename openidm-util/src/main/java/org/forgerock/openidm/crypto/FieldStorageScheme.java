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
 * Portions copyright 2015 ForgeRock AS.
 */
package org.forgerock.openidm.crypto;

/**
 * This interface defines a field storage scheme. 
 */
public interface FieldStorageScheme {

    /**
     * Returns a hashed, salted, and encoded version of the supplied plain text field.
     * 
     * @param plaintext a plain text version of the field to encode.
     * @return a hashed, salted, and encoded field.
     */
    public String hashField(String plaintext);
    
    /**
     * Returns true if the supplied plain text field matches the supplied stored field after being
     * encoded and compared, false otherwise.
     * 
     * @param plaintextfield the plain text field to compare.
     * @param storedField the encoded field to compare.
     * @return true if the fields match, false otherwise.
     */
    public boolean fieldMatches(String plaintextfield, String storedField);
}
