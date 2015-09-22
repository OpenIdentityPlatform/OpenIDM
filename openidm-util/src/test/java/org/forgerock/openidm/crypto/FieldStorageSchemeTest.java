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

import static org.fest.assertions.api.Assertions.assertThat;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests FieldStorageScheme methods
 */
public class FieldStorageSchemeTest {
    
    @DataProvider
    public Object[][] testData() throws Exception {
        return new Object[][] {
                { new SaltedMD5FieldStorageScheme(), 44 },
                { new SaltedSHA1FieldStorageScheme(), 48 },
                { new SaltedSHA256FieldStorageScheme(), 64 },
                { new SaltedSHA384FieldStorageScheme(), 88 },
                { new SaltedSHA512FieldStorageScheme(), 108 }  
        };
    }
    
    @Test(dataProvider = "testData")
    public void testFieldStorageScheme(FieldStorageScheme fieldStorageScheme, int hashedLength) {
        String testField = "valueToHash";
        
        // Test field hashing
        String hashedField = fieldStorageScheme.hashField(testField);
        assertThat(hashedField).isNotNull();
        assertThat(hashedField.length()).isEqualTo(hashedLength);
        
        // Test matching against hashed field
        assertThat(fieldStorageScheme.fieldMatches(testField, hashedField)).isTrue();
        assertThat(fieldStorageScheme.fieldMatches(testField + " ", hashedField)).isFalse();
    }
}
