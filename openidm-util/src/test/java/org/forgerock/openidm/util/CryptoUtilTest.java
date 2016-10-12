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
 * Copyright 2011-2016 ForgeRock AS.
 */
package org.forgerock.openidm.util;

import org.testng.Assert;
import org.testng.annotations.Test;

public class CryptoUtilTest {
    @Test
    public void testObfuscate() throws Exception {
        String expected = "So Long, and Thanks for All the Fish";
        String actual = CryptoUtil.obfuscate(expected);
        Assert.assertEquals(CryptoUtil.deobfuscate(actual), expected);
    }

    @Test
    public void testEncrypt() throws Exception {
        String expected = "How many roads must a man walk down?";
        String actual = CryptoUtil.encrypt(expected);
        Assert.assertEquals(CryptoUtil.decrypt(actual), expected);
    }

    @Test
    public void testUnfold() throws Exception {
        String expectedA = "So Long, and Thanks for All the Fish";
        String obfuscated = CryptoUtil.obfuscate(expectedA);
        String expectedB = "How many roads must a man walk down?";
        String encrypted = CryptoUtil.encrypt(expectedB);
        Assert.assertEquals(CryptoUtil.unfold(obfuscated), expectedA.toCharArray());
        Assert.assertEquals(CryptoUtil.unfold(encrypted), expectedB.toCharArray());
    }
}
